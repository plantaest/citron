package io.github.plantaest.citron.client;

import io.github.plantaest.citron.dto.OprGetPageRankResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(baseUri = "https://openpagerank.com/api/v1.0")
@ClientHeaderParam(name = "API-OPR", value = "${citron.classifier.open-page-rank-api-key}")
public interface OpenPageRankClient {

    @GET
    @Path("/getPageRank")
    OprGetPageRankResponse getPageRank(@QueryParam("domains[]") List<String> domains);

}
