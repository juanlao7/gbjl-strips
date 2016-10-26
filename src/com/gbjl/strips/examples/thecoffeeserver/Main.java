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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            System.out.flush();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            
            System.err.println("Error. " + e.getMessage());
            
            // TODO: do not show the stack trace
            e.printStackTrace();
            
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
    
    /*private Param findNearestMachine(Param initialPoint, Param finalPoint, PredicateSet currentState, Param serves){
        int shortestDistance;
        int distance;
        Iterator<Predicate> i = currentState.iterator();
        
        do {
            Predicate predicate = i.next();
            String predicateName = predicate.getName();
        } while (!predicateName.equals("Machine"));
        
        Param midPoint = new Param(predicate.getParams().get(0));
        shortestDistance = MoveOperator.getManhattanDistance(initialPoint, midPoint) + MoveOperator.getManhattanDistance(midPoint, finalPoint);
        Param positionNearestMachine = new Param(midPoint);
        
        while (i.hasNext()) {
            predicate = i.next();
            predicateName = predicate.getName();
            
            if (predicateName.equals("Machine")){
                midPoint = predicate.getParams().get(0);
                distance = MoveOperator.getManhattanDistance(initialPoint, midPoint) + MoveOperator.getManhattanDistance(midPoint, finalPoint);
                if (distance < shortestDistance){
                    shortestDistance = distance;
                    positionNearestMachine = midPoint;
                }
            }
        }
        Map<Param, int> shortestPath = new HashMap<Param, Integer>(positionNearestMachine, shortestDistance);
        return shortestPath;
    }    
    
    // variable in which we will store the map of a given location and the location of the nearest machine to make the following caffe
    private Set<Map<Param, Param>> pathChoices = new HashSet<>();
        
    //@Override
    /*public ArrayList<Predicate> heuristicSortPredicateSetGuille(Set<Predicate> currentState, ArrayList<Element> currentStack, PredicateSet predicateSet) {
        // The last element of the array is the first element to be stacked in the stack.
        ArrayList<Predicate> sortedPredicateList = new ArrayList<>(predicateSet.size());
        
        // We create a new predicate set with just the Served predicates
        PredicateSet servedPredicates = new PredicateSet();
        
        Iterator<Predicate> i = predicateSet.iterator();
            
        while (i.hasNext()) {
            Predicate predicate = i.next();
            String predicateName = predicate.getName();
                
            if (predicateName.equals("Served")) {
                servedPredicates.add(predicate);
            }
        }
        
        if (servedPredicates.isEmpty()) {
            // TODO: no podemos retornar esto vacio
            return sortedPredicateList;
        }

        i = currentState.iterator();
        Param initialPoint;
        
        while (i.hasNext()){
            Predicate predicate = i.next();
            String predicateName = predicate.getName();
            
            if(predicateName.equals("Robot-location")) {
                initialPoint = predicate.getParams().get(0);
                break;
            }
        }

        do {
            i = servedPredicates.iterator();
            Predicate predicate = i.next();
            Param serveLocation = predicate.getParams().get(0);
            Predicate nextPredicate = predicate;
            Map<Param, Integer> shortestPath = findNearestMachine(initialPoint, finalPoint, currentState );
            Iterator<Param> j = shortestPath.keySet().iterator();
            Param machineLocation = j.next();
            int shortestDistance = shortestPath.get(midPoint);

            while(i.hasNext()){
                predicate = i.next();
                Param finalPoint = predicate.getParams().get(0);
                shortestPath = findNearestMachine(initialPoint, finalPoint, currentState );
                j = shortestPath.keySet().iterator();
                Param midPoint = j.next();
                int distance = shortestPath.get(midPoint);
                if (distance < shortestDistance){
                    nextPredicate = predicate;
                    serveLocation = finalPoint;
                    machineLocation = midPoint;
                    shortestDistance = distance;
                }
            }

            pathChoices.add(new HashMap<>(initialPoint, machineLocation));
            initialPoint = serveLocation;
            sortedPredicateList.add(nextPredicate);
            servedPredicates.remove(nextPredicate);

        } while (!servedPredicates.isEmpty());
        
        // FALTA AÑADIR EL RESTO DE PREDICADOS
        // DUDO SOBRE SI DISTINGUIR ENTRE PROCEDENCIA DE OPERADORES O LISTARLOS DE FORMA GENERAL
        
        return sortedPredicateList;
    }*/
    
    @Override
    public ArrayList<Predicate> heuristicSortPredicateSet(PredicateSet currentState, ArrayList<Element> currentStack, PredicateSet predicateSet) {
        // The first element of the array is the first element to be stacked in the stack.
        
        if (currentStack.size() <= 1) {
            // We are sorting the goal state
            // TODO: aquí va la heuristica que falta
            return predicateSet.sortPredicatesByName(new String[]{"Robot-location", "Served"});
        }
        
        Element topElementAfterTheSet = currentStack.get(currentStack.size() - 1);
        
        if (!(topElementAfterTheSet instanceof Operator)) {
            return predicateSet.toArrayList();
        }
        
        Operator operator = (Operator)topElementAfterTheSet;
        
        if (operator.getName().equals("Make")) {
            return predicateSet.sortPredicatesByName(new String[]{"Robot-location", "Machine", "Robot-free"});
        }
        else if (operator.getName().equals("Serve")) {
            return predicateSet.sortPredicatesByName(new String[]{"Robot-location", "Robot-loaded", "Petition"});
        }
        
        return predicateSet.toArrayList();
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
        System.out.print(message);
    }
}
