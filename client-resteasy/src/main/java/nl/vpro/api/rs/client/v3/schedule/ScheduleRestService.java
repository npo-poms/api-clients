/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.client.v3.schedule;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import nl.vpro.domain.api.ApiScheduleEvent;
import nl.vpro.domain.api.Constants;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.ScheduleResult;
import nl.vpro.domain.api.media.ScheduleSearchResult;

/**
 * Deze class is er omdat de annotaties op nl.vpro.api.rs.v3.schedule.ScheduleRestService voor datums 'one-way' zijn.
 * Hierdoor kan de ResteasyClientBuilder niet het juiste format bepalen om de datums te marshallen.
 *
 * Door hier de argumenten van Date naar String te zetten, delegeren we dit probleem naar de uiteindelijke caller.
 * Die wordt geacht de datums in de juiste format (yyyy-MM-dd of ISO8601) op te sturen.
 *
 * @author Rico Jansen
 * @since 1.13
 */
@Path(ScheduleRestService.PATH)
@Produces(
        {MediaType.APPLICATION_JSON + "; charset=utf-8",
                MediaType.APPLICATION_XML + "; charset=utf-8"})
public interface ScheduleRestService {
    String PATH = "/schedule";

    String CHANNEL = "channel";
    String NET = "net";
    String BROADCASTER = "broadcaster";
    String ANCESTOR = "ancestor";

    String GUIDE_DAY = "guideDay";
    String START = "start";
    String STOP = "stop";
    String PROPERTIES = "properties";
    String SORT = "sort";
    String OFFSET = "offset";
    String MAX = "max";
    String PROFILE = "profile";

    String YEAR_MONTH_DATE = "yyyy-MM-dd";

    String ASC = "asc";

    String ZERO = "0";

    @GET
    ScheduleResult list(
            @QueryParam(GUIDE_DAY) String guideDay,
            @QueryParam(START) String start,
            @QueryParam(STOP) String stop,
            @QueryParam(PROPERTIES) String properties,
            @QueryParam(SORT) @DefaultValue(ASC) String sort,
            @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/ancestor/{ancestor}")
    ScheduleResult listForAncestor(
            @PathParam(ANCESTOR) String mediaId,
            @QueryParam(GUIDE_DAY) String guideDay,
            @QueryParam(START) String start,
            @QueryParam(STOP) String stop,
            @QueryParam(PROPERTIES) String properties,
            @QueryParam(SORT) @DefaultValue(ASC) String sort,
            @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/ancestor/{ancestor}/now")
    ApiScheduleEvent nowForAncestor(
            @PathParam(ANCESTOR) String mediaId,
            @QueryParam(PROPERTIES) String properties
    );

    @GET
    @Path("/ancestor/{ancestor}/next")
    ApiScheduleEvent nextForAncestor(
            @PathParam(ANCESTOR) String mediaId,
            @QueryParam(PROPERTIES) String properties
    );

    @GET
    @Path("/broadcaster/{broadcaster}")
    ScheduleResult listBroadcaster(
            @PathParam(BROADCASTER) String broadcaster,
            @QueryParam(GUIDE_DAY) String guideDay,
            @QueryParam(START) String start,
            @QueryParam(STOP) String stop,
            @QueryParam(PROPERTIES) String properties,
            @QueryParam(SORT) @DefaultValue(ASC) String sort,
            @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/broadcaster/{broadcaster}/now")
    ApiScheduleEvent nowForBroadcaster(
            @PathParam(BROADCASTER) String broadcaster,
            @QueryParam(PROPERTIES) String properties
    );

    @GET
    @Path("/broadcaster/{broadcaster}/next")
    ApiScheduleEvent nextForBroadcaster(
            @PathParam(BROADCASTER) String broadcaster,
            @QueryParam(PROPERTIES) String properties
    );

    @GET
    @Path("/channel/{channel}")
    ScheduleResult listChannel(
            @PathParam(CHANNEL) String channel,
            @QueryParam(GUIDE_DAY) String guideDay,
            @QueryParam(START) String start,
            @QueryParam(STOP) String stop,
            @QueryParam(PROPERTIES) String properties,
            @QueryParam(SORT) @DefaultValue(ASC) String sort,
            @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/channel/{channel}/now")
    ApiScheduleEvent nowForChannel(
            @PathParam(CHANNEL) String channel,
            @QueryParam(PROPERTIES) String properties
    );

    @GET
    @Path("/channel/{channel}/next")
    ApiScheduleEvent nextForChannel(
            @PathParam(CHANNEL) String channel,
            @QueryParam(PROPERTIES) String properties
    );

    @GET
    @Path("/net/{net}")
    ScheduleResult listNet(
            @PathParam(NET) String net,
            @QueryParam(GUIDE_DAY) String guideDay,
            @QueryParam(START) String start,
            @QueryParam(STOP) String stop,
            @QueryParam(PROPERTIES) String properties,
            @QueryParam(SORT) @DefaultValue(ASC) String sort,
            @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @GET
    @Path("/net/{net}/now")
    ApiScheduleEvent nowForNet(
            @PathParam(NET) String net,
            @QueryParam(PROPERTIES) String properties
    );

    @GET
    @Path("/net/{net}/next")
    ApiScheduleEvent nextForNet(
            @PathParam(NET) String net,
            @QueryParam(PROPERTIES) String properties
    );

    @POST
    ScheduleSearchResult find(
            @Valid MediaForm form,
            @QueryParam(PROFILE) String profile,
            @QueryParam(PROPERTIES) String properties,
            @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );
}
