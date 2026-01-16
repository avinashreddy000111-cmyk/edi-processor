package com.edi.processor.exception;

public class EdiProcessingException extends RuntimeException {

    private final String transactionType;
    private final String responseType;
    private final String format;
    private final String uuid;

    public EdiProcessingException(String message, String transactionType, String responseType, 
                                   String format, String uuid) {
        super(message);
        this.transactionType = transactionType;
        this.responseType = responseType;
        this.format = format;
        this.uuid = uuid;
    }

    public EdiProcessingException(String message, String transactionType, String responseType, 
                                   String format, String uuid, Throwable cause) {
        super(message, cause);
        this.transactionType = transactionType;
        this.responseType = responseType;
        this.format = format;
        this.uuid = uuid;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getFormat() {
        return format;
    }

    public String getUuid() {
        return uuid;
    }
}
