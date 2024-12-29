package io.github.plantaest.citron.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UserGroupsResponse(
        Query query
) {

    public record Query(
            List<User> users
    ) {

        public record User(
                @JsonProperty("userid")
                long userId,
                String name,
                List<String> groups
        ) {}

    }

}
