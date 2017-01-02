package nl.vpro.mse3526;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.api.client.utils.NpoApiMediaUtil;
import nl.vpro.domain.api.media.RedirectList;
import nl.vpro.domain.media.DescendantRef;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.jackson2.JsonArrayIterator;
import nl.vpro.util.Env;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class Search {


    public static void main(String[] args) throws IOException {
        NpoApiClients client = NpoApiClients.configured(Env.PROD).build();
        NpoApiMediaUtil util = new NpoApiMediaUtil(client);

        RedirectList redirects = util.redirects();

        File es = new File("/Users/michiel/npo/api/trunk/scripts/NPA-212/es.all.json");
        JsonArrayIterator<MediaObject> objects = new JsonArrayIterator<>(
            new FileInputStream(es),
            MediaObject.class);

        MediaObject newestMediaObject = null;
        Instant newest = Instant.EPOCH;
        long count = 0;
        long totalCount = 0;
        while(objects.hasNext()) {
            MediaObject o = objects.next();
            if (o.getWorkflow() != Workflow.PUBLISHED) {
                continue;
            }
            totalCount++;
            boolean found = false;
            for (DescendantRef ref : o.getDescendantOf()) {
                if (redirects.getMap().containsKey(ref.getMidRef())) {
                    found = true;
                }
            }
            if (found) {
                Instant publisheddate = o.getLastPublishedInstant();
                if (publisheddate == null){
                    publisheddate = o.getLastModifiedInstant();
                }
                count++;
                if (publisheddate.isAfter(newest)) {
                    newestMediaObject = o;
                    newest = newestMediaObject.getLastModifiedInstant();
                    //System.out.print(".");
                }
                System.out.println(o.getMediaType() + "\t" + o.getMid() + "\t" + o.getMainTitle() + "\t" + o.getLastPublishedInstant());
            }
        }
        log.info("{}/{} Found errorneous descendant ref ", count, totalCount, newestMediaObject);


    }
}
