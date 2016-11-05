package nl.vpro.api.client.utils;

import lombok.EqualsAndHashCode;

import java.util.*;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.collect.Lists;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.IdList;
import nl.vpro.domain.api.MultiplePageResult;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.api.page.PageFormBuilder;
import nl.vpro.domain.api.page.PageSearchResult;
import nl.vpro.domain.page.Embed;
import nl.vpro.domain.page.Page;

/**
 * @author Michiel Meeuwissen
 */
@Named
public class NpoApiPageUtil  {

    final NpoApiClients clients;
    final NpoApiRateLimiter limiter;


    @Inject
    public NpoApiPageUtil(NpoApiClients clients, NpoApiRateLimiter limiter) {
        this.clients = clients;
        this.limiter = limiter;
    }

    public NpoApiPageUtil(NpoApiClients clients) {
        this(clients, new NpoApiRateLimiter());
    }

    public Page[] load(String... id) {
        Page[] result = new Page[id.length];
        if (id.length > 0) {
            limiter.acquire();
            MultiplePageResult pageResult = clients.getPageService().loadMultiple(new IdList(id), null, null);

            for (int i = 0; i < id.length; i++) {
                result[i] = pageResult.getItems().get(i).getResult();
            }

            limiter.upRate();
        }
        return result;
    }

    @EqualsAndHashCode
    private class SupplyKey {
        private final List<String> profiles;
        private final String props;

        private SupplyKey(List<String> profiles, String props) {
            this.profiles = profiles;
            this.props = props;
        }

    }
    private final Map<SupplyKey, PageSupplier> pageSupplier = new HashMap<>();

    public Supplier<Optional<Page>> supplyByMid(List<String> profiles, String props, String mid) {
        SupplyKey key = new SupplyKey(profiles, props);
        PageSupplier supplier = pageSupplier.computeIfAbsent(key, PageSupplier::new);
        return supplier.add(mid);
    }

    public PageSearchResult find(
        PageForm form,
        String profile,
        Long offset,
        Integer max) {
        limiter.acquire();
        PageSearchResult result = clients.getPageService().find(form, profile, null, offset == null ? 0 : offset, max);
        limiter.upRate();
        return result;
    }


    public Page[] loadByMid(List<String> profiles, String props, String... mids) {

        Map<String, Page> map = new HashMap<>();


        for (String profile : profiles) {
            PageFormBuilder builder = PageFormBuilder.form();
            for (List<String> idList : Lists.partition(Arrays.asList(mids), 500)) {

                builder.mediaForm().mediaIds(idList.toArray(new String[idList.size()]));
                PageSearchResult pages =
                    clients.getPageService().find(builder.build(), profile, props, 0L, 240);
                for (Page page : pages.asList()) {
                    if (page != null && page.getEmbeds() != null) {
                        for (Embed embed : page.getEmbeds()) {
                            String mid = embed.getMedia().getMid();
                            if (!map.containsKey(mid)) {
                                map.put(mid, page);
                            }
                        }
                    }
                }
            }
        }
        Page[] result = Collections.nCopies(mids.length, null).toArray(new Page[mids.length]);
        for (int i = 0; i < mids.length; i++) {
            result[i] = map.get(mids[i]);
        }
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(clients);
    }


    public NpoApiClients getClients() {
        return clients;
    }

    private class PageSupplier {
        private List<String> mids = new ArrayList<>();
        private Page[] results;

        private final SupplyKey key;

        private PageSupplier(SupplyKey key) {
            this.key = key;
        }

        Supplier<Optional<Page>> add(final String mid) {
            if (results != null) {
                throw new IllegalStateException();
            }
            if (!mids.contains(mid)) {
                mids.add(mid);
            }
            return () -> get(mid);
        }

        private Optional<Page> get(String mid) {
            NpoApiPageUtil.this.pageSupplier.put(key, new PageSupplier(key));
            if (results == null) {
                results = NpoApiPageUtil.this.loadByMid(key.profiles, key.props, mids.toArray(new String[mids.size()]));
            }
            return Optional.ofNullable(results[mids.indexOf(mid)]);
        }

    }
}
