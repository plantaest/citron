package io.github.plantaest.citron.client;

import io.github.plantaest.citron.config.CitronConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class WikiActionClientManager {

    @Inject
    CitronConfig citronConfig;

    private final Map<String, WikiActionClient> clients = new ConcurrentHashMap<>();

    public WikiActionClient getClient(String serverName) {
        return clients.computeIfAbsent(serverName, this::createClient);
    }

    private WikiActionClient createClient(String serverName) {
        String baseUri = "https://" + serverName + "/w/api.php";
        AtomicReference<WikiActionClient> wikiActionClientRef = new AtomicReference<>();

        WikiActionClient wikiActionClient = RestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUri))
                .register(CookieManager.class)
                .register(new CsrfTokenInterceptor(wikiActionClientRef))
                .build(WikiActionClient.class);

        wikiActionClientRef.set(wikiActionClient);

        // Login
        String loginToken = wikiActionClient.getLoginToken().at("/query/tokens/logintoken").asText();
        wikiActionClient.login(citronConfig.bot().username(), citronConfig.bot().password(), loginToken);

        return wikiActionClient;
    }

}
