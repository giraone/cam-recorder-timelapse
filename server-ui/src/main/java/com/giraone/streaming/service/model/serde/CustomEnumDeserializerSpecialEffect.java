package com.giraone.streaming.service.model.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.giraone.streaming.service.model.CameraSettings;

import java.io.IOException;

public class CustomEnumDeserializerSpecialEffect extends StdDeserializer<CameraSettings.SpecialEffect> {

    protected CustomEnumDeserializerSpecialEffect() {
        super(String.class);
    }

    @Override
    public CameraSettings.SpecialEffect deserialize(JsonParser jsonParser, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int value = Integer.parseInt(node.asText());
        for (CameraSettings.SpecialEffect e : CameraSettings.SpecialEffect.ALL) {
            if (e.ordinal() == value) {
                return e;
            }
        }
        return null;
    }
}
