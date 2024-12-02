package com.giraone.camera.service.api.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class CustomSerializerBoolean extends StdSerializer<Boolean> {

    protected CustomSerializerBoolean() {
        super(Boolean.class);
    }

    @Override
    public void serialize(Boolean aBoolean, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeNumber(aBoolean != null && aBoolean ? 1 : 0);
    }
}
