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
     * Inserisce una nuova riga nella tabella MONITORAGGIO_GATEWAY all'inizio
     * dell'elaborazione di una chiamata (stato "IN_PROGRESS").
     *
     * Serve per registrare l'avvio della richiesta prima di invocare il web
     * service, così da avere traccia anche se la chiamata fallisce o non
     * risponde.
     *
     * @param servizioChiamante Nome del servizio chiamante (es. "Carica
     * Documento")
     * @param cfPaziente Codice fiscale del paziente
     * @param cfOperatore Codice fiscale dell'operatore
     * @param programma Programma selezionato (es. "CERT_VACC")
     * @return L'ID generato della riga appena inserita (può essere null in caso
     * di errore)
     */
    public Long insertMonitoraggioGatewayInizio(
            String servizioChiamante,
            String cfPaziente,
            String cfOperatore,
            String programma) {

        // Query parametrica: inserisce una riga con stato iniziale "IN_PROGRESS"
        String sql = """
        INSERT INTO MONITORAGGIO_GATEWAY
        (DATA_ORA, SERVIZIO_CHIAMANTE, ESITO, CF_PAZIENTE, CF_OPERATORE, PROGRAMMA)
        VALUES (SYSTIMESTAMP, ?, 'IN_PROGRESS', ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection(); // Specifica che vogliamo ottenere l'ID generato dalla sequenza/identity
                 PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID"})) {

            // Parametri dinamici
            ps.setString(1, servizioChiamante);
            ps.setString(2, cfPaziente);
            ps.setString(3, cfOperatore);
            ps.setString(4, programma);

            ps.executeUpdate();

            // Recupera l'ID generato (solo se la tabella usa una colonna IDENTITY o trigger con RETURNING)
            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante l'inserimento iniziale in MONITORAGGIO_GATEWAY: {0}", e.getMessage());
        }

        return null; // in caso di errore
    }

    /**
     * Aggiorna una riga esistente nella tabella MONITORAGGIO_GATEWAY alla fine
     * dell'elaborazione, popolando i campi con l'esito e i dati restituiti dal
     * web service.
     *
     * Viene chiamata sia in caso di successo che di errore.
     *
     * @param id ID della riga precedentemente inserita
     * @param esito Stato finale ("SUCCESS", "ERROR_HTTP", "ERROR_GATEWAY",
     * ecc.)
     * @param gatewayStatus Campo "status" restituito dal JSON del gateway (può
     * essere null)
     * @param gatewayTitle Campo "title" del JSON gateway
     * @param gatewayDetail Campo "detail" del JSON gateway
     * @param gatewayTraceId Campo "traceID" del JSON gateway
     * @param gatewayWorkflowInstanceId Campo "workflowInstanceId" del JSON
     * gateway
     * @param messaggio messaggio di errore in caso non dipenda dal gateway
     * @param jsonCompleto L’intera risposta JSON come stringa (verrà salvata
     * come CLOB)
     */
    public void updateMonitoraggioGatewayFine(
            Long id,
            String esito,
            Integer gatewayStatus,
            String gatewayTitle,
            String gatewayDetail,
            String gatewayTraceId,
            String gatewayWorkflowInstanceId,
            String messaggio,
            String jsonCompleto) {

        // Query di aggiornamento parametrica
        String sql = """
        UPDATE MONITORAGGIO_GATEWAY
        SET ESITO = ?, 
            GATEWAY_STATUS = ?, 
            GATEWAY_TITLE = ?, 
            GATEWAY_DETAIL = ?, 
            GATEWAY_TRACE_ID = ?, 
            GATEWAY_WORKFLOW_INSTANCE_ID = ?, 
            JSON_COMPLETO = ?,
                     MESSAGGIO = ? 
        WHERE ID = ?
        """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            // Impostazione parametri dinamici
            ps.setString(1, esito);

            if (gatewayStatus != null) {
                ps.setInt(2, gatewayStatus);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }

            ps.setString(3, gatewayTitle);
            ps.setString(4, gatewayDetail);
            ps.setString(5, gatewayTraceId);
            ps.setString(6, gatewayWorkflowInstanceId);
            ps.setString(8, messaggio);

            // Scrittura del JSON come CLOB
            if (jsonCompleto != null) {
                ps.setCharacterStream(7, new java.io.StringReader(jsonCompleto));
            } else {
                ps.setNull(7, java.sql.Types.CLOB);
            }

            ps.setLong(9, id);

            // Esecuzione UPDATE
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante l'aggiornamento in MONITORAGGIO_GATEWAY: {0}", e.getMessage());
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
