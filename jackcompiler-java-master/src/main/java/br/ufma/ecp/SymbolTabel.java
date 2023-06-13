package br.ufma.ecp;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable{

    public enum Kind {
        STATIC, FIELD, ARG, VAR
    };

    public static record Symbol (String name, String type, Kind kind, int index) {}

    private Map<String, Symbol> classScope = new HashMap<>();
    private Map<String, Symbol> subroutineScope  = new HashMap<>();
    private Map<Kind, Integer> countVars  = Map.of(Kind.ARG, 0, Kind.FIELD, 0, Kind.ARG, 0, Kind.VAR, 0 );

    void define (String name, String type, Kind kind) {
        Symbol s = new Symbol (name, type, kind, varCount(kind) );
        if (kind == kind.STATIC || kind == Kind.FIELD) {
            classScope.put(name, s);
        } else {
           subroutineScope.put(name, s);
        }
        countVars.put(kind, countVars.get(kind) + 1);
    }

    public Symbol resolve (String name) {
        Symbol s = subroutineScope.get(name);
        if (s != null) return s;
        return classScope.get(name);
    }

    public int varCount (Kind kind) {
        return countVars.get(kind);
    }

    
}
