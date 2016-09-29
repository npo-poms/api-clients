package nl.vpro.api.client.utils;

import java.time.Instant;
import java.time.LocalDate;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.ScheduleForm;
import nl.vpro.domain.api.media.ScheduleRepository;
import nl.vpro.domain.api.media.ScheduleResult;
import nl.vpro.domain.api.media.ScheduleSearchResult;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.Channel;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaType;
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
    public ScheduleResult listSchedules(Instant start, Instant stop, Order order, long offset, Integer max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleResult listSchedules(Channel channel, Instant start, Instant stop, Order order, long offset, Integer max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleResult listSchedules(Channel channel, LocalDate guideDay, Order order, long offset, Integer max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleResult listSchedules(Net net, Instant start, Instant stop, Order order, long offset, Integer max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleResult listSchedulesForBroadcaster(String broadcaster, Instant start, Instant stop, Order order, long offset, Integer max) {
        throw new UnsupportedOperationException();

    }

    @Override
    public ScheduleResult listSchedulesForAncestor(String mediaId, Instant start, Instant stop, Order order, long offset, Integer max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleResult listSchedulesForMediaType(MediaType mediaType, Instant start, Instant stop, Order order, long offset, Integer max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleSearchResult findSchedules(ProfileDefinition<MediaObject> profileDefinition, ScheduleForm scheduleForm, long l, Integer integer) {
        throw new UnsupportedOperationException();

    }

}
