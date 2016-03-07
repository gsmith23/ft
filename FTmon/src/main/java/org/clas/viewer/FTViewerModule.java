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
import javax.swing.JTabbedPane;

import org.clas.fthodo.FTHODOViewerModule;
import org.clas.ftcal.FTCALViewerModule;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.basic.IDetectorModule;
import org.jlab.clas12.basic.IDetectorProcessor;
import org.jlab.clas12.calib.DetectorShapeTabView;

import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clasrec.main.DetectorEventProcessorPane;
//import org.jlab.clasrec.main.DetectorEventProcessorPane;

import org.jlab.data.io.DataEvent;
import org.jlab.evio.clas12.EvioDataEvent;
import org.root.attr.ColorPalette;
//


/**
 *
 * @author gavalian
 */
public class FTViewerModule implements IDetectorProcessor, IDetectorModule, ActionListener {
    
    FTHODOViewerModule moduleFTHODO=new FTHODOViewerModule();
    FTCALViewerModule moduleFTCAL=new FTCALViewerModule();
    
    DetectorEventProcessorPane evPane = new DetectorEventProcessorPane();
   
    //this.threadDelay = 0;
    
    EventDecoder decoder = new EventDecoder();
    int nProcessed = 0;
    
    // ColorPalette class defines colors 
    ColorPalette palette = new ColorPalette();
 
    JPanel detectorPanel = null;
    JPanel FTCALPanel = null;
    JPanel FTHODOPanel = null;
    DetectorShapeTabView FTview = new DetectorShapeTabView();
    JTabbedPane tabbedPane = null;

    public FTViewerModule() {
        
        moduleFTCAL.setDecoder(decoder);
        moduleFTHODO.setDecoder(decoder);
        
        this.initRawDataDecoder();
        
        this.initDetector();
        this.initHistograms();
        
        this.evPane.addProcessor(this);
        
        /*Graphics starts here*/
        this.detectorPanel = new JPanel();
        this.detectorPanel.setLayout(new BorderLayout());
        
        this.FTCALPanel = new JPanel(new BorderLayout());
        this.FTHODOPanel = new JPanel(new BorderLayout());

        this.tabbedPane = new JTabbedPane();
        tabbedPane.add("FT-HODO",this.FTHODOPanel);
        tabbedPane.add("FT-CAL",this.FTCALPanel);
        tabbedPane.add("FT",this.FTview);
  
        // filling main panel with tabs for different 
	// FT subdetectors and event handling panel
        this.detectorPanel.add(tabbedPane, BorderLayout.CENTER);
        this.detectorPanel.add(this.evPane, BorderLayout.PAGE_END);
        
        moduleFTCAL.setDetectorPanel(this.FTCALPanel);
        moduleFTHODO.setDetectorPanel(this.FTHODOPanel);
        
        moduleFTCAL.initPanel();
        moduleFTHODO.initPanel();
    }

    private void initDetector() {
        
        moduleFTCAL.initDetector();
        moduleFTHODO.initDetector();
        
        // TODO define general view for FT
        
    }

    private void initRawDataDecoder() {
        moduleFTCAL.initDecoder();
        moduleFTHODO.initDecoder();
    }

    private void initHistograms() {
      
        moduleFTCAL.initHistograms();
        moduleFTHODO.initHistograms();
    }

    private void resetHistograms() {
       
        moduleFTCAL.resetHistograms();
        moduleFTHODO.resetHistograms();
    }

    
    public void processEvent(DataEvent de) {
        EvioDataEvent event = (EvioDataEvent) de;
        
        decoder.decode(event);
        nProcessed++;
        
        moduleFTCAL.processDecodedEvent();        
        moduleFTHODO.processDecodedEvent();
    
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

  
    
  

}
