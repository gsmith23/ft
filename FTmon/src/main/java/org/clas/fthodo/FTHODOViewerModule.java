package org.clas.fthodo;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.basic.IDetectorModule;
import org.jlab.clas12.basic.IDetectorProcessor;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeTabView;
import org.jlab.clas12.calib.DetectorShapeView2D;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clas12.detector.DetectorChannel;
import org.jlab.clas12.detector.DetectorCounter;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clas12.detector.FADCBasicFitter;
import org.jlab.clas12.detector.IFADCFitter;
import org.jlab.clasrec.main.DetectorEventProcessorPane;
import org.jlab.data.io.DataEvent;
import org.jlab.evio.clas12.EvioDataEvent;
import org.root.attr.ColorPalette;
import org.root.attr.TStyle;
import org.root.func.F1D;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.root.pad.EmbeddedCanvas;


public class FTHODOViewerModule implements IDetectorListener,ActionListener{

    JPanel detectorPanel;
    EventDecoder decoder;
    int nProcessed = 0;
    
    public EventDecoder getDecoder() {
        return decoder;
    }



    public void setDecoder(EventDecoder decoder) {
        this.decoder = decoder;
    }



    public JPanel getDetectorPanel() {
        return detectorPanel;
    }



    public void setDetectorPanel(JPanel detectorPanel) {
        this.detectorPanel = detectorPanel;
    }

    DetectorShapeTabView view = new DetectorShapeTabView();
    EmbeddedCanvas canvas = new EmbeddedCanvas();
   
    static int crystal_size=15;
    
    
    public FTHODOViewerModule(){
        this.detectorPanel=null;
    }
  
    
    
    public void initDetector(){
        
        DetectorShapeView2D viewFTCAL = new DetectorShapeView2D("FTHODO");
        for (int component = 0; component < 22*22; component++) {
           // if(doesThisCrystalExist(component)) {
                int iy = component / 22;
                int ix = component - iy * 22;
                double xcenter = crystal_size * (22 - ix - 0.5);
                double ycenter = crystal_size * (22 - iy - 0.5);
                DetectorShape2D shape = new DetectorShape2D(DetectorType.FTCAL, 0, 0, component);
                shape.createBarXY(crystal_size, crystal_size);
                shape.getShapePath().translateXYZ(xcenter, ycenter, 0.0);
                shape.setColor(0, 145, 0);
                viewFTCAL.addShape(shape);               
            //}
        }
        this.view.addDetectorLayer(viewFTCAL);
        view.addDetectorListener(this);
    }
    
    
    
    
    
    public void actionPerformed(ActionEvent e) {
          
    }



    public void detectorSelected(DetectorDescriptor desc) {
        System.out.println("detectorSelected in FTHOO!");
        
    }



    public void update(DetectorShape2D shape) {
        System.out.println("update in FTHOO!");
    }



    public void initHistograms() {
       
        
    }



    public void resetHistograms() {
       
        
    }



    public void processDecodedEvent() {
        System.out.println("Decoding in FTHOO!");
        nProcessed++;
    }



    public void initPanel() {
        // TODO Auto-generated method stub
        
    }



    public void initDecoder() {
        // TODO Auto-generated method stub
        
    }

}
