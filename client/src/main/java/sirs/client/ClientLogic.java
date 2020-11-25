package sirs.client;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class ClientLogic {

    ClientFrontend frontend;

    public ClientLogic(String host, int port) throws SSLException {
        this.frontend = new ClientFrontend(host, port);
    }

    public void register(String username, String certificatePath) {
        try {
            ByteString certificate = ByteString.copyFrom(new FileInputStream(certificatePath).readAllBytes());
            frontend.register(username, certificate);
            System.out.println("User registered successfully");
        } catch (IOException | StatusRuntimeException e) {
            System.out.println("Unable to register: " + e.getMessage());
        }
    }

    public void login(String username, String privateKeyPath) {
        try {
            PrivateKey privateKey = this.readPrivateKey(privateKeyPath);
            this.frontend.login(username, privateKey);
            System.out.println("User logged in successfully");
        } catch (StatusRuntimeException | IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidKeySpecException | IllegalBlockSizeException | BadPaddingException e) {
            System.out.println("Unable to login: " + e.getMessage());
        }
    }

    private X509Certificate readX509Certificate(String certificatePath) throws FileNotFoundException, CertificateException {
        FileInputStream is = new FileInputStream(certificatePath);
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        return certificate;
    }

    private PrivateKey readPrivateKey(String keyPath) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        FileInputStream is = new FileInputStream(keyPath);
        PrivateKey privKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(is.readAllBytes()));
        return privKey;
    }

    public void upload(String fileName) throws GeneralSecurityException, IOException {
        Key fileKey;
        if (!new File(fileName + ".key").exists()) {
            fileKey = this.generateKey(fileName + ".key");
        } else {
            fileKey = this.readKey(fileName + ".key");
        }

        String encryptedFile = fileName + ".aes";
        this.transform(fileName, encryptedFile, fileKey, Cipher.ENCRYPT_MODE);

        this.frontend.upload(encryptedFile);
        new File(fileName).delete();
    }

    public void download(String fileName) throws GeneralSecurityException, IOException {
        if (!new File(fileName + ".key").exists()) {
            System.out.println("No key found for file");
            return;
        }
        Key fileKey = this.readKey(fileName + ".key");
        String encryptedFile = fileName + ".aes";
        this.frontend.download(encryptedFile);

        this.transform(encryptedFile, fileName, fileKey, Cipher.DECRYPT_MODE);
    }

    private Key generateKey(String keyPath) throws GeneralSecurityException, FileNotFoundException, IOException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        Key key = keyGen.generateKey();
        byte[] keyBytes = key.getEncoded();

        FileOutputStream fos = new FileOutputStream(keyPath);
        fos.write(keyBytes);
        fos.close();

        return new SecretKeySpec(keyBytes, 0, 16, "AES");
    }

    private Key readKey(String keyPath) throws GeneralSecurityException, FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(keyPath);
        byte[] keyBytes = new byte[fis.available()];
        fis.read(keyBytes);
        fis.close();

        return new SecretKeySpec(keyBytes, 0, 16, "AES");
    }

    private void transform(String inputFile, String outputFile, Key key, int mode) throws GeneralSecurityException, FileNotFoundException, IOException {
        byte[] inputBytes = Files.readAllBytes(new File(inputFile).toPath());

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(mode, key, new IvParameterSpec(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}));
        byte[] outputBytes = cipher.doFinal(inputBytes);

        Files.write(new File(outputFile).toPath(), outputBytes);
    }

    public void close() {
        this.frontend.close();
    }
}
