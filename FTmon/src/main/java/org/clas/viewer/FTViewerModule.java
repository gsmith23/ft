/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.clas.fthodo.FTHODOViewerModule;
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
public class FTViewerModule implements IDetectorProcessor, IDetectorModule, IDetectorListener, ActionListener {

    
    FTHODOViewerModule moduleFTHODO=new FTHODOViewerModule();
    FTCALViewerModule moduleFTCAL=new FTCALViewerModule();
    
        
    DetectorEventProcessorPane evPane = new DetectorEventProcessorPane();
   
    
    EventDecoder decoder = new EventDecoder();
    int nProcessed = 0;
    
    
    
    // ColorPalette class defines colors 
    ColorPalette palette = new ColorPalette();
 
    JPanel detectorPanel = null;
    JPanel FTCALPanel = null;
    JPanel FTHODOPanel = null;
    JSplitPane FTview = new JSplitPane();
    DetectorShapeTabView FTview1 = new DetectorShapeTabView();
    DetectorShapeTabView FTview2 = new DetectorShapeTabView();
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
        
        this.FTHODOPanel = new JPanel(new BorderLayout());
        this.FTCALPanel = new JPanel(new BorderLayout());

        this.tabbedPane = new JTabbedPane();
        tabbedPane.add("FT-HODO",this.FTHODOPanel);
	tabbedPane.add("FT-CAL",this.FTCALPanel);
        
        tabbedPane.add("FT",this.FTview);
  
        this.FTview.setLeftComponent(this.FTview1);
        this.FTview.setRightComponent(this.FTview2);
        FTview.setDividerLocation(750);
	
        // filling main panel with tabs for different FT subdetectors and event handling panel
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
        this.FTview1.addDetectorLayer(moduleFTCAL.drawDetector(-10., 0));
        this.FTview2.addDetectorLayer(moduleFTHODO.drawDetector(+10.,0.));
        this.FTview1.addDetectorListener(this);
        this.FTview2.addDetectorListener(this);
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
    
        this.FTview.repaint();
    }

//    public DetectorShapeView2D drawDetector(double x0, double y0) {
//        double p15_WW=15;
//        double p30_WW=30.;
//        int    p15_N = 11;
//        double[] p15_X = {7.5,  22.5,  37.5,  52.5,  52.5,  67.5,  67.5,  67.5,  67.5,  97.5, 127.5};
//        double[] p15_Y = {67.5,  67.5,  67.5,  52.5,  67.5,   7.5,  22.5,  37.5,  52.5, 127.5,  97.5};
//
//        int    p30_N = 18;
//        double[] p30_X = {15.,  15.,  15.,  45.,  45.,  45.,  75.,  75.,  75.,  90.,  90., 105., 105., 120., 120., 135., 150., 150.};
//        double[] p30_Y = {90., 120., 150.,  90., 120., 150.,  75., 105., 135.,  15.,  45.,  75., 105.,  15.,  45.,  75.,  15.,  45.};
//        double[] q_X = {1., -1., -1.,  1.};
//        double[] q_Y = {1.,  1., -1., -1.};
//
//        DetectorShapeView2D viewFTHODO = new DetectorShapeView2D("FTHODO");
//        
//        for(int q = 0; q < 4; q++ ) {
//            for(int i = 0; i < p15_N; i++ ) {
//                double p_X = p15_X[i]*q_X[q];
//                double p_Y = p15_Y[i]*q_Y[q];
//                DetectorShape2D shape = new DetectorShape2D(DetectorType.FTHODO, 0, 0, q*p15_N+i);
//                shape.createBarXY(p15_WW, p15_WW);
//                shape.getShapePath().translateXYZ(p_X+x0, p_Y+y0, 0.0);
//                shape.setColor(0, 145, 0);
//                viewFTHODO.addShape(shape);
//            }
//        }
//        for(int q = 0; q < 4; q++ ) {
//            for(int i = 0; i < p30_N; i++ ) {
//                double p_X = p30_X[i]*q_X[q];
//                double p_Y = p30_Y[i]*q_Y[q];
//                DetectorShape2D shape = new DetectorShape2D(DetectorType.FTHODO, 0, 0, 4*p15_N+q*p30_N+i);
//                shape.createBarXY(p30_WW, p30_WW);
//                shape.getShapePath().translateXYZ(p_X+x0, p_Y+y0, 0.0);
//                shape.setColor(0, 145, 0);
//                viewFTHODO.addShape(shape);
//            }
//        }
//                        
//        return viewFTHODO;
//    }

  
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
       
    }

    public void detectorSelected(DetectorDescriptor dd) {
        //To change body of generated methods, choose Tools | Templates.
    }

  
    
  

}
