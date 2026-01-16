package com.edi.processor.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestDetails {

    @JsonProperty("TRANSACTION TYPE")
    @JsonAlias({"TRANSACTION_TYPE", "TRANSACTIONTYPE", "transactionType"})
    private String transactionType;

    @JsonProperty("ORDER TYPE")
    @JsonAlias({"ORDER_TYPE", "ORDERTYPE", "orderType"})
    private String orderType;

    @JsonProperty("FORMAT")
    @JsonAlias({"format"})
    private String format;

    @JsonProperty("RESPONSE TYPE")
    @JsonAlias({"RESPONSE_TYPE", "RESPONSETYPE", "responseType"})
    private String responseType;

    @JsonProperty("Input File")
    @JsonAlias({"INPUT_FILE", "inputFile", "InputFile"})
    private String inputFile;

    // ... rest of constructors, getters, setters remain the same

    public RequestDetails(String transactionType, String orderType, String format, 
                          String responseType, String inputFile) {
        this.transactionType = transactionType;
        this.orderType = orderType;
        this.format = format;
        this.responseType = responseType;
        this.inputFile = inputFile;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }
}
