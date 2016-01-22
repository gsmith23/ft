/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.detector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas12.basic.IDetectorProcessor;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeTabView;
import org.jlab.clas12.calib.DetectorShapeView2D;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clasrec.main.DetectorEventProcessorDialog;
import org.jlab.data.io.DataEvent;
import org.jlab.evio.clas12.EvioDataBank;
import org.jlab.evio.clas12.EvioDataEvent;
import org.root.attr.ColorPalette;
import org.root.histogram.H1D;
import org.root.func.F1D;
import org.root.pad.EmbeddedCanvas;
//import org.jlab.evio.clas12.*;
//import org.jlab.clas12.raw.*;
//import org.jlab.evio.decode.*;
import org.jlab.clas.detector.*;
import org.jlab.clas12.basic.IDetectorModule;
import org.jlab.clas12.detector.*;
import org.jlab.clasrec.main.DetectorEventProcessorPane;

/**
 *
 * @author gavalian
 */
public class FTCALViewerWithCanvas extends JFrame implements IDetectorListener, IDetectorProcessor, ActionListener {

    DetectorCollection<H1D>   tdcH = new DetectorCollection<H1D>();
    DetectorCollection<H1D>   adcH = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_fADC_dc      = new DetectorCollection<H1D>();
    TreeMap<Integer,H1D> H_fADC      = new TreeMap<Integer,H1D>();
    TreeMap<Integer,H1D> H_COSMIC    = new TreeMap<Integer,H1D>();
    TreeMap<Integer,H1D> H_INT       = new TreeMap<Integer,H1D>();
    TreeMap<Integer,H1D> H_INT_WIDE  = new TreeMap<Integer,H1D>();
    TreeMap<Integer,H1D> H_NOISE     = new TreeMap<Integer,H1D>();
    TreeMap<Integer,F1D> mylandau    = new TreeMap<Integer,F1D>();
//    DetectorCollection<H1D> H_COSMIC    = new DetectorCollection<H1D>();
//    DetectorCollection<H1D> H_INT       = new DetectorCollection<H1D>();
//    DetectorCollection<H1D> H_INT_WIDE  = new DetectorCollection<H1D>();
//    DetectorCollection<H1D> H_NOISE     = new DetectorCollection<H1D>();
//    DetectorCollection<F1D> mylandau    = new DetectorCollection<F1D>();
    H1D H_fADC_N    = null;
    H1D H_COSMIC_N  = null;
    
    int threshold = 15; // 10 fADC value <-> ~ 5mV
    int ped_i1 = 4;
    int ped_i2 = 24;
    int pul_i1 = 40;
    int pul_i2 = 99;
    double LSB=0.4884;
    int[] cry_event = new int[484];
    int[] cry_max   = new int[484];
    int[] cry_n     = new int[22];
    
    int icounter;
    int gcounter;
    int ncrystal;
    DetectorEventProcessorPane evPane = new DetectorEventProcessorPane();
    DetectorShapeTabView  view   = new DetectorShapeTabView();
    EmbeddedCanvas        canvas = new EmbeddedCanvas();
    EventDecoder          decoder = new EventDecoder();
    int                   nProcessed = 0;

    // ColorPalette class defines colors 
    ColorPalette         palette   = new ColorPalette();
    
    


//    AbsDetectorTranslationTable  trTable = new  AbsDetectorTranslationTable("FTCAL",900);
    
    public FTCALViewerWithCanvas(){
        super();
        
        this.initDetector();
        this.initHistograms();
        this.initRawDataDecoder();
        this.setLayout(new BorderLayout());
        JSplitPane  splitPane = new JSplitPane();
        splitPane.setLeftComponent(this.view);
        splitPane.setRightComponent(this.canvas);
        this.add(splitPane,BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        JButton process = new JButton("Process");
        buttons.setLayout(new FlowLayout());
        buttons.add(process);
        process.addActionListener(this);
        //this.add(buttons,BorderLayout.PAGE_END);
        evPane.addProcessor(this);
        this.add(evPane,BorderLayout.PAGE_END);
        this.pack();
        this.setVisible(true);
    }
    
    private void initRawDataDecoder() {
        decoder.addFitter(DetectorType.FTCAL,
        new FADCBasicFitter(ped_i1, // first bin for pedestal
                            ped_i2, // last bin for pedestal
                            pul_i1, // first bin for pulse integral
                            pul_i2  // last bin for pulse integral
                              ));
    }
    
    
    
    private void initHistograms(){
        
        for(int component = 0 ; component < 22*22; component++){
            int iy=component/22;
            int ix=component-iy*22;
            if(ix>10) ix=ix-10;
            else      ix=ix-11;
            if(iy>10) iy=iy-10;
            else      iy=iy-11;
            String title = "Crystal " + component + " (" + ix + "," + iy + ")";
            H_fADC_dc.add(0,0,component, new H1D(DetectorDescriptor.getName("fADC",0,0,component),100,0.0,100.0));
//            H_COSMIC.add(0,0,component, new H1D(DetectorDescriptor.getName("Cosmic",0,0,component),title,100,0.0,100.0));
//            H_INT.add(0,0,component, new H1D(DetectorDescriptor.getName("Charge",0,0,component),title,100,0.0,40.0));
//            H_INT_WIDE.add(0,0,component, new H1D(DetectorDescriptor.getName("Charge",0,0,component),title,100,0.0,200.0));
//            H_NOISE.add(0,0,component, new H1D(DetectorDescriptor.getName("Noise",0,0,component),title,200,0.0,10.0));
//           mylandau.add(0,0,component, new F1D(DetectorDescriptor.getName("Fit",0,0,component),0.0,40.0));
            System.out.println(component + " " + H_fADC_dc.get(0,0,component).getTitle() + " 0 " + H_fADC_dc.get(0,0,0).getTitle());
            H1D fADC     = new H1D("H_fADC_"    +component,title,100,0.0,100.0); fADC.setXTitle("Sample #");       fADC.setYTitle("ADC");        H_fADC.put(component,fADC);
            H1D COSMIC   = new H1D("H_COSMIC_"  +component,title,100,0.0,100.0); COSMIC.setXTitle("Sample #");     COSMIC.setYTitle("ADC");      H_COSMIC.put(component,COSMIC);
            H1D INT      = new H1D("H_INT_"     +component,title,100,0.0, 40.0); INT.setXTitle("Charge(pC)");      INT.setYTitle("Counts");      H_INT.put(component,INT);
            H1D INT_WIDE = new H1D("H_INT_WIDE_"+component,title,100,0.0,200.0); INT_WIDE.setXTitle("Charge(pC)"); INT_WIDE.setYTitle("Counts"); H_INT_WIDE.put(component,INT_WIDE);
            H1D NOISE    = new H1D("H_NOISE_"   +component,title,200,0.0, 10.0); NOISE.setXTitle("Noise RMS (mV)");NOISE.setYTitle("Counts");    H_NOISE.put(component,NOISE);
        }
        for(int component = 0 ; component < 22*22; component++){
//            System.out.println(component + " " + H_fADC.get(0,0,component).getTitle());
        }
        H_fADC_N   = new H1D("fADC" ,484,0,484);
        H_COSMIC_N = new H1D("EVENT",484,0,484);
        
        // initialize variables
        for(int i=0; i<484; i++) {
            cry_event[i]=0;
            cry_max[i]=0;
        }
        
        icounter=0;
        gcounter=0;
        ncrystal=0;
        

        
//        for(int sector = 0; sector < 6; sector++){
//            for(int paddle = 0; paddle < 5; paddle++){
//                // DetectorDescriptor.getName() returns a numbered 
//                // String with sector, layer and paddle numbers.
//                tdcH.add(sector, 2, paddle, 
//                        new H1D(DetectorDescriptor.getName("TDC", sector,2,paddle),
//                        300,0.0,5000.0));
//                adcH.add(sector, 2, paddle, 
//                        new H1D(DetectorDescriptor.getName("ADC", sector,2,paddle),
//                        300,0.0,5000.0));
//            }
//        }
    }
    /**
     * Creates a detector Shape.
     */
    private void initDetector(){
       
        DetectorShapeView2D  viewFTCAL = new DetectorShapeView2D("FTCAL");
        double crystal_size=15;
        for(int iy=0; iy<22; iy++) {
            for(int ix=0; ix<22; ix++) {
                int component=iy*22+ix;
                double xcenter=crystal_size*(22-ix-0.5);
                double ycenter=crystal_size*(22-iy-0.5);
                double rcenter=Math.sqrt(Math.pow(xcenter-crystal_size*11,2.0)+Math.pow(ycenter-crystal_size*11,2.0));
                DetectorShape2D shape = new DetectorShape2D(DetectorType.FTCAL,0,0,component);
                shape.createBarXY(crystal_size,crystal_size);
                shape.getShapePath().translateXYZ(xcenter,ycenter,0.0);
//                System.out.println(component + " " + ix + " " + iy + " " + xcenter + " " + ycenter);
                shape.setColor(0,145,0);
                if(rcenter>crystal_size*4 && rcenter<crystal_size*11) 
                viewFTCAL.addShape(shape);
            }
        }
                
//        for(int sector = 0; sector < 6; sector++){
//            for(int paddle = 0; paddle < 5; paddle++){
//                DetectorShape2D  shape = new DetectorShape2D(DetectorType.FTCAL,sector,2,paddle);
//                // create an Arc with 
//                // inner  radius = 40 + paddle*10
//                // outter radius = 50 + paddle*10
//                // starting angle -25.0 degrees
//                // ending angle    25.0 degrees
//                shape.createArc(40 + paddle*10, 40 + paddle*10 + 10, -25.0, 25.0);
//                shape.getShapePath().rotateZ(Math.toRadians(sector*60.0));
//                if(paddle%2==0){
//                    shape.setColor(180, 255, 180);
//                } else {
//                    shape.setColor(180, 180, 255);
//                }
//                dv2.addShape(shape);                
//            }
//        }
        this.view.addDetectorLayer(viewFTCAL);
        view.addDetectorListener(this);
    }
    /**
     * When the detector is clicked, this function is called
     * @param desc 
     */
    public void detectorSelected(DetectorDescriptor desc) {
        this.canvas.divide(1,2);
        if(H_fADC.containsKey(desc.getComponent())){
            H1D hfADC = H_fADC.get(desc.getComponent());
            hfADC.setFillColor(2);
            System.out.println(desc.getSector() + " " + desc.getLayer() + " " + desc.getComponent() + " hfADC.getName()=" + hfADC.getName());
            canvas.cd(0);
            canvas.draw(hfADC);
        }
        if(H_NOISE.containsKey(desc.getComponent())){
            H1D hnoise = H_NOISE.get(desc.getComponent());
            hnoise.setFillColor(4);
            canvas.cd(1);
            canvas.draw(hnoise);
        }
//      if(tdcH.hasEntry(desc.getSector(),desc.getLayer(),desc.getComponent())){
//            H1D h1 = tdcH.get(desc.getSector(),desc.getLayer(),desc.getComponent());
//            h1.setTitle(h1.getName());
//            canvas.cd(0);
//            canvas.draw(h1);
//        }
//        if(adcH.hasEntry(desc.getSector(),desc.getLayer(),desc.getComponent())){
//            H1D h1 = adcH.get(desc.getSector(),desc.getLayer(),desc.getComponent());
//            h1.setTitle(h1.getName());
//            canvas.cd(1);
//            canvas.draw(h1);
//        }
    }
    
    /**
     * Each redraw of the canvas passes detector shape object to this routine
     * and user can change the color of specific component depending
     * on occupancy or some other criteria.
     * @param shape 
     */
    public void update(DetectorShape2D shape) {
        int sector = shape.getDescriptor().getSector();
        int layer = shape.getDescriptor().getLayer();
        int paddle = shape.getDescriptor().getComponent();
        //shape.setColor(200, 200, 200);
        if(this.H_fADC.containsKey(shape.getDescriptor().getComponent())){
            int nent = this.H_fADC.get(paddle).getEntries();
//            Color col = palette.getColor3D(nent, nProcessed, true);           
            /*int colorRed = 240;
            if(nProcessed!=0){
                colorRed = (255*nent)/(nProcessed);
            }*/
//            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
            if(nent>0) {
                if(this.H_NOISE.get(paddle).getMean()>2 &&
                   this.H_NOISE.get(paddle).getMean()<3) {
                    shape.setColor(0,145,0);
                }
                else{
                    shape.setColor(0, 145, 0);
                }
            }
            else {
                shape.setColor(100, 100, 100);
            }
        }
    }

    public void processEvent(DataEvent de) {
        
       
        
        EvioDataEvent event = (EvioDataEvent) de;
//        if(event.hasBank("FTOF2B::dgtz")){
//            EvioDataBank bank = (EvioDataBank) event.getBank("FTOF2B::dgtz");
//            int  rows = bank.rows();
//            
//            for(int row = 0; row < rows; row++){
//                this.nProcessed++;
//                int sector = bank.getInt("sector", row) - 1;
//                int paddle = bank.getInt("paddle", row) - 1;
//                int ADCL   = bank.getInt("ADCL", row);
//                int TDCL   = bank.getInt("TDCL", row);
//                tdcH.get(sector, 2, paddle).fill(TDCL);
//                adcH.get(sector, 2, paddle).fill(ADCL);
//            }
//        }
        decoder.decode(event);
        
        List<DetectorCounter>   counters = decoder.getDetectorCounters(DetectorType.FTCAL);
        MyADCFitter fadcFitter = new MyADCFitter();
        int icounter=0;
        for(DetectorCounter counter : counters){
             int key = counter.getDescriptor().getComponent();
//             System.out.println(counters.size() + " " + icounter + " " + counter.getDescriptor().getComponent());
//             System.out.println(counter);
             fadcFitter.fit(counter.getChannels().get(0));
//             System.out.println(" pedestal = " + fadcFitter.getPedestal() + " rms = " + fadcFitter.getRMS());
             short pulse[] = counter.getChannels().get(0).getPulse();
             for(int i=0; i<Math.min(pulse.length,H_fADC.get(key).getAxis().getNBins()); i++) {
                  H_fADC.get(key).fill(i,pulse[i]-fadcFitter.getPedestal()+10.0);
             }
             H_NOISE.get(key).fill(fadcFitter.getRMS());

             
             icounter++;
        }
        
//        for(DetectorBankEntry bank : banks) {
//            if(bank.getType()==BankType.ADCPULSE){
//                H1D hp = EventDecoder.getADCPulse(bank);
//                for(int bin = 0; bin < hp.getxAxis().getNBins();bin++){
////                    System.out.println(bin + " " + hp.getBinContent(bin));
//                }
//            }
//        }
        // The name FTOF1A comes from TRANSLATION TABLE (look below)
        // For other detectors use decoder.getDataEntries("PCAL") for example
        // List<DetectorBankEntry> counters =  decoder.getDataEntries("FTCAL");
        // The entire list of decoded data can be obtained by:
        // List<DetectorBankEntry> counters =  decoder.getDataEntries();
//        decoder.getDetectorCounters(DetectorType.FTCAL);

//        for(DetectorBankEntry cnt : counters){
//            System.out.println(cnt);
//        }
    }

    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().compareTo("Process")==0){
            DetectorEventProcessorDialog dialog = new DetectorEventProcessorDialog(this);
        }
    }
    
    public static void main(String[] args){
        new FTCALViewerWithCanvas();
    }

    
    
    public class MyADCFitter implements IFADCFitter {
        double rms = 0;
        double pedestal=0;
        
        public double getPedestal(){
            return pedestal;
        } 
        
        public double getRMS(){
            return rms;
        } 
        
        public void fit(DetectorChannel dc) {
            short[] pulse = dc.getPulse();
            double  ped   = 0.0;
            double  noise=0;
            for(int bin = ped_i1; bin < ped_i2; bin++){
                ped   += pulse[bin];
                noise += pulse[bin]*pulse[bin];
            }
            pedestal = ped/(ped_i2-ped_i1);
            rms      = LSB*Math.sqrt(noise/(ped_i2-ped_i1)-pedestal*pedestal);
        }
            

    }
}
