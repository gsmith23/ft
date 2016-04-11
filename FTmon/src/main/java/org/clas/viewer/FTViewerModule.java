/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JButton;

import org.clas.fthodo.FTHODOViewerModule;
import org.clas.ftcal.FTCALViewerModule;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.basic.IDetectorModule;
import org.jlab.clas12.basic.IDetectorProcessor;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeTabView;
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
public class FTViewerModule implements IDetectorProcessor,
				       IDetectorModule,
				       IDetectorListener,
				       ActionListener {

    
    FTHODOViewerModule moduleFTHODO = new FTHODOViewerModule();
    FTCALViewerModule  moduleFTCAL  = new FTCALViewerModule();
    
    DetectorEventProcessorPane evPane = new DetectorEventProcessorPane();
    
    EventDecoder decoder = new EventDecoder();
    int          nProcessed = 0;
    
    // frequency by which panels are repainted
    // see ActionEvent
    private int repaintFrequency = 1;
    private int buttonSelect     = 0;
	    
    ColorPalette palette = new ColorPalette();
 
    JPanel detectorPanel = null;
    JPanel FTCALPanel = null;
    JPanel FTHODOPanel = null;
    
    JSplitPane FTviewMaster    = new JSplitPane();
    JSplitPane FTviewDetectors = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    
    JPanel     FTviewEventsContainer   = new JPanel(new BorderLayout());
    JSplitPane FTviewEvents   = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    
    DetectorShapeTabView FTviewCAL  = new DetectorShapeTabView();
    DetectorShapeTabView FTviewHODO = new DetectorShapeTabView();
    
    JTabbedPane tabbedPane = null;

    
    private int componentSelect = 0;
    private int secSelect       = 0;
    private int layerSelect     = 0;
    
    public FTViewerModule() {
	
	moduleFTCAL.setDecoder(decoder); 
        moduleFTHODO.setDecoder(decoder);
        
        this.initRawDataDecoder();
        
        this.initDetector();
        this.initHistograms();
        
        this.evPane.addProcessor(this);
       
	
        /*Graphics starts here*/
        // whole panel
	this.detectorPanel = new JPanel();
        this.detectorPanel.setLayout(new BorderLayout());

	// create tabbed objects for CAL and HODO modules
	this.FTHODOPanel = new JPanel(new BorderLayout());
        this.FTCALPanel  = new JPanel(new BorderLayout());
	
        this.tabbedPane = new JTabbedPane();
        tabbedPane.add("FT",this.FTviewMaster);
	tabbedPane.add("FT-CAL",this.FTCALPanel);
	tabbedPane.add("FT-HODO",this.FTHODOPanel);
	
	// LHS of FTviewMaster will be the detectors view
	this.FTviewDetectors.setTopComponent(this.FTviewHODO);
	this.FTviewDetectors.setBottomComponent(this.FTviewCAL);
	this.FTviewDetectors.setDividerLocation(350);
        
        // filling main panel with tabs for different FT subdetectors 
	// and event handling panel
        this.detectorPanel.add(tabbedPane, BorderLayout.CENTER);
        this.detectorPanel.add(this.evPane, BorderLayout.PAGE_END);
        
	moduleFTCAL.setDetectorPanel(this.FTCALPanel);
        moduleFTHODO.setDetectorPanel(this.FTHODOPanel);
	
	moduleFTCAL.initPanel();
        moduleFTHODO.initPanel();

	FTviewEventsContainer.add(FTviewEvents,BorderLayout.CENTER);
	
	JPanel      buttonPane = new JPanel();
	buttonPane.setLayout(new FlowLayout());
	
	// NB these buttons are only visible in the combined viewer at 
	// present but can also take affect in the FT-CAL and FT-HODO viewers
	JButton     allEventsButton       = new JButton("All");
	JButton     onceInTenButton       = new JButton("1/10");
	JButton     onceInAHundredButton  = new JButton("1/100");
	JButton     onceInAThousandButton = new JButton("1/1000");
	//JButton     onceInABlueMoon      = new JButton("Blue Moon");
	
	allEventsButton.addActionListener(this);
	onceInTenButton.addActionListener(this);
	onceInAHundredButton.addActionListener(this);
	onceInAThousandButton.addActionListener(this);
	//onceInABlueMoon.addActionListener(this);

	buttonPane.add(allEventsButton);
	buttonPane.add(onceInTenButton);
	buttonPane.add(onceInAHundredButton);
	buttonPane.add(onceInAThousandButton);
	//buttonPane.add(onceInABlueMoon);
	FTviewEventsContainer.add(buttonPane, BorderLayout.PAGE_END);
	
	// RHS of FTviewMaster will be the histograms view	
	this.FTviewEvents.setTopComponent(moduleFTHODO.canvasHODOEvent);
	this.FTviewEvents.setBottomComponent(moduleFTCAL.canvasCALEvent);
	this.FTviewEvents.setDividerLocation(250);
	
	this.FTviewMaster.setLeftComponent(this.FTviewDetectors);
	this.FTviewMaster.setRightComponent(this.FTviewEventsContainer);
	this.FTviewMaster.setDividerLocation(250);
    	
    } // end of : public FTViewerModule() {

    private void initDetector() {
        
	moduleFTCAL.initDetector();
        moduleFTHODO.initDetector();
	this.FTviewCAL.addDetectorLayer(moduleFTCAL.drawDetector(-10., 0));
        this.FTviewHODO.addDetectorLayer(moduleFTHODO.drawDetector(+10.,0.));
        this.FTviewCAL.addDetectorListener(this);
        this.FTviewHODO.addDetectorListener(this);
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
        moduleFTHODO.processDecodedEvent(this.repaintFrequency,0);
	moduleFTHODO.processDecodedEvent(this.repaintFrequency,1);

	
        if(nProcessed%repaintFrequency==0)
	    this.FTviewMaster.repaint();
	
    }

    public void update(DetectorShape2D shape) {
	
        int sector = shape.getDescriptor().getSector();
        int layer = shape.getDescriptor().getLayer();
        int paddle = shape.getDescriptor().getComponent();
        if(shape.getDescriptor().getType() == DetectorType.FTCAL) {
	    Color col = moduleFTCAL.getComponentStatus(paddle);
            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
        }
        else {
            Color col = moduleFTHODO.getComponentStatus(sector,layer,paddle);
            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
        }
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
	
	if (e.getActionCommand().compareTo("All") == 0) {
	    buttonSelect = 0;
            repaintFrequency = 1;
	}
        else if (e.getActionCommand().compareTo("1/10") == 0) {
            buttonSelect = 1;
            repaintFrequency = 10;
	    System.out.println("Updating every 10 events ");
        }
        else if (e.getActionCommand().compareTo("1/100") == 0) {
            buttonSelect = 2;
            repaintFrequency = 100;
	    System.out.println("Updating every 100 events ");
        }
        else if (e.getActionCommand().compareTo("1/1000") == 0) {
            buttonSelect = 3;
            repaintFrequency = 1000;
	    System.out.println("Updating every 1000 events ");
        }
        else if (e.getActionCommand().compareTo("1/10000") == 0) {
            buttonSelect = 4;
            repaintFrequency = 10000;
	    System.out.println("Updating every Blue Moon ");
        }
        moduleFTCAL.setRepaintFrequency(repaintFrequency);

    }
    
    public void detectorSelected(DetectorDescriptor dd) {
	//To change body of generated methods, choose Tools | Templates.
	componentSelect = dd.getComponent();
	secSelect = dd.getSector();
	layerSelect = dd.getLayer();
	
	moduleFTCAL.detectorSelected(dd);
	moduleFTHODO.detectorSelected(dd);
	
    }

}
	