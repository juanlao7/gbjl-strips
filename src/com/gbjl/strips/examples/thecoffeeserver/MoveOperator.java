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
                int distance = 1;       // TODO: calculate manhattan distance
                originalPostconditions.add(new Predicate("Steps", true, new String[]{(currentSteps) + ""}, true));
                originalPostconditions.add(new Predicate("Steps", false, new String[]{(currentSteps + distance) + ""}, true));
                break;
            }
        }
        
        return originalPostconditions;
    }
}
