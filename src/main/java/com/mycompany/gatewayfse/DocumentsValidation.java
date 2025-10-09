/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.gatewayfse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 *
 * @author f.matraxia
 */
@MultipartConfig  // Abilita il supporto per multipart/form-data
public class DocumentsValidation extends HttpServlet {

    private final Properties properties = new Properties();
    Logger logger;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static Repository rep;
    static TokenJWTUtility tu;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String servletName = request.getServletPath();
        String logString = null;

        Object result = null;
        File file = null;
        ServletContext context = getServletContext();

        Utility.logInfo(logger, rep, "METHOD START", servletName, logString);

        try {
            // Supponiamo che l'URL e l'Authorization Header siano già stati letti dalla servlet
            String tokenUrl = properties.getProperty("token.url");
            String authorizationHeader = properties.getProperty("authorization.header");
            String urlValidations = properties.getProperty("url.validation");

            // Estrai i nuovi parametri dalla richiesta
            // Ottieni i parametri dalla richiesta multipart
            Part cfPazientePart = request.getPart("cf_paziente");
            Part cfOperatorePart = request.getPart("cf_operatore");
            Part tipoProgrammaPart = request.getPart("tipo_programma");

            // Estrai i valori come stringhe dai parametri
            String codiceFiscalePaziente = new String(cfPazientePart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String codiceFiscaleOperatore = new String(cfOperatorePart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String tipoProgramma = new String(tipoProgrammaPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Estrai il file PDF dal form
            Part filePart = request.getPart("file"); // "file" è il nome dell'input nel form HTML

            // Validazione dei parametri in input
            if (codiceFiscalePaziente == null || codiceFiscalePaziente.isEmpty()
                    || codiceFiscaleOperatore == null || codiceFiscaleOperatore.isEmpty()
                    || tipoProgramma == null || tipoProgramma.isEmpty()
                    || filePart == null) {
                // Se uno dei parametri è mancante o invalido, restituisci un errore 400
                ErrorResponse errorResponse = Utility.handleBadRequest(response, "Parametri mancanti o invalidi");
                Utility.logError(logger, rep, "Parametri mancanti o invalidi", servletName, logString);
                // Restituzione del JSON
                objectMapper.writeValue(response.getWriter(), errorResponse);
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // ottieni solo il nome del file
            file = new File(System.getProperty("java.io.tmpdir") + "/" + fileName); // Salva temporaneamente il file

            // Scrivi i dati del file sul filesystem temporaneo
            Files.copy(filePart.getInputStream(), file.toPath());

            logString = String.format("CF_PAZIENTE=%s;CF_OPERATORE=%s;TIPO_PROGRAMMA=%s; FILE=%s",
                    codiceFiscalePaziente, codiceFiscaleOperatore, tipoProgramma, fileName);

            // Simuliamo il JSON del requestBody
            String requestBodyJson = "{\"healthDataFormat\": \"CDA\", \"mode\": \"ATTACHMENT\", \"activity\": \"VALIDATION\"}";

            
            try {

                Utility.logInfo(logger, rep, "Richiedo Access Token...", servletName, logString);
                // Richiama la funzione per ottenere l'access token o l'errore
                result = HttpUtility.getAccessToken(tokenUrl, authorizationHeader);

                // Imposta il content type della risposta
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                // Controlla il tipo di risposta (successo o errore) e scrivi la risposta JSON
                switch (result) {
                    //case TokenResponse tokenResponse -> response.getWriter().write(new ObjectMapper().writeValueAsString(tokenResponse));
                    case ErrorTokenResponse errorResponse -> {
                        Utility.logError(logger, rep, errorResponse.error_description, servletName, logString);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
                        return; // Termina qui se c'è un errore di autenticazione
                    }
                    default -> {
                    }
                }
            } catch (IOException e) {
                //response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante l'ottenimento del token: " + e.getMessage());
                //Utility.logError(logger, rep, e.getLocalizedMessage(), servletName, logString);
                Exception ex = new Exception("Errore nell'ottenimento del token: " + e.getLocalizedMessage());
                ErrorResponse errorResponse = Utility.handleException(response, ex);
                // Restituzione del JSON
                objectMapper.writeValue(response.getWriter(), errorResponse);
                return; // Termina l'elaborazione se c'è un'eccezione
            }
            
            
            Utility.logInfo(logger, rep, "Access Token ottenuto", servletName, logString);

            // Access token e JWT
            //String accessToken = "Bearer " + ((TokenResponse)result).access_token;
            Utility.logInfo(logger, rep, "Gerazione dei JWT...", servletName, logString);
            TokenResponseDTO jwtToken = tu.CreaToken(tipoProgramma, codiceFiscalePaziente, codiceFiscaleOperatore, properties, context);
            String fseJwtAuthorization = jwtToken.getAuthorizationBearer();
            String fseJwtSignature = jwtToken.getFseJwtSignature();
            Utility.logInfo(logger, rep, "JWT generati", servletName, logString);

            if (result instanceof TokenResponse tokenResponse) {
                String relativePathP12 = properties.getProperty("path.FileP12");

                String absolutePathP12 = context.getRealPath(relativePathP12);
//                // Carica i certificati e la chiave privata (come descritto prima)
//                File crtFile = new File(absolutePathP12);
//                File keyFile = new File("C:\\Users\\f.matraxia\\Documents\\FSE regionale\\FSE Dati\\Certificati Postman\\prova.key");
//                File serverFile = new File("C:\\Users\\f.matraxia\\Documents\\FSE regionale\\FSE Dati\\Certificati Postman\\modipa-val.fse.salute.gov.der");

                //SSLContext sslContext = TLSUtility.configureTLS(crtFile, keyFile, serverFile, "L3tt3ra!");
                Utility.logInfo(logger, rep, "Configuro il contesto SSL", servletName, logString);
                // Configura il contesto SSL
                SSLContext sslContext = TLSUtility.prepareSSLContext(properties, absolutePathP12);//TLSUtility.configureTLS(absolutePathP12, properties.getProperty("passwordTLS"));

                Utility.logInfo(logger, rep, "Invocazione dell'endpoint " + urlValidations, servletName, logString);
                // Esegui la richiesta utilizzando HttpUtility
                Object resultValidation = HttpUtility.postMultipartRequest(urlValidations,
                        tokenResponse.access_token, // Rimuovi il prefisso "Bearer "
                        fseJwtAuthorization,
                        fseJwtSignature,
                        file,
                        requestBodyJson,
                        ValidationResDTO.class,
                        ValidationErrorResponseDTO.class,
                        sslContext
                );

                // Restituisci la risposta in formato JSON
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                if (resultValidation instanceof ValidationResDTO) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    Utility.logInfo(logger, rep, "L'endpoint ha risposto: " + resultValidation.toString(), servletName, logString);
                    objectMapper.writeValue(response.getWriter(), resultValidation);
                } else if (resultValidation instanceof ValidationErrorResponseDTO) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    Utility.logInfo(logger, rep, "L'endpoint ha risposto: " + resultValidation.toString(), servletName, logString);
                    objectMapper.writeValue(response.getWriter(), resultValidation);
                }
            }
        } catch (Exception ex) {
            Utility.logError(logger, rep, ex.getLocalizedMessage(), servletName, logString);
            // Gestione dell'eccezione non gestita
            ErrorResponse errorResponse = Utility.handleException(response, ex);
            // Restituzione del JSON
            objectMapper.writeValue(response.getWriter(), errorResponse);
        } finally {
            // Elimina il file temporaneo dopo la richiesta
            if (file != null) {
                Files.deleteIfExists(file.toPath());
            }

            Utility.logInfo(logger, rep, "METHOD END", servletName, logString);
        }
    }

    /**
     *
     * inizializzazione della servlet, carica le configurazioni e il logger
     *
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new ServletException("Sorry, unable to find config.properties");
            }
            properties.load(input);

            Utility.initializeLogger(properties, this.getClass().getName());
            logger = Utility.getLogger();
            rep = new Repository(logger);
            tu = new TokenJWTUtility(logger);
        } catch (IOException ex) {
            throw new ServletException("Error loading configuration", ex);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    @Operation(summary = "Handle GET request", description = "Restituisce un messaggio di benvenuto tramite una richiesta GET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Operation(summary = "Handle POST request", description = "Restituisce un messaggio di benvenuto tramite una richiesta POST")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
