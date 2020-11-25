package sirs.client;

import io.grpc.StatusRuntimeException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.Cipher;
import java.security.Key;
import java.security.GeneralSecurityException;

public class ClientApp {

    public static void main(String[] args) throws GeneralSecurityException, FileNotFoundException, IOException {
        System.out.println("Hello World!");

        String host = "localhost";
        int port = 8443;

        ClientFrontend frontend = new ClientFrontend(host, port);
        try {
            upload(frontend, args[0]);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getMessage());
        } finally {
            frontend.close();
        }
        /*
        try {
            frontend.download(args[0]);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getMessage());
        } finally {
            frontend.close();
        }*/

        System.out.println("bye!");
    }

    public static void upload(ClientFrontend frontend, String fileName) throws GeneralSecurityException, FileNotFoundException, IOException {
        Key fileKey;
        if (!new File(fileName + ".key").exists()) {
            fileKey = generateKey(fileName + ".key");
        } else {
            fileKey = readKey(fileName + ".key");
        }

        String encryptedFile = fileName + ".aes";
        transform(fileName, encryptedFile, fileKey, Cipher.ENCRYPT_MODE);

        frontend.upload(encryptedFile);
        new File(fileName).delete();
    }

    public static void download(ClientFrontend frontend, String fileName) throws GeneralSecurityException, FileNotFoundException, IOException {
        if (!new File(fileName + ".key").exists()) {
            System.out.println("No key found for file");
            return;
        }
        Key fileKey = readKey(fileName + ".key");
        String encryptedFile = fileName + ".aes";
        frontend.download(encryptedFile);

        transform(encryptedFile, fileName, fileKey, Cipher.DECRYPT_MODE);
    }

    public static Key generateKey(String keyPath) throws GeneralSecurityException, FileNotFoundException, IOException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        Key key = keyGen.generateKey();
        byte[] keyBytes = key.getEncoded();

        FileOutputStream fos = new FileOutputStream(keyPath);
        fos.write(keyBytes);
        fos.close();

        return new SecretKeySpec(keyBytes, 0, 16, "AES");
    }

    public static Key readKey(String keyPath) throws GeneralSecurityException, FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(keyPath);
        byte[] keyBytes = new byte[fis.available()];
        fis.read(keyBytes);
        fis.close();

        return new SecretKeySpec(keyBytes, 0, 16, "AES");
    }

    public static void transform(String inputFile, String outputFile, Key key, int mode) throws GeneralSecurityException, FileNotFoundException, IOException {
        byte[] inputBytes = Files.readAllBytes(new File(inputFile).toPath());

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(mode, key, new IvParameterSpec(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }));
        byte[] outputBytes = cipher.doFinal(inputBytes);

        Files.write(new File(outputFile).toPath(), outputBytes);
    }
}
