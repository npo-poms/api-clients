package nl.vpro.rs.media;

import java.io.IOException;
import java.io.InputStream;
import java.util.SortedSet;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.providers.multipart.XopWithMultipartRelated;
import org.jboss.resteasy.plugins.providers.multipart.MultipartConstants;

import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.search.MediaList;
import nl.vpro.domain.media.search.MediaListItem;
import nl.vpro.domain.media.update.*;

/**
 * @author Michiel Meeuwissen
 * @since 3.3
 */
@Path("/media")
@Consumes({MediaType.APPLICATION_XML, MultipartConstants.MULTIPART_RELATED})
@Produces(MediaType.APPLICATION_XML + ";charset=UTF-8")
public interface MediaRestController {

    @POST
    @Path("find")
    MediaList<MediaListItem> find(
        InputStream formInputstream,
        @QueryParam("writable") @DefaultValue("false") boolean writable
    ) throws IOException;

    @GET
    @Path("{entity:(media|program|group|segment)}/{id}")
    MediaUpdate getMedia(
        @PathParam("entity") final String entity,
        @PathParam("id") final String id
    ) throws IOException;

    @GET
    @Path("{entity:(media|program|group|segment)}/{id}/full")
    MediaObject getFullMediaObject(
        @PathParam("entity") final String entity,
        @PathParam("id") final String id
    ) throws IOException;

    @POST
    @Path("{entity:(media|segment|program|group)}")
    @Produces("text/plain")
    Response update(
        @PathParam("entity") final String entity,
        @XopWithMultipartRelated MediaUpdate update,
        @QueryParam("errors") String errors,
        @QueryParam("lookupcrid") @DefaultValue("true") Boolean lookupcrid
    ) throws IOException;

    @GET
    @Path("{entity:(media|program|group|segment)}/{id}/members")
    MediaUpdateList<MemberUpdate> getGroupMembers(
        @PathParam("entity") final String entity,
        @PathParam("id") final String id,
        @QueryParam("offset") @DefaultValue("0") final Long offset,
        @QueryParam("max") @DefaultValue("20") final Integer max,
        @QueryParam("order") @DefaultValue("ASC") final String order
    ) throws IOException;

    @GET
    @Path("group/{id}/episodes")
    MediaUpdateList<MemberUpdate> getGroupEpisodes(
        @PathParam("id") final String id,
        @QueryParam("offset") @DefaultValue("0") final Long offset,
        @QueryParam("max") @DefaultValue("10") final Integer max,
        @QueryParam("order") @DefaultValue("ASC") final String order

    ) throws IOException;

    @POST
    @Path("{entity:(media|program|group|segment)}/{id}/location")
    @Produces("text/plain")
    Response addLocation(
        @PathParam("entity") final String entity,
        LocationUpdate location,
        @PathParam("id") final String id,
        @QueryParam("errors") String errors
    );

    @DELETE
    @Path("{entity:(media|program|group|segment)}/{id}/location/{locationId}")
    @Produces("text/plain")
    Response removeLocation(
        @PathParam("entity") final String entity,
        @PathParam("id") final String id,
        @PathParam("locationId") final String locationId
    );

    @GET
    @Path("{entity:(media|program|group|segment)}/{id}/locations")
    SortedSet<LocationUpdate> getLocations(
        @PathParam("entity") final String entity,
        @PathParam("id") final String id
    ) throws IOException;


    @POST
    @Path("{entity:(media|program|group|segment)}/{id}/image")
    @Produces("text/plain")
    Response addImage(
        ImageUpdate imageUpdate,
        @PathParam("entity") final String entity,
        @PathParam("id") final String id,
        @QueryParam("errors") String errors
    );
    @GET
    @Path("{entity:(media|program|group|segment)}/{id}/memberOfs")
    MediaUpdateList<MemberRefUpdate> getMemberOfs(
        @PathParam("entity") final String entity,
        @PathParam("id") final String id
    ) throws IOException;

    @POST
    @Path("{entity:(media|program|group|segment)}/{id}/memberOf")
    Response addMemberOf(
        MemberRefUpdate memberRefUpdate,
        @PathParam("entity") final String entity,
        @PathParam("id") final String id
    ) throws IOException;

    @GET
    @Path("program/{id}/episodeOfs")
    MediaUpdateList<MemberRefUpdate> getEpisodeOfs(
        @PathParam("id") final String id
    ) throws IOException;

    @POST
    @Path("program/{id}/episodeOf")
    Response addEpisodeOf(
        MemberRefUpdate memberRefUpdate,
        @PathParam("id") final String id
    ) throws IOException;

}
