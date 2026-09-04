package com.giraone.camera.service.api.serde;

import com.giraone.camera.service.api.CameraSettings;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

public class CustomEnumDeserializerFrameSize extends StdDeserializer<CameraSettings.FrameSize> {

    protected CustomEnumDeserializerFrameSize() {
        super(String.class);
    }

    @Override
    public CameraSettings.FrameSize deserialize(JsonParser jsonParser, DeserializationContext ctxt) {
        final JsonNode node = jsonParser.readValueAsTree();
        final int value = Integer.parseInt(node.asString());
        for (CameraSettings.FrameSize e : CameraSettings.FrameSize.ALL) {
            if (e.ordinal() == value) {
                return e;
            }
        }
        return null;
    }
}
