package nl.vpro.mse3526;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.api.client.utils.NpoApiMediaUtil;
import nl.vpro.domain.api.media.RedirectList;
import nl.vpro.domain.media.MediaObject;
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

        while(objects.hasNext()) {
            MediaObject o = objects.next();
            System.out.println(" " + o);
        }


    }
}
