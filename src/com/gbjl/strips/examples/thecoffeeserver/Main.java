package com.gbjl.strips.examples.thecoffeeserver;

import com.gbjl.strips.Element;
import com.gbjl.strips.HeuristicProvider;
import com.gbjl.strips.Operator;
import com.gbjl.strips.Param;
import com.gbjl.strips.Predicate;
import com.gbjl.strips.PredicateSet;
import com.gbjl.strips.STRIPSException;
import com.gbjl.strips.Solver;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Main implements HeuristicProvider {
    private Set<Predicate> initialState;
    private Set<Predicate> goalState;
    private Set<Operator> operators;
    
    public static void main(String[] args) {
        Main main = new Main(args);
        main.run();
    }
    
    private Main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar gbjl-strips.jar <input file>");
        }
        
        try {
            InputStream inStream = new FileInputStream(new File(args[0]));
            Properties input = new Properties();
            input.load(inStream);
            
            this.initialState = this.readState(input, "InitialState");
            this.goalState = this.readState(input, "GoalState");
            this.operators = this.createOperators();
            
            // TODO: add default conditions to the initial state, such as Robot-free and Steps(0)
        } catch (Exception ex) {
            System.err.println("Error. " + ex.getMessage());
        }
    }
    
    private void run() {
        Solver solver = new Solver();
        
        try {
            ArrayList<Operator> plan = solver.solve(this.operators, this.initialState, this.goalState, this);
            System.out.println(plan);
        } catch (STRIPSException ex) {
            System.err.println("Error. " + ex.getMessage());
        }
    }
    
    private Set<Predicate> readState(Properties input, String key) throws Exception {
        if (!input.contains(key)) {
            throw new Exception("State \"" + key + "\" not found in the input file.");
        }
        
        return PredicateSet.fromString(input.getProperty(key));
    }
    
    private Set<Operator> createOperators() {
        Set<Operator> operators = new HashSet<>();
        
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
        return new ArrayList<>(predicateSet);
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
