/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import io.jsonwebtoken.Jwts;
import java.security.Key;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author f.matraxia
 */
public class TokenJWTUtility {

    static char[] pwdP12 = null;
    static String pathFileToPublish = null;
    static Integer nHour = 24;
    static String aliasP12 = null;
    private final Logger logger;

    public TokenJWTUtility(Logger logger) {
        this.logger = logger;
    }

    private static String get(Map<String, String> mapJD, JWTAuthEnum jdk) {
        return mapJD.get(jdk.getKey());
    }

    private static String cleanIss(String iss) {
        if (iss == null) {
            return null;
        }
        return iss.replaceFirst("integrity:", "").replaceFirst("auth:", "");
    }

    public TokenResponseDTO CreaToken(String tipoProgramma, String cfPaziente, String cfOperatore, Properties properties, ServletContext context) throws Exception {
        logger.log(Level.INFO, "Creating JWT token for program type: {0}", tipoProgramma);
        TokenResponseDTO t = buildTokens(tipoProgramma, cfPaziente, cfOperatore, properties, context);

        if (t.getFseJwtSignature() == null || t.getAuthorizationBearer() == null) {
            logger.log(Level.WARNING, "JWT token creation failed for program type: {0}", tipoProgramma);
        } else {
            logger.log(Level.INFO, "JWT token successfully created for program type: {0}", tipoProgramma);
        }
        return t;
    }

    private TokenResponseDTO buildTokens(String tipo, String cfPaz, String cfSub, Properties properties, ServletContext context) throws Exception {
        logger.log(Level.FINE, "Building tokens for program type: {0}", tipo);
        Map<String, String> mapJD = null;
        TokenMap c = new TokenMap();
        TokenMap.ProgramType programType = TokenMap.ProgramType.valueOf(tipo);
        mapJD = c.mappa(programType, cfSub, cfPaz, properties, context);
        switch (tipo) {
            case "LDO" -> {
                logger.log(Level.FINE, "Selected password for type LDO");
                pwdP12 = properties.getProperty("p12.Topgate.password").toCharArray();
            }
            case "CERT_VACC_SKIPPER" -> {
                logger.log(Level.FINE, "Selected password for type CERT_VACC_SKIPPER");
                pwdP12 = properties.getProperty("p12.Topgate.password").toCharArray();
            }
            case "SING_VACC_SKIPPER" -> {
                logger.log(Level.FINE, "Selected password for type SING_VACC_SKIPPER");
                pwdP12 = properties.getProperty("p12.Topgate.password").toCharArray();
            }
            case "SING_VACC_PHTRACK" -> {
                logger.log(Level.FINE, "Selected password for type SING_VACC_PHTRACK");
                pwdP12 = properties.getProperty("p12.asl5.password").toCharArray();
            }
            case "CERT_VACC" -> {
                logger.log(Level.FINE, "Selected password for type CERT_VACC");
                pwdP12 =  properties.getProperty("p12.asl5.password").toCharArray();
            }
            case "SING_VACC" -> {
                logger.log(Level.FINE, "Selected password for type SING_VACC");
                pwdP12 =  properties.getProperty("p12.asl5.password").toCharArray();
            }
            default -> {
                logger.log(Level.SEVERE, "Invalid program type: {0}", tipo);
                TokenResponseDTO t = new TokenResponseDTO(null, null);
                System.err.print("Errore nel tipo di documento");
                return t;
            }
        }

        byte[] privateKeyP12 = Utility.getFileFromFS(get(mapJD, JWTAuthEnum.P12_PATH));
        if (privateKeyP12 == null) {
            logger.log(Level.SEVERE, "Failed to load private key from path");
        }

        byte[] pem = Utility.getFileFromFS(get(mapJD, JWTAuthEnum.PEM_PATH));
        if (pem == null) {
            logger.log(Level.SEVERE, "Failed to load PEM from path");
        }

        byte[] fileToHash = null;
        if (!Utility.nullOrEmpty(pathFileToPublish)) {
            fileToHash = Utility.getFileFromFS(pathFileToPublish);
        }
        return getTokens(mapJD, privateKeyP12, pem, fileToHash);
    }

    private TokenResponseDTO getTokens(Map<String, String> mapJD, byte[] privateKeyP12, byte[] pem, byte[] fileToHash) throws Exception {
        logger.log(Level.FINE, "Generating tokens with given data");

        Security.addProvider((Provider) new BouncyCastleProvider());
        logger.log(Level.FINE, "BouncyCastle provider added");

        Key privateKey = Utility.extractKeyByAliasFromP12(pwdP12, aliasP12, privateKeyP12);
        if (privateKey == null) {
            logger.log(Level.SEVERE, "Failed to extract private key");
            throw new Exception("Private key extraction failed");
        }

        String cleanedPEM = new String(pem)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\n", "");

        String publicKey = cleanedPEM;
        String iss = get(mapJD, JWTAuthEnum.ISS);
        Date iat = new Date();

        Date exp = Utility.addHoursToJavaUtilDate(iat, nHour);
        
        logger.log(Level.FINE, "Generating auth JWT");
        String jwt = generateAuthJWT(mapJD, privateKey, publicKey, iat, exp, iss);
        logger.log(Level.FINE, "Auth JWT generated successfully");
        
        logger.log(Level.FINE, "Generating claims JWT");
        String claimsJwt = generateClaimsJWT(mapJD, privateKey, publicKey, iat, exp, iss, fileToHash);
        logger.log(Level.FINE, "Claims JWT generated successfully");
        
        return new TokenResponseDTO(jwt, claimsJwt);
    }

    private static String generateAuthJWT(Map<String, String> mapJD, Key privateKey, String x5c, Date iat, Date exp, String iss) {
        Map<String, Object> headerParams = new HashMap<>();
        headerParams.put(JWTAuthEnum.ALG.getKey(), SignatureAlgorithm.RS256);
        headerParams.put(JWTAuthEnum.TYP.getKey(), JWTAuthEnum.JWT.getKey());
        headerParams.put(JWTAuthEnum.X5C.getKey(), Arrays.asList(x5c).toArray());

        Map<String, Object> claims = new HashMap<>();
        for (JWTAuthEnum k : JWTAuthEnum.values()) {
            if (k.getAutoFlagPayloadClaim() && mapJD.containsKey(k.getKey())) {
                claims.put(k.getKey(), mapJD.get(k.getKey()));
            }
        }
        claims.put(JWTAuthEnum.IAT.getKey(), iat.getTime() / 1000);
        claims.put(JWTAuthEnum.EXP.getKey(), exp.getTime() / 1000);
        claims.put(JWTAuthEnum.ISS.getKey(), "auth:" + cleanIss(iss));

        return Jwts.builder().setHeaderParams(headerParams).setClaims(claims).signWith(SignatureAlgorithm.RS256, privateKey).compact();
    }

    /**
     * Generate Claims JWT.
     *
     * @param mapJD arguments map
     * @param privateKey private key
     * @param x5c public key
     * @param iat issuing time
     * @param exp expiring time
     * @param pathFileToPublish file to hash
     * @return jwt
     * @throws Exception
     */
    private static String generateClaimsJWT(Map<String, String> mapJD, Key privateKey, String x5c, Date iat, Date exp, String iss, byte[] fileToHash) throws Exception {
        Map<String, Object> headerParams = new HashMap<>();
        headerParams.put(JWTClaimsEnum.ALG.getKey(), SignatureAlgorithm.RS256);
        headerParams.put(JWTClaimsEnum.TYP.getKey(), JWTClaimsEnum.JWT.getKey());
        headerParams.put(JWTClaimsEnum.X5C.getKey(), Arrays.asList(x5c).toArray());

        Map<String, Object> claims = new HashMap<>();
        for (JWTClaimsEnum k : JWTClaimsEnum.values()) {
            if (k.getAutoFlagPayloadClaim() && mapJD.containsKey(k.getKey())) {
                claims.put(k.getKey(), mapJD.get(k.getKey()));
            }
        }
        claims.put(JWTClaimsEnum.PATIENT_CONSENT.getKey(), true);
        claims.put(JWTClaimsEnum.IAT.getKey(), iat.getTime() / 1000);
        claims.put(JWTClaimsEnum.EXP.getKey(), exp.getTime() / 1000);
        claims.put(JWTAuthEnum.ISS.getKey(), "integrity:" + cleanIss(iss));

        if (Utility.isPdf(fileToHash)) {
            String hash = Utility.encodeSHA256(fileToHash);
            claims.put(JWTClaimsEnum.ATTACHMENT_HASH.getKey(), hash);
        }

        return Jwts.builder().setHeaderParams(headerParams).setClaims(claims)
                .signWith(SignatureAlgorithm.RS256, privateKey).compact();
    }

}
