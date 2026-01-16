package com.edi.processor.controller;

import com.edi.processor.model.request.EdiRequest;
import com.edi.processor.model.response.EdiResponse;
import com.edi.processor.service.EdiProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/edi")
public class EdiController {

    private static final Logger log = LoggerFactory.getLogger(EdiController.class);

    private final EdiProcessorService ediProcessorService;

    public EdiController(EdiProcessorService ediProcessorService) {
        this.ediProcessorService = ediProcessorService;
    }

    /**
     * Process EDI request and return appropriate response
     * 
     * @param ediRequest The incoming EDI request
     * @return ResponseEntity containing EdiResponse or no content for timeout scenarios
     */
    @PostMapping(value = "/process", 
                 consumes = MediaType.APPLICATION_JSON_VALUE, 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EdiResponse> processEdiRequest(@RequestBody EdiRequest ediRequest) {
        
        log.info("Received EDI request with UUID: {}", 
                ediRequest != null ? ediRequest.getUuid() : "null");

        // Check if this is an errortimeout transaction - don't send response
        if (ediProcessorService.shouldSuppressResponse(ediRequest)) {
            log.info("Transaction type is errortimeout - suppressing response for UUID: {}", 
                    ediRequest.getUuid());
            // Return no content for errortimeout scenario
            return ResponseEntity.noContent().build();
        }

        // Process the request and return response
        EdiResponse response = ediProcessorService.processRequest(ediRequest);
        
        log.info("Successfully processed EDI request for UUID: {}", ediRequest.getUuid());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     * 
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("EDI Processor Service is running");
    }
}
