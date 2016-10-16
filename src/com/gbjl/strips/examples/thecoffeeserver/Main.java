package com.gbjl.strips.examples.thecoffeeserver;

import com.gbjl.strips.Element;
import com.gbjl.strips.HeuristicProvider;
import com.gbjl.strips.Operator;
import com.gbjl.strips.Param;
import com.gbjl.strips.Predicate;
import com.gbjl.strips.PredicateSet;
import com.gbjl.strips.STRIPSLogger;
import com.gbjl.strips.Solver;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Main implements HeuristicProvider, STRIPSLogger {
    private Set<Predicate> initialState;
    private Set<Predicate> goalState;
    private Set<Operator> operators;
    
    public static void main(String[] args) {
        try {
            Main main = new Main(args);
            main.run();
        } catch (Exception e) {
            System.out.flush();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            
            System.err.println("Error. " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private Main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new Exception("Wrong number of parameters. Usage: java -jar gbjl-strips.jar <input file>");
        }
        
        InputStream inStream = new FileInputStream(new File(args[0]));
        Properties input = new Properties();
        input.load(inStream);

        this.initialState = this.readState(input, "InitialState");
        this.goalState = this.readState(input, "GoalState");

        // Fix: add default conditions to the initial state, Robot-free and Steps(0)
        boolean addRobotFree = true;
        boolean addSteps0 = true;
        Iterator<Predicate> i = this.initialState.iterator();
        
        while (i.hasNext()) {
            String predicateName = i.next().getName();
            
            if (predicateName.equals("Robot-free") || predicateName.equals("Robot-loaded")) {
                addRobotFree = false;
            }
            else if (predicateName.equals("Steps")) {
                addSteps0 = false;
            }
        }
        
        if (addRobotFree) {
            this.initialState.add(new Predicate("Robot-free", false));
        }
        
        if (addSteps0) {
            this.initialState.add(new Predicate("Steps", false, new String[]{"0"}, true));
        }
        
        this.operators = new HashSet<>();
        
        // Make operator
        Operator makeOperator = new Operator("Make");
        makeOperator.addParam(new Param("o", false));
        makeOperator.addParam(new Param("n", false));
        
        makeOperator.addPrecondition(new Predicate("Robot-location", false, new String[]{"o"}, false));
        makeOperator.addPrecondition(new Predicate("Robot-free", false));
        makeOperator.addPrecondition(new Predicate("Machine", false, new String[]{"o", "n"}, false));
        
        makeOperator.addPostcondition(new Predicate("Robot-loaded", false, new String[]{"n"}, false));
        makeOperator.addPostcondition(new Predicate("Robot-free", true));
        
        this.operators.add(makeOperator);
        
        // Move operator
        this.operators.add(new MoveOperator());     // This is a complex operator due to the Steps(x + distance(o1,o2)) postcondition. It requires an extension of the Operator class.
        
        // Serve operator
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
        
        this.operators.add(serveOperator);
    }
    
    private void run() throws Exception {
        Solver solver = new Solver();
        ArrayList<Operator> plan = solver.solve(this.operators, this.initialState, this.goalState, this, this);
        System.out.println(plan);
    }
    
    private Set<Predicate> readState(Properties input, String key) throws Exception {
        if (!input.containsKey(key)) {
            throw new Exception("State \"" + key + "\" not found in the input file.");
        }
        
        return PredicateSet.fromString(input.getProperty(key));
    }

    @Override
    public ArrayList<Predicate> heuristicSortPredicateSet(Set<Predicate> currentState, ArrayList<Element> currentStack, PredicateSet predicateSet) {
        // The first element of the array is the first element to be stacked in the stack.
        
        if (currentStack.size() < 2) {
            return predicateSet.toArrayList();
        }
        
        Element topElementAfterTheSet = currentStack.get(currentStack.size() - 2);
        
        if (!(topElementAfterTheSet instanceof Operator)) {
            return predicateSet.toArrayList();
        }
        
        Operator operator = (Operator)topElementAfterTheSet;
        
        if (operator.getName().equals("Make")) {
            ArrayList<Predicate> sortedPredicateList = new ArrayList<>(Arrays.asList(null, null, null));
            Iterator<Predicate> i = predicateSet.iterator();
            
            while (i.hasNext()) {
                Predicate predicate = i.next();
                String predicateName = predicate.getName();
                
                if (predicateName.equals("Robot-location")) {
                    sortedPredicateList.set(0, predicate);
                }
                else if (predicateName.equals("Machine")) {
                    sortedPredicateList.set(1, predicate);
                }
                else if (predicateName.equals("Robot-free")) {
                    sortedPredicateList.set(2, predicate);
                }
            }
            
            return sortedPredicateList;
        }
        else if (operator.getName().equals("Serve")) {
            ArrayList<Predicate> sortedPredicateList = new ArrayList<>(Arrays.asList(null, null, null));
            Iterator<Predicate> i = predicateSet.iterator();
            
            while (i.hasNext()) {
                Predicate predicate = i.next();
                String predicateName = predicate.getName();
                
                if (predicateName.equals("Robot-location")) {
                    sortedPredicateList.set(0, predicate);
                }
                else if (predicateName.equals("Robot-loaded")) {
                    sortedPredicateList.set(1, predicate);
                }
                else if (predicateName.equals("Petition")) {
                    sortedPredicateList.set(2, predicate);
                }
            }
            
            return sortedPredicateList;
        }
        
        return predicateSet.toArrayList();
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

    @Override
    public void logSTRIPS(String message) {
        // TODO: write this to the output file.
        System.out.print(message);
    }
}
