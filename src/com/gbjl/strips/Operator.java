package com.gbjl.strips;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Operator implements Element {
    private final String name;
    protected ArrayList<Param> params;
    protected PredicateSet preconditions;
    protected PredicateSet postconditions;
    
    public Operator(String name) {
        this.name = name;
        this.params = new ArrayList<>();
        this.preconditions = new PredicateSet();
        this.postconditions = new PredicateSet();
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
        this.postconditions.add(postcondition);
        return this;
    }
    
    public String getName() {
        return this.name;
    }
    
    public PredicateSet getPreconditions(Set<Predicate> state) {
        return new PredicateSet(this.preconditions);
    }
    
    public PredicateSet getPostconditions(Set<Predicate> state) {
        return new PredicateSet(this.postconditions);
    }
    
    public Operator copy() {
        Operator copy = new Operator(this.name);
        copy.params = new ArrayList<>(this.params);
        copy.preconditions = new PredicateSet(this.preconditions);
        copy.postconditions = new PredicateSet(this.postconditions);
        return copy;
    }

    @Override
    public void replaceParams(Map<String, Param> replacement) {
        Predicate.replaceParamsFromAList(replacement, this.params);
        this.preconditions.replaceParams(replacement);
        this.postconditions.replaceParams(replacement);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Operator) {
            Operator otherOperator = (Operator)other;
            return (this.name.equals(otherOperator.name) && this.params.equals(otherOperator.params) && this.preconditions.equals(otherOperator.preconditions) && this.postconditions.equals(otherOperator.postconditions));
        }
        
        return (this == other);
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + this.name.hashCode();
        hash = hash * 31 + this.params.hashCode();
        hash = hash * 13 + this.preconditions.hashCode();
        hash = hash * 23 + this.postconditions.hashCode();
        return hash;
    }
    
    @Override
    public String toString() {
        return Predicate.nameAndParamsToString(this.name, this.params, true);
    }
}
