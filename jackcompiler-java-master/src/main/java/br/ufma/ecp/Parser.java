package br.ufma.ecp;

import java.beans.Expression;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {

    private static class ParseError extends RuntimeException {}
    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();

    private String className;

    
    public Parser (byte[] input) {
        scan = new Scanner(input);
        nextToken();
    }

    public void parse () {
    }


    private void nextToken () {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }


    public String VMOutput() {
        return "";
    }

     // funções auxiliares
     public String XMLOutput() {
        return xmlOutput.toString();
    }

    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }


    boolean peekTokenIs(TokenType type) {
        return peekToken.type == type;
    }

    boolean currentTokenIs(TokenType type) {
        return currentToken.type == type;
    }

    private void expectPeek(TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                expectPeek(type);
                return;
            }
        }

       throw error(peekToken, "Expected a statement");

    }

    private void expectPeek(TokenType type) {
        if (peekToken.type == type) {
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        } else {
            throw error(peekToken, "Expected "+type.name());
        }
    }


    private static void report(int line, String where,
        String message) {
            System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
    }


    private ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
        return new ParseError();
    }

    void parseClass() {
        printNonTerminal("class");
        expectPeek(TokenType.CLASS);
        expectPeek(TokenType.IDENT);
        className = currentToken.value();
        expectPeek(TokenType.LBRACE);

        while (peekTokenIs(TokenType.STATIC) || peekTokenIs(TokenType.FIELD)) {
            parseClassVarDec();
        }

        while (peekTokenIs(TokenType.FUNCTION) || peekTokenIs(TokenType.CONSTRUCTOR) || peekTokenIs(TokenType.METHOD)) {
            parseSubroutineDec();
        }

        expectPeek(TokenType.RBRACE);

        printNonTerminal("/class");
    }
    
    void parseVarDec() {
        printNonTerminal("varDec");
        expectPeek(TokenType.VAR);

        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        String type = currentToken.value();

        expectPeek(TokenType.IDENT);
        String name = currentToken.value();

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);

            name = currentToken.value();
        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/varDec");
    }

    void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(TokenType.FIELD, TokenType.STATIC);

        //if (currentTokenIs(TokenType.FIELD))

        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        String type = currentToken.value();

        expectPeek(TokenType.IDENT);
        String name = currentToken.value();

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);

            name = currentToken.value();
        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/classVarDec");
    }

    void parseSubroutineDec() {
        printNonTerminal("subroutineDec");

        expectPeek(TokenType.CONSTRUCTOR, TokenType.FUNCTION, TokenType.METHOD);
        var subroutineType = currentToken.type;

        /*if (subroutineType == TokenType.METHOD) {
            symbolTable.define("this", className, Kind.ARG);
        }*/

        expectPeek(TokenType.VOID, TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        expectPeek(TokenType.IDENT);

        var functionName = className + "." + currentToken.value();

        expectPeek(TokenType.LPAREN);
        parseParameterList();
        expectPeek(TokenType.RPAREN);
        parseSubroutineBody(functionName, subroutineType);

        printNonTerminal("/subroutineDec");
    }

    void parseParameterList() {
        printNonTerminal("parameterList");

        if (!peekTokenIs(TokenType.RPAREN)) // verifica se tem pelo menos uma expressao
        {
            expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
            String type = currentToken.value();

            expectPeek(TokenType.IDENT);
            String name = currentToken.value();

            while (peekTokenIs(TokenType.COMMA)) {
                expectPeek(TokenType.COMMA);
                expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
                type = currentToken.value();

                expectPeek(TokenType.IDENT);
                name = currentToken.value();
            }

        }
        printNonTerminal("/parameterList");
    }

    void parseSubroutineBody(String functionName, TokenType subroutineType) {

        printNonTerminal("subroutineBody");
        expectPeek(TokenType.LBRACE);
        while (peekTokenIs(TokenType.VAR)) {
            parseVarDec();
        }

        parseStatements();
        expectPeek(TokenType.RBRACE);
        printNonTerminal("/subroutineBody");
    }

    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
          case NUMBER:
            expectPeek(TokenType.NUMBER);
            break;
          case STRING:
            expectPeek(TokenType.STRING);
            break;
          case FALSE:
          case NULL:
          case TRUE:
            expectPeek(TokenType.FALSE, TokenType.NULL, TokenType.TRUE);
            break;
          case THIS:
            expectPeek(TokenType.THIS);
            break;
          case IDENT:
            expectPeek(TokenType.IDENT);
            if (peekTokenIs(TokenType.LPAREN) || peekTokenIs(TokenType.DOT)){
                parseSubroutineCall();
            }else if (peekTokenIs(TokenType.LBRACKET)){
                expectPeek(TokenType.LBRACKET);
                parseExpression();
                expectPeek(TokenType.RBRACKET);
            }
            break;
          default:
            throw error(peekToken, "term expected");
        }
    
        printNonTerminal("/term");
      }

      static public boolean isOperator(String op) {
        return "+-*/<>=~&|".contains(op);
        }

        int parseExpressionList() {
            printNonTerminal("expressionList");
    
            var nArgs = 0;
    
            if (!peekTokenIs(TokenType.RPAREN)) 
            {
                parseExpression();
                nArgs = 1;
            }
    
            while (peekTokenIs(TokenType.COMMA)) {
                expectPeek(TokenType.COMMA);
                parseExpression();
                nArgs++;
            }
            printNonTerminal("/expressionList");
            return nArgs;
        }

        void parseExpression() {
            printNonTerminal("expression");
            parseTerm ();
            while (isOperator(peekToken.lexeme)) {
                expectPeek(peekToken.type);
                parseTerm();
            }
            printNonTerminal("/expression");
      }

      void parseStatements() {
        printNonTerminal("statements");
        while (peekToken.type == TokenType.WHILE || 
        peekToken.type == TokenType.IF || 
        peekToken.type == TokenType.LET || 
        peekToken.type == TokenType.DO || 
        peekToken.type == TokenType.RETURN) {
        parseStatement();
        }
        printNonTerminal("/statements");
      }

      void parseStatement() {
        switch (peekToken.type) {
            case LET:
                parseLet();
                break;
            case WHILE:
                parseWhile();
                break;
            case IF:
                parseIf();
                break;
            case RETURN:
                parseReturn();
                break;
            case DO:
                parseDo();
                break;
            default:
                throw error(peekToken, "Expected a statement");
        }
    }

      void parseLet() {
        printNonTerminal("letStatement");
        expectPeek(TokenType.LET);
        expectPeek(TokenType.IDENT);
        expectPeek(TokenType.EQ);

        /*if (peekTokenIs(!TokenType.LBRACKET)) {
            expectPeek(TokenType.LBRACKET);
            parseExpression();
            expectPeek(TokenType.RBRACKET);
        }

        if (peekTokenIs(TokenType.IDENT)){
            parseExpression();
        }else{
            parseExpression();
        }*/

        parseExpression();
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/letStatement");
    }

    void parseIf(){
        printNonTerminal("ifStatement");
        expectPeek(TokenType.IF);

        expectPeek(TokenType.LPAREN);
        parseExpression();
        expectPeek(TokenType.RPAREN);

        expectPeek(TokenType.LBRACE);
        parseStatements();
        expectPeek(TokenType.RBRACE);

        if (peekTokenIs(TokenType.ELSE)){
            expectPeek(TokenType.ELSE);
            expectPeek(TokenType.LBRACE);
            parseStatements();
            expectPeek(TokenType.RBRACE);
        }
        printNonTerminal("/ifStatement");
    }

    void parseSubroutineCall() {
        /*int nArgs = 0;
        String functionName = (TokenType.IDENT + ",");*/

        /*if (TokenType.isKeyword(null)){
            functionName = TokenType.isSymbol(null) + "." + currentToken.value();
            nArgs = 1; // do proprio objeto
        }*/

        if (peekTokenIs(TokenType.LPAREN)) {
            expectPeek(TokenType.LPAREN);
            parseExpressionList();
            expectPeek(TokenType.RPAREN);
            /*functionName = className + "." + TokenType.IDENT;*/

        } else if(peekTokenIs(TokenType.DOT)){
            expectPeek(TokenType.DOT);
            expectPeek(TokenType.IDENT); 
            /*functionName += currentToken.value();*/

            expectPeek(TokenType.LPAREN);
            parseExpressionList();
            expectPeek(TokenType.RPAREN);
        }
        /*} else {
            throw new Error("Invalid subroutine call");
        } term expected*/
    }

    void parseDo(){
        printNonTerminal("doStatement");
        expectPeek(TokenType.DO);
        expectPeek(TokenType.IDENT);
        parseSubroutineCall();
        expectPeek(TokenType.SEMICOLON);

        printNonTerminal("/doStatement");
    }

    void parseWhile(){
        printNonTerminal("whileStatement");
        expectPeek(TokenType.WHILE);
        expectPeek(TokenType.LPAREN);
        parseExpression();
        expectPeek(TokenType.RPAREN);
        expectPeek(TokenType.LBRACE);
        parseStatements();
        expectPeek(TokenType.RBRACE);
        printNonTerminal("/whileStatement");
    }

    void parseReturn(){
        printNonTerminal("returnStatement");
        expectPeek(TokenType.RETURN);
        if (!peekTokenIs(TokenType.SEMICOLON)) {
            parseExpression();
        }
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/returnStatement");
    }

}