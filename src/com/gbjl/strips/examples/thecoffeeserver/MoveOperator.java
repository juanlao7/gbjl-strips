package com.gbjl.strips.examples.thecoffeeserver;

import com.gbjl.strips.Operator;
import com.gbjl.strips.Param;
import com.gbjl.strips.Predicate;
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
        // Postcondition Steps(x + distance(o1, o2)) and ¬Steps(x) will be added on the fly.
    }
    
    @Override
    public Set<Predicate> getPostconditions(Set<Predicate> state) {
        Set<Predicate> postconditions = super.getPostconditions(state);
        Iterator<Predicate> i = state.iterator();
        
        while (i.hasNext()) {
            Predicate predicate = i.next();
            
            if (predicate.getName().equals("Steps")) {
                int currentSteps = Integer.parseInt(predicate.getParams().get(0).getName());
                int distance = 1;       // TODO: calculate manhattan distance
                postconditions.add(new Predicate("Steps", true, new String[]{(currentSteps) + ""}, true));
                postconditions.add(new Predicate("Steps", false, new String[]{(currentSteps + distance) + ""}, true));
                break;
            }
        }
        
        return postconditions;
    }
}
