package com.gbjl.strips;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Operator implements Element {
    private final String name;
    private final ArrayList<Param> params;
    private final PredicateSet preconditions;
    private final PredicateSet postconditions;
    
    public Operator(String name) {
        this.name = name;
        this.params = new ArrayList<>();
        this.preconditions = new PredicateSet();
        this.postconditions = new PredicateSet();
    }
    
    public Operator(Operator original) {
        this.name = original.name;
        this.params = new ArrayList<>(original.params);
        this.preconditions = new PredicateSet(original.preconditions);
        this.postconditions = new PredicateSet(original.postconditions);
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
        return new HashSet<>(this.preconditions);
    }
    
    public Set<Predicate> getPostconditions(Set<Predicate> state) {
        return new HashSet<>(this.postconditions);
    }

    @Override
    public void replaceParams(Map<String, Param> replacement) {
        Predicate.replaceParamsFromAList(replacement, this.params);
        this.preconditions.replaceParams(replacement);
        this.postconditions.replaceParams(replacement);
    }
    
    public String toString() {
        return Predicate.nameAndParamsToString(this.name, this.params, true);
    }
}
