package io.github.plantaest.citron.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.NewCookie;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CookieManager implements ClientRequestFilter, ClientResponseFilter {

    private final Map<String, NewCookie> cookies = new ConcurrentHashMap<>();

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (!cookies.isEmpty()) {
            String cookieHeader = cookies.values().stream()
                    .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                    .collect(Collectors.joining("; "));
            requestContext.getHeaders().putSingle("Cookie", cookieHeader);
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        Map<String, NewCookie> responseCookies = responseContext.getCookies();
        cookies.putAll(responseCookies);
    }

}
