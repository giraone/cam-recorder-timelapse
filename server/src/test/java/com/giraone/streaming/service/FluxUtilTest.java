package com.giraone.streaming.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FluxUtilTest {

    @Test
    void byteBufferToArray() {

        // arrange
        ByteBuffer byteBuffer = ByteBuffer.wrap("0123456789".getBytes(StandardCharsets.UTF_8));
        // act
        byte[] bytes = FluxUtil.byteBufferToArray(byteBuffer);
        // assert
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("0123456789");
    }

    @Test
    void writeByteBufferToStream() throws IOException {

        // arrange
        ByteBuffer byteBuffer = ByteBuffer.wrap("0123456789".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // act
        FluxUtil.writeByteBufferToStream(byteBuffer, byteArrayOutputStream);
        // assert
        assertThat(byteArrayOutputStream.toString(StandardCharsets.UTF_8)).isEqualTo("0123456789");
    }

    @Test
    void writeToOutputStream() {

        Flux<ByteBuffer> flux = Flux.just(ByteBuffer.wrap("0123456789".getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // act
        FluxUtil.writeToOutputStream(flux, byteArrayOutputStream).block();
        // assert
        assertThat(byteArrayOutputStream.toString(StandardCharsets.UTF_8)).isEqualTo("0123456789");
    }
}