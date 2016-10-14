package com.gbjl.strips;

public class Param {
    private String name;
    private boolean instantiated;
    
    public Param(String name, boolean instantiated) {
        this.name = name;
        this.instantiated = instantiated;
    }
    
    public String getName() {
        return this.name;
    }
    
    public boolean isInstantiated() {
        return this.instantiated;
    }
}
