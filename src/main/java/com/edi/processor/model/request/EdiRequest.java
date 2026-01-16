package com.edi.processor.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EdiRequest {

    @JsonProperty("UUID")
    private String uuid;

    @JsonProperty("Request")
    private RequestDetails request;

    public EdiRequest() {
    }

    public EdiRequest(String uuid, RequestDetails request) {
        this.uuid = uuid;
        this.request = request;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public RequestDetails getRequest() {
        return request;
    }

    public void setRequest(RequestDetails request) {
        this.request = request;
    }
}
