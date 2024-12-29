package io.github.plantaest.citron.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.plantaest.citron.dto.UserGroupsResponse;
import io.quarkus.rest.client.reactive.ClientFormParam;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

import java.util.Map;

@RegisterRestClient
@ClientHeaderParam(name = "User-Agent", value = "Citron/${citron.version} (plantaest@gmail.com)")
@ClientQueryParam(name = "format", value = "json")
@ClientQueryParam(name = "formatversion", value = "2")
public interface WikiActionClient {

    @GET
    @ClientQueryParam(name = "action", value = "query")
    @ClientQueryParam(name = "list", value = "users")
    @ClientQueryParam(name = "usprop", value = "groups")
    UserGroupsResponse getUserGroups(@QueryParam("ususers") String username);

    // Ref: https://www.mediawiki.org/wiki/API:Tokens#Response
    @GET
    @ClientQueryParam(name = "action", value = "query")
    @ClientQueryParam(name = "meta", value = "tokens")
    @ClientQueryParam(name = "type", value = "login")
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode getLoginToken();

    @POST
    @ClientFormParam(name = "action", value = "login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode login(@FormParam("lgname") String username,
                   @FormParam("lgpassword") String password,
                   @FormParam("lgtoken") String loginToken);

    @GET
    @ClientQueryParam(name = "action", value = "query")
    @ClientQueryParam(name = "meta", value = "tokens")
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode getCsrfToken();

    @POST
    @ClientHeaderParam(name = "X-Interceptor", value = "csrf")
    @ClientFormParam(name = "action", value = "edit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode edit(@RestForm Map<String, String> params);

    @POST
    @ClientFormParam(name = "action", value = "purge")
    JsonNode purge(@FormParam("titles") String titles);

}
