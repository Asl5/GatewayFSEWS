/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 *
 * @author f.matraxia
 */
public class HttpUtility {
    // Metodo per ottenere l'access token, passando l'URL e l'Authorization header come parametri

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static OkHttpClient client;

    // Metodo per configurare il client OkHttp con SSL personalizzato
    private static OkHttpClient getSslClient(SSLContext sslContext, X509TrustManager trustManager) {
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        // Configura il client OkHttp con il socket SSL e il TrustManager
        return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build();
    }

    // Metodo per ottenere il TrustManager da SSLContext
    private static X509TrustManager getTrustManager(SSLContext sslContext) throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);  // Usa il truststore predefinito

        // Cerca un'istanza di X509TrustManager tra i TrustManager
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new IllegalStateException("Nessun X509TrustManager trovato");
    }

    // Metodo per ottenere l'access token
    public static Object getAccessToken(String tokenUrl, String authorizationHeader) throws IOException {
        // Imposta i parametri per la richiesta
        String urlParameters = "grant_type=client_credentials";
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

        // Configura la connessione
        HttpURLConnection connection = createPostConnection(tokenUrl, authorizationHeader, "application/x-www-form-urlencoded", postData);

        // Controlla la risposta
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Deserializza la risposta in TokenResponse
            return objectMapper.readValue(connection.getInputStream(), TokenResponse.class);
        } else {
            // Deserializza la risposta di errore
            return objectMapper.readValue(connection.getErrorStream(), ErrorTokenResponse.class);
        }
    }

    // Metodo generico per richieste POST
    public static Object postRequest(String apiUrl, String authorizationHeader, String jwtAuthorization, String jwtSignature, String contentType, byte[] postData, Class<?> successResponseClass, Class<?> errorResponseClass) throws IOException {
        // Configura la connessione HTTP POST
        HttpURLConnection connection = createPostConnection(apiUrl, authorizationHeader, contentType, postData);
        connection.setRequestProperty("FSE-JWT-Authorization", jwtAuthorization);
        connection.setRequestProperty("FSE-JWT-Signature", jwtSignature);

        // Esegui la richiesta e leggi la risposta
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            // Successo: deserializza la risposta
            return objectMapper.readValue(connection.getInputStream(), successResponseClass);
        } else {
            // Errore: deserializza la risposta di errore
            return objectMapper.readValue(connection.getErrorStream(), errorResponseClass);
        }
    }

    // Metodo per creare la connessione POST
    private static HttpURLConnection createPostConnection(String apiUrl, String authorizationHeader, String contentType, byte[] postData) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", authorizationHeader);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.getOutputStream().write(postData);
        return connection;
    }

    // Metodo per inviare una richiesta multipart/form-data
    public static Object postMultipartRequest(String apiUrl, String accessToken, String jwtAuthorization, String jwtSignature, File file, String requestBodyJson, Class<?> successResponseClass, Class<?> errorResponseClass, SSLContext sslContext) throws IOException, Exception {


        X509TrustManager trustManager = getTrustManager(sslContext);
        client = getSslClient(sslContext, trustManager);

// Crea il contenuto multipart
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("requestBody", requestBodyJson)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse(Files.probeContentType(file.toPath()))))
                .build();

        // Costruisci la richiesta
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("FSE-JWT-Authorization", jwtAuthorization)
                .addHeader("FSE-JWT-Signature", jwtSignature)
                .addHeader("Accept", "application/json")
                .post(requestBody)
                .build();

        // Esegui la richiesta
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                // Deserializza la risposta di successo
                return new ObjectMapper().readValue(response.body().string(), successResponseClass);
            } else {
                // Deserializza la risposta di errore
                return new ObjectMapper().readValue(response.body().string(), errorResponseClass);
            }
        }
    }

}
