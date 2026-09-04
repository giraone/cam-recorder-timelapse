package com.giraone.camera.service.api.serde;

import com.giraone.camera.service.api.CameraSettings;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

public class CustomEnumDeserializerLevel extends StdDeserializer<CameraSettings.Level> {

    protected CustomEnumDeserializerLevel() {
        super(String.class);
    }

    @Override
    public CameraSettings.Level deserialize(JsonParser jsonParser, DeserializationContext ctxt) {
        final JsonNode node = jsonParser.readValueAsTree();
        final int value = Integer.parseInt(node.asString());
        for (CameraSettings.Level e : CameraSettings.Level.ALL) {
            if (e.ordinal() == value + 2) {
                return e;
            }
        }
        return CameraSettings.Level.M;
    }
}
