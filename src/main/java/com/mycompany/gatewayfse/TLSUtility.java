/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;

/**
 *
 * @author f.matraxia
 */
public class TLSUtility {

    // Carica il certificato dal file .pem
    public static X509Certificate loadCertificateFromFile(File certificateFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(certificateFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }

    // Carica la chiave privata dal file .key
    public static PrivateKey loadPrivateKeyFromFile(File keyFile) throws Exception {
        byte[] keyBytes;
        try (FileInputStream inputStream = new FileInputStream(keyFile)) {
            keyBytes = inputStream.readAllBytes();  // Usa readAllBytes per leggere tutto il file
        }

        String keyBytesStr = new String(keyBytes)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");  // Rimuove tutti gli spazi bianchi e le nuove linee

        // Decodifica la chiave Base64
        byte[] decodedKey = Base64.getDecoder().decode(keyBytesStr);

        // Assumi che la chiave privata sia in formato PKCS#8
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    // Configura SSLContext per utilizzare il certificato client, la chiave privata e il certificato server
    public static SSLContext configureTLS(File crtFile, File keyFile, File serverFile, String keyPassword) throws Exception {
        // Carica i certificati e la chiave privata
        X509Certificate clientCertificate = loadCertificateFromFile(crtFile);
        X509Certificate serverCertificate = loadCertificateFromFile(serverFile);
        PrivateKey privateKey = loadPrivateKeyFromFile(keyFile);

        // Crea un KeyStore e carica la chiave privata e i certificati
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("client", privateKey, keyPassword.toCharArray(), new X509Certificate[]{clientCertificate});
        keyStore.setCertificateEntry("server", serverCertificate);

        // Configura il TrustManagerFactory per utilizzare il KeyStore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        // Crea un SSLContext configurato con il TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }

    // Configura SSLContext per utilizzare il p12
    public static SSLContext configureTLS(String p12FilePath, String keyPassword) throws Exception {
        // Carica il KeyStore dal file .p12
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream keyStoreStream = new FileInputStream(p12FilePath)) {
            keyStore.load(keyStoreStream, keyPassword.toCharArray());
        }

        // Configura il KeyManagerFactory con il KeyStore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyPassword.toCharArray());

        // Configura il TrustManagerFactory con lo stesso KeyStore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);  // Usa il keystore predefinito

        // Ottieni il contesto SSL
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Inizializza il contesto SSL con il KeyManager e il TrustManager
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }

    public static SSLContext prepareSSLContext(Properties properties, String keyStorePath) throws Exception {
        try (InputStream keyStoreStream = new FileInputStream(keyStorePath)) {
            KeyStore clientStore = KeyStore.getInstance("PKCS12");
            
            
            clientStore.load(keyStoreStream, properties.getProperty("ssl.keystore.password").toCharArray());

            return SSLContextBuilder.create()
                    .setProtocol("TLS")
                    .loadKeyMaterial(clientStore, properties.getProperty("ssl.keystore.password").toCharArray())
                    .loadTrustMaterial((chain, authType) -> true) // Questo accetta tutti i certificati
                    .build();
        }
    }

}
