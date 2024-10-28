/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author f.matraxia
 */
public class Repository {
    
    private static DataSource dataSource;
    private final Logger logger;

    /**
     * Il costruttore prende come parametro il DataSource e il logger del
     * servizio.
     *
     * @param logger il logger da utilizzare per loggare gli eventi
     */
    public Repository(Logger logger) {
        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:/comp/env/jdbc/FSEGatewayDataSource");
        } catch (NamingException e) {
            throw new RuntimeException("Failed to look up DataSource", e);
        }
        this.logger = logger;
    }

    /**
     * Funzione che inserisce il log nel database relativamente ai passi
     * eseguiti dalla servlet.
     *
     * @param severityLog tipologia del log (INFO, ERROR, WARNING, ecc...)
     * @param message messaggio di log da scrivere
     * @param functionName la funzione che ha generato il log
     * @param servizio il servizio che ha generato il log
     * @param params i parametri dlla servlet
     */
    public void LogDB(String severityLog, String message, String functionName, String servizio, String params) {
        String query = "INSERT INTO LOG_TABLE (TIPO_LOG, MESSAGGIO, FUNZIONE, TIMESTAMP, PARAMS, SERVIZIO, DATI) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection(); PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setString(1, severityLog);
            pstmt.setString(2, message);
            pstmt.setString(3, functionName);
            pstmt.setDate(4, new java.sql.Date(System.currentTimeMillis()));
            pstmt.setString(5, params);
            pstmt.setString(6, servizio);
            pstmt.setString(7, null);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Exception, Failed to insert log into database: {0}", e.getLocalizedMessage());
        }
    }

    /**
     * Restituisce una connessione al database tramite il DataSource
     * configurato.
     *
     * @return un oggetto Connection per il database
     * @throws SQLException se non è possibile ottenere la connessione
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
