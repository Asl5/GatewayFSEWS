/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import java.io.Console;
import java.util.Scanner;

/**
 *
 * @author f.matraxia
 */
public class EncryptPassword {

    public static void main(String[] args) {
        String plainPassword;

        if (args.length > 0) {
            plainPassword = args[0];
        } else {
            Console console = System.console();

            if (console != null) {
                char[] passwordChars = console.readPassword("Password DB da cifrare: ");
                plainPassword = new String(passwordChars);
            } else {
                System.out.print("Password DB da cifrare: ");
                Scanner scanner = new Scanner(System.in);
                plainPassword = scanner.nextLine();
            }
        }

        String encryptedPassword = PasswordCrypto.encrypt(plainPassword);

        System.out.println();
        System.out.println("Password cifrata da mettere nel context.xml:");
        System.out.println(encryptedPassword);
    }
}
