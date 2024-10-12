package com.giraone.streaming.service;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Parts of this are copied from
 * https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/util/FluxUtil.java
 */
class FileReadFlux extends Flux<ByteBuffer> {
    private final AsynchronousFileChannel fileChannel;
    private final int chunkSize;
    private final long offset;
    private final long length;

    FileReadFlux(AsynchronousFileChannel fileChannel, int chunkSize, long offset, long length) {
        this.fileChannel = fileChannel;
        this.chunkSize = chunkSize;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public void subscribe(CoreSubscriber<? super ByteBuffer> actual) {
        FileReadSubscription subscription =
            new FileReadSubscription(actual, fileChannel, chunkSize, offset, length);
        actual.onSubscribe(subscription);
    }

    static final class FileReadSubscription implements Subscription, CompletionHandler<Integer, ByteBuffer> {
        private static final int NOT_SET = -1;

        private final Subscriber<? super ByteBuffer> subscriber;
        private volatile long position;

        private final AsynchronousFileChannel fileChannel;
        private final int chunkSize;
        private final long offset;
        private final long length;

        private volatile boolean done;
        private Throwable error;
        private volatile ByteBuffer next;
        private volatile boolean cancelled;

        volatile int wip;
        static final AtomicIntegerFieldUpdater<FileReadSubscription> ATOMIC_WIP =
            AtomicIntegerFieldUpdater.newUpdater(FileReadSubscription.class, "wip");

        volatile long requested;
        static final AtomicLongFieldUpdater<FileReadSubscription> ATOMIC_REQUESTED =
            AtomicLongFieldUpdater.newUpdater(FileReadSubscription.class, "requested");

        FileReadSubscription(Subscriber<? super ByteBuffer> subscriber, AsynchronousFileChannel fileChannel,
                             int chunkSize, long offset, long length) {
            this.subscriber = subscriber;
            this.fileChannel = fileChannel;
            this.chunkSize = chunkSize;
            this.offset = offset;
            this.length = length;
            this.position = NOT_SET;
        }

        @Override
        public void request(long n) {
            if (Operators.validate(n)) {
                Operators.addCap(ATOMIC_REQUESTED, this, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public void completed(Integer bytesRead, ByteBuffer buffer) {
            if (!cancelled) {
                if (bytesRead == -1) {
                    done = true;
                } else {
                    // use local variable to perform fewer volatile reads
                    long pos = position;
                    int bytesWanted = Math.min(bytesRead, maxRequired(pos));
                    long position2 = pos + bytesWanted;
                    position = position2;
                    buffer.position(bytesWanted);
                    buffer.flip();
                    next = buffer;
                    if (position2 >= offset + length) {
                        done = true;
                    }
                }
                drain();
            }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            if (!cancelled) {
                // must set error before setting done to true so that is visible in drain loop
                error = exc;
                done = true;
                drain();
            }
        }

        private void drain() {
            if (ATOMIC_WIP.getAndIncrement(this) != 0) {
                return;
            }
            // on first drain (first request) we initiate the first read
            if (position == NOT_SET) {
                position = offset;
                doRead();
            }
            int missed = 1;
            while (true) {
                if (cancelled) {
                    return;
                }
                if (ATOMIC_REQUESTED.get(this) > 0) {
                    boolean emitted = false;
                    // read d before next to avoid race
                    boolean d = done;
                    ByteBuffer bb = next;
                    if (bb != null) {
                        next = null;
                        subscriber.onNext(bb);
                        emitted = true;
                    }
                    if (d) {
                        if (error != null) {
                            subscriber.onError(error);
                        } else {
                            subscriber.onComplete();
                        }
                        // exit without reducing wip so that further drains will be NOOP
                        return;
                    }
                    if (emitted) {
                        // do this after checking d to avoid calling read when done
                        Operators.produced(ATOMIC_REQUESTED, this, 1);
                        doRead();
                    }
                }
                missed = ATOMIC_WIP.addAndGet(this, -missed);
                if (missed == 0) {
                    return;
                }
            }
        }

        private void doRead() {
            // use local variable to limit volatile reads
            long pos = position;
            ByteBuffer innerBuf = ByteBuffer.allocate(Math.min(chunkSize, maxRequired(pos)));
            fileChannel.read(innerBuf, pos, innerBuf, this);
        }

        private int maxRequired(long pos) {
            long maxRequired = offset + length - pos;
            if (maxRequired <= 0) {
                return 0;
            } else {
                int m = (int) (maxRequired);
                // support really large files by checking for overflow
                if (m < 0) {
                    return Integer.MAX_VALUE;
                } else {
                    return m;
                }
            }
        }
    }
}

