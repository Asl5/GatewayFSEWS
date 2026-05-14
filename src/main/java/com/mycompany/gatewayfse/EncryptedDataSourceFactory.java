/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import org.apache.tomcat.jdbc.pool.DataSourceFactory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.util.Hashtable;

/**
 *
 * @author f.matraxia
 */
public class EncryptedDataSourceFactory extends DataSourceFactory {

    @Override
    @SuppressWarnings({"UseOfObsoleteCollectionType", "java:S1149"})
    public Object getObjectInstance(
            Object obj,
            Name name,
            Context nameCtx,
            Hashtable<?, ?> environment
    ) throws Exception {

        if (obj instanceof Reference reference) {
            RefAddr passwordAddr = reference.get("password");

            if (passwordAddr != null) {
                String encryptedPassword = String.valueOf(passwordAddr.getContent());
                String decryptedPassword = PasswordCrypto.decrypt(encryptedPassword);

                replaceReferenceAttribute(reference, "password", decryptedPassword);
            }
        }

        return super.getObjectInstance(obj, name, nameCtx, environment);
    }

    private void replaceReferenceAttribute(Reference reference, String attributeName, String newValue) {
        for (int i = 0; i < reference.size(); i++) {
            RefAddr refAddr = reference.get(i);

            if (attributeName.equals(refAddr.getType())) {
                reference.remove(i);
                break;
            }
        }

        reference.add(new StringRefAddr(attributeName, newValue));
    }
}
