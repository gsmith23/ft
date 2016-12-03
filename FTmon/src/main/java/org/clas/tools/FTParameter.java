/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.awt.Color;
import org.root.attr.ColorPalette;

/**
 *
 * @author devita
 */
public class FTParameter {
    
    private String name;
    private double parValue;
    private double parMin;
    private double parMax;
    private double parScale;
    private double parLimit;
    
    public FTParameter(String n) {
        this.name = n;
    }

    public FTParameter(String n, double min, double max) {
        this.name = n;
        this.parMin = min;
        this.parMax = max;
    }

    public String getName() {
        return this.name;
    }

    public double getValue() {
        return this.parValue;
    }
        
    public double getParMin() {
        return this.parMin;
    }

    public double getParMax() {
        return this.parMax;
    }

    public double getParScale() {
        return this.parScale;
    }
    
    public double getParLimit() {
        return this.parLimit;
    }
        
    public void setName(String name) {
        this.name = name;
    }

    public void setValue(double value) {
        this.parValue = value;
    }
            
    public void setMin(double parMin) {
        this.parMin = parMin;
    }

    public void setMax(double parMax) {
        this.parMax = parMax;
    }
    
    public void setRange(double parMin, double parMax) {
        this.parMin = parMin;
        this.parMax = parMax;
    }
    
    public void setRanges(double parMin, double parMax, double parScale, double parLimit) {
        this.parMin = parMin;
        this.parMax = parMax;
        this.parScale = parScale;
        this.parLimit = parLimit;
    }
    
    public void setScale(double parScale) {
        this.parScale = parScale;
    }
    
    public void setLimit(double parLimit) {
        this.parLimit = parLimit;
    }
    
    public boolean isValid(double value) {
        boolean stat = false;
        this.setValue(value);
        if(value>=this.parMin && value<=this.parMax) stat=true;
        return stat;
    }
    
    public boolean isLow(double value) {
        boolean stat = false;
        this.setValue(value);
        if(value<this.parMin) stat=true;
        return stat;
    }
    
    public boolean isHigh(double value) {
        boolean stat = false;
        this.setValue(value);
        if(value>this.parMax) stat=true;
        return stat;
    }
    
    public Color getColor(double value, boolean bool) {
        ColorPalette palette = new ColorPalette();
        Color col = new Color(100, 100, 100);
        if(bool) {
            if(this.isValid(value))    col = new Color(0, 145, 0);
            else if(this.isLow(value)) col = new Color(0, 0, 100);
            else                       col = new Color(255, 100, 0);             
        }
        else {
            col = palette.getColor3D(value*this.parScale, this.parLimit*this.parScale, true);
         
        }
        return col;
    }
}