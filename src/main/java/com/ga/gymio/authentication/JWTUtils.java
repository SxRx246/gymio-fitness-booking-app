package com.ga.gymio.authentication;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides utility methods for generating, parsing, and validating JWT tokens
 * used to authenticate users in the Gymio application.
 */
@Service
public class JWTUtils {

    Logger logger = Logger.getLogger(JWTUtils.class.getName());

    @Value("${jwt-secret}")
    private String jwtSecret;

    @Value("${jwt-expiration-ms}")
    private int jwtExpirationMs;

    /**
     * Generates a JWT token for the specified authenticated user.
     * The token contains the username, issue time, and expiration time.
     *
     * @param myUserDetails the authenticated user's details
     * @return the generated JWT token
     */
    public String generateJwtToken(MyUserDetails myUserDetails){
        return Jwts.builder()
                .setSubject((myUserDetails.getUsername()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    /**
     * Extracts the username stored in a JWT token.
     *
     * @param token the JWT token to parse
     * @return the username stored in the token
     */
    public String getUserNameFromJwtToken(String token){
        return Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * Checks whether a JWT token has a valid signature and can be parsed.
     *
     * @param authToken the JWT token to validate
     * @return true if the token is valid; false if its signature is invalid
     */
    public boolean validateJwtToken(String authToken) {
        try{
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, "Invalid JWT Signature: {0}", e.getMessage());
        }
        return false;
    }
}