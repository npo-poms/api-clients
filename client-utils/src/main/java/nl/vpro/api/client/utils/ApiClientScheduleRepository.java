package nl.vpro.api.client.utils;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.ScheduleResult;
import nl.vpro.domain.api.media.ScheduleRepository;
import nl.vpro.domain.media.Channel;
import nl.vpro.domain.media.Net;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Named
public class ApiClientScheduleRepository implements ScheduleRepository {

    final NpoApiMediaUtil util;

    @Inject
    public ApiClientScheduleRepository(NpoApiMediaUtil util) {
        this.util = util;

    }

    @Override
    public ScheduleResult listSchedules(Date start, Date stop, Order order, Long offset, int max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleResult listSchedules(Channel channel, Date start, Date stop, Order order, Long offset, int max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleResult listSchedules(Net net, Date start, Date stop, Order order, Long offset, int max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleResult listSchedulesForBroadcaster(String broadcaster, Date start, Date stop, Order order, Long offset, int max) {
        throw new UnsupportedOperationException();

    }

    @Override
    public ScheduleResult listSchedulesForAncestor(String mediaId, Date start, Date stop, Order order, Long offset, int max) {
        throw new UnsupportedOperationException();
    }
}
