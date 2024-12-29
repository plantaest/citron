package io.github.plantaest.citron.client;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class WikiRestClientManager {

    private final Map<String, WikiRestClient> clients = new ConcurrentHashMap<>();

    public WikiRestClient getClient(String serverName) {
        return clients.computeIfAbsent(serverName, this::createClient);
    }

    private WikiRestClient createClient(String serverName) {
        String baseUri = "https://" + serverName + "/w/rest.php/v1";
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUri))
                .followRedirects(true)
                .build(WikiRestClient.class);
    }

}
