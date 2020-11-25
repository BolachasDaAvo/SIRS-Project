package sirs.server.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    public static PrivateKey privateKey;

    public static PublicKey publicKey;

    private static SignatureAlgorithm algorithm = SignatureAlgorithm.RS256;

    public static String generateToken(int id) {
        return Jwts.builder().claim("id", id).setIssuedAt(new Date()).signWith(privateKey).compact();
    }

    public static int extractId(String token) {
        return (Integer) Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody().get("id");
    }
}
