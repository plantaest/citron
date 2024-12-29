package io.github.plantaest.citron.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/")
@Tag(name = "Index")
public class IndexRoute {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response index() {
        return Response.ok("The homepage is under construction.").build();
    }

    @GET
    @Path("/favicon.ico")
    public Response favicon() {
        return Response.noContent().build();
    }

}
