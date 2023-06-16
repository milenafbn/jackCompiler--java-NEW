package br.ufma.ecp;
import static br.ufma.ecp.token.TokenType.*;


import java.beans.Expression;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;
import br.ufma.ecp.SymbolTable.Kind;
import br.ufma.ecp.SymbolTable.Symbol;
import br.ufma.ecp.VMWriter.Command;
import br.ufma.ecp.VMWriter.Segment;

public class Parser {

    private static class ParseError extends RuntimeException {}
    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();
    private VMWriter vmWriter = new VMWriter();
	private SymbolTable symTable = new SymbolTable();

    private String className;
    private int ifLabelNum = 0 ;
    private int whileLabelNum = 0;
    
    public Parser (byte[] input) {
        scan = new Scanner(input);
        symTable = new SymbolTable();
        vmWriter = new VMWriter();

        nextToken();

        ifLabelNum = 0;
        whileLabelNum = 0;
    }

    public void parse () {
        parseClass();
    }


    private void nextToken () {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }


    public String VMOutput() {
        return vmWriter.vmOutput();
    }

     // funções auxiliares
     public String XMLOutput() {
        return xmlOutput.toString();
    }

    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }
    private Segment kind2Segment(Kind kind) {
        if (kind == Kind.STATIC)
            return Segment.STATIC;
        if (kind == Kind.FIELD)
            return Segment.THIS;
        if (kind == Kind.VAR)
            return Segment.LOCAL;
        if (kind == Kind.ARG)
            return Segment.ARG;
        return null;
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
        ifLabelNum = 0;
        whileLabelNum = 0;

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
            case INT:
                expectPeek(TokenType.INT);
                vmWriter.writePush(Segment.CONST, Integer.parseInt(currentToken.value()));
                break;
            case NUMBER:
                expectPeek(TokenType.NUMBER);
                vmWriter.writePush(Segment.CONST, Integer.parseInt(currentToken.lexeme));
                break;
        case STRING:
                expectPeek(TokenType.STRING);
                var strValue = currentToken.lexeme;
                vmWriter.writePush(Segment.CONST, strValue.length());
                vmWriter.writeCall("String.new", 1);
                for (int i = 0; i < strValue.length(); i++) {
                    vmWriter.writePush(Segment.CONST, strValue.charAt(i));
                    vmWriter.writeCall("String.appendChar", 2);
                }
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(TokenType.FALSE, TokenType.NULL, TokenType.TRUE);
                vmWriter.writePush(Segment.CONST, 0);
                if (currentToken.type == TRUE)
                    vmWriter.writeArithmetic(Command.NOT);
                break;
            case THIS:
                expectPeek(TokenType.THIS);
                vmWriter.writePush(Segment.POINTER, 0);
                break;
            case IDENT:
                expectPeek(TokenType.IDENT);
                /* Symbol sym = SymbolTable.resolve(currentToken.value()); */
                if (peekTokenIs(TokenType.LPAREN) || peekTokenIs(TokenType.DOT)){
                    parseSubroutineCall();
                }else if(peekTokenIs(TokenType.LBRACKET)){
                    expectPeek(TokenType.LBRACKET);
                    parseExpression();
                    vmWriter.writePush(kind2Segment(sym.kind()), sym.index());
                    vmWriter.writeArithmetic(Command.ADD);
                    expectPeek(TokenType.RBRACKET);
                    vmWriter.writePop(Segment.POINTER, 1); 
                    vmWriter.writePush(Segment.THAT, 0);   
                }else{
                    vmWriter.writePush(kind2Segment(sym.kind()), sym.index());
                }
                break;
            case LPAREN:
                expectPeek(TokenType.LPAREN);
                parseExpression();
                expectPeek(TokenType.RPAREN);
                break;
            case MINUS:
            case NOT:
                expectPeek(TokenType.MINUS, TokenType.NOT);
                var op = currentToken.type;
                parseTerm();
                if (op == MINUS)
                    vmWriter.writeArithmetic(Command.NEG);
                else
                    vmWriter.writeArithmetic(Command.NOT);
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
                var ope = peekToken.type;
                expectPeek(peekToken.type);
                parseTerm();
                compileOperators(ope);
            }
            printNonTerminal("/expression");
      }

       public void compileOperators(TokenType type) {

        if (type == ASTERISK) {
            vmWriter.writeCall("Math.multiply", 2);
        } else if (type == SLASH) {
            vmWriter.writeCall("Math.divide", 2);
        } else {
            vmWriter.writeArithmetic(typeOperator(type));
        }
    }

    private Command typeOperator(TokenType type) {
        if (type == PLUS)
            return Command.ADD;
        if (type == MINUS)
            return Command.SUB;
        if (type == LT)
            return Command.LT;
        if (type == GT)
            return Command.GT;
        if (type == EQ)
            return Command.EQ;
        if (type == AND)
            return Command.AND;
        if (type == OR)
            return Command.OR;
        return null;
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
        /*var isArray = false;*/
        printNonTerminal("letStatement");
        expectPeek(TokenType.LET);
        expectPeek(TokenType.IDENT);
        if (peekTokenIs(TokenType.LBRACKET)) {
            expectPeek(TokenType.LBRACKET);
            parseExpression();
            expectPeek(TokenType.RBRACKET);
            /*isArray = true;*/
        }/*else if(peekTokenIs(TokenType.IDENT)){
            expectPeek(TokenType.IDENT);
            parseExpression();
        }*/
        expectPeek(TokenType.EQ);
        parseExpression();
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/letStatement");
    }

    void parseIf() {
        printNonTerminal("ifStatement");

        var labelTrue = "IF_TRUE" + ifLabelNum;
        var labelFalse = "IF_FALSE" + ifLabelNum;
        var labelEnd = "IF_END" + ifLabelNum;

        ifLabelNum++;
    
        expectPeek(IF);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);

        vmWriter.writeIf(labelTrue);
        vmWriter.writeGoto(labelFalse);
        vmWriter.writeLabel(labelTrue);
    
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);
        if (peekTokenIs(ELSE)){
            vmWriter.writeGoto(labelEnd);
        }

        vmWriter.writeLabel(labelFalse);

        if (peekTokenIs(ELSE))
        {
            expectPeek(ELSE);
            expectPeek(LBRACE)
            parseStatements();
            expectPeek(RBRACE);
            vmWriter.writeLabel(labelEnd);
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

        } else{
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

    void parseWhile() {
        printNonTerminal("whileStatement");

        var labelTrue = "WHILE_EXP" + whileLabelNum;
        var labelFalse = "WHILE_END" + whileLabelNum;
        whileLabelNum++;

        vmWriter.writeLabel(labelTrue);

        expectPeek(WHILE);
        expectPeek(LPAREN);
        parseExpression();

        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(labelFalse);

        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();

        vmWriter.writeGoto(labelTrue); // Go back to labelTrue and check condition
        vmWriter.writeLabel(labelFalse); // Breaks out of while loop because ~(condition) is true

        expectPeek(RBRACE);
        printNonTerminal("/whileStatement");
    }

    void parseReturn(){
        printNonTerminal("returnStatement");
        expectPeek(TokenType.RETURN);
        if (!peekTokenIs(TokenType.SEMICOLON)) {
            parseExpression();
        } else {
            vmWriter.writePush(Segment.CONST, 0);
        }
        expectPeek(TokenType.SEMICOLON);
        vmWriter.writeReturn();

        printNonTerminal("/returnStatement");
    }

}