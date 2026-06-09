package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class OagTagParserTest {

    @Test
    void parserTest() throws JsonProcessingException {
        OasTagParser tagParser = new OasTagParser();
//        Map<String, OasModels.OasFields> oasFieldsMap =  tagParser.parseOas("F:\\Git\\Spring-boot-snippets\\oas-tester\\oas\\openapi.yaml");
        Map<String, OasModels.OasFields> oasFieldsMap =  tagParser.parseOas("F:\\Git\\Spring-boot-snippets\\oas-tester\\oas\\complex.yaml");

        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println(
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(oasFieldsMap)
        );

    }
}
