package com.giraone.streaming.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;

public class ObjectMapperBuilder {

    // Hide
    private ObjectMapperBuilder() {
    }

    public static ObjectMapper build() {
        return build(false, false, false, false);
    }

    public static ObjectMapper build(boolean snakeCase, boolean bigNumberAsString, boolean sortKeys, boolean indent) {

        final ObjectMapper mapper = new ObjectMapper();
        // Be tolerant in reading
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Do not write empty stuff
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Date/Date-Time settings
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // StdDateFormat is ISO8601 since jackson 2.9 - we force +05:00 instead of +0500
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        // Enum settings
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);

        if (bigNumberAsString) {
            // Write Big-Decimal as Strings
            final SimpleModule module = new SimpleModule();
            module.addSerializer(BigDecimal.class, new ToStringSerializer());
            mapper.registerModule(module);
        }
        if (snakeCase) {
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        }
        if (sortKeys) {
            mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        }
        if (indent) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return mapper;
    }

    public static ObjectMapper build(boolean snakeCase) {
        return build(snakeCase, false, false, false);
    }

    public static ObjectMapper build(boolean snakeCase, boolean bigNumberAsString) {
        return build(snakeCase, bigNumberAsString, false, false);
    }

    public static ObjectMapper build(boolean snakeCase, boolean bigNumberAsString, boolean sortKeys) {
        return build(snakeCase, bigNumberAsString, sortKeys, false);
    }
}
