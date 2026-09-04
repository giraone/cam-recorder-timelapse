package com.giraone.camera.service.api.serde;

import com.giraone.camera.service.api.CameraSettings;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

public class CustomEnumDeserializerSpecialEffect extends StdDeserializer<CameraSettings.SpecialEffect> {

    protected CustomEnumDeserializerSpecialEffect() {
        super(String.class);
    }

    @Override
    public CameraSettings.SpecialEffect deserialize(JsonParser jsonParser, DeserializationContext ctxt) {
        final JsonNode node = jsonParser.readValueAsTree();
        final int value = Integer.parseInt(node.asString());
        for (CameraSettings.SpecialEffect e : CameraSettings.SpecialEffect.ALL) {
            if (e.ordinal() == value) {
                return e;
            }
        }
        return null;
    }
}
