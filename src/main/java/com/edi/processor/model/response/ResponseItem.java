package com.edi.processor.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseItem {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("content")
    private String content;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("message")
    private String message;

    public ResponseItem() {
    }

    public ResponseItem(boolean success, String filename, String content, String mimeType, String message) {
        this.success = success;
        this.filename = filename;
        this.content = content;
        this.mimeType = mimeType;
        this.message = message;
    }

    // Private constructor for builder
    private ResponseItem(Builder builder) {
        this.success = builder.success;
        this.filename = builder.filename;
        this.content = builder.content;
        this.mimeType = builder.mimeType;
        this.message = builder.message;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Builder class
    public static class Builder {
        private boolean success;
        private String filename;
        private String content;
        private String mimeType;
        private String message;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public ResponseItem build() {
            return new ResponseItem(this);
        }
    }
}
