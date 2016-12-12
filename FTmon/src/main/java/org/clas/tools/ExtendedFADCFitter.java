/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import org.jlab.clas12.detector.DetectorChannel;
import org.jlab.clas12.detector.IFADCFitter;

/**
 *
 * @author devita
 */
public class ExtendedFADCFitter implements IFADCFitter {

    private int    ped_i1;
    private int    ped_i2;
    private int    pul_i1;
    private int    pul_i2;
    private double threshold;

    double nsPerSample=4;
    double LSB = 0.4884;

    private double rms = 0;
    private double pedestal = 0;
    private double wave_max=0;
    private int    thresholdCrossing=0;
    private double time_3 = 0;
    private double time_7 = 0;
    
    public void setPedestalRange(int min, int max) {
        this.ped_i1=min;
        this.ped_i2=max;
    }

    public void setPulseRange(int min, int max) {
        this.pul_i1=min;
        this.pul_i2=max;
    }
    
    public void setThreshold(double t) {
        this.threshold=t;
    } 
    
    public double getPedestal() {
        return pedestal;
    }

    public double getRMS() {
        return rms;
    }

    public double getWave_Max() {
        return wave_max;
    }

    public int getThresholdCrossing() {
        return thresholdCrossing;
    }

    public double getTime(int mode) {
        double time=0;
        if(mode==3)       time = time_3;
        else if(mode==7)  time = time_7;
        else System.out.println(" Unknown mode for time calculation, check...");
        return time;
    }

    public void fit(DetectorChannel dc) {
        //System.out.println("Erica caspiterina0");
        short[] pulse = dc.getPulse();
        double ped    = 0.0;
        double noise  = 0;
        double wmax   = 0;
        double pmax   = 0;
        int    tcross = 0; 
        // calculate pedestal means and noise
        for (int bin = ped_i1; bin < ped_i2; bin++) {
            ped += pulse[bin];
            noise += pulse[bin] * pulse[bin];
        }
        //System.out.println("Erica caspiterina1");
        pedestal = ped / (ped_i2 - ped_i1);
        rms = LSB * Math.sqrt(noise / (ped_i2 - ped_i1) - pedestal * pedestal);
        // determine waveform max
        for (int bin=0; bin<pulse.length; bin++) {
            if(pulse[bin]>wmax) wmax=pulse[bin];
        }
        //System.out.println("Erica caspiterina2");
        wave_max=wmax;
        // find threshold crossing in pulse region: this determines mode-3 time (4 ns resolution)
        for (int bin=pul_i1; bin<pul_i2; bin++) {
            if(pulse[bin]>pedestal+threshold) {
                tcross=bin;
                break;
            }
        }
        //System.out.println("Erica caspiterina3");
        thresholdCrossing=tcross;
        time_3=tcross*nsPerSample;
        // find pulse max
        for (int bin=thresholdCrossing; bin<pulse.length; bin++) { 
            if (pulse[bin+1]<pulse[bin]){ 
                pmax=pulse[bin];
                break; 
            }
        }
        //System.out.println("Erica caspiterina4");
        
        // calculating high resolution time    
        double halfMax = (pmax+pedestal)/2;
        time_7 = time_3;
        int t0 = -1;
        if(tcross>0) { 
            for (int bin=tcross-1; bin<pul_i2; bin++) {
                if (pulse[bin]<=halfMax && pulse[bin+1]>halfMax) {
                    t0 = bin;
                    break;
                }
            }
            if(t0>-1) { 
                int t1 = t0 + 1;
                int a0 = pulse[t0];
                int a1 = pulse[t1];
		//final double slope = (a1 - a0); // units = ADC/sample
		//final double yint = a1 - slope * t1;  // units = ADC 
                time_7 = ((halfMax - a0)/(a1-a0) + t0)* nsPerSample;
            }
        }
        //System.out.println("Erica caspiterina55555555555");
    }
    
    //    public void fitException(DetectorChannel dc, int ped_i, int ped_f, int pul_i, int pul_f) {
    //        System.out.println("Erica fitexception:");
    //        short[] pulse = dc.getPulse();
    //        double ped    = 0.0;
    //        double noise  = 0;
    //        double wmax   = 0;
    //        double pmax   = 0;
    //        int    tcross = 0; 
    //        // calculate pedestal means and noise
    //        for (int bin = ped_i; bin < ped_i; bin++) {
    //            ped += pulse[bin];
    //            noise += pulse[bin] * pulse[bin];
    //        }
    //        pedestal = ped / (ped_i - ped_i);
    //        rms = LSB * Math.sqrt(noise / (ped_i - ped_i) - pedestal * pedestal);
    //        // determine waveform max
    //        for (int bin=0; bin<pulse.length; bin++) {
    //            if(pulse[bin]>wmax) wmax=pulse[bin];
    //        }
    //        wave_max=wmax;
    //        // find threshold crossing in pulse region: this determines mode-3 time (4 ns resolution)
    //        for (int bin=pul_i; bin<pul_i; bin++) {
    //            if(pulse[bin]>pedestal+threshold) {
    //                tcross=bin;
    //                break;
    //            }
    //        }
    //        thresholdCrossing=tcross;
    //        time_3=tcross*nsPerSample;
    //        // find pulse max
    //        for (int bin=thresholdCrossing; bin<pulse.length; bin++) { 
    //            if (pulse[bin+1]<pulse[bin]){ 
    //                pmax=pulse[bin];
    //                break; 
    //            }
    //        }
    //        // calculating high resolution time    
    //        double halfMax = (pmax+pedestal)/2;
    //        time_7 = time_3;
    //        int t0 = -1;
    //        if(tcross>0) { 
    //            for (int bin=tcross-1; bin<pul_i; bin++) {
    //                if (pulse[bin]<=halfMax && pulse[bin+1]>halfMax) {
    //                    t0 = bin;
    //                    break;
    //                }
    //            }
    //            if(t0>-1) { 
    //                int t1 = t0 + 1;
    //                int a0 = pulse[t0];
    //                int a1 = pulse[t1];
    //                   //final double slope = (a1 - a0); // units = ADC/sample
    //                   //final double yint = a1 - slope * t1;  // units = ADC 
    //                time_7 = ((halfMax - a0)/(a1-a0) + t0)* nsPerSample;
    //            }
    //        }
    //   }
    
    public void fit(DetectorChannel dc, double t) {
        
        this.setThreshold(t);
        this.fit(dc);
    }
    
}