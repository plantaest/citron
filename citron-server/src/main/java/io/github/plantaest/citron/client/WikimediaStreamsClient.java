package io.github.plantaest.citron.client;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://stream.wikimedia.org/v2/stream")
public interface WikimediaStreamsClient {

    @GET
    @Path("/recentchange")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Multi<String> getRawRecentChanges();

}
