package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Lists;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.subtitles.SubtitlesRestService;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.subtitles.Subtitles;
import nl.vpro.domain.subtitles.SubtitlesId;
import nl.vpro.jackson2.JsonArrayIterator;
import nl.vpro.util.FileCachingInputStream;
import nl.vpro.util.LazyIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Slf4j
public class MediaRestClientUtils {

    /**
     * Similar to the v1 call for easier migration
     */
    public static MediaForm getRecentPlayablePrograms(AVType avType) {

        MediaFormBuilder formBuilder = MediaFormBuilder.form();

        if (avType != null) {
            formBuilder.avTypes(Match.MUST, avType);
        }

        List<MediaType> excludedTypes = new ArrayList<>();
        for (GroupType type : GroupType.values()) {
            excludedTypes.add(type.getMediaType());
        }
        excludedTypes.add(ProgramType.TRACK.getMediaType());
        excludedTypes.add(SegmentType.SEGMENT.getMediaType());

        MediaForm form = formBuilder.types(Match.NOT, excludedTypes.toArray(new MediaType[excludedTypes.size()])).build();
        form.addSortField(new MediaSortOrder(MediaSortField.sortDate, Order.DESC));

        return form;
    }

    public static List<MediaObject> adapt(final MediaSearchResult result) {
        return new AbstractList<MediaObject>() {

            @Override
            public MediaObject get(int index) {
                SearchResultItem<? extends MediaObject> object = result.getItems().get(index);
                return object.getResult();

            }

            @Override
            public int size() {
                return result.getSize();
            }
        };
    }

    public static MediaObject loadOrNull(MediaRestService restService, String id) throws IOException {
        return wrapForOrNull(
            () -> restService.load(id, null, null),
            () -> id
        );
    }

    public static Subtitles loadOrNull(SubtitlesRestService restService, String mid, Locale language) throws IOException {
        return wrapForOrNull(
            () -> restService.get(mid, language),
            () -> SubtitlesId.builder().mid(mid).language(language).build().toString()
        );

    }

    /**
     * Converts some exceptions to 'null', most noticably {@link NotFoundException}
     *
     * @param supplier The action to perform
     * @param id Id to use for logging if exceptions happen
     * @throws IOException
     */
    private static <T> T wrapForOrNull(
        Supplier<T> supplier,
        Supplier<String> id
    ) throws IOException {
        try {
            return supplier.get();
        } catch (NotFoundException nfe) {
            // not even log, this is not errorneous
            return null;
        } catch (ProcessingException pe) {
            unwrapIO(pe);
            log.warn(id.get() + " " + pe.getMessage());
            return null;
        } catch (RuntimeException ise) {
            // Completely unexpected, this should remain to be an exception!
            throw ise;
        } catch (Exception e) {
            log.error(id.get() + " " + e.getClass().getName() + " " + e.getMessage());
            return null;
        }
    }


    public static void unwrapIO(ProcessingException pe) throws IOException {
        Throwable t = pe.getCause();
        if (t instanceof IOException) {
            throw (IOException) t;
        }
    }

    public static MediaObject[] load(MediaRestService restService, String... ids) {
        return loadWithMultiple(restService, ids);
        //loadWithSearch(restService, ids); // doesn't preserve order/duplicates, probable slower too.
    }

    private static MediaObject[] loadWithMultiple(MediaRestService restService, String... ids) {
        List<MediaObject> result = new ArrayList<>(ids.length);
        if (ids.length > 0) {
            for (List<String> idList : Lists.partition(Arrays.asList(ids), 240)) {
                MultipleMediaResult mediaResult = restService.loadMultiple(new IdList(idList), null, null);
                result.addAll(Lists.transform(mediaResult.getItems(), MultipleEntry::getResult));
            }
        }
        return result.toArray(new MediaObject[result.size()]);
    }
    // unused for now
    private static MediaObject[] loadWithSearch(MediaRestService restService, String... ids) {
        List<MediaObject> result = new ArrayList<>(ids.length);

        /*
         * Calling restService.find with max > Constants.MAX_RESULTS gets you a BadRequestException,
         * so the list of ids is partitioned first and if needed multiple find calls are executed...
         */
        for (List<String> idList : Lists.partition(Arrays.asList(ids), Constants.MAX_RESULTS)) {
            String[] partitionedIds = idList.toArray(new String[idList.size()]);
            MediaForm mediaForm = MediaFormBuilder.form().mediaIds(partitionedIds).build();
            MediaSearchResult mediaSearchResult = restService.find(mediaForm, null, null, 0L, idList.size());
            result.addAll(adapt(mediaSearchResult));
        }

        return result.toArray(new MediaObject[result.size()]);
    }

    @Deprecated
    public static JsonArrayIterator<Change> changes(MediaRestService restService, String profile, long since, Order order, Integer max) throws IOException {
        try {
            final InputStream inputStream = restService.changes(profile, null, since, null, order.name().toLowerCase(), max, null, null, null, null);
            return new JsonArrayIterator<>(inputStream, Change.class, () -> IOUtils.closeQuietly(inputStream));
        } catch (ProcessingException pi) {
            Throwable t = pi.getCause();
            throw new RuntimeException(t.getMessage(), t);
        }

    }

    public static JsonArrayIterator<Change> changes(MediaRestService restService, String profile, boolean profileCheck, Instant since, String mid, Order order, Integer max, Deletes deletes) throws IOException {
        try {

            final InputStream inputStream = restService.changes(profile, null, null, sinceString(since, mid), order.name().toLowerCase(), max, profileCheck, deletes, null, null);
            return new JsonArrayIterator<>(inputStream, Change.class, () -> IOUtils.closeQuietly(inputStream));
        } catch (ProcessingException pi) {
            Throwable t = pi.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new RuntimeException(t.getMessage(), t);
            }
        }

    }

    public static String sinceString(Instant since, String mid) {
        String sinceString = since == null ? null : since.toString();
        if (mid != null && sinceString != null) {
            sinceString += "," + mid;
        }
        return sinceString;
    }

    /**
     *
     * @deprecated We'll make a sitemap feature on page rest service.
     */
    @Deprecated
    public static Iterator<MediaObject> iterate(MediaRestService restService, MediaForm form, String profile) throws IOException {
		return new LazyIterator<>(
				() -> {
					try {
						final InputStream inputStream = restService.iterate(form, profile, null, 0L, Integer.MAX_VALUE, null, null);

                        // Cache the stream to a file first.
                        // If we don't do this, the stream seems to be inadvertedly truncated sometimes if the client doesn't consume the iterator fast enough.
                        FileCachingInputStream cacheToFile = FileCachingInputStream.builder()
                            .filePrefix("iterate-" + profile + "-")
                            .batchSize(1000000L)
                            .logger(log)
                            .input(inputStream)
                            .build();

						return JsonArrayIterator.<MediaObject>builder()
                            .inputStream(cacheToFile)
                            .valueClass(MediaObject.class)
                            .callback(() -> IOUtils.closeQuietly(inputStream))
                            .logger(log)
                            .build();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
    }


    static Properties properties = null;
    static File propertiesFile = null;
    static long timeStamp = -1;

    public static void setIdToMidFile(File file) {
        propertiesFile = file;
        timeStamp = -1;
        properties = null;
    }


    /**
     * Only call this during the migration to NPO API while not everything is converted to MID yet.
     *
     * Refresh id_to_mid.properties like so:
     * ssh vpro05ap@upload-sites.omroep.nl "psql -A -q -t -h poms2madb -U vpro mediadb -c 'select id,mid from mediaobject;'" | sed 's/|/=/' > /tmp/id_to_mid.properties ; scp /tmp/id_to_mid.properties uploadvpro:/e/ap/v3.rs.vpro.nl/data/
     *
     * @deprecated Migrate code and data from URN to MID.
     */
    @Deprecated
    public static String toMid(final String urn) {
        if (propertiesFile == null) {
            if (System.getProperty("id_to_mid.file") != null) {
                propertiesFile = new File(System.getProperty("id_to_mid.file"));
            } else {
                propertiesFile = new File("/e/ap/v3.rs.vpro.nl/data/id_to_mid.properties");
            }
        }
        if (propertiesFile.exists() && propertiesFile.lastModified() > timeStamp) {
            log.info("Will reload {}", propertiesFile);
            properties = null;
        }
        if (properties == null) {
            properties = new Properties();
            try {
                if (propertiesFile.exists()) {
                    properties.load(new FileInputStream(propertiesFile));
                    timeStamp = propertiesFile.lastModified();
                } else {
                    properties.load(MediaRestClientUtils.class.getResourceAsStream("/id_to_mid.properties"));
                }

            } catch (IOException e) {
                log.error(e.getMessage(), e);
                properties = null;
                return null;
            }
        }
        String id;
        int index = urn.lastIndexOf(":");
        if (index > 0) {
            id = urn.substring(index + 1);
        } else {
            id = urn;
        }
        return properties.getProperty(id);
    }


}
