package sirs.client;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import org.json.simple.*;
import org.json.simple.parser.*;

public class ClientLogic {

    ClientFrontend frontend;
    String username = "";
    JSONObject cache;

    public ClientLogic(String host, int port) throws SSLException, IOException, ParseException {
        this.frontend = new ClientFrontend(host, port);
        try {
            FileInputStream fis = new FileInputStream("fileCache.json");
            this.cache = (JSONObject) new JSONParser().parse(new FileReader("fileCache.json"));
        } catch (FileNotFoundException e) {
            this.cache = new JSONObject();
        }
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
            List<String> invites = this.frontend.login(username, privateKey);
            System.out.println("User logged in successfully");
            this.username = username;
            
            for (String invite : invites) {
                String filename = invite.substring(0, invite.lastIndexOf('.'));
                System.out.println("You have been invited to edit " + filename);
            }
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

    public void unlock(String fileName) throws GeneralSecurityException, IOException {
        String encryptedFile = "./files/" + fileName + ".aes";
        Key fileKey;
        JSONObject file = (JSONObject) this.cache.get(fileName);
        String lastModifier;

        if (file == null) {
            System.out.println("Unable to unlock: File not in cache");
            return;
        } else {
            lastModifier = (String) file.get("lastModifier");
        }

        if (!new File(fileName + ".key").exists()) {
            System.out.println("Unable to unlock: No key present");
            return;
        } else {
            fileKey = this.readKey(fileName + ".key");
        }

        byte[] iv = usernameToIV(lastModifier);
        try {
            this.transform(encryptedFile, fileName, fileKey, Cipher.DECRYPT_MODE, iv);
        } catch (NoSuchFileException e) {
            System.out.println("Unable to unlock: " + e.getMessage());
            return;
        }
        System.out.println("File unlocked");
    }

    public void upload(String fileName) throws GeneralSecurityException, IOException {
        Key fileKey;
        JSONObject file = (JSONObject) this.cache.get(fileName);

        if (this.username.equals("")) {
            System.out.println("User not logged in");
            return;
        }

        if (!new File(fileName + ".key").exists()) {
            fileKey = this.generateKey(fileName + ".key");
        } else {
            fileKey = this.readKey(fileName + ".key");
        }

        java.io.File directory = new java.io.File("./files/");
        if (!directory.exists()) {
            directory.mkdir();
        }

        // Encrypt the file
        String encryptedFile = "./files/" + fileName + ".aes";
        byte[] iv = usernameToIV(this.username);
        try {
            this.transform(fileName, encryptedFile, fileKey, Cipher.ENCRYPT_MODE, iv);
        } catch (NoSuchFileException e) {
            System.out.println("Unable to upload: " + e.getMessage());
            return;
        }

        // Sign the file
        PrivateKey signKey = readPrivateKey("keys/key_pkcs8.key");
        byte[] signature = sign(encryptedFile, signKey);

        String owner;
        // Get file owner
        if (file == null) {
            owner = this.username;
        } else {
            owner = (String) file.get("owner");
        }

        int version;
        try {
            version = this.frontend.upload(fileName + ".aes", signature, owner);
        } catch(Exception e) {
            System.out.println("Unable to upload: " + e.getMessage());
            return;
        }

        // Update cache
        if (file == null) {
            file = new JSONObject();
            file.put("owner", this.username);
            file.put("version", 1);
            file.put("lastModifier", this.username);
        } else {
            file.put("version", version);
            file.put("lastModifier", this.username);
        }
        this.cache.put(fileName, file);

        new File(fileName).delete();
        System.out.println("File uploaded");
    }

    public void download(String fileName) throws GeneralSecurityException, IOException {
        if (!new File(fileName + ".key").exists()) {
            System.out.println("No key found for file");
            return;
        }

        java.io.File directory = new java.io.File("./files/");
        if (!directory.exists()) {
            directory.mkdir();
        }

        Key fileKey = this.readKey(fileName + ".key");
        String encryptedFile = fileName + ".aes";
        JSONObject file;
        try {
            file = this.frontend.download(encryptedFile);
        } catch (Exception e) {
            System.out.println("Unable to download: " + e.getMessage());
            return;
        }
        if(file != null) {
            String lastModifier = (String) file.get("lastModifier");
            System.out.println("Signature verified");
            byte[] iv = usernameToIV(lastModifier);
            this.transform("./files/" + encryptedFile, fileName, fileKey, Cipher.DECRYPT_MODE, iv);

            // Update cache
            this.cache.put(fileName, file);
        } else
            System.out.println("File was tampered. Try again later or upload your own version");

    }

    public void invite(String username, String fileName) throws GeneralSecurityException, IOException {
        byte[] certBytes;
        try {
            certBytes = this.frontend.share(username);
        } catch (Exception e) {
            System.out.println("Unable to get users certificate: " + e.getMessage());
            return;
        }
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes));

        if (!new File(fileName + ".key").exists()) {
            System.out.println("No key found for file");
            return;
        }

        byte[] fileKey = Files.readAllBytes(new File(fileName + ".key").toPath());
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, certificate);
        byte[] keyBytes = cipher.doFinal(fileKey);

        String encryptedFile = fileName + ".aes";
        try {
            this.frontend.invite(username, encryptedFile, keyBytes);
        } catch (Exception e) {
            System.out.println("Unable to send invite: " + e.getMessage());
            return;
        }
        System.out.println("Invite sent");
    }

    public void accept(String fileName) throws GeneralSecurityException, FileNotFoundException, IOException {
        byte[] encryptedKeyBytes;
        try {
            encryptedKeyBytes = this.frontend.accept(fileName + ".aes");
        } catch (Exception e) {
            System.out.println("Unable to accept invite: " + e.getMessage());
            return;
        }
        PrivateKey key = readPrivateKey("keys/key_pkcs8.key");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] keyBytes = cipher.doFinal(encryptedKeyBytes);

        FileOutputStream fos = new FileOutputStream(fileName + ".key");
        fos.write(keyBytes);
        fos.close();
        System.out.println("Invite accepted");
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

    private void transform(String inputFile, String outputFile, Key key, int mode, byte[] iv) throws GeneralSecurityException, FileNotFoundException, IOException {
        byte[] inputBytes = Files.readAllBytes(new File(inputFile).toPath());

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(mode, key, new IvParameterSpec(iv));
        byte[] outputBytes = cipher.doFinal(inputBytes);

        Files.write(new File(outputFile).toPath(), outputBytes);
    }

    private byte[] sign(String fileName, PrivateKey key) throws IOException, GeneralSecurityException {
        byte[] inputBytes = Files.readAllBytes(new File(fileName).toPath());
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(inputBytes);

        return signature.sign();
    }

    private byte[] usernameToIV(String username) {
        String holder = new String(username);

        while (holder.length() < 16)
            holder = holder.concat(username);

        return holder.substring(0, 16).getBytes();
    }

    public void close() throws IOException {
        FileOutputStream fos = new FileOutputStream("fileCache.json");
        fos.write(this.cache.toString().getBytes());
        this.frontend.close();
    }
}
