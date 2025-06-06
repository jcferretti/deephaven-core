//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
package io.deephaven.extensions.s3.testlib;

import io.deephaven.extensions.s3.S3Constants;
import io.deephaven.extensions.s3.S3Instructions;
import io.deephaven.extensions.s3.UniversalS3SeekableChannelProviderPlugin;
import io.deephaven.util.channel.SeekableChannelsProvider;
import io.deephaven.util.channel.SeekableChannelsProviderPlugin;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;

import static io.deephaven.extensions.s3.testlib.S3Helper.TIMEOUT_SECONDS;

public abstract class S3SeekableChannelTestSetup {

    protected static final String SCHEME = S3Constants.S3_URI_SCHEME;

    protected ExecutorService executor;
    protected S3AsyncClient asyncClient;
    protected String bucket;

    protected abstract S3AsyncClient s3AsyncClient();

    protected abstract S3Instructions.Builder s3Instructions(S3Instructions.Builder builder);

    protected final void doSetUp() throws ExecutionException, InterruptedException, TimeoutException {
        executor = Executors.newCachedThreadPool();
        bucket = UUID.randomUUID().toString();
        asyncClient = s3AsyncClient();
        asyncClient.createBucket(CreateBucketRequest.builder().bucket(bucket).build())
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    protected final void doTearDown() throws ExecutionException, InterruptedException, TimeoutException {
        S3Helper.deleteAllKeys(asyncClient, bucket);
        asyncClient.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build())
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        asyncClient.close();
        executor.shutdownNow();
    }

    protected void uploadDirectory(final Path directory, final String prefix)
            throws ExecutionException, InterruptedException, TimeoutException {
        S3Helper.uploadDirectory(asyncClient, directory, bucket, prefix, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    protected final URI uri(String key) {
        return URI.create(String.format("%s://%s/%s", SCHEME, bucket, key));
    }

    protected final void putObject(String key, AsyncRequestBody body)
            throws ExecutionException, InterruptedException, TimeoutException {
        asyncClient.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), body).get(5,
                TimeUnit.SECONDS);
    }

    protected final SeekableChannelsProvider providerImpl() {
        return providerImpl(S3Instructions.builder());
    }

    protected final SeekableChannelsProvider providerImpl(
            @NotNull final S3Instructions.Builder s3InstructionsBuilder) {
        final SeekableChannelsProviderPlugin plugin = new UniversalS3SeekableChannelProviderPlugin();
        final S3Instructions instructions =
                s3Instructions(s3InstructionsBuilder)
                        .build();
        return plugin.createProvider(SCHEME, instructions);
    }

    protected static ByteBuffer readAll(ReadableByteChannel channel, int maxBytes) throws IOException {
        final ByteBuffer dst = ByteBuffer.allocate(maxBytes);
        while (dst.remaining() > 0 && channel.read(dst) != -1) {
            // continue
        }
        if (dst.remaining() == 0) {
            if (channel.read(ByteBuffer.allocate(1)) != -1) {
                throw new RuntimeException(String.format("channel has more than %d bytes", maxBytes));
            }
        }
        dst.flip();
        return dst;
    }

    protected static void fillBuffer(ReadableByteChannel channel, final ByteBuffer dst) throws IOException {
        final int numBytes = dst.remaining();
        while (dst.remaining() > 0 && channel.read(dst) != -1) {
            // continue
        }
        if (dst.remaining() > 0) {
            throw new RuntimeException(String.format("channel has less than %d bytes", numBytes));
        }
        dst.flip();
    }

    /**
     * Build an {@link S3AsyncClient} which records when it was used for writing or reading
     */
    protected S3AsyncClient readWriteTrackingS3Client(
            @NotNull final S3Instructions instructions,
            final boolean[] usedForWriting,
            final boolean[] usedForReading) {
        final S3AsyncClient s3AsyncClient = S3AsyncClient.builder()
                .endpointOverride(instructions.endpointOverride().orElseThrow(
                        () -> new IllegalArgumentException("Endpoint override is required for S3AsyncClient")))
                .region(Region.of(instructions.regionName().orElseThrow(
                        () -> new IllegalArgumentException("Region name is required for S3AsyncClient"))))
                .credentialsProvider(instructions.awsV2CredentialsProvider())
                .build();

        return new DelegatingS3AsyncClient(s3AsyncClient) {
            @Override
            public CompletableFuture<UploadPartResponse> uploadPart(
                    final UploadPartRequest uploadPartRequest,
                    final AsyncRequestBody requestBody) {
                usedForWriting[0] = true;
                return super.uploadPart(uploadPartRequest, requestBody);
            }

            @Override
            public <ReturnT> CompletableFuture<ReturnT> getObject(
                    final GetObjectRequest getObjectRequest,
                    final AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {
                usedForReading[0] = true;
                return super.getObject(getObjectRequest, asyncResponseTransformer);
            }
        };
    }
}
