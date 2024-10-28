/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

/**
 *
 * @author f.matraxia
 */
public class TokenResponseDTO {

    private final String authorizationBearer;
    private final String fseJwtSignature;

    public TokenResponseDTO(String authJWT, String signJWT) {
        this.authorizationBearer = authJWT;
        this.fseJwtSignature = signJWT;
    }

    public String getAuthorizationBearer() {
        return authorizationBearer;
    }

    public String getFseJwtSignature() {
        return fseJwtSignature;
    }
}
