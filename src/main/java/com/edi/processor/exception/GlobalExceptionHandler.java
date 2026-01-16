package com.edi.processor.exception;

import com.edi.processor.model.response.EdiResponse;
import com.edi.processor.model.response.ResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Collections;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERROR_MESSAGE = "unable to process request";
    private static final String ERROR_MIME_TYPE = "plain/text";

    @ExceptionHandler(EdiProcessingException.class)
    public ResponseEntity<EdiResponse> handleEdiProcessingException(
            EdiProcessingException ex, WebRequest request) {
        
        log.error("EDI Processing Exception: {}", ex.getMessage(), ex);

        String filename = buildErrorFilename(
                ex.getTransactionType(),
                ex.getResponseType(),
                ex.getUuid(),
                ex.getFormat()
        );

        ResponseItem errorItem = ResponseItem.builder()
                .success(false)
                .filename(filename)
                .content(ERROR_MESSAGE)
                .mimeType(ERROR_MIME_TYPE)
                .message(ERROR_MESSAGE)
                .build();

        EdiResponse response = EdiResponse.builder()
                .response(Collections.singletonList(errorItem))
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<EdiResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.error("Illegal Argument Exception: {}", ex.getMessage(), ex);

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String filename = "UNKNOWN_UNKNOWN_ERROR_" + uniqueId + ".txt";

        ResponseItem errorItem = ResponseItem.builder()
                .success(false)
                .filename(filename)
                .content(ERROR_MESSAGE)
                .mimeType(ERROR_MIME_TYPE)
                .message(ex.getMessage() != null ? ex.getMessage() : ERROR_MESSAGE)
                .build();

        EdiResponse response = EdiResponse.builder()
                .response(Collections.singletonList(errorItem))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<EdiResponse> handleNullPointerException(
            NullPointerException ex, WebRequest request) {
        
        log.error("Null Pointer Exception: {}", ex.getMessage(), ex);

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String filename = "UNKNOWN_UNKNOWN_ERROR_" + uniqueId + ".txt";

        ResponseItem errorItem = ResponseItem.builder()
                .success(false)
                .filename(filename)
                .content(ERROR_MESSAGE)
                .mimeType(ERROR_MIME_TYPE)
                .message("Missing required field in request")
                .build();

        EdiResponse response = EdiResponse.builder()
                .response(Collections.singletonList(errorItem))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EdiResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected Exception: {}", ex.getMessage(), ex);

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String filename = "UNKNOWN_UNKNOWN_ERROR_" + uniqueId + ".txt";

        ResponseItem errorItem = ResponseItem.builder()
                .success(false)
                .filename(filename)
                .content(ERROR_MESSAGE)
                .mimeType(ERROR_MIME_TYPE)
                .message(ERROR_MESSAGE)
                .build();

        EdiResponse response = EdiResponse.builder()
                .response(Collections.singletonList(errorItem))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String buildErrorFilename(String transactionType, String responseType, 
                                       String uuid, String format) {
        String txnType = transactionType != null ? transactionType : "UNKNOWN";
        String respType = responseType != null ? responseType : "UNKNOWN";
        String uniqueId = uuid != null ? uuid : UUID.randomUUID().toString().substring(0, 8);
        String fileFormat = format != null ? format.toLowerCase() : "txt";
        
        return txnType + "_" + respType + "_ERROR_" + uniqueId + "." + fileFormat;
    }
}
