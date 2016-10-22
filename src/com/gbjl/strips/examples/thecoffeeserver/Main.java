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
    
    
    int ManhattanDistance(Param o1, Param o2) {
        
        // We substract 1 to have a start index at 0.
        int o1Int = Integer.parseInt(o1.getName().substring(1)) - 1;
        int o2Int = Integer.parseInt(o2.getName().substring(1)) - 1;
        
        int o1X = o1Int % 6;
        int o1Y = o1Int / 6;
        
        int o2X = o2Int % 6;
        int o2Y = o2Int / 6;
        
        return Math.abs(o1X - o2X) + Math.abs(o1Y - o2Y);
    }
    
    private Map<Param, int> findNearestMachine(Param initialPoint, Param finalPoint, Set<Predicate> currentState){
        int shortestDistance;
        int distance;
        Iterator<Predicate> i = currentState.iterator();
        
        do {
            Predicate predicate = i.next();
            String predicateName = predicate.getName();
        } while (!predicateName.equals("Machine"));
        
        Param midPoint = new Param(predicate.getParams().get(0));
        shortestDistance = ManhattanDistance(initialPoint, midPoint) + ManhattanDistance(midPoint, finalPoint);
        Param positionNearestMachine = new Param(midPoint);
        
        while (i.hasNext()) {
            predicate = i.next();
            predicateName = predicate.getName();
            
            if (predicateName.equals("Machine")){
                midPoint = predicate.getParams().get(0);
                distance = ManhattanDistance(initialPoint, midPoint) + ManhattanDistance(midPoint, finalPoint);
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
    public ArrayList<Predicate> heuristicSortPredicateSetGuille(Set<Predicate> currentState, ArrayList<Element> currentStack, PredicateSet predicateSet) {
        // The last element of the array is the first element to be stacked in the stack.
        ArrayList<Predicate> sortedPredicateList = new ArrayList<>(predicateSet.size());
        PredicateSet servedPredicates = new PredicateSet();
        
        Iterator<Predicate> i = predicateSet.iterator();
            
        while (i.hasNext()) {
            Predicate predicate = i.next();
            String predicateName = predicate.getName();
                
            if (predicateName.equals("Served")) {
                servedPredicates.add(predicate);
            }
        }
        
        if(!servedPredicates.isEmpty()){
            i = currentState.iterator();
            Param initialPoint;
            while(i.hasNext()){
                Predicate predicate = i.next();
                String predicateName = predicate.getName();
                if(predicateName.equals("Robot-location")) {
                    initialPoint = predicate.getParams().get(0);
                }
            }
            
            do{
                i = servedPredicates.iterator();
                Predicate predicate = i.next();
                Param serveLocation = predicate.getParams().get(0);
                Predicate nextPredicate = new Predicate(predicate);
                Map<Param, int> shortestPath = findNearestMachine(initialPoint, finalPoint, currentState );
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
        }
        
        // FALTA AÑADIR EL RESTO DE PREDICADOS
        // DUDO SOBRE SI DISTINGUIR ENTRE PROCEDENCIA DE OPERADORES O LISTARLOS DE FORMA GENERAL
        
        return sortedPredicateList;
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
        try {
            this.output.write(message.getBytes());
        } catch (IOException ex) {
        }
        
        // TODO: do not show the output on the stdout
        System.out.print(message);
    }
}
