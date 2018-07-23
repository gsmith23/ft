/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.util.Set;
import org.clas.tools.FTDetector;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.calib.DetectorShape2D;

/**
 *
 * @author smith, devita
 */
public class FTHODODetector extends FTDetector {
    
    String viewName;
    
    private final int nCrystalX = 22;
    private final int nCrystalY = nCrystalX;
    
    private final double crystal_size = 15;
    
    private double x0=0;
    private double y0=0;
    
    private DetectorCollection<ShapePoint> points = new DetectorCollection<ShapePoint>();
    DetectorCollection<Double> thresholds = new DetectorCollection<Double>();
        

    public FTHODODetector(String name) {
        super(name);
        
        for (int component = 0; component < nCrystalX*nCrystalY; component++) {
            if(doesThisCrystalExist(component)) {
                int iy = component / nCrystalX;
                int ix = component - iy * nCrystalX;               
                double xcenter = crystal_size * (nCrystalX - ix - 0.5);
                double ycenter = crystal_size * (nCrystalY - iy - 0.5 + 1.);
                if (ix > 10) {
                    ix = ix - 10;
                } else {
                    ix = ix - 11;
                }
                if (iy > 10) {
                    iy = iy - 10;
                } else {
                    iy = iy - 11;
                }
                points.add(0, 0, component, new ShapePoint(ix,iy));
                DetectorShape2D shape = new DetectorShape2D(DetectorType.FTHODO, 0, 0, component);
                shape.createBarXY(crystal_size, crystal_size);
                shape.getShapePath().translateXYZ(xcenter+x0, ycenter+y0, 0.0);
                shape.setColor(0, 145, 0);
                this.addShape(shape);           
            }
        }
    }

    
    public void setCenter(double xCenter, double yCenter) {
        this.x0=xCenter;
        this.y0=yCenter;
    }
    
    public void addPaddles() {
        for(int ipaddle=0; ipaddle<4; ipaddle++) {
            int component = 501+ipaddle;
            points.add(0, 0, component, new ShapePoint(0,0));
            DetectorShape2D paddle = new DetectorShape2D(DetectorType.FTHODO, 0, 0, 501+ipaddle);
            paddle.createBarXY(crystal_size*nCrystalX, crystal_size/2.);
            paddle.getShapePath().translateXYZ(crystal_size*nCrystalX/2.,crystal_size*(nCrystalX+2)*(ipaddle % 2)+crystal_size/4.*(((int) ipaddle/2)*2-1),0.0);
            paddle.setColor(0, 145, 0);
            this.addShape(paddle);
        }
    }
    
    public void addSync() {
        DetectorShape2D paddle = new DetectorShape2D(DetectorType.FTHODO, 0, 0, 500);
        points.add(0, 0, 500, new ShapePoint(0,0));
        paddle.createBarXY(crystal_size, crystal_size);
        paddle.getShapePath().translateXYZ(crystal_size*0.5,crystal_size*(22-0.5+1),0.0);
        paddle.setColor(0, 145, 0);
        this.addShape(paddle);
    }
    
    
    public void setThresholds(double threshold) {
        for (int component : this.getDetectorComponents()) {
            int ix = this.getIdX(component);
            int iy = this.getIdY(component);
            thresholds.add(0, 0, component, threshold);// for runs Run>692
            // For runs 487(?)>Run<=691 from here 1)
	    //            if(ix!=-9) {
	    //                thresholds.add(0, 0, component, threshold);
	    //            }
	    //            else if(ix==-9) {
	    ////                    if     (iy==-6 || iy==-5 || iy==-4 || iy==-3 || iy==-2 || iy==-1) thresholdValue.add(0, 0, component, threshold/3.);     
	    ////                    else if(iy== 6 ||           iy==4  || iy==3  || iy==2  || iy==1 ) thresholdValue.add(0, 0, component, threshold/2.);
	    ////                    else                                                              thresholdValue.add(0, 0, component, threshold);
	    //                if     (iy==-7 || iy==5 || iy==6 || iy==7) thresholds.add(0, 0, component, threshold);
	    //                else                                       thresholds.add(0, 0, component, threshold/3.);
	    //            }// to here 1)
        }
    }
    
    public DetectorCollection<Double> getThresholds() {
        return this.thresholds;
    }
    
    public DetectorCollection getShapePoints() {
        return this.points;
    }
    
    public int getIdX(int component) {
        return this.points.get(0, 0, component).x();
    }
    
    public int getIdY(int component) {
        return this.points.get(0, 0, component).y();
    }

    public int getIX(int component) {
        int i = this.points.get(0, 0, component).x();
        if (i > 0) {
            i = i + 10;
        } else {
            i = i + 11;
        }
        return i;
    }
    
    public int getIY(int component) {
        int i = this.points.get(0, 0, component).y();
        if (i > 0) {
            i = i + 10;
        } else {
            i = i + 11;
        }
        return i;    
    }

    public int getComponent(int ix, int iy) {
        return iy*nCrystalX+ix; 
    }
        
    public String getComponentName(int component) {
        String title = "(" + this.getIdX(component) + "," + this.getIdY(component) + ")";
        return title;
    }
    
    public Set<Integer> getDetectorComponents() {
        return this.points.getComponents(0, 0);
    }
 
    public boolean hasComponent(int component) {
        return this.points.hasEntry(0, 0, component);
    }
    
    public boolean hasComponent(int ix, int iy) {
        int component = iy*nCrystalX+ix; 
        return this.points.hasEntry(0, 0, component);
    }

    public int getNComponents() {
        return this.points.getComponents(0, 0).size();
    }
    
    public int getComponentMaxCount() {
        int keyMax=0;
        for(int key : this.points.getComponents(0, 0)) keyMax=key;
        return keyMax;
    }

    public int[] getIDArray() {
        int[] crystalID = new int[this.getNComponents()];
        int ipointer=0;
        for(int component : this.points.getComponents(0, 0)) {
            crystalID[ipointer]=component;
            ipointer++;
        }
        return crystalID;
    }
    
    private boolean doesThisCrystalExist(int id) {

        boolean crystalExist=false;
        int iy = id / nCrystalX;
        int ix = id - iy * nCrystalX;

        double xcrystal = crystal_size * (nCrystalX - ix - 0.5);
        double ycrystal = crystal_size * (nCrystalY - iy - 0.5);
        double rcrystal = Math.sqrt(Math.pow(xcrystal - crystal_size * 11, 2.0) + Math.pow(ycrystal - crystal_size * 11, 2.0));
        if (rcrystal > crystal_size * 4 && rcrystal < crystal_size * 11) {
            crystalExist=true;
        }
        return crystalExist;
    }
    
    public class ShapePoint {
        private int x; // the x coordinate
        private int y; // the y coordinate
    
        public ShapePoint(int x, int y) {
            set(x, y);
        }    

        private void set(int x, int y) {
            setX(x);
            setY(y);
        }

        private void setX(int x) {
            this.x = x;
        }

        private void setY(int y) {
            this.y = y;
        }
        
        public int x() {
            return x;
        }
        
        public int y() {
            return y;
        }
    }
}
