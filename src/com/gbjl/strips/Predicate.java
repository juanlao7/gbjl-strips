package com.gbjl.strips;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Predicate implements Element {
    private final String name;
    private ArrayList<Param> params;
    private final boolean negated;
    
    public Predicate(String name, boolean negated) {
        this.name = name;
        this.negated = negated;
        this.params = new ArrayList<>();
    }
    
    public Predicate(String name, boolean negated, String[] params, boolean instantiated) {
        this(name, negated);
        
        for (int i = 0; i < params.length; ++i) {
            this.addParam(new Param(params[i], instantiated));
        }
    }
    
    public void addParam(Param param) {
        this.params.add(param);
    }
    
    public String getName() {
        return this.name;
    }
    
    public ArrayList<Param> getParams() {
        return new ArrayList<>(this.params);
    }
    
    public boolean isNegated() {
        return this.negated;
    }
    
    public Predicate getInverse() {
        Predicate inverse = new Predicate(this.name, !this.negated);
        inverse.params = new ArrayList<>(this.params);
        return inverse;
    }
    
    public boolean isFullyInstantiated() {
        Iterator<Param> i = this.params.iterator();
        
        while (i.hasNext()) {
            if (!i.next().isInstantiated()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public void replaceParams(Map<String, Param> replacement) {
        replaceParamsFromAList(replacement, this.params);
    }
    
    public static void replaceParamsFromAList(Map<String, Param> replacement, ArrayList<Param> list) {
        int n = list.size();
        
        for (int i = 0; i < n; ++i) {
            Param originalParam = list.get(i);
            
            if (!originalParam.isInstantiated()) {
                Param paramReplacement = replacement.get(originalParam.getName());
            
                if (paramReplacement != null) {
                    list.add(i, paramReplacement);
                    list.remove(i + 1);
                }
            }
        }
    }
    
    public static Predicate fromString(String representation) {
        boolean negated = false;
        int nameStart = 0;
        
        if (representation.charAt(0) == '¬') {
            negated = true;
            nameStart = 1;
        }
        
        int parenthesisPosition = representation.indexOf("(");
        
        if (parenthesisPosition < 0) {
            return new Predicate(representation.substring(nameStart), negated);
        }
        
        String[] paramList = representation.substring(parenthesisPosition + 1, representation.length() - 1).split(",");
        return new Predicate(representation.substring(nameStart, parenthesisPosition), negated, paramList, true);
    }
    
    public String toString() {
        return ((this.negated) ? "¬" : "") + nameAndParamsToString(this.name, this.params, false);
    }
    
    public static String nameAndParamsToString(String name, ArrayList<Param> params, boolean addParenthesisWithNoParams) {
        if (params.isEmpty() && !addParenthesisWithNoParams) {
            return name;
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        builder.append("(");
        
        if (!params.isEmpty()) {
            Iterator<Param> i = params.iterator();
            builder.append(i.next());

            while (i.hasNext()) {
                builder.append(",");
                builder.append(i.next());
            }
        }
        
        builder.append(")");
        return builder.toString();
    }
}
