package nl.vpro.mse3261;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.domain.media.update.GroupUpdate;
import nl.vpro.domain.media.update.MediaUpdate;
import nl.vpro.domain.media.update.ProgramUpdate;
import nl.vpro.domain.media.update.SegmentUpdate;
import nl.vpro.rs.media.MediaRestClient;

/**
 * @author Michiel Meeuwissen
 */
public class Cleanup {

    private static final Logger LOG = LoggerFactory.getLogger(Cleanup.class);

    private static Unmarshaller UNMARSHAL;
    static {
        try {
            UNMARSHAL = JAXBContext.newInstance(SegmentUpdate.class, ProgramUpdate.class, GroupUpdate.class).createUnmarshaller();
        } catch (JAXBException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws IOException {
        MediaRestClient client = new MediaRestClient().configured("prod");

        List<String> midsToFix = new ArrayList<>();
        File errorneous = new File("/tmp/segmentswithoutittle.txt");
        try (FileReader reader = new FileReader(errorneous);
             BufferedReader buffered = new BufferedReader(reader);
        ) {
            String line = buffered.readLine();
            while (line != null) {
                String mid = line.trim().split("[\\s|]+", 2)[0];
                midsToFix.add(mid);
                line = buffered.readLine();
            }

        }


        File log = new File("/tmp/segments.log");
        try(FileReader reader = new FileReader(log);
            BufferedReader buffered = new BufferedReader(reader);
        ) {
            String line = buffered.readLine();
            String xml = "";
            while (line != null) {
                try {
                    String[] split = line.split("\\s+", 7);
                    String method = split[4];
                    if (! "POST".equals(method)) {
                        continue;
                    }
                    if (split.length < 7) {
                        continue;
                    }
                    if ("400".equals(split[6])) {
                        continue;
                    }
                    if ("202".equals(split[6])) {
                        continue;
                    }
                    if ("200".equals(split[6])) {
                        continue;
                    }
                    xml += split[6];
                    if (! xml.endsWith(">")) {
                        continue;
                    }
                    MediaUpdate media;
                    try {
                        media = (MediaUpdate) UNMARSHAL.unmarshal(new StringReader(xml));
                    } catch (Exception ue) {
                        LOG.error(xml + "\n" + ue.getMessage());
                        xml = "";
                        continue;
                    }
                    xml = "";
                    LOG.debug("{}", media);
                    if (! (media instanceof SegmentUpdate)) {
                        continue;

                    }
                    SegmentUpdate segment = (SegmentUpdate) media;

                    if (segment.getMid() == null) {
                        boolean matched = false;
                        ProgramUpdate parent = client.getProgram(segment.getMidRef());

                        for (SegmentUpdate segmentUpdate : parent.getSegments()) {
                            if (segmentUpdate.getStart().equals(segment.getStart())) {
                                matched = true;
                                segment.setMid(segmentUpdate.getMid());
                            }
                        }
                        if (!matched) {
                            LOG.info("Not found {}", segment);
                            continue;
                        }
                    }

                    if (!midsToFix.contains(segment.getMid())) {
                        LOG.info("No need to fix {}", segment);
                        continue;
                    }
                    LOG.info(segment.getMidRef() + " " + Duration.ofMillis(segment.getStart().getTime()) + " " + segment.getTitles());
                    File dir = new File("/tmp/mse3162/" + segment.getBroadcasters().get(0));
                    dir.mkdirs();
                    File create = new File(dir, segment.getMid() + ".xml");
                    JAXB.marshal(segment, create);
                    LOG.info("Created {}", create);
                    midsToFix.remove(segment.getMid());
                } finally {
                    line = buffered.readLine();
                }



            }


        }
        LOG.info("Unhandled mids " + midsToFix + " " + midsToFix.size());
        LOG.info("READY");

    }

}
