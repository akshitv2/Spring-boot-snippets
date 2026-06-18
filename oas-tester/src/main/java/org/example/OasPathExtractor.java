package org.example;

import com.atlassian.oai.validator.model.SimpleRequest;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OasPathExtractor {

    public static Optional<String> getGenericPath(OpenAPI openApi, String runtimeUri) {
        // 1. Get the underlying OpenAPI model from the validator
        Paths paths = openApi.getPaths();

        if (paths == null) {
            return Optional.empty();
        }

        // 2. Use Atlassian's path matcher regex logic or standard iteration
        // to find which template fits the concrete URI
        return paths.keySet().stream()
                .filter(templatePath -> matchesUri(templatePath, runtimeUri))
                .findFirst();
    }

    private static boolean matchesUrix(String templatePath, String runtimeUri) {
        // Convert OAS format "/user/{userid}/data" to Regex "/user/[^/]+/data"
        String regex = templatePath.replaceAll("\\{[^}]+}", "[^/]+");
        return runtimeUri.matches(regex);
    }

    private static boolean matchesUri(String templatePath, String runtimeUri) {
        if (templatePath == null || runtimeUri == null) {
            return false;
        }

        // 1. Strip query parameters from runtime URI (e.g., /users/123?raw=true -> /users/123)
        String runtimePath = runtimeUri.split("\\?")[0];

        // 2. Normalize leading slashes
        if (!templatePath.startsWith("/")) {
            templatePath = "/" + templatePath;
        }
        if (!runtimePath.startsWith("/")) {
            runtimePath = "/" + runtimePath;
        }

        // 3. Normalize trailing slashes (except for root "/")
        if (templatePath.endsWith("/") && templatePath.length() > 1) {
            templatePath = templatePath.substring(0, templatePath.length() - 1);
        }
        if (runtimePath.endsWith("/") && runtimePath.length() > 1) {
            runtimePath = runtimePath.substring(0, runtimePath.length() - 1);
        }

        // 4. Convert OAS template "/users/{userId}" to regex "^/users/[^/]+$"
        String regex = "^" + templatePath.replaceAll("\\{[^}]+}", "[^/]+") + "$";

        return runtimePath.matches(regex);
    }

    public static void main(String[] args) {
        String runtimeUri = "/users/12345";
        ParseOptions options = new ParseOptions();
        options.setResolve(true); // Resolves $ref pointers within the spec
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("F:\\Git\\Spring-boot-snippets\\oas-tester\\oas\\openapi.yaml", null, options);
        Optional<String> genericPath = getGenericPath(result.getOpenAPI(), runtimeUri);
        // Outputs: Generic Path: /user/{userid}/data
        genericPath.ifPresent(path -> System.out.println("Generic Path: " + path));
    }
    private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)=?([^&]*)");
    private static void populateQueryParams(SimpleRequest.Builder builder, String query) {
        if (query == null || query.isEmpty()) {
            return;
        }

        Matcher matcher = QUERY_PATTERN.matcher(query);
        while (matcher.find()) {
            try {
                String key = URLDecoder.decode(matcher.group(1), "UTF-8");
                String value = URLDecoder.decode(matcher.group(2), "UTF-8");

                builder.withQueryParam(key, value);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 encoding not supported", e);
            }
        }
    }
}