package com.gbjl.strips;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PredicateSet extends HashSet<Predicate> implements Element {
    public PredicateSet() {
        super();
    }
    
    public PredicateSet(Set<Predicate> original) {
        super(original);
    }
    
    @Override
    public void replaceParams(Map<String, Param> replacement) {
        Iterator<Predicate> i = this.iterator();
        
        while (i.hasNext()) {
            i.next().replaceParams(replacement);
        }
    }
}
