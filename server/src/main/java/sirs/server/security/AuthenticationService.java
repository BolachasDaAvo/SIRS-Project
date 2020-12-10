package sirs.server.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sirs.server.domain.User;
import sirs.server.repository.UserRepository;

import javax.crypto.*;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    private Cache<Integer, String> authCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();

    private SecureRandom secureRandom = new SecureRandom();

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public User registerUser(String username, byte[] certificate) throws AuthenticationException {
        User user = this.userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            throw new AuthenticationException("Username " + username + " is in use.");
        }
        user = new User(username, certificate);
        this.userRepository.save(user);
        return user;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public String getNumber(String username) {
        User user = this.userRepository.findByUsername(username).orElseThrow();
        String number = Integer.toString(secureRandom.nextInt());
        authCache.put(user.getId(), number);
        return number;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public String getToken(String username, byte[] encryptedNumber) throws AuthenticationException {
        User user = this.userRepository.findByUsername(username).orElseThrow();
        String number;
        try {
            number = this.authCache.get(user.getId(), () -> null);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new AuthenticationException("Unknown error");
        }
        if (number == null) {
            throw new AuthenticationException("Number not found");
        }

        X509Certificate userCertificate;
        try {
            userCertificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(user.getCertificate()));
        } catch (CertificateException e) {
            e.printStackTrace();
            throw new AuthenticationException("Unknown error");
        }
        PublicKey userPublicKey = userCertificate.getPublicKey();

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AuthenticationException("Unknown error");
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, userPublicKey);
        } catch (InvalidKeyException e) {
            throw new AuthenticationException("Unknown error");
        }

        String decryptedNumber;
        try {
            decryptedNumber = new String(cipher.doFinal(encryptedNumber), "UTF8");
        } catch (UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException e) {
            throw new AuthenticationException("Unknown error");
        }

        if (!decryptedNumber.equals(number)) {
            throw new AuthenticationException("Numbers do not match");
        }

        authCache.invalidate(user.getId());
        return JwtTokenProvider.generateToken(user.getId());
    }

}
