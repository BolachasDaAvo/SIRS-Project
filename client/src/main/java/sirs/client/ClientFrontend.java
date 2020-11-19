package sirs.client;

import sirs.grpc.Contract.*;
import sirs.grpc.RemoteGrpc;
import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ManagedChannel;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ClientFrontend {
    final ManagedChannel channel;
    RemoteGrpc.RemoteBlockingStub stub;

    public ClientFrontend(String host, String port) {
        String target = host + ":" + port;

        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = RemoteGrpc.newBlockingStub(channel);
    }

    public void upload(String filename) throws FileNotFoundException, IOException {
        UploadRequest.Builder request = UploadRequest.newBuilder().setName(filename);
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        ByteString bytes = ByteString.readFrom(fis);

        request.setFile(bytes);

        stub.upload(request.build());
    }

    public void close() {
        if (this.channel != null) channel.shutdownNow();
    }
}