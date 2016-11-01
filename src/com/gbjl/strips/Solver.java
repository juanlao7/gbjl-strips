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
        return this.solve(operators, initialState, goalState, null, null);
    }
    
    public ArrayList<Operator> solve(Set<Operator> operators, Set<Predicate> initialState, Set<Predicate> goalState, HeuristicProvider heuristicProvider, STRIPSLogger logger) throws STRIPSException {
        PredicateSet currentState = new PredicateSet(initialState);
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
            stack.add(new Predicate(i.next()));
        }
        
        this.logln(logger, "STRIPS execution started");
        this.logState(logger, "Initial state", currentState);
        this.logState(logger, "Goal state", goalStateAsPredicateSet);
        
        while (!stack.isEmpty()) {
            this.logCurrentStack(logger, stack);
            Element element = stack.remove(stack.size() - 1);
            this.log(logger, "Analyzing top element " + element + " ");
            
            if (element instanceof Operator) {
                this.logln(logger, "(operator)");
                Operator operator = (Operator)element;
                this.logln(logger, "Applying operator " + operator + " postconditions to the current state:");
                i = operator.getPostconditions(currentState).iterator();
                
                while (i.hasNext()) {
                    Predicate postcondition = i.next();
                    this.logln(logger, "\t" + postcondition);
                    
                    if (postcondition.isNegated()) {
                        currentState.remove(postcondition.getInverse());
                    }
                    else {
                        currentState.add(new Predicate(postcondition));
                    }
                }
                
                this.logState(logger, "Current state", currentState);
                this.logln(logger, "Adding " + operator + " to the plan");
                plan.add(operator.copy());
                this.logCurrentPlan(logger, plan);
            }
            else if (element instanceof PredicateSet) {
                this.logln(logger, "(predicate set)");
                PredicateSet predicateSet = (PredicateSet)element;
                
                if (heuristicProvider == null) {
                    i = predicateSet.iterator();
                }
                else {
                    i = heuristicProvider.heuristicSortPredicateSet(currentState, stack, predicateSet).iterator();
                }
                
                this.logln(logger, "Analyzing predicates, one by one:");
                boolean setReinserted = false;
                
                while (i.hasNext()) {
                    Predicate predicate = i.next();
                    this.log(logger, "\t" + predicate);
                    
                    if (!currentState.contains(predicate)) {
                        if (!setReinserted) {
                            stack.add(element);
                            setReinserted = true;
                        }
                        
                        this.logln(logger, " (not included in the current state, added to the stack)");
                        stack.add(new Predicate(predicate));
                    }
                    else {
                        this.logln(logger, " (already included in the current state)");
                    }
                }
            }
            else if (element instanceof Predicate) {
                this.logln(logger, "(predicate)");
                Predicate predicate = (Predicate)element;
                
                if (predicate.isFullyInstantiated()) {
                    this.logln(logger, "Predicate " + predicate + " is fully instantiated");
                    
                    if (!currentState.contains(predicate)) {
                        this.logln(logger, "Predicate " + predicate + " is not included in the current state");
                        this.logln(logger, "Searching for an operator that reaches the predicate " + predicate + "...");
                        Operator operator = this.searchOperator(operators, currentState, stack, predicate, heuristicProvider, logger);
                        this.logln(logger, "Operator " + operator + " reaches the predicate " + predicate + ", added to the stack with the required preconditions");
                        stack.add(operator.copy());
                        PredicateSet operatorPreconditions = operator.getPreconditions(currentState);
                        
                        if (heuristicProvider == null) {
                            i = operatorPreconditions.iterator();
                        }
                        else {
                            i = heuristicProvider.heuristicSortPredicateSet(currentState, stack, operatorPreconditions).iterator();
                        }
                        
                        stack.add(new PredicateSet(operatorPreconditions));
                        
                        while (i.hasNext()) {
                            stack.add(new Predicate(i.next()));
                        }
                    }
                    else {
                        this.logln(logger, "Predicate " + predicate + " is already included in the current state");
                    }
                }
                else {
                    this.logln(logger, "Predicate " + predicate + " is partially instantiated");
                    this.logln(logger, "Searching for an instantiation for the predicate " + predicate + "...");
                    Map<String, Param> replacement = this.searchInstantiation(currentState, stack, predicate, heuristicProvider, logger);
                    this.logInstantiation(logger, replacement);
                    Iterator<Element> j = stack.iterator();
                    
                    while (j.hasNext()) {
                        j.next().replaceParams(replacement);
                    }
                    
                    this.logCurrentStack(logger, stack);
                }
            }
        }
        
        this.logState(logger, "Finish state", currentState);
        this.logln(logger, "STRIPS execution finished");
        this.logln(logger, "Plan to archieve the goal: " + plan);
        
        return plan;
    }
    
    private Operator searchOperator(Set<Operator> operators, PredicateSet state, ArrayList<Element> stack, Predicate desiredCondition, HeuristicProvider heuristicProvider, STRIPSLogger logger) throws OperatorNotFoundSTRIPSException {
        String desiredConditionName = desiredCondition.getName();
        boolean isDesiredConditionNegated = desiredCondition.isNegated();
        ArrayList<Param> desiredConditionParams = desiredCondition.getParams();
        Set<Operator> candidates = new HashSet<>();
        Iterator<Operator> i = operators.iterator();
        
        while (i.hasNext()) {
            Operator operator = i.next();
            this.logln(logger, "\tAnalyzing operator " + operator + ":");
            Iterator<Predicate> j = operator.getPostconditions(state).iterator();
            
            while (j.hasNext()) {
                Predicate postcondition = j.next();
                this.log(logger, "\t\tPostcondition " + postcondition + ": ");
                
                if (postcondition.getName().equals(desiredConditionName) && postcondition.isNegated() == isDesiredConditionNegated) {
                    this.logln(logger, "valid!");
                    operator = operator.copy();
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
                    break;
                }
                else {
                    this.logln(logger, "not valid");
                }
            }
        }
        
        if (candidates.isEmpty()) {
            throw new OperatorNotFoundSTRIPSException("Predicate \"" + desiredCondition + "\" cannot be reached with the given operators.");
        }
        
        return heuristicProvider.heuristicBestOperator(state, stack, candidates);
    }
    
    private Map<String, Param> searchInstantiation(PredicateSet state, ArrayList<Element> stack, Predicate partiallyInstantiatedPredicate, HeuristicProvider heuristicProvider, STRIPSLogger logger) throws InstantiationNotFoundSTRIPSException {
        String partiallyInstantiatedPredicateName = partiallyInstantiatedPredicate.getName();
        boolean isPartiallyInstantiatedPredicateNegated = partiallyInstantiatedPredicate.isNegated();
        ArrayList<Param> partiallyInstantiatedPredicateParams = partiallyInstantiatedPredicate.getParams();
        Set<Map<String, Param>> candidates = new HashSet<>();
        Iterator<Predicate> i = state.iterator();
        
        while (i.hasNext()) {
            Predicate statePredicate = i.next();
            this.log(logger, "\tAnalyzing predicate included in the current state " + statePredicate + ": ");
            
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
                    this.logln(logger, "valid!");
                    
                    if (heuristicProvider == null) {
                        return replacement;
                    }
                    
                    candidates.add(replacement);
                }
                else {
                    this.logln(logger, "not valid");
                }
            }
            else {
                this.logln(logger, "not valid");
            }
        }
        
        if (candidates.isEmpty()) {
            throw new InstantiationNotFoundSTRIPSException("Predicate \"" + partiallyInstantiatedPredicate + "\" cannot be instantiated in the state \"" + state + "\".");
        }
        
        return heuristicProvider.heuristicBestInstantiation(state, stack, partiallyInstantiatedPredicate, candidates);
    }
    
    private void logln(STRIPSLogger logger, String message) {
        this.log(logger, message + "\r\n");
    }
    
    private void log(STRIPSLogger logger, String message) {
        if (logger == null) {
            return;
        }
        
        logger.logSTRIPS(message);
    }
    
    private void logState(STRIPSLogger logger, String stateName, PredicateSet state) {
        this.logln(logger, stateName + ": " + state);
    }
    
    private void logCurrentStack(STRIPSLogger logger, ArrayList<Element> stack) {
        if (logger == null) {
            return;
        }
        
        this.logln(logger, "Current stack:");
        int i = stack.size() - 1;
        
        while (i >= 0) {
            this.logln(logger, "\t" + stack.get(i));
            --i;
        }
    }
    
    private void logCurrentPlan(STRIPSLogger logger, ArrayList<Operator> plan) {
        this.logln(logger, "Current plan: " + plan);
    }
    
    private void logInstantiation(STRIPSLogger logger, Map<String, Param> replacement) {
        if (logger == null) {
            return;
        }
        
        this.logln(logger, "Instantiation found:");
        Iterator<Map.Entry<String, Param>> i = replacement.entrySet().iterator();
        
        while (i.hasNext()) {
            Map.Entry entry = i.next();
            this.logln(logger, "\t[" + entry.getKey() + "] := " + entry.getValue());
        }
    }
}
