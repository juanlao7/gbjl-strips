package com.gbjl.strips;

public class Param {
    private final String name;
    private final boolean instantiated;
    
    public Param(String name, boolean instantiated) {
        this.name = name;
        this.instantiated = instantiated;
    }
    
    public Param(Param original) {
        this.name = original.name;
        this.instantiated = original.instantiated;
    }
    
    public String getName() {
        return this.name;
    }
    
    public boolean isInstantiated() {
        return this.instantiated;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Param) {
            Param otherParam = (Param)other;
            return (this.name.equals(otherParam.name) && this.instantiated == otherParam.instantiated);
        }
        
        return (this == other);
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + this.name.hashCode();
        hash = hash * 31 + (this.instantiated ? 0 : 1);
        return hash;
    }
    
    @Override
    public String toString() {
        if (this.instantiated) {
            return this.name;
        }
        
        return "[" + this.name + "]";
    }
}
