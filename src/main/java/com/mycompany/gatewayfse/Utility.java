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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
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

    //private static Logger logger = null;
    private static boolean isLoggerInitialized = false;

    private static Logger logger;
    private static LocalDate currentDate;
    private static FileHandler fileHandler;
    private static ScheduledExecutorService scheduler;
    private static boolean isDebugMode;
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
//    public static synchronized void InizializeLogger(Properties properties, String className) {
//        if (isLoggerInitialized) {
//            return; // Avoid reinitialization
//        }
//
//        logger = Logger.getLogger(className);
//        logger.setUseParentHandlers(false); // Disable parent handlers
//
//        try {
//            // Rimuovi gli handler esistenti per evitare più file di log
//            for (Handler handler : logger.getHandlers()) {
//                logger.removeHandler(handler);
//                handler.close();
//            }
//
//            // Estrai solo il nome della classe senza il package
//            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
//
//            // Configura il nome del file con la data corrente
//            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//            String date = dtf.format(LocalDateTime.now());
//
//            String logFilePath = properties.getProperty("log.path") + date + "-" + simpleClassName + ".log";
//
//            // Configura il FileHandler con rotazione basata sulla dimensione del file
//            Handler fileHandler = new FileHandler(logFilePath, 10485760, 5, true);
//            fileHandler.setFormatter(new SimpleFormatter());
//
//            logger.addHandler(fileHandler);
//            logger.setLevel(Level.ALL);
//
//            isLoggerInitialized = true;
//            logger.log(Level.INFO, "Logger initialized for {0}", simpleClassName);
//
//        } catch (IOException e) {
//            logger.log(Level.SEVERE, "Failed to initialize logger", e);
//        }
//    }
    /**
     * Inizializza il logger per una determinata classe con configurazioni da un
     * file di proprietà.
     *
     * @param properties Le proprietà caricate dal file di configurazione.
     * @param className Il nome completo della classe per cui inizializzare il
     * logger.
     */
    public static synchronized void initializeLogger(Properties properties, String className) {
        if (logger != null) {
            return; // Evita la reinizializzazione
        }

        logger = Logger.getLogger(className);
        logger.setUseParentHandlers(false);

        // Verifica se l'applicazione è in modalità debug
        isDebugMode = Boolean.parseBoolean(properties.getProperty("debug.mode", "false"));
        Level logLevel = isDebugMode ? Level.FINE : Level.INFO;

        try {
            currentDate = LocalDate.now();
            setupFileHandler(properties, className, currentDate, logLevel);

            // Avvia un scheduler per aggiornare il file di log ogni giorno
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                LocalDate newDate = LocalDate.now();
                if (!newDate.equals(currentDate)) {
                    currentDate = newDate;
                    updateFileHandler(properties, className, currentDate, logLevel);
                }
            }, 1, 1, TimeUnit.DAYS);
            isLoggerInitialized = true;
            logger.log(Level.INFO, "Logger initialized for {0} in {1} mode", new Object[]{className, isDebugMode ? "DEBUG" : "PRODUCTION"});

        } catch (IOException e) {
            System.err.println("Failed to initialize logger for " + className + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Configura un nuovo FileHandler per il logger con il nome del file basato
     * sulla data corrente.
     *
     * @param properties Le proprietà caricate dal file di configurazione.
     * @param className Il nome completo della classe per cui configurare il
     * logger.
     * @param date La data corrente per creare il nome del file di log.
     * @param logLevel Il livello di log da impostare.
     * @throws IOException Se si verifica un errore durante la creazione del
     * file di log.
     */
    private static void setupFileHandler(Properties properties, String className, LocalDate date, Level logLevel) throws IOException {
        // Rimuovi l'handler esistente se presente
        if (fileHandler != null) {
            logger.removeHandler(fileHandler);
            fileHandler.close();
        }

        // Estrai solo il nome della classe senza il package
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String logDirectoryPath = properties.getProperty("log.path");

        if (logDirectoryPath == null || logDirectoryPath.isEmpty()) {
            throw new IOException("Log path is not defined in properties.");
        }

        // Crea la directory di log se non esiste
        Files.createDirectories(Paths.get(logDirectoryPath));

        String logFilePath = logDirectoryPath + "/" + dateStr + "-" + simpleClassName + ".log";
        fileHandler = new FileHandler(logFilePath, 10485760, 5, true);
        fileHandler.setFormatter(new SimpleFormatter());
        fileHandler.setLevel(logLevel);

        logger.addHandler(fileHandler);
        logger.setLevel(logLevel);
    }

    /**
     * Aggiorna il FileHandler per il logger quando la data cambia.
     *
     * @param properties Le proprietà caricate dal file di configurazione.
     * @param className Il nome completo della classe per cui aggiornare il
     * logger.
     * @param newDate La nuova data per creare un nuovo file di log.
     * @param logLevel Il livello di log da impostare.
     */
    private static void updateFileHandler(Properties properties, String className, LocalDate newDate, Level logLevel) {
        try {
            setupFileHandler(properties, className, newDate, logLevel);
            logger.log(Level.INFO, "Logger updated for new date: {0}", newDate);
        } catch (IOException e) {
            System.err.println("Failed to update logger for " + className + ": " + e.getMessage());
            e.printStackTrace();
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
        //logger.log(Level.SEVERE, "Errore: {0}", e.getMessage());

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
        logger.log(Level.WARNING, "Extracting key by alias from p12...");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(p12)) {
            logger.log(Level.FINE, "Initializing KeyStore with PKCS12 format...");
            java.security.KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");

            logger.log(Level.FINE, "Listing available security providers...");
            for (Provider provider : Security.getProviders()) {
                logger.log(Level.FINE, "Provider found: {0}", provider.getName());
            }

            logger.log(Level.FINE, "Loading KeyStore...");
            keyStore.load(bais, password);

            if (nullOrEmpty(alias)) {
                logger.log(Level.FINE, "No alias provided, searching for first key entry...");
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String a = aliases.nextElement();
                    logger.log(Level.FINER, "Checking alias: {0}", a);
                    if (keyStore.isKeyEntry(a)) {
                        logger.log(Level.INFO, "Key entry found, using alias: {0}", a);
                        return keyStore.getKey(a, password);
                    }
                }
                logger.log(Level.WARNING, "No key entry found in the KeyStore.");
            } else {
                logger.log(Level.FINE, "Searching for key with specified alias: {0}", alias);
                if (keyStore.isKeyEntry(alias)) {
                    logger.log(Level.INFO, "Key entry found for alias: {0}", alias);
                    return keyStore.getKey(alias, password);
                } else {
                    logger.log(Level.WARNING, "No key entry found for alias: {0}", alias);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while extracting key by alias from p12: {0}", e.getLocalizedMessage());
            logger.log(Level.FINE, "Stack trace: ", e);
        }

        logger.log(Level.WARNING, "Returning null, no key extracted.");
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
        if(repository != null) repository.LogDB("INFO", message, servletName, "GATEWAY FSE", logString);
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
        if(repository != null) repository.LogDB("ERROR", message, servletName, "GATEWAY FSE", logString);
    }
}
