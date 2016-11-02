package com.gbjl.strips.examples.thecoffeeserver;

import com.gbjl.strips.Element;
import com.gbjl.strips.HeuristicProvider;
import com.gbjl.strips.Operator;
import com.gbjl.strips.Param;
import com.gbjl.strips.Predicate;
import com.gbjl.strips.PredicateSet;
import com.gbjl.strips.STRIPSLogger;
import com.gbjl.strips.Solver;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Main implements HeuristicProvider, STRIPSLogger {
    private Set<Predicate> initialState;
    private Set<Predicate> goalState;
    private Set<Operator> operators;
    private FileOutputStream output;
    
    public static void main(String[] args) {
        try {
            Main main = new Main(args);
            main.run();
        } catch (Exception e) {
            /*System.out.flush();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }*/
            
            System.err.println("Error. " + e.getMessage());
            
            // TODO: do not show the stack trace
            //e.printStackTrace();
            
            System.exit(1);
        }
    }
    
    private Main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new Exception("Wrong number of parameters. Usage: java -jar gbjl-strips.jar <input file> <output file>");
        }
        
        this.output = new FileOutputStream(args[1]);
        
        InputStream inStream = new FileInputStream(args[0]);
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
        solver.solve(this.operators, this.initialState, this.goalState, this, this);
    }
    
    private Set<Predicate> readState(Properties input, String key) throws Exception {
        if (!input.containsKey(key)) {
            throw new Exception("State \"" + key + "\" not found in the input file.");
        }
        
        return PredicateSet.fromString(input.getProperty(key));
    }
    
    @Override
    public ArrayList<Predicate> heuristicSortPredicateSet(PredicateSet currentState, ArrayList<Element> currentStack, PredicateSet predicateSet) {
        // We are sorting the preconditions of a cetain operator
        if (currentStack.size() > 1){
            return predicateSet.sortPredicatesByName(new String[]{"Steps", "Robot-location", "Machine", "Robot-loaded", "Robot-free", "Petition"});
        }
        
        // We are sorting the goal state
        // The first element of the array is the first element to be stacked in the stack.
        ArrayList<Predicate> sortedPredicateList = new ArrayList<>(predicateSet.size());
        PredicateSet servedPredicates = predicateSet.getPredicatesByName("Served");
        
        // We create two array list. One will be filled with the sequence of nearest 
        // "served" offices starting from the initial position, and the other with an 
        // analogous sequence starting from the final position
        
        ArrayList<Predicate> firstServedPredicates = new ArrayList<>();
        ArrayList<Predicate> lastServedPredicates = new ArrayList<>();
        
        // We get the initial and final positions of the robot
        Param initialPosition = currentState.getPredicatesByName("Robot-location").iterator().next().getParams().get(0);
        Param finalPosition = predicateSet.getPredicatesByName("Robot-location").iterator().next().getParams().get(0);
        
        for (int i = 0; !servedPredicates.isEmpty() ; i++){
            // Each even step we will add an element to the list of first served predicates
            // finding the nearest one to the last element of that list.
            // Each odd step we do the same with thelist of last served predicates
            // In both cases, we delete the selected element from servedPredicates list
            if (i % 2 == 0){
                Iterator<Predicate> j = servedPredicates.iterator();
                Predicate nextPredicate = j.next();
                Param nextPosition = nextPredicate.getParams().get(0);
                int minDistance = MoveOperator.getManhattanDistance(initialPosition, nextPosition);

                while(j.hasNext()){
                    Predicate predicate = j.next();
                    Param position = predicate.getParams().get(0);
                    int distance = MoveOperator.getManhattanDistance(initialPosition, position);
                    if (distance < minDistance){
                        minDistance = distance;
                        nextPosition = position;
                        nextPredicate = predicate;
                    }
                }
                initialPosition = nextPosition;
                firstServedPredicates.add(nextPredicate);
                servedPredicates.remove(nextPredicate);
            } else {
                Iterator<Predicate> j = servedPredicates.iterator();
                Predicate nextPredicate = j.next();
                Param nextPosition = nextPredicate.getParams().get(0);
                int minDistance = MoveOperator.getManhattanDistance(finalPosition, nextPosition);

                while(j.hasNext()){
                    Predicate predicate = j.next();
                    Param position = predicate.getParams().get(0);
                    int distance = MoveOperator.getManhattanDistance(finalPosition, position);
                    if (distance < minDistance){
                        minDistance = distance;
                        nextPosition = position;
                        nextPredicate = predicate;
                    }
                }
                finalPosition = nextPosition;
                lastServedPredicates.add(nextPredicate);
                servedPredicates.remove(nextPredicate);
            }
        }
        
        // First we add all possible predicates that are not "Served" in the correct order
        sortedPredicateList.addAll(predicateSet.sortPredicatesByName(new String[]{"Petition", "Robot-location", "Robot-loaded"}));
        // Then we add those served predicates that have been chosen from the final position
        sortedPredicateList.addAll(lastServedPredicates);
        // Finally, we add the ones chosen from the initial position in the correct order
        // To obtain the correct order, we must reverse the arraylist
        Collections.reverse(firstServedPredicates);
        sortedPredicateList.addAll(firstServedPredicates);

        return sortedPredicateList;
    }    
 
    @Override
    public Operator heuristicBestOperator(PredicateSet currentState, ArrayList<Element> currentStack, Set<Operator> operators) {
        /*
         * In this problem, there is no interesection on the postconditions of the operators.
         * For this reason, we return the uniq proposed operator.
         */
        return operators.iterator().next();
    }

    @Override
    public Map<String, Param> heuristicBestInstantiation(PredicateSet currentState, ArrayList<Element> currentStack, Predicate partiallyInstantiatedPredicate, Set<Map<String, Param>> instantiations) {
        String partiallyInstantiatedPredicateName = partiallyInstantiatedPredicate.getName();
        
        if (partiallyInstantiatedPredicateName.equals("Machine")) {
            /*
             * We need to find the closest machine to the petition we want to serve.
             * We need to know where we must serve the coffee, so we iterate the stack until we find a Serve operator.
             * We need to know our current position.
             */
            
            Param currentOffice = currentState.getPredicatesByName("Robot-location").iterator().next().getParams().get(0);
            
            for (int i = currentStack.size() - 1; i >= 0; --i) {
                Element element = currentStack.get(i);
                
                if (element instanceof Operator) {
                    Operator operator = (Operator)element;
                    
                    if (operator.getName().equals("Serve")) {
                        Param serveOffice = operator.getParams().get(0);
                        int minDistance = -1;
                        Map<String, Param> bestInstantiation = null;
                        Iterator<Map<String, Param>> j = instantiations.iterator();
                        
                        while (j.hasNext()) {
                            Map<String, Param> instantiation = j.next();
                            Param machineOffice = instantiation.values().iterator().next();
                            int distance = MoveOperator.getManhattanDistance(currentOffice, machineOffice) + MoveOperator.getManhattanDistance(machineOffice, serveOffice);
                            
                            if (minDistance == -1 || distance < minDistance) {
                                minDistance = distance; 
                                bestInstantiation = instantiation;
                            }
                        }
                        
                        return bestInstantiation;
                    }
                }
            }
        }
        
        /*
         * For the rest of partially instantiated predicates, there is only one possible instantiation.
         * Those parameters are: Robot-location(x), Steps(x), Petition(O, x)
         */
        return instantiations.iterator().next();
    }

    @Override
    public void logSTRIPS(String message) {
        try {
            this.output.write(message.getBytes());
        } catch (IOException ex) {
        }
        
        // TODO: do not show the output on the stdout
        //System.out.print(message);
    }
}
