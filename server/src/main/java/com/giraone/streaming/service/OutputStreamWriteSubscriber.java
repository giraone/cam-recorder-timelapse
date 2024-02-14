package com.giraone.streaming.service;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Operators;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

/**
 * Subscriber that writes a stream of {@link ByteBuffer ByteBuffers} to an {@link OutputStream}.
 */
public final class OutputStreamWriteSubscriber implements Subscriber<ByteBuffer> {
    private final MonoSink<Void> emitter;
    private final OutputStream stream;

    private Subscription subscription;

    /**
     * Creates a subscriber that writes a stream of {@link ByteBuffer ByteBuffers} to an {@link OutputStream}.
     * @param emitter The {@link MonoSink} that will be notified when the stream has been written.
     * @param stream The {@link OutputStream} to write the stream of {@link ByteBuffer ByteBuffers} to.
     */
    public OutputStreamWriteSubscriber(MonoSink<Void> emitter, OutputStream stream) {
        this.emitter = emitter;
        this.stream = stream;
    }

    @Override
    public void onSubscribe(Subscription s) {
        // Only set the Subscription if one has not been previously set. Any additional Subscriptions will be cancelled.
        if (Operators.validate(this.subscription, s)) {
            subscription = s;
            s.request(1);
        }
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        try {
            FluxUtil.writeByteBufferToStream(byteBuffer, stream);
            subscription.request(1);
        } catch (IOException ex) {
            onError(new UncheckedIOException(ex));
        }
    }

    @Override
    public void onError(Throwable throwable) {
        subscription.cancel();
        emitter.error(throwable);
    }

    @Override
    public void onComplete() {
        emitter.success();
    }
}

