package com.gbjl.strips;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public interface HeuristicProvider {
    public ArrayList<Predicate> heuristicSortPredicateSet(Set<Predicate> currentState, ArrayList<Element> currentStack, PredicateSet predicateSet);
    public Operator heuristicBestOperator(Set<Predicate> currentState, ArrayList<Element> currentStack, Set<Operator> operators);
    public Map<String, Param> heuristicBestInstantiation(Set<Predicate> currentState, ArrayList<Element> currentStack, Set<Map<String, Param>> instantiations);
}
