package com.edi.processor.service;

import com.edi.processor.exception.EdiProcessingException;
import com.edi.processor.model.request.EdiRequest;
import com.edi.processor.model.request.RequestDetails;
import com.edi.processor.model.response.EdiResponse;
import com.edi.processor.model.response.ResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class EdiProcessorService {

    private static final Logger log = LoggerFactory.getLogger(EdiProcessorService.class);

    private static final String MIME_TYPE_EDI = "application/edi-x12";
    private static final String MIME_TYPE_JSON = "application/json";
    private static final String MIME_TYPE_TEXT = "plain/text";
    private static final String SUCCESS_MESSAGE = "File processed successfully";
    private static final String ERROR_MESSAGE = "unable to process request";

    // Response Type Constants
    private static final String RESPONSE_TYPE_GETSCHEMA = "GETSCHEMA";
    private static final String RESPONSE_TYPE_ACK = "ACK";
    private static final String RESPONSE_TYPE_SHIPCONFIRM = "SHIPCONFIRM";
    private static final String RESPONSE_TYPE_RECEIPT = "RECEIPT";

    // Transaction Type Constants
    private static final String TRANSACTION_TYPE_ORDER = "ORDER";
    private static final String TRANSACTION_TYPE_ASN = "ASN";
    private static final String TRANSACTION_TYPE_SHIPCONFIRM = "SHIPCONFIRM";
    private static final String TRANSACTION_TYPE_ERROR_RESPONSE = "ERRORRESPONSE";
    private static final String TRANSACTION_TYPE_ERROR_TIMEOUT = "ERRORTIMEOUT";

    // Order Type Constants
    private static final String ORDER_TYPE_LTL = "LTL";
    private static final String ORDER_TYPE_PARCEL = "PARCEL";

    /**
     * Process the EDI request and return appropriate response based on business logic
     */
    public EdiResponse processRequest(EdiRequest ediRequest) {
        validateRequest(ediRequest);

        RequestDetails request = ediRequest.getRequest();
        String transactionType = normalizeString(request.getTransactionType());
        String orderType = normalizeString(request.getOrderType());
        String format = normalizeString(request.getFormat());
        String responseType = normalizeString(request.getResponseType());
        String uuid = ediRequest.getUuid();

        log.info("Processing request - UUID: {}, TransactionType: {}, OrderType: {}, Format: {}, ResponseType: {}",
                uuid, transactionType, orderType, format, responseType);

        try {
            return processBusinessLogic(transactionType, orderType, format, responseType, uuid);
        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage(), e);
            throw new EdiProcessingException(e.getMessage(), transactionType, responseType, format, uuid, e);
        }
    }

    /**
     * Check if response should be suppressed (for errortimeout case)
     */
    public boolean shouldSuppressResponse(EdiRequest ediRequest) {
        if (ediRequest == null || ediRequest.getRequest() == null) {
            return false;
        }
        String transactionType = normalizeString(ediRequest.getRequest().getTransactionType());
        return TRANSACTION_TYPE_ERROR_TIMEOUT.equalsIgnoreCase(transactionType);
    }

    private EdiResponse processBusinessLogic(String transactionType, String orderType,
                                              String format, String responseType, String uuid) {

        // CHECK error transaction types FIRST (highest priority)
        if (TRANSACTION_TYPE_ERROR_RESPONSE.equalsIgnoreCase(transactionType)) {
            log.info("Processing errorresponse transaction");
            return buildErrorResponse(transactionType, responseType, format, uuid);
        }

        // errortimeout - handled in controller, but return error if it reaches here
        if (TRANSACTION_TYPE_ERROR_TIMEOUT.equalsIgnoreCase(transactionType)) {
            log.info("Processing errortimeout transaction");
            return buildErrorResponse(transactionType, responseType, format, uuid);
        }

        // GETSCHEMA Response Type Logic
        if (RESPONSE_TYPE_GETSCHEMA.equalsIgnoreCase(responseType)) {
            return handleGetSchemaResponse(transactionType, orderType, format, uuid);
        }

        // Transaction Type: ORDER
        if (TRANSACTION_TYPE_ORDER.equalsIgnoreCase(transactionType)) {
            return handleOrderTransaction(orderType, format, responseType, uuid);
        }

        // Transaction Type: ASN
        if (TRANSACTION_TYPE_ASN.equalsIgnoreCase(transactionType)) {
            return handleAsnTransaction(orderType, format, responseType, uuid);
        }

        // Default: Unknown transaction type - return error
        return buildErrorResponse(transactionType, responseType, format, uuid);
    }

    private EdiResponse handleGetSchemaResponse(String transactionType, String orderType,
                                                 String format, String uuid) {
        String mimeType = determineMimeType(format);
        String fileExtension = determineFileExtension(format);

        // ORDER Transaction Type
        if (TRANSACTION_TYPE_ORDER.equalsIgnoreCase(transactionType)) {
            if (ORDER_TYPE_LTL.equalsIgnoreCase(orderType) || ORDER_TYPE_PARCEL.equalsIgnoreCase(orderType)) {
                String filename = transactionType + "_" + orderType + "_Schema_" + uuid + "." + fileExtension;
                return buildSuccessResponse(filename, "This is the content for " + orderType +" "+transactionType+" Schema", mimeType);
            }
        }

        // ASN Transaction Type
        if (TRANSACTION_TYPE_ASN.equalsIgnoreCase(transactionType)) {
            String filename = transactionType + "_Schema_" + uuid + "." + fileExtension;
            return buildSuccessResponse(filename, "This is the content for "+transactionType+" Schema", mimeType);
        }

        // SHIPCONFIRM Transaction Type
        if (TRANSACTION_TYPE_SHIPCONFIRM.equalsIgnoreCase(transactionType)) {
            String filename = transactionType + "_" + orderType + "_Schema_" + uuid + "." + fileExtension;
            return buildSuccessResponse(filename, "This is the content for "+transactionType+" Schema", mimeType);
        }

        // Default for GETSCHEMA with unknown transaction type
        String filename = transactionType + "_Schema_" + uuid + "." + fileExtension;
        return buildSuccessResponse(filename, "This is the content for "+transactionType+" Schema", mimeType);
    }

    private EdiResponse handleOrderTransaction(String orderType, String format,
                                                String responseType, String uuid) {
        String mimeType = determineMimeType(format);
        String fileExtension = determineFileExtension(format);

        // Response Type: ACK - returns single response
        if (RESPONSE_TYPE_ACK.equalsIgnoreCase(responseType)) {
            String filename = TRANSACTION_TYPE_ORDER + "_" +orderType+"_"+ responseType + "_" + uuid + "." + fileExtension;
            return buildSuccessResponse(filename, "This is the content for " + orderType + " " + TRANSACTION_TYPE_ORDER + " " + responseType, mimeType);
        }

        // Response Type: SHIPCONFIRM - returns TWO response items
        if (RESPONSE_TYPE_SHIPCONFIRM.equalsIgnoreCase(responseType)) {
            List<ResponseItem> items = new ArrayList<>();

            // First response item - ACK
            String ackFilename = TRANSACTION_TYPE_ORDER + "_" + orderType + "_ACK_" + uuid + "." + fileExtension;
            items.add(ResponseItem.builder()
                    .success(true)
                    .filename(ackFilename)
                    .content("This is the content for " + orderType + " " + TRANSACTION_TYPE_ORDER + " ACK")
                    .mimeType(mimeType)
                    .message(SUCCESS_MESSAGE)
                    .build());

            // Second response item - SHIPCONFIRM
            String respFilename = TRANSACTION_TYPE_ORDER + "_" + orderType + "_" + responseType + "_" + uuid + "." + fileExtension;
            items.add(ResponseItem.builder()
                    .success(true)
                    .filename(respFilename)
                    .content("This is the content for " + orderType + " " + TRANSACTION_TYPE_ORDER + " " + responseType)
                    .mimeType(mimeType)
                    .message(SUCCESS_MESSAGE)
                    .build());

            return EdiResponse.builder().response(items).build();
        }

        // Default for ORDER - returns single response
        String filename = TRANSACTION_TYPE_ORDER + "_" + responseType + "_" + uuid + "." + fileExtension;
        return buildSuccessResponse(filename, "This is the content for " + orderType + " " + TRANSACTION_TYPE_ORDER + " " + responseType, mimeType);
    }

    private EdiResponse handleAsnTransaction(String orderType, String format,
                                              String responseType, String uuid) {
        String mimeType = determineMimeType(format);
        String fileExtension = determineFileExtension(format);

        // Response Type: ACK - returns single response
        if (RESPONSE_TYPE_ACK.equalsIgnoreCase(responseType)) {
            String filename = TRANSACTION_TYPE_ASN + "_" + responseType + "_" + uuid + "." + fileExtension;
            return buildSuccessResponse(filename, "This is the content for " + TRANSACTION_TYPE_ASN + " " + responseType, mimeType);
        }

        // Response Type: RECEIPT - returns TWO response items
        if (RESPONSE_TYPE_RECEIPT.equalsIgnoreCase(responseType)) {
            List<ResponseItem> items = new ArrayList<>();

            // First response item - ACK
            String ackFilename = TRANSACTION_TYPE_ASN + "_ACK_" + uuid + "." + fileExtension;
            items.add(ResponseItem.builder()
                    .success(true)
                    .filename(ackFilename)
                    .content("This is the content for " + TRANSACTION_TYPE_ASN + " ACK")
                    .mimeType(mimeType)
                    .message(SUCCESS_MESSAGE)
                    .build());

            // Second response item - RECEIPT
            String respFilename = TRANSACTION_TYPE_ASN + "_" + responseType + "_" + uuid + "." + fileExtension;
            items.add(ResponseItem.builder()
                    .success(true)
                    .filename(respFilename)
                    .content("This is the content for " + TRANSACTION_TYPE_ASN + " " + responseType)
                    .mimeType(mimeType)
                    .message(SUCCESS_MESSAGE)
                    .build());

            return EdiResponse.builder().response(items).build();
        }

        // Default for ASN - returns single response
        String filename = TRANSACTION_TYPE_ASN + "_" + responseType + "_" + uuid + "." + fileExtension;
        return buildSuccessResponse(filename, "This is the content for " + TRANSACTION_TYPE_ASN + " " + responseType, mimeType);
    }

    private EdiResponse buildSuccessResponse(String filename, String content, String mimeType) {
        ResponseItem item = ResponseItem.builder()
                .success(true)
                .filename(filename)
                .content(content)
                .mimeType(mimeType)
                .message(SUCCESS_MESSAGE)
                .build();

        return EdiResponse.builder()
                .response(Collections.singletonList(item))
                .build();
    }

    private EdiResponse buildErrorResponse(String transactionType, String responseType,
                                            String format, String uuid) {
        String fileExtension = determineFileExtension(format);
        String filename = transactionType + "_" + responseType + "_ERROR_" + uuid + "." + fileExtension;

        ResponseItem item = ResponseItem.builder()
                .success(false)
                .filename(filename)
                .content(ERROR_MESSAGE)
                .mimeType(MIME_TYPE_TEXT)
                .message(ERROR_MESSAGE)
                .build();

        return EdiResponse.builder()
                .response(Collections.singletonList(item))
                .build();
    }

    private String determineMimeType(String format) {
        if (format == null) {
            return MIME_TYPE_TEXT;
        }
        return switch (format.toUpperCase()) {
            case "EDI" -> MIME_TYPE_EDI;
            case "JSON" -> MIME_TYPE_JSON;
            default -> MIME_TYPE_TEXT;
        };
    }

    private String determineFileExtension(String format) {
        if (format == null) {
            return "txt";
        }
        return switch (format.toUpperCase()) {
            case "EDI" -> "edi";
            case "JSON" -> "json";
            default -> format.toLowerCase();
        };
    }

    private String normalizeString(String value) {
        return value != null ? value.trim() : null;
    }

    private void validateRequest(EdiRequest ediRequest) {
        if (ediRequest == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (ediRequest.getUuid() == null || ediRequest.getUuid().trim().isEmpty()) {
            throw new IllegalArgumentException("UUID is required");
        }
        if (ediRequest.getRequest() == null) {
            throw new IllegalArgumentException("Request details cannot be null");
        }
        if (ediRequest.getRequest().getTransactionType() == null ||
                ediRequest.getRequest().getTransactionType().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction Type is required");
        }
    }
}