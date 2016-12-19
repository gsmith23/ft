package org.clas.fthodo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static java.lang.Math.*;
import java.lang.String;

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
    HashTable            ccdbTable = null;
    
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
        
    // Semi Accumulated
    DetectorCollection<H1D> H_PED_TEMP = new DetectorCollection<H1D>();
    
    // Accumulated
    DetectorCollection<H1D> H_PED = new DetectorCollection<H1D>();

    DetectorCollection<GraphErrors> H_PED_VS_EVENT = new DetectorCollection<GraphErrors>();
    DetectorCollection<GraphErrors> H_PED_INDEX;
    
    DetectorCollection<H1D> H_NOISE_V = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_MIP_V   = new DetectorCollection<H1D>();
    
    DetectorCollection<H1D> H_NOISE_Q = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_MIP_Q   = new DetectorCollection<H1D>();
        
    DetectorCollection<H1D> H_NPE_INT   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NPE_NOISE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NPE_MATCH = new DetectorCollection<H1D>();
    
    DetectorCollection<H1D> H_TIME_MODE3 = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_DT_MODE3   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_DT_MODE7   = new DetectorCollection<H1D>();
    
    // 2D
    DetectorCollection<H2D> H_MAXV_VS_T = new DetectorCollection<H2D>();
    DetectorCollection<H2D> H_T1_T2     = new DetectorCollection<H2D>();

    // 1D
    DetectorCollection<H1D> H_COSMIC_fADC   = new DetectorCollection<H1D>();
    
    // Fit Functions 
    DetectorCollection<F1D> fPed  = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fQ1   = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fQ2   = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fQMIP = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fV1   = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fV2   = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fVMIP = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fDT   = new DetectorCollection<F1D>();
    
    // Functions (that are not used to fit)
    DetectorCollection<F1D> fThr   = new DetectorCollection<F1D>();
    DetectorCollection<F1D> fVThr  = new DetectorCollection<F1D>();
    
    DetectorCollection<Integer> dcHits = new DetectorCollection<Integer>();
    
    H1D H_W_MAX      = null;
    H1D H_V_MAX      = null;
    H1D H_NPE_MAX    = null;
    H1D H_CHARGE_MAX = null;
    
    //=================================
    //           ARRAYS
    //=================================
    
    private double[][][] status;
    private double[][][] thrshNPE;
    
    private double[][][] ped;

    private double[][][] pedMean;
    
    private double[][][] gain;
    private double[][][] errGain;
    
    private double[][][] gain_mV;
    private double[][][] errGain_mV;
    
    private double[][][] meanNPE;
    private double[][][] errNPE;
    private double[][][] sigNPE;
    
    private double[][][] meanNPE_mV;
    private double[][][] errNPE_mV;
    private double[][][] sigNPE_mV;

    
    // not ccdb constants
    private double[][][] npeEvent;

    private double[][][] vMax;
    private double[][][] vMaxEvent;
    
    private double[][][] qMax;
    
    boolean testMode = false;
    boolean debugging_mode = false;
    
    //=================================
    //           CONSTANTS
    //=================================
    
    boolean fitTwoPeaksV = false;
    boolean fitTwoPeaksQ = false;
    
    final double nStatus     = 5.0;
    final double nThrshNPE   = 2.5;
    final double nPed        = 200.0;
    final double nPed_RMS    = 10.0;
    final double nGain       = 20.0;
    final double nErrGain    = 0.0;
    final double nGain_mV    = 10;
    final double nErrGain_mV = 0.0;
    
    final double nMipCThin   = 700.;   
    final double nMipCThick  = 1500.;   
    final double nTOff       = 0.0;    
    final double nTRes       = 1.0;    
    
    final double[] nMipE   = {0.0,1.2 ,2.65 };    
    final double[] nMipQ   = {0.0,700.,1500.};    
    
    int    fADCBins      = 4096;
    double voltageMax    = 2000.;
    double LSB           = voltageMax/fADCBins;
    
    
    final double thrshNoiseNPE = 0.5;
    
    double voltsPerSPE   = 10.;
    double binsPerSPE    = voltsPerSPE/LSB;
    
    double thrshVolts    = nThrshNPE*voltsPerSPE;
    double noiseThrshV   = thrshNoiseNPE*voltsPerSPE;
    
    double cosmicsThrsh  = thrshVolts/LSB;
    double noiseThrsh    = noiseThrshV/LSB;
    

    //==================
    // greater than 2.5 p.e.
    // (pe * v/pe * bins /v )
    // = 51 
    double threshD   = nThrshNPE * 10.0 / LSB; 
    int    threshold;
    
    double nsPerSample = 4;
    //=================

    int ped_i1 = 4;
    int ped_i2 = 24;
    int pul_i1 = 30;
    int pul_i2 = 70;
    
    final boolean fitBackground = false;
    final int NBinsCosmic = 64;
    
    final int CosmicQXMin[]  = {0,200,300};
    final int CosmicQXMax[]  = {10000,5200,5300};
    
    final int CosmicNPEXMin[]  = {0,3,5};
    final int CosmicNPEXMax[]  = {200,93,133};
    
    boolean simulatedAnalysis = true;
    boolean useDefaultGain    = true;
    
    double NoiseQXMin[] = {0.  ,0.5*nGain,0.5*nGain};
    double NoiseQXMax[] = {310.,3.0*nGain,3.0*nGain};
    
    double NoiseVXMin[] = {0.  ,0.5*nGain_mV,0.5*nGain_mV};
    double NoiseVXMax[] = {310.,3.0*nGain_mV,3.0*nGain_mV};

    int[] NBinsNoise = {100,100,100};
    
    final int NBinsPed = 1050; 
    //pedestal min and max bin values for histogram
    double[] PedQX = {50, 400}; 
    double PedBinWidth=(PedQX[1]-PedQX[0])/(1.0*NBinsPed);
    
    // number of points in pedestal vs event index 
    final int      nPointsPed = 200;  
    double[]       px_H_PED_VS_EVENT  = new double[nPointsPed];
    double[]       pex_H_PED_VS_EVENT = new double[nPointsPed];
    double[][][][] py_H_PED_VS_EVENT  = new double[8][2][20][nPointsPed];
    double[][][][] pey_H_PED_VS_EVENT = new double[8][2][20][nPointsPed];
    
    //=================================
    //           VARIABLES
    //=================================
    
    double   tile_size = 15;
    int      nDecodedProcessed = 0;
    
    private int secSel = 5;
    private int laySel = 2;
    private int comSel = 1;    
    private int indexSel = 0;
    
    private boolean drawByElec = true;
    private boolean useGain_mV = false;
    
    JPanel rBPane;
    int    previousTabSel = 0;

    private int tabSel = 4;

    // the following indices must
    // correspond to the order the
    // columns are added to the 
    // ccdb table
    
    final private int statCCDB   = 3;
    final private int npeThCCDB  = 4;
    final private int pedCCDB    = 5;
    final private int pedRMSCCDB = 6;
    final private int gainPCCCDB = 7;
    final private int gainMVCCDB = 8;
    final private int mipEnCCDB  = 9;
    final private int mipChCCDB  = 10;
    final private int tOffCCDB   = 11;
    final private int tResCCDB   = 12;

    
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

        JSplitPane splitPane = new JSplitPane();
        
        this.initTable();
        this.initConstants();
	
        HashTableViewer canvasTable = new HashTableViewer(ccdbTable);
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
        tabbedPane.setSelectedIndex(this.tabSel);
        this.initCanvas();
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        
        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(this);
        buttonPane.add(resetBtn);
        
        JButton fitBtn = new JButton("Fit");
        fitBtn.addActionListener(this);
        buttonPane.add(fitBtn);
        
        JButton variablesBtn = new JButton("Constants");
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
	//  rBGainPeak.isSelected());
	    
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
        this.canvasEvent.divide(2,2);
	
	drawCanvasEvent(secSel,laySel,comSel);
        
	this.canvasPed.setGridX(false);
	this.canvasPed.setGridY(false);
        this.canvasPed.setAxisFontSize(10);
        this.canvasPed.setTitleFontSize(16);
        this.canvasPed.setAxisTitleFontSize(14);
        this.canvasPed.setStatBoxFontSize(8);
        this.canvasPed.divide(3,2);
        
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
    
//     final double nStatus     = 2.0;
//     final double nThrshNPE   = 2.5;
//     final double nPed        = 200.0;
//     final double nPed_RMS    = 10.0;
//     final double nGain       = 20.0;
//     final double nErrGain    = 0.0;
//     final double nGain_mV    = 10;
//     final double nErrGain_mV = 0.0;
//     final double nMipEThin   = 1.2;    
//     final double nMipEThick  = 2.65;    
//     final double nMipCThin   = 700.;   
//     final double nMipCThick  = 1500.;   
//     final double nTOff       = 0.0;    
//     final double nTRes       = 1.0;    
    
    private void initConstants(){
	
	int s,l,c;
	
	HistPara HP =  new HistPara();
	
	double mipE = 0.0;
	double mipC = 0.0;
	    
	for (int index = 0; index < 232; index++) {
	    HP.setAllParameters(index,'h');
	    
	    s = HP.getS();
	    l = HP.getL();
	    c = HP.getC();
	    
	    mipE = 2.0;
	    mipC = 1000.0;
	    
	    if      (l==1){
		mipE = mipE*0.7;
		mipC = mipC*0.7;
	    }
	    else if (l==2){
		mipE = mipE*1.5;
		mipC = mipC*1.5;
	    }
	    
	    initThresholdParameters(s,l,c);
	
	}
	
    }
    

    @SuppressWarnings("empty-statement")
    private void initTable() {
        ccdbTable = new HashTable(3,
				  "STATUS:d",          // 3
				  "NPE_THRESHOLD:d",   // 4 
				  "PEDESTAL:d",        // 5
				  "PEDESTAL_RMS:d",    // 6
				  "GAIN_PC:d",         // 7
				  "GAIN_MV:d",         // 8
				  "MIPS_ENERGY:d",     // 9
				  "MIPS_CHARGE:d",     // 10
				  "TIME_OFFSET:d",     // 11
				  "TIME_RESOLUTION:d");// 12
		
	
        double[] tableInitialValues = {0.0, nThrshNPE, 200., 10., 20,
				       10., 1.4, 700., 99.9, 99.9};

        
        for (int layer = 2; layer > 0; layer--) {
            for (int sector = 1; sector < 9; sector++) {
                for ( int component = 1 ; component < 21 ; component++){
                    
                    if(sector%2==1 && component > 9 ) continue;
		    
                    ccdbTable.addRow(tableInitialValues,
				     sector,
				     layer,
				     component);
                    
		    ccdbTable.addConstrain(statCCDB, -0.5, 0.5);
		    ccdbTable.addConstrain(npeThCCDB, 2.0, 3.0);
		    
		    ccdbTable.addConstrain(pedCCDB,  100.0, 400.0);
		    ccdbTable.addConstrain(pedRMSCCDB, 1.0, 100.0);
                    
		    ccdbTable.addConstrain(gainPCCCDB, 10.0, 30.0);
                    ccdbTable.addConstrain(gainMVCCDB,  5.0, 15.0);

		    ccdbTable.addConstrain(mipEnCCDB, 1.0, 4.0);
		    ccdbTable.addConstrain(mipChCCDB, 500, 3000);
		    
		    ccdbTable.addConstrain(tOffCCDB, -2.0, 2.0);
		    ccdbTable.addConstrain(tResCCDB, -5.0, 5.0);
		    
		    
                }
            }
        }
        
    } // end of : private void initTable() {
    
    
    public FTHODOViewerModule(){
	
	System.out.println( " -------------------");
	System.out.println( " FTHODOViewerModule ");
	System.out.println( " -------------------");

        this.threshold = (int)threshD;

        this.detectorPanel = null;
        this.decoder = null;

    }

    public void initDetector(){

        DetectorShapeView2D viewFTHODO = this.drawDetector(0.0, 0.0);
        this.view.addDetectorLayer(viewFTHODO);

	DetectorShapeView2D viewChannels = this.drawChannels(0.0, 0.0);
        this.view.addDetectorLayer(viewChannels);
	
	// Gagik to implement
        // viewFTHODO.addActionListener(this);
	
	// DetectorShapeView2D viewPaddles = this.drawPaddles(0.0, 0.0);
	// this.view.addDetectorLayer(viewPaddles);
	
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
        if (e.getActionCommand().compareTo("Constants") == 0) {
            updateConstants();
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
    
    private boolean initFitPedestalParameters(int s,int l,int c,H1D H1){
	
	if (H1.integral() > 100 ){
	    fPed.add(s,l,c, new F1D("gaus",0,400));
	    fPed.get(s,l,c).setParameter(0,H1.getBinContent(H1.getMaximumBin()));
	    fPed.get(s,l,c).setParameter(1,H1.getMean());
	    fPed.get(s,l,c).setParameter(2,H1.getRMS());
	    
	    fPed.get(s,l,c).setParLimits(0, 0, 2*H1.getBinContent(H1.getMaximumBin()));
	    fPed.get(s,l,c).setParLimits(0, 0, 400);
	    fPed.get(s,l,c).setParLimits(0, 0, 5*H1.getRMS());
	    
	    return true;
	}
	else
	    return false;
    }

    private void fitPedestals(int s,int l,int c,String fitOption){
	
	if (testMode)
	    System.out.println(" Fitting Pedestal (S,L,C) = (" + 
			       s  + "," + l + "," + c + ")");

	if (initFitPedestalParameters(s,l,c,H_PED.get(s,l,c))){
	    H_PED.get(s,l,c).fit(fPed.get(s,l,c),fitOption);
	    
	    if (testMode)
		System.out.println(" Fitted Pedestal (S,L,C) = (" + 
				   s  + "," + l + "," + c + ")");
	}
	else
	    if (testMode)
		System.out.println(" No Pedestal Fit (S,L,C) = (" + 
				   s  + "," + l + "," + c + ")");
    }
    
    private boolean initFitQNoiseParameters(int s,int l,int c,H1D H1){
	
	if(testMode)
	    System.out.println(" initFitQNoiseParameters start ") ;
	
	double ampl = H1.getBinContent(H1.getMaximumBin());
	double mean = H1.getMaximumBin();
	mean = mean * H1.getAxis().getBinWidth(2);
	mean = mean + H1.getAxis().min();
	double std  = 5.0;
	
	if(testMode)
	    System.out.println(" initFitQNoiseParameters variables initialised ") ;
		
        if (H1.getEntries() > 500){

	    if(testMode)
		System.out.println(" initFitQNoiseParameters setting fQ1 parameters ") ;
	    
	    
	    fQ1.add(s,l,c,new F1D("exp+gaus", 
				  H1.getAxis().min(),  
				  nGain*1.5));
	    
	    // exponential
	    fQ1.get(s,l,c).setParameter(0, ampl/5.);
            fQ1.get(s,l,c).setParameter(1, -0.001);
        
	    // gaus 1
	    fQ1.get(s,l,c).setParameter(2, ampl);
            fQ1.get(s,l,c).setParameter(3, nGain);
            fQ1.get(s,l,c).setParameter(4, std);

	    if(testMode)
		System.out.println(" initFitQNoiseParameters setting fQ1 limits ") ;
	    
	    // exponential
	    fQ1.get(s,l,c).setParLimits(0, ampl/10.0, 5.0*ampl);
            fQ1.get(s,l,c).setParLimits(1, -5.0, -0.0001);
            	    
	    // gaus 1
	    fQ1.get(s,l,c).setParLimits(2, ampl/2., ampl*2);
            fQ1.get(s,l,c).setParLimits(3, 0.5*nGain, 1.5*nGain);
            fQ1.get(s,l,c).setParLimits(4, 1, std/2.);
	    
	    fQ1.get(s,l,c).setLineColor(1);
	    
	    if(testMode)
		System.out.println(" initFitQNoiseParameters setting fQ2 parameters ") ;

	    fQ2.add(s,l,c, 
		    new F1D("gaus", 1.5*nGain, 2.5*nGain));
	    
	    fQ2.get(s,l,c).setParameter(0, ampl/5.0);
	    fQ2.get(s,l,c).setParameter(1, 2.0*nGain);
	    fQ2.get(s,l,c).setParameter(2, std);
            
	    if(testMode)
		System.out.println(" initFitQNoiseParameters setting fQ2 limits ") ;
		    
	    fQ2.get(s,l,c).setParLimits(0, 0, ampl/2.0);
	    fQ2.get(s,l,c).setParLimits(1, mean+20, mean+100);
	    fQ2.get(s,l,c).setParLimits(2, 1, std*3.0);
	    
	    fQ2.get(s,l,c).setLineColor(1);
	    
	    return true;

	}
	else{
	    if(testMode)
		System.out.println(" initFitQNoiseParameters insufficient entries ") ;
	    
	    return false;
	
	}
    }

    private void fitQNoise(int s,int l,int c,String fitOption){

	if (testMode)
	    System.out.println(" Fitting Q Noise(S,L,C) = (" +
			       s  + "," + l + "," + c + ")"); 
	
	if (initFitQNoiseParameters(s,l,c,H_NOISE_Q.get(s,l,c))){
	    
	    H_NOISE_Q.get(s,l,c).
		fit(fQ1.get(s,l,c),fitOption);
	    
	    if(this.fitTwoPeaksQ){
		H_NOISE_Q.get(s,l,c).
		    fit(fQ2.get(s,l,c),fitOption);
	    }
	    
	    if (testMode)
		System.out.println(" Fitted Q Noise 1 & 2 (S,L,C) = (" + 
				   s  + "," + l + "," + c + ")");
	}
	else
	    if (testMode)
		System.out.println(" No Q Noise 1 & 2 Fits (S,L,C) = (" +
				   s  + "," + l + "," + c + ")");
    }
	
    private boolean initFitVNoiseParameters(int s,int l,int c,H1D H1){
	
	if(testMode)
	    System.out.println(" initFitVNoiseParameters start ") ;
	
	double ampl = H1.getBinContent(H1.getMaximumBin());
	double mean = H1.getMaximumBin();
	mean = mean * H1.getAxis().getBinWidth(2);
	mean = mean + H1.getAxis().min();
	double std  = 0.5;
	
	if(testMode)
	    System.out.println(" initFitVNoiseParameters variables" +
			       " initialised ") ;

	if (H1.getEntries() > 200){
	    
	    if(testMode)
		System.out.println(" initFitVNoiseParameters " +
				   " setting fV1 parameters ") ;
	    
            fV1.add(s,l,c, new F1D("gaus+exp", 
				   H1.getAxis().min(), 
				   1.5*nGain_mV));
            
	    fV1.get(s,l,c).setParameter(0, ampl);
            fV1.get(s,l,c).setParameter(1, nGain_mV);
            fV1.get(s,l,c).setParameter(2, std);
            fV1.get(s,l,c).setParameter(3, ampl/5.0);
            fV1.get(s,l,c).setParameter(4, -0.001);

	    if(testMode)
		System.out.println(" initFitVNoiseParameters setting " +
				   " fV1 limits ") ;
	    
            
            fV1.get(s,l,c).setParLimits(0, ampl/2, ampl*2);
            fV1.get(s,l,c).setParLimits(1, 
					H1.getAxis().min(), 
					2*nGain_mV);
            fV1.get(s,l,c).setParLimits(2, 0, std*4.0);
            fV1.get(s,l,c).setParLimits(3, 0, ampl*5.);
            fV1.get(s,l,c).setParLimits(4, -5, -0.0001);

	    if(testMode)
		System.out.println(" initFitVNoiseParameters setting " +
				   " fV2 parameters ") ;
            
	    fV2.add(s,l,c, new F1D("gaus",
				   1.5*nGain_mV, 
				   3.0*nGain_mV));
	    
	    fV2.get(s,l,c).
		setParameter(0, ampl/3.0);
	    fV2.get(s,l,c).
		setParameter(1, 2.0*nGain_mV);
	    fV2.get(s,l,c).
		setParameter(2, std);
	    
	    if(testMode)
		System.out.println(" initFitVNoiseParameters setting" +
				   " fV2 limits ") ;
	    
	    fV2.get(s,l,c).
		setParLimits(0, 0, ampl);
	    fV2.get(s,l,c).
		setParLimits(1,
			     1.5*nGain_mV,
			     2.5*nGain_mV);
	    fV2.get(s,l,c).
		setParLimits(2, 0., std*4.0);
	    
	    return true;
        }
	else{
	    if(testMode)
		System.out.println(" initFitVNoiseParameters insufficient"+
				   " entries ") ;
	    
	    return false;
	
	}
	
    }

    private void fitVNoise(int s,int l,int c,String fitOption){

	if (testMode)
	    System.out.println(" Fitting V Noise (S,L,C) = (" +
			       s  + "," + l + "," + c + ")"); 
	
	if (initFitVNoiseParameters(s,l,c,H_NOISE_V.get(s,l,c))){
	    
	    if (testMode)
		System.out.println(" Fitting fV1 ");
	    
	    H_NOISE_V.get(s,l,c).
		fit(fV1.get(s,l,c),fitOption);
	    
	    if( this.fitTwoPeaksV ){
		
		if (testMode)
		    System.out.println(" Fitting fV2 ");
		
		H_NOISE_V.get(s,l,c).
		    fit(fV2.get(s,l,c),fitOption);
	    }
	    else{
		if (testMode)
		    System.out.println(" Skipping fV2 Fit ");
		
	    }
	    
	    if (testMode)
		System.out.println(" Fitted V Noise 1 & 2 (S,L,C) = (" + 
				   s  + "," + l + "," + c + ")");
	}
	else
	    if (testMode)
		System.out.println(" No V Noise 1 & 2 Fits (S,L,C) = (" +
				   s  + "," + l + "," + c + ")");
    }

    private boolean initFitQMIPParameters(int s,int l,int c,H1D H1){
	
	double[] rangeLow  = {0.0, 200.,  500};
	double[] rangeHigh = {0.0, 2500., 4000};
        	
	int integralLowBin  = (int)(rangeLow[l] - CosmicQXMin[l])*NBinsCosmic;
        integralLowBin  = integralLowBin/((int)CosmicQXMax[l]-(int)CosmicQXMin[l]);
	
        int integralHighBin = NBinsCosmic-1;
	
	
        if (H1.integral(integralLowBin,
			integralHighBin ) > 25){
	    
            double ampl = 0;
            double mean = 0;
	    
	    for (int i = integralLowBin; 
		 i < integralHighBin; 
		 i++){
		
		if (H1.getBinContent(i) > ampl){
                    ampl = H1.getBinContent(i);
                    mean = i * (CosmicQXMax[l] - CosmicQXMin[l]);
                    mean = mean/NBinsCosmic + CosmicQXMin[l];
                }
            }
	    
	    String fitFunc = "landau";
	    
	    if(fitBackground)
		fitFunc = "landau+exp";
	    
            fQMIP.add(s,l,c, new F1D(fitFunc,
	    			     rangeLow[l],
	    			     rangeHigh[l]));
	    
            fQMIP.get(s,l,c).setParameter(0, ampl);
            fQMIP.get(s,l,c).setParameter(1, mean);
            fQMIP.get(s,l,c).setParameter(2, 150);
	    
	    if(fitBackground){
		fQMIP.get(s,l,c).setParameter(3, ampl/5);
		fQMIP.get(s,l,c).setParameter(4, -0.001);
	    }
	    
            fQMIP.get(s,l,c).setParLimits(0, 0, ampl*2.0);
            fQMIP.get(s,l,c).setParLimits(1, mean-400, mean+400);
            fQMIP.get(s,l,c).setParLimits(2, 50, 1500);
	    
	    if(fitBackground){
		fQMIP.get(s,l,c).setParLimits(3, ampl/10, ampl*20.0);
		fQMIP.get(s,l,c).setParLimits(4, -5.0,0.00);
	    }
	    
	    fQMIP.get(s,l,c).setLineColor(1);
	    
	    return true;
	}
	else
	    return false;
	
    }

    private void fitQMIP(int s,int l,int c,String fitOption){
	
	if (testMode)
	    System.out.println(" Fitting Q MIP (S,L,C) = (" +
			       s  + "," + l + "," + c + ")"); 
	
	if(initFitQMIPParameters(s,l,c,H_MIP_Q.get(s,l,c))){
	
	    H_MIP_Q.get(s,l,c).
		fit(fQMIP.get(s,l,c),fitOption);
	
	if (testMode)
	    System.out.println(" Fitted Q MIP (S,L,C) = (" + 
			       s  + "," + l + "," + c + ")");
	
	}
	else{
	    if (testMode)
		System.out.println(" No Q MIP Fit (S,L,C) = (" + 
				   s  + "," + l + "," + c + ")");
	}
    }
    
    private boolean initFitVMIPParameters(int s,int l,int c,H1D H1){
	
	double ampl = H1.getBinContent(H1.getMaximumBin());
	//mean = H1.getMaximumBin()*45 + 250;
	double mean[] = {0.,300.,600.};
	double std  = 5.0;
	
	double[] rangeLow  = {0.0, 100.,  250};
	double[] rangeHigh = {0.0, 1250., 2000};
        	
	if (H1.integral() > 10 ){
	    
	    fVMIP.add(s,l,c, new F1D("landau",
				     rangeLow[l],
				     rangeHigh[l]));
		
	    fVMIP.get(s,l,c).setParameter(0, ampl);
	    fVMIP.get(s,l,c).setParameter(1, mean[l]);
	    fVMIP.get(s,l,c).setParameter(2, 200);
		    
 	    fVMIP.get(s,l,c).setParLimits(0, ampl*0.5, ampl*2.5);
	    fVMIP.get(s,l,c).setParLimits(1, 0, 1500);
	    fVMIP.get(s,l,c).setParLimits(2, 50, 100);
	
	    return true;
	}
	else
	    return false;
	
    }

    private void fitVMIP(int s,int l,int c,String fitOption){
	
	if (testMode)
	    System.out.println(" Fitting V MIP (S,L,C) = (" +
			       s  + "," + l + "," + c + ")"); 

	if(initFitVMIPParameters(s,l,c,H_MIP_V.get(s,l,c))){
	
	    H_MIP_V.get(s,l,c).
		fit(fVMIP.get(s,l,c),fitOption);
	
	if (testMode)
	    System.out.println(" Fitted V MIP (S,L,C) = (" + 
			       s  + "," + l + "," + c + ")");
	
	}
	else{
	    if (testMode)
		System.out.println(" No V MIP Fit (S,L,C) = (" + 
				   s  + "," + l + "," + c + ")");
	}
    }

    private boolean initFitDTParameters(int s,int l,int c,H1D H1){
	
	double ampl = H1.getBinContent(H1.getMaximumBin());
	double mean = 0.;
	double std  = 1.0;
	
	double rangeLow  = -10.;
	double rangeHigh = 10.0;
        	
	if (H1.integral() > 10 ){
	    
	    fDT.add(s,l,c, new F1D("gaus",
				   rangeLow,
				   rangeHigh));
		
	    fDT.get(s,l,c).setParameter(0, ampl);
	    fDT.get(s,l,c).setParameter(1, mean);
	    fDT.get(s,l,c).setParameter(2, std);
		    
	    fDT.get(s,l,c).setParLimits(0, ampl*0.5, ampl*2);
	    fDT.get(s,l,c).setParLimits(1, rangeLow, rangeHigh);
	    fDT.get(s,l,c).setParLimits(2, 0.3, 4.0);
	    
	    fDT.get(s,l,c).setLineColor(1);

	    	    
	    return true;
	}
	else
	    return false;
	
    }

    private void fitDT(int s,int l,int c,String fitOption){
	
	if (testMode)
	    System.out.println(" Fitting DT (S,L,C) = (" +
			       s  + "," + l + "," + c + ")"); 
	
	if(initFitDTParameters(s,l,c,H_DT_MODE7.get(s,l,c))){
	    
	    H_DT_MODE7.get(s,l,c).
		fit(fDT.get(s,l,c),fitOption);
	    
	    	    
	if (testMode)
	    System.out.println(" Fitted DT (S,L,C) = (" + 
			       s  + "," + l + "," + c + ")");
	
	}
	else{
	    if (testMode)
		System.out.println(" No DT Fit (S,L,C) = (" + 
				   s  + "," + l + "," + c + ")");
	}
    }
    
    private void initThresholdParameters(int s,int l,int c) {
	
	fThr.add(s,l,c, new F1D("p0",0,100));
	fThr.get(s,l,c).setParameter(0, threshold);
	fThr.get(s,l,c).setLineColor(2);
	
	double vTh = threshold*LSB;
	
	fVThr.add(s,l,c, new F1D("p0",0,400));
	fVThr.get(s,l,c).setParameter(0,vTh);
	fVThr.get(s,l,c).setLineColor(2);
	
    }
    
    private void fitHistograms() {

	System.out.println(" Fitting Histograms");
	
	String fitOption = "NRQ";
	        
        HistPara HP =  new HistPara();
        
	int s, l, c;
	
	// Do the fitting for all components
        for (int index = 0; index < 232; index++) {
	    
            HP.setAllParameters(index,'h');
	    
	    s = HP.getS();
	    l = HP.getL();
	    c = HP.getC();

	    //fitPedestals(s,l,c,fitOption);
	    //fitVNoise(s,l,c,fitOption);
	    fitVMIP(s,l,c,fitOption);
	    //fitQNoise(s,l,c,fitOption);
	    fitQMIP(s,l,c,fitOption);
	    
	    if(l==1)
		fitDT(s,l,c,fitOption);
	    
	    
	} // end of : for (int index = 0; index < 232; index++) {
	
	System.out.println(" Fitting Histograms complete");
    
    }// end of: private void fitHistograms() {
    
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
	// raw fADC pulse
	
	canvasEvent.cd(layCDL);
	
        if(H_FADC.hasEntry(secSel,laySel,comSel)){
	    this.canvasEvent.draw(H_FADC.get(secSel,
                                             laySel,
                                             comSel));
	    
	    if(fThr.hasEntry(secSel,
			     laySel,
			     comSel)){
		
		this.canvasEvent.draw(fThr.get(secSel,
 					       laySel,
 					       comSel),"same");
		
	    }
	    
        }
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	// raw fADC pulse
        canvasEvent.cd(oppCDL);
        
        if(H_FADC.hasEntry(secSel,
                           oppSel,
                           comSel)){
            this.canvasEvent.draw(H_FADC.get(secSel,
                                             oppSel,
                                             comSel));
        
	    if(fThr.hasEntry(secSel,
			     oppSel,
			     comSel))
                this.canvasEvent.draw(fThr.get(secSel,
					       oppSel,
					       comSel),"same");
	    
        }
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasEvent.cd(layCDR);
        
        if(H_VT.hasEntry(secSel,
			 laySel,
			 comSel)){
            this.canvasEvent.draw(H_VT.get(secSel,
					   laySel,
					   comSel));
	    
	    if(fVThr.hasEntry(secSel,
			      laySel,
			      comSel)){
		this.canvasEvent.draw(fVThr.get(secSel,
						laySel,
						comSel),"same");
		
	    }
	}
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasEvent.cd(oppCDR);
        if(H_VT.hasEntry(secSel,
			 oppSel,
			 comSel)){
            this.canvasEvent.draw(H_VT.get(secSel,
					   oppSel,
					   comSel));
	    if(fVThr.hasEntry(secSel,
			      oppSel,
			      comSel))
                this.canvasEvent.draw(fVThr.get(secSel,
						oppSel,
						comSel),"same");
	}
	
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
	
	
        int layCDL =  3*layCD;// 1-->0; 2-->3;
        int oppCDL =  3*oppCD;// 1-->3; 2-->0;

        int layCDM = layCDL + 1;
        int oppCDM = oppCDL + 1;

        int layCDR = layCDL + 2;
        int oppCDR = oppCDL + 2;
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
            // Middle top (bottom) for thin (thick) layer
        canvasPed.cd(layCDM);
        
        if(H_PED_TEMP.hasEntry(secSel,laySel,comSel)){
            
            this.canvasPed.draw(H_PED_TEMP.get(secSel,
                                          laySel,
                                          comSel));
            
        }
	//----------------------------------------
	// Middle top (bottom) for thin (thick) layer
        canvasPed.cd(oppCDM);
        
        if(H_PED_TEMP.hasEntry(secSel,oppSel,comSel)){
            
            this.canvasPed.draw(H_PED_TEMP.get(secSel,
                                          oppSel,
                                          comSel));
        
        }
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasPed.cd(layCDR);
        if(H_PED_VS_EVENT.hasEntry(secSel,laySel,comSel)){
            this.canvasPed.draw(H_PED_VS_EVENT.get(secSel,
                                          laySel,
                                          comSel));
        }
        
	// GraphErrors
        
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
        canvasPed.cd(oppCDR);
        if(H_PED_VS_EVENT.hasEntry(secSel,oppSel,comSel)){
            this.canvasPed.draw(H_PED_VS_EVENT.get(secSel,
                                          oppSel,
                                          comSel));
        }
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
        
        if(H_NOISE_V.hasEntry(secSel,laySel,comSel)){
            this.canvasNoise.draw(H_NOISE_V.get(secSel,
                                             laySel,
                                             comSel));
            if(fV1.hasEntry(secSel,
			    laySel,
			    comSel))
                this.canvasNoise.draw(fV1.get(secSel,
					      laySel,
					      comSel),"same S");
            if(this.fitTwoPeaksV && 
	       fV2.hasEntry(secSel,
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
	
	if(H_NOISE_V.hasEntry(secSel,oppSel,comSel)){
            this.canvasNoise.draw(H_NOISE_V.get(secSel,
                                             oppSel,
                                             comSel));
            
	    if(fV1.hasEntry(secSel,
			    oppSel,
			    comSel))
                this.canvasNoise.draw(fV1.get(secSel,
					      oppSel,
					      comSel),"same S");
            
            if( this.fitTwoPeaksV &&
		fV2.hasEntry(secSel,
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
        if( this.fitTwoPeaksQ &&
	    fQ2.hasEntry(secSel,
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
        if(  this.fitTwoPeaksQ && 
	     fQ2.hasEntry(secSel,
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
	
	if(H_DT_MODE3.hasEntry(secSel,
			       laySel,
			       comSel) ){
            this.canvasTime.draw(H_DT_MODE3.get(secSel,
						laySel,
						comSel));
	}
	else if(H_DT_MODE3.hasEntry(secSel,
				    oppSel,
				    comSel)){
            this.canvasTime.draw(H_DT_MODE3.get(secSel,
						oppSel,
						comSel));
	}
	
	
	//----------------------------------------
	// right bottom
        canvasTime.cd(3);
	
	if(H_DT_MODE7.hasEntry(secSel,
			       laySel,
			       comSel) ){
            this.canvasTime.draw(H_DT_MODE7.get(secSel,
						laySel,
						comSel));
	
	    if(fDT.hasEntry(secSel,
			    laySel,
			    comSel))
                this.canvasTime.draw(fDT.get(secSel,
					       laySel,
					       comSel),"same S");
	
	}
	else if(H_DT_MODE7.hasEntry(secSel,
				    oppSel,
				    comSel)){
            this.canvasTime.draw(H_DT_MODE7.get(secSel,
						oppSel,
						comSel));
	    
	    if(fDT.hasEntry(secSel,
			    oppSel,
			    comSel))
                this.canvasTime.draw(fDT.get(secSel,
					       oppSel,
					       comSel),"same S");

	    
	}

	// if(H_T1_T2.hasEntry(secSel,
// 			       laySel,
// 			       comSel) ){
//             this.canvasTime.draw(H_T1_T2.get(secSel,
// 						laySel,
// 						comSel));
// 	}
// 	else if(H_T1_T2.hasEntry(secSel,
// 				    oppSel,
// 				    comSel)){
//             this.canvasTime.draw(H_T1_T2.get(secSel,
// 					     oppSel,
// 					     comSel));
// 	}
	

    
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
            if( this.fitTwoPeaksQ  && 
		fQ2.hasEntry(secSel,
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
        if( this.fitTwoPeaksQ &&
	    fQ2.hasEntry(secSel,
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
        
        if(H_NOISE_V.hasEntry(secSel,laySel,comSel)){
            this.canvasVoltage.draw(H_NOISE_V.get(secSel,
					       laySel,
					       comSel));
	    if(fV1.hasEntry(secSel,
			    laySel,
			    comSel))
		this.canvasVoltage.draw(fV1.get(secSel,
						laySel,
						comSel),"same S");
	    if( this.fitTwoPeaksV &&
		fV2.hasEntry(secSel,
			     laySel,
			     comSel))
		this.canvasVoltage.draw(fV2.get(secSel,
						laySel,
						comSel),"same S");
	    
        }
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
        canvasVoltage.cd(oppCDL);
        
        if(H_NOISE_V.hasEntry(secSel,
                              oppSel,
                              comSel))
            this.canvasVoltage.draw(H_NOISE_V.get(secSel,
                                                 oppSel,
                                                 comSel));
       if(fV1.hasEntry(secSel,
                            oppSel,
                            comSel))
           this.canvasVoltage.draw(fV1.get(secSel,
                                               oppSel,
                                               comSel),"same S");
       if(  this.fitTwoPeaksV && 
	    fV2.hasEntry(secSel,
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
            if(  this.fitTwoPeaksQ &&
		 fQ2.hasEntry(secSel,
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
        if(  this.fitTwoPeaksQ &&
	     fQ2.hasEntry(secSel,
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
	    G_NPE[layerM].setMarkerSize(10); // 1 or 2
	    
            
        }
        canvasMIP.draw(G_NPE[0]);
        canvasMIP.draw(G_NPE[1],"same");
	
	System.out.println(" Marker Size = " + G_NPE[0].getMarkerSize());
	
    }
    
    void drawCanvasMIPElec(int s,int l,int c){
        
        if(s == 0 || l == 0)
            return;
        
        canvasMIP.divide(1,1);
        
        GraphErrors G_NPE;
	
	int mezSel = getMezz4SLC(s,l,c);
	
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
		
		npeArr[chan] = getNPEMean(sect,laye,comp);
		npeErr[chan] = getNPEError(sect,laye,comp);
	    }
	}
	
	for (int chan = 0 ; chan < 16 ; chan++ ){
	    if(npeErr[chan] > 20.0 ||
	       npeArr[chan] < 20.0){
		
		npeErr[chan] = 20.0;
		
		if(chan < 8)
		    npeArr[chan] = 40.0;
		else
		    npeArr[chan] = 60.0;
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
	G_NPE.setMarkerSize(10); 
	canvasMIP.draw(G_NPE);
	
    }
    
    void drawCanvasGain(int s,
                        int laySel,
                        int comSel
                        ){
        
	if(s == 0)
            return;
	
	boolean evenSecSelect = true;
        
        String sectors[] = new String[8];
        
        if(s%2==1)
            evenSecSelect = false;
        
        int sector2CD[] = {4,0,1,2,5,8,7,6,3};
        
        canvasGain.cd(sector2CD[s]);        
        
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
		    
		    evenGain[lM][c]    = gain[s][lM+1][evenI[c]];
		    evenGainErr[lM][c] = errGain[s][lM+1][evenI[c]];
		    
		}
		else{
		    evenGain[lM][c]    = gain_mV[s][lM+1][evenI[c]];
		    evenGainErr[lM][c] = errGain_mV[s][lM+1][evenI[c]];
		
		}
		
		if (evenGainErr[lM][c] > maxErr){
		    evenGain[lM][c]    = 0.0;
		    evenGainErr[lM][c] = 0.0;
		}
		
            }
            // loop over odd indices
	    for (int c = 0 ; c < oddI.length ; c++){

		if(!useGain_mV){
		    oddGain[lM][c]    = gain[s][lM+1][oddI[c]];
		    oddGainErr[lM][c] = errGain[s][lM+1][oddI[c]];
		}
		else {
		    oddGain[lM][c]    = gain_mV[s][lM+1][oddI[c]];
		    oddGainErr[lM][c] = errGain_mV[s][lM+1][oddI[c]];
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
	    title = "sector " + s;
            
	    G_Gain[lM].setTitle(title);
            G_Gain[lM].setXTitle("component");
            G_Gain[lM].setYTitle(yTitle);
            G_Gain[lM].setMarkerSize(5);
            G_Gain[lM].setMarkerColor(lM+1); // 0-9 for given palette
            G_Gain[lM].setMarkerStyle(lM+1); // 1 or 2
	    
        }
        
        int nXBins[] = {20,9};
        int nYBins = 100;
        double[] xLimits = {0.5,(double)nXBins[s%2]+0.5};
        double[] yLimits = {5.0,30.};
        
        H1D H1 = new H1D("H1","component","gain (pC)",
			 nXBins[s%2],xLimits[0],xLimits[1]);
        
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
	
	int sectI;
	int compI;
	int layeI;
	int ii = 0;
	
	int mezz = getMezz4SLC(secSel,laySel,comSel);
	
	for (int chan = 0 ; chan < 16 ; chan++){
	    
	    sectI  = getSect4ChMez(chan,mezz);
	    compI  = getComp4ChMez(chan,mezz);
	    layeI  = chan/8 + 1;
	    
	    if(!useGain_mV){
		gainArr[ii]    = gain[sectI][layeI][compI] ;
		gainErrArr[ii] = errGain[sectI][layeI][compI];
	    
		if(gainErrArr[ii] > 5.0 ||
		   gainArr[ii]    < 15.0){
		    gainArr[ii]    = 20.0;
		    gainErrArr[ii] =  5.0; 
		}
		
		
	    }
	    else {
		gainArr[ii]    = gain_mV[sectI][layeI][compI];
		gainErrArr[ii] = errGain_mV[sectI][layeI][compI];
	    
		if(gainErrArr[ii] > 2.0 ||
		   gainArr[ii]    < 8.0){
		    gainArr[ii]    = 10.0;
		    gainErrArr[ii] =  2.0; 
		}
		
	    }
	    
	    chanArr[ii] = chan;
	    chanErrArr[ii] = 0;
	    ii++;
	}
	
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
	G_Gain.setXTitle("Channel");
	G_Gain.setYTitle(titleHY);
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
        
	indexSel = getIndex4SLC(secSel,laySel,comSel);
	
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
	
        if      ( tabSel == this.tabIndexEvent ) {
	    
	    
	    drawCanvasEvent(secSel,
			    laySel,
			    comSel);
            
        }
        else if ( tabSel == this.tabIndexPed ) {
	 
	    drawCanvasPed(secSel,
			  laySel,
			  comSel);
            
        }
        else if ( tabSel == this.tabIndexNoise ) {
	    
            drawCanvasNoise(secSel,
                            laySel,
                            comSel);
            
        }
        else if ( tabSel == this.tabIndexGain ) {
	    
	    
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
        else if ( tabSel == this.tabIndexCharge ) {
            
            drawCanvasCharge(secSel,
                             laySel,
                             comSel);
			     
        }
        else if ( tabSel == this.tabIndexVoltage ) {
            
            drawCanvasVoltage(secSel,
			      laySel,
			      comSel);
			     
        }
        else if ( tabSel == this.tabIndexMIP ) {
            
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
	    
        else if ( tabSel == this.tabIndexMatch ) {
            
            drawCanvasMatch(secSel,
                            laySel,
                            comSel);
        }
	else if ( tabSel == this.tabIndexTime ) {
            
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
					   1.0*nGain_mV,
					   true);
	}
	else{  
	    gainColor = palette.getColor3D(gain[sec][lay][com],
					   1.0*nGain,
					   true);
	}
	
	Color  voltColor = palette.getColor3D(vMax[sec][lay][com],
					      12*nGain_mV,
					      true);
	
	Color  qColor    = palette.getColor3D(qMax[sec][lay][com],
					      250*nGain,
					      true);

        if    ( tabSel == tabIndexEvent ) {
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
	else if( tabSel == tabIndexPed ) {
	    shape.setColor(pedColor.getRed(),
			   pedColor.getGreen(),
			   pedColor.getBlue());
        }
        else if( tabSel == tabIndexNoise ){ 
	    shape.setColor(noiseColor.getRed(),
			   noiseColor.getGreen(),
			   noiseColor.getBlue());
	}
	else if( tabSel == tabIndexVoltage ) {
	    shape.setColor(voltColor.getRed(),
			   voltColor.getGreen(),
			   voltColor.getBlue());
        }
	else if( tabSel == tabIndexCharge ) {
	    shape.setColor(qColor.getRed(),
			   qColor.getGreen(),
			   qColor.getBlue());
        }
	else if( tabSel == tabIndexGain ){ 
	    shape.setColor(gainColor.getRed(),
			   gainColor.getGreen(),
			   gainColor.getBlue());
	}
	else if( tabSel == tabIndexMIP    ||
		 tabSel == tabIndexCharge ){
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
    // Constants
    
    private double getStatus(int s, int l, int c){
        return status[s][l][c];
    }
    
    // 0  Okay, 1  Noisy, 3  Dead, 5  Other
    private void setStatus(int s, int l, int c){
	
	boolean goodQ = false;
 	boolean lowQ  = false;
	boolean zeroQ = false;
	
	double  meanQ = getQMean(s,l,c);
	
	if     ((l==1 && meanQ > 500  ) ||
		(l==2 && meanQ > 1000 ))
	    goodQ = true;
	else if ( qMax[s][l][c] > 0.0 )
	    lowQ  = true;
	else
	    zeroQ = true;
	
	if     (goodQ)
	    status[s][l][c] = 0.0;
	else if(lowQ)
	    status[s][l][c] = 5.0;
	else if(zeroQ)
	    status[s][l][c] = 3.0;

    }
    
    private double getThrshNPE(int s, int l, int c){
	return nThrshNPE;
    }
    
    private double getPedMean(int s, int l, int c){
	return pedMean[s][l][c];
    }
    
    private void setPedMean(int s, int l, int c){
        
	if(fPed.hasEntry(s, l, c))
	    pedMean[s][l][c] = fPed.get(s,l,c).getParameter(1);
	else
	    pedMean[s][l][c] = 200.0;
    }
    
    private double getPedRMS(int s, int l, int c){
	return nPed_RMS;
    }
    
    private double getGain(int s, int l, int c){
        double thisGain = 0.0;
	
	if( useDefaultGain ){
	    thisGain = nGain;
	}
	else if ( !this.fitTwoPeaksQ ){
	    
	    if(fQ1.hasEntry(s, l, c)){
		
		double n1 = fQ1.get(s,l,c).getParameter(3);		
		thisGain  =  n1;
	    }
	    
	}
	else if( this.fitTwoPeaksQ ){
	    
	    if(fQ1.hasEntry(s, l, c) &&
	       fQ2.hasEntry(s, l, c)){
		
		double n2 = fQ2.get(s,l,c).getParameter(1);
		double n1 = fQ1.get(s,l,c).getParameter(3);		
		thisGain = n2 - n1;
	    }
	}
	
	if (thisGain < 15.0 ||
	    thisGain > 25.0)
	    thisGain = 0.0;
	
	return thisGain;
    }
    
    private double getGainError(int s, int l, int c){
	
	double gainError = 0.0;
	
	if( useDefaultGain ){
	    gainError = 0.0;
	}
	else if ( !this.fitTwoPeaksQ ){
	    if(fQ1.hasEntry(s,l,c) &&
	       getGain(s,l,c) > 0.0 ){
		
		double n1Error = fQ1.get(s,l,c).getParError(3);
		
		gainError    = n1Error*n1Error;
		gainError    = sqrt(gainError);
		
	    }
	}
        else if( this.fitTwoPeaksQ ){
	    if(fQ1.hasEntry(s,l,c) &&
	       fQ2.hasEntry(s,l,c) &&
	       getGain(s,l,c) > 0.0 ){
		
		double n2Error = fQ2.get(s,l,c).getParError(1);
		double n1Error = fQ1.get(s,l,c).getParError(3);
		
		gainError    = n2Error*n2Error + n1Error*n1Error;
		gainError    = sqrt(gainError);
		
	    }
	}
	
        return gainError;
        
    }
    
    private double getGain_mV(int s, int l, int c){
	
        double thisGain_mV = 0.0;
	
	if( useDefaultGain ){
	    thisGain_mV = nGain_mV;
	}
	else if( !this.fitTwoPeaksV ){
	    if(fV1.hasEntry(s, l, c)){
		double m1 = fV1.get(s,l,c).getParameter(1);
		thisGain_mV =  m1;
	    }
	    
	    if (thisGain_mV < 0.5*nGain_mV ||
		thisGain_mV > 1.5*nGain_mV)
		thisGain_mV = 0.0;
	    
	}
	else if( this.fitTwoPeaksV ){
	    if(fV1.hasEntry(s, l, c) &&
	       fV2.hasEntry(s, l, c)){
		
		// note that the functions were added in the other
		// order compared to the charge fits
		double m2 = fV2.get(s,l,c).getParameter(1);
		double m1 = fV1.get(s,l,c).getParameter(1);
		
		thisGain_mV = m2 - m1;
		
	    }
        }
        return thisGain_mV;
    }
    
    private double getGainErr_mV(int s, int l, int c){
	    
        double gainErr_mV = 0.0;

	if( useDefaultGain ){
	    gainErr_mV = 0.0;
	}
	else if( !this.fitTwoPeaksV ){
	    if(fV1.hasEntry(s,l,c) &&
	       getGain_mV(s,l,c) > 0.0 ){
		double m1Error = fV1.get(s,l,c).getParError(1);
		gainErr_mV = m1Error;
	    }
	}
	else if ( this.fitTwoPeaksV ){
	    
	    if(fV1.hasEntry(s, l, c) &&
	       fV2.hasEntry(s, l, c)){
		
		// note that the functions were added in the other
		// order to the charge fits
		double m2Error = fV2.get(s,l,c).getParError(1);
		double m1Error = fV1.get(s,l,c).getParError(1);
		gainErr_mV   = m2Error*m2Error + m1Error*m1Error;
		gainErr_mV   = sqrt(gainErr_mV);
		
	    }
	}
	
        return gainErr_mV;
        
    }
    
    private double getE(int s, int l, int c){
	return nMipE[l];
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

    private double getDTMean(int s, int l, int c){
	
        double tMean = 0.0;
	
	int thinLayer = 1;
	
        if(fDT.hasEntry(s, thinLayer, c))
	    tMean = fDT.get(s,thinLayer,c).getParameter(1);
        
        return tMean;
        
    }
    
    private double getDTSigma(int s, int l, int c){
	
        double tSigma = 0.0;
        
	int thinLayer = 1;
	
        if(fDT.hasEntry(s, thinLayer, c))
	    tSigma = fDT.get(s,thinLayer,c).getParameter(2);
        
        return tSigma;
        
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
    
    private void updateConstants() {
        
	int index;
	
	double mipE = 0.0;
	double mipC = 0.0;
	
	for (int s = 1; s < 9; s++) {
            for (int l = 1; l < 3; l++) {
                for (int c = 1 ; c < 21 ; c++){
                    
                    if(s%2==1 && c > 9 ) continue;
		    
		    index = getIndex4SLC(s,l,c);

		    //------------------------------
		    // set constants
		    //------------------------------
		    
		    meanNPE[s][l][c]    = getNPEMean(s,l,c);
                    errNPE[s][l][c]     = getNPEError(s,l,c);
                    sigNPE[s][l][c]     = getSigNPE(s,l,c);
		    
		    gain[s][l][c]       = getGain(s,l,c);
                    errGain[s][l][c]    = getGainError(s,l,c);
		    
		    meanNPE_mV[s][l][c] = getNPEMean_mV(s,l,c);
                    errNPE_mV[s][l][c]  = getNPEErr_mV(s,l,c);
                    gain_mV[s][l][c]    = getGain_mV(s,l,c);
                    errGain_mV[s][l][c] = getGainErr_mV(s,l,c);
                    
		    
		    setStatus(s,l,c);
		    setPedMean(s,l,c);
		    
		    
		    //---------------------------------
		    // Update the table
		    
		    ccdbTable.setValueAtAsDouble(statCCDB-3,
						 getStatus(s,l,c),
						 s,l,c);
		    ccdbTable.setValueAtAsDouble(npeThCCDB-3,
						 getThrshNPE(s,l,c),
						 s,l,c);
		    ccdbTable.setValueAtAsDouble(pedCCDB-3,
						 getPedMean(s,l,c),
						 s,l,c);
		    ccdbTable.setValueAtAsDouble(pedRMSCCDB-3,
						 getPedRMS(s,l,c),
						 s,l,c);
		    ccdbTable.setValueAtAsDouble(gainPCCCDB-3,
						 getGain(s,l,c),
						 s,l,c);
                    ccdbTable.setValueAtAsDouble(gainMVCCDB-3,
						 getGain_mV(s,l,c),
						 s,l,c);
		    ccdbTable.setValueAtAsDouble(mipEnCCDB-3,
						 getE(s,l,c),
						 s,l,c);
		    ccdbTable.setValueAtAsDouble(mipChCCDB-3,
						 getQMean(s,l,c),
						 s,l,c);
		    ccdbTable.setValueAtAsDouble(tOffCCDB-3,
						 getDTMean(s,l,c),
						 s,l,c);
		    ccdbTable.setValueAtAsDouble(tResCCDB-3,
						 getDTSigma(s,l,c),
						 s,l,c);
		    
		    
		    
//                     ccdbTable.setValueAtAsDouble(,
// 						 errNPE[s][l][c],
		    // s,l,c);
		
// 		    ccdbTable.setValueAtAsDouble(gainPCCCDB,
// 						 gain[s][l][c],
// 						 s,l,c);
                    
//                     ccdbTable.setValueAtAsDouble(,
// 						 errGain[s][l][c],
// 						 s,l,c);
                    
//                     ccdbTable.setValueAtAsDouble(gainMVCCDB,
// 						 gain_mV[s][l][c],
// 						 s,l,c);
                    
//                     ccdbTable.setValueAtAsDouble(6,
// 						 errGain_mV[s][l][c],
// 						 s,l,c);
                    
                }
            }
        }
        if(testMode)
	    ccdbTable.show();
        this.view.repaint();
        
    } // end of: private void updateTable() {
    
    public void stateChanged(ChangeEvent e) {
        JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
	
	tabSel = sourceTabbedPane.getSelectedIndex();
        
	if ( tabSel == this.tabIndexGain ||
	     tabSel == this.tabIndexMIP )
	    this.canvasPane.add(rBPane, BorderLayout.NORTH);
	
	if( (this.previousTabSel==this.tabIndexGain ||
	     this.previousTabSel==this.tabIndexMIP ) && 
	    tabSel!=this.tabIndexGain )
            this.canvasPane.remove(rBPane);
    
	System.out.println("Tab changed to: " +
                           sourceTabbedPane.getTitleAt(tabSel) +
                           " with index " + tabSel);
        
        if(tabSel == tabIndexTable)
            this.updateConstants();
        
	previousTabSel = tabSel;
	
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

	String namePed = HP.getTitle()+" for 100 events";
	
        H_PED_TEMP.add(HP.getS(),HP.getL(),HP.getC(),
                  new H1D(DetectorDescriptor.
                          getName("H_PED_TEMP",
                                  HP.getS(),
                                  HP.getL(),
                                  HP.getC()),
                          namePed,
                          NBinsPed,PedQX[0],PedQX[1]));
        
        H_PED_TEMP.get(HP.getS(),HP.getL(),HP.getC()).
        setFillColor(2);
        H_PED_TEMP.get(HP.getS(),HP.getL(),HP.getC()).
        setXTitle("Pedestal");
        H_PED_TEMP.get(HP.getS(),HP.getL(),HP.getC()).
        setYTitle("Counts");
        
        for (int i = 0 ; i < nPointsPed ; i++) {
            px_H_PED_VS_EVENT[i] = i*1.0;
            pex_H_PED_VS_EVENT[i] = 0.0;
	    //py_H_PED_VS_EVENT[i] = 0.0;
	    //pey_H_PED_VS_EVENT[i] = 0.0;
        }
        
        H_PED_VS_EVENT.add(HP.getS(),HP.getL(),HP.getC(),
                           new GraphErrors(namePed, 
					   px_H_PED_VS_EVENT, 
					   py_H_PED_VS_EVENT[HP.getS()-1][HP.getL()-1][HP.getC()-1],
                                           pex_H_PED_VS_EVENT, 
					   pey_H_PED_VS_EVENT[HP.getS()-1][HP.getL()-1][HP.getC()-1]));
        H_PED_VS_EVENT.get(HP.getS(),HP.getL(),HP.getC()).setTitle(HP.getTitle());
        H_PED_VS_EVENT.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("Event Index");
        H_PED_VS_EVENT.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("Average Pedestal");
        H_PED_VS_EVENT.get(HP.getS(),HP.getL(),HP.getC()).setMarkerSize(5);
        
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
                              NBinsNoise[HP.getL()],
			      NoiseQXMin[HP.getL()],
			      NoiseQXMax[HP.getL()]));

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
        
        H_NOISE_V.add(HP.getS(),HP.getL(), HP.getC(),
                   new H1D(DetectorDescriptor.
                           getName("WAVEMAX",
                                   HP.getS(),HP.getL(),HP.getC()),
			   HP.getTitle(), 130,
			   NoiseVXMin[HP.getL()],
			   NoiseVXMax[HP.getL()]));

	H_NOISE_V.get(HP.getS(),HP.getL(), HP.getC()).
	    setFillColor(2);
	
        H_NOISE_V.get(HP.getS(),HP.getL(), HP.getC()).
	    setXTitle("Waveform Max (mV)");
	
        H_NOISE_V.get(HP.getS(),HP.getL(), HP.getC()).
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
        
        H_TIME_MODE3.add(HP.getS(),HP.getL(), HP.getC(),
                         new H1D(DetectorDescriptor.
                                 getName("H_TIME_MODE3",
                                         HP.getS(),HP.getL(),HP.getC()),
                                 HP.getTitle(), 100, 0.0, 400));
        
        H_TIME_MODE3.get(HP.getS(),HP.getL(), HP.getC()).setFillColor(4);
        H_TIME_MODE3.get(HP.getS(),
			 HP.getL(), 
			 HP.getC()).setXTitle("Mode 1 Time (ns)");
        H_TIME_MODE3.get(HP.getS(),HP.getL(), HP.getC()).setYTitle("Counts");
        
	H_MAXV_VS_T.add(HP.getS(),
			HP.getL(), 
			HP.getC(),
			new H2D(DetectorDescriptor.
				getName("H_MAXV_VS_T",
					HP.getS(),HP.getL(),HP.getC()),
				HP.getTitle(),
				32,100.,150.,
				32,0.,2000.));
	
	//H_TIME_MODE3.get(HP.getS(),HP.getL(), HP.getC()).setFillColor(4);
        
	H_MAXV_VS_T.get(HP.getS(),
			HP.getL(), 
			HP.getC()).setXTitle("Mode 1 Time (ns)");
        H_MAXV_VS_T.get(HP.getS(),HP.getL(), 
			HP.getC()).setYTitle("Peak Voltage (mV)");
        
	if (HP.getL()==1){
            H_DT_MODE3.add(HP.getS(),
			   HP.getL(), 
			   HP.getC(),
			   new H1D(DetectorDescriptor.
				   getName("H_DT_MODE3",
					   HP.getS(),HP.getL(),HP.getC()),
				   HP.getTitle(), 32, -16, 16));
            H_DT_MODE3.get(HP.getS(),HP.getL(), 
			   HP.getC()).setFillColor(4);
            H_DT_MODE3.get(HP.getS(),
			   HP.getL(), 
			   HP.getC()).setXTitle("Mode Three #Delta T (ns) [thick - thin]");
            H_DT_MODE3.get(HP.getS(),HP.getL(), 
			   HP.getC()).setYTitle("Counts");
	    
	    H_DT_MODE7.add(HP.getS(),
			   HP.getL(), 
			   HP.getC(),
			   new H1D(DetectorDescriptor.
				   getName("H_DT_MODE7",
					   HP.getS(),HP.getL(),HP.getC()),
				   HP.getTitle(), 64, -16, 16));
	    //		   HP.getTitle(), 64, -100, 100));
            H_DT_MODE7.get(HP.getS(),HP.getL(), 
			   HP.getC()).setFillColor(4);
            
	    H_DT_MODE7.get(HP.getS(),HP.getL(), 
			   HP.getC()).setLineColor(1);
	    
	    H_DT_MODE7.get(HP.getS(),
			   HP.getL(), 
			   HP.getC()).setXTitle("Mode Seven #Delta T (ns) [thick - thin]");
            H_DT_MODE7.get(HP.getS(),HP.getL(), 
			   HP.getC()).setYTitle("Counts");
	    
	    H_T1_T2.add(HP.getS(),
			   HP.getL(), 
			   HP.getC(),
			   new H2D(DetectorDescriptor.
				   getName("H_T1_T2",
					   HP.getS(),HP.getL(),HP.getC()),
				   HP.getTitle(), 
				   256, 125., 275.,
				   256, 125., 275.));
            
            H_T1_T2.get(HP.getS(),
			HP.getL(), 
			HP.getC()).setXTitle("Mode Seven Time (ns) [thin layer]");
            H_T1_T2.get(HP.getS(),HP.getL(), 
			HP.getC()).setYTitle("Mode Seven Time (ns) [thick layer]");
	    
	
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
        
        H_NOISE_V.add(HP.getS(),HP.getL(), HP.getC(),
                   new H1D(DetectorDescriptor.
                           getName("WAVEMAX",
                                   HP.getS(),HP.getL(),HP.getC()),
			   HP.getTitle(), 130,
			   0.5*nGain_mV,
			   3.0*nGain_mV));
        
	H_NOISE_V.get(HP.getS(),HP.getL(), HP.getC()).
	    setFillColor(2);
	
        H_NOISE_V.get(HP.getS(),HP.getL(), HP.getC()).
	    setXTitle("Waveform Max (mV)");
	
        H_NOISE_V.get(HP.getS(),HP.getL(), HP.getC()).
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
        
        H_FADC.get(HP.getS(),
                   HP.getL(),
                   HP.getC()).reset();
        
        H_NOISE_V.get(HP.getS(),
		   HP.getL(),
		   HP.getC()).reset();
	
	
	    
	if(detector=='h'){
	
	    H_PED.get(HP.getS(),
		      HP.getL(),
		      HP.getC()).reset();
	    
	    H_COSMIC_fADC.get(HP.getS(),
			      HP.getL(),
			      HP.getC()).reset();
	    H_MIP_V.get(HP.getS(),
			HP.getL(),
			HP.getC()).reset();
	    
	}
	
    }
    
    
    public void initArrays() {
        
	status   = new double[9][3][21];
	thrshNPE = new double[9][3][21];
	
	ped      = new double[9][3][21];
	pedMean  = new double[9][3][21];

	vMax      = new double[9][3][21];
	vMaxEvent = new double[9][3][21];
	
	qMax      = new double[9][3][21];
	
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
	
        for (int s = 0; s < 9; s++) {
            for (int l = 0; l < 3; l++) {
                for ( int c = 0 ; c < 21 ; c++){
		    
		    this.status[s][l][c]     = nStatus;
		    
		    this.thrshNPE[s][l][c]   = nThrshNPE;
		    
		    this.ped[s][l][c]        = nPed;
		    this.pedMean[s][l][c]    = nPed_RMS;
		    
		    this.gain[s][l][c]       = nGain;
                    this.errGain[s][l][c]    = nErrGain;
		    
		    this.gain_mV[s][l][c]    = nGain_mV;
                    this.errGain_mV[s][l][c] = nErrGain_mV;
		    
		    this.meanNPE[s][l][c]    = 0.0;
                    this.errNPE[s][l][c]     = 0.0;
                    this.sigNPE[s][l][c]     = 100.0;
		    
                    
		    this.meanNPE_mV[s][l][c]  = 0.0;
                    this.errNPE_mV[s][l][c]   = 0.0;
                    
		    this.vMax[s][l][c]       = 0.0;
		    this.vMaxEvent[s][l][c]  = 0.0;
		    
		    this.qMax[s][l][c]       = 0.0;
		    
		    this.npeEvent[s][l][c]   = 0.0;                    
		    
		} // end: for ( int c = 0 ; c...
	    } // end: for (int l = 0; l < 3; l...
	} // end: for (int s = 0; s...
	
    }// end: public void initArra....

    
    public void processDecodedSimEvent(DetectorCollection<Double> adc, 
				       DetectorCollection<Double> tdc){
	
	double[] time = { -9.9, -99.9, -999.9 };
	double   time2Hodo = 6.0;
	double   startTime = 124.25;
	
	time[0] = startTime + time2Hodo;
	
	boolean[] goodTime     = {false, false, false};
	boolean[] veryGoodTime = {false, false, false};
	boolean   goodDT;
	
	double    tCut   = 5.0;
	double    tRange = 100.0;
	double    dtCut  = 2.5;
	
	boolean   applyTCuts = true;
	boolean[] applyTCut  = {false, applyTCuts, applyTCuts}; 
	boolean   applyDTCut = false; 
	
	boolean   applyNoCuts = false;
	
	if (
	    (applyTCut[1] ||
	     applyTCut[2] ||
	     applyDTCut)  &&
	    applyNoCuts
	    ){
	    System.out.println(" conflict in cut logic ");
	}
	
	double[]  charge   = {0.0, 0.0, 0.0};
	double[]  peakVolt = {0.0, 0.0, 0.0};
	double    deltaT;
	
	for (int s = 1 ; s < 9 ; s++){
	    for (int c = 1 ; c < 21 ; c++){
		
		if( s%2==1 && c > 9 ) 
		    continue;
		
		deltaT      = -999.9;
		goodDT      = false;
		time[1]     = -99.9;
		time[2]     = -999.9;
		
		for (int l = 1 ; l < 3 ; l++){    
		    
		    peakVolt[l] = 0.0;
		    charge[l]   = 0.0;
		    goodTime[l] = false;
		    veryGoodTime[l] = false;
		    	
		    if( tdc.hasEntry(s,l,c) ){
			time[l] = tdc.get(s,l,c)*nsPerSample/100.;
			
			if(debugging_mode)
			    System.out.println(" time[" + l +
					       " = " + time[l] );
			
			if(abs(time[l] - time[0]) < tRange ) 
			    goodTime[l] = true;

			if(abs(time[l] - time[0]) < tCut ) 
			    veryGoodTime[l] = true;
	
		    }
		    		    
		    if (adc.hasEntry(s,l,c)) {
			
			charge[l]   = adc.get(s,l,c)*LSB*4.0/50;
			peakVolt[l] = charge[l]/2;
			
			if( charge[l] > qMax[s][l][c])
			    qMax[s][l][c] = charge[l];
			
			// cut conditions
			if( (applyTCut[l] && 
			     veryGoodTime[l]) ||
			    (applyDTCut && 
			     goodDT)          ||
			    (applyNoCuts)
			    ){
			    
			    H_MIP_Q.get(s, l, c)
				.fill(charge[l]);
			    
			    H_NOISE_Q.get(s, l, c)
				.fill(charge[l]);
			    
			    H_MIP_V.get(s, l, c)
				.fill(peakVolt[l]);
			    
			    H_NOISE_V.get(s, l, c)
				.fill(peakVolt[l]);
			    
			    
			} // end of cut conditions
			
			H_MAXV_VS_T.get(s,l,c)
			    .fill(time[l],peakVolt[l]);
			   

		    } // end of: if (adc.hasEntry(s,l,c)) {....
		    
		    
		    // time difference stuff
		    if( l == 2      &&
			goodTime[1] &&
			goodTime[2] &&
			(charge[1] > 100.0) &&
			(charge[2] > 100.0) 
			){
			
			deltaT = time[2] - time[1];
			
			if( abs(deltaT) < dtCut ){
			    goodDT = true;
			}
			
			H_DT_MODE7.get(s, 1, c).fill(deltaT);
		    
		    }// end of time difference stuff

		    // 		    if(charge > 500)
// 			System.out.println(" charge (s,l,c) = " + 
// 					   charge               +
// 					   "(" + s + "," + l    +
// 					   "," + c + ")");
 		} // end of: for (int l = 1 ; l < 3 ; l++)

	    }
	}
	
	if     (tabSel == tabIndexCharge)
	    drawCanvasCharge(secSel,laySel,comSel);
	else if(tabSel == tabIndexTime)
	    drawCanvasTime(secSel,laySel,comSel);
    }
    
    public void processDecodedEvent(int repaintFrequency, int detType) {
        nDecodedProcessed++;
        
        List<DetectorCounter> counters;
        
        if(detType == 0)
            counters = decoder.getDetectorCounters(DetectorType.FTCAL);
        else
            counters = decoder.getDetectorCounters(DetectorType.FTHODO);
        
        //System.out.println("event #: " + nDecodedProcessed);
        
        FTHODOViewerModule.MyADCFitter fadcFitter;
        fadcFitter = new FTHODOViewerModule.MyADCFitter();
        
        double[][][] time_M3 = new double[8][2][20];
	double[][][] time_M7 = new double[8][2][20];
	double[][]   dT_M3      = new double[8][20];
	double[][]   dT_M7      = new double[8][20];
        
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
            int maxPedbin = 0;
            double avePed=0.0;
            double nEventsAvePed=0;
            int index = getIndex4SLC(sec,lay,com);
            
            short  pulse[] = counter.getChannels().get(0).getPulse();
            double npeWave;
            double calibratedWave;
            double baselineSubRaw;
            double vOffset = 5.0;
            
            int eventloop;
            
	    // reset non-accumulating histograms
            H_FADC.get(sec, lay, com).reset();
            H_VT.get(sec, lay, com).reset();
            H_NPE.get(sec, lay, com).reset();
            
	    //===============================
	    // npe for this event only
	    // to be used in loop two below
            npeEvent[sec][lay][com] = 0.0;
            
            if(gain[sec][lay][com] > 15.0){
		
                npeEvent[sec][lay][com] = counter.getChannels().
                get(0).getADC().get(0)*LSB*4.0/50./gain[sec][lay][com];
                
                H_NPE_INT.get(sec, lay, com).fill(npeEvent[sec][lay][com]);
                
            }
	    
            H_PED.get(sec,lay,com).fill(fadcFitter.getPedestal());
	    
	    // Maybe this can be of number of events in histogram instead.. 
	    // however all histograms should have ped for every event
	    
            if (nDecodedProcessed/100 < nPointsPed)
                eventloop=nDecodedProcessed;
            else
                eventloop=nDecodedProcessed-nDecodedProcessed/(nPointsPed*100)*nPointsPed*100;
	    
            if (eventloop%100!=0){
		// Fills a histogram for pedestal by averaging 
		// out a number of bins at a hardcoded position
                H_PED_TEMP.get(sec,lay,com).fill(fadcFitter.getPedestal()); 
            }
            else {
		//System.out.println("Nick: " + eventloop/100);
		//Finds maximum-content bin of pedestal histogram
                maxPedbin = H_PED_TEMP.get(sec,lay,com).getMaximumBin(); 
                avePed = 0.0;
                nEventsAvePed = 0;
		// Calculates most prob pedestal value by taking 
		// +/-5 bins from maximum-content bin
                for (int i=0; i<5;i++){
                    avePed = avePed + PedBinWidth * (maxPedbin+i) * H_PED_TEMP.get(sec,lay,com).getBinContent(maxPedbin+i)+PedBinWidth*(maxPedbin-i)*H_PED_TEMP.get(sec,lay,com).getBinContent(maxPedbin-i);
                    nEventsAvePed=nEventsAvePed+H_PED_TEMP.get(sec,lay,com).getBinContent(maxPedbin+i)+H_PED_TEMP.get(sec,lay,com).getBinContent(maxPedbin-i);
                }
		//calculates average and corrects for offset of histogram
                avePed=avePed/nEventsAvePed+PedQX[0]; 
		// System.out.println("Nick: " + sec +" "+lay+" "+com+
		//" "+ (maxPedbin*PedBinWidth+PedQX[0]) +" "+avePed);
		// Ideally I would use setPoint function -- 
		//H_PED_VS_EVENT.get(sec,lay,com).setPoint(eventloop/100, eventloop/100, avePed); 
		//-- but functionality not present for coatjava 2.4
                py_H_PED_VS_EVENT[sec-1][lay-1][com-1][eventloop/100]=avePed;
                H_PED_VS_EVENT.add(sec,lay,com,
                                   new GraphErrors(px_H_PED_VS_EVENT, 
						   py_H_PED_VS_EVENT[sec-1][lay-1][com-1],
                                                   pex_H_PED_VS_EVENT, 
						   pey_H_PED_VS_EVENT[sec-1][lay-1][com-1]));
		
                String namePed2;
                if (lay==1)
                    namePed2="Thin S"+sec+" C"+com+" for 100 events";
                else
                    namePed2="Thick S"+sec+" C"+com+" for 100 events";
		
                H_PED_VS_EVENT.get(sec,lay,com).setTitle(namePed2);
                H_PED_VS_EVENT.get(sec,lay,com).setXTitle("Event Index");
                H_PED_VS_EVENT.get(sec,lay,com).setYTitle("Average Pedestal");
                H_PED_VS_EVENT.get(sec,lay,com).setMarkerSize(5);
		H_PED_TEMP.get(sec,lay,com).reset();
	    
	    }
	    
	    // Loop through fADC bins filling event-by-event histograms
            for (int i = 0;
                 i < min(pulse.length,
                         H_FADC.get(sec,lay,com).
			 getAxis().getNBins()); i++) {
                
		if( i == 100 ){
		    System.out.println(" pulse[" + i + "] = " + pulse[i]);
		}
		
                // Baseline unsubtracted
		// H_FADC.get(sec,lay,com).fill(i, pulse[i]);
		
		baselineSubRaw = pulse[i] - fadcFitter.getPedestal() + 10.0;
		H_FADC.get(sec,lay,com).fill(i,baselineSubRaw);
                
                calibratedWave = (pulse[i]-fadcFitter.getPedestal())*LSB + vOffset;
                H_VT.get(sec,lay,com).fill(i*4,calibratedWave);
                
                npeWave = (pulse[i] - fadcFitter.getPedestal())*LSB/voltsPerSPE;
                H_NPE.get(sec, lay, com).fill(i*4, npeWave);
		
		
		
	    }
            
	    double waveMax = 0.;
	    waveMax =  - fadcFitter.getPedestal();
            waveMax = waveMax + fadcFitter.getWave_Max();
	    vMaxEvent[sec][lay][com] = waveMax*LSB;
	    
	    
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
		.fill(counter.getChannels().get(0).
		      getADC().get(0)*LSB*4.0/50);
            
            H_NOISE_Q.get(sec, lay, com)
		.fill(counter.getChannels().get(0).
		      getADC().get(0)*LSB*4.0/50);
            	    
            double waveMax = 0.;
	    waveMax =  - fadcFitter.getPedestal();
            waveMax = waveMax + fadcFitter.getWave_Max();
	    
	    double voltMax = waveMax*LSB;
            
	    double npeMax  = 0.;
	    
	    if(detType == 1)
		npeMax = voltMax/gain_mV[sec][lay][com];
	                
	    // Matching Hits in layers
            if( detType == 1               &&
		gain[sec][lay][com] > 15.0 &&
		gain[sec][opp][com] > 15.0){
                
                double npeOtherLayer  = npeEvent[sec][opp][com];
                double meanNPEOther   = meanNPE[sec][opp][com];
		
                double sigNPEOther    = sigNPE[sec][opp][com];
                double npeLowLimOther = meanNPEOther - abs(sigNPEOther);
                
                //if ( npeLowLimOther < 3.0 )
		//npeLowLimOther = 3.0;
		
		//if ( npeOtherLayer > npeLowLimOther )
		
		double threshDV = (double)threshD * LSB;
		
 		if( vMaxEvent[sec][lay][com] > threshDV &&
 		    vMaxEvent[sec][lay][opp] > threshDV
		    ){
		    
// 		    System.out.println(" vMaxEvent["+sec+"]["+lay+"]["+com+"] = " + vMaxEvent[sec][lay][com]);
// 		    System.out.println(" vMaxEvent["+sec+"]["+opp+"]["+com+"] = " + vMaxEvent[sec][opp][com]);
// 		    System.out.println(" threshDV                             = " + threshDV);
		    
		    if(npeEvent[sec][lay][com] > 0.0)
			H_NPE_MATCH.get(sec, lay, com)
			    .fill(npeEvent[sec][lay][com]);
		    
		}
	    }
	    
            H_W_MAX.fill(index,waveMax);
            H_V_MAX.fill(index,voltMax);
            H_NPE_MAX.fill(index,npeMax);
            
	    H_NOISE_V.get(sec, lay, com).
		fill(voltMax);
	    
	    if( lay > 0 &&
		voltMax > vMax[sec][lay][com])
		vMax[sec][lay][com] = voltMax;

	    if (lay > 0   && 
		fadcFitter.getTime(3) > 10){
		
		time_M3[sec-1][lay-1][com-1] = fadcFitter.getTime(3);
		time_M7[sec-1][lay-1][com-1] = fadcFitter.getTime(7);
	    
		H_TIME_MODE3.get(sec, 
				 lay, 
				 com).fill(time_M3[sec-1][lay-1][com-1]);
		
		H_MAXV_VS_T.get(sec,
				lay,
				com).fill(time_M3[sec-1][lay-1][com-1],
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
		
                if (time_M3[secM][1][comM] > 0 &&
                    time_M3[secM][0][comM] > 0){
		    
		    dT_M3[secM][comM] =  - time_M3[secM][1][comM];
		    dT_M3[secM][comM] += time_M3[secM][0][comM];
		    
		    H_DT_MODE3.get(secM+1, 1, comM+1).
			fill(dT_M3[secM][comM]);
		    
		    if (time_M7[secM][1][comM] > 0 &&
			time_M7[secM][0][comM] > 0){
			
			dT_M7[secM][comM] = - time_M7[secM][1][comM];
			dT_M7[secM][comM] +=  time_M7[secM][0][comM];
			
			H_DT_MODE7.get(secM+1, 1, comM+1).
			fill(dT_M7[secM][comM]);
			
			H_T1_T2.get(secM+1, 1, comM+1).
			    fill(time_M7[secM][0][comM],
				 time_M7[secM][1][comM]);
			
		    }
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
        
	double hwmax =  H_W_MAX.getBinContent(indexSel);
	    
	if( tabSel == tabIndexEvent ){
	    
	    if(H_W_MAX.getBinContent(indexSel) > threshold){
	    
// 	    drawCanvasEvent(secSel,
// 			    laySel,
// 			    comSel);
	    
	    }
	}
        else if( tabSel == tabIndexNoise &&
		 (nDecodedProcessed%(10*repaintFrequency)==0) ) {
	    
	    if(H_W_MAX.getBinContent(indexSel) > threshold){
		drawCanvasNoise(secSel,
				laySel,
				comSel);
	    }
        }
        else if( tabSel == tabIndexPed &&
		 (nDecodedProcessed%(10*repaintFrequency)==0) ) {
            
            drawCanvasPed(secSel,
			  laySel,
			  comSel);
            
        }
        else if( tabSel == tabIndexCharge  &&
                (nDecodedProcessed%(10*repaintFrequency)==0) ) {
            
            drawCanvasCharge(secSel,
                             laySel,
                             comSel);
            
        }
	//==========================================
        
        if(nDecodedProcessed%repaintFrequency==0)
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
        
//         double rms = 0;
//         double pedestal = 0;
//         double wave_max=0;
	int    fadctime=0;
        
//         public double getPedestal() {
//             return pedestal;
//         }
        
//         public double getRMS() {
//             return rms;
//         }
        
//         public double getWave_Max() {
//             return wave_max;
//         }
	public int getADCtime() {
	    return fadctime;
	}
	
	//============================================================

        private double rms = 0;
        private double pedestal = 0;
        private double wave_max=0;
        private double pulse_max=0;
        private double half_max=0;
        private int    thresholdCrossing=0;
        private int    pulsePosition=0;
        private double time_3 = 0;
        private double time_7 = 0;
        private double time_f = 0;
        private double width = 0;

        public double getPedestal() {
            return pedestal;
        }

        public double getRMS() {
            return rms;
        }

        public double getWave_Max() {
            return wave_max;
        }

        public double getPulse_Max() {
            return pulse_max;
        }

        public double getHalf_Max() {
            return half_max;
        }

        public int getThresholdCrossing() {
            return thresholdCrossing;
        }

        public int getPulsePosition() {
            return pulsePosition;
        }
        
        public double getTime(int mode) {
            double time=0;
            if(mode==3)       time = time_3;
            else if(mode==7){  
		time = time_7;
	    }
            else System.out.println(" Unknown mode for time calculation, check...");
            return time;
        }

        public double getTimeF() {
            return time_f;
        }

        public double getFWHM() {
            return width;
        }		    

	//===========================================================
        
        public void fit(DetectorChannel dc) {
            short[] pulse = dc.getPulse();

	    //==================================================
	    double ped    = 0.0;
            double noise  = 0;
            double wmax   = 0;
            double pmax   = 0;
            int    tcross = 0; 
            int    ppos   = 0;
            // calculate pedestal means and noise
            for (int bin = ped_i1; bin < ped_i2; bin++) {
                ped   += pulse[bin];
                noise += pulse[bin] * pulse[bin];
            }
            pedestal = ped / (ped_i2 - ped_i1);
            rms = LSB * Math.sqrt(noise / (ped_i2 - ped_i1) - pedestal * pedestal);
            // determine waveform max
            for (int bin=0; bin<pulse.length; bin++) {
                if(pulse[bin]>wmax) wmax=pulse[bin];
            }
            wave_max=wmax;
            // find threshold crossing in pulse region: this determines mode-3 time (4 ns resolution)
            for (int bin=pul_i1; bin<pul_i2; bin++) {
                if(pulse[bin]>pedestal+threshold) {
                    tcross=bin;
                    break;
                }
            }
            thresholdCrossing=tcross;
            time_3=tcross*nsPerSample;
	    //System.out.println("time_3 = " + time_3 );

	    // find pulse max
            for (int bin=thresholdCrossing; bin<(pulse.length-1); bin++) { 
                if (pulse[bin+1]<pulse[bin]){ 
                    pmax=pulse[bin];
                    ppos=bin;
                    break; 
                }
            }
            pulse_max=pmax;
            pulsePosition=ppos;
            // calculating high resolution time    
            double halfMax = (pmax+pedestal)/2;
            half_max = halfMax;
            time_7 = time_3;
            int t0 = -1;
            int t1 = -1;
            if(tcross>0) { 
                for (int bin=tcross-1; bin<pul_i2; bin++) {
                    if (pulse[bin]<=halfMax && pulse[bin+1]>halfMax) {
                        t0 = bin;
                        break;
                    }
                }
                for (int bin=ppos; bin<pul_i2; bin++) {
                    if (pulse[bin]>halfMax && pulse[bin+1]<=halfMax) {
                        t1 = bin;
                        break;
                    }
                }
                if(t0>-1) { 
		    // int t1 = t0 + 1;
                    int a0 = pulse[t0];
                    int a1 = pulse[t0+1];
		    //final double slope = (a1 - a0); // units = ADC/sample
		    //final double yint = a1 - slope * t1;  // units = ADC 
                    time_7 = ((halfMax - a0)/(a1-a0) + t0)* nsPerSample;

		}
                if(t1>-1 && t0>-1) {
                    int a0 = pulse[t1];
                    int a1 = pulse[t1+1];
                    time_f = t1*nsPerSample;//((halfMax - a0)/(a1-a0) + t1)* nsPerSample;
                    width  = time_f - time_7;
                }
            }
	    
	    
	    
	    //==================================================
	    
	}	
    }
    
}
