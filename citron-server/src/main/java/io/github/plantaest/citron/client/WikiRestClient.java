package io.github.plantaest.citron.client;

import io.github.plantaest.citron.dto.CompareRevisionsResponse;
import io.github.plantaest.citron.dto.WikiPageResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@ClientHeaderParam(name = "User-Agent", value = "Citron/${citron.version} (plantaest@gmail.com)")
public interface WikiRestClient {

    @GET
    @Path("/revision/{from}/compare/{to}")
    @Produces(MediaType.APPLICATION_JSON)
    CompareRevisionsResponse compareRevisions(@PathParam("from") long from, @PathParam("to") long to);

    @GET
    @Path("/page/{title}")
    @Produces(MediaType.APPLICATION_JSON)
    WikiPageResponse getPage(@PathParam("title") String title);

}