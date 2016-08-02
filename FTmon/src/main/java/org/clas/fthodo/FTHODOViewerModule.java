package org.clas.fthodo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static java.lang.Math.*;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JPanel;
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

import org.jlab.containers.HashTable;
import org.jlab.containers.HashTableViewer;
import org.jlab.containers.IHashTableListener;

import org.root.attr.ColorPalette;

import org.root.func.F1D;

import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.root.histogram.H2D;

import org.root.basic.EmbeddedCanvas;

public class FTHODOViewerModule implements IDetectorListener,
					   IHashTableListener,
					   ActionListener,
					   ChangeListener{
    EventDecoder decoder;
    
    //=================================
    //    PANELS, CANVASES ETC
    //=================================
    
    JPanel detectorPanel;
    JPanel canvasPane = new JPanel(new BorderLayout());
    
    EmbeddedCanvas canvasEvent   = new EmbeddedCanvas();
    EmbeddedCanvas canvasPed     = new EmbeddedCanvas();
    EmbeddedCanvas canvasNoise   = new EmbeddedCanvas();
    EmbeddedCanvas canvasGain    = new EmbeddedCanvas();
    EmbeddedCanvas canvasCharge  = new EmbeddedCanvas();
    EmbeddedCanvas canvasVoltage = new EmbeddedCanvas();
    EmbeddedCanvas canvasMIP     = new EmbeddedCanvas();
    EmbeddedCanvas canvasMatch   = new EmbeddedCanvas();
    EmbeddedCanvas canvasTime    = new EmbeddedCanvas();
    
    public EmbeddedCanvas canvasHODOEvent  = new EmbeddedCanvas();
    
    DetectorShapeTabView view    = new DetectorShapeTabView();
    // Gagik to implement
    // view.addChangeListener(this);
    
    ColorPalette         palette = new ColorPalette();
    HashTable            summaryTable = null;
    
    //=================================
    //     HISTOGRAMS, GRAPHS
    //=================================
    
    //---------------
    // Event-by-Event
    // raw pulse
    DetectorCollection<H1D> H_FADC = new DetectorCollection<H1D>();
    // baseline subtracted pulse calibrated to voltage and time
    DetectorCollection<H1D> H_VT = new DetectorCollection<H1D>();
    // '' calibrated to no. photoelectrons and time
    DetectorCollection<H1D> H_NPE = new DetectorCollection<H1D>();
        
    // Accumulated
    DetectorCollection<H1D> H_PED = new DetectorCollection<H1D>();

    DetectorCollection<GraphErrors> H_PED_VS_EVENT;
    DetectorCollection<GraphErrors> H_PED_INDEX;
    
    DetectorCollection<H1D> H_MAXV = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_MIP_V = new DetectorCollection<H1D>();
    
    DetectorCollection<H1D> H_NOISE_Q = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_MIP_Q = new DetectorCollection<H1D>();
        
    DetectorCollection<H1D> H_NPE_INT   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NPE_NOISE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NPE_MATCH = new DetectorCollection<H1D>();
    
    DetectorCollection<H1D> H_TIME_MODE1 = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_DT_MODE1   = new DetectorCollection<H1D>();
    
    // 2D
    DetectorCollection<H2D> H_MAXV_VS_T = new DetectorCollection<H2D>();
    DetectorCollection<H2D> H_T1_T2     = new DetectorCollection<H2D>();

    // 1D
    DetectorCollection<H1D> H_fADC = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_fADC   = new DetectorCollection<H1D>();
    
    // Fit Functions
    DetectorCollection<F1D> fPed  = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fQ1   = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fQ2   = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fQMIP = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fV1   = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fV2   = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fVMIP = new DetectorCollection<F1D>();
    
    DetectorCollection<Integer> dcHits = new DetectorCollection<Integer>();
    
    H1D H_W_MAX      = null;
    H1D H_V_MAX      = null;
    H1D H_NPE_MAX    = null;
    H1D H_CHARGE_MAX = null;
    
    //=================================
    //           ARRAYS
    //=================================
    
    private double pedMean[][][];
    private double vMax[][][];
    
    private double meanNPE[][][];
    private double errNPE[][][];
    private double sigNPE[][][];
    
    private double meanNPE_mV[][][];
    private double errNPE_mV[][][];
    private double sigNPE_mV[][][];

    private double gain[][][];
    private double errGain[][][];
    
    final   double gainNew    = 20;
    final   double gainOld    = 70;
    
    private double gain_mV[][][];
    private double errGain_mV[][][];
    
    final   double gainNew_mV = 10;
    final   double gainOld_mV = 44;
    
    private double npeEvent[][][];
        
    boolean testMode = false;

    double nGain    = gainNew;
    double nGain_mV = gainNew_mV;
    
    //=================================
    //           CONSTANTS
    //=================================
    
    // extract from file name
    int    runNumber     = 982;
        
    int    fADCBins      = 4096;
    double voltageMax    = 2000.;
    double LSB           = voltageMax/fADCBins;
    
    double thrshNPE      = 3.;
    double thrshNoiseNPE = 0.5;
    
    double voltsPerSPE   = 10.;
    double binsPerSPE    = voltsPerSPE/LSB;
    
    double thrshVolts    = thrshNPE*voltsPerSPE;
    double noiseThrshV   = thrshNoiseNPE*voltsPerSPE;
    
    double cosmicsThrsh  = thrshVolts/LSB;
    double noiseThrsh    = noiseThrshV/LSB;
    
    int ped_i1 = 4;
    int ped_i2 = 24;
    int pul_i1 = 30;
    int pul_i2 = 70;
    
    int ped_j1 = 79;
    int ped_j2 = 99;
    
    final int NBinsCosmic = 50;
    
    final int CosmicQXMin[]  = {0,200,300};
    final int CosmicQXMax[]  = {10000,5200,5300};
    
    final int CosmicNPEXMin[]  = {0,3,5};
    final int CosmicNPEXMax[]  = {200,93,133};
    
    final int NBinsNoise = 100;
    
    double NoiseQXMin[] = {0.  ,10., 10.};
    double NoiseQXMax[] = {310.,310.,310.};
    
    //=================================
    //           VARIABLES
    //=================================
    
    double   tile_size = 15;
    int      nProcessed = 0;
    
    private int tabSelect = 0;

    private int secSel = 5;
    private int laySel = 2;
    private int comSel = 1;    

    // !! change to radio button
    private boolean drawByElec = true;
    private boolean useGain_mV = true;
    
    JPanel rBPane;
    int previousTabSelect = 0;

    // the following indices must correspond
    // to the order the canvased are added
    // to 'tabbedPane'
    final private int tabIndexEvent   = 0;
    final private int tabIndexPed     = 1;
    final private int tabIndexNoise   = 2;
    final private int tabIndexGain    = 3;
    final private int tabIndexCharge  = 4;
    final private int tabIndexVoltage = 5;
    final private int tabIndexMIP     = 6;
    final private int tabIndexMatch   = 7;
    final private int tabIndexTime    = 8;
    final private int tabIndexTable   = 9;
    
    public void initPanel() {

	System.out.println(" LSB = " + LSB );
            
        JSplitPane splitPane = new JSplitPane();
        
        this.initTable();
        
        HashTableViewer canvasTable = new HashTableViewer(summaryTable);
        canvasTable.addListener(this);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Event"   ,this.canvasEvent);
        tabbedPane.add("Pedestal",this.canvasPed);
	tabbedPane.add("Noise"   ,this.canvasNoise);
        tabbedPane.add("Gain"    ,this.canvasGain);
        tabbedPane.add("Charge"  ,this.canvasCharge);
        tabbedPane.add("Voltage"  ,this.canvasVoltage);
        tabbedPane.add("MIP"     ,this.canvasMIP);
        tabbedPane.add("Match"   ,this.canvasMatch);
        tabbedPane.add("Time"    ,this.canvasTime);
        tabbedPane.add("Table"   ,canvasTable);
	tabbedPane.addChangeListener(this);
        this.initCanvas();
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        
        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(this);
        buttonPane.add(resetBtn);
        
        JButton fitBtn = new JButton("Fit");
        fitBtn.addActionListener(this);
        buttonPane.add(fitBtn);
        
        JButton variablesBtn = new JButton("Variables");
        variablesBtn.addActionListener(this);
        buttonPane.add(variablesBtn);

	//====        
	ButtonGroup group = new ButtonGroup();
	
	rBPane = new JPanel();
        rBPane.setLayout(new FlowLayout());
	
	JRadioButton rBGainPeak = new JRadioButton("Peak");
	JRadioButton rBGainChrg = new JRadioButton("Charge");
	
	JRadioButton rBElec   = new JRadioButton("Electronics");
	JRadioButton rBDetect = new JRadioButton("Detector");
	
	group.add(rBElec);
	rBPane.add(rBElec);
	rBElec.setSelected(true);
	rBElec.addActionListener(this);
    	
	group.add(rBDetect);
	rBPane.add(rBDetect);
	rBDetect.addActionListener(this);
    	
 	group.add(rBGainPeak);
	rBPane.add(rBGainPeak);
	rBGainPeak.setSelected(true);
	rBGainPeak.addActionListener(this);

//	System.out.println("rBGainPeak.isSelected() = " +
// 			   rBGainPeak.isSelected());
	    
	group.add(rBGainChrg);
	rBPane.add(rBGainChrg);
	//rBGainChrg.setSelected(true);
	rBGainChrg.addActionListener(this);
    	
	//=================================
	//      PLOTTING OPTIONS
	//=================================
	
        this.canvasPane.add(tabbedPane, BorderLayout.CENTER);
	
	this.canvasPane.add(buttonPane, BorderLayout.PAGE_END);
        
        splitPane.setRightComponent(this.canvasPane);
        splitPane.setLeftComponent(this.view);
        
        splitPane.setDividerLocation(400);
        
        this.detectorPanel.add(splitPane, BorderLayout.CENTER);
        
    } // end of: public void initPanel() {
	
    public void initCanvas() {
	
	// combined view event canvas
	this.canvasHODOEvent.setGridX(false);
        this.canvasHODOEvent.setGridY(false);
        this.canvasHODOEvent.setAxisFontSize(10);
        this.canvasHODOEvent.setTitleFontSize(16);
        this.canvasHODOEvent.setAxisTitleFontSize(14);
        this.canvasHODOEvent.setStatBoxFontSize(8);
        this.canvasHODOEvent.divide(2,1);
	
        this.canvasEvent.setGridX(false);
        this.canvasEvent.setGridY(false);
        this.canvasEvent.setAxisFontSize(10);
        this.canvasEvent.setTitleFontSize(16);
        this.canvasEvent.setAxisTitleFontSize(14);
        this.canvasEvent.setStatBoxFontSize(8);
        this.canvasEvent.divide(3,2);
	
	drawCanvasEvent(secSel,laySel,comSel);
        
	this.canvasPed.setGridX(false);
	this.canvasPed.setGridY(false);
        this.canvasPed.setAxisFontSize(10);
        this.canvasPed.setTitleFontSize(16);
        this.canvasPed.setAxisTitleFontSize(14);
        this.canvasPed.setStatBoxFontSize(8);
        this.canvasPed.divide(2,2);
        
	drawCanvasPed(secSel,laySel,comSel);
	
        this.canvasNoise.setGridX(false);
        this.canvasNoise.setGridY(false);
        this.canvasNoise.setAxisFontSize(10);
        this.canvasNoise.setTitleFontSize(16);
        this.canvasNoise.setAxisTitleFontSize(14);
        this.canvasNoise.setStatBoxFontSize(8);
        this.canvasNoise.divide(3,2);
        
	drawCanvasNoise(secSel,laySel,comSel);
	    
        this.canvasGain.setGridX(false);
        this.canvasGain.setGridY(false);
        this.canvasGain.setAxisFontSize(10);
        this.canvasGain.setTitleFontSize(16);
        this.canvasGain.setAxisTitleFontSize(14);
        this.canvasGain.setStatBoxFontSize(2);
        this.canvasGain.divide(3,3);

	drawCanvasGainElec(secSel,laySel,comSel);
    
        this.canvasCharge.setGridX(false);
        this.canvasCharge.setGridY(false);
        this.canvasCharge.setAxisFontSize(10);
        this.canvasCharge.setTitleFontSize(16);
        this.canvasCharge.setAxisTitleFontSize(14);
        this.canvasCharge.setStatBoxFontSize(2);
        this.canvasCharge.divide(2,2);
        
        drawCanvasCharge(secSel,laySel,comSel);
        
        this.canvasVoltage.setGridX(false);
        this.canvasVoltage.setGridY(false);
        this.canvasVoltage.setAxisFontSize(10);
        this.canvasVoltage.setTitleFontSize(16);
        this.canvasVoltage.setAxisTitleFontSize(14);
        this.canvasVoltage.setStatBoxFontSize(2);
        this.canvasVoltage.divide(2,2);
        
	drawCanvasVoltage(secSel,laySel,comSel);
	
        this.canvasMIP.setGridX(false);
        this.canvasMIP.setGridY(false);
        this.canvasMIP.setAxisFontSize(10);
        this.canvasMIP.setTitleFontSize(16);
        this.canvasMIP.setAxisTitleFontSize(14);
        this.canvasMIP.setStatBoxFontSize(2);
        this.canvasMIP.divide(3,3);
	
	drawCanvasMIPElec(secSel,laySel,comSel);
        
        this.canvasMatch.setGridX(true);
        this.canvasMatch.setGridY(true);
        this.canvasMatch.setAxisFontSize(10);
        this.canvasMatch.setTitleFontSize(16);
        this.canvasMatch.setAxisTitleFontSize(14);
        this.canvasMatch.setStatBoxFontSize(8);
        this.canvasMatch.divide(2,2);
	
	drawCanvasMatch(secSel,laySel,comSel);

        this.canvasTime.setGridX(false);
        this.canvasTime.setGridY(false);
        this.canvasTime.setAxisFontSize(10);
        this.canvasTime.setTitleFontSize(16);
        this.canvasTime.setAxisTitleFontSize(14);
        this.canvasTime.setStatBoxFontSize(8);
        this.canvasTime.divide(2,2);
        
	drawCanvasTime(secSel,laySel,comSel);
	
    }
    
    private void initTable() {
        summaryTable = new HashTable(3,
                                     "NPE:d",
                                     "NPE Error:d",
                                     "Gain:d",
                                     "Gain Error:d",
                                     "Gain [mV]:d",
                                     "Gain Error [mV]:d");
        
        double[] summaryInitialValues = {-1,-1,-1,-1,-1,-1};
        
        for (int layer = 2; layer > 0; layer--) {
            for (int sector = 1; sector < 9; sector++) {
                for ( int component = 1 ; component < 21 ; component++){
                    
                    if(sector%2==1 && component > 9 ) continue;
                    
                    summaryTable.addRow(summaryInitialValues,
                                        sector,
                                        layer,
                                        component);
                    
		    // npe
                    summaryTable.addConstrain(3, 15.0, 1000.0);
		    // gain
                    summaryTable.addConstrain(5, 20.0, 100.0);
		    // gain_mV
                    summaryTable.addConstrain(5, 5.0, 80.0);
                    
                }
            }
        }
        
    } // end of : private void initTable() {
    
    
    public FTHODOViewerModule(){
        
        this.detectorPanel = null;
        this.decoder = null;
	
	if(runNumber > 750){
	    this.nGain    = gainNew;
	    this.nGain_mV = gainNew_mV;
	}
	else{
	    this.nGain    = gainOld;
	    this.nGain_mV = gainOld_mV;
	}
	
    }
    
    public void initDetector(){

        DetectorShapeView2D viewChannels = this.drawChannels(0.0, 0.0);
        this.view.addDetectorLayer(viewChannels);

        DetectorShapeView2D viewFTHODO = this.drawDetector(0.0, 0.0);
        this.view.addDetectorLayer(viewFTHODO);
        //!!!
        //viewFTHODO.addActionListener(this);
        
//         DetectorShapeView2D viewPaddles = this.drawPaddles(0.0, 0.0);
//         this.view.addDetectorLayer(viewPaddles);
        

        // required to view plots
        view.addDetectorListener(this);
    }
    
    public EventDecoder getDecoder() {
        return decoder;
    }
    
    public void setDecoder(EventDecoder decoder) {
        this.decoder = decoder; // decoder sent from FTViewerModule
    }
    
    public JPanel getDetectorPanel() {
        return detectorPanel;
    }
    
        // argument sent from FTViewerModule
    public void setDetectorPanel(JPanel detectorPanel) {
        this.detectorPanel = detectorPanel;
    }
    public int getMezz4SLC(int isec,
			   int ilay,
			   int icomp){
	// FT-Cal
        if(ilay==0 || 
	   (ilay > 0 && 
	    (isec==0 || icomp==0))
	   )
            return -1;
	
	//System.out.println("s,l,c = " + isec + ", " + ilay + ", " + icomp);
	    
	int[][][] Mezz = {
	    //Layer 1
            {{3,3,3,4,1,2,4,6,6,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, //sec1
	     {1,2,13,15,2,4,12,13,3,5,11,12,6,6,5,5,10,10,10,10}, //sec2
	     {13,13,13,12,11,12,14,9,9,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, //sec3
	     {15,14,15,13,14,13,14,12,10,10,14,11,9,8,8,8,8,8,8,10}, //sec4
	     {11,2,11,13,12,12,12,11,10,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, //sec5
	     {1,9,7,14,14,9,7,2,1,9,5,15,9,9,11,11,5,5,7,7}, //sec6
	     {4,3,4,3,4,14,3,5,6,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, //sec7
	     {3,1,1,1,4,2,2,1,4,2,6,5,6,7,7,7,7,8,8,6}}, //sec8
	    //Layer 2
            {{3,3,3,4,1,2,4,6,6,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, //sec1
	     {1,2,13,15,2,4,12,14,3,5,11,12,6,6,5,5,10,10,10,10}, //sec2
	     {13,13,13,12,11,12,14,9,9,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, //sec3
	     {15,14,15,13,14,13,14,12,10,10,14,11,9,8,8,8,8,8,8,10}, //sec4
	     {11,2,11,13,12,12,12,11,10,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, //sec5
	     {1,9,7,14,13,9,7,2,1,9,5,15,9,9,11,11,5,5,7,7}, //sec6
	     {4,3,4,3,4,14,3,5,6,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, //sec7
	     {3,1,1,1,4,2,2,1,4,2,6,5,6,7,7,7,7,8,8,6}} //sec8
        };
        
        int mezzanine = Mezz[ilay-1][isec-1][icomp-1];
        // convention to agree with controller electronics [0,14]
	return (mezzanine-1);
    }

    public int getChan4SLC(int isec,
                           int ilay,
                           int icomp){
        
            // FT-Cal
        if(ilay==0 || 
	   (ilay > 0 && 
	    (isec==0 || icomp==0))
	   )
           
	   return -1;
        
        int[][][] chanM = {
                //Layer 1
            {{7,4,6,4,1,5,6,2,4,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, //sec1
                {0,6,0,1,7,7,0,1,5,7,0,1,7,6,5,6,3,4,7,6},//sec2
                {4,2,3,3,1,2,6,5,7,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1},//sec3
                {3,2,4,5,5,6,4,4,2,5,7,2,6,3,2,4,5,7,6,1},//sec4
                {6,0,7,7,6,5,7,5,0,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1},//sec5
                {6,3,7,1,0,4,2,1,7,2,0,6,0,1,4,3,1,2,4,3},//sec6
                {2,2,1,0,0,3,1,3,1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1},//sec7
                {3,4,5,3,3,3,2,2,5,4,5,4,0,5,6,0,1,1,0,3}},//sec8
                                                           //Layer 2
            {{13,12,14,9,15,12,11,10,8,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1},//sec1
                {14,15,8,9,14,8,9,14,15,10,8,8,11,12,8,9,9,8,12,11},//sec2
                {10,11,9,10,9,11,15,8,10,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1},//sec3
                {11,11,12,13,12,12,10,13,10,13,13,10,9,11,10,13,12,15,14,14},//sec4
                {14,9,15,14,15,12,14,13,15,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1},//sec5
                {8,15,15,8,15,12,10,8,9,11,15,14,14,13,11,12,13,14,13,14},//sec6
                {15,11,14,9,13,9,8,12,13,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1},//sec7
                {10,10,11,13,10,10,11,12,12,13,15,11,14,12,11,9,8,8,9,9}}//sec8
        };
        
        int chan = chanM[ilay-1][isec-1][icomp-1];
        return chan;
        
    }
    
    public int getComp4ChMez(int chan,
                             int mezz){
	if( chan > 15 ||
	    mezz > 14)
	    return -1;
                
        int[][] compM = {{1 ,5 ,8 ,4 ,2 ,3 ,1 ,9 ,1 ,9 ,2 ,3 ,8 ,4 ,1 ,5 },
			 {2 ,8 ,7 ,6 ,10,6 ,2 ,5 ,8 ,2 ,6 ,7 ,6 ,10,5 ,2 },
			 {4 ,7 ,2 ,1 ,2 ,9 ,3 ,1 ,7 ,4 ,1 ,2 ,2 ,1 ,3 ,9 },
			 {5 ,3 ,1 ,5 ,4 ,9 ,7 ,6 ,6 ,4 ,5 ,7 ,9 ,5 ,3 ,1 },
			 {11,17,18,8 ,12,15,16,10,15,16,10,12,8 ,17,18,11},
			 {13,9 ,8 ,20,9 ,11,14,13,9 ,20,8 ,13,14,9 ,13,11},
			 {16,17,7 ,20,19,14,15,3 ,17,16,7 ,15,14,19,20,3 },
			 {19,18,15,14,16,17,19,18,18,19,15,14,17,16,19,18},
			 {13,14,10,2 ,6 ,8 ,13,9 ,8 ,13,9 ,10,6 ,14,13,2 },
			 {9 ,20,9 ,17,18,10,20,19,18,17,9 ,20,19,10,20,9 },
			 {11,5 ,12,16,15,8 ,1 ,3 ,11,5 ,12,15,16,8 ,1 ,3 },
			 {7 ,12,6 ,4 ,8 ,6 ,5 ,7 ,12,7 ,4 ,6 ,6 ,8 ,7 ,5 },
			 {3 ,8, 2 ,3 ,1 ,4 ,6 ,4 ,3 ,3 ,1 ,2 ,6 ,4 ,4 ,5 },
			 {5 ,4, 2 ,6 ,7 ,5 ,7 ,11,4 ,6 ,7 ,2 ,5 ,11,8 ,7 },
			 {0 ,4, 0 ,1 ,3 ,0 ,12,0 ,0 ,4 ,0 ,1 ,3 ,0 ,12,0 }};
        
        int comp = compM[mezz][chan];
        
        return comp;
        
    }
    
    public int getSect4ChMez(int chan,
                             int mezz){

	if( chan > 15 ||
	    mezz > 14)
	    return -1;
        
        int[][] sectM = {{2,1,8,8,8,8,6,6,6,6,8,8,8,8,2,1},
			 {5,6,8,8,8,1,2,2,6,5,8,8,1,8,2,2},
			 {7,7,7,8,1,2,1,1,7,7,8,7,1,1,1,2},
			 {7,7,7,8,1,8,1,2,2,1,8,1,8,7,7,7},
			 {6,6,6,7,8,2,2,2,2,2,2,8,7,6,6,6},
			 {8,7,1,8,1,8,2,2,1,8,1,2,2,7,8,8},
			 {8,8,6,6,6,8,8,6,8,8,6,8,8,6,6,6},
			 {8,8,4,4,4,4,4,4,8,8,4,4,4,4,4,4},
			 {6,6,6,6,6,3,4,3,3,4,3,6,6,6,6,6},
			 {5,4,4,2,2,4,2,2,2,2,4,2,2,4,4,5},
			 {2,3,4,6,6,5,5,5,2,3,4,6,6,5,5,5},
			 {2,2,3,3,4,5,5,5,2,2,3,3,5,4,5,5},
			 {2,2,3,3,3,4,4,5,2,3,3,3,4,4,5,6},
			 {6,6,4,7,4,4,3,4,6,7,4,4,4,4,2,3},
			 {0,2,0,4,4,0,6,0,0,2,0,4,4,0,6,0}};
        
        int sect = sectM[mezz][chan];
        return sect;
        
    }
    
    public boolean channelIsEmpty(int channel){
        
        boolean empty = false;
        
        int emptyChannels[] = {0,2,5,7,8,10,13,15};
        
        for(int i = 0 ; i < emptyChannels.length ; i++){
            
            if ( channel == emptyChannels[i] ) {
                empty = true;
                break;
            }
        }
        return empty;
    }
    
    public DetectorShapeView2D drawChannels(double x0, double y0) {
        DetectorShapeView2D viewChannels = new DetectorShapeView2D("Electronics");
        
        int nChannels = 16;
        int nMezz = 15;
        
        int sec;
        int com;
        int lay;
        
        int width = 10;
        
        for(int iMez = 0 ; iMez < nMezz; iMez++){
            lay = 1;
            for(int iCh = 0; iCh < nChannels; iCh++) {
                
                if( iCh > 7 ) lay = 2;
                
                if( iMez == 14 && channelIsEmpty(iCh)) continue;
                
                com = getComp4ChMez(iCh,iMez);
                sec = getSect4ChMez(iCh,iMez);
                
                DetectorShape2D channel = new DetectorShape2D(DetectorType.FTHODO,
                                                              sec,lay,com);
                
                channel.createBarXY(width,width);
                
                channel.getShapePath().translateXYZ( 2*(iMez-7)*width,
						     (width*iCh)+width*(lay-1),
						     0.0);
		//viewChannels.setColor(0, 145, 0, 0);
                viewChannels.addShape(channel);
            }
        }
        
        return viewChannels;
    };
    
    public DetectorShapeView2D drawPaddles(double x0, double y0) {
        DetectorShapeView2D viewPaddles = new DetectorShapeView2D("FTPADDLES");
        
        int nPaddles = 4;
        
        for(int ipaddle=0; ipaddle < nPaddles; ipaddle++) {
            DetectorShape2D paddle = new DetectorShape2D(DetectorType.FTCAL,
                                                         0, 0, 501 + ipaddle );
            paddle.createBarXY(tile_size*11, tile_size/2.);
            
            paddle.getShapePath().translateXYZ(tile_size*11/2.*(((int) ipaddle/2)*2+1),
                                               tile_size*(22+2)*(ipaddle % 2),
                                               0.0);
                //paddle.setColor(0, 145, 0, 0);
            viewPaddles.addShape(paddle);
        }
        
        return viewPaddles;
    };
    
    
    public DetectorShapeView2D drawDetector(double x0, double y0) {
        DetectorShapeView2D viewFTHODO = new DetectorShapeView2D("Detector");
        
		
            // sectors 1-8 for each layer.
            // detector symmetry is fourfold
            // with elements 0-28 for each quarter.
        int sector;
        
            // tile component
            // 1-9 for odd sectors
            // 1-20 for even
        int component;
        
            // thick and thin
        int layer;
        
            // y-offset to place thin and thick layer on same pane
        double[] layerOffsetY = {-200.0,200.0};
            // size of tiles per quadrant
        double[] tileSize = {15.0,30.0,15.0,30.0,30.0,30.0,30.0,30.0,15.0,
			     30.0,30.0,30.0,30.0,30.0,30.0,30.0,30.0,30.0,30.0,
			     30.0,30.0,15.0,15.0,15.0,15.0,15.0,15.0,15.0,15.0};
        
        double[] tileThickness = {7., 15.};
        
            //============================================================
        double[] xx = {-97.5 ,  -75.0, -127.5, -105.0, -75.0,
            -135.0, -105.0,  -75.0,  -52.5,
            -45.0 ,  -15.0,   15.0,   45.0, -45.0,
            -15.0 ,   15.0,   45.0,  -45.0, -15.0,
            15.0  ,   45.0,  -52.5,  -37.5, -22.5,
            -7.5  ,    7.5,   22.5,   37.5,  52.5};
        
        double[] yy = {-127.5, -135.0,  -97.5, -105.0, -105.0,
            -75.0 ,  -75.0,  -75.0,  -52.5,
            -150.0, -150.0, -150.0, -150.0, -120.0,
            -120.0, -120.0, -120.0,  -90.0,  -90.0,
            -90.0 ,  -90.0,  -67.5,  -67.5,  -67.5,
            -67.5 ,  -67.5,  -67.5,  -67.5,  -67.5};
            //============================================================
        
        double xcenter = 0;
        double ycenter = 0;
        double zcenter;
        
            // two layers: I==0 for thin and I==1 for thick
        for (int layerI = 0; layerI < 2; layerI++){
            layer = layerI+1;
            
                // 4 symmetry sectors per layer (named quadrant) from 0-3
            for (int quadrant=0; quadrant < 4; quadrant++) {
                
                    // 29 elements per symmetry sector
                for (int element = 0; element < 29; element++) {
                    
                        // sector is odd for first 9 elements
                        // and even for the rest
                    if (element < 9) {
                        sector = quadrant*2 + 1;
                            // component number for odd sector is 1-9
                        component = element + 1;
                    }
                    else  {
                        sector = quadrant*2 + 2;
                            // component number for even sector is 1-20
                        component = element + 1 - 9;
                    }
                    
                    // calculate the x-element of the center of each tile;
                    switch (quadrant) {
                        case 0:
                            xcenter = xx[element];
                            break;
                        case 1:
                            xcenter =-yy[element];
                            break;
                        case 2:
                            xcenter =-xx[element];
                            break;
                        case 3:
                            xcenter = yy[element];
                            break;
                        default:
                            break;
                    }
                    
                    // calculate the y-element of the center of each tile
                    switch (quadrant) {
                        case 0:
                            ycenter = yy[element] + layerOffsetY[layerI];
                            break;
                        case 1:
                            ycenter = xx[element] + layerOffsetY[layerI];
                            break;
                        case 2:
                            ycenter =-yy[element] + layerOffsetY[layerI];
                            break;
                        case 3:
                            ycenter =-xx[element] + layerOffsetY[layerI];
                            break;
                        default:
                            break;
                    }
                    
                    if(layerI==0){
                        zcenter = -tileThickness[layerI]/2.0;
                    }
                    else
                        zcenter =  tileThickness[layerI]/2.0;
                    
                        // Sectors 1-8
                        // (sect=1: upper left - clockwise);
                        // layers 1-2 (thin==1, thick==2);
                        // tiles (1-9 for odd and 1-20 for even sectors)
                    DetectorShape2D shape  = new DetectorShape2D(DetectorType.FTHODO,
                                                                 sector,
                                                                 layer,
                                                                 component);
                    
                    DetectorShape2D shape2 = new DetectorShape2D(DetectorType.FTHODO,
                                                                 sector,
                                                                 layer,
                                                                 component);
                    
                        // defines the 2D bars dimensions using the element size
                    shape.createBarXY(tileSize[element], tileSize[element]);
                    
                    shape2.createBarXY(tileSize[element],tileThickness[layerI]);
                    
                        // defines the placements of the 2D bar according to the
                        // xcenter and ycenter calculated above
                    shape.getShapePath().translateXYZ(xcenter,ycenter,zcenter);
                    
                        //
                    shape.setColor(0, 0, 0, 0);
                    
                    viewFTHODO.addShape(shape);
                    
                    //===========================================================
                    // calculate the y-element of the center of each tile
                    switch (quadrant) {
                        case 0:
                            ycenter = yy[element];
                            break;
                        case 1:
                            ycenter = xx[element];
                            break;
                        case 2:
                            ycenter =-yy[element];
                            break;
                        case 3:
                            ycenter =-xx[element];
                            break;
                        default:
                            break;
                    }
                    
                    shape2.setColor(0, 0, 0, 0);
                    
                    shape2.getShapePath().translateXYZ(xcenter, zcenter, 0);
                    
                    viewFTHODO.addShape(shape2);
                    
                }
            }
        }
        
        return viewFTHODO;
    }
    
    public void actionPerformed(ActionEvent e) {
        
	System.out.println("ACTION = " + e.getActionCommand());
        
        if (e.getActionCommand().compareTo("Reset") == 0) {
            resetHistograms();
        }
        if (e.getActionCommand().compareTo("Variables") == 0) {
            updateVariables();
        }
        if (e.getActionCommand().compareTo("Fit") == 0) {
            fitHistograms();
        }
	if (e.getActionCommand().compareTo("Peak") == 0) {
	    this.useGain_mV = true;
	}
	if (e.getActionCommand().compareTo("Charge") == 0) {
	    this.useGain_mV = false;
	}
	if (e.getActionCommand().compareTo("Electronics") == 0) {
	    this.drawByElec = true;
	}
	if (e.getActionCommand().compareTo("Detector") == 0) {
	    this.drawByElec = false;
	}
	
    }
    
    private void fitHistograms() {
        
        int    binNmax = 0;
        double maxCont = 0;
	
	String fitOption = "NRQ";
        
        HistPara HP =  new HistPara();
        
	// Do the fitting for all components
        for (int index = 0; index < 232; index++) {
            
            HP.setAllParameters(index,'h');
	    
	    initFitParams(HP.getS(), HP.getL(), HP.getC(), 
			  H_PED.get(HP.getS(), HP.getL(), HP.getC()),
			  H_NOISE_Q.get(HP.getS(), HP.getL(), HP.getC()),
			  H_MIP_Q.get(HP.getS(), HP.getL(), HP.getC()),
			  H_MAXV.get(HP.getS(), HP.getL(), HP.getC()),
			  H_MIP_V.get(HP.getS(), HP.getL(), HP.getC()));
	    
	    // fit pedestal
            if (fPed.hasEntry(HP.getS(),HP.getL(),HP.getC())){
		
                H_PED.get(HP.getS(), HP.getL(), HP.getC()).
		    fit(fPed.get(HP.getS(), HP.getL(), HP.getC()),
			fitOption);
                
		if (testMode){
		    System.out.println("Fitted Noise1 Index = " + index +
				       " Sector =" + HP.getS() +
				       " Layer = " + HP.getL() +
				       " Component="+ HP.getC());
		}
            }

	    // fit first charge peak
            if (fQ1.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                
                H_NOISE_Q.get(HP.getS(), HP.getL(), HP.getC()).
		    fit(fQ1.get(HP.getS(), HP.getL(), HP.getC()),fitOption);
                
		if (testMode){
		    System.out.println("Fitted Noise1 Index = " + index +
				       " Sector =" + HP.getS() +
				       " Layer = " + HP.getL() +
				       " Component="+ HP.getC());
		}
            }
	    
	    // fit second charge peak
            if (fQ2.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                
                H_NOISE_Q.get(HP.getS(), HP.getL(), HP.getC()).
		    fit(fQ2.get(HP.getS(), HP.getL(), HP.getC()),fitOption);
		
		if (testMode){
		    System.out.println("Fitted Noise Index = " + index +
				       " Sector = " + HP.getS() +
				       " Layer = " + HP.getL() +
				       " Component="+ HP.getC());
		}
            }

            if(fQMIP.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                
                H_MIP_Q.get(HP.getS(), HP.getL(), HP.getC()).
                fit(fQMIP.get(HP.getS(), HP.getL(), HP.getC()),fitOption);
                
		if (testMode){
		    System.out.println("Fitted Cosmic Index = " + index +
				       " Sector = " + HP.getS() +
				       " Layer = " + HP.getL() +
				       " Component = " + HP.getC());
		}
            }
	    
	    if(fVMIP.hasEntry(HP.getS(), HP.getL(), HP.getC())){
		
                H_MIP_V.get(HP.getS(), HP.getL(), HP.getC()).
		    fit(fVMIP.get(HP.getS(), HP.getL(), HP.getC()),fitOption);
                
		if (testMode){
		    System.out.println("Fitted Cosmic Index = " + index +
				       " Sector = " + HP.getS() +
				       " Layer = " + HP.getL() +
				       " Component = " + HP.getC());
		}
            }
	    
            if(fV1.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                
                H_MAXV.get(HP.getS(), HP.getL(), HP.getC()).
                fit(fV1.get(HP.getS(), HP.getL(), HP.getC()),fitOption);
                
		if (testMode){
		    System.out.println("Fitted Cosmic Index = " + index +
				       " Sector = " + HP.getS()  +
				       " Layer = " + HP.getL()   +
				       " Component = " + HP.getC());
		}
            }
            
	    if(fV2.hasEntry(HP.getS(), HP.getL(), HP.getC())){
		
		if(!H_MAXV.hasEntry(HP.getS(), HP.getL(), HP.getC())){
		
		    System.out.println(" No H_MAXV entry " + index +
				       " Sector = " + HP.getS() +
				       " Layer = " + HP.getL() +
				       " Component = " + HP.getC());
		}
		    
                H_MAXV.get(HP.getS(), HP.getL(), HP.getC()).
		    fit(fV2.get(HP.getS(), HP.getL(), HP.getC()),fitOption);
                
		if (testMode){
		    System.out.println("Fitted Cosmic Index = " + index +
				       " Sector = " + HP.getS() +
				       " Layer = " + HP.getL() +
				       " Component = " + HP.getC());
		}
            }
            
        } // end of : for (int index = 0; index < 232; index++) {

	boolean flag_parnames = false;
        
	System.out.println(" Printing Fit Results: ");
	// Print out fit results
        for(int index = 0; index < 232; index++) {
            
            HP.setAllParameters(index,'h');
            
            if(fPed.hasEntry(HP.getS(), HP.getL(), HP.getC())   &&
	       fQ1.hasEntry(HP.getS(), HP.getL(), HP.getC())   &&
               fQ2.hasEntry(HP.getS(), HP.getL(), HP.getC())   &&
               fV1.hasEntry(HP.getS(), HP.getL(), HP.getC())   &&
               fV2.hasEntry(HP.getS(), HP.getL(), HP.getC())   &&
               fQMIP.hasEntry(HP.getS(), HP.getL(), HP.getC()) &&
               fVMIP.hasEntry(HP.getS(), HP.getL(), HP.getC())) 
            {
                
                if(flag_parnames) {
                    System.out.println("Ind\t Sec\t Lay\t Comp\t " +
                                       "amp\t mean\t sigma\t " +
                                       "amp\t mean\t sigma\t " +
                                       "amp\t mean\t sigma");
                    
                    flag_parnames=false;
                }
                
                System.out.print(index     + "\t " +
                                 HP.getS() + "\t " +
                                 HP.getL() + "\t " +
                                 HP.getC() + "\t ");
                
                if(fQ1.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                    
                    for(int i = 2; 
			i<fQ1.get(HP.getS(), 
				  HP.getL(), 
				  HP.getC()).getNParams(); i++)
                        System.out.format("%.2f\t ",
                                          fQ1.get(HP.getS(), 
						  HP.getL(), 
						  HP.getC()).
					  getParameter(i));
                }
                else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
                
                if(fQ2.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                    
                    for(int i=0; i<fQ2.get(HP.getS(), HP.getL(), HP.getC()).getNParams(); i++)
                        System.out.format("%.2f\t ",
                                          fQ2.get(HP.getS(), HP.getL(), HP.getC()).getParameter(i));
                }
                else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
                
                if(fQMIP.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                    for(int i=0; i<fQMIP.get(HP.getS(), HP.getL(), HP.getC()).getNParams(); i++)
                        System.out.format("%.2f\t ",fQMIP.get(HP.getS(), HP.getL(), HP.getC()).getParameter(i));
                }
                else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
                
                if(fVMIP.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                    for(int i=0; i<fVMIP.get(HP.getS(), HP.getL(), HP.getC()).getNParams(); i++)
                        System.out.format("%.2f\t ",fVMIP.get(HP.getS(), HP.getL(), HP.getC()).getParameter(i));
                }
                else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
                
                if(fV1.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                    for(int i=0; i<fV1.get(HP.getS(), HP.getL(), HP.getC()).getNParams(); i++)
                        System.out.format("%.2f\t ",fV1.get(HP.getS(), HP.getL(), HP.getC()).getParameter(i));
                }
                else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
                
                if(fV2.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                    for(int i=0; i<fV2.get(HP.getS(), HP.getL(), HP.getC()).getNParams(); i++)
                        System.out.format("%.2f\t ",fV2.get(HP.getS(), HP.getL(), HP.getC()).getParameter(i));
                }
                else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
                
                System.out.format("\n");
            }
        } // end of: for(int index = 0; index < 23
	
	System.out.println(" Fitting Histograms complete");
    }
    
    private void initFitParams(int sec,
			       int lay,
			       int com,
			       H1D H_P,
			       H1D H_QLow,
			       H1D H_Q,
			       H1D H_VLow,
			       H1D H_V) {
        
        double ampl = H_QLow.getBinContent(H_QLow.getMaximumBin());
        
	double mean = H_QLow.getMaximumBin();
	mean = mean * H_QLow.getAxis().getBinWidth(2);
	mean = mean + H_QLow.getAxis().min();
        
	double std  = 5.0;
        
	// create first charge fit function if 
	// sufficient statistics exist then
	// initialise parameters and limits
        if (H_QLow.getEntries() > 250){
	    
            fQ1.add(sec, lay, com, 
			 new F1D("exp+gaus", 
				 H_QLow.getAxis().min(),  
				 nGain*1.5));
	    
	    // exponential
	    fQ1.get(sec, lay, com).setParameter(0, ampl/5.);
            fQ1.get(sec, lay, com).setParameter(1, -0.001);
        
	    // gaus 1
	    fQ1.get(sec, lay, com).setParameter(2, ampl);
            fQ1.get(sec, lay, com).setParameter(3, nGain);
            fQ1.get(sec, lay, com).setParameter(4, std);
            
	    // exponential
	    fQ1.get(sec, lay, com).setParLimits(0, ampl/10.0, ampl);
            fQ1.get(sec, lay, com).setParLimits(1, -5, -0.0001);
            
	    // gaus 1
	    fQ1.get(sec, lay, com).setParLimits(2, ampl/2, ampl*2);
            fQ1.get(sec, lay, com).setParLimits(3, 0.5*nGain, 1.5*nGain);
            fQ1.get(sec, lay, com).setParLimits(4, 1, std*3.0);
        
	    // create second charge fit function and
	    // initialise parameters and limits
	    
	    fQ2.add(sec, lay, com, 
		    new F1D("gaus", 1.5*nGain, 2.5*nGain));
	    
	    fQ2.get(sec, lay, com).setParameter(0, ampl/5.0);
	    fQ2.get(sec, lay, com).setParameter(1, 2.0*nGain);
	    fQ2.get(sec, lay, com).setParameter(2, std);
            
	    fQ2.get(sec, lay, com).setParLimits(0, 0, ampl/2.0);
	    fQ2.get(sec, lay, com).setParLimits(1, mean+20, mean+100);
	    fQ2.get(sec, lay, com).setParLimits(2, 1, std*3.0);
        }
        
        ampl = H_VLow.getBinContent(H_VLow.getMaximumBin());
	
        mean = H_VLow.getMaximumBin();
	mean = mean * H_VLow.getAxis().getBinWidth(2);
	mean = mean + H_VLow.getAxis().min();
	
	std  = 0.5;
	
        if (H_VLow.getEntries() > 250){
        
	    // fit function for first peak
            fV1.add(sec, lay, com, new F1D("gaus+exp", 
					   H_VLow.getAxis().min(), 
					   1.5*nGain_mV));
            
	    fV1.get(sec, lay, com).setParameter(0, ampl);
            fV1.get(sec, lay, com).setParameter(1, nGain_mV);
            fV1.get(sec, lay, com).setParameter(2, std);
            fV1.get(sec, lay, com).setParameter(3, ampl/5.0);
            fV1.get(sec, lay, com).setParameter(4, -0.001);
            
            fV1.get(sec, lay, com).setParLimits(0, ampl/2, ampl*2);
            fV1.get(sec, lay, com).setParLimits(1, 
						H_VLow.getAxis().min(), 
						2*nGain_mV);
            fV1.get(sec, lay, com).setParLimits(2, 0, std*4.0);
            fV1.get(sec, lay, com).setParLimits(3, 0, ampl*5.);
            fV1.get(sec, lay, com).setParLimits(4, -5, -0.0001);
            
	    if(testMode){
		System.out.println("H_VLow.getAxis()." +
				   "getBin(3.0/2.0*mean) = " + 
				   H_VLow.getAxis().
				   getBin(3.0/2.0*mean) );
		
		System.out.println("H_VLow.getAxis()." +
				   "getNBins()-1) = " +
				   H_VLow.getAxis().
				   getNBins());
		
		System.out.println("mean = " + mean );
	    }
	    
	    // fit function for second peak
	    fV2.add(sec, lay, com, new F1D("gaus",
					   1.5*nGain_mV, 
					   3.0*nGain_mV));
	    
	    fV2.get(sec, lay, com).
		setParameter(0, ampl/3.0);
	    fV2.get(sec, lay, com).
		setParameter(1, 2.0*nGain_mV);
	    fV2.get(sec, lay, com).
		setParameter(2, std);
	    
	    fV2.get(sec, lay, com).
		setParLimits(0, 0, ampl);
	    fV2.get(sec, lay, com).
		setParLimits(1,
			     1.5*nGain_mV,
			     2.5*nGain_mV);
	    fV2.get(sec, lay, com).
		setParLimits(2, 0., std*4.0);
	    
        }
        
        int integralLowBin  = (500 - (int)CosmicQXMin[lay])*NBinsCosmic;
        integralLowBin  = integralLowBin/((int)CosmicQXMax[lay]-(int)CosmicQXMin[lay]);
        
        int integralHighBin = NBinsCosmic-1;
        
        if (H_Q.integral(integralLowBin,
			 integralHighBin ) > 25){
            ampl = 0;
            mean = 0;
	    
	    for (int i = integralLowBin; 
		 i < integralHighBin; 
		 i++){
		
		if (H_Q.getBinContent(i) > ampl){
                    ampl = H_Q.getBinContent(i);
                    mean = i * (CosmicQXMax[lay] - CosmicQXMin[lay]);
                    mean = mean/NBinsCosmic + CosmicQXMin[lay];
                }
            }
	    
            fQMIP.add(sec, lay, com, new F1D("landau", 500, 4500));
            fQMIP.get(sec, lay, com).setParameter(0, ampl);
            fQMIP.get(sec, lay, com).setParameter(1, mean);
            fQMIP.get(sec, lay, com).setParameter(2, 150);
            fQMIP.get(sec, lay, com).setParLimits(0, 0, ampl*2.0);
            fQMIP.get(sec, lay, com).setParLimits(1, mean-400, mean+400);
            fQMIP.get(sec, lay, com).setParLimits(2, 50, 1500);
        }
        
	// fitting of charge [mV] histograms
	ampl = H_V.getBinContent(H_V.getMaximumBin());
	//mean = H_V.getMaximumBin()*45 + 250;
	mean = 600.;
	std  = 5.0;
	
	if (H_V.integral() > 10 ){
	    
	    fVMIP.add(sec, lay, com, new F1D("landau", 250, 1500));
		
	    fVMIP.get(sec, lay, com).setParameter(0, ampl);
	    fVMIP.get(sec, lay, com).setParameter(1, mean);
	    fVMIP.get(sec, lay, com).setParameter(2, 200);
	    
	    // fVMIP.get(sec, lay, com).setParameter(0, 0);
	    // fVMIP.get(sec, lay, com).setParameter(1, 0);
	    // fVMIP.get(sec, lay, com).setParameter(2, 0);
	    
 	    fVMIP.get(sec, lay, com).setParLimits(0, ampl*0.5, ampl*2.5);
	    fVMIP.get(sec, lay, com).setParLimits(1, 0, 1500);
	    fVMIP.get(sec, lay, com).setParLimits(2, 50, 100);
	}
            
	// fitting of pedestal
	ampl = H_P.getBinContent(H_P.getMaximumBin());
	mean = H_P.getMean();
	std  = H_P.getRMS();
	
	if (H_P.integral() > 100 ){
	    
	    fPed.add(sec, lay, com, new F1D("gaus"));
	    
	    fPed.get(sec, lay, com).setParameter(0, ampl);
	    fPed.get(sec, lay, com).setParameter(1, mean);
	    fPed.get(sec, lay, com).setParameter(2, std);
	    
 	    // fVMIP.get(sec, lay, com).setParLimits(0, ampl*0.5, ampl*2.5);
	    // fVMIP.get(sec, lay, com).setParLimits(1, 0, mean+600);
	    // fVMIP.get(sec, lay, com).setParLimits(2, 50, 1500);
	}
        
    }
    
    
    
    void drawCanvasHODOEvent(boolean calSel,
			    int secSel,
			    int comSel,
			    int oppSel,
			    int layCD,
			    int oppCD){
        
        if(calSel){
            
            int nPaddles = 4;
            
            this.canvasHODOEvent.divide(nPaddles, 1);
            
            for (int ipaddle = 0  ; ipaddle < nPaddles ; ipaddle++){
                
                canvasHODOEvent.cd(ipaddle);
                
                if(H_FADC.hasEntry(secSel,laySel,501+ipaddle))
                    
                    this.canvasHODOEvent.draw(H_FADC.get(secSel,
                                                         laySel,
                                                         501+ipaddle));
            }
            
        }
        else{
            
            this.canvasHODOEvent.divide(2, 1);
            
            canvasHODOEvent.cd(layCD);
            if(H_FADC.hasEntry(secSel,laySel,comSel))
                this.canvasHODOEvent.draw(H_FADC.get(secSel,
                                                     laySel,
                                                     comSel));
            
            canvasHODOEvent.cd(oppCD);
            if(H_FADC.hasEntry(secSel,oppSel,comSel))
                this.canvasHODOEvent.draw(H_FADC.get(secSel,
                                                     oppSel,
                                                     comSel));
        }
    }
   
    
    void drawCanvasEvent(int secSel,
                         int laySel,
                         int comSel){
        
        
        
	// map [1,2] to [0,1]
        int layCD  = laySel-1;
	// map [1,2] to [1,0]
        int oppCD  = laySel%2;
	// map [1,0] to [2,1]
        int oppSel = oppCD+1;
        
	// map [0,1] to [0,3]
        int layCDL =  3*layCD;
	// map [1,0] to [3,0]
        int oppCDL =  3*oppCD;
        
	// map [0,3] to [1,4]
        int layCDM = layCDL + 1;
	// map [3,0] to [4,1]
        int oppCDM = oppCDL + 1;
        
	// map [0,3] to [2,5]
        int layCDR = layCDL + 2;
	// map [3,0] to [5,2]
        int oppCDR = oppCDL + 2;
        
	// // paddles
	// if(laySel==0){
	//     layCD  = 0;
	//     oppCD  = 1;
	//     oppSel = 0;
	// }
        
	// if     (component==501)
	//     layerStr = "Top Long Paddle";
	// else if(component==502)
	//             layerStr = "Bottom Long Paddle";
	// else if(component==503)
	//     layerStr = "Top Short Paddle";
	// else if(component==504)
	//     layerStr = "Bottom Short Paddle";
        
        
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	// raw fADC pulse
        canvasEvent.cd(layCDL);
        
        if(H_FADC.hasEntry(secSel,laySel,comSel)){
            this.canvasEvent.draw(H_FADC.get(secSel,
                                             laySel,
                                             comSel));
            
        }
            //----------------------------------------
            // left top (bottom) for thin (thick) layer
            // raw fADC pulse
        canvasEvent.cd(oppCDL);
        
        if(H_FADC.hasEntry(secSel,
                           oppSel,
                           comSel))
            this.canvasEvent.draw(H_FADC.get(secSel,
                                             oppSel,
                                             comSel));
        
            //----------------------------------------
            // middle top (bottom) for thin (thick) layer
            // calibrated fADC pulse
        canvasEvent.cd(layCDM);
        
        if(H_VT.hasEntry(secSel,
                            laySel,
                            comSel))
            this.canvasEvent.draw(H_VT.get(secSel,
                                              laySel,
                                              comSel));
        
            //----------------------------------------
            // right top (bottom) for thin (thick) layer
            // calibrated fADC pulse
        canvasEvent.cd(oppCDM);
        if(H_VT.hasEntry(secSel,
                            oppSel,
                            comSel))
            this.canvasEvent.draw(H_VT.get(secSel,
                                              oppSel,
                                              comSel));
            //----------------------------------------
            // right top (bottom) for thin (thick) layer
            // calibrated fADC pulse
        canvasEvent.cd(layCDR);
        
        if(H_NPE.hasEntry(secSel,
                          laySel,
                          comSel))
            this.canvasEvent.draw(H_NPE.get(secSel,
                                            laySel,
                                            comSel));
        
            //----------------------------------------
            // right top (bottom) for thin (thick) layer
            // calibrated fADC pulse
        canvasEvent.cd(oppCDR);
        if(H_NPE.hasEntry(secSel,
                          oppSel,
                          comSel))
            this.canvasEvent.draw(H_NPE.get(secSel,
                                            oppSel,
                                            comSel));
    }
    
    void drawCanvasPed(int secSel,
		       int laySel,
		       int comSel){
	
	// map [1,2] to [0,1]
        int layCD = laySel-1;
	// map [1,2] to [1,0]
        int oppCD = laySel%2;
	// map [1,2] to [2,1]
        int oppSel   = (laySel%2)+1;
	
	int layCDL =  2*layCD;
        int oppCDL =  2*oppCD;
        
        int layCDR = layCDL + 1;
        int oppCDR = oppCDL + 1;

	//----------------------------------------
	// left top (bottom) for thin (thick) layer
        canvasPed.cd(layCDL);
	
        if(H_PED.hasEntry(secSel,laySel,comSel)){
            
	    this.canvasPed.draw(H_PED.get(secSel,
					  laySel,
					  comSel));
            
	    if(fPed.hasEntry(secSel,
			     laySel,
			     comSel)){
		this.canvasPed.draw(fPed.get(secSel,
					     laySel,
					     comSel),"same S");
	    }
            
        }
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
        canvasPed.cd(oppCDL);
        
	if(H_PED.hasEntry(secSel,oppSel,comSel)){
	    
	    this.canvasPed.draw(H_PED.get(secSel,
					  oppSel,
					  comSel));
            
	    if(fPed.hasEntry(secSel,
			     oppSel,
			     comSel)){
                this.canvasPed.draw(fPed.get(secSel,
					     oppSel,
					     comSel),"same S");
		
	    }
	    
	}
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasPed.cd(layCDR);
        
	// GraphErrors
        
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasPed.cd(oppCDR);
    
	// GraphErrors
	
    }
    

    void drawCanvasNoise(int secSel,
                         int laySel,
                         int comSel){
        
	// map [1,2] to [0,1]
        int layCD  = laySel-1;
	// map [1,2] to [1,0]
        int oppCD  = laySel%2;
	// map [1,0] to [2,1]
        int oppSel = oppCD+1;
        
	// map [0,1] to [0,3]
        int layCDL =  3*layCD;
	// map [1,0] to [3,0]
        int oppCDL =  3*oppCD;
        
	// map [0,3] to [1,4]
        int layCDM = layCDL+1;
	// map [3,0] to [4,1]
        int oppCDM = oppCDL+1;
        
	// map [0,3] to [2,5]
        int layCDR = layCDL+2;
	// map [3,0] to [5,2]
        int oppCDR = oppCDL+2;
        
	String style = "S";
	
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	// calibrated fADC pulse
        canvasNoise.cd(layCDL);
        
        if(H_VT.hasEntry(secSel,laySel,comSel)){
            this.canvasNoise.draw(H_VT.get(secSel,
					   laySel,
					   comSel));
	    
        }
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	// calibrated fADC pulse
        canvasNoise.cd(oppCDL);
        
        if(H_VT.hasEntry(secSel,oppSel,comSel))
            this.canvasNoise.draw(H_VT.get(secSel,
					   oppSel,
					   comSel));
        
	//----------------------------------------
	// middle top (bottom) for thin (thick) layer
	// voltage maximum
        canvasNoise.cd(layCDM);
        
        if(H_MAXV.hasEntry(secSel,laySel,comSel)){
            this.canvasNoise.draw(H_MAXV.get(secSel,
                                             laySel,
                                             comSel));
            if(fV1.hasEntry(secSel,
			    laySel,
			    comSel))
                this.canvasNoise.draw(fV1.get(secSel,
					      laySel,
					      comSel),"same S");
            if(fV2.hasEntry(secSel,
			    laySel,
			    comSel))
                this.canvasNoise.draw(fV2.get(secSel,
					      laySel,
					      comSel),"same S");
	    
        }
        
	//----------------------------------------
	// middle top (bottom) for thin (thick) layer
	// calibrated fADC pulse
        
        canvasNoise.cd(oppCDM);
	
	if(H_MAXV.hasEntry(secSel,oppSel,comSel)){
            this.canvasNoise.draw(H_MAXV.get(secSel,
                                             oppSel,
                                             comSel));
            
	    if(fV1.hasEntry(secSel,
			    oppSel,
			    comSel))
                this.canvasNoise.draw(fV1.get(secSel,
					      oppSel,
					      comSel),"same S");
            
            if(fV2.hasEntry(secSel,
			    oppSel,
			    comSel))
                this.canvasNoise.draw(fV2.get(secSel,
					      oppSel,
					      comSel),"same S");
        }
        
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
	// accumulated noise charge
        canvasNoise.cd(layCDR);
        
        if(H_NOISE_Q.hasEntry(secSel,
                              laySel,
                              comSel))
            this.canvasNoise.draw(H_NOISE_Q.get(secSel,
                                                laySel,
                                                comSel));
        if(fQ1.hasEntry(secSel,
			laySel,
			comSel))
            this.canvasNoise.draw(fQ1.get(secSel,
					  laySel,
					  comSel),"same S");
        if(fQ2.hasEntry(secSel,
			laySel,
			comSel))
            this.canvasNoise.draw(fQ2.get(secSel,
					  laySel,
					  comSel),"same S");
        
        
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
	// calibrated fADC pulse
        canvasNoise.cd(oppCDR);
        
	if(H_NOISE_Q.hasEntry(secSel,
                              oppSel,
                              comSel))
            this.canvasNoise.draw(H_NOISE_Q.get(secSel,
                                                oppSel,
                                                comSel));
        if(fQ1.hasEntry(secSel,
			oppSel,
			comSel))
            this.canvasNoise.draw(fQ1.get(secSel,
					  oppSel,
					  comSel),"same S");
        if(fQ2.hasEntry(secSel,
			oppSel,
			comSel))
            this.canvasNoise.draw(fQ2.get(secSel,
					  oppSel,
					  comSel),"same S");
        
    }
    
    void drawCanvasTime(int secSel,
			int laySel,
			int comSel){
        
	// map [1,2] to [0,1]
        int layCD = laySel-1;
	// map [1,2] to [1,0]
        int oppCD = laySel%2;
	// map [1,2] to [2,1]
        int oppSel   = (laySel%2)+1;
	
	int layCDL =  2*layCD;
        int oppCDL =  2*oppCD;
        
        int layCDR = layCDL + 1;
        int oppCDR = oppCDL + 1;
        

	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	canvasTime.cd(layCDL);
	
	if(H_MAXV_VS_T.hasEntry(secSel,
				 laySel,
				 comSel) ){
	    
	    this.canvasTime.draw(H_MAXV_VS_T.get(secSel,
						 laySel,
						 comSel));
	    
	}

	//----------------------------------------
	// left top (bottom) for thin (thick) layer
        canvasTime.cd(oppCDL);
	
	if(H_MAXV_VS_T.hasEntry(secSel,
				oppSel,
				comSel) ){
	    
	    this.canvasTime.draw(H_MAXV_VS_T.get(secSel,
						 oppSel,
						 comSel));
	    
	}
	
	//----------------------------------------
	// right top 
        canvasTime.cd(1);
	
	if(H_DT_MODE1.hasEntry(secSel,
			       laySel,
			       comSel) ){
            this.canvasTime.draw(H_DT_MODE1.get(secSel,
						laySel,
						comSel));
	}
	else if(H_DT_MODE1.hasEntry(secSel,
				    oppSel,
				    comSel)){
            this.canvasTime.draw(H_DT_MODE1.get(secSel,
						oppSel,
						comSel));
	}
	
	
	//----------------------------------------
	// right bottom
        canvasTime.cd(3);
	
	if(H_T1_T2.hasEntry(secSel,
			       laySel,
			       comSel) ){
            this.canvasTime.draw(H_T1_T2.get(secSel,
						laySel,
						comSel));
	}
	else if(H_T1_T2.hasEntry(secSel,
				    oppSel,
				    comSel)){
            this.canvasTime.draw(H_T1_T2.get(secSel,
					     oppSel,
					     comSel));
	}
	

    
    }
    
    void drawCanvasCharge(int secSel,
                          int laySel,
                          int comSel){
	
	// map [1,2] to [0,1]
        int layCD = laySel-1;
	// map [1,2] to [1,0]
        int oppCD = laySel%2;
	// map [1,2] to [2,1]
        int oppSel   = (laySel%2)+1;
	
	int layCDL =  2*layCD;
        int oppCDL =  2*oppCD;
        
        int layCDR = layCDL + 1;
        int oppCDR = oppCDL + 1;
                
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
        canvasCharge.cd(layCDL);
	
        if(H_NOISE_Q.hasEntry(secSel,laySel,comSel)){
            this.canvasCharge.draw(H_NOISE_Q.get(secSel,
                                                 laySel,
                                                 comSel));
            if(fQ1.hasEntry(secSel,
                                 laySel,
                                 comSel))
                this.canvasCharge.draw(fQ1.get(secSel,
                                                    laySel,
                                                    comSel),"same S");
            if(fQ2.hasEntry(secSel,
                                 laySel,
                                 comSel))
                this.canvasCharge.draw(fQ2.get(secSel,
                                                    laySel,
                                                    comSel),"same S");
            
        }
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
        canvasCharge.cd(oppCDL);
        
        if(H_NOISE_Q.hasEntry(secSel,
                              oppSel,
                              comSel))
            this.canvasCharge.draw(H_NOISE_Q.get(secSel,
                                                 oppSel,
                                                 comSel));
        if(fQ1.hasEntry(secSel,
                             oppSel,
                             comSel))
            this.canvasCharge.draw(fQ1.get(secSel,
                                                oppSel,
                                                comSel),"same S");
        if(fQ2.hasEntry(secSel,
                             oppSel,
                             comSel))
            this.canvasCharge.draw(fQ2.get(secSel,
                                                oppSel,
                                                comSel),"same S");
        
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasCharge.cd(layCDR);
        
        if(H_MIP_Q.hasEntry(secSel,
                            laySel,
                            comSel)){
            this.canvasCharge.draw(H_MIP_Q.get(secSel,
                                               laySel,
                                               comSel));
            if(fQMIP.hasEntry(secSel,
                              laySel,
                              comSel))
                this.canvasCharge.draw(fQMIP.get(secSel,
                                                 laySel,
                                                 comSel),"same S");
        }
        
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasCharge.cd(oppCDR);
        if(H_MIP_Q.hasEntry(secSel,
                            oppSel,
                            comSel)){
            this.canvasCharge.draw(H_MIP_Q.get(secSel,
                                               oppSel,
                                               comSel));
            if(fQMIP.hasEntry(secSel,
                              oppSel,
                              comSel))
                this.canvasCharge.draw(fQMIP.get(secSel,
                                                 oppSel,
                                                 comSel),"same S");
        }
    }
    
    void drawCanvasVoltage(int secSel,
			   int laySel,
			   int comSel){

	// map [1,2] to [0,1]
        int layCD = laySel-1;
	// map [1,2] to [1,0]
        int oppCD = laySel%2;
	// map [1,2] to [2,1]
        int oppSel   = (laySel%2)+1;
	
	int layCDL =  2*layCD;
        int oppCDL =  2*oppCD;
        
        int layCDR = layCDL + 1;
        int oppCDR = oppCDL + 1;
	
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
        canvasVoltage.cd(layCDL);
        
        if(H_MAXV.hasEntry(secSel,laySel,comSel)){
            this.canvasVoltage.draw(H_MAXV.get(secSel,
					       laySel,
					       comSel));
	    if(fV1.hasEntry(secSel,
			    laySel,
			    comSel))
		this.canvasVoltage.draw(fV1.get(secSel,
						laySel,
						comSel),"same S");
	    if(fV2.hasEntry(secSel,
			    laySel,
			    comSel))
		this.canvasVoltage.draw(fV2.get(secSel,
						laySel,
						comSel),"same S");
	    
        }
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
        canvasVoltage.cd(oppCDL);
        
        if(H_MAXV.hasEntry(secSel,
                              oppSel,
                              comSel))
            this.canvasVoltage.draw(H_MAXV.get(secSel,
                                                 oppSel,
                                                 comSel));
       if(fV1.hasEntry(secSel,
                            oppSel,
                            comSel))
           this.canvasVoltage.draw(fV1.get(secSel,
                                               oppSel,
                                               comSel),"same S");
       if(fV2.hasEntry(secSel,
                            oppSel,
                            comSel))
           this.canvasVoltage.draw(fV2.get(secSel,
                                               oppSel,
                                               comSel),"same S");
        
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasVoltage.cd(layCDR);
        
        if(H_MIP_V.hasEntry(secSel,
                            laySel,
                            comSel)){
            this.canvasVoltage.draw(H_MIP_V.get(secSel,
                                               laySel,
                                               comSel));
           if(fVMIP.hasEntry(secSel,
                             laySel,
                             comSel))
               this.canvasVoltage.draw(fVMIP.get(secSel,
						 laySel,
						 comSel),"same S");
        }
        
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasVoltage.cd(oppCDR);
        if(H_MIP_V.hasEntry(secSel,
                            oppSel,
                            comSel)){
            this.canvasVoltage.draw(H_MIP_V.get(secSel,
						oppSel,
						comSel));
	    if(fVMIP.hasEntry(secSel,
			      oppSel,
			      comSel))
		this.canvasVoltage.draw(fVMIP.get(secSel,
						  oppSel,
						  comSel),"same S");
        }
    } 
    
    void drawCanvasMatch(int secSel,
                         int laySel,
                         int comSel){
	
	// map [1,2] to [0,1]
        int layCD = laySel-1;
	// map [1,2] to [1,0]
        int oppCD = laySel%2;
	// map [1,2] to [2,1]
        int oppSel   = (laySel%2)+1;
	
	int layCDL =  2*layCD;
        int oppCDL =  2*oppCD;
        
        int layCDR = layCDL + 1;
        int oppCDR = oppCDL + 1;
        
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
        canvasMatch.cd(layCDL);
	
        if(H_NOISE_Q.hasEntry(secSel,laySel,comSel)){
            this.canvasMatch.draw(H_NOISE_Q.get(secSel,
                                                laySel,
                                                comSel));
            if(fQ1.hasEntry(secSel,
                                 laySel,
                                 comSel))
                this.canvasMatch.draw(fQ1.get(secSel,
                                                   laySel,
                                                   comSel),"same S");
            if(fQ2.hasEntry(secSel,
                                 laySel,
                                 comSel))
                this.canvasMatch.draw(fQ2.get(secSel,
                                                   laySel,
                                                   comSel),"same S");
            
        }
            //----------------------------------------
            // left top (bottom) for thin (thick) layer
        canvasMatch.cd(oppCDL);
        
        if(H_NOISE_Q.hasEntry(secSel,
                              oppSel,
                              comSel))
            this.canvasMatch.draw(H_NOISE_Q.get(secSel,
                                                oppSel,
                                                comSel));
        if(fQ1.hasEntry(secSel,
                             oppSel,
                             comSel))
            this.canvasMatch.draw(fQ1.get(secSel,
                                               oppSel,
                                               comSel),"same S");
        if(fQ2.hasEntry(secSel,
                             oppSel,
                             comSel))
            this.canvasMatch.draw(fQ2.get(secSel,
                                               oppSel,
                                               comSel),"same S");
        
            //----------------------------------------
            // right top (bottom) for thin (thick) layer
        canvasMatch.cd(layCDR);
        
        if(H_NPE_MATCH.hasEntry(secSel,
                                laySel,
                                comSel)){
            this.canvasMatch.draw(H_NPE_MATCH.get(secSel,
                                                  laySel,
                                                  comSel));
        }
        
            //----------------------------------------
            // right top (bottom) for thin (thick) layer
        canvasMatch.cd(oppCDR);
        if(H_NPE_MATCH.hasEntry(secSel,
                                oppSel,
                                comSel)){
            this.canvasMatch.draw(H_NPE_MATCH.get(secSel,
                                                  oppSel,
                                                  comSel));
        }
        
    }
    
        // if(H_NPE_INT.hasEntry(secSel,
        // 		      laySel,
        // 		      comSel)){
        //     this.canvasCharge.draw(H_NPE_INT.get(secSel,
        // 					 laySel,
        // 					 comSel));
    
        // }
    
        // //----------------------------------------
        // // right top (bottom) for thin (thick) layer
        // canvasCharge.cd(oppCDR);
        // if(H_NPE_INT.hasEntry(secSel,
        // 		      oppSel,
        // 		      comSel)){
        //     this.canvasCharge.draw(H_NPE_INT.get(secSel,
        // 					 oppSel,
        // 					 comSel));
        // }
    
    
    void drawCanvasMIP(int secSel,
		       int laySel,
		       int comSel
                       ){
        
        if(secSel == 0)
            return;
        
        int sector2CD[] = {4,0,1,2,5,8,7,6,3};
        
        canvasMIP.cd(sector2CD[secSel]);
        
        GraphErrors[] G_NPE;
        
        int p30EvenI[] = {1,2,3,4,5,6,7,8,9,10,11,12};
        int p15EvenI[] = {13,14,15,16,17,18,19,20};
        int p30OddI[]  = {2,4,5,6,7,8};
        int p15OddI[]  = {1,3,9};
        
        double p30EvenD[] = {1,2,3,4,5,6,7,8,9,10,11,12};
        double p15EvenD[] = {13,14,15,16,17,18,19,20};
        double p30OddD[]  = {2,4,5,6,7,8};
        double p15OddD[]  = {1,3,9};
        
	// Was a P30 or a P15 tile selected?
        boolean plotP30       = true;
        boolean evenSecSelect = true;
        
        if(secSel%2==1)
            evenSecSelect = false;
        
        if(evenSecSelect){
            for ( int i = 0 ; i < p15EvenI.length ; i++){
                if( comSel == p15EvenI[i] ){
                    plotP30 = false;
                    break;
                }
            }
        }
        else{
            for ( int i = 0 ; i < p15OddI.length ; i++){
                if( comSel == p15OddI[i] ){
                    plotP30 = false;
                    break;
                }
            }
        }
        
        double p30EvenE[] = {0,0,0,0,0,0,0,0,0,0,0,0};
        double p15EvenE[] = {0,0,0,0,0,0,0,0};
        double p30OddE[]  = {0,0,0,0,0,0};
        double p15OddE[]  = {0,0,0};
        
        double p30EvenNPE[][] = new double[2][12];
        double p30EvenERR[][] = new double[2][12];
        double p15EvenNPE[][] = new double[2][8];
        double p15EvenERR[][] = new double[2][8];
        double p30OddNPE[][]  = new double[2][6];
        double p30OddERR[][]  = new double[2][6];
        double p15OddNPE[][]  = new double[2][3];
        double p15OddERR[][]  = new double[2][3];
	
	if(useGain_mV){
	    for( int lM = 0 ; lM < 2 ; lM++){
		for (int c = 0 ; c < p30EvenI.length ; c++){
		    p30EvenNPE[lM][c] = meanNPE_mV[secSel][lM+1][p30EvenI[c]];
		    p30EvenERR[lM][c] = errNPE_mV[secSel][lM+1][p30EvenI[c]];
		}
		for (int c = 0 ; c < p15EvenI.length ; c++){
		    p15EvenNPE[lM][c] = meanNPE_mV[secSel][lM+1][p15EvenI[c]];
		    p15EvenERR[lM][c] = errNPE_mV[secSel][lM+1][p15EvenI[c]];
		}
		
		for (int c = 0 ; c < p30OddI.length ; c++){
		    p30OddNPE[lM][c] = meanNPE_mV[secSel][lM+1][p30OddI[c]];
		    p30OddERR[lM][c] = errNPE_mV[secSel][lM+1][p30OddI[c]];
		}
		for (int c = 0 ; c < p15OddI.length ; c++){
		    p15OddNPE[lM][c] = meanNPE_mV[secSel][lM+1][p15OddI[c]];
		    p15OddERR[lM][c] = errNPE_mV[secSel][lM+1][p15OddI[c]];
		}
	    }
	}
	else{
	    for( int lM = 0 ; lM < 2 ; lM++){
		for (int c = 0 ; c < p30EvenI.length ; c++){
		    p30EvenNPE[lM][c] = meanNPE[secSel][lM+1][p30EvenI[c]];
		    p30EvenERR[lM][c] = errNPE[secSel][lM+1][p30EvenI[c]];
		}
		for (int c = 0 ; c < p15EvenI.length ; c++){
		    p15EvenNPE[lM][c] = meanNPE[secSel][lM+1][p15EvenI[c]];
		    p15EvenERR[lM][c] = errNPE[secSel][lM+1][p15EvenI[c]];
		}
		
		for (int c = 0 ; c < p30OddI.length ; c++){
		    p30OddNPE[lM][c] = meanNPE[secSel][lM+1][p30OddI[c]];
		    p30OddERR[lM][c] = errNPE[secSel][lM+1][p30OddI[c]];
		}
		for (int c = 0 ; c < p15OddI.length ; c++){
		    p15OddNPE[lM][c] = meanNPE[secSel][lM+1][p15OddI[c]];
		    p15OddERR[lM][c] = errNPE[secSel][lM+1][p15OddI[c]];
		}
	    }
	}
	
        G_NPE    = new GraphErrors[2];
                
	for( int layerM = 0 ; layerM < 2 ; layerM++){
            
            if(plotP30){
                if(evenSecSelect)
                    G_NPE[layerM] = new GraphErrors(p30EvenD,
                                                    p30EvenNPE[layerM],
                                                    p30EvenE,
                                                    p30EvenERR[layerM]);
                else
                    G_NPE[layerM] = new GraphErrors(p30OddD,
                                                    p30OddNPE[layerM],
                                                    p30OddE,
                                                    p30OddERR[layerM]);
            }
            else{
                if(evenSecSelect)
                    G_NPE[layerM] = new GraphErrors(p15EvenD,
                                                    p15EvenNPE[layerM],
                                                    p15EvenE,
                                                    p15EvenERR[layerM]);
                else
                    G_NPE[layerM] = new GraphErrors(p15OddD,
                                                    p15OddNPE[layerM],
                                                    p15OddE,
                                                    p15OddERR[layerM]);
            }
            
	    String title;
            title = "sector " + secSel;
            G_NPE[layerM].setTitle(title);
            G_NPE[layerM].setXTitle("component");
            G_NPE[layerM].setYTitle("NPE mean ");
            G_NPE[layerM].setMarkerSize(5);
            G_NPE[layerM].setMarkerColor(layerM+1); // 0-9 for given palette
            G_NPE[layerM].setMarkerStyle(layerM+1); // 1 or 2
            
        }
        canvasMIP.draw(G_NPE[0]);
        canvasMIP.draw(G_NPE[1],"same");
    }
    
    void drawCanvasMIPElec(int secSel,
			   int laySel,
			   int comSel
			   ){
        
        if(secSel == 0 ||
	   laySel == 0)
            return;
        
        canvasMIP.divide(1,1);
        
        GraphErrors G_NPE;
	
	int mezSel = getMezz4SLC(secSel,
				 laySel,
				 comSel);
	
	double[] chanArr = {0,1,2,3,
			    4,5,6,7,
			    8,9,10,11,
			    12,13,14,15};
	
	double[] chanErr = {0,0,0,0,
			    0,0,0,0,
			    0,0,0,0,
			    0,0,0,0};
	
	double[] npeArr = new double[16];
	double[] npeErr = new double[16];
		
	int sect;
	int comp;
	int laye;

	if(useGain_mV){
	    for (int chan = 0 ; chan < 16 ; chan++ ){
		sect = getSect4ChMez(chan,mezSel);
		comp = getComp4ChMez(chan,mezSel);
		laye = chan/8 + 1;
		
		npeArr[chan] = meanNPE_mV[sect][laye][comp];
		npeErr[chan] = errNPE_mV[sect][laye][comp];
	    }
	}
	else{
	    for (int chan = 0 ; chan < 16 ; chan++ ){
		sect = getSect4ChMez(chan,mezSel);
		comp = getComp4ChMez(chan,mezSel);
		laye = chan/8 + 1;
		
		npeArr[chan] = meanNPE[sect][laye][comp] ;
		npeErr[chan] = errNPE[sect][laye][comp];
	    }
	}
	
	G_NPE = new GraphErrors(chanArr,npeArr,
				chanErr,npeErr);
	
	String title;
	title = "mezzanine " + mezSel;
	G_NPE.setTitle(title);
	G_NPE.setXTitle("channel");
	G_NPE.setYTitle("NPE mean");
	G_NPE.setMarkerSize(5);
	G_NPE.setMarkerColor(1); 
	G_NPE.setMarkerStyle(1); 
	
	canvasMIP.draw(G_NPE);
	
    }
    
    void drawCanvasGain(int secSel,
                        int laySel,
                        int comSel
                        ){
        
	if(secSel == 0)
            return;
	
	boolean evenSecSelect = true;
        
        String sectors[] = new String[8];
        
        if(secSel%2==1)
            evenSecSelect = false;
        
        int sector2CD[] = {4,0,1,2,5,8,7,6,3};
        
        canvasGain.cd(sector2CD[secSel]);        
        
        int evenI[]    = {1 ,2 ,3 ,4 ,5 ,
			  6 ,7 ,8 ,9 ,10,
			  11,12,13,14,15,
			  16,17,18,19,20};
        
        double evenD[] = {1 ,2 ,3 ,4 ,5 ,
			  6 ,7 ,8 ,9 ,10,
			  11,12,13,14,15,
			  16,17,18,19,20};
        
        int oddI[]     = {1,2,3,4,5,
			  6,7,8,9};
        
        double oddD[]  = {1,2,3,4,5,
			  6,7,8,9};
        
        double evenE[] = {0,0,0,0,0,
			  0,0,0,0,0,
			  0,0,0,0,0,
			  0,0,0,0,0};
        
        double oddE[]  = {0,0,0,0,0,
			  0,0,0,0};
        
        double evenGain[][]    = new double[2][20];
        double evenGainErr[][] = new double[2][20];
        double oddGain[][]     = new double[2][9];
        double oddGainErr[][]  = new double[2][9];
        
	GraphErrors[] G_Gain;
	G_Gain    = new GraphErrors[2];
	
	String yTitle = "gain (pC)";
	
	double maxErr = 10.;
	
	if(useGain_mV){
	    yTitle = "gain (mV)";
	    maxErr = 5.0;
	}
	
	for( int lM = 0 ; lM < 2 ; lM++){
            // loop over even indices
            for (int c = 0 ; c < evenI.length ; c++){
		
		if(!useGain_mV){
		    
		    evenGain[lM][c]    = gain[secSel][lM+1][evenI[c]];
		    evenGainErr[lM][c] = errGain[secSel][lM+1][evenI[c]];
		    
		}
		else{
		    evenGain[lM][c]    = gain_mV[secSel][lM+1][evenI[c]];
		    evenGainErr[lM][c] = errGain_mV[secSel][lM+1][evenI[c]];
		
		}
		
		if (evenGainErr[lM][c] > maxErr){
		    evenGain[lM][c]    = 0.0;
		    evenGainErr[lM][c] = 0.0;
		}
		
            }
            // loop over odd indices
	    for (int c = 0 ; c < oddI.length ; c++){

		if(!useGain_mV){
		    oddGain[lM][c]    = gain[secSel][lM+1][oddI[c]];
		    oddGainErr[lM][c] = errGain[secSel][lM+1][oddI[c]];
		}
		else {
		    oddGain[lM][c]    = gain_mV[secSel][lM+1][oddI[c]];
		    oddGainErr[lM][c] = errGain_mV[secSel][lM+1][oddI[c]];
		}
		
		if (oddGainErr[lM][c] > maxErr){
		    oddGain[lM][c]    = 0.0;
		    oddGainErr[lM][c] = 0.0;
		}
		    
            }

            if(evenSecSelect)
                G_Gain[lM] = new GraphErrors(evenD,
                                             evenGain[lM],
                                             evenE,
                                             evenGainErr[lM]);
            else
                G_Gain[lM] = new GraphErrors(oddD,
                                             oddGain[lM],
                                             oddE,
                                             oddGainErr[lM]);
            
            
	    String title;
	    title = "sector " + secSel;
            
	    G_Gain[lM].setTitle(title);
            G_Gain[lM].setXTitle("component");
            G_Gain[lM].setYTitle(yTitle);
            G_Gain[lM].setMarkerSize(5);
            G_Gain[lM].setMarkerColor(lM+1); // 0-9 for given palette
            G_Gain[lM].setMarkerStyle(lM+1); // 1 or 2
	    
        }
        
        int nXBins[] = {20,9};
        int nYBins = 100;
        double[] xLimits = {0.5,(double)nXBins[secSel%2]+0.5};
        double[] yLimits = {5.0,30.};
        
        H1D H1 = new H1D("H1","component","gain (pC)",
			 nXBins[secSel%2],xLimits[0],xLimits[1]);
        
	H1.setYTitle(yTitle);	
	canvasGain.draw(H1);
        canvasGain.draw(G_Gain[0],"same");
        canvasGain.draw(G_Gain[1],"same");
        H1.getYaxis().set(nYBins,yLimits[0],yLimits[1]);
	canvasGain.draw(H1,"same");
	
    } // end: drawCanvasGain.....
    
    void drawCanvasGainElec(int secSel,
			    int laySel,
			    int comSel
			    ){
	if(secSel == 0 || laySel == 0)
            return;

	canvasGain.divide(1,1);
	    
	double[] gainArr    = new double[16];
        double[] gainErrArr = new double[16];
        double[] chanArr    = new double[16];
        double[] chanErrArr = new double[16];

// 	double[] gainArr    = new double[240];
//      double[] gainErrArr = new double[240];
//      double[] chanArr    = new double[240];
//      double[] chanErrArr = new double[240];
	
	int sectI;
	int compI;
	int layeI;
	int ii = 0;
	
	int mezz = getMezz4SLC(secSel,laySel,comSel);
	
	//	for( int mezz = 0 ; mezz < 15 ; mezz++){
	for (int chan = 0 ; chan < 16 ; chan++){
	    
	    sectI  = getSect4ChMez(chan,mezz);
	    compI  = getComp4ChMez(chan,mezz);
	    layeI  = chan/8 + 1;
	    
	    if(!useGain_mV){
		gainArr[ii]    = gain[sectI][layeI][compI] ;
		gainErrArr[ii] = errGain[sectI][layeI][compI];
	    }
	    else {
		gainArr[ii]    = gain_mV[sectI][layeI][compI];
		gainErrArr[ii] = errGain_mV[sectI][layeI][compI];
	    }
	    //gainArr[ii] = gainArr[ii] + (double)mezz*100.;
	    
	    chanArr[ii] = chan;
	    chanErrArr[ii] = 0;

// 	    System.out.println(" gain_mV["+
// 			       sectI +"]["+
// 			       layeI +"]["+
// 			       compI +"] = " + 
// 			       gain_mV[sectI][layeI][compI]);
	    
// 	    System.out.println(" Channel =" + chan ); 
	    
// 		System.out.println(" gainArr[ii] = " +  gainArr[ii] +
// 				   ",chanArr[ii] = " +  chanArr[ii] +
// 				   ",         ii = " +  ii);
	    
	    ii++;
	}
	//    }
	
	String title;        
	title = "mezzanine" + mezz;
	
        GraphErrors G_Gain;
	
	G_Gain = new GraphErrors(chanArr,
				 gainArr,
				 chanErrArr,
				 gainErrArr);
		
	String titleH  = "H1";
	String titleHY = "Gain (pC)";
	String titleHX = "Channel";

	double[] xLimits = {-0.5,15.5};
	double[] yLimits = {10., 30.};
		
        if(useGain_mV){
            titleHY = "Gain (mV)";
	    yLimits[0] =  5.;
	    yLimits[1] = 15.;
	}
	
	G_Gain.setTitle(title);
	G_Gain.setXTitle(titleHY);
	G_Gain.setYTitle("Channel");
	G_Gain.setMarkerSize(5);
	G_Gain.setMarkerColor(1); // 0-9 for given palette
	G_Gain.setMarkerStyle(laySel); // 1 or 2
	    
        int nXBins = 16;
        int nYBins = 100;

// 	H1D H1 = new H1D(titleH,titleHX,titleHY,
// 			 nXBins,xLimits[0],xLimits[1]);
        
	canvasGain.cd(0);
	//canvasGain.draw(H1);
        //canvasGain.draw(G_Gain,"same");
	canvasGain.draw(G_Gain);
//	H1.getXaxis().set(nXBins,xLimits[0],xLimits[1]);
	//canvasGain.draw(H1,"same");
    
    } // end: drawCanvasGainEle.....
    
    
    
    
    //=======================================================
    //               Detector Selected
    //=======================================================
    public void detectorSelected(DetectorDescriptor desc) {
	//System.out.println("SELECTED = " + desc);
        
        secSel = desc.getSector();
        laySel = desc.getLayer();
        comSel = desc.getComponent();
        
        String  detector;
        boolean calSel = false;
        boolean hodSelect = false;
        
        if      ( desc.getType() == DetectorType.FTCAL ){
            detector = "FTCAL";
            calSel = true;
	    
        }
        else if ( desc.getType() == DetectorType.FTHODO ){
            detector = "FTHODO";
            hodSelect = true;
        }
        else {
            detector  = "Unidentified";
            return;
        }
        
        System.out.println(" Detector Selected is " +
                           detector);
        
        System.out.println(" Sector = " +
                           secSel    +
                           ", Layer = "  +
                           laySel    +
                           ", Component = " +
                           comSel);
	
        
        System.out.println(" Channel = " +
                           getChan4SLC(secSel,laySel,comSel) );
	
        System.out.println(" Mezzanine = " +
                           getMezz4SLC(secSel,laySel,comSel) );
	
	// only process paddles if cal selected
        if(calSel && comSel < 500){
            return;
        }
        
        if(!hodSelect && !calSel){
            return;
        }
	
	// map [1,2] to [0,1]
        int layCD = laySel-1;
	// map [1,2] to [1,0]
        int oppCD = laySel%2;
	// map [1,2] to [2,1]
        int oppSel   = (laySel%2)+1;
	
	// indices for paddles
        if(calSel){
            layCD = 0;
            oppCD = 1;
            oppSel   = 0;
        }
        
        int layCDL =  2*layCD;
        int oppCDL =  2*oppCD;
        
        int layCDR = layCDL + 1;
        int oppCDR = oppCDL + 1;
        
        
	//============================================================
	// FTViewer (Combined view tab)
        
        drawCanvasHODOEvent(calSel,
                            secSel,
                            comSel,
                            oppSel,
                            layCD,
                            oppCD);
        
	// end of FTViewer (Combined view tab)
	//============================================================
        
	//============================================================
	// FTHODOViewer
	
        if      ( tabSelect == this.tabIndexEvent ) {
	    
	    System.out.println(" (S,L,C) = (" + 
			       secSel + ","   +
			       laySel + ","   +
			       comSel + ")"   );
	    
	    drawCanvasEvent(secSel,
                            laySel,
                            comSel);
            
        }
        else if ( tabSelect == this.tabIndexPed ) {
	 
	    drawCanvasPed(secSel,
			  laySel,
			  comSel);
            
        }
        else if ( tabSelect == this.tabIndexNoise ) {
	    
            drawCanvasNoise(secSel,
                            laySel,
                            comSel);
            
        }
        else if ( tabSelect == this.tabIndexGain ) {
	    
	    
	    if(drawByElec==false){
		this.canvasGain.divide(3,3);
                
		for (int i = 1 ; i < 9 ; i++ )
		    drawCanvasGain(i,
				   laySel,
				   comSel);
	    }
            else{
		this.canvasGain.divide(1,1);
                drawCanvasGainElec(secSel,
				   laySel,
				   comSel);
		
		}
	
	}
        else if ( tabSelect == this.tabIndexCharge ) {
            
            drawCanvasCharge(secSel,
                             laySel,
                             comSel);
			     
        }
        else if ( tabSelect == this.tabIndexVoltage ) {
            
            drawCanvasVoltage(secSel,
			      laySel,
			      comSel);
			     
        }
        else if ( tabSelect == this.tabIndexMIP ) {
            
	    if(drawByElec==false){
		drawCanvasMIP(secSel,
			      laySel,
			      comSel);
	    }
	    else{
		drawCanvasMIPElec(secSel,
				  laySel,
				  comSel);
	    }
	    
	}
	    
        else if ( tabSelect == this.tabIndexMatch ) {
            
            drawCanvasMatch(secSel,
                            laySel,
                            comSel);
        }
	else if ( tabSelect == this.tabIndexTime ) {
            
            drawCanvasTime(secSel,
			   laySel,
			   comSel);
        }
	
	
    } // end of: public void detectorSelected(DetectorD....
    
    
    public Color getComponentStatus(int sec, int lay, int com) {
        
        int index = com;
        
        if (lay > 0) // cal layer is always 0
            index = getIndex4SLC(sec,lay,com);
        
        Color col = new Color(100,100,100);
        
        if(H_W_MAX.getBinContent(index) > cosmicsThrsh) {
            col = palette.
		getColor3D(H_W_MAX.getBinContent(index),
			   4000, 
			   true);
        }
        return col;
    }
    
    public int getIndex4SLC(int sec, int lay, int com){
        	
        int sector_count[] = {0,9,29,38,58,67,87,96};
        
        int index  = (lay - 1)*116 + sector_count[sec-1] + com;
        
        return index;
    }
    
        // for all shapes made this is executed
        // for every event and every action
    public void update(DetectorShape2D shape) {
        
        int sec = shape.getDescriptor().getSector();
        int lay = shape.getDescriptor().getLayer();
        int com = shape.getDescriptor().getComponent();
        
        int index = com;
        
        if  (shape.getDescriptor().getType() == DetectorType.FTCAL){
            sec   = 0;
            lay   = 0;
            index = com;
        }
        else{
            
	    if(sec > 0 && sec < 9)
		index = getIndex4SLC(sec,lay,com);
	    else{
		System.out.println(" S = " +  sec +
				   ",L = " +  lay +
				   ",C = " +  com);
	    }
	}
        double waveMax     = H_W_MAX.getBinContent(index);
	double voltMax     = H_V_MAX.getBinContent(index);
        
	double npeWaveMax  = H_NPE_MAX.getBinContent(index);
        
            // map [0,4095] to [0,255]
        int    signalAlpha = (int)(waveMax)/16;
        
            // map [0,20] to [0,255]
        int    noiseAlpha  = min((int)(npeWaveMax/20*255),255);
	
	Color  pedColor = palette.getColor3D(pedMean[sec][lay][com],
					     400,
					     true);
	
	Color  noiseColor = palette.getColor3D(vMax[sec][lay][com],
					       2*nGain_mV,
					       false);
	
	Color  gainColor;

	if(useGain_mV){
	    gainColor = palette.getColor3D(gain_mV[sec][lay][com],
					   1.5*nGain_mV,
					   true);
	}
	else{  
	    gainColor = palette.getColor3D(gain[sec][lay][com],
					   1.5*nGain,
					   true);
	}
	
	Color  voltColor = palette.getColor3D(vMax[sec][lay][com],
					      12*nGain_mV,
					      true);
	
	
        if    ( tabSelect == tabIndexEvent ) {
            if      ( waveMax > cosmicsThrsh) {
                shape.setColor(0, 255, 0, signalAlpha);
            }
            else if ( waveMax  > noiseThrsh) {
                shape.setColor(255, 255, 0, noiseAlpha);
            }
            else {
                shape.setColor(255, 255, 255, 0);
            }
        }
	else if( tabSelect == tabIndexPed ) {
	    shape.setColor(pedColor.getRed(),
			   pedColor.getGreen(),
			   pedColor.getBlue());
        }
        else if( tabSelect == tabIndexNoise ){ 
	    shape.setColor(noiseColor.getRed(),
			   noiseColor.getGreen(),
			   noiseColor.getBlue());
	}
	else if( tabSelect == tabIndexVoltage ) {
	    shape.setColor(voltColor.getRed(),
			   voltColor.getGreen(),
			   voltColor.getBlue());
        }
	else if( tabSelect == tabIndexGain ){ 
	    shape.setColor(gainColor.getRed(),
			   gainColor.getGreen(),
			   gainColor.getBlue());
	}
	else if( tabSelect == tabIndexMIP    ||
		 tabSelect == tabIndexCharge ){
            if      (waveMax  > cosmicsThrsh ) {
                shape.setColor(0, 255, 0, (256/4)-1);
            }
            else if (waveMax  > cosmicsThrsh*1.5){
                shape.setColor(0, 255, 0, (256/2)-1);
            }
            else if (waveMax  > cosmicsThrsh*2.0){
                shape.setColor(0, 255, 0, 255);
            }
        }
    } // end of : public void update(Detec

    //--------------------------------------------
    // Calculate Constants
    
    private double getPedMean(int s, int l, int c){
        
        double thisPed = 0.0;
        
        if(fPed.hasEntry(s, l, c)){
	    
	    thisPed = fPed.get(s,l,c).getParameter(1);
            
        }

        return thisPed;
    }

    private double getGain(int s, int l, int c){
        
        double thisGain = 0.0;
        
        if(fQ1.hasEntry(s, l, c) &&
           fQ2.hasEntry(s, l, c)){
            
            double n2 = fQ2.get(s,l,c).getParameter(1);
            double n1 = fQ1.get(s,l,c).getParameter(3);
            
            thisGain = n2 - n1;
            
        }
        
        if (thisGain < 15.0 ||
            thisGain > 25.0)
            thisGain = 0.0;
	
        return thisGain;
    }
    
//     private void setGainToZero(int s, int l, int c){
	
// 	gain[s][l][c] = 0.0;
	
//     }
    
    private double getGainError(int s, int l, int c){
        
        double gainError = 0.0;
        
        if(fQ1.hasEntry(s,l,c) &&
           fQ2.hasEntry(s,l,c) &&
	   getGain(s,l,c) > 0.0 ){
            
            double n2Error = fQ2.get(s,l,c).getParError(1);
            double n1Error = fQ1.get(s,l,c).getParError(3);
            
	    gainError    = n2Error*n2Error + n1Error*n1Error;
            gainError    = sqrt(gainError);
            
        }
        
        return gainError;
        
    }
    
    private double getGain_mV(int s, int l, int c){
	
        double thisGain_mV = 0.0;
	
//         if(fV1.hasEntry(s, l, c) &&
//     	   fV2.hasEntry(s, l, c)){

// 	    // note that the functions were added in the other
// 	    // order to the charge fits
// 	    double m2 = fV2.get(s,l,c).getParameter(1);
//             double m1 = fV1.get(s,l,c).getParameter(1);
            
//             thisGain_mV = m2 - m1;
	    
//         }
        
	if(fV1.hasEntry(s, l, c)){
	    double m1 = fV1.get(s,l,c).getParameter(1);
	    thisGain_mV =  m1;
	}
        
	if (thisGain_mV < 0.5*nGain_mV ||
	    thisGain_mV > 1.5*nGain_mV)
	    thisGain_mV = 0.0;
	
        return thisGain_mV;
	
    }
    
    private double getGainErr_mV(int s, int l, int c){
	    
        double gainErr_mV = 0.0;
	
        if(fV1.hasEntry(s,l,c) &&
	   getGain_mV(s,l,c) > 0.0 ){
	    double m1Error = fV1.get(s,l,c).getParError(1);
            gainErr_mV = m1Error;
        }
	
// 	if(fV1.hasEntry(s, l, c) &&
//            fV2.hasEntry(s, l, c)){
	    
// 	    // note that the functions were added in the other
// 	    // order to the charge fits
//             double m2Error = fV2.get(s,l,c).getParError(1);
//             double m1Error = fV1.get(s,l,c).getParError(1);
//             gainErr_mV   = m2Error*m2Error + m1Error*m1Error;
//             gainErr_mV   = sqrt(gainErr_mV);
	    
//         }
	    
        return gainErr_mV;
        
    }
    
    private double getQMean(int s, int l, int c){
	
        double qMean = 0.0;
        
        if(fQMIP.hasEntry(s, l, c))
            qMean = fQMIP.get(s,l,c).getParameter(1);
        
        return qMean;
        
    }

    private double getVMean(int s, int l, int c){
	
        double vMean = 0.0;
        
        if(fVMIP.hasEntry(s, l, c))
            vMean = fVMIP.get(s,l,c).getParameter(1);
        
        return vMean;
        
    }

    private double getQMeanError(int s, int l, int c){
        
        double qMeanError = 0.0;
        
        if(fQMIP.hasEntry(s, l, c))
            qMeanError = fQMIP.get(s,l,c).getParError(1);
        
        return qMeanError;
        
    }

    private double getVMeanError(int s, int l, int c){
        
        double vMeanError = 0.0;
        
        if(fVMIP.hasEntry(s, l, c))
            vMeanError = fVMIP.get(s,l,c).getParError(1);
        
        return vMeanError;
        
    }
    
    
    private double getNPEMean(int s, int l, int c){
        
        double npeMean = 0.0;
        
        if( getGain(s,l,c) > 0.0 )
            npeMean = getQMean(s,l,c)/getGain(s,l,c);
        
        return npeMean;
        
    }

    private double getNPEMean_mV(int s, int l, int c){
	
        double npeMean_mV = 0.0;
        
        if( getGain_mV(s,l,c) > 0.0 )
            npeMean_mV = getVMean(s,l,c)/getGain_mV(s,l,c);
        
        return npeMean_mV;
        
    }

    private double getSigNPE(int s, int l, int c){
	
        double sig = 10.0;
        
        return sig;
    }
    


    private double getNPEError(int s, int l, int c){
	
	double npeError = 0.0;
        
	if( getQMean(s,l,c) > 0.0 &&
	    getGain(s,l,c)  > 0.0 ){
            
            npeError = getQMeanError(s,l,c)*getQMeanError(s,l,c);
	    
	    npeError = npeError / ( getQMean(s,l,c)*getQMean(s,l,c));
	    npeError = npeError +
		(getGainError(s,l,c)*getGainError(s,l,c)/
		 (getGain(s,l,c)*getGain(s,l,c)));
	    
	    npeError = sqrt(npeError);
	    npeError = getNPEMean(s,l,c)*npeError;
	}
	
        return npeError;
        
    }

    private double getNPEErr_mV(int s, int l, int c){
	
	double npeErr_mV = 0.0;
	
	if( getVMean(s,l,c)   > 0.0 &&
	    getGain_mV(s,l,c) > 0.0 ){
            
            npeErr_mV = getVMeanError(s,l,c)*getVMeanError(s,l,c);
	    
	    npeErr_mV = npeErr_mV / ( getVMean(s,l,c)*getVMean(s,l,c));
	    npeErr_mV = npeErr_mV +
		(getGainErr_mV(s,l,c)*getGainErr_mV(s,l,c)/
		 (getGain_mV(s,l,c)*getGain_mV(s,l,c)));
	    
	    npeErr_mV = sqrt(npeErr_mV);
	    npeErr_mV = getNPEMean_mV(s,l,c)*npeErr_mV;
	}
	
        return npeErr_mV;
        
    }
    
        // private double getNPEStd(int s, int l, int c){
    
        // 	double npeMean = 0.0;
        // 	if( get > 0.0 )
        // 	    npeMean = getQMean(s,l,c)/getGain(s,l,c);
    
        // 	return npeMean;
    
        // }
    
    
    private void updateVariables() {
        
	int index;
        
	for (int s = 1; s < 9; s++) {
            for (int l = 1; l < 3; l++) {
                for (int c = 1 ; c < 21 ; c++){
                    
                    if(s%2==1 && c > 9 ) continue;

		    index = getIndex4SLC(s,l,c);
		    
		    pedMean[s][l][c]    = getPedMean(s,l,c);
		    
		    meanNPE[s][l][c]    = getNPEMean(s,l,c);
                    errNPE[s][l][c]     = getNPEError(s,l,c);
                    sigNPE[s][l][c]     = getSigNPE(s,l,c);
		    
		    gain[s][l][c]       = getGain(s,l,c);
                    errGain[s][l][c]    = getGainError(s,l,c);
		    
		    
		    meanNPE_mV[s][l][c] = getNPEMean_mV(s,l,c);
                    errNPE_mV[s][l][c]  = getNPEErr_mV(s,l,c);
                    gain_mV[s][l][c]    = getGain_mV(s,l,c);
                    errGain_mV[s][l][c] = getGainErr_mV(s,l,c);
		    
		    //  System.out.println(" Nick Said " +s+" "+l+" "+c+" "+
		    //         gain_mV[s][l][c]);
                    
                    summaryTable.setValueAtAsDouble(0,
                                                    meanNPE[s][l][c],
                                                    s,l,c);
                    
                    summaryTable.setValueAtAsDouble(1,
                                                    errNPE[s][l][c],
                                                    s,l,c);
                    
                    summaryTable.setValueAtAsDouble(2,
                                                    gain[s][l][c],
                                                    s,l,c);
                    
                    summaryTable.setValueAtAsDouble(3,
                                                    errGain[s][l][c],
                                                    s,l,c);
                    
                    summaryTable.setValueAtAsDouble(4,
                                                    gain_mV[s][l][c],
                                                    s,l,c);
                    
                    summaryTable.setValueAtAsDouble(5,
                                                    errGain_mV[s][l][c],
                                                    s,l,c);
                    
                }
            }
        }
        summaryTable.show();
        this.view.repaint();
        
    } // end of: private void updateTable() {
    
    public void stateChanged(ChangeEvent e) {
        JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
	
	//!! sourceDetTabPane = () e.getSource();
	tabSelect = sourceTabbedPane.getSelectedIndex();
	// detTabSel = sourceDetTabPane.getSelectedIndex();
        
	if ( tabSelect == this.tabIndexGain ||
	     tabSelect == this.tabIndexMIP )
	    this.canvasPane.add(rBPane, BorderLayout.NORTH);
	
	if( (this.previousTabSelect==this.tabIndexGain ||
	     this.previousTabSelect==this.tabIndexMIP ) && 
	    tabSelect!=this.tabIndexGain )
            this.canvasPane.remove(rBPane);
    
	System.out.println("Tab changed to: " +
                           sourceTabbedPane.getTitleAt(tabSelect) +
                           " with index " + tabSelect);
        
        if(tabSelect == tabIndexTable)
            this.updateVariables();
        
	previousTabSelect = tabSelect;
	
        this.view.repaint();
    }
    
    public void initHistograms() {
        
        this.initArrays();
        
	// hodoscope
        for (int index = 0; index < 232; index++)
            setHistogramsHodo(index);
        
	// calorimeter
        for (int index = 0; index < 505; index++)
            setHistogramsCal(index);
        
        H_W_MAX    = new H1D("H_W_MAX", 504, 0, 504);
        H_V_MAX    = new H1D("H_V_MAX", 504, 0, 2000);
        H_NPE_MAX  = new H1D("H_NPE_MAX", 500, 0, 50);
        
    }
    
    private void setHistogramsHodo(int index){
        char detector='h';
        HistPara HP = new HistPara();
        
        HP.setAllParameters(index,detector);
        
	//----------------------------
	// Event-by-Event Histograms
	
        H_FADC.add(HP.getS(),HP.getL(),HP.getC(),
                   new H1D(DetectorDescriptor.
                           getName("H_FADC",
				   HP.getS(),
				   HP.getL(),
				   HP.getC()),
                           HP.getTitle(),100, 0.0, 100.0));
        
	H_FADC.get(HP.getS(),HP.getL(),HP.getC()).
	    setFillColor(4);
	H_FADC.get(HP.getS(),HP.getL(),HP.getC()).
	    setXTitle("fADC Time");
        H_FADC.get(HP.getS(),HP.getL(),HP.getC()).
	    setYTitle("fADC Amplitude");
        
        H_VT.add(HP.getS(),HP.getL(),HP.getC(),
		 new H1D(DetectorDescriptor.
			 getName("H_VT",
				 HP.getS(),
				 HP.getL(),
				 HP.getC()),
			 HP.getTitle(), 100, 0.0, 400.0));
        
	H_VT.get(HP.getS(),HP.getL(),HP.getC()).
	    setFillColor(3);
        H_VT.get(HP.getS(),HP.getL(),HP.getC()).
	    setXTitle("Time (ns)");
        H_VT.get(HP.getS(),HP.getL(),HP.getC()).
	    setYTitle("Voltage (mV)");
        
        H_NPE.add(HP.getS(),HP.getL(), HP.getC(),
                  new H1D(DetectorDescriptor.
                          getName("H_NPE",
				  HP.getS(),
				  HP.getL(),
				  HP.getC()),
                          HP.getTitle(), 100, 0.0, 400.0));
        H_NPE.get(HP.getS(),HP.getL(),HP.getC()).
	    setFillColor(5);
        H_NPE.get(HP.getS(),HP.getL(),HP.getC()).
	    setXTitle("Time (ns)");
        H_NPE.get(HP.getS(),HP.getL(),HP.getC()).
	    setYTitle("Photoelectrons (amp mV/spe mV");
        
        
	//----------------------------
	// Accumulated Histograms
        
	// PEDESTAL CANVAS
        H_PED.add(HP.getS(),HP.getL(),HP.getC(),
		  new H1D(DetectorDescriptor.
			  getName("H_PED",
				  HP.getS(),
				  HP.getL(),
				  HP.getC()),
			  HP.getTitle(),
			  128,50.,400.));
	
        H_PED.get(HP.getS(),HP.getL(),HP.getC()).
	    setFillColor(2);
	H_PED.get(HP.getS(),HP.getL(),HP.getC()).
	    setXTitle("Pedestal");
	H_PED.get(HP.getS(),HP.getL(),HP.getC()).
	    setYTitle("Counts");

	
	H_MIP_Q.add(HP.getS(),HP.getL(),HP.getC(),
                    new H1D(DetectorDescriptor.
                            getName("Cosmic Charge",
                                    HP.getS(),
				    HP.getL(),
				    HP.getC()),
                            HP.getTitle(),
                            NBinsCosmic,
                            CosmicQXMin[HP.getL()],
			    CosmicQXMax[HP.getL()]));

        H_MIP_Q.get(HP.getS(),HP.getL(),HP.getC()).
	    setFillColor(3);
        
	H_MIP_Q.get(HP.getS(),HP.getL(),HP.getC()).
	    setXTitle("Charge (pC)");
        
	H_MIP_Q.get(HP.getS(),HP.getL(),HP.getC()).
	    setYTitle("Counts");
	
	H_NOISE_Q.add(HP.getS(),HP.getL(),HP.getC(),
                      new H1D(DetectorDescriptor.
                              getName("Noise Charge",
                                      HP.getS(),
				      HP.getL(),
				      HP.getC()),
                              HP.getTitle(),
                              NBinsNoise,
			      0.5*nGain,
			      3.0*nGain));
	
	H_NOISE_Q.get(HP.getS(),HP.getL(),HP.getC()).
	    setFillColor(5);
	
        H_NOISE_Q.get(HP.getS(),HP.getL(),HP.getC()).
	    setXTitle("Charge (pC)");
	
        H_NOISE_Q.get(HP.getS(),HP.getL(),HP.getC()).
	    setYTitle("Counts");
        
        H_NPE_INT.add(HP.getS(),HP.getL(),HP.getC(),
                      new H1D(DetectorDescriptor.
                              getName("NPE integrated",
                                      HP.getS(),
				      HP.getL(),
				      HP.getC()),
                              HP.getTitle(),
                              100,0,100));
	
	
        H_NPE_INT.get(HP.getS(),HP.getL(),HP.getC()).
	    setFillColor(6);
	
        H_NPE_INT.get(HP.getS(),HP.getL(),HP.getC()).
	    setXTitle("npe (peak/gain)");
	
        H_NPE_INT.get(HP.getS(),HP.getL(),HP.getC()).
	    setYTitle("Counts");
        
        H_NPE_MATCH.add(HP.getS(),HP.getL(),HP.getC(),
                        new H1D(DetectorDescriptor.
                                getName("NPE int, matched layers",
                                        HP.getS(),
					HP.getL(),
					HP.getC()),
                                HP.getTitle(), 100,
                                0,
                                CosmicNPEXMax[HP.getL()]));
        
	H_NPE_MATCH.get(HP.getS(),HP.getL(),HP.getC()).
	    setFillColor(7);
	
        H_NPE_MATCH.get(HP.getS(),HP.getL(),HP.getC()).
	    setXTitle("npe (peak/gain)");
	
        H_NPE_MATCH.get(HP.getS(),HP.getL(),HP.getC()).
	    setYTitle("Counts");
        
        H_fADC.add(HP.getS(),HP.getL(),HP.getC(),
                   new H1D(DetectorDescriptor.
                           getName("fADC",
                                   HP.getS(),HP.getL(),HP.getC()),
                           HP.getTitle(), 100, 0.0, 100.0));
        
        H_MAXV.add(HP.getS(),HP.getL(), HP.getC(),
                   new H1D(DetectorDescriptor.
                           getName("WAVEMAX",
                                   HP.getS(),HP.getL(),HP.getC()),
			   HP.getTitle(), 130,
			   0.5*nGain_mV,
			   3.0*nGain_mV));
        
	H_MAXV.get(HP.getS(),HP.getL(), HP.getC()).
	    setFillColor(2);
	
        H_MAXV.get(HP.getS(),HP.getL(), HP.getC()).
	    setXTitle("Waveform Max (mV)");
	
        H_MAXV.get(HP.getS(),HP.getL(), HP.getC()).
	    setYTitle("Counts");
        
	H_MIP_V.add(HP.getS(),HP.getL(), HP.getC(),
		    new H1D(DetectorDescriptor.
			    getName("MIP WAVEMAX",
				    HP.getS(),
				    HP.getL(),
				    HP.getC()),
			    HP.getTitle(), 50, 100.0, 2000.));
	
        H_MIP_V.get(HP.getS(),HP.getL(), HP.getC()).
	    setFillColor(3);
	
        H_MIP_V.get(HP.getS(),HP.getL(), HP.getC()).
	    setXTitle("Waveform Max (mV)");
        
	H_MIP_V.get(HP.getS(),HP.getL(), HP.getC()).
	    setYTitle("Counts");
        
        H_TIME_MODE1.add(HP.getS(),HP.getL(), HP.getC(),
                         new H1D(DetectorDescriptor.
                                 getName("H_TIME_MODE1",
                                         HP.getS(),HP.getL(),HP.getC()),
                                 HP.getTitle(), 100, 0.0, 400));
        
        H_TIME_MODE1.get(HP.getS(),HP.getL(), HP.getC()).setFillColor(4);
        H_TIME_MODE1.get(HP.getS(),
			 HP.getL(), 
			 HP.getC()).setXTitle("Mode 1 Time (ns)");
        H_TIME_MODE1.get(HP.getS(),HP.getL(), HP.getC()).setYTitle("Counts");
        
	H_MAXV_VS_T.add(HP.getS(),
			HP.getL(), 
			HP.getC(),
			new H2D(DetectorDescriptor.
				getName("H_MAXV_VS_T",
					HP.getS(),HP.getL(),HP.getC()),
				HP.getTitle(),
				32,125.,275.,
				32,0.,2000.));
	
	//H_TIME_MODE1.get(HP.getS(),HP.getL(), HP.getC()).setFillColor(4);
        
	H_MAXV_VS_T.get(HP.getS(),
			HP.getL(), 
			HP.getC()).setXTitle("Mode 1 Time (ns)");
        H_MAXV_VS_T.get(HP.getS(),HP.getL(), 
			HP.getC()).setYTitle("Peak Voltage (mV)");
        
	if (HP.getL()==1){
            H_DT_MODE1.add(HP.getS(),
			   HP.getL(), 
			   HP.getC(),
			   new H1D(DetectorDescriptor.
				   getName("H_DT_MODE1",
					   HP.getS(),HP.getL(),HP.getC()),
				   HP.getTitle(), 56, -28, 28));
            H_DT_MODE1.get(HP.getS(),HP.getL(), 
			   HP.getC()).setFillColor(4);
            H_DT_MODE1.get(HP.getS(),
			   HP.getL(), 
			   HP.getC()).setXTitle("Mode One #Delta T (ns) [thick - thin]");
            H_DT_MODE1.get(HP.getS(),HP.getL(), 
			   HP.getC()).setYTitle("Counts");
	    
	    H_T1_T2.add(HP.getS(),
			   HP.getL(), 
			   HP.getC(),
			   new H2D(DetectorDescriptor.
				   getName("H_T1_T2",
					   HP.getS(),HP.getL(),HP.getC()),
				   HP.getTitle(), 
				   32, 125., 275.,
				   32, 125., 275.));
            
            H_T1_T2.get(HP.getS(),
			HP.getL(), 
			HP.getC()).setXTitle("Mode One Time (ns) [thin layer]");
            H_T1_T2.get(HP.getS(),HP.getL(), 
			HP.getC()).setYTitle("Mode One Time (ns) [thick layer]");
	    
	
	}
        
        
        H_COSMIC_fADC.add(HP.getS(),HP.getL(), HP.getC(),
                          new H1D(DetectorDescriptor.
                                  getName("Cosmic fADC",
                                          HP.getS(),HP.getL(),HP.getC()),
                                  HP.getTitle(), 100, 0.0, 100.0));
        H_COSMIC_fADC.get(HP.getS(),HP.getL(), 
			  HP.getC()).setFillColor(3);
        H_COSMIC_fADC.get(HP.getS(),HP.getL(), 
			  HP.getC()).setXTitle("fADC Sample");
        H_COSMIC_fADC.get(HP.getS(),HP.getL(), 
			  HP.getC()).setYTitle("fADC Counts");
    }
    
    private void setHistogramsCal(int index){
        char detector='c';
        
        HistPara HP = new HistPara();
        
        HP.setAllParameters(index,detector);
        
            //----------------------------
            // Event-by-Event Histograms
        
        H_FADC.add(HP.getS(),HP.getL(),HP.getC(),
                   new H1D(DetectorDescriptor.
                           getName("WAVE",HP.getS(),HP.getL(),HP.getC()),
                           HP.getTitle(),100, 0.0, 100.0));
        H_FADC.get(HP.getS(),HP.getL(),HP.getC()).setFillColor(4);
        H_FADC.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("fADC Sample");
        H_FADC.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("fADC Counts");
        
        H_VT.add(HP.getS(),HP.getL(),HP.getC(),
                    new H1D(DetectorDescriptor.
                            getName("Calibrated",HP.getS(),HP.getL(),HP.getC()),
                            HP.getTitle(), 100, 0.0, 400.0));
        H_VT.get(HP.getS(),HP.getL(),HP.getC()).setFillColor(3);
        H_VT.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("Time (ns)");
        H_VT.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("Voltage (mV)");
        
        H_NPE.add(HP.getS(),HP.getL(), HP.getC(),
                  new H1D(DetectorDescriptor.
                          getName("NPE",HP.getS(),HP.getL(),HP.getC()),
                          HP.getTitle(), 100, 0.0, 400.0));
        H_NPE.get(HP.getS(),HP.getL(),HP.getC()).setFillColor(5);
        H_NPE.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("Time (ns)");
        H_NPE.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("Voltage / SPE Voltage");
        
        
	//----------------------------
	// Accumulated Histograms
        
        H_MIP_Q.add(HP.getS(),HP.getL(),HP.getC(),
                    new H1D(DetectorDescriptor.
                            getName("Cosmic Charge",
                                    HP.getS(),HP.getL(),HP.getC()),
                            HP.getTitle(),
                            NBinsCosmic,
                            CosmicQXMin[HP.getL()],
                            CosmicQXMax[HP.getL()]));
        H_MIP_Q.get(HP.getS(),HP.getL(),HP.getC()).setFillColor(3);
        H_MIP_Q.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("Charge (pC)");
        H_MIP_Q.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("Counts");
        
        
        H_NOISE_Q.add(HP.getS(),HP.getL(),HP.getC(),
                      new H1D(DetectorDescriptor.
                              getName("Noise Charge",
                                      HP.getS(),HP.getL(),HP.getC()),
                              HP.getTitle(),
                              100,10.0,310.0));
        H_NOISE_Q.get(HP.getS(),HP.getL(),HP.getC()).setFillColor(5);
        H_NOISE_Q.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("Charge (pC)");
        H_NOISE_Q.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("Counts");
        
        H_NPE_INT.add(HP.getS(),HP.getL(),HP.getC(),
                      new H1D(DetectorDescriptor.
                              getName("NPE integrated",
                                      HP.getS(),HP.getL(),HP.getC()),
                              HP.getTitle(),
                              100,
                              CosmicNPEXMin[HP.getL()],
                              CosmicNPEXMax[HP.getL()]));
        
        H_NPE_INT.get(HP.getS(),HP.getL(),HP.getC()).setFillColor(6);
        H_NPE_INT.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("npe (peak/gain)");
        H_NPE_INT.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("Counts");
        
        H_NPE_MATCH.add(HP.getS(),HP.getL(),HP.getC(),
                        new H1D(DetectorDescriptor.
                                getName("NPE int, matched layers",
                                        HP.getS(),HP.getL(),HP.getC()),
                                HP.getTitle(), 100,
                                0,
                                CosmicNPEXMax[HP.getL()]));
        H_NPE_MATCH.get(HP.getS(),HP.getL(),HP.getC()).setFillColor(7);
        H_NPE_MATCH.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("npe (peak/gain)");
        H_NPE_MATCH.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("Counts");
        
        H_fADC.add(HP.getS(),HP.getL(),HP.getC(),
                   new H1D(DetectorDescriptor.
                           getName("fADC",
                                   HP.getS(),HP.getL(),HP.getC()),
                           HP.getTitle(), 100, 0.0, 100.0));
        
        H_MAXV.add(HP.getS(),HP.getL(), HP.getC(),
                   new H1D(DetectorDescriptor.
                           getName("WAVEMAX",
                                   HP.getS(),HP.getL(),HP.getC()),
			   HP.getTitle(), 130,
			   0.5*nGain_mV,
			   3.0*nGain_mV));
        
	H_MAXV.get(HP.getS(),HP.getL(), HP.getC()).
	    setFillColor(2);
	
        H_MAXV.get(HP.getS(),HP.getL(), HP.getC()).
	    setXTitle("Waveform Max (mV)");
	
        H_MAXV.get(HP.getS(),HP.getL(), HP.getC()).
	    setYTitle("Counts");
	
    }
    
    
    
    public void resetHistograms() {
        
        for (int index = 0; index < 232; index++)
            resetAllHistograms(index,'h');
        
        for (int index = 0; index < 505; index++)
            resetAllHistograms(index,'c');
    }
    
    public void resetAllHistograms(int index, char detector){
        
        HistPara HP =  new HistPara();
        
        HP.setAllParameters(index,detector);
        
        H_MIP_Q.get(HP.getS(),
                    HP.getL(),
                    HP.getC()).reset();
        
        H_NOISE_Q.get(HP.getS(),
                      HP.getL(),
                      HP.getC()).reset();
        
        H_NPE_INT.get(HP.getS(),
                      HP.getL(),
                      HP.getC()).reset();
        
        H_NPE_MATCH.get(HP.getS(),
                        HP.getL(),
                        HP.getC()).reset();
        
        
        H_fADC.get(HP.getS(),
                   HP.getL(),
                   HP.getC()).reset();
        
        H_COSMIC_fADC.get(HP.getS(),
                          HP.getL(),
                          HP.getC()).reset();
        
        H_MAXV.get(HP.getS(),
		   HP.getL(),
		   HP.getC()).reset();
	
        H_MIP_V.get(HP.getS(),
		    HP.getL(),
		    HP.getC()).reset();
        
    }
    
    
    public void initArrays() {
        
	pedMean  = new double[9][3][21];
	vMax     = new double[9][3][21];
	
        meanNPE  = new double[9][3][21];
        errNPE   = new double[9][3][21];
        sigNPE   = new double[9][3][21];
	
	gain     = new double[9][3][21];
        errGain  = new double[9][3][21];

	meanNPE_mV  = new double[9][3][21];
        errNPE_mV   = new double[9][3][21];
        gain_mV     = new double[9][3][21];
        errGain_mV  = new double[9][3][21];
        
	npeEvent = new double[9][3][21];

	boolean top = false;
	boolean bot = false;
        
        for (int s = 0; s < 9; s++) {
            for (int l = 0; l < 3; l++) {
                for ( int c = 0 ; c < 21 ; c++){
                    
		    this.pedMean[s][l][c]    = 0.0;
		    this.vMax[s][l][c]       = 0.0;
		    
		    this.meanNPE[s][l][c]    = 0.0;
                    this.errNPE[s][l][c]     = 0.0;
                    this.sigNPE[s][l][c]     = 100.0;
		    
		    this.gain[s][l][c]       = gainOld;
                    this.errGain[s][l][c]    = 0.0;

                    
		    this.meanNPE_mV[s][l][c]    = 0.0;
                    this.errNPE_mV[s][l][c]     = 0.0;
                    
		    this.gain_mV[s][l][c]    = gainOld_mV;
                    this.errGain_mV[s][l][c] = 0.0;

		    this.npeEvent[s][l][c]   = 0.0;                    
		    
		    if (runNumber > 750 ){
			this.gain[s][l][c]       = gainNew;
			this.gain_mV[s][l][c]    = gainNew_mV;
		    }
		    
		    // NEW CONVENTION 
		    // Mezzanine numbering [0,14] (not [1,15])
		    
		    if  ( getChan4SLC(s,l,c) <  8 )
			top = true;
		    else 
			bot = true;
		    
		    // Mezz 9 - includes S2 C20 
		    // tested with new SiPMS 
		    // in run 721
		    if     ( getMezz4SLC(s,l,c) == 9){
			
			if    ( runNumber > 710 ){
			    // new SiPMs with gain of 90
			    this.gain_mV[s][l][c] = gainNew_mV;
			    this.gain[s][l][c]    = gainNew;
			}
			
		    }
		    
		    if (getMezz4SLC(s,l,c) == 12 ){
					
			if     (top){
			    
			    if ( runNumber == 705  ){
				this.gain_mV[s][l][c] = gainNew_mV * 450/90;
				this.gain[s][l][c]    = gainNew * 450/90;
			    }
			    
			}
			else if(bot){
			    
			    if ( runNumber == 696){
				this.gain_mV[s][l][c] = gainOld_mV * 300/450;
				this.gain[s][l][c]    = gainOld * 300/450;
			    }
			    
			    if ( runNumber == 705  ){
				this.gain_mV[s][l][c] = gainNew_mV * 150/90;
				this.gain[s][l][c]    = gainNew * 150/90;
			    }
			    
						    
			}
		    }

		    if (getMezz4SLC(s,l,c) == 13 ){
						
			if     (top){
			    
			    if ( runNumber == 696 ){
				this.gain_mV[s][l][c] = gainNew_mV * 450/90;
				this.gain[s][l][c]    = gainNew * 450/90;
			    }
			    if ( runNumber == 705  ){
				this.gain_mV[s][l][c] = gainNew_mV * 450/90;
				this.gain[s][l][c]    = gainNew * 450/90;
			    }
			    
			}
			else if(bot){
			    
			    if ( runNumber == 696){
				this.gain_mV[s][l][c] = gainNew_mV * 150/90;
				this.gain[s][l][c]    = gainNew * 150/90;
			    }
			    
			    if ( runNumber == 705  ){
				this.gain_mV[s][l][c] = gainNew_mV * 300/90;
				this.gain[s][l][c]    = gainNew * 300/90;
			    }
			}

			if ( runNumber >= 721  ){
			    this.gain_mV[s][l][c] = gainNew_mV;
			    this.gain[s][l][c]    = gainNew;
			}			
		    }
		    
		    top = false;
		    bot = false;
		
		} // end: for ( int c = 0 ; c...
	    } // end: for (int l = 0; l < 3; l...
	} // end: for (int s = 0; s...
	
    }// end: public void initArra....

    
    
    public void processDecodedEvent(int repaintFrequency, int detType) {
        nProcessed++;
        
        List<DetectorCounter> counters;
        
        if(detType == 0)
            counters = decoder.getDetectorCounters(DetectorType.FTCAL);
        else
            counters = decoder.getDetectorCounters(DetectorType.FTHODO);
        
        //System.out.println("event #: " + nProcessed);
        
        FTHODOViewerModule.MyADCFitter fadcFitter;
        fadcFitter = new FTHODOViewerModule.MyADCFitter();
        
        int[][][]  time_M1 = new int[8][2][20];
	int[][]    dT      = new int[8][20];
        
        int nPosADC;
        int nNegADC;
        
        H_W_MAX.reset();
        H_V_MAX.reset();
        H_NPE_MAX.reset();
        
        //=-=-=-=-=-=-=-=-=-=-=-=-=-
        // Loop One
        for (DetectorCounter counter : counters) {
            
            if(counter.getDescriptor().getType() != (DetectorType.FTHODO))
                break;
            
            fadcFitter.fit(counter.getChannels().get(0));
	    
            int sec = counter.getDescriptor().getSector();
            int lay = counter.getDescriptor().getLayer();
            int com = counter.getDescriptor().getComponent();
            
            int index = getIndex4SLC(sec,lay,com);
            
            short  pulse[] = counter.getChannels().get(0).getPulse();
            double npeWave;
            double calibratedWave;
            double baselineSubRaw;
            
                // reset non-accumulating histograms
            H_FADC.get(sec, lay, com).reset();
            H_VT.get(sec, lay, com).reset();
            H_NPE.get(sec, lay, com).reset();
            
            
	    //===============================
	    // npe for this event only
	    // to be used in loop two below
            npeEvent[sec][lay][com] = 0.0;
            
            if(gain[sec][lay][com] > 0.0){
                
                npeEvent[sec][lay][com] = counter.getChannels().
                get(0).getADC().get(0)*LSB*4.0/50./gain[sec][lay][com];
                
                H_NPE_INT.get(sec, lay, com).fill(npeEvent[sec][lay][com]);
                
            }
		    
            H_PED.get(sec,lay,com).fill(fadcFitter.getPedestal());
	    
	    // Loop through fADC bins filling event-by-event histograms
            for (int i = 0;
                 i < min(pulse.length,
                         H_fADC.get(sec,lay,com).getAxis().getNBins());
                 i++) {
                
                
                H_FADC.get(sec,lay,com).fill(i, pulse[i]);
                
                baselineSubRaw = pulse[i] - fadcFitter.getPedestal() + 10.0;
                H_fADC.get(sec,lay,com).fill(i,baselineSubRaw);
                
                calibratedWave = (pulse[i]-fadcFitter.getPedestal())*LSB + 5.0;
                H_VT.get(sec,lay,com).fill(i*4,calibratedWave);
                
                npeWave = (pulse[i] - fadcFitter.getPedestal())*LSB/voltsPerSPE;
                H_NPE.get(sec, lay, com).fill(i*4, npeWave);
            }
            
            
        }
        
        
            //=-=-=-=-=-=-=-=-=-=-=-=-=-
            // Loop Two
        for (DetectorCounter counter : counters) {
            
            fadcFitter.fit(counter.getChannels().get(0));
            
            int sec = counter.getDescriptor().getSector();
            int lay = counter.getDescriptor().getLayer();
            int com = counter.getDescriptor().getComponent();
            int opp = (lay%2)+1;
            
            int index;
            
            if(counter.getDescriptor().getType() == DetectorType.FTHODO){
		
		index = getIndex4SLC(sec,lay,com);
            }
            else{
                
		index  = com;
                sec    = 0;
                lay    = 0;
            }
            
            H_MIP_Q.get(sec, lay, com)
            .fill(counter.getChannels().get(0).getADC().get(0)*LSB*4.0/50);
            
            H_NOISE_Q.get(sec, lay, com)
            .fill(counter.getChannels().get(0).getADC().get(0)*LSB*4.0/50);
            
	    // Matching Hits in layers
            if( detType == 1              &&
		gain[sec][lay][com] > 0.0 &&
		gain[sec][opp][com] > 0.0){
                
                double npeOtherLayer  = npeEvent[sec][opp][com];
                
                double meanNPEOther   = meanNPE[sec][opp][com];
		
                double sigNPEOther    = sigNPE[sec][opp][com];
                double npeLowLimOther = meanNPEOther - abs(sigNPEOther);
                
                if ( npeLowLimOther < 5.0 )
                    npeLowLimOther = 5.0;
                
                if ( npeOtherLayer > npeLowLimOther )
                    H_NPE_MATCH.get(sec, lay, com)
                    .fill(npeEvent[sec][lay][com]);
                
            }
            	    
            double waveMax = 0.;
	    waveMax =  - fadcFitter.getPedestal();
            waveMax = waveMax + fadcFitter.getWave_Max();
	    
	    double voltMax = waveMax*LSB;
            
	    double npeMax  = 0.;
	    
	    if(detType == 1)
		npeMax = voltMax/gain_mV[sec][lay][com];
	    
            H_W_MAX.fill(index,waveMax);
            H_V_MAX.fill(index,voltMax);
            H_NPE_MAX.fill(index,npeMax);
            
	    
//             if ( voltMax > 10 )
		
// 		//!!!!!
// 		if( voltMax > 700. && 
// 		    detType == 1   && 
// 		    (
// 		     getMezz4SLC(sec,lay,com) > 1 
// 		     )
// 		    ){
		    
// 		    System.out.println(" =========================== " );
// 		    System.out.println(" voltMax = " + voltMax );
		    
// 		    System.out.println(" sector  = " + sec  );
// 		    System.out.println(" layer   = " + lay  );
// 		    System.out.println(" comp    = " + com  );
		    
		
// 		    System.out.println(" Channel = " +
// 				       getChan4SLC(sec,lay,com) );
		    
// 		    System.out.println(" Mezzanine = " +
// 				       getMezz4SLC(sec,lay,com) );
		    

// 		}
	    
	    H_MAXV.get(sec, lay, com).
		fill(voltMax);
	    
	    if( lay > 0 &&
		voltMax > vMax[sec][lay][com])
		vMax[sec][lay][com] = voltMax;
	    
            if (lay > 0 && fadcFitter.getADCtime() > 10){
		
		time_M1[sec-1][lay-1][com-1] = fadcFitter.getADCtime()*4;
		
		H_TIME_MODE1.get(sec, 
				 lay, 
				 com).fill(time_M1[sec-1][lay-1][com-1]);
		
		H_MAXV_VS_T.get(sec,
				lay,
				com).fill(time_M1[sec-1][lay-1][com-1],
					  voltMax);
            }
	    
	    
	    if( detType == 1 )
		H_MIP_V.get(sec, lay, com)
		    .fill(voltMax);
	    
        } // end of: for (DetectorCounter counter : counters) {
                
        for (int secM = 0; secM < 8; secM++) {
            for (int comM = 0; comM < 20; comM++) {
                
		// odd sectors have only 9 elements
		if ((secM+1)%2==1 && comM > 8)
                    continue;
		
                if (time_M1[secM][1][comM] > 0 &&
                    time_M1[secM][0][comM] > 0){
		    
		    dT[secM][comM] =  - time_M1[secM][1][comM];
		    dT[secM][comM] += time_M1[secM][0][comM];
					       
		    H_DT_MODE1.get(secM+1, 1, comM+1).
			fill(dT[secM][comM]);
		    
		    H_T1_T2.get(secM+1, 1, comM+1).
			fill(time_M1[secM][0][comM],
			     time_M1[secM][1][comM]);
		    
		}
	    }
	}
	
        //=======================================================
        //             DRAW HISTOGRAMS PER EVENT
        //=======================================================
        //   User chooses which histogram/s to display
        
        // map [1,2] to [0,1]
        int layCD = laySel-1;
        // map [1,2] to [1,0]
        int oppCD = laySel%2;
        // map [1,2] to [2,1]
        int oppSel = (laySel%2)+1;
        
        boolean skip = false;
	
        // paddles (calorimeter )
        if(laySel==0) {
            if(comSel < 501) skip = true;
            layCD  = 0;
            oppCD  = 1;
            oppSel = 0;
        }
        
        int layCDL =  2*layCD;
        int oppCDL =  2*oppCD;
        
        int layCDR = layCDL + 1;
        int oppCDR = oppCDL + 1;
        
        if( laySel == 0 )
            return;
        
        //============================================================
        // Event Tab Selected
        
        if     ( tabSelect == tabIndexEvent &&
                (nProcessed%repaintFrequency==0) ) {
            
            drawCanvasEvent(secSel,
			    laySel,
			    comSel);
        }
        else if( tabSelect == tabIndexNoise &&
                (nProcessed%(10*repaintFrequency)==0) ) {
            
            drawCanvasNoise(secSel,
			    laySel,
			    comSel);
            
        }
        else if( tabSelect == tabIndexPed &&
		 (nProcessed%(10*repaintFrequency)==0) ) {
            
            drawCanvasPed(secSel,
			  laySel,
			  comSel);
            
        }
        else if( tabSelect == tabIndexCharge  &&
                (nProcessed%(10*repaintFrequency)==0) ) {
            
            drawCanvasCharge(secSel,
                             laySel,
                             comSel);
            
        }
	//==========================================
        
        if(nProcessed%repaintFrequency==0)
            this.view.repaint();
    }
    
    
    public void hashTableCallback(String string, Long l) {
        System.out.println("Selected table row " + string + " " + l);
    }
    
    public void initDecoder() {
        decoder.addFitter(DetectorType.FTHODO,
                          new FADCBasicFitter(ped_i1,
                                              ped_i2,
                                              pul_i1,
                                              pul_i2
                                              ));
        
        decoder.addFitter(DetectorType.FTCAL,
                          new FADCBasicFitter(ped_i1,
                                              ped_i2,
                                              pul_i1,
                                              pul_i2
                                              ));
        
    }
    
    public class HistPara extends Object {
        
        public int    layer;
        public int    quadrant;
        public int    element;
        public int    sector;
        public int    component;
        public String title;
        public String layerStr;
        
        public void setAllParameters(int  index,
                                     char detector){
            
            if(detector == 'h' ){
                layer = index/116 + 1;
                
                if (layer==1)
                    layerStr = "Thin";
                else
                    layerStr = "Thick";
                
                    // (map indices in both layers to [0,115])
                    // /map indices to quadrants [0,3]
                quadrant = (index-(layer-1)*116) / 29;
                
                    // map indices to [0,28]
                element = index - quadrant * 29 -(layer-1)*116;
                
                    // map quadrant to sectors [1,8]
                    // map element to tiles [1,9] or
                    // map element to tiles [1,20]
                
                if (element < 9) {
                    sector    = quadrant*2 + 1;
                    component = element + 1;
                }
                else {
                    sector    = quadrant*2 + 2;
                    component = element + 1 - 9;
                }
                title =  " " + layerStr + " S" + sector + " C" + component ;
                
            }
            else{
                layer     = 0;
                quadrant  = 0;
                element   = index;
                component = index;
                sector    = 0;
                
                if     (component==501)
                    layerStr = "Top Long Paddle";
                else if(component==502)
                    layerStr = "Bottom Long Paddle";
                else if(component==503)
                    layerStr = "Top Short Paddle";
                else if(component==504)
                    layerStr = "Bottom Short Paddle";
                
                title =  " " + layerStr;
                
            }
            
        }
        
        
        public int getL(){
            return layer;
        }
        
        public int getQuad(){
            return quadrant;
        }
        
        public int getElem(){
            return element;
        }
        
        public int getS(){
            return sector;
        }
        
        public int getC(){
            return component;
        }
        
        public String getTitle(){
            return title;
        }
        
        public String getLayerStr(){
            return layerStr;
        }
    }
    
    
    public class MyADCFitter implements IFADCFitter {
        
        double rms = 0;
        double pedestal = 0;
        double wave_max=0;
        int    fadctime=0;
        
        public double getPedestal() {
            return pedestal;
        }
        
        public double getRMS() {
            return rms;
        }
        
        public double getWave_Max() {
            return wave_max;
        }
        public int getADCtime() {
            return fadctime;
        }
        
        public void fit(DetectorChannel dc) {
            short[] pulse = dc.getPulse();
	    
	    // pedestal calculated using bins ped_i1, ped_i2
	    double pedlow = 0.0;
	    // pedestal calculated using bins ped_j1, ped_j2
            double pedhigh = 0.0;
                        
	    double noise = 0;
            
	    double wmax = 0;
	    
            for (int bin = ped_i1; bin < ped_i2; bin++) {
                pedlow += pulse[bin];
                noise  += pulse[bin] * pulse[bin];
            }
            
            for (int bin = ped_j1; bin < ped_j2; bin++) {
                pedhigh += pulse[bin];
            }
	    
	    // find pedestal average
            if (pedlow < pedhigh)
                pedestal = pedlow / (ped_i2 - ped_i1);
            else
                pedestal = pedhigh / (ped_j2 - ped_j1);

	    // find pulse maximum
            for (int bin = 0 ; bin < pulse.length ; bin++)
		if( pulse[bin] > wmax ) 
		    wmax = pulse[bin];
	    
	    int timeAt100 = 0;
	    
	    // record time when signal goes above
	    // pedestal plus 100 bins
	    // if pulse maximum is greater than threshold 
	    
	    for (int bin = 0; bin < pulse.length ; bin++) {
                
		if( ( ( pulse[bin] - pedestal ) > 100 )  &&
		    ( ( wmax - pedestal ) > cosmicsThrsh ) ){
                    
		    if ( timeAt100 == 0 )
			timeAt100 = bin;

		}
            
	    }
	    
	    // Returns the smallest pedestal value.
	    // Works better if peak is close to the 
	    // beginning of the histogram.
	    
            rms = LSB * sqrt(noise / (ped_i2 - ped_i1) - pedestal * pedestal);
            wave_max = wmax;
            fadctime = timeAt100;
       
	    //==================================================
	    
	    
	    //==================================================
	    
	}	
    }
    
}