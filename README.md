# EDI Processor Spring Boot Application

A Spring Boot backend application that processes EDI (Electronic Data Interchange) requests and returns appropriate responses based on transaction types, order types, and response types.

## Project Structure

```
edi-processor/
├── pom.xml
├── src/main/java/com/edi/processor/
│   ├── EdiProcessorApplication.java          # Main application entry point
│   ├── controller/
│   │   └── EdiController.java                # REST API controller
│   ├── service/
│   │   └── EdiProcessorService.java          # Business logic service
│   ├── model/
│   │   ├── request/
│   │   │   ├── EdiRequest.java               # Main request wrapper
│   │   │   └── RequestDetails.java           # Request details model
│   │   └── response/
│   │       ├── EdiResponse.java              # Response wrapper
│   │       └── ResponseItem.java             # Individual response item
│   └── exception/
│       ├── EdiProcessingException.java       # Custom exception
│       └── GlobalExceptionHandler.java       # Global exception handler
└── src/main/resources/
    └── application.properties                # Application configuration
```

## Requirements

- Java 17+
- Maven 3.6+

## Building the Application

```bash
cd edi-processor
mvn clean package
```

## Running the Application

```bash
mvn spring-boot:run
```

Or run the JAR file:

```bash
java -jar target/edi-processor-1.0.0.jar
```

The application starts on port 8080 by default.

## API Endpoints

### POST /api/v1/edi/process

Process an EDI request.

**Request:**

```json
{
  "UUID": "123456788",
  "Request": {
    "TRANSACTION TYPE": "ORDER",
    "ORDER TYPE": "LTL",
    "FORMAT": "EDI",
    "RESPONSE TYPE": "ACK",
    "Input File": "SVNBKjAwKiAgICAgICAgICAqMDAq..."
  }
}
```

### GET /api/v1/edi/health

Health check endpoint.

## Business Logic Summary

### When RESPONSE TYPE = "GETSCHEMA"

| Transaction Type | Order Type | Filename Pattern |
|-----------------|------------|------------------|
| ORDER | LTL | `ORDER_LTL_Schema_{uuid}.{format}` |
| ORDER | PARCEL | `ORDER_PARCEL_Schema_{uuid}.{format}` |
| ASN | - | `ASN_Schema_{uuid}.{format}` |
| SHIPCONFIRM | * | `SHIPCONFIRM_{orderType}_Schema_{uuid}.{format}` |

### When TRANSACTION TYPE = "ORDER"

| Response Type | Order Type | Response |
|--------------|------------|----------|
| ACK | - | Single response: `ORDER_ACK_{uuid}.{format}` |
| * | shipconfirm | Two responses: ACK + response type |

### When TRANSACTION TYPE = "ASN"

| Response Type | Order Type | Response |
|--------------|------------|----------|
| ACK | - | Single response: `ASN_ACK_{uuid}.{format}` |
| * | receipt | Two responses: ACK + response type |

### Special Cases

- **errorresponse**: Returns error response with `success: false`
- **errortimeout**: Returns HTTP 204 No Content (no response body)

### MIME Types

| Format | MIME Type |
|--------|-----------|
| EDI | `application/edi-x12` |
| JSON | `application/json` |
| Other | `plain/text` |

## Example cURL Commands

### Order with ACK Response

```bash
curl -X POST http://localhost:8080/api/v1/edi/process \
  -H "Content-Type: application/json" \
  -d '{
    "UUID": "123456788",
    "Request": {
      "TRANSACTION TYPE": "ORDER",
      "ORDER TYPE": "LTL",
      "FORMAT": "EDI",
      "RESPONSE TYPE": "ACK",
      "Input File": "SVNBKjAwKiAgICAgICAgICAqMDAq..."
    }
  }'
```

### Get Schema Request

```bash
curl -X POST http://localhost:8080/api/v1/edi/process \
  -H "Content-Type: application/json" \
  -d '{
    "UUID": "987654321",
    "Request": {
      "TRANSACTION TYPE": "ORDER",
      "ORDER TYPE": "PARCEL",
      "FORMAT": "JSON",
      "RESPONSE TYPE": "GETSCHEMA",
      "Input File": ""
    }
  }'
```

## Error Handling

All exceptions are handled globally and return a standardized error response:

```json
{
  "response": [
    {
      "success": false,
      "filename": "TRANSACTION_RESPONSE_ERROR_uuid.format",
      "content": "unable to process request",
      "mimeType": "plain/text",
      "message": "unable to process request"
    }
  ]
}
```
