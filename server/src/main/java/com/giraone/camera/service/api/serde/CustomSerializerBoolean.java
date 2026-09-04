package com.giraone.camera.service.api.serde;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class CustomSerializerBoolean extends StdSerializer<Boolean> {

    protected CustomSerializerBoolean() {
        super(Boolean.class);
    }

    @Override
    public void serialize(Boolean value, JsonGenerator jsonGenerator, SerializationContext ctxt) throws JacksonException {
        jsonGenerator.writeNumber(value != null && value ? 1 : 0);
    }
}
