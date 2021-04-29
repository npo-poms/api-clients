package nl.vpro.api.client.utils;

import java.time.Instant;
import java.time.LocalDate;

import javax.inject.Inject;
import javax.inject.Named;

import org.checkerframework.checker.nullness.qual.NonNull;

import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.*;

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
    public ScheduleResult listSchedules(Instant start, Instant stop, Order order, long offset, Integer max) {
        return util.getClients().getScheduleService().list(null, start, stop, null, order(order), offset, max);
    }

    @Override
    public ScheduleResult listSchedules(@NonNull Channel channel, Instant start, Instant stop, Order order, long offset, Integer max) {
        return util.getClients().getScheduleService().listChannel(channel.name(), null, start, stop, null, order(order), offset, max);
    }

    @Override
    public ScheduleResult listSchedules(@NonNull Channel channel, LocalDate guideDay, Order order, long offset, Integer max) {
        return util.getClients().getScheduleService().listChannel(channel.name(), guideDay, null, null, null, order(order), offset, max);
    }

    @Override
    public ScheduleResult listSchedules(Net net, Instant start, Instant stop, Order order, long offset, Integer max) {
        return util.getClients().getScheduleService().listNet(net.getId(), null, start, stop, null, order(order), offset, max);
    }

    @Override
    public ScheduleResult listSchedulesForBroadcaster(String broadcaster, Instant start, Instant stop, Order order, long offset, Integer max) {
        return util.getClients().getScheduleService().listBroadcaster(broadcaster, null, start, stop, null, order(order), offset, max);
    }

    @Override
    public ScheduleResult listSchedulesForAncestor(String mediaId, Instant start, Instant stop, Order order, long offset, Integer max) {
        return util.getClients().getScheduleService().listForAncestor(mediaId, null, start, stop, null, order(order), offset, max);
    }

    @Override
    public ScheduleResult listSchedulesForMediaType(MediaType mediaType, Instant start, Instant stop, Order order, long offset, Integer max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleSearchResult findSchedules(ProfileDefinition<MediaObject> profileDefinition, ScheduleForm scheduleForm, Order order, long offset, Integer max) {
        return util.getClients().getScheduleService().find(scheduleForm, order(order), profileDefinition.getName(),  null, offset, max);
    }

    String order(Order order) {
        return order == null ? "asc" : order.name().toLowerCase();
    }

}
