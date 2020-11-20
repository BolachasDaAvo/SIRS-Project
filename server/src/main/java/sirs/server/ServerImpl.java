package sirs.server;

import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sirs.grpc.Contract.*;
import sirs.grpc.RemoteGrpc.RemoteImplBase;
import com.google.protobuf.ByteString;
import sirs.server.service.FileService;
import sirs.server.service.InviteService;
import sirs.server.service.UserService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Component
public class ServerImpl extends RemoteImplBase {

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private InviteService inviteService;

    @Override
    public void upload(final UploadRequest request, final StreamObserver<UploadResponse> responseObserver) {
        System.out.println(request.getName());

        try {
            ByteString bytes = request.getFile();
            FileOutputStream fos = new FileOutputStream(request.getName());
            bytes.writeTo(fos);
        } catch (Exception e) {
            System.out.println("Failed writing file");
        }
        final UploadResponse response = UploadResponse.getDefaultInstance();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void download(final DownloadRequest request, final StreamObserver<DownloadResponse> responseObserver) {
        String filename = request.getName();
        DownloadResponse.Builder builder = DownloadResponse.newBuilder();
        try {
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            ByteString bytes = ByteString.readFrom(fis);
            builder.setFile(bytes);
        } catch (Exception e) {
            System.out.println("Failed reading from file");
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
