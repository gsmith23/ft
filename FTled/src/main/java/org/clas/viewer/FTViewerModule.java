/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;


import org.clas.ftcal.FTCALViewerModule;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.basic.IDetectorModule;
import org.jlab.clas12.basic.IDetectorProcessor;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeTabView;
import org.jlab.clas12.calib.DetectorShapeView2D;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clasrec.main.DetectorEventProcessorPane;
import org.jlab.data.io.DataEvent;
import org.jlab.evio.clas12.EvioDataEvent;
import org.root.attr.ColorPalette;

/**
 *
 * @author gavalian
 */
public class FTViewerModule implements IDetectorProcessor, IDetectorModule, ActionListener {

    
    FTCALViewerModule moduleFTCAL=new FTCALViewerModule();
    
        
    DetectorEventProcessorPane evPane = new DetectorEventProcessorPane();
   
   
    EventDecoder decoder = new EventDecoder();
    int nProcessed = 0;
 
    
    
    // ColorPalette class defines colors 
    ColorPalette palette = new ColorPalette();
 
    JPanel detectorPanel = null;
    JPanel FTCALPanel = null;
    
  

    public FTViewerModule() {
        
        moduleFTCAL.setDecoder(decoder);
        
        this.initRawDataDecoder();
        
        this.initDetector();
        this.initHistograms();
        
        this.evPane.addProcessor(this);
        
        /*Graphics starts here*/
        this.detectorPanel = new JPanel();
        this.detectorPanel.setLayout(new BorderLayout());
        
        this.FTCALPanel = new JPanel(new BorderLayout());
        moduleFTCAL.setDetectorPanel(this.FTCALPanel);        
        moduleFTCAL.initPanel();
        
        // filling main panel with tabs for different FT subdetectors and event handling panel
        this.detectorPanel.add(this.FTCALPanel, BorderLayout.CENTER);
        this.detectorPanel.add(this.evPane, BorderLayout.PAGE_END);
        
        
    }

    private void initDetector() {
        
        moduleFTCAL.initDetector();
    }

    private void initRawDataDecoder() {
        moduleFTCAL.initDecoder();
    }

    private void initHistograms() {
        moduleFTCAL.initHistograms();
    }

    private void resetHistograms() {
       
        moduleFTCAL.resetHistograms();
    }

    
    
    
    
    public void processEvent(DataEvent de) {
        EvioDataEvent event = (EvioDataEvent) de;

        
        decoder.decode(event);
        nProcessed++;
        
        moduleFTCAL.processDecodedEvent();        

    }

        
    public String getName() {
        return "FTViewerModule";
    }

    public String getAuthor() {
        return "De Vita";
    }

    public DetectorType getType() {
        return DetectorType.FTCAL;
    }

    public String getDescription() {
        return "FT Display";
    }

    public JPanel getDetectorPanel() {
        return this.detectorPanel;
    }

    public static void main(String[] args) {
        FTViewerModule module = new FTViewerModule();
        JFrame frame = new JFrame();
        frame.add(module.getDetectorPanel());
        frame.pack();
        frame.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println("FTViewer ACTION = " + e.getActionCommand());
       
    }

    public void detectorSelected(DetectorDescriptor dd) {
        //To change body of generated methods, choose Tools | Templates.
    }

  
    
  

}
