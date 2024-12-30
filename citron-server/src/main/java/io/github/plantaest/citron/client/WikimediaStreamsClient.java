package io.github.plantaest.citron.client;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.SseEvent;
import org.jboss.resteasy.reactive.client.SseEventFilter;

import java.util.function.Predicate;

@RegisterRestClient(baseUri = "https://stream.wikimedia.org/v2/stream")
@ClientHeaderParam(name = "User-Agent", value = "Citron/${citron.version} (plantaest@gmail.com)")
public interface WikimediaStreamsClient {

    @GET
    @Path("/recentchange")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseEventFilter(WikimediaStreamsFilter.class)
    Multi<SseEvent<String>> getRawRecentChanges(@HeaderParam("Last-Event-ID") String lastEventId);

    class WikimediaStreamsFilter implements Predicate<SseEvent<String>> {
        @Override
        public boolean test(SseEvent<String> event) {
            return event.data() != null && !event.data().isBlank();
        }
    }

}
