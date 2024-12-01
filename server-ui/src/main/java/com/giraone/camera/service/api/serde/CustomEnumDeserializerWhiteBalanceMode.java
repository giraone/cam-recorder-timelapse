package com.giraone.camera.service.api.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.giraone.camera.service.api.CameraSettings;

import java.io.IOException;

public class CustomEnumDeserializerWhiteBalanceMode extends StdDeserializer<CameraSettings.WhiteBalanceMode> {

    protected CustomEnumDeserializerWhiteBalanceMode() {
        super(String.class);
    }

    @Override
    public CameraSettings.WhiteBalanceMode deserialize(JsonParser jsonParser, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int value = Integer.parseInt(node.asText());
        for (CameraSettings.WhiteBalanceMode e : CameraSettings.WhiteBalanceMode.ALL) {
            if (e.ordinal() == value) {
                return e;
            }
        }
        return null;
    }
}
