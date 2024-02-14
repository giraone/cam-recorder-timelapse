package com.giraone.streaming.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;

/**
 * * Parts of this are copied from
 * * https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/util/FluxUtil.java
 */
public final class FluxUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxUtil.class);

    public static final int DEFAULT_CHUNK_SIZE = 1024 * 64;

    // Hide
    private FluxUtil() {
    }

    /**
     * Propagates a {@link RuntimeException} through the error channel of {@link Mono}.
     * @param logger The {@link Logger} to log the exception.
     * @param ex The {@link RuntimeException}.
     * @param <T> The return type.
     * @return A {@link Mono} that terminates with error wrapping the {@link RuntimeException}.
     */
    public static <T> Mono<T> monoError(Logger logger, RuntimeException ex) {
        logger.error("Error occurred", ex);
        return Mono.error(ex);
    }

    //--- Write to file and read from file -----------------------------------------------------------------------------

    /**
     * Writes the {@link ByteBuffer ByteBuffers} emitted by a {@link Flux} of {@link ByteBuffer} to an {@link
     * AsynchronousFileChannel}.
     * <p>
     * The {@code outFile} is not closed by this call, closing of the {@code outFile} is managed by the caller.
     * <p>
     * The response {@link Mono} will emit an error if {@code content} or {@code outFile} are null. Additionally, an
     * error will be emitted if the {@code outFile} wasn't opened with the proper open options, such as {@link
     * StandardOpenOption#WRITE}.
     * @param content The {@link Flux} of {@link ByteBuffer} content.
     * @param outFile The {@link AsynchronousFileChannel}.
     * @return A {@link Mono} which emits a completion status once the {@link Flux} has been written to the {@link
     * AsynchronousFileChannel}.
     * @throws NullPointerException When {@code content} is null.
     * @throws NullPointerException When {@code outFile} is null.
     */
    public static Mono<Void> writeFile(Flux<ByteBuffer> content, AsynchronousFileChannel outFile) {
        return writeFile(content, outFile, 0);
    }

    /**
     * Writes the {@link ByteBuffer ByteBuffers} emitted by a {@link Flux} of {@link ByteBuffer} to an {@link
     * AsynchronousFileChannel} starting at the given {@code position} in the file.
     * <p>
     * The {@code outFile} is not closed by this call, closing of the {@code outFile} is managed by the caller.
     * <p>
     * The response {@link Mono} will emit an error if {@code content} or {@code outFile} are null or {@code position}
     * is less than 0. Additionally, an error will be emitted if the {@code outFile} wasn't opened with the proper open
     * options, such as {@link StandardOpenOption#WRITE}.
     * @param content The {@link Flux} of {@link ByteBuffer} content.
     * @param outFile The {@link AsynchronousFileChannel}.
     * @param position The position in the file to begin writing the {@code content}.
     * @return A {@link Mono} which emits a completion status once the {@link Flux} has been written to the {@link AsynchronousFileChannel}.
     * @throws NullPointerException When {@code content} is null.
     * @throws NullPointerException When {@code outFile} is null.
     * @throws IllegalArgumentException When {@code position} is negative.
     */
    public static Mono<Void> writeFile(Flux<ByteBuffer> content, AsynchronousFileChannel outFile, long position) {
        if (content == null && outFile == null) {
            return monoError(LOGGER, new NullPointerException("'content' and 'outFile' cannot be null."));
        } else if (content == null) {
            return monoError(LOGGER, new NullPointerException("'content' cannot be null."));
        } else if (outFile == null) {
            return monoError(LOGGER, new NullPointerException("'outFile' cannot be null."));
        } else if (position < 0) {
            return monoError(LOGGER, new IllegalArgumentException("'position' cannot be less than 0."));
        }

        return writeToAsynchronousByteChannel(content, IoChannelUtils.toAsynchronousByteChannel(outFile, position));
    }

    /**
     * Writes the {@link ByteBuffer ByteBuffers} emitted by a {@link Flux} of {@link ByteBuffer} to an {@link
     * AsynchronousByteChannel}.
     * <p>
     * The {@code channel} is not closed by this call, closing of the {@code channel} is managed by the caller.
     * <p>
     * The response {@link Mono} will emit an error if {@code content} or {@code channel} are null.
     * @param content The {@link Flux} of {@link ByteBuffer} content.
     * @param channel The {@link AsynchronousByteChannel}.
     * @return A {@link Mono} which emits a completion status once the {@link Flux} has been written to the {@link
     * AsynchronousByteChannel}.
     * @throws NullPointerException When {@code content} is null.
     * @throws NullPointerException When {@code channel} is null.
     */
    public static Mono<Void> writeToAsynchronousByteChannel(Flux<ByteBuffer> content, AsynchronousByteChannel channel) {

        if (content == null && channel == null) {
            return monoError(LOGGER, new NullPointerException("'content' and 'channel' cannot be null."));
        } else if (content == null) {
            return monoError(LOGGER, new NullPointerException("'content' cannot be null."));
        } else if (channel == null) {
            return monoError(LOGGER, new NullPointerException("'channel' cannot be null."));
        }
        return Mono.create(emitter -> content.subscribe(
            new AsynchronousByteChannelWriteSubscriber(channel, emitter)));
    }

    /**
     * Writes the {@link ByteBuffer ByteBuffers} emitted by a {@link Flux} of {@link ByteBuffer} to an {@link
     * WritableByteChannel}.
     * <p>
     * The {@code channel} is not closed by this call, closing of the {@code channel} is managed by the caller.
     * <p>
     * The response {@link Mono} will emit an error if {@code content} or {@code channel} are null.
     * @param content The {@link Flux} of {@link ByteBuffer} content.
     * @param channel The {@link WritableByteChannel}.
     * @return A {@link Mono} which emits a completion status once the {@link Flux} has been written to the {@link
     * WritableByteChannel}.
     * @throws NullPointerException When {@code content} is null.
     * @throws NullPointerException When {@code channel} is null.
     */
    public static Mono<Void> writeToWritableByteChannel(Flux<ByteBuffer> content, WritableByteChannel channel) {

        if (content == null && channel == null) {
            return monoError(LOGGER, new NullPointerException("'content' and 'channel' cannot be null."));
        } else if (content == null) {
            return monoError(LOGGER, new NullPointerException("'content' cannot be null."));
        } else if (channel == null) {
            return monoError(LOGGER, new NullPointerException("'channel' cannot be null."));
        }
        return content.publishOn(Schedulers.boundedElastic())
            .map(buffer -> {
                try {
                    IoChannelUtils.fullyWriteBuffer(buffer, channel);
                } catch (IOException e) {
                    throw Exceptions.propagate(e);
                }
                return buffer;
            }).then();
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a {@link Flux} from an {@link AsynchronousFileChannel} which reads part of a file into chunks of the
     * given size.
     * @param fileChannel The file channel.
     * @param chunkSize the size of file chunks to read.
     * @param offset The offset in the file to begin reading.
     * @param length The number of bytes to read from the file.
     * @return the Flux.
     */
    public static Flux<ByteBuffer> readFile(AsynchronousFileChannel fileChannel, int chunkSize, long offset, long length) {
        return new FileReadFlux(fileChannel, chunkSize, offset, length);
    }

    /**
     * Creates a {@link Flux} from an {@link AsynchronousFileChannel} which reads part of a file.
     * @param fileChannel The file channel.
     * @param offset The offset in the file to begin reading.
     * @param length The number of bytes to read from the file.
     * @return the Flux.
     */
    public static Flux<ByteBuffer> readFile(AsynchronousFileChannel fileChannel, long offset, long length) {
        return readFile(fileChannel, DEFAULT_CHUNK_SIZE, offset, length);
    }

    /**
     * Creates a {@link Flux} from an {@link AsynchronousFileChannel} which reads the entire file.
     * @param fileChannel The file channel.
     * @param chunkSize the size of file chunks to read.
     * @return The AsyncInputStream.
     */
    public static Flux<ByteBuffer> readFile(AsynchronousFileChannel fileChannel, int chunkSize) {
        try {
            final long size = fileChannel.size();
            return readFile(fileChannel, chunkSize, 0, size);
        } catch (IOException e) {
            return Flux.error(new RuntimeException("Failed to read the file.", e));
        }
    }

    /**
     * Creates a {@link Flux} from an {@link AsynchronousFileChannel} which reads the entire file.
     * @param fileChannel The file channel.
     * @return The AsyncInputStream.
     */
    public static Flux<ByteBuffer> readFile(AsynchronousFileChannel fileChannel) {
        try {
            final long size = fileChannel.size();
            return readFile(fileChannel, DEFAULT_CHUNK_SIZE, 0, size);
        } catch (IOException e) {
            return Flux.error(new RuntimeException("Failed to read the file.", e));
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Writes the {@link ByteBuffer ByteBuffers} emitted by a {@link Flux} of {@link ByteBuffer} to an {@link
     * OutputStream}.
     * <p>
     * The {@code stream} is not closed by this call, closing of the {@code stream} is managed by the caller.
     * <p>
     * The response {@link Mono} will emit an error if {@code content} or {@code stream} are null. Additionally, an
     * error will be emitted if an exception occurs while writing the {@code content} to the {@code stream}.
     * @param content The {@link Flux} of {@link ByteBuffer} content.
     * @param stream The {@link OutputStream} being written into.
     * @return A {@link Mono} which emits a completion status once the {@link Flux} has been written to the {@link
     * OutputStream}, or an error status if writing fails.
     */
    public static Mono<Void> writeToOutputStream(Flux<ByteBuffer> content, OutputStream stream) {

        if (content == null && stream == null) {
            return monoError(LOGGER, new NullPointerException("'content' and 'stream' cannot be null."));
        } else if (content == null) {
            return monoError(LOGGER, new NullPointerException("'content' cannot be null."));
        } else if (stream == null) {
            return monoError(LOGGER, new NullPointerException("'stream' cannot be null."));
        }
        return Mono.create(emitter -> content.subscribe(new OutputStreamWriteSubscriber(emitter, stream)));
    }

    /**
     * Writes a {@link ByteBuffer} into an {@link OutputStream}.
     * This method provides writing optimization based on the type of {@link ByteBuffer} and {@link OutputStream}
     * passed. For example, if the {@link ByteBuffer} has a backing {@code byte[]} this method will access that directly
     * to write to the {@code stream} instead of buffering the contents of the {@link ByteBuffer} into a temporary
     * buffer.
     * @param buffer The {@link ByteBuffer} to write into the {@code stream}.
     * @param stream The {@link OutputStream} where the {@code buffer} will be written.
     * @throws IOException If an I/O occurs while writing the {@code buffer} into the {@code stream}.
     */
    public static void writeByteBufferToStream(ByteBuffer buffer, OutputStream stream) throws IOException {

        // First check if the buffer has a backing byte[]. The backing byte[] can be accessed directly and written
        // without an additional buffering byte[].
        if (buffer.hasArray()) {
            // Write the byte[] from the current view position to the length remaining in the view.
            stream.write(buffer.array(), buffer.position(), buffer.remaining());
            // Update the position of the ByteBuffer to treat this the same as getting from the buffer.
            buffer.position(buffer.position() + buffer.remaining());
            return;
        }

        // Next begin checking for specific instances of OutputStream that may provide better writing options for direct ByteBuffers.
        if (stream instanceof FileOutputStream fileOutputStream) {
            // Writing to the FileChannel directly may provide native optimizations for moving the OS managed memory into the file.
            // Write will move both the OutputStream's and ByteBuffer's position so there is no need to perform
            // additional updates that are required when using the backing array.
            fileOutputStream.getChannel().write(buffer);
            return;
        }

        // All optimizations have been exhausted, fallback to buffering write.
        stream.write(FluxUtil.byteBufferToArray(buffer));
    }

    /**
     * Gets the content of the provided ByteBuffer as a byte array. This method will create a new byte array even if the
     * ByteBuffer can have optionally backing array.
     * @param byteBuffer the byte buffer
     * @return the byte array
     */
    public static byte[] byteBufferToArray(ByteBuffer byteBuffer) {
        final int length = byteBuffer.remaining();
        final byte[] byteArray = new byte[length];
        byteBuffer.get(byteArray);
        return byteArray;
    }
}
