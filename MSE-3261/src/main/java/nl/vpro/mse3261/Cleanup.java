package nl.vpro.mse3261;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.domain.media.update.ProgramUpdate;
import nl.vpro.domain.media.update.SegmentUpdate;
import nl.vpro.rs.media.MediaRestClient;

/**
 * @author Michiel Meeuwissen
 */
public class Cleanup {

    private static final Logger LOG = LoggerFactory.getLogger(Cleanup.class);
    public static void main(String[] args) throws IOException {
        MediaRestClient client = new MediaRestClient().configured();

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
            while (line != null) {
                String xml = line.split("\\s+", 7)[6];
                try {
                    SegmentUpdate segment;
                    try {
                        segment = JAXB.unmarshal(new StringReader(xml), SegmentUpdate.class);

                        if (! midsToFix.contains(segment.getMid())) {
                            continue;
                        }
                        System.out.println(segment.getMidRef() + " " + Duration.ofMillis(segment.getStart().getTime()) + " " + segment.getTitles());

                    } catch (Exception ue) {
                        LOG.error(line + " " + ue.getMessage());
                        continue;

                    }
                    ProgramUpdate parent = client.getProgram(segment.getMidRef());
                    boolean matched = false;
                    for (SegmentUpdate segmentUpdate : parent.getSegments()) {
                        if (segmentUpdate.getStart().equals(segment.getStart())) {
                            matched = true;
                            LOG.info("Matched " + segmentUpdate);
                            midsToFix.remove(segmentUpdate.getMid());
                        }
                    }
                } finally {
                    line = buffered.readLine();
                }



            }


        }
        LOG.info("Unhandled mids " + midsToFix + " " + midsToFix.size());

    }

}
