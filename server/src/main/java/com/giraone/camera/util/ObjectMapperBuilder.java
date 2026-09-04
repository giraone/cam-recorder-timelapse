package com.giraone.camera.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.util.StdDateFormat;

import java.math.BigDecimal;

public class ObjectMapperBuilder {

    // Hide
    private ObjectMapperBuilder() {
    }

    public static ObjectMapper build() {
        return build(false, false, false, false);
    }

    public static ObjectMapper build(boolean snakeCase, boolean bigNumberAsString, boolean sortKeys, boolean indent) {

        final JsonMapper.Builder builder = JsonMapper.builder()
            .findAndAddModules()
            // Be tolerant in reading
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // Restore pre-jackson-3 default: null JSON values are silently coerced to primitive defaults
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            // Do not write empty stuff (applies to both bean properties and Map/Collection entries)
            .changeDefaultPropertyInclusion(inclusion -> inclusion
                .withValueInclusion(JsonInclude.Include.NON_NULL)
                .withContentInclusion(JsonInclude.Include.NON_NULL))
            // Date/Date-Time settings
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            // StdDateFormat is ISO8601 since jackson 2.9 - we force +05:00 instead of +0500
            .defaultDateFormat(new StdDateFormat().withColonInTimeZone(true))
            // Enum settings
            .configure(EnumFeature.WRITE_ENUMS_USING_TO_STRING, true)
            .configure(EnumFeature.READ_ENUMS_USING_TO_STRING, true);

        if (bigNumberAsString) {
            // Write Big-Decimal as Strings
            final SimpleModule module = new SimpleModule();
            module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
            builder.addModule(module);
        }
        if (snakeCase) {
            builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        }
        if (sortKeys) {
            builder.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        }
        if (indent) {
            builder.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return builder.build();
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
