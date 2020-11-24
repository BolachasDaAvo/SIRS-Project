package sirs.client;

import sirs.grpc.Contract.*;
import sirs.grpc.RemoteGrpc;
import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ManagedChannel;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;

public class ClientFrontend {
    final ManagedChannel channel;
    RemoteGrpc.RemoteBlockingStub stub;

    public ClientFrontend(String host, String port) throws SSLException {
        String target = host + ":" + port;

        channel = NettyChannelBuilder.forTarget(target).sslContext(GrpcSslContexts.forClient().trustManager(new File("TLS/client/certClient.pem")).build()).build();
        stub = RemoteGrpc.newBlockingStub(channel);

    }

    public void upload(String filename) throws FileNotFoundException, IOException {
        UploadRequest.Builder request = UploadRequest.newBuilder().setName(filename);
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        ByteString bytes = ByteString.readFrom(fis);
        fis.close();

        request.setFile(bytes);

        stub.upload(request.build());
    }

    public void download(String filename) throws FileNotFoundException, IOException {
        DownloadRequest request = DownloadRequest.newBuilder().setName(filename).build();
        DownloadResponse response = stub.download(request);
        FileOutputStream fos = new FileOutputStream(filename);
        ByteString bytes = response.getFile();

        bytes.writeTo(fos);
        fos.close();
    }

    public void close() {
        if (this.channel != null) channel.shutdownNow();
    }
}