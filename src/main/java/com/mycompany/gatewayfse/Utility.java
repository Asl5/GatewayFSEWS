/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;

/**
 *
 * @author f.matraxia
 */
public class Utility {

    private static Logger logger = null;
    private static boolean isLoggerInitialized = false;
    /**
     * Chunk size file.
     */
    private static final int CHUNK_SIZE = 16384;

    /**
     * Initializes the logger with a single FileHandler.
     *
     * @param className the name of the class where the logger is used
     * @param properties the properties containing the log file path
     */
    public static synchronized void InizializeLogger(Properties properties, String className) {
        if (isLoggerInitialized) {
            return; // Avoid reinitialization
        }

        logger = Logger.getLogger(className);
        logger.setUseParentHandlers(false); // Disable parent handlers

        try {
            // Rimuovi gli handler esistenti per evitare più file di log
            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
                handler.close();
            }

            // Estrai solo il nome della classe senza il package
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

            // Configura il nome del file con la data corrente
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String date = dtf.format(LocalDateTime.now());

            String logFilePath = properties.getProperty("log.path") + date + "-" + simpleClassName + ".log";

            // Configura il FileHandler con rotazione basata sulla dimensione del file
            Handler fileHandler = new FileHandler(logFilePath, 10485760, 5, true);
            fileHandler.setFormatter(new SimpleFormatter());

            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);

            isLoggerInitialized = true;
            logger.log(Level.INFO, "Logger initialized for {0}", simpleClassName);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize logger", e);
        }
    }

    /**
     * Returns the logger instance.
     *
     * @return the initialized logger
     */
    public static Logger getLogger() {
        if (!isLoggerInitialized) {
            throw new IllegalStateException("Logger not initialized. Call initializeLogger first.");
        }
        return logger;
    }

    /**
     * Metodo per gestire le eccezioni e restituire una risposta JSON.
     *
     * @param response HttpServletResponse
     * @param e Exception l'eccezione che si è verificata
     * @return
     * @throws IOException
     */
    public static ErrorResponse handleException(HttpServletResponse response, Exception e) throws IOException {
        // Log dell'eccezione
        logger.log(Level.SEVERE, "Errore: {0}", e.getMessage());

        // Imposta il tipo di risposta JSON e lo stato HTTP
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        // Creazione di un oggetto JSON per l'errore
        return new ErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * Metodo per gestire gli errori di bad request (400) e restituire una
     * risposta JSON.
     *
     * @param response HttpServletResponse
     * @param message String il messaggio di errore da restituire
     * @return
     * @throws IOException
     */
    public static ErrorResponse handleBadRequest(HttpServletResponse response, String message) throws IOException {
        // Log dell'errore
        logger.log(Level.WARNING, "Bad Request: {0}", message);

        // Imposta il tipo di risposta JSON e lo stato HTTP
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // Creazione di un oggetto JSON per l'errore
        return new ErrorResponse(HttpServletResponse.SC_BAD_REQUEST, message);
    }

    /**
     * Get file from file system.
     *
     * @param filename	path file
     * @return	content of the file
     * @throws Exception
     */
    public static byte[] getFileFromFS(final String filename) throws Exception {
        byte[] b = null;
        try (InputStream is = new FileInputStream(new File(filename))) {
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();) {
                int nRead;
                byte[] data = new byte[CHUNK_SIZE];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                b = buffer.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while retrieving file from fs: %s", e);
        }
        return b;
    }

    /**
     * Get key from P12 from alias and password.
     *
     * @param password	p12 password
     * @param alias	p12 alias
     * @param p12	certificate
     * @return	key
     * @throws Exception
     */
    public static Key extractKeyByAliasFromP12(char[] password, String alias, byte[] p12) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(p12)) {
            java.security.KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
            for (Provider provider : Security.getProviders()) {
                System.out.println(provider.getName());
            }
            keyStore.load(bais, password);
            //If no alias is specified try to find a key and return the first found
            if (nullOrEmpty(alias)) {
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String a = aliases.nextElement();
                    if (keyStore.isKeyEntry(a)) {
                        //LOGGER.info(() -> String.format("Using alias: %s", a));
                        return keyStore.getKey(a, password);
                    }
                }
            } else {
                return keyStore.getKey(alias, password);
            }
        } catch (Exception e) {

            logger.log(Level.SEVERE, "Error while extracting key by alias from p12: %s", e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Add hours to a date.
     *
     * @param date	date
     * @param hours	hours to add
     * @return	updated date
     */
    public static Date addHoursToJavaUtilDate(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }

    /**
     * Check if a string is null or empty.
     *
     * @param str	string to check
     * @return	flag
     */
    public static boolean nullOrEmpty(String str) {
        return (str == null) || str.length() == 0;
    }

    /**
     * Check if the magic number of the file is "%PDF".
     *
     * @param pdf	file to check
     * @return	flag
     */
    public static boolean isPdf(byte[] pdf) {
        boolean out = false;
        if (pdf != null && pdf.length > 4) {
            byte[] magicNumber = Arrays.copyOf(pdf, 4);
            String strMagicNumber = new String(magicNumber);
            out = strMagicNumber.equals("%PDF");
        }
        return out;
    }

    /**
     * Encode object in sha 256 and then in hexadecimal.
     *
     * @param objectToEncode	object to encode
     * @return	encoded object
     * @throws NoSuchAlgorithmException
     */
    public static String encodeSHA256(byte[] objectToEncode) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Hex.encodeHexString(digest.digest(objectToEncode));
    }

    /**
     * Logs an informational message and stores it in the database.
     *
     * @param logger The Logger object to log the message.
     * @param repository The Repository object used to log to the database.
     * @param message The informational message to be logged.
     * @param servletName The name of the servlet logging the message.
     * @param logString Additional log information to be stored in the database.
     */
    public static void logInfo(Logger logger, Repository repository, String message, String servletName, String logString) {
        logger.log(Level.INFO, message);
        repository.LogDB("INFO", message, servletName, "ITCURA", logString);
    }

    /**
     * Logs an error message and stores it in the database.
     *
     * @param logger The Logger object to log the message.
     * @param repository The Repository object used to log to the database.
     * @param message The informational message to be logged.
     * @param servletName The name of the servlet logging the message.
     * @param logString Additional log information to be stored in the database.
     */
    public static void logError(Logger logger, Repository repository, String message, String servletName, String logString) {
        logger.log(Level.SEVERE, message);
        repository.LogDB("ERROR", message, servletName, "ITCURA", logString);
    }
}
