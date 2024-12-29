package io.github.plantaest.citron.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public record Change(
        String $schema,
        Meta meta,
        long id,
        String type,
        int namespace,
        String title,
        @JsonProperty("title_url")
        URI titleUrl,
        String comment,
        long timestamp,
        String user,
        boolean bot,
        @JsonProperty("notify_url")
        URI notifyUrl,
        boolean minor,
        Length length,
        Revision revision,
        @JsonProperty("server_url")
        String serverUrl,
        @JsonProperty("server_name")
        String serverName,
        @JsonProperty("server_script_path")
        String serverScriptPath,
        String wiki,
        @JsonProperty("parsedcomment")
        String parsedComment
) {

    public record Meta(
            URI uri,
            @JsonProperty("request_id")
            String requestId,
            String id,
            String dt,
            String domain,
            String stream,
            String topic,
            int partition,
            long offset
    ) {}

    public record Length(
            int old,
            @JsonProperty("new")
            int _new
    ) {}

    public record Revision(
            long old,
            @JsonProperty("new")
            long _new
    ) {}

}
