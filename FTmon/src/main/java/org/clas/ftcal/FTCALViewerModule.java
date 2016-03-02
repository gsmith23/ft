package org.clas.ftcal;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeTabView;
import org.jlab.clas12.calib.DetectorShapeView2D;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clas12.detector.DetectorChannel;
import org.jlab.clas12.detector.DetectorCounter;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clas12.detector.FADCBasicFitter;
import org.jlab.clas12.detector.IFADCFitter;
import org.root.attr.ColorPalette;
import org.root.func.F1D;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.root.basic.EmbeddedCanvas;

public class FTCALViewerModule implements IDetectorListener,ActionListener,ChangeListener{

    // panels and canvases
    JPanel detectorPanel;
    ColorPalette palette = new ColorPalette();
    EmbeddedCanvas canvas = new EmbeddedCanvas();
    EmbeddedCanvas canvasEvent     = new EmbeddedCanvas();
    EmbeddedCanvas canvasNoise     = new EmbeddedCanvas();
    EmbeddedCanvas canvasEnergy    = new EmbeddedCanvas();
    EmbeddedCanvas canvasTime      = new EmbeddedCanvas();
    DetectorShapeTabView view = new DetectorShapeTabView();
    
    // histograms, functions and graphs
    DetectorCollection<H1D> H_fADC = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_WAVE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_fADC   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_VMAX = new DetectorCollection<H1D>();
    DetectorCollection<F1D> mylandau = new DetectorCollection<F1D>();
    H1D hfADC=null;
    H1D H_fADC_N = null;
    H1D H_WMAX   = null;
    H1D H_COSMIC_N = null;
    double[] crystalID; 
    double[] noiseRMS;
    double[] cosmicCharge;
    int[] crystalPointers;    
    

    // decoded related information
    int nProcessed = 0;
    EventDecoder decoder;
    
    public EventDecoder getDecoder() {
        return decoder;
    }
    
    public void setDecoder(EventDecoder decoder) {
        this.decoder = decoder;
    }


    // analysis parameters
    int threshold = 12; // 10 fADC value <-> ~ 5mV
    int ped_i1 = 4;
    int ped_i2 = 24;
    int pul_i1 = 30;
    int pul_i2 = 70;
    double LSB = 0.4884;
    int[] cry_event = new int[484];
    int[] cry_max = new int[484];
    int[] cry_n = new int[22];
    int ncry_cosmic = 4;        // number of crystals above threshold in a column for cosmics selection
    double crystal_size = 15;


    // control variables
    private int plotSelect = 0;  // 0 - waveforms, 1 - noise
    private int keySelect = 8;

    
    
    public FTCALViewerModule(){
        this.detectorPanel=null;
        this.decoder=null;
    }


    public JPanel getDetectorPanel() {
        return detectorPanel;
    }

    public void setDetectorPanel(JPanel detectorPanel) {
        this.detectorPanel = detectorPanel;
    }

    public void initPanel() {
        // detector panel consists of a split pane with detector view and tabbed canvases
        JSplitPane splitPane = new JSplitPane();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Event Viewer",this.canvasEvent);
        tabbedPane.add("Noise"       ,this.canvasNoise);
        tabbedPane.add("Energy"      ,this.canvasEnergy);
        tabbedPane.add("Time"        ,this.canvasTime);
        tabbedPane.add("General"     ,this.canvas);
        tabbedPane.addChangeListener(this);
        this.initCanvas();
        
        JPanel canvasPane = new JPanel();

        canvasPane.setLayout(new BorderLayout());
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());

        JButton resetBtn = new JButton("Clear Histograms");
        resetBtn.addActionListener(this);
        buttonPane.add(resetBtn);

        JButton fitBtn = new JButton("Fit Histograms");
        fitBtn.addActionListener(this);
        buttonPane.add(fitBtn);

        JRadioButton wavesRb  = new JRadioButton("Waveforms");
        JRadioButton noiseRb  = new JRadioButton("Noise");
        JRadioButton cosmicOccRb = new JRadioButton("Cosmics(Occ)");
        JRadioButton cosmicCrgRb = new JRadioButton("Cosmics(Fit)");
        ButtonGroup group = new ButtonGroup();
        group.add(wavesRb);
        group.add(noiseRb);
        group.add(cosmicOccRb);
        group.add(cosmicCrgRb);
//        buttonPane.add(wavesRb);
//        buttonPane.add(noiseRb);
//        buttonPane.add(cosmicOccRb);
//        buttonPane.add(cosmicCrgRb);
        wavesRb.setSelected(true);
        wavesRb.addActionListener(this);
        noiseRb.addActionListener(this);
        cosmicOccRb.addActionListener(this);
        cosmicCrgRb.addActionListener(this);

        canvasPane.add(tabbedPane, BorderLayout.CENTER);
        canvasPane.add(buttonPane, BorderLayout.PAGE_END);
    
        splitPane.setLeftComponent(this.view);
        splitPane.setRightComponent(canvasPane);
 
        this.detectorPanel.add(splitPane, BorderLayout.CENTER);

    }

    public void initCanvas() {
        // event canvas
        this.canvasEvent.setGridX(false);
        this.canvasEvent.setGridY(false);
        this.canvasEvent.setAxisFontSize(10);
        this.canvasEvent.setTitleFontSize(16);
        this.canvasEvent.setAxisTitleFontSize(14);
        this.canvasEvent.setStatBoxFontSize(8);
        // noise
        this.canvasNoise.divide(1, 2);
        this.canvasNoise.cd(0);
        this.canvasNoise.setGridX(false);
        this.canvasNoise.setGridY(false);
        this.canvasNoise.setAxisFontSize(10);
        this.canvasNoise.setTitleFontSize(16);
        this.canvasNoise.setAxisTitleFontSize(14);
        this.canvasNoise.setStatBoxFontSize(8);
        this.canvasNoise.cd(1);
        this.canvasNoise.setGridX(false);
        this.canvasNoise.setGridY(false);
        this.canvasNoise.setAxisFontSize(10);
        this.canvasNoise.setTitleFontSize(16);
        this.canvasNoise.setAxisTitleFontSize(14);
        this.canvasNoise.setStatBoxFontSize(8);
        // energy
        this.canvasEnergy.divide(2, 2);
        this.canvasEnergy.cd(0);
        this.canvasEnergy.setGridX(false);
        this.canvasEnergy.setGridY(false);
        this.canvasEnergy.setAxisFontSize(10);
        this.canvasEnergy.setTitleFontSize(16);
        this.canvasEnergy.setAxisTitleFontSize(14);
        this.canvasEnergy.setStatBoxFontSize(8);
        this.canvasEnergy.cd(1);
        this.canvasEnergy.setGridX(false);
        this.canvasEnergy.setGridY(false);
        this.canvasEnergy.setAxisFontSize(10);
        this.canvasEnergy.setTitleFontSize(16);
        this.canvasEnergy.setAxisTitleFontSize(14);
        this.canvasEnergy.setStatBoxFontSize(8);
        this.canvasEnergy.cd(2);
        this.canvasEnergy.setGridX(false);
        this.canvasEnergy.setGridY(false);
        this.canvasEnergy.setAxisFontSize(10);
        this.canvasEnergy.setTitleFontSize(16);
        this.canvasEnergy.setAxisTitleFontSize(14);
        this.canvasEnergy.setStatBoxFontSize(8);
    }

    private void resetCanvas() {
        this.canvas.divide(1, 1);
        canvas.cd(0);
    }
    
    public void initDetector() {
        DetectorShapeView2D viewFTCAL = new DetectorShapeView2D("FTCAL");
        for (int component = 0; component < 22*22; component++) {
            if(doesThisCrystalExist(component)) {
                int iy = component / 22;
                int ix = component - iy * 22;
                double xcenter = crystal_size * (22 - ix - 0.5);
                double ycenter = crystal_size * (22 - iy - 0.5);
                DetectorShape2D shape = new DetectorShape2D(DetectorType.FTCAL, 0, 0, component);
                shape.createBarXY(crystal_size, crystal_size);
                shape.getShapePath().translateXYZ(xcenter, ycenter, 0.0);
                shape.setColor(0, 145, 0);
                viewFTCAL.addShape(shape);               
            }
        }
        this.view.addDetectorLayer(viewFTCAL);
        view.addDetectorListener(this);

    }

    private boolean doesThisCrystalExist(int id) {

        boolean crystalExist=false;
        int iy = id / 22;
        int ix = id - iy * 22;

        double xcrystal = crystal_size * (22 - ix - 0.5);
        double ycrystal = crystal_size * (22 - iy - 0.5);
        double rcrystal = Math.sqrt(Math.pow(xcrystal - crystal_size * 11, 2.0) + Math.pow(ycrystal - crystal_size * 11, 2.0));
        if (rcrystal > crystal_size * 4 && rcrystal < crystal_size * 11) {
            crystalExist=true;
        }
        return crystalExist;
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        System.out.println("FTCALViewerModule ACTION = " + e.getActionCommand());
        if (e.getActionCommand().compareTo("Reset") == 0) {
            resetHistograms();
        }
        if (e.getActionCommand().compareTo("Fit") == 0) {
            fitHistograms();
        }

//        if (e.getActionCommand().compareTo("Waveforms") == 0) {
//            plotSelect = 0;
//            resetCanvas();
//        } else if (e.getActionCommand().compareTo("Noise") == 0) {
//            plotSelect = 1;
//        } else if (e.getActionCommand().compareTo("Cosmics(Occ)") == 0) {
//            plotSelect = 2;
//        } else if (e.getActionCommand().compareTo("Cosmics(Fit)") == 0) {
//            plotSelect = 3;
//        }

    }

     public void stateChanged(ChangeEvent e) {
        JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
        int index = sourceTabbedPane.getSelectedIndex();
        System.out.println("Tab changed to: " + sourceTabbedPane.getTitleAt(index) + " with index " + index);
        plotSelect = index;
    }

    public void initHistograms() {
        for (int component = 0; component < 503; component++) {
            if(doesThisCrystalExist(component) || component==501 || component==502) {

                int iy = component / 22;
                int ix = component - iy * 22;
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
                String title = "Crystal " + component + " (" + ix + "," + iy + ")";
                H_fADC.add(0, 0, component, new H1D(DetectorDescriptor.getName("fADC", 0, 0, component), title, 100, 0.0, 100.0));
                H_NOISE.add(0, 0, component, new H1D(DetectorDescriptor.getName("Noise", 0, 0, component), title, 200, 0.0, 10.0));
                H_NOISE.get(0, 0, component).setFillColor(4);
                H_NOISE.get(0, 0, component).setXTitle("RMS (mV)");
                H_NOISE.get(0, 0, component).setYTitle("Counts");                        
                H_WAVE.add(0, 0, component, new H1D(DetectorDescriptor.getName("WAVE", 0, 0, component), title, 100, 0.0, 100.0));
                H_WAVE.get(0, 0, component).setFillColor(5);
                H_WAVE.get(0, 0, component).setXTitle("fADC Sample");
                H_WAVE.get(0, 0, component).setYTitle("fADC Counts");
                H_COSMIC_fADC.add(0, 0, component, new H1D(DetectorDescriptor.getName("Cosmic fADC", 0, 0, component), title, 100, 0.0, 100.0));
                H_COSMIC_fADC.get(0, 0, component).setFillColor(3);
                H_COSMIC_fADC.get(0, 0, component).setXTitle("fADC Sample");
                H_COSMIC_fADC.get(0, 0, component).setYTitle("fADC Counts");
                H_COSMIC_CHARGE.add(0, 0, component, new H1D(DetectorDescriptor.getName("Cosmic Charge", 0, 0, component), title, 80, 0.0, 80.0));
                H_COSMIC_CHARGE.get(0, 0, component).setFillColor(2);
                H_COSMIC_CHARGE.get(0, 0, component).setXTitle("Charge (pC)");
                H_COSMIC_CHARGE.get(0, 0, component).setYTitle("Counts");
                H_COSMIC_VMAX.add(0, 0, component, new H1D(DetectorDescriptor.getName("Cosmic Amplitude", 0, 0, component), title, 80, 0.0, 40.0));
                H_COSMIC_VMAX.get(0, 0, component).setFillColor(2);
                H_COSMIC_VMAX.get(0, 0, component).setXTitle("Amplitude (mV)");
                H_COSMIC_VMAX.get(0, 0, component).setYTitle("Counts");
                 
                mylandau.add(0, 0, component, new F1D("landau",     0.0, 80.0));
            }
        }
        H_fADC_N   = new H1D("fADC", 484, 0, 484);
        H_WMAX     = new H1D("WMAX", 484, 0, 484);
        H_COSMIC_N = new H1D("EVENT", 484, 0, 484);

        crystalID       = new double[332];
        noiseRMS        = new double[332];
        crystalPointers = new int[484];
        int ipointer=0;
        for(int i=0; i<484; i++) {
            if(doesThisCrystalExist(i)) {
                crystalPointers[i]=ipointer;
                crystalID[ipointer]=i;
                ipointer++;
            }
            else {
                crystalPointers[i]=-1;
            }
        }


    }

    private void initLandauFitPar(int key, H1D hcosmic) {
        if(hcosmic.getBinContent(0)==0) mylandau.add(0, 0, key, new F1D("landau",     0.0, 80.0));
        else                            mylandau.add(0, 0, key, new F1D("landau+exp", 0.0, 80.0));
        if(hcosmic.getBinContent(0)<10) {
            mylandau.get(0, 0, key).setParameter(0, hcosmic.getBinContent(hcosmic.getMaximumBin()));
        }
        else {
            mylandau.get(0, 0, key).setParameter(0, 10);
        }
        mylandau.get(0, 0, key).setParameter(1,hcosmic.getMean());
        mylandau.get(0, 0, key).setParameter(2,5);
        if(hcosmic.getBinContent(0)!=0) {
            mylandau.get(0, 0, key).setParameter(3,hcosmic.getBinContent(0));
            mylandau.get(0, 0, key).setParameter(4, -0.2);
        }
    }

    private void fitHistograms() {
        for(int key=0; key< 22*22; key++) {
            if(H_COSMIC_CHARGE.hasEntry(0, 0, key)) {
                if(H_COSMIC_CHARGE.get(0, 0, key).getEntries()>200) {
                    H1D hcosmic = H_COSMIC_CHARGE.get(0,0,key);
                    initLandauFitPar(key,hcosmic);
                    hcosmic.fit(mylandau.get(0, 0, key));
                }
            }   
        }
        boolean flag_parnames=true;
        for(int key=0; key< 22*22; key++) {
            if(mylandau.hasEntry(0, 0, key)) {
                if(flag_parnames) {
                    System.out.println("Component\t amp\t mean\t sigma\t p0\t p1\t Chi2");
                    flag_parnames=false;
                }
                System.out.print(key + "\t\t ");
                for(int i=0; i<mylandau.get(0, 0, key).getNParams(); i++) System.out.format("%.2f\t ",mylandau.get(0, 0, key).getParameter(i));
                if(mylandau.get(0, 0, key).getNParams()==3) System.out.print("0.0\t 0.0\t");
                double perrors = mylandau.get(0, 0, key).parameter(0).error();
                if(mylandau.get(0, 0, key).getParameter(0)>0)
                    System.out.format("%.2f\n",mylandau.get(0, 0, key).getChiSquare(H_COSMIC_CHARGE.get(0,0,key).getDataSet())
                            /mylandau.get(0, 0, key).getNDF(H_COSMIC_CHARGE.get(0,0,key).getDataSet()));
                else
                    System.out.format("0.0\n");
            }
        }
    }

    public void resetHistograms() { 
        for (int component = 0; component < 22 * 22; component++) {
            H_fADC.get(0, 0, component).reset();
            H_NOISE.get(0, 0, component).reset();
            H_fADC_N.reset();
            H_COSMIC_fADC.get(0, 0, component).reset();
            H_COSMIC_CHARGE.get(0, 0, component).reset();
            H_COSMIC_VMAX.get(0, 0, component).reset();
            H_COSMIC_N.reset();
        }
        // TODO Auto-generated method stub

    }
    
    public void initDecoder() {
        decoder.addFitter(DetectorType.FTCAL,
                new FADCBasicFitter(ped_i1, // first bin for pedestal
                        ped_i2, // last bin for pedestal
                        pul_i1, // first bin for pulse integral
                        pul_i2 // last bin for pulse integral
                        ));    
    }

    public class MyADCFitter implements IFADCFitter {

        double rms = 0;
        double pedestal = 0;
        double wave_max=0;

        public double getPedestal() {
            return pedestal;
        }

        public double getRMS() {
            return rms;
        }

        public double getWave_Max() {
            return wave_max;
        }

        public void fit(DetectorChannel dc) {
            short[] pulse = dc.getPulse();
            double ped = 0.0;
            double noise = 0;
            double wmax=0;
            for (int bin = ped_i1; bin < ped_i2; bin++) {
                ped += pulse[bin];
                noise += pulse[bin] * pulse[bin];
            }
            for (int bin=0; bin<pulse.length; bin++) {
                if(pulse[bin]>wmax) wmax=pulse[bin];
            }
            pedestal = ped / (ped_i2 - ped_i1);
            rms = LSB * Math.sqrt(noise / (ped_i2 - ped_i1) - pedestal * pedestal);
            wave_max=wmax;
        }

    }


    public void processDecodedEvent() {
        // TODO Auto-generated method stub

        nProcessed++;

        //    System.out.println("event #: " + nProcessed);
        List<DetectorCounter> counters = decoder.getDetectorCounters(DetectorType.FTCAL);
        FTCALViewerModule.MyADCFitter fadcFitter = new FTCALViewerModule.MyADCFitter();
        H_WMAX.reset();
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            //                System.out.println(counters.size() + " " + key + " " + counter.getDescriptor().getComponent());
            //                 System.out.println(counter);
            fadcFitter.fit(counter.getChannels().get(0));
            short pulse[] = counter.getChannels().get(0).getPulse();
            H_fADC_N.fill(key);
            H_WAVE.get(0, 0, key).reset();
            for (int i = 0; i < Math.min(pulse.length, H_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                H_fADC.get(0, 0, key).fill(i, pulse[i] - fadcFitter.getPedestal() + 10.0);
                H_WAVE.get(0, 0, key).fill(i, pulse[i]);
            }
            H_WMAX.fill(key,fadcFitter.getWave_Max()-fadcFitter.getPedestal());
            if(fadcFitter.getWave_Max()-fadcFitter.getPedestal()>threshold) 
                //            System.out.println("   Component #" + key + " is above threshold, max=" + fadcFitter.getWave_Max() + " ped=" + fadcFitter.getPedestal());
                H_NOISE.get(0, 0, key).fill(fadcFitter.getRMS());
        }
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            int iy  = key/22;
            int ix  = key - iy * 22;
            int nCrystalInColumn = 0;
            fadcFitter.fit(counter.getChannels().get(0));
            for(int i=0; i<22; i++) {
                if(i!=iy && doesThisCrystalExist(i*22+ix)) {
                    if(H_WMAX.getBinContent(i*22+ix)>threshold) nCrystalInColumn++;                    
                }
            }
            if(nCrystalInColumn>4) {
                short pulse[] = counter.getChannels().get(0).getPulse();
                H_COSMIC_N.fill(key);
                for (int i = 0; i < Math.min(pulse.length, H_COSMIC_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                    H_COSMIC_fADC.get(0, 0, key).fill(i, pulse[i]-fadcFitter.getPedestal() + 10.0);                
                }
                H_COSMIC_CHARGE.get(0, 0, key).fill(counter.getChannels().get(0).getADC().get(0)*LSB*4.0/50);
                H_COSMIC_VMAX.get(0, 0, key).fill((fadcFitter.getWave_Max()-fadcFitter.getPedestal())*LSB);
            }
        }
        if (plotSelect == 0 && H_WAVE.hasEntry(0, 0, keySelect)) {
            this.canvas.draw(H_WAVE.get(0, 0, keySelect));            
        }
        this.canvasEvent.draw(H_WAVE.get(0, 0, keySelect)); 
        //this.dcHits.show();
        this.view.repaint();


    }
    
    public void detectorSelected(DetectorDescriptor desc) {
        // TODO Auto-generated method stub


        keySelect = desc.getComponent();

        // event viewer
        this.canvasEvent.draw(H_WAVE.get(0, 0, keySelect));
        // noise
        for(int key=0; key<crystalPointers.length; key++) {
            if(crystalPointers[key]>=0) {
                noiseRMS[crystalPointers[key]]=H_NOISE.get(0, 0, key).getMean();
            }
        }
        canvasNoise.cd(0);
        GraphErrors  G_NOISE = new GraphErrors(crystalID,noiseRMS);
        G_NOISE.setTitle(" "); //  title
        G_NOISE.setXTitle("Crystal ID"); // X axis title
        G_NOISE.setYTitle("Noise RMS (mV)");   // Y axis title
        G_NOISE.setMarkerColor(4); // color from 0-9 for given palette
        G_NOISE.setMarkerSize(5); // size in points on the screen
        G_NOISE.setMarkerStyle(1); // Style can be 1 or 2
        canvasNoise.draw(G_NOISE);
        if (H_NOISE.hasEntry(0, 0, keySelect)) {
            H1D hnoise = H_NOISE.get(0, 0, keySelect);
            canvasNoise.cd(1);
            canvasNoise.draw(hnoise,"S");
        }
        // Energy - Cosmics for now
        canvasEnergy.cd(0);
        if (H_COSMIC_fADC.hasEntry(0, 0, keySelect)) {
            hfADC = H_COSMIC_fADC.get(0, 0, keySelect).histClone(" ");
            hfADC.normalize(H_COSMIC_N.getBinContent(keySelect));
            hfADC.setFillColor(3);
            hfADC.setXTitle("fADC Sample");
            hfADC.setYTitle("fADC Counts");
            canvasEnergy.cd(0);
            canvasEnergy.draw(hfADC,"S");               
        }
        canvasEnergy.cd(1);
        if(H_COSMIC_VMAX.hasEntry(0, 0, keySelect)) {
            H1D hcosmic = H_COSMIC_VMAX.get(0,0,keySelect);
            canvasEnergy.draw(hcosmic,"S");
        }
        canvasEnergy.cd(2);
        if(H_COSMIC_CHARGE.hasEntry(0, 0, keySelect)) {
            H1D hcosmic = H_COSMIC_CHARGE.get(0,0,keySelect);
            initLandauFitPar(keySelect,hcosmic);
            hcosmic.fit(mylandau.get(0, 0, keySelect));
            canvasEnergy.draw(hcosmic,"S");
            canvasEnergy.draw(mylandau.get(0, 0, keySelect),"sameS");
        }       
    }

    public void update(DetectorShape2D shape) {
    

        int sector = shape.getDescriptor().getSector();
        int layer = shape.getDescriptor().getLayer();
        int paddle = shape.getDescriptor().getComponent();
        //shape.setColor(200, 200, 200);
        if(plotSelect==0) {
            if(H_WMAX.getBinContent(paddle)>threshold) {
                shape.setColor(200, 0, 200);
            }
            else {
                shape.setColor(100, 100, 100);
            }
        }
        else if(plotSelect==1) {
            if (this.H_fADC.hasEntry(sector, layer, paddle)) {
                int nent = this.H_fADC.get(sector, layer, paddle).getEntries();
                //            Color col = palette.getColor3D(nent, nProcessed, true);           
                /*int colorRed = 240;
                 if(nProcessed!=0){
                 colorRed = (255*nent)/(nProcessed);
                 }*/
                //            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
                if (nent > 0) {
                    if (this.H_NOISE.get(sector, layer, paddle).getMean() > 1.0
                            && this.H_NOISE.get(sector, layer, paddle).getMean() < 1.5) {
                        shape.setColor(0, 145, 0);
                    } else if (this.H_NOISE.get(sector, layer, paddle).getMean() < 1.0) {
                        shape.setColor(0, 0, 100);
                    } else {
                        shape.setColor(255, 100, 0);
                    }
                } else {
                    shape.setColor(100, 100, 100);
                }
            }
        }
        else {
            if (this.H_COSMIC_CHARGE.hasEntry(sector, layer, paddle)) {
                if(plotSelect==2) {
                    int nent = this.H_COSMIC_CHARGE.get(sector, layer, paddle).getEntries();
                    Color col = palette.getColor3D(nent, nProcessed, true);           
                    /*int colorRed = 240;
                 if(nProcessed!=0){
                 colorRed = (255*nent)/(nProcessed);
                 }*/
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
                }
                else if(plotSelect==3){
                    double lmean = this.mylandau.get(sector, layer, paddle).getParameter(1);
                    Color col = palette.getColor3D(lmean, 80., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
                }
            }
        }
    }
 




}
