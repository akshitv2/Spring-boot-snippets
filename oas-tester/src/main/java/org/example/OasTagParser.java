package org.example;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.OasModels.*;

public class OasTagParser {

    // Updated regex to capture any word containing alphanumeric characters and underscores
    private static final Pattern ATTR_PATTERN = Pattern.compile("<!ATTR(?:\\s*:\\s*([A-Za-z0-9_]+))?>");
    private static final Pattern MASKED_PATTERN = Pattern.compile("<!MASKED>");
    private static final Pattern RENDER_PATTERN = Pattern.compile("<!RENDER>");

    public Map<String, OasFields> parseOas(String openApiFilePath) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read(openApiFilePath, null, options);

        Map<String, OasFields> endpointMap = new HashMap<>();

        if (openAPI == null || openAPI.getPaths() == null) {
            return endpointMap;
        }

        openAPI.getPaths().forEach((pathKey, pathItem) -> {
            Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();

            operations.forEach((method, operation) -> {
                String mapKey = method.name() + pathKey;
                OasFields oasFields = new OasFields();

                // 1. Process Parameters (URI & Query)
                if (operation.getParameters() != null) {
                    for (Parameter parameter : operation.getParameters()) {
                        String in = parameter.getIn();
                        String name = parameter.getName();
                        String jaywayPath = "$." + name;

                        String description = parameter.getDescription();
                        extractAndBucketParam(description, jaywayPath, in, oasFields);
                    }
                }

                // 2. Process Request Body
                if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                    MediaType mediaType = operation.getRequestBody().getContent().get("application/json");
                    if (mediaType != null && mediaType.getSchema() != null) {
                        parseSchemaFields(openAPI, mediaType.getSchema(), "$", oasFields, "request");
                    }
                }

                // 3. Process Response Bodies
                if (operation.getResponses() != null) {
                    operation.getResponses().forEach((statusCode, response) -> {
                        if (response.getContent() != null) {
                            MediaType mediaType = response.getContent().get("application/json");
                            if (mediaType != null && mediaType.getSchema() != null) {
                                parseSchemaFields(openAPI, mediaType.getSchema(), "$", oasFields, "response");
                            }
                        }
                    });
                }

                endpointMap.put(mapKey, oasFields);
            });
        });

        return endpointMap;
    }

    private void parseSchemaFields(OpenAPI openAPI, Schema<?> schema, String currentPath, OasFields oasFields, String location) {
        if (schema == null) return;

        if (schema.get$ref() != null) {
            schema = resolveReference(openAPI, schema.get$ref());
            if (schema == null) return;
        }

        String description = schema.getDescription();
        if (description != null) {
            extractAndBucketBody(description, currentPath, oasFields, location);
        }

        if (schema.getProperties() != null) {
            schema.getProperties().forEach((propName, propSchema) -> {
                String nextPath = currentPath.equals("$") ? "$." + propName : currentPath + "." + propName;
                parseSchemaFields(openAPI, (Schema<?>) propSchema, nextPath, oasFields, location);
            });
        } else if (schema.getItems() != null) {
            String nextPath = currentPath + "[*]";
            parseSchemaFields(openAPI, schema.getItems(), nextPath, oasFields, location);
        }
    }

    private Schema<?> resolveReference(OpenAPI openAPI, String ref) {
        if (ref == null) return null;
        String schemaName = ref.substring(ref.lastIndexOf("/") + 1);
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            return openAPI.getComponents().getSchemas().get(schemaName);
        }
        return null;
    }

    private void extractAndBucketParam(String text, String jaywayPath, String in, OasFields oasFields) {
        if (text == null) return;

        if (MASKED_PATTERN.matcher(text).find()) {
            addParamToLocation(oasFields.fieldSpecificBuckets.get("MASKED"), in, new FieldDetails(jaywayPath, null));
        }
        if (RENDER_PATTERN.matcher(text).find()) {
            addParamToLocation(oasFields.fieldSpecificBuckets.get("RENDER"), in, new FieldDetails(jaywayPath, null));
        }
        Matcher attrMatcher = ATTR_PATTERN.matcher(text);
        if (attrMatcher.find()) {
            // Assigns the dynamically matched word group, defaults to "Normal" if bare tag used
            String type = attrMatcher.group(1) != null ? attrMatcher.group(1) : "Normal";
            addParamToLocation(oasFields.fieldSpecificBuckets.get("ATTR"), in, new FieldDetails(jaywayPath, type));
        }
    }

    private void extractAndBucketBody(String text, String jaywayPath, OasFields oasFields, String location) {
        if (text == null) return;

        if (MASKED_PATTERN.matcher(text).find()) {
            addBodyToLocation(oasFields.fieldSpecificBuckets.get("MASKED"), location, new FieldDetails(jaywayPath, null));
        }
        if (RENDER_PATTERN.matcher(text).find()) {
            addBodyToLocation(oasFields.fieldSpecificBuckets.get("RENDER"), location, new FieldDetails(jaywayPath, null));
        }
        Matcher attrMatcher = ATTR_PATTERN.matcher(text);
        if (attrMatcher.find()) {
            String type = attrMatcher.group(1) != null ? attrMatcher.group(1) : "Normal";
            addBodyToLocation(oasFields.fieldSpecificBuckets.get("ATTR"), location, new FieldDetails(jaywayPath, type));
        }
    }

    private void addParamToLocation(BucketData bucket, String in, FieldDetails details) {
        if ("path".equalsIgnoreCase(in)) {
            bucket.uriParam.add(details);
        } else if ("query".equalsIgnoreCase(in)) {
            bucket.queryParam.add(details);
        }
    }

    private void addBodyToLocation(BucketData bucket, String location, FieldDetails details) {
        if ("request".equalsIgnoreCase(location)) {
            bucket.requestFields.add(details);
        } else if ("response".equalsIgnoreCase(location)) {
            bucket.responseFields.add(details);
        }
    }
}