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

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import sirs.server.domain.File;

@Component
public class ServerImpl extends RemoteImplBase {

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private InviteService inviteService;

    private void writeFile(ByteString bytes, String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            bytes.writeTo(fos);
        } catch (Exception e) {
            System.out.println("Failed writing file");
        }
    }

    private ByteString readFile(String path) {
        try {
            FileInputStream fis = new FileInputStream(path);
            return ByteString.readFrom(fis);
        } catch (Exception e) {
            System.out.println("Failed reading from file");
        }
        return null;
    }

    @Override
    public void upload(final UploadRequest request, final StreamObserver<UploadResponse> responseObserver) {
        // TODO: Use jwt to authenticate user
        // Construct path
        String filename = request.getName();
        String filePath = "./users/mockuser/" + filename;

        // Check if file already exists
        if (fileService.getFile(filename) == null)
            fileService.createFile(1, filename, filePath);
        else {
            fileService.updateVersion(filename);
        }

        writeFile(request.getFile(), filePath);

        final UploadResponse response = UploadResponse.getDefaultInstance();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void download(final DownloadRequest request, final StreamObserver<DownloadResponse> responseObserver) {
        String filename = request.getName();
        DownloadResponse.Builder builder = DownloadResponse.newBuilder();
        File file = fileService.getFile(filename);
        // TODO: Test file existence
        // TODO: Implement gRPC exceptions
        String filePath = file.getPath();
        ByteString bytes = readFile(filePath);
        builder.setFile(bytes);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
