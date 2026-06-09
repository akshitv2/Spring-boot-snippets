package org.example.oastest.config;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Load the specification file
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createForSpecificationUrl("oas/openapi.yaml")
                .build();

        // Add the interceptor to the registry
        registry.addInterceptor( new OpenApiValidationInterceptor(validator));
    }
}