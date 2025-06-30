package io.github.plantaest.citron.client;

import io.github.plantaest.citron.dto.CompareRevisionsResponse;
import io.github.plantaest.citron.dto.WikiPageResponse;
import io.github.plantaest.citron.dto.WikiRevisionResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@ClientHeaderParam(name = "User-Agent", value = "Citron/${citron.version} (plantaest@gmail.com)")
public interface WikiRestClient {

    @GET
    @Path("/revision/{from}/compare/{to}")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 5, delay = 800, jitter = 200)
    CompareRevisionsResponse compareRevisions(@PathParam("from") long from, @PathParam("to") long to);

    @GET
    @Path("/page/{title}")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 5, delay = 800, jitter = 200)
    WikiPageResponse getPage(@PathParam("title") String title);

    @GET
    @Path("/revision/{revisionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 5, delay = 800, jitter = 200)
    WikiRevisionResponse getRevision(@PathParam("revisionId") long revisionId);

}
