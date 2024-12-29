package io.github.plantaest.citron.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.concurrent.atomic.AtomicReference;

public record CsrfTokenInterceptor(
        AtomicReference<WikiActionClient> wikiActionClientRef
) implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        Object interceptorHeader = requestContext.getHeaders().getFirst("X-Interceptor");
        Object contentTypeHeader = requestContext.getHeaders().getFirst("Content-Type");

        if ("csrf".equals(interceptorHeader) &&
                MediaType.APPLICATION_FORM_URLENCODED.equals(contentTypeHeader)) {
            // Get CSRF token
            String csrfToken = wikiActionClientRef.get().getCsrfToken()
                    .at("/query/tokens/csrftoken").asText();

            // Add to form
            Object entity = requestContext.getEntity();
            @SuppressWarnings("unchecked")
            MultivaluedMap<String, String> formParams = entity instanceof MultivaluedMap
                    ? (MultivaluedMap<String, String>) entity
                    : new MultivaluedHashMap<>();

            formParams.add("token", csrfToken);
            requestContext.setEntity(formParams, null, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

            // Remove X-Interceptor header
            requestContext.getHeaders().remove("X-Interceptor");
        }
    }

}
