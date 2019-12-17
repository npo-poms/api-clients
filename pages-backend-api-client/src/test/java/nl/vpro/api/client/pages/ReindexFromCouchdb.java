package nl.vpro.api.client.pages;

import java.io.*;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.page.update.*;
import nl.vpro.jackson2.JsonArrayIterator;
import nl.vpro.rs.pages.update.PageUpdateRestService;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
@Disabled
public class ReindexFromCouchdb {
    static PageUpdateApiClient client;
    static {
        client = PageUpdateApiClient.configured().build();
    }

    @Test
    public void reindex() throws IOException {
        JsonArrayIterator<PageUpdate> update =
                new JsonArrayIterator<>(new FileInputStream(new File("/tmp/pageupdates.json")), (jp, tree) -> {
            try {
                return jp.getCodec().treeToValue(tree.get("doc"), PageUpdate.class);
            } catch (JsonProcessingException e) {
                throw new JsonArrayIterator.ValueReadException(e);
            }
        });
        long count = 0;
        PageUpdateRestService restService = client.getPageUpdateRestService();
        LOOP:
        while (update.hasNext()) {
            PageUpdate pu = update.next();
            if (update.getCount() % 1000 == 0) {
                System.out.print('.');
                System.out.flush();
            }
            if (pu.getPortal() == null) {
                continue;
            }
            if (pu.getPortal().getId().equals("VPRONL")) {
                if (pu.getUrl().startsWith("http://www-test.vpro.nl/cinema")) {

                    if (pu.getPortal().getSection() == null || pu.getPortal().getSection().getPath() == null) {
                        continue;
                    }
                    switch(pu.getType()) {
                        case HOME:
                        case ARTICLE:
                            continue LOOP;
                    }
                    if (pu.getPortal().getSection().getPath().equals("/cinema")) {
                        String url = pu.getUrl().replace("www-test.", "www.");
                        url = url.replaceAll("/cinema/film~", "/cinema/films/film~");
                        url = url.replaceAll("/cinema/persoon~", "/cinema/personen/persoon~");
                        pu.setUrl(url);
                        pu.getPortal().setUrl("http://www.vpro.nl/cinema.html");
                        //pu.setRevision(null);
                        pu.setLastPublished(null);
                        for (ImageUpdate u : pu.getImages()) {
                            ImageLocation il = (ImageLocation) u.getImage();
                            String imageUrl = il.getUrl();
                            imageUrl = imageUrl.replaceAll("jpg", "jpeg");
                            imageUrl = imageUrl.replaceAll("www-test.", "www.");
                            il.setUrl(imageUrl);
                        }
                        count++;
                        System.out.println(url);
                        Response response = restService.save(pu, false);
                        response.close();
                        if (response.getStatus() != 202) {
                            System.out.println("" + response);
                        }

                    }
                }
            }

        }
        System.out.println("Count " + count);
    }
}
