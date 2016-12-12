/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.data;

import org.jlab.clas12.basic.IDetectorProcessor;
import org.jlab.clasrec.main.DetectorEventProcessorDialog;
import org.jlab.clasrec.main.DetectorEventProcessorThread;
import org.jlab.data.io.DataEvent;

/**
 *
 * @author gavalian
 */
public class SimpleEventProcessor implements IDetectorProcessor {
    int  nEvents = 0;

    public void processEvent(DataEvent ev) {
        // Event structure is passed to this routine.
        // EvioDataEvent event = (EvioDataEvent) ev;        
        nEvents++;
    }
    
    public void show(){
        System.out.println("----> # events processed = " + nEvents);
    }
    
    public static void main(String[] args){
                
        SimpleEventProcessor  processor = new SimpleEventProcessor();
        String inputFile = "/Users/gavalian/Work/Software/Release-8.0/COATJAVA/etaPXSection_0_recon.evio";
        //String inputFile = "clondaq5:/tmp/et_system_clasprod2";

        (new DetectorEventProcessorThread(inputFile ,processor)).start();
        processor.show();
        
        // This opens a dialog with a progress bar and runs the file through event processor.
        //DetectorEventProcessorDialog dialog = new DetectorEventProcessorDialog(inputFile,processor);
        
        // This line opens a dialog with File choser and lets user chose the file to process
        // then runs all events through the IDetectorProcessor class.
        //DetectorEventProcessorDialog dialog = new DetectorEventProcessorDialog(processor);
        
    }
}
