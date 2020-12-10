package sirs.server;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;
import sirs.grpc.Contract.*;
import sirs.grpc.RemoteGrpc;
import sirs.grpc.RemoteGrpc.RemoteImplBase;
import com.google.protobuf.ByteString;
import sirs.server.domain.User;
import sirs.server.security.AuthCredentials;
import sirs.server.security.AuthInterceptor;
import sirs.server.security.AuthenticationException;
import sirs.server.security.AuthenticationService;
import sirs.server.service.FileService;
import sirs.server.service.InviteService;
import sirs.server.service.UserService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.io.FileInputStream;

import sirs.server.domain.File;
import sirs.server.domain.Invite;

import javax.net.ssl.SSLException;

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

    // Backup
    public boolean primary;
    public ZKNaming zkNaming;
    private ManagedChannel channel;
    private RemoteGrpc.RemoteBlockingStub stub;

    static final String BASE_PATH = "/grpc/sirs/server";

    private void connectToBackup() throws ZKNamingException, SSLException {
        System.out.print("looking for backup server");
        ZKRecord record = this.zkNaming.lookup(BASE_PATH + "/" + "backup");
        this.channel = NettyChannelBuilder.forTarget(record.getURI()).sslContext(GrpcSslContexts.forClient().trustManager(new java.io.File("../TLS/ca-cert.pem")).build()).build();
        this.stub = RemoteGrpc.newBlockingStub(channel);
    }

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

        // Forward request to backup
        if (primary) {
            try {
                if (stub == null) {
                    connectToBackup();
                }
                stub.withCallCredentials(new AuthCredentials(AuthInterceptor.USER_TOKEN.get())).upload(request);
            } catch (ZKNamingException e) {
                System.out.println("Unable to find backup");
            } catch (SSLException e) {
                e.printStackTrace();
                System.out.println("Unable to create channel");
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
            }
        }

        // Construct path
        User owner = userService.getUserByUsername(request.getOwner());
        String filename = request.getName();
        ByteString sig = request.getSignature();
        byte[] sigBytes = new byte[sig.size()];
        sig.copyTo(sigBytes, 0);
        String filePath = "./users/" + owner.getId() + "/" + filename;

        // Check if file already exists
        File file = this.fileService.getFileByPath(filePath);
        if (file == null)
            fileService.createFile(userId, filename, sigBytes);
        else {
            file = fileService.getFileByUser(filename, userId);
            if (file == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("You do not have access to file").asRuntimeException());
                return;
            }
            fileService.updateFile(filePath, sigBytes, userId);
        }

        try {
            writeFile(request.getFile(), filePath);
        } catch (IOException e) {
            responseObserver.onError(Status.UNKNOWN.withDescription("Failed writing file").asRuntimeException());
            return;
        }

        file = this.fileService.getFileByPath(filePath);
        final UploadResponse response = UploadResponse.newBuilder().setVersion(file.getVersion()).build();
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

        // Forward request to backup
        if (primary) {
            try {
                if (stub == null) {
                    connectToBackup();
                }
                stub.withCallCredentials(new AuthCredentials(AuthInterceptor.USER_TOKEN.get())).download(request);
            } catch (ZKNamingException e) {
                System.out.println("Unable to find backup");
            } catch (SSLException e) {
                e.printStackTrace();
                System.out.println("Unable to create channel");
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
            }
        }

        String filename = request.getName();
        File file = fileService.getFileByUser(filename, userId);
        if (file == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("You do not have access to file.").asRuntimeException());
            return;
        }

        DownloadResponse.Builder builder = DownloadResponse.newBuilder();

        String filePath = file.getPath();
        ByteString bytes;
        byte[] sigBytes = file.getSignature();
        ByteString sig = ByteString.copyFrom(sigBytes);
        User modifier = file.getLastModifier();
        int version = file.getVersion();
        User owner = file.getOwner();
        byte[] certBytes = modifier.getCertificate();
        ByteString cert = ByteString.copyFrom(certBytes);
        try {
            bytes = readFile(filePath);
        } catch (IOException e) {
            responseObserver.onError(Status.UNKNOWN.withDescription("Failed reading file").asRuntimeException());
            return;
        }
        builder.setFile(bytes);
        builder.setSignature(sig);
        builder.setCertificate(cert);
        builder.setLastModifier(modifier.getUsername());
        builder.setVersion(version);
        builder.setOwner(owner.getUsername());

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void share(final ShareRequest request, final StreamObserver<ShareResponse> responseObserver) {
        Integer userId = AuthInterceptor.USER_ID.get();
        if (userId == -1) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Share endpoint is for authenticated users only.").asRuntimeException());
            return;
        }

        // Forward request to backup
        if (primary) {
            try {
                if (stub == null) {
                    connectToBackup();
                }
                stub.withCallCredentials(new AuthCredentials(AuthInterceptor.USER_TOKEN.get())).share(request);
            } catch (ZKNamingException e) {
                System.out.println("Unable to find backup");
            } catch (SSLException e) {
                e.printStackTrace();
                System.out.println("Unable to create channel");
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
            }
        }

        String username = request.getUser();
        User user;
        try {
            user = userService.getUserByUsername(username);
        } catch (Exception e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User does not exist.").asRuntimeException());
            return;
        }
        byte[] certBytes = user.getCertificate();
        ByteString cert = ByteString.copyFrom(certBytes);

        ShareResponse response = ShareResponse.newBuilder().setCertificate(cert).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void remove(final RemoveRequest request, final StreamObserver<RemoveResponse> responseObserver) {
        Integer userId = AuthInterceptor.USER_ID.get();
        if (userId == -1) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Remove endpoint is for authenticated users only.").asRuntimeException());
            return;
        }

        // Forward request to backup
        if (primary) {
            try {
                if (stub == null) {
                    connectToBackup();
                }
                stub.withCallCredentials(new AuthCredentials(AuthInterceptor.USER_TOKEN.get())).remove(request);
            } catch (ZKNamingException e) {
                System.out.println("Unable to find backup");
            } catch (SSLException e) {
                e.printStackTrace();
                System.out.println("Unable to create channel");
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
            }
        }

        // Make sure user owns the file
        String fileName = request.getFile();
        File file = fileService.getFileByUser(fileName, userId);
        if (file == null || file.getOwner().getId() != userId) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("You do not own the file.").asRuntimeException());
            return;
        }

        String username = request.getUser();
        User user;
        try {
            user = userService.getUserByUsername(username);
        } catch (Exception e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User does not exist.").asRuntimeException());
            return;
        }

        // Check if user is in share
        if (fileService.getFileByUser(fileName, user.getId()) == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User does not have access to file.").asRuntimeException());
            return;
        }

        // Collect certificates
        RemoveResponse.Builder response = RemoveResponse.newBuilder();
        for (User collaborator : fileService.getFileCollaboratos(file.getId())) {
            if (collaborator.getId() == userId || collaborator.getUsername().equals(username)) {
                continue;
            }

            byte[] certBytes = collaborator.getCertificate();
            ByteString cert = ByteString.copyFrom(certBytes);
            RemoveResponse.User pair = RemoveResponse.User.newBuilder()
                    .setUsername(collaborator.getUsername())
                    .setCertificate(cert)
                    .build();
            response.addUser(pair);
        }
        fileService.clearCollaborators(file.getId());
        fileService.addCollaborator(file.getId(), userId);

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void invite(final InviteRequest request, final StreamObserver<InviteResponse> responseObserver) {
        Integer userId = AuthInterceptor.USER_ID.get();
        if (userId == -1) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Invite endpoint is for authenticated users only.").asRuntimeException());
            return;
        }

        // Forward request to backup
        if (primary) {
            try {
                if (stub == null) {
                    connectToBackup();
                }
                stub.withCallCredentials(new AuthCredentials(AuthInterceptor.USER_TOKEN.get())).invite(request);
            } catch (ZKNamingException e) {
                System.out.println("Unable to find backup");
            } catch (SSLException e) {
                e.printStackTrace();
                System.out.println("Unable to create channel");
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
            }
        }

        // Make sure user owns the file
        String fileName = request.getFile();
        File file = fileService.getFileByUser(fileName, userId);
        if (file == null || file.getOwner().getId() != userId) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("You do not own the file.").asRuntimeException());
            return;
        }

        User invited = userService.getUserByUsername(request.getUser());
        if (fileService.getFileByUser(fileName, invited.getId()) != null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User can already edit the file.").asRuntimeException());
            return;
        }

        for (Invite invite : invited.getPendingInvites()) {
            if (invite.getFile().getId() == file.getId()) {
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User has already been invited to edit that file.").asRuntimeException());
                return;
            }
        }

        ByteString key = request.getKey();
        byte[] keyBytes = new byte[key.size()];
        key.copyTo(keyBytes, 0);

        inviteService.createInvite(invited.getId(), file.getId(), keyBytes);

        InviteResponse response = InviteResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void accept(AcceptRequest request, StreamObserver<AcceptResponse> responseObserver) {
        Integer userId = AuthInterceptor.USER_ID.get();
        if (userId == -1) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Accept endpoint is for authenticated users only.").asRuntimeException());
            return;
        }

        // Forward request to backup
        if (primary) {
            try {
                if (stub == null) {
                    connectToBackup();
                }
                stub.withCallCredentials(new AuthCredentials(AuthInterceptor.USER_TOKEN.get())).accept(request);
            } catch (ZKNamingException e) {
                System.out.println("Unable to find backup");
            } catch (SSLException e) {
                e.printStackTrace();
                System.out.println("Unable to create channel");
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
            }
        }

        Invite invite = inviteService.getInviteByUser(request.getFile(), userId);
        if (invite == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("You have not been invited to edit this file.").asRuntimeException());
            return;
        }
        inviteService.acceptInvite(invite.getId());
        ByteString fileKey = ByteString.copyFrom(invite.getFileKey());

        AcceptResponse response = AcceptResponse.newBuilder().setKey(fileKey).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {

        // Forward request to backup
        if (primary) {
            try {
                if (stub == null) {
                    connectToBackup();
                }
                stub.register(request);
            } catch (ZKNamingException e) {
                System.out.println("Unable to find backup");
            } catch (SSLException e) {
                e.printStackTrace();
                System.out.println("Unable to create channel");
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
            }
        }

        User user;
        try {
            user = this.authenticationService.registerUser(request.getUsername(), request.getCertificate().toByteArray());
        } catch (AuthenticationException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
            return;
        }

        // Create user directory
        java.io.File directory = new java.io.File("./users/" + user.getId() + "/");
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
            TokenResponse.Builder tokenResponse = TokenResponse.newBuilder();
            User user = userService.getUserByUsername(request.getUsername());

            for (Invite invite : user.getPendingInvites()) {
                tokenResponse.addInvite(invite.getFile().getName());
            }

            tokenResponse.setToken(token);
            responseObserver.onNext(tokenResponse.build());
            responseObserver.onCompleted();
        } catch (AuthenticationException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
