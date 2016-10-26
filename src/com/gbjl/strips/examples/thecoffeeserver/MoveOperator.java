package com.gbjl.strips.examples.thecoffeeserver;

import com.gbjl.strips.Operator;
import com.gbjl.strips.Param;
import com.gbjl.strips.Predicate;
import com.gbjl.strips.PredicateSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class MoveOperator extends Operator {
    public MoveOperator() {
        super("Move");
        this.addParam(new Param("o1", false));
        this.addParam(new Param("o2", false));
        
        this.addPrecondition(new Predicate("Robot-location", false, new String[]{"o1"}, false));
        // Precondition Steps(x) is not included, since it will be always true.
        
        this.addPostcondition(new Predicate("Robot-location", false, new String[]{"o2"}, false));
        this.addPostcondition(new Predicate("Robot-location", true, new String[]{"o1"}, false));
        // Postconditions ¬Steps(x) and Steps(x + distance(o1, o2)) will be added on the fly.
    }
    
    @Override
    public Operator copy() {
        MoveOperator copy = new MoveOperator();
        copy.params = new ArrayList<>(this.params);
        copy.preconditions = new PredicateSet(this.preconditions);
        copy.postconditions = new PredicateSet(this.postconditions);
        return copy;
    }
    
    @Override
    public PredicateSet getPostconditions(Set<Predicate> state) {
        PredicateSet originalPostconditions = super.getPostconditions(state);
        Iterator<Predicate> i = state.iterator();
        
        while (i.hasNext()) {
            Predicate predicate = i.next();
            
            if (predicate.getName().equals("Steps")) {
                int currentSteps = Integer.parseInt(predicate.getParams().get(0).getName());
                originalPostconditions.add(new Predicate("Steps", true, new String[]{(currentSteps) + ""}, true));
                int manhattanDistance = getManhattanDistance(this.params.get(0), this.params.get(1));
                originalPostconditions.add(new Predicate("Steps", false, new String[]{(currentSteps + manhattanDistance) + ""}, true));
                break;
            }
        }
        
        return originalPostconditions;
    }
    
    public static int getManhattanDistance(Param o1, Param o2) {
        if (!o1.isInstantiated() || !o2.isInstantiated()) {
            return 0;
        }
        
        // We substract 1 to have a start index at 0.
        int o1Int = Integer.parseInt(o1.getName().substring(1)) - 1;
        int o2Int = Integer.parseInt(o2.getName().substring(1)) - 1;
        
        int o1X = o1Int % 6;
        int o1Y = o1Int / 6;
        
        int o2X = o2Int % 6;
        int o2Y = o2Int / 6;
        
        return Math.abs(o1X - o2X) + Math.abs(o1Y - o2Y);
    }
}
