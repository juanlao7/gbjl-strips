package com.gbjl.strips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Solver {
    public Solver() {
    }
    
    public ArrayList<Operator> solve(Set<Operator> operators, Set<Predicate> initialState, Set<Predicate> goalState) throws STRIPSException {
        return this.solve(operators, initialState, goalState, null);
    }
    
    public ArrayList<Operator> solve(Set<Operator> operators, Set<Predicate> initialState, Set<Predicate> goalState, HeuristicProvider heuristicProvider) throws STRIPSException {
        Set<Predicate> currentState = new HashSet<>(initialState);
        ArrayList<Operator> plan = new ArrayList<>();
        ArrayList<Element> stack = new ArrayList<>();
        PredicateSet goalStateAsPredicateSet = new PredicateSet(goalState);
        stack.add(goalStateAsPredicateSet);
        Iterator<Predicate> i;
        
        if (heuristicProvider == null) {
            i = goalStateAsPredicateSet.iterator();
        }
        else {
            i = heuristicProvider.heuristicSortPredicateSet(currentState, stack, goalStateAsPredicateSet).iterator();
        }
        
        while (i.hasNext()) {
            stack.add(i.next());
        }
        
        while (!stack.isEmpty()) {
            Element element = stack.remove(stack.size() - 1);
            
            if (element instanceof Operator) {
                Operator operator = (Operator)element;
                i = operator.getPostconditions(currentState).iterator();
                
                while (i.hasNext()) {
                    Predicate postcondition = i.next();
                    currentState.remove(postcondition.getInverse());
                    currentState.add(postcondition);
                }
                
                plan.add(operator);
            }
            else if (element instanceof PredicateSet) {
                PredicateSet predicateSet = (PredicateSet)element;
                
                if (heuristicProvider == null) {
                    i = predicateSet.iterator();
                }
                else {
                    i = heuristicProvider.heuristicSortPredicateSet(currentState, stack, predicateSet).iterator();
                }
                
                while (i.hasNext()) {
                    Predicate predicate = i.next();
                    
                    if (!currentState.contains(predicate)) {
                        stack.add(predicate);
                    }
                }
            }
            else if (element instanceof Predicate) {
                Predicate predicate = (Predicate)element;
                
                if (predicate.isFullyInstantiated()) {
                    if (!currentState.contains(predicate)) {
                        Operator operator = this.searchOperator(operators, currentState, stack, predicate, heuristicProvider);
                        stack.add(operator);
                        Set<Predicate> operatorPreconditions = operator.getPreconditions(currentState);
                        stack.add(new PredicateSet(operatorPreconditions));
                        i = operatorPreconditions.iterator();
                        
                        while (i.hasNext()) {
                            stack.add(i.next());
                        }
                    }
                }
                else {
                    Map<String, Param> replacement = this.searchInstantiation(currentState, stack, predicate, heuristicProvider);
                    Iterator<Element> j = stack.iterator();
                    
                    while (j.hasNext()) {
                        j.next().replaceParams(replacement);
                    }
                }
            }
        }
        
        return plan;
    }
    
    private Operator searchOperator(Set<Operator> operators, Set<Predicate> state, ArrayList<Element> stack, Predicate desiredCondition, HeuristicProvider heuristicProvider) throws OperatorNotFoundSTRIPSException {
        String desiredConditionName = desiredCondition.getName();
        boolean isDesiredConditionNegated = desiredCondition.isNegated();
        ArrayList<Param> desiredConditionParams = desiredCondition.getParams();
        Set<Operator> candidates = new HashSet<>();
        Iterator<Operator> i = operators.iterator();
        
        while (i.hasNext()) {
            Operator operator = i.next();
            Iterator<Predicate> j = operator.getPostconditions(state).iterator();
            
            while (j.hasNext()) {
                Predicate postcondition = j.next();
                
                if (postcondition.getName().equals(desiredConditionName) && postcondition.isNegated() == isDesiredConditionNegated) {
                    operator = new Operator(operator);
                    Map<String, Param> replacement = new HashMap<>();
                    ArrayList<Param> postconditionParams = postcondition.getParams();
                    int n = postcondition.getParams().size();
                    
                    for (int k = 0; k < n; ++k) {
                        replacement.put(postconditionParams.get(k).getName(), desiredConditionParams.get(k));
                    }
                    
                    operator.replaceParams(replacement);
                    
                    if (heuristicProvider == null) {
                        return operator;
                    }
                    
                    candidates.add(operator);
                }
            }
        }
        
        if (candidates.isEmpty()) {
            throw new OperatorNotFoundSTRIPSException();
        }
        
        return heuristicProvider.heuristicBestOperator(state, stack, candidates);
    }
    
    private Map<String, Param> searchInstantiation(Set<Predicate> state, ArrayList<Element> stack, Predicate partiallyInstantiatedPredicate, HeuristicProvider heuristicProvider) throws InstantiationNotFoundSTRIPSException {
        String partiallyInstantiatedPredicateName = partiallyInstantiatedPredicate.getName();
        boolean isPartiallyInstantiatedPredicateNegated = partiallyInstantiatedPredicate.isNegated();
        ArrayList<Param> partiallyInstantiatedPredicateParams = partiallyInstantiatedPredicate.getParams();
        Set<Map<String, Param>> candidates = new HashSet<>();
        Iterator<Predicate> i = state.iterator();
        
        while (i.hasNext()) {
            Predicate statePredicate = i.next();
            
            if (statePredicate.getName().equals(partiallyInstantiatedPredicateName) && statePredicate.isNegated() == isPartiallyInstantiatedPredicateNegated) {
                Map<String, Param> replacement = new HashMap<>();
                boolean discard = false;
                ArrayList<Param> statePredicateParams = statePredicate.getParams();
                int n = statePredicateParams.size();
                
                for (int j = 0; j < n && !discard; ++j) {
                    Param partiallyInstantiatedPredicateParam = partiallyInstantiatedPredicateParams.get(j);
                    
                    if (partiallyInstantiatedPredicateParam.isInstantiated()) {
                        if (!statePredicateParams.get(j).getName().equals(partiallyInstantiatedPredicateParam.getName())) {
                            discard = true;
                        }
                    }
                    else {
                        replacement.put(partiallyInstantiatedPredicateParam.getName(), statePredicateParams.get(j));
                    }
                }
                
                if (!discard) {
                    if (heuristicProvider == null) {
                        return replacement;
                    }
                    
                    candidates.add(replacement);
                }
            }
        }
        
        if (candidates.isEmpty()) {
            throw new InstantiationNotFoundSTRIPSException();
        }
        
        return heuristicProvider.heuristicBestInstantiation(state, stack, candidates);
    }
}
