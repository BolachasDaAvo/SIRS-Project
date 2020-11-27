package sirs.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sirs.grpc.Contract.*;
import sirs.grpc.RemoteGrpc.RemoteImplBase;
import com.google.protobuf.ByteString;
import sirs.server.domain.User;
import sirs.server.security.AuthInterceptor;
import sirs.server.security.AuthenticationException;
import sirs.server.security.AuthenticationService;
import sirs.server.service.FileService;
import sirs.server.service.InviteService;
import sirs.server.service.UserService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;

import sirs.server.domain.File;

@Component
public class ServerImpl extends RemoteImplBase {

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private InviteService inviteService;

    @Autowired
    private AuthenticationService authenticationService;

    private void writeFile(ByteString bytes, String path) throws IOException {
        FileOutputStream fos = new FileOutputStream(path);
        bytes.writeTo(fos);
    }

    private ByteString readFile(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        return ByteString.readFrom(fis);
    }

    @Override
    public void upload(final UploadRequest request, final StreamObserver<UploadResponse> responseObserver) {
        Integer userId = AuthInterceptor.USER_ID.get();
        if (userId == -1) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Upload endpoint is for authenticated users only.").asRuntimeException());
            return;
        }
        // Construct path
        String filename = request.getName();
        ByteString sig = request.getSignature();
        byte[] sigBytes = new byte[sig.size()];
        sig.copyTo(sigBytes, 0);
        String filePath = "./users/" + userId + "/" + filename;

        // Check if file already exists
        File file = this.fileService.getFileByPath(filePath);
        if (file == null)
            fileService.createFile(userId, filename, filePath, sigBytes);
        else {
            fileService.updateFile(filePath, sigBytes, userId);
        }

        try {
            writeFile(request.getFile(), filePath);
        } catch (IOException e) {
            responseObserver.onError(Status.UNKNOWN.withDescription("Failed writing file").asRuntimeException());
            return;
        }

        final UploadResponse response = UploadResponse.getDefaultInstance();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void download(final DownloadRequest request, final StreamObserver<DownloadResponse> responseObserver) {
        Integer userId = AuthInterceptor.USER_ID.get();
        if (userId == -1) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Download endpoint is for authenticated users only.").asRuntimeException());
            return;
        }
        String filename = request.getName();
        File file = fileService.getFileByUser(filename, userId);
        if (file == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("File " + filename + " does not exist.").asRuntimeException());
            return;
        }

        DownloadResponse.Builder builder = DownloadResponse.newBuilder();

        String filePath = file.getPath();
        ByteString bytes;
        byte[] sigBytes = file.getSignature();
        ByteString sig = ByteString.copyFrom(sigBytes);
        User modifier = file.getLastModifier();
        byte[] certBytes = modifier.getCertificate();
        ByteString cert = ByteString.copyFrom(certBytes);
        try {
            bytes = readFile(filePath);
        } catch(IOException e) {
            responseObserver.onError(Status.UNKNOWN.withDescription("Failed reading file").asRuntimeException());
            return;
        }
        builder.setFile(bytes);
        builder.setSignature(sig);
        builder.setCertificate(cert);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        User user;
        try {
            user = this.authenticationService.registerUser(request.getUsername(), request.getCertificate().toByteArray());
        } catch (AuthenticationException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
            return;
        }

        // Create user directory
        java.io.File directory = new java.io.File("./users/" + user.getId() +"/");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        RegisterResponse registerResponse = RegisterResponse.newBuilder().build();
        responseObserver.onNext(registerResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getNumber(NumberRequest request, StreamObserver<NumberResponse> responseObserver) {
        String number = this.authenticationService.getNumber(request.getUsername());
        NumberResponse numberResponse = NumberResponse.newBuilder().setNumber(number).build();
        responseObserver.onNext(numberResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getToken(TokenRequest request, StreamObserver<TokenResponse> responseObserver) {
        try {
            String token = this.authenticationService.getToken(request.getUsername(), request.getNumber().toByteArray());
            TokenResponse tokenResponse = TokenResponse.newBuilder().setToken(token).build();
            responseObserver.onNext(tokenResponse);
            responseObserver.onCompleted();
        } catch (AuthenticationException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
