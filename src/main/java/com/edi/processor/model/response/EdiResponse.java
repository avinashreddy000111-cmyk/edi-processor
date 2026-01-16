package com.edi.processor.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EdiResponse {

    @JsonProperty("response")
    private List<ResponseItem> response;

    public EdiResponse() {
    }

    public EdiResponse(List<ResponseItem> response) {
        this.response = response;
    }

    // Private constructor for builder
    private EdiResponse(Builder builder) {
        this.response = builder.response;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ResponseItem> getResponse() {
        return response;
    }

    public void setResponse(List<ResponseItem> response) {
        this.response = response;
    }

    // Builder class
    public static class Builder {
        private List<ResponseItem> response;

        public Builder response(List<ResponseItem> response) {
            this.response = response;
            return this;
        }

        public EdiResponse build() {
            return new EdiResponse(this);
        }
    }
}
