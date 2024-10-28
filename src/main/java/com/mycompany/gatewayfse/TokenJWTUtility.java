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

    private static String get(Map<String, String> mapJD, JWTAuthEnum jdk) {
        return mapJD.get(jdk.getKey());
    }

    private static String cleanIss(String iss) {
        if (iss == null) {
            return null;
        }
        return iss.replaceFirst("integrity:", "").replaceFirst("auth:", "");
    }

    public static TokenResponseDTO CreaToken(String tipoProgramma, String cfPaziente, String cfOperatore, Properties properties,ServletContext context) throws Exception {
        //System.out.print(c.getTipo());
        TokenResponseDTO t = buildTokens(tipoProgramma, cfPaziente, cfOperatore, properties, context);

        //System.out.println(t.getFseJwtSignature() + t.getFseJwtSignature());
        return t;
    }

    private static TokenResponseDTO buildTokens(String tipo, String cfPaz, String cfSub, Properties properties,ServletContext context) throws Exception {
        Map<String, String> mapJD = null;
        TokenMap c = new TokenMap();
        TokenMap.ProgramType programType = TokenMap.ProgramType.valueOf(tipo);
        mapJD = c.mappa(programType, cfSub, cfPaz, properties, context);
        switch (tipo) {
            case "LDO" ->
                pwdP12 = "L3tt3ra!".toCharArray();
            case "CERT_VACC_SKIPPER" ->
                pwdP12 = "L3tt3ra!".toCharArray();
            case "SING_VACC_SKIPPER" ->
                pwdP12 = "L3tt3ra!".toCharArray();
            case "SING_VACC_PHTRACK" ->
                pwdP12 = "S1aKeySt0re".toCharArray();
            case "CERT_VACC" ->
                pwdP12 = "S1aKeySt0re".toCharArray();
            case "SING_VACC" ->
                pwdP12 = "S1aKeySt0re".toCharArray();
            default -> {
                TokenResponseDTO t = new TokenResponseDTO(null, null);
                System.err.print("Errore nel tipo di documento");
                return t;
            }
        }
        byte[] privateKeyP12 = Utility.getFileFromFS(get(mapJD, JWTAuthEnum.P12_PATH));
        byte[] pem = Utility.getFileFromFS(get(mapJD, JWTAuthEnum.PEM_PATH));
        byte[] fileToHash = null;
        if (!Utility.nullOrEmpty(pathFileToPublish)) {
            fileToHash = Utility.getFileFromFS(pathFileToPublish);
        }
        return getTokens(mapJD, privateKeyP12, pem, fileToHash);
    }

    private static TokenResponseDTO getTokens(Map<String, String> mapJD, byte[] privateKeyP12, byte[] pem, byte[] fileToHash) throws Exception {

        Security.addProvider((Provider) new BouncyCastleProvider());

        Key privateKey = Utility.extractKeyByAliasFromP12(pwdP12, aliasP12, privateKeyP12);

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
        String jwt = generateAuthJWT(mapJD, privateKey, publicKey, iat, exp, iss);
        String claimsJwt = generateClaimsJWT(mapJD, privateKey, publicKey, iat, exp, iss, fileToHash);
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
