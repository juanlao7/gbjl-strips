package com.gbjl.strips;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Predicate implements Element {
    private String name;
    private ArrayList<Param> params;
    private boolean negated;
    
    public Predicate(String name, boolean negated) {
        this.name = name;
        this.negated = negated;
        this.params = new ArrayList<Param>();
    }
    
    public Predicate(String name, boolean negated, String[] params, boolean instantiated) {
        this(name, negated);
        
        for (int i = 0; i < params.length; ++i) {
            this.addParam(new Param(params[i], instantiated));
        }
    }
    
    public Predicate addParam(Param param) {
        this.params.add(param);
        return this;
    }
    
    public String getName() {
        return this.name;
    }
    
    public ArrayList<Param> getParams() {
        // TODO: return a copy?
        return this.params;
    }
    
    public boolean isNegated() {
        return this.negated;
    }
    
    public Predicate getInverse() {
        Predicate inverse = new Predicate(this.name, !this.negated);
        // TODO: copy?
        inverse.params = this.params;
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
        // TODO: replace only the uninstantiated ones
        int n = this.params.size();
        
        for (int i = 0; i < n; ++i) {
            Param paramReplacement = replacement.get(this.params.get(i).getName());
            
            if (paramReplacement != null) {
                this.params.add(i, paramReplacement);
                this.params.remove(i + 1);
            }
        }
    }
}
