package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.page.Page;
import nl.vpro.util.Env;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("This is an integration test")
@Slf4j
public class NpoApiPageUtilTest {

    private static String[] TEST_MIDS = {"AVRO_1656037", "AVRO_1656037", "POMS_VPRO_487567", "BLOE_234", "WO_VPRO_4993480"};

    private NpoApiPageUtil util = new NpoApiPageUtil(
        NpoApiClients.configured(Env.PROD).build(),
        new NpoApiRateLimiter());

    @Test
    public void testLoadMultiple() {
        Page[] result = util.loadByMid(Arrays.asList("vpro", null), null, TEST_MIDS);
        System.out.println(Arrays.asList(result));
        System.out.println(util.getClients().getCounts());
        assertThat(util.getClients().getCount("PageRestService.find")).isEqualTo(2);

    }

    @Test
    public void testSupplier() {
        List<Supplier<Optional<Page>>> result = new ArrayList<>();
        for (String m : TEST_MIDS) {
            result.add(util.supplyByMid(Arrays.asList("vpro", null), null, m));
        }

        System.out.println(util.getClients().getCounts());

        for (Supplier<Optional<Page>> p : result) {
            System.out.println(p.get().orElse(null));
        }
        System.out.println(util.getClients().getCounts());
        assertThat(util.getClients().getCount("PageRestService.find")).isEqualTo(2);

    }


    @Test
    public void iterate() throws IOException {
        String profile = "vpro-predictions";
        File out = new File("/tmp/" + profile);
        try (BufferedWriter stream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out)))) {
            AtomicLong count = new AtomicLong(0);
            util.iterate(new PageForm(), profile).forEachRemaining((p) -> {
                if (count.incrementAndGet() % 1000 == 0) {
                    log.info("{} {}", count.get(), p.getUrl());
                }
                try {
                    stream.write(p.getUrl() + "\n");
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }

            });
        }

    }



}
