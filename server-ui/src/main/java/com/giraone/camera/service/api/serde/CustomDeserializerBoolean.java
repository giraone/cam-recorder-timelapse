package com.giraone.camera.service.api.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class CustomDeserializerBoolean extends StdDeserializer<Boolean> {

    protected CustomDeserializerBoolean() {
        super(String.class);
    }

    @Override
    public Boolean deserialize(JsonParser jsonParser, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int value = node.asInt(0);
        return value > 0;
    }
}
