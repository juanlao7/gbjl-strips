package com.gbjl.strips;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PredicateSet extends HashSet<Predicate> implements Element {
    public PredicateSet() {
        super();
    }
    
    public PredicateSet(Set<Predicate> original) {
        super();
        Iterator<Predicate> i = original.iterator();
        
        while (i.hasNext()) {
            this.add(new Predicate(i.next()));
        }
    }
    
    public ArrayList<Predicate> toArrayList() {
        return new ArrayList<>(this);
    }
    
    public PredicateSet getPredicatesByName(String name) {
        PredicateSet result = new PredicateSet();
        Iterator<Predicate> i = this.iterator();
        
        while (i.hasNext()) {
            Predicate predicate = i.next();
            
            if (predicate.getName().equals(name)) {
                result.add(new Predicate(predicate));
            }
        }
        
        return result;
    }
    
    public ArrayList<Predicate> sortPredicatesByName(String[] nameOrder) {
        ArrayList<Predicate> result = new ArrayList<>();
        
        for (int i = 0; i < nameOrder.length; ++i) {
            result.addAll(this.getPredicatesByName(nameOrder[i]));
        }
        
        return result;
    }
    
    @Override
    public void replaceParams(Map<String, Param> replacement) {
        Iterator<Predicate> i = this.iterator();
        
        while (i.hasNext()) {
            i.next().replaceParams(replacement);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<Predicate> i = this.iterator();
        
        while (i.hasNext()) {
            builder.append(i.next());
            builder.append(";");
        }
        
        return builder.toString();
    }
    
    public static PredicateSet fromString(String representation) {
        PredicateSet predicateSet = new PredicateSet();
        String[] predicateList = representation.split(";");
        
        for (int i = 0; i < predicateList.length; ++i) {
            if (!predicateList[i].isEmpty()) {
                predicateSet.add(Predicate.fromString(predicateList[i]));
            }
        }
        
        return predicateSet;
    }
}
