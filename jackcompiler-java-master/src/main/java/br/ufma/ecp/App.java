package br.ufma.ecp;
package com.java.nio;

import static br.ufma.ecp.token.TokenType.*;

import br.ufma.ecp.token.Token; 

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class App 
{

    
    public static void main( String[] args )
    {

    
        String input = "45  + if - \"ola\" laranja 876";
        Scanner scan = new Scanner (input.getBytes());
        for (Token tk = scan.nextToken(); tk.type != EOF; tk = scan.nextToken()) {
            System.out.println(tk);
        }

        /*
        Parser p = new Parser (input.getBytes());
        p.parse();
        */


        //Parser p = new Parser (fromFile().getBytes());
        //p.parse();

        /*
        String input = "489-85+69";
        Scanner scan = new Scanner (input.getBytes());
        System.out.println(scan.nextToken());
        System.out.println(scan.nextToken());
        System.out.println(scan.nextToken());
        System.out.println(scan.nextToken());
        System.out.println(scan.nextToken());
        Token tk = new Token(NUMBER, "42");
        System.out.println(tk);
        */
    }
}
