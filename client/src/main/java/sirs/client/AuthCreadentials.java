package sirs.client;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.Executor;

public class AuthCreadentials extends CallCredentials {
    public String token;

    public AuthCreadentials(String token) {
        this.token = token;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier metadataApplier) {
        executor.execute(() -> {
            if (this.token == null) {
                metadataApplier.fail(Status.UNAUTHENTICATED.withDescription("Token is not present"));
            }
            try {
                Metadata headers = new Metadata();
                headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + this.token);
                metadataApplier.apply(headers);
            } catch (Exception e) {
                metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }

    @Override
    public void thisUsesUnstableApi() {
    }
}
