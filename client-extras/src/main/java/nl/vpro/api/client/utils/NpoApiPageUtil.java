package nl.vpro.api.client.utils;

import lombok.EqualsAndHashCode;

import java.util.*;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.common.collect.Lists;

import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.page.*;
import nl.vpro.domain.page.Embed;
import nl.vpro.domain.page.Page;
import nl.vpro.util.BatchedReceiver;
import nl.vpro.util.CloseableIterator;

/**
 * Utility that wraps some 'pages' endpoint of the NPO frontend API. {@link NpoApiClients}
 * It may simplifyy parameters, implement default values. It also added client side rate limiting.
 *
 * @author Michiel Meeuwissen
 * @see NpoApiMediaUtil
 * @see NpoApiImageUtil
 */
@Named
public class NpoApiPageUtil {

    final NpoApiClients clients;
    final NpoApiRateLimiter limiter;
    private final Map<SupplyKey, PageSupplier> pageSupplier = new HashMap<>();

    @Inject
    public NpoApiPageUtil(NpoApiClients clients, NpoApiRateLimiter limiter) {
        this.clients = clients;
        this.limiter = limiter;
    }

    public NpoApiPageUtil(NpoApiClients clients) {
        this(clients, new NpoApiRateLimiter());
    }

    public Page[] load(String... id) {
        return load(new IdList(id));
    }

    public Page[] load(IdList idList) {
        return loadMultipleEntries(idList).stream().map(MultipleEntry::getResult).toArray(Page[]::new);
    }

    public List<MultipleEntry<Page>> loadMultipleEntries(final IdList idList) {
        final List<MultipleEntry<Page>> result = new ArrayList<>();
        if (idList.size() > 0) {
            BatchedReceiver.<MultipleEntry<Page>>builder()
                .batchSize(240)
                .batchGetter((offset, max) -> {
                    int end = Math.min(offset.intValue() + max, idList.size());
                    if (offset >= end) {
                        return null;
                    }
                    limiter.acquire();
                    MultiplePageResult pageResult = clients.getPageService().loadMultiple(idList.subList(offset.intValue(), end), null, null);
                    limiter.upRate();
                    return pageResult.iterator();
                })
                .build().forEachRemaining(result::add);
        }
        return result;
    }

    public Page get(String id) {
        return load(id)[0];
    }

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

                builder.mediaForm().mediaIds(idList.toArray(new String[0]));
                PageSearchResult pages =
                    clients.getPageService().find(builder.build(), profile, props, 0L, 240);
                for (Page page : pages.asList()) {
                    for (Embed embed : page.getEmbeds()) {
                        String mid = embed.getMedia().getMid();
                        if (!map.containsKey(mid)) {
                            map.put(mid, page);
                        }
                    }
                }
            }
        }
        Page[] result = Collections.<Page>nCopies(mids.length, null)
            .toArray(new Page[0]);
        for (int i = 0; i < mids.length; i++) {
            result[i] = map.get(mids[i]);
        }
        return result;
    }

    public CloseableIterator<Page> iterate(PageForm form, String profile) {
        limiter.acquire();
        try {
            CloseableIterator<Page> result = PageRestClientUtils.iterate(clients.getPageServiceNoTimeout(), form, profile);
            limiter.upRate();
            return result;
        } catch (Throwable e) {
            limiter.downRate();
            throw new RuntimeException(clients + ":" + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(clients);
    }

    public NpoApiClients getClients() {
        return clients;
    }

    @EqualsAndHashCode
    private static class SupplyKey {
        private final List<String> profiles;
        private final String props;

        private SupplyKey(List<String> profiles, String props) {
            this.profiles = profiles;
            this.props = props;
        }

    }

    private class PageSupplier {
        private final List<String> mids = new ArrayList<>();
        private final SupplyKey key;
        private Page[] results;

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
                results = NpoApiPageUtil.this.loadByMid(key.profiles, key.props, mids
                    .toArray(new String[0]));
            }
            int idx = mid != null ? mids.indexOf(mid) : -1;
            return Optional.ofNullable((idx >= 0 && idx < results.length) ? results[idx] : null);
        }

    }
}
