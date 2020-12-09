package sirs.client;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;
import java.io.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyPairGenerator;
import java.security.KeyFactory;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class SecurityLogic {

    public static void createKeystore(String username, char[] password, PrivateKey privKey, Certificate cert) throws IOException, FileNotFoundException, GeneralSecurityException, KeyStoreException {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null, null);
        
        keystore.setKeyEntry("auth", privKey, password, new Certificate[] {cert});

        FileOutputStream fos = new FileOutputStream(username + ".ks");
        keystore.store(fos, password);
        fos.close();
    }

    public static Pair<PrivateKey, Certificate> generateCertificate() throws GeneralSecurityException, OperatorCreationException {
        String bc = BouncyCastleProvider.PROVIDER_NAME;
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", bc);
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        X500Name dnName = new X500Name("CN=sirs");
        BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());
        String signatureAlgorithm = "SHA256WithRSA";
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
            .build(keyPair.getPrivate());
        Instant startDate = Instant.now();
        Instant endDate = startDate.plus(2 * 365, ChronoUnit.DAYS);
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            dnName, certSerialNumber, Date.from(startDate), Date.from(endDate), dnName,
            keyPair.getPublic());
        Certificate certificate = new JcaX509CertificateConverter().setProvider(bc)
            .getCertificate(certBuilder.build(contentSigner));
        return Pair.of(keyPair.getPrivate(), certificate);
    }

    // Generates AES key and stores in keystore
    public static Key generateAESKey(String username, char[] password, String alias) throws GeneralSecurityException, FileNotFoundException, IOException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        Key key = keyGen.generateKey();

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fis = new FileInputStream(username + ".ks");
        keystore.load(fis, password);
        keystore.setKeyEntry(alias, key, password, null);
        fis.close();

        FileOutputStream fos = new FileOutputStream(username + ".ks");
        keystore.store(fos, password);
        fos.close();

        return key;
    }

    // Reads key from keystore
    public static Key getKey(String username, char[] password, String alias) throws GeneralSecurityException, FileNotFoundException, IOException {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fis = new FileInputStream(username + ".ks");
        keystore.load(fis, password);
        fis.close();

        Key key = keystore.getKey(alias, password);
        return key;
    }

    // Store key in keystore
    public static void saveKey(String username, char[] password, String alias, Key key) throws GeneralSecurityException, FileNotFoundException, IOException {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fis = new FileInputStream(username + ".ks");
        keystore.load(fis, password);
        keystore.setKeyEntry(alias, key, password, null);
        fis.close();

        FileOutputStream fos = new FileOutputStream(username + ".ks");
        keystore.store(fos, password);
        fos.close();

    }

    // AES ecnrypt/decrypt function
    public static void transform(String inputFile, String outputFile, Key key, int mode, byte[] iv) throws GeneralSecurityException, FileNotFoundException, IOException {
        byte[] inputBytes = Files.readAllBytes(new File(inputFile).toPath());

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(mode, key, new IvParameterSpec(iv));
        byte[] outputBytes = cipher.doFinal(inputBytes);

        Files.write(new File(outputFile).toPath(), outputBytes);
    }

    public static byte[] sign(String fileName, PrivateKey key) throws IOException, GeneralSecurityException {
        byte[] inputBytes = Files.readAllBytes(new File(fileName).toPath());
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(inputBytes);

        return signature.sign();
    }

    public static byte[] usernameToIV(String username) {
        String holder = new String(username);

        while (holder.length() < 16)
            holder = holder.concat(username);

        return holder.substring(0, 16).getBytes();
    }
}