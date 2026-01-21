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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class EdiProcessorService {

    private static final Logger log = LoggerFactory.getLogger(EdiProcessorService.class);

    private final ContentProviderService contentProvider;

    private static final String MIME_TYPE_EDI = "application/edi-x12";
    private static final String MIME_TYPE_JSON = "application/json";
    private static final String MIME_TYPE_TEXT = "plain/text";
    private static final String SUCCESS_MESSAGE = "File processed successfully";
    private static final String ERROR_MESSAGE = "unable to process request";
    private static final String INVALID_VALUE_MESSAGE = "Invalid value provided";

    // Transaction Type Constants
    private static final String TRANSACTION_TYPE_GETSCHEMA = "GETSCHEMA";
    private static final String TRANSACTION_TYPE_ORDER = "ORDER";
    private static final String TRANSACTION_TYPE_ASN = "ASN";
    private static final String TRANSACTION_TYPE_ITEM = "ITEM";
    private static final String TRANSACTION_TYPE_ERROR_RESPONSE = "ERRORRESPONSE";
    private static final String TRANSACTION_TYPE_ERROR_TIMEOUT = "ERRORTIMEOUT";

    // Valid Transaction Types List
    private static final List<String> VALID_TRANSACTION_TYPES = Arrays.asList(
            TRANSACTION_TYPE_GETSCHEMA,
            TRANSACTION_TYPE_ORDER,
            TRANSACTION_TYPE_ASN,
            TRANSACTION_TYPE_ITEM,
            TRANSACTION_TYPE_ERROR_RESPONSE,
            TRANSACTION_TYPE_ERROR_TIMEOUT
    );

    // Response Type Constants
    private static final String RESPONSE_TYPE_ACK = "ACK";
    private static final String RESPONSE_TYPE_ASN = "ASN";
    private static final String RESPONSE_TYPE_ITEM = "ITEM";
    private static final String RESPONSE_TYPE_ORDER = "ORDER";
    private static final String RESPONSE_TYPE_SHIPCONFIRM = "SHIPCONFIRM";
    private static final String RESPONSE_TYPE_RECEIPT = "RECEIPT";

    // Valid Response Types for each Transaction Type
    private static final List<String> GETSCHEMA_RESPONSE_TYPES = Arrays.asList(
            RESPONSE_TYPE_ASN, RESPONSE_TYPE_ITEM, RESPONSE_TYPE_ORDER,RESPONSE_TYPE_SHIPCONFIRM, RESPONSE_TYPE_RECEIPT
    );

    private static final List<String> ORDER_RESPONSE_TYPES = Arrays.asList(
            RESPONSE_TYPE_ACK, RESPONSE_TYPE_SHIPCONFIRM
    );

    private static final List<String> ASN_RESPONSE_TYPES = Arrays.asList(
            RESPONSE_TYPE_ACK, RESPONSE_TYPE_RECEIPT
    );

    private static final List<String> ITEM_RESPONSE_TYPES = Arrays.asList(
            RESPONSE_TYPE_ACK
    );

    // Order Type Constants
    private static final String ORDER_TYPE_LTL = "LTL";
    private static final String ORDER_TYPE_PARCEL = "PARCEL";

    // Valid Order Types List
    private static final List<String> VALID_ORDER_TYPES = Arrays.asList(
            ORDER_TYPE_LTL,
            ORDER_TYPE_PARCEL
    );

    // Format Constants
    private static final String FORMAT_EDI = "EDI";
    private static final String FORMAT_JSON = "JSON";

    // Valid Formats List
    private static final List<String> VALID_FORMATS = Arrays.asList(
            FORMAT_EDI,
            FORMAT_JSON
    );

    // Constructor injection
    public EdiProcessorService(ContentProviderService contentProvider) {
        this.contentProvider = contentProvider;
    }

    /**
     * Process the EDI request
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

        // Validate all field values
        String validationError = validateFieldValues(transactionType, orderType, format, responseType);
        if (validationError != null) {
            log.error("Validation failed: {}", validationError);
            return buildValidationErrorResponse(transactionType, responseType, format, uuid, validationError);
        }

        try {
            return processBusinessLogic(transactionType, orderType, format, responseType, uuid);
        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage(), e);
            throw new EdiProcessingException(e.getMessage(), transactionType, responseType, format, uuid, e);
        }
    }

    /**
     * Validate all field values
     */
    private String validateFieldValues(String transactionType, String orderType,
                                        String format, String responseType) {

        // Validate Transaction Type
        if (transactionType == null || !isValidTransactionType(transactionType)) {
            return "Invalid TRANSACTION TYPE: '" + transactionType + "'. Valid values are: " + VALID_TRANSACTION_TYPES;
        }

        // Validate Format (only if provided)
        if (format != null && !format.isEmpty() && !isValidFormat(format)) {
            return "Invalid FORMAT: '" + format + "'. Valid values are: " + VALID_FORMATS;
        }

        // Skip response type validation for error transactions
        if (TRANSACTION_TYPE_ERROR_RESPONSE.equalsIgnoreCase(transactionType) ||
            TRANSACTION_TYPE_ERROR_TIMEOUT.equalsIgnoreCase(transactionType)) {
            return null;
        }

        // Validate Response Type is provided
        if (responseType == null || responseType.isEmpty()) {
            return "RESPONSE TYPE is required.";
        }

        // Validate Response Type based on Transaction Type
        String responseTypeError = validateResponseTypeForTransaction(transactionType, responseType);
        if (responseTypeError != null) {
            return responseTypeError;
        }

        // Validate Order Type - ONLY required when Transaction Type = ORDER
        if (TRANSACTION_TYPE_ORDER.equalsIgnoreCase(transactionType)) {
            if (orderType == null || orderType.isEmpty()) {
                return "ORDER TYPE is required when TRANSACTION TYPE is 'ORDER'. Valid values are: " + VALID_ORDER_TYPES;
            }
            if (!isValidOrderType(orderType)) {
                return "Invalid ORDER TYPE: '" + orderType + "'. Valid values are: " + VALID_ORDER_TYPES;
            }
        }

        return null;
    }

    /**
     * Validate Response Type based on Transaction Type
     */
    private String validateResponseTypeForTransaction(String transactionType, String responseType) {
        
        if (TRANSACTION_TYPE_GETSCHEMA.equalsIgnoreCase(transactionType)) {
            if (!GETSCHEMA_RESPONSE_TYPES.stream().anyMatch(r -> r.equalsIgnoreCase(responseType))) {
                return "Invalid RESPONSE TYPE: '" + responseType + "' for TRANSACTION TYPE 'GETSCHEMA'. Valid values are: " + GETSCHEMA_RESPONSE_TYPES;
            }
        } else if (TRANSACTION_TYPE_ORDER.equalsIgnoreCase(transactionType)) {
            if (!ORDER_RESPONSE_TYPES.stream().anyMatch(r -> r.equalsIgnoreCase(responseType))) {
                return "Invalid RESPONSE TYPE: '" + responseType + "' for TRANSACTION TYPE 'ORDER'. Valid values are: " + ORDER_RESPONSE_TYPES;
            }
        } else if (TRANSACTION_TYPE_ASN.equalsIgnoreCase(transactionType)) {
            if (!ASN_RESPONSE_TYPES.stream().anyMatch(r -> r.equalsIgnoreCase(responseType))) {
                return "Invalid RESPONSE TYPE: '" + responseType + "' for TRANSACTION TYPE 'ASN'. Valid values are: " + ASN_RESPONSE_TYPES;
            }
        } else if (TRANSACTION_TYPE_ITEM.equalsIgnoreCase(transactionType)) {
            if (!ITEM_RESPONSE_TYPES.stream().anyMatch(r -> r.equalsIgnoreCase(responseType))) {
                return "Invalid RESPONSE TYPE: '" + responseType + "' for TRANSACTION TYPE 'ITEM'. Valid values are: " + ITEM_RESPONSE_TYPES;
            }
        }

        return null;
    }

    private boolean isValidTransactionType(String transactionType) {
        return VALID_TRANSACTION_TYPES.stream()
                .anyMatch(valid -> valid.equalsIgnoreCase(transactionType));
    }

    private boolean isValidOrderType(String orderType) {
        return VALID_ORDER_TYPES.stream()
                .anyMatch(valid -> valid.equalsIgnoreCase(orderType));
    }

    private boolean isValidFormat(String format) {
        return VALID_FORMATS.stream()
                .anyMatch(valid -> valid.equalsIgnoreCase(format));
    }

    public boolean shouldSuppressResponse(EdiRequest ediRequest) {
        if (ediRequest == null || ediRequest.getRequest() == null) {
            return false;
        }
        String transactionType = normalizeString(ediRequest.getRequest().getTransactionType());
        return TRANSACTION_TYPE_ERROR_TIMEOUT.equalsIgnoreCase(transactionType);
    }

    private EdiResponse processBusinessLogic(String transactionType, String orderType,
                                              String format, String responseType, String uuid) {

        // ERRORRESPONSE - return error
        if (TRANSACTION_TYPE_ERROR_RESPONSE.equalsIgnoreCase(transactionType)) {
            log.info("Processing ERRORRESPONSE transaction");
            return buildErrorResponse(transactionType, responseType, format, uuid);
        }

        // ERRORTIMEOUT - handled in controller
        if (TRANSACTION_TYPE_ERROR_TIMEOUT.equalsIgnoreCase(transactionType)) {
            log.info("Processing ERRORTIMEOUT transaction");
            return buildErrorResponse(transactionType, responseType, format, uuid);
        }

        // GETSCHEMA Transaction - always returns 1 response
        if (TRANSACTION_TYPE_GETSCHEMA.equalsIgnoreCase(transactionType)) {
            return handleGetSchemaTransaction(transactionType,orderType,format, responseType, uuid);
        }

        // ORDER Transaction
        if (TRANSACTION_TYPE_ORDER.equalsIgnoreCase(transactionType)) {
            return handleOrderTransaction(transactionType,orderType, format, responseType, uuid);
        }

        // ASN Transaction
        if (TRANSACTION_TYPE_ASN.equalsIgnoreCase(transactionType)) {
            return handleAsnTransaction(transactionType,format, responseType, uuid);
        }

        // ITEM Transaction - always returns 1 response
        if (TRANSACTION_TYPE_ITEM.equalsIgnoreCase(transactionType)) {
            return handleItemTransaction(transactionType,format, responseType, uuid);
        }

        // Default - return error
        return buildErrorResponse(transactionType, responseType, format, uuid);
    }

    /**
     * Handle GETSCHEMA Transaction - Returns 1 response
     * Valid Response Types: ASN, ITEM, SHIPCONFIRM, RECEIPT
     */
    private EdiResponse handleGetSchemaTransaction(String transactionType, String orderType, String format, String responseType, String uuid) {
        String mimeType = determineMimeType(format);
        String fileExtension = determineFileExtension(format);
        String filename = transactionType + "_" + responseType + "_" + uuid + "." + fileExtension;
        String content;
        if (orderType != null && !orderType.trim().isEmpty()) {
            filename = transactionType + "_" + responseType + "_" + orderType + "_" + uuid + "." + fileExtension;
            content = contentProvider.getfileWithOrdTypeContent(transactionType,responseType,orderType,format);
        }else{
            content = contentProvider.getfileWithoutOrdTypeContent(transactionType,responseType,format);
        }
        return buildSuccessResponse(filename, content, mimeType);
    }

    /**
     * Handle ORDER Transaction
     * Valid Response Types: ACK (1 response), SHIPCONFIRM (2 responses)
     */
    private EdiResponse handleOrderTransaction(String transactionType,String orderType, String format,
                                                String responseType, String uuid) {
        String mimeType = determineMimeType(format);
        String fileExtension = determineFileExtension(format);

        // SHIPCONFIRM - returns 2 responses (ACK + SHIPCONFIRM)
        if (RESPONSE_TYPE_SHIPCONFIRM.equalsIgnoreCase(responseType)) {
            List<ResponseItem> items = new ArrayList<>();

            // First response - ACK
            String ackFilename = transactionType + "_" + orderType + "_" + responseType + "_" + uuid + "." + fileExtension;
            String ackContent = contentProvider.getfileWithOrdTypeContent(transactionType,"ACK",orderType,format);
            items.add(ResponseItem.builder()
                    .success(true)
                    .filename(ackFilename)
                    .content(ackContent)
                    .mimeType(mimeType)
                    .message(SUCCESS_MESSAGE)
                    .build());

            // Second response - SHIPCONFIRM
            String shipFilename = transactionType + "_" + orderType + "_" + responseType + "_" + uuid + "." + fileExtension;
            String shipContent = contentProvider.getfileWithOrdTypeContent(transactionType,responseType,orderType,format);
            items.add(ResponseItem.builder()
                    .success(true)
                    .filename(shipFilename)
                    .content(shipContent)
                    .mimeType(mimeType)
                    .message(SUCCESS_MESSAGE)
                    .build());

            return EdiResponse.builder().response(items).build();
        }

        // ACK - returns 1 response
        String filename = transactionType + "_" + orderType + "_" + responseType + "_" + uuid + "." + fileExtension;
        String content = contentProvider.getfileWithOrdTypeContent(transactionType,"ACK",orderType,format);
        return buildSuccessResponse(filename, content, mimeType);
    }

    /**
     * Handle ASN Transaction
     * Valid Response Types: ACK (1 response), RECEIPT (2 responses)
     */
    private EdiResponse handleAsnTransaction(String format, String responseType, String uuid) {
        String mimeType = determineMimeType(format);
        String fileExtension = determineFileExtension(format);

        // RECEIPT - returns 2 responses (ACK + RECEIPT)
        if (RESPONSE_TYPE_RECEIPT.equalsIgnoreCase(responseType)) {
            List<ResponseItem> items = new ArrayList<>();

            // First response - ACK
            String ackFilename = "ASN_ACK_" + uuid + "." + fileExtension;
            String ackContent = contentProvider.getAsnReceiptAckContent();
            items.add(ResponseItem.builder()
                    .success(true)
                    .filename(ackFilename)
                    .content(ackContent)
                    .mimeType(mimeType)
                    .message(SUCCESS_MESSAGE)
                    .build());

            // Second response - RECEIPT
            String receiptFilename = "ASN_RECEIPT_" + uuid + "." + fileExtension;
            String receiptContent = contentProvider.getAsnReceiptContent();
            items.add(ResponseItem.builder()
                    .success(true)
                    .filename(receiptFilename)
                    .content(receiptContent)
                    .mimeType(mimeType)
                    .message(SUCCESS_MESSAGE)
                    .build());

            return EdiResponse.builder().response(items).build();
        }

        // ACK - returns 1 response
        String filename = "ASN_ACK_" + uuid + "." + fileExtension;
        String content = contentProvider.getAsnAckContent();
        return buildSuccessResponse(filename, content, mimeType);
    }

    /**
     * Handle ITEM Transaction - Returns 1 response
     * Valid Response Types: ACK
     */
    private EdiResponse handleItemTransaction(String format, String responseType, String uuid) {
        String mimeType = determineMimeType(format);
        String fileExtension = determineFileExtension(format);

        String filename = "ITEM_ACK_" + uuid + "." + fileExtension;
        String content = contentProvider.getItemAckContent();

        return buildSuccessResponse(filename, content, mimeType);
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
        String respType = responseType != null ? responseType : "ERROR";
        //String filename = transactionType + "_" + respType + "_ERROR_" + uuid + "." + fileExtension;
        String filename = " ";
        String errMsg = ERROR_MESSAGE + " for " + format + " " + responseType + " for " + transactionType +" request : UUID:-"||uuid;

        ResponseItem item = ResponseItem.builder()
                .success(false)
                .filename(filename)
                .content(contentProvider.getErrorContent())
                .mimeType(MIME_TYPE_TEXT)
                .message(errMsg)
                .build();

        return EdiResponse.builder()
                .response(Collections.singletonList(item))
                .build();
    }

    private EdiResponse buildValidationErrorResponse(String transactionType, String responseType,
                                                      String format, String uuid, String errorMessage) {
        String fileExtension = format != null ? format.toLowerCase() : "txt";
        String txnType = transactionType != null ? transactionType : "UNKNOWN";
        String respType = responseType != null ? responseType : "UNKNOWN";
        String filename = txnType + "_" + respType + "_VALIDATION_ERROR_" + uuid + "." + fileExtension;

        ResponseItem item = ResponseItem.builder()
                .success(false)
                .filename(filename)
                .content(errorMessage)
                .mimeType(MIME_TYPE_TEXT)
                .message(INVALID_VALUE_MESSAGE + ": " + errorMessage)
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
