package sirs.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;
import sirs.grpc.Contract.*;
import sirs.grpc.RemoteGrpc;
import io.grpc.ManagedChannel;
import com.google.protobuf.ByteString;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.*;
import java.security.cert.*;
import java.util.List;
import java.util.ArrayList;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLException;

import org.json.simple.*;
import org.apache.commons.lang3.tuple.Pair;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class ClientFrontend {
    private ManagedChannel channel;
    private RemoteGrpc.RemoteBlockingStub stub;
    private ZKNaming zkNaming;
    private String token;

    public static final String BASE_PATH = "/grpc/sirs/server";

    public ClientFrontend(String zkHost, String zkPort) throws SSLException, ZKNamingException {
        this.zkNaming = new ZKNaming(zkHost, zkPort);
        ZKRecord record = this.zkNaming.lookup(BASE_PATH + "/" + "primary");

        channel = NettyChannelBuilder.forTarget(record.getURI()).sslContext(GrpcSslContexts.forClient().trustManager(new File("../TLS/ca-cert.pem")).build()).build();
        stub = RemoteGrpc.newBlockingStub(channel);
    }

    public void register(String username, ByteString certificate) {
        while (true) {
            try {
                RegisterRequest request = RegisterRequest.newBuilder().setUsername(username).setCertificate(certificate).build();
                stub.register(request);
                return;
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    try {
                        this.GetPrimaryChannel();
                    } catch (SSLException | ZKNamingException ex) {
                        throw Status.UNAVAILABLE.withDescription("Server is unavailable").asRuntimeException();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public List<String> login(String username, Key privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        while (true) {
            try {
                // Request number
                NumberRequest numberRequest = NumberRequest.newBuilder().setUsername(username).build();
                NumberResponse numberResponse = stub.getNumber(numberRequest);
                String number = numberResponse.getNumber();

                // Cipher number with private key
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, privateKey);
                byte[] cipheredNumber = cipher.doFinal(number.getBytes("UTF8"));

                // Request login token
                TokenRequest tokenRequest = TokenRequest.newBuilder().setUsername(username).setNumber(ByteString.copyFrom(cipheredNumber)).build();
                TokenResponse tokenResponse = stub.getToken(tokenRequest);

                // Set token
                this.token = tokenResponse.getToken();
                this.stub = this.stub.withCallCredentials(new AuthCredentials(this.token));

                return tokenResponse.getInviteList();
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    try {
                        this.GetPrimaryChannel();
                    } catch (SSLException | ZKNamingException ex) {
                        throw Status.UNAVAILABLE.withDescription("Server is unavailable").asRuntimeException();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public int upload(String filename, byte[] signature, String owner) throws IOException {
        while (true) {
            try {
                UploadRequest.Builder request = UploadRequest.newBuilder().setName(filename);
                FileInputStream fis = new FileInputStream("./files/" + filename);
                ByteString bytes = ByteString.readFrom(fis);
                ByteString sig = ByteString.copyFrom(signature);
                fis.close();

                request.setFile(bytes);
                request.setSignature(sig);
                request.setOwner(owner);

                UploadResponse response = stub.upload(request.build());
                return response.getVersion();
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    try {
                        this.GetPrimaryChannel();
                    } catch (SSLException | ZKNamingException ex) {
                        throw Status.UNAVAILABLE.withDescription("Server is unavailable").asRuntimeException();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public JSONObject download(String filename) throws GeneralSecurityException, FileNotFoundException, IOException {
        while (true) {
            try {
                DownloadRequest request = DownloadRequest.newBuilder().setName(filename).build();
                DownloadResponse response = stub.download(request);
                ByteString bytes = response.getFile();
                ByteString signature = response.getSignature();
                ByteString certificate = response.getCertificate();
                String lastModifier = response.getLastModifier();
                int version = response.getVersion();
                String owner = response.getOwner();

                // Verify signature
                byte[] fileBytes = new byte[bytes.size()];
                bytes.copyTo(fileBytes, 0);

                byte[] sigBytes = new byte[signature.size()];
                signature.copyTo(sigBytes, 0);

                byte[] certBytes = new byte[certificate.size()];
                certificate.copyTo(certBytes, 0);

                if (verifySignature(fileBytes, certBytes, sigBytes)) {
                    FileOutputStream fos = new FileOutputStream("./files/" + filename);
                    bytes.writeTo(fos);
                    fos.close();
                    JSONObject file = new JSONObject();
                    file.put("owner", owner);
                    file.put("version", version);
                    file.put("lastModifier", lastModifier);

                    return file;
                } else {
                    return null;
                }
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    try {
                        this.GetPrimaryChannel();
                    } catch (SSLException | ZKNamingException ex) {
                        throw Status.UNAVAILABLE.withDescription("Server is unavailable").asRuntimeException();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public List<Pair<String, ByteString>> remove(String fileName, String username) {
        while (true) {
            try {
                RemoveRequest request = RemoveRequest.newBuilder().setFile(fileName).setUser(username).build();
                RemoveResponse response = stub.remove(request);

                List<Pair<String, ByteString>> list = new ArrayList<Pair<String, ByteString>>();
                for (RemoveResponse.User user : response.getUserList()) {
                    list.add(Pair.of(user.getUsername(), user.getCertificate()));
                }
                return list;
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    try {
                        this.GetPrimaryChannel();
                    } catch (SSLException | ZKNamingException ex) {
                        throw Status.UNAVAILABLE.withDescription("Server is unavailable").asRuntimeException();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public byte[] share(String user) {
        while (true) {
            try {
                ShareRequest request = ShareRequest.newBuilder().setUser(user).build();
                ShareResponse response = stub.share(request);
                ByteString cert = response.getCertificate();
                byte[] certBytes = new byte[cert.size()];
                cert.copyTo(certBytes, 0);

                return certBytes;
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    try {
                        this.GetPrimaryChannel();
                    } catch (SSLException | ZKNamingException ex) {
                        throw Status.UNAVAILABLE.withDescription("Server is unavailable").asRuntimeException();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public void invite(String user, String file, byte[] keyBytes) throws FileNotFoundException, IOException {
        while (true) {
            try {
                InviteRequest.Builder request = InviteRequest.newBuilder();
                ByteString key = ByteString.copyFrom(keyBytes);
                request.setUser(user);
                request.setFile(file);
                request.setKey(key);
                stub.invite(request.build());
                return;
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    try {
                        this.GetPrimaryChannel();
                    } catch (SSLException | ZKNamingException ex) {
                        throw Status.UNAVAILABLE.withDescription("Server is unavailable").asRuntimeException();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public byte[] accept(String fileName) {
        while (true) {
            try {
                AcceptRequest request = AcceptRequest.newBuilder().setFile(fileName).build();
                AcceptResponse response = stub.accept(request);
                ByteString encryptedKey = response.getKey();
                byte[] encryptedKeyBytes = new byte[encryptedKey.size()];
                encryptedKey.copyTo(encryptedKeyBytes, 0);

                return encryptedKeyBytes;
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    try {
                        this.GetPrimaryChannel();
                    } catch (SSLException | ZKNamingException ex) {
                        throw Status.UNAVAILABLE.withDescription("Server is unavailable").asRuntimeException();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    private boolean verifySignature(byte[] file, byte[] cert, byte[] signature) throws GeneralSecurityException, CertificateException {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert));

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(certificate);
        sig.update(file);

        return sig.verify(signature);
    }

    public void close() {
        if (this.channel != null) channel.shutdownNow();
    }

    private void GetPrimaryChannel() throws ZKNamingException, SSLException {
        ZKRecord record = this.zkNaming.lookup(BASE_PATH + "/" + "primary");

        this.channel = NettyChannelBuilder.forTarget(record.getURI()).sslContext(GrpcSslContexts.forClient().trustManager(new File("../TLS/ca-cert.pem")).build()).build();
        if (this.token != null) {
            this.stub = RemoteGrpc.newBlockingStub(channel).withCallCredentials(new AuthCredentials(this.token));
        } else {
            this.stub = RemoteGrpc.newBlockingStub(channel);
        }
    }
}
