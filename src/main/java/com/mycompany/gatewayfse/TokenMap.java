/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;

/**
 *
 * @author f.matraxia
 */
public class TokenMap {
    // Costanti comuni

    private static final String SUBJECT_ROLE = "DSA";
    private static final String PURPOSE_OF_USE = "TREATMENT";
    private static final String ISSUER_LDO = "S1#111TOPGATEXXXX";
    private static final String ISSUER_VACC = "S1#070105000000XX";
    private static final String LOCALITY = "RADIOLOGIA ASL5 PROVA^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^070105010025";
    private static final String SUBJECT_ORG_ID = "070";
    private static final String SUBJECT_ORGANIZATION = "Regione Liguria";
    private static final String AUDIENCE = "https://modipa-val.fse.salute.gov.it/govway/rest/in/FSE/gateway/v1";
    private static final String PATIENT_CONSENT = "true";
    private static final String ACTION_ID = "CREATE";
    private static final String JTI = "6888670001";

    // Enum per i tipi di programma
    public enum ProgramType {
        LDO,
        CERT_VACC_SKIPPER,
        SING_VACC_SKIPPER,
        SING_VACC_PHTRACK,
        CERT_VACC,
        SING_VACC
    }

    public TokenMap() {
    }

    // Metodo per creare la mappa in base al tipo di programma
    public Map<String, String> mappa(ProgramType tipo, String cfSub, String cfPaz, Properties properties, ServletContext context) {
        Map<String, String> mapJD = createBaseMap(cfSub, cfPaz);

        // Aggiungi le chiavi specifiche in base al tipo di programma
        switch (tipo) {
            case LDO -> {
                String relativePathPem = properties.getProperty("pem.LDO");
                String absolutePathPem = context.getRealPath(relativePathPem);
                String relativePathP12 = properties.getProperty("p12.LDO");
                String absolutePathP12 = context.getRealPath(relativePathP12);
                mapJD.put("iss", ISSUER_LDO);
                mapJD.put("subject_application_id", "letteradimissione");
                mapJD.put("subject_application_vendor", "topgate");
                mapJD.put("subject_application_version", "1.0");
                mapJD.put("resource_hl7_type", "('34105-7^^2.16.840.1.113883.6.1')");
                mapJD.put("pem_path", absolutePathPem);
                mapJD.put("p12_path", absolutePathP12);
            }

            case CERT_VACC_SKIPPER, SING_VACC_SKIPPER -> {
                String relativePathPem = tipo == ProgramType.CERT_VACC_SKIPPER
                        ? properties.getProperty("pem.CERT_VACC_SKIPPER")
                        : properties.getProperty("pem.SING_VACC_SKIPPER");
                String absolutePathPem = context.getRealPath(relativePathPem);
                String relativePathP12 = tipo == ProgramType.CERT_VACC_SKIPPER
                        ? properties.getProperty("p12.CERT_VACC_SKIPPER")
                        : properties.getProperty("p12.SING_VACC_SKIPPER");
                String absolutePathP12 = context.getRealPath(relativePathP12);
                mapJD.put("iss", ISSUER_LDO); // Issuer è lo stesso per entrambi
                mapJD.put("subject_application_id", "skipper");
                mapJD.put("subject_application_vendor", "topgate");
                mapJD.put("subject_application_version", "1.0");
                mapJD.put("resource_hl7_type", tipo == ProgramType.CERT_VACC_SKIPPER
                        ? "('82593-5^^2.16.840.1.113883.6.1')"
                        : "('87273-9^^2.16.840.1.113883.6.1')");
                mapJD.put("pem_path", absolutePathPem);
                mapJD.put("p12_path", absolutePathP12);
            }
            case SING_VACC_PHTRACK -> {
                String relativePathPem = properties.getProperty("pem.SING_VACC_PHTRACK");
                String absolutePathPem = context.getRealPath(relativePathPem);
                String relativePathP12 = properties.getProperty("p12.SING_VACC_PHTRACK");
                String absolutePathP12 = context.getRealPath(relativePathP12);
                mapJD.put("iss", ISSUER_VACC);
                mapJD.put("subject_application_id", "PHTRACK");
                mapJD.put("subject_application_vendor", "ASL5");
                mapJD.put("subject_application_version", "1.0");
                mapJD.put("resource_hl7_type", "('87273-9^^2.16.840.1.113883.6.1')");
                mapJD.put("pem_path", absolutePathPem);
                mapJD.put("p12_path", absolutePathP12);
            }

            case CERT_VACC, SING_VACC -> {
                String relativePathPem = tipo == ProgramType.CERT_VACC
                        ? properties.getProperty("pem.CERT_VACC")
                        : properties.getProperty("pem.SING_VACC");
                String absolutePathPem = context.getRealPath(relativePathPem);
                String relativePathP12 = tipo == ProgramType.CERT_VACC
                        ? properties.getProperty("p12.CERT_VACC")
                        : properties.getProperty("p12.SING_VACC");
                String absolutePathP12 = context.getRealPath(relativePathP12);
                mapJD.put("iss", ISSUER_VACC);
                mapJD.put("subject_application_id", "GestioneVaccinazioni");
                mapJD.put("subject_application_vendor", "ASL5");
                mapJD.put("subject_application_version", "1.0");
                mapJD.put("resource_hl7_type", tipo == ProgramType.CERT_VACC
                        ? "('82593-5^^2.16.840.1.113883.6.1')"
                        : "('87273-9^^2.16.840.1.113883.6.1')");
                mapJD.put("pem_path", absolutePathPem);
                mapJD.put("p12_path", absolutePathP12);
            }
            default ->
                throw new IllegalArgumentException("Tipo di programma non supportato: " + tipo);
        }

        return mapJD;
    }

    // Metodo per creare la mappa di base con i valori comuni
    private Map<String, String> createBaseMap(String cfSub, String cfPaz) {
        return new HashMap<String, String>() {
            {
                put("sub", cfSub + "^^^&2.16.840.1.113883.2.9.4.3.2&ISO");
                put("subject_role", SUBJECT_ROLE);
                put("purpose_of_use", PURPOSE_OF_USE);
                put("locality", LOCALITY);
                put("subject_organization_id", SUBJECT_ORG_ID);
                put("subject_organization", SUBJECT_ORGANIZATION);
                put("aud", AUDIENCE);
                put("patient_consent", PATIENT_CONSENT);
                put("action_id", ACTION_ID);
                put("jti", JTI);
                put("person_id", cfPaz + "^^^&2.16.840.1.113883.2.9.4.3.2&ISO");
            }
        };
    }
}
