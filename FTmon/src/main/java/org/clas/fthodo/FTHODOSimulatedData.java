/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.fthodo;

import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.evio.clas12.EvioDataBank;
import org.jlab.evio.clas12.EvioDataEvent;

/**
 *
 * @author fanchini
 */
public class FTHODOSimulatedData extends FTApplication{
    EvioDataBank eviobank;
    
    private DetectorCollection<Double> sim_adc = new DetectorCollection<Double>();
    private DetectorCollection<Double> sim_tdc = new DetectorCollection<Double>();
    
    boolean debugging_mode = false;
    
    public FTHODOSimulatedData(FTDetector d) {
	super(d);
    }
      
    public void eventBankDecoder(EvioDataEvent event, String bank){
	eviobank = (EvioDataBank) event.getBank(bank);
	if(bank=="FTHODO::dgtz")fthodoBank(bank);
	else {
	    System.out.println("Different bank not included");
	}
	if(debugging_mode){
	    eviobank.show(); // printout the content of the bank
	    System.out.println("After .show");
	}
    }
    
    private void fthodoBank(String bank){
        clearSimcollection();
        
        double adc=0.0, tdc=0.0;
        int    s=0, l=0, c=0;
        for (int row = 0; row < this.eviobank.rows(); row++) {
	    
            s = this.eviobank.getInt("sector"   , row);
            l = this.eviobank.getInt("layer"    , row);
            c = this.eviobank.getInt("component", row);

            adc = (double)this.eviobank.getInt("ADC", row);
            tdc = (double)this.eviobank.getInt("TDC", row);
	    
	    if(debugging_mode){
		System.out.println(" ---------- ");
		System.out.println(" fthodoBank");
		System.out.println(" adc = " + adc );
		System.out.println(" sector = " + s );
		System.out.println(" layer  = " + l );
		System.out.println(" comp   = " + c );
		System.out.println(" ---------- ");
	    }
	    
            this.sim_adc.add(s, l, c, adc);
            this.sim_tdc.add(s, l, c, tdc);
            
	}
        //System.out.println("Erica Sim1: sim_ad.size:"+this.sim_adc.getComponents(0, 0).size());
    }
   
 
               
    public DetectorCollection<Double> getSimAdc() {
	//System.out.println("Erica Sim2: sim_ad.size:"+this.sim_adc.getComponents(0, 0).size());
	//System.out.println("Erica Sim2: sim_ad.size:"+this.sim_adc.getComponents(0, 0).size());
	
	return this.sim_adc;
    }
        
    public DetectorCollection<Double> getSimTdc() {
	//System.out.println("Erica Sim2: sim_ad.size:"+this.sim_tdc.getComponents(0, 0).size());
        return this.sim_tdc;
    }
       
    
    public void clearSimcollection(){
        this.sim_adc.clear();
        this.sim_tdc.clear();
    }
    
}