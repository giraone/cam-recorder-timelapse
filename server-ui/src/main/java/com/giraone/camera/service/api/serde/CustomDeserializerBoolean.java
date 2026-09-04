package com.giraone.camera.service.api.serde;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class CustomDeserializerBoolean extends StdDeserializer<Boolean> {

    protected CustomDeserializerBoolean() {
        super(String.class);
    }

    @Override
    public Boolean deserialize(JsonParser jsonParser, DeserializationContext ctxt) {
        final JsonNode node = jsonParser.readValueAsTree();
        final int value = node.asInt(0);
        return value > 0;
    }
}
