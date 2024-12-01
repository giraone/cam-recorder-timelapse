package com.giraone.camera.service.api.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.giraone.camera.service.api.CameraSettings;

import java.io.IOException;

public class CustomEnumDeserializerLevel extends StdDeserializer<CameraSettings.Level> {

    protected CustomEnumDeserializerLevel() {
        super(String.class);
    }

    @Override
    public CameraSettings.Level deserialize(JsonParser jsonParser, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int value = Integer.parseInt(node.asText());
        for (CameraSettings.Level e : CameraSettings.Level.ALL) {
            if (e.ordinal() == value + 2) {
                return e;
            }
        }
        return CameraSettings.Level.M;
    }
}
