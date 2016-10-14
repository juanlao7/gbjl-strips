package com.gbjl.strips;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Operator implements Element {
    private String name;
    private ArrayList<Param> params;
    private PredicateSet preconditions;
    private PredicateSet postconditions;
    
    public Operator(String name) {
        this.name = name;
        this.params = new ArrayList<Param>();
        this.preconditions = new PredicateSet();
        this.postconditions = new PredicateSet();
    }
    
    public Operator(Operator original) {
        this.name = original.name;
        // TODO copy?
        this.params = original.params;
        this.preconditions = original.preconditions;
        this.postconditions = original.postconditions;
    }
    
    public Operator addParam(Param param) {
        this.params.add(param);
        return this;
    }
    
    public Operator addPrecondition(Predicate precondition) {
        this.preconditions.add(precondition);
        return this;
    }
    
    public Operator addPostcondition(Predicate postcondition) {
        this.preconditions.add(postcondition);
        return this;
    }
    
    public String getName() {
        return this.name;
    }
    
    public Set<Predicate> getPreconditions(Set<Predicate> state) {
        return new HashSet<Predicate>(this.preconditions);
    }
    
    public Set<Predicate> getPostconditions(Set<Predicate> state) {
        return new HashSet<Predicate>(this.postconditions);
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
        
        this.preconditions.replaceParams(replacement);
        this.postconditions.replaceParams(replacement);
    }
}
