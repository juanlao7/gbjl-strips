package com.gbjl.strips;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public interface HeuristicProvider {
    public ArrayList<Predicate> heuristicSortPredicateSet(PredicateSet currentState, ArrayList<Element> currentStack, PredicateSet predicateSet);
    public Operator heuristicBestOperator(PredicateSet currentState, ArrayList<Element> currentStack, Set<Operator> operators);
    public Map<String, Param> heuristicBestInstantiation(PredicateSet currentState, ArrayList<Element> currentStack, Predicate partiallyInstantiatedPredicate, Set<Map<String, Param>> instantiations);
}
