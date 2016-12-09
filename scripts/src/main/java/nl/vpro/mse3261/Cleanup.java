package nl.vpro.mse3261;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import nl.vpro.domain.media.update.GroupUpdate;
import nl.vpro.domain.media.update.MediaUpdate;
import nl.vpro.domain.media.update.ProgramUpdate;
import nl.vpro.domain.media.update.SegmentUpdate;
import nl.vpro.rs.media.MediaRestClient;
import nl.vpro.util.Env;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class Cleanup {

    private static Unmarshaller UNMARSHAL;
    static {
        try {
            UNMARSHAL = JAXBContext.newInstance(SegmentUpdate.class, ProgramUpdate.class, GroupUpdate.class).createUnmarshaller();
        } catch (JAXBException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws IOException {
        MediaRestClient client = MediaRestClient.configured(Env.PROD).build();

        List<String> midsToFix = new ArrayList<>();
        File errorneous = new File("/tmp/segmentswithoutitle.txt");
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
                        Cleanup.log.error(xml + "\n" + ue.getMessage());
                        xml = "";
                        continue;
                    }
                    xml = "";
                    Cleanup.log.debug("{}", media);
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
                            Cleanup.log.info("Not found {}", segment);
                            continue;
                        }
                    }

                    Cleanup.log.info(segment.getMidRef() + " " + Duration.ofMillis(segment.getStart().getTime()) + " " + segment.getTitles());

                    File baseDir = new File("/tmp/mse3162");
                    File dir;
                    if (! midsToFix.contains(segment.getMid())) {
                        dir = new File(baseDir, "noneed");
                    } else {
                        dir = new File(baseDir, segment.getBroadcasters().get(0));
                    }
                    dir.mkdirs();
                    File create = new File(dir, segment.getMid() + ".xml");
                    JAXB.marshal(segment, create);
                    Cleanup.log.info("Created {}", create);
                } finally {
                    line = buffered.readLine();
                }



            }


        }
        Cleanup.log.info("Unhandled mids " + midsToFix + " " + midsToFix.size());
        Cleanup.log.info("READY");
        client.shutdown();

    }

}
