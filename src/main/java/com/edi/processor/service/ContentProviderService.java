package com.edi.processor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
public class ContentProviderService {

    private static final Logger log = LoggerFactory.getLogger(ContentProviderService.class);

    private Properties contentProperties;

    private static final String PROPERTIES_FILE = "response-content.properties";
    private static final String DEFAULT_CONTENT = "Default response content";

    @PostConstruct
    public void init() {
        contentProperties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                contentProperties.load(input);
                log.info("Loaded {} properties from {}", contentProperties.size(), PROPERTIES_FILE);
            } else {
                log.warn("Properties file '{}' not found. Using default values.", PROPERTIES_FILE);
            }
        } catch (IOException e) {
            log.error("Error loading properties file: {}", e.getMessage(), e);
        }
    }

    /**
     * Get content for GETSCHEMA transaction
     */
    public String getfileWithOrdTypeContent(String transactionType,String orderType,String format,String responseType) {
        String key = transactionType.toUpperCase() + "." + responseType.toUpperCase() + "." + orderType.toUpperCase() + "." + format.toUpperCase() + ".content";
        String content = contentProperties.getProperty(key);
        
        if (content != null) {
            return content;
        }
        
        return contentProperties.getProperty("DEFAULT.content", DEFAULT_CONTENT);
    }
    
    /**
     * Get content for GETSCHEMA transaction
     */
    public String getGetSchemaContent(String responseType) {
        String key = "GETSCHEMA." + responseType.toUpperCase() + ".content";
        String content = contentProperties.getProperty(key);
        
        if (content != null) {
            return content;
        }
        
        return contentProperties.getProperty("DEFAULT.content", DEFAULT_CONTENT);
    }

    /**
     * Get content for ORDER ACK response
     */
    public String getOrderAckContent(String orderType) {
        String key = "ORDER." + orderType.toUpperCase() + ".ACK.content";
        String content = contentProperties.getProperty(key);
        
        if (content != null) {
            return content;
        }
        
        return contentProperties.getProperty("DEFAULT.content", DEFAULT_CONTENT);
    }

    /**
     * Get ACK content for ORDER SHIPCONFIRM dual response
     */
    public String getOrderShipconfirmAckContent(String orderType) {
        String key = "ORDER." + orderType.toUpperCase() + ".SHIPCONFIRM.ACK.content";
        String content = contentProperties.getProperty(key);
        
        if (content != null) {
            return content;
        }
        
        return contentProperties.getProperty("DEFAULT.content", DEFAULT_CONTENT);
    }

    /**
     * Get SHIPCONFIRM content for ORDER transaction
     */
    public String getOrderShipconfirmContent(String orderType) {
        String key = "ORDER." + orderType.toUpperCase() + ".SHIPCONFIRM.content";
        String content = contentProperties.getProperty(key);
        
        if (content != null) {
            return content;
        }
        
        return contentProperties.getProperty("DEFAULT.content", DEFAULT_CONTENT);
    }

    /**
     * Get content for ASN ACK response
     */
    public String getAsnAckContent() {
        String content = contentProperties.getProperty("ASN.ACK.content");
        
        if (content != null) {
            return content;
        }
        
        return contentProperties.getProperty("DEFAULT.content", DEFAULT_CONTENT);
    }

    /**
     * Get ACK content for ASN RECEIPT dual response
     */
    public String getAsnReceiptAckContent() {
        String content = contentProperties.getProperty("ASN.RECEIPT.ACK.content");
        
        if (content != null) {
            return content;
        }
        
        return contentProperties.getProperty("DEFAULT.content", DEFAULT_CONTENT);
    }

    /**
     * Get RECEIPT content for ASN transaction
     */
    public String getAsnReceiptContent() {
        String content = contentProperties.getProperty("ASN.RECEIPT.content");
        
        if (content != null) {
            return content;
        }
        
        return contentProperties.getProperty("DEFAULT.content", DEFAULT_CONTENT);
    }

    /**
     * Get content for ITEM ACK response
     */
    public String getItemAckContent() {
        String content = contentProperties.getProperty("ITEM.ACK.content");
        
        if (content != null) {
            return content;
        }
        
        return contentProperties.getProperty("DEFAULT.content", DEFAULT_CONTENT);
    }

    /**
     * Get error content
     */
    public String getErrorContent() {
        return contentProperties.getProperty("ERROR.content", "Unable to process request");
    }

    /**
     * Get validation error content
     */
    public String getValidationErrorContent() {
        return contentProperties.getProperty("VALIDATION.ERROR.content", "Invalid value provided");
    }
}
