package com.gbjl.strips.examples.thecoffeeserver;

import com.gbjl.strips.Element;
import com.gbjl.strips.HeuristicProvider;
import com.gbjl.strips.InstantiationNotFoundSTRIPSException;
import com.gbjl.strips.Operator;
import com.gbjl.strips.OperatorNotFoundSTRIPSException;
import com.gbjl.strips.Param;
import com.gbjl.strips.Predicate;
import com.gbjl.strips.PredicateSet;
import com.gbjl.strips.STRIPSException;
import com.gbjl.strips.Solver;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Main implements HeuristicProvider {
    public static void main(String[] args) {
        Main main = new Main();
        main.run();
    }
    
    private Main() {
    }
    
    private void run() {
        Set<Operator> operators = this.createOperators();
        
        // TODO: read initial and goal state from a file.
        // TODO: add default conditions to the initial state, such as Robot-free and Steps(0)
        
        Set<Predicate> initialState = new HashSet<Predicate>();
        
        Set<Predicate> goalState = new HashSet<Predicate>();
        
        Solver solver = new Solver();
        
        try {
            ArrayList<Operator> plan = solver.solve(operators, initialState, goalState, this);
            // TODO: success message
        } catch (OperatorNotFoundSTRIPSException ex) {
            // TODO: error message
        } catch (InstantiationNotFoundSTRIPSException ex) {
            // TODO: error message            
        } catch (STRIPSException ex) {
            // TODO: error message            
        }
    }
    
    private Set<Operator> createOperators() {
        Set<Operator> operators = new HashSet<Operator>();
        
        // Make
        Operator makeOperator = new Operator("Make");
        makeOperator.addParam(new Param("o", false));
        makeOperator.addParam(new Param("n", false));
        
        makeOperator.addPrecondition(new Predicate("Robot-location", false, new String[]{"o"}, false));
        makeOperator.addPrecondition(new Predicate("Robot-free", false));
        makeOperator.addPrecondition(new Predicate("Machine", false, new String[]{"o", "n"}, false));
        
        makeOperator.addPostcondition(new Predicate("Robot-loaded", false, new String[]{"n"}, false));
        makeOperator.addPostcondition(new Predicate("Robot-free", true));
        
        operators.add(makeOperator);
        
        // Move
        operators.add(new MoveOperator());     // This is a complex operator due to the Steps(x + distance(o1,o2)) postcondition. It requires an extension of the Operator class.
        
        // Serve
        Operator serveOperator = new Operator("Serve");
        serveOperator.addParam(new Param("o", false));
        serveOperator.addParam(new Param("n", false));
        
        serveOperator.addPrecondition(new Predicate("Robot-location", false, new String[]{"o"}, false));
        serveOperator.addPrecondition(new Predicate("Robot-loaded", false, new String[]{"n"}, false));
        serveOperator.addPrecondition(new Predicate("Petition", false, new String[]{"o", "n"}, false));
        
        serveOperator.addPostcondition(new Predicate("Served", false, new String[]{"o"}, false));
        serveOperator.addPostcondition(new Predicate("Robot-free", false));
        serveOperator.addPostcondition(new Predicate("Petition", true, new String[]{"o", "n"}, false));
        serveOperator.addPostcondition(new Predicate("Robot-loaded", true, new String[]{"n"}, false));
        
        operators.add(serveOperator);
        
        return operators;
    }

    @Override
    public ArrayList<Predicate> heuristicSortPredicateSet(Set<Predicate> currentState, ArrayList<Element> currentStack, PredicateSet predicateSet) {
        // TODO: fill this function
        // The first element of the array is the first element to be stacked in the stack.
        return new ArrayList<Predicate>(predicateSet);
    }

    @Override
    public Operator heuristicBestOperator(Set<Predicate> currentState, ArrayList<Element> currentStack, Set<Operator> operators) {
        // TODO: fill this function
        return operators.iterator().next();
    }

    @Override
    public Map<String, Param> heuristicBestInstantiation(Set<Predicate> currentState, ArrayList<Element> currentStack, Set<Map<String, Param>> instantiations) {
        // TODO: fill this function
        return instantiations.iterator().next();
    }
}
