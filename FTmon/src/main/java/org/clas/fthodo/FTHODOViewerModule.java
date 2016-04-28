package org.clas.fthodo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

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

import org.jlab.containers.HashTable;
import org.jlab.containers.HashTableViewer;
import org.jlab.containers.IHashTableListener;

import org.root.attr.ColorPalette;

import org.root.func.F1D;

import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;

import org.root.basic.EmbeddedCanvas;

public class FTHODOViewerModule implements IDetectorListener,
					   IHashTableListener,
					   ActionListener,
					   ChangeListener{
    EventDecoder decoder;
    
    //=================================
    //    PANELS, CANVASES ETC
    //=================================
    JPanel         detectorPanel;
    
    JPanel canvasPane = new JPanel(new BorderLayout());
    
    EmbeddedCanvas canvasEvent  = new EmbeddedCanvas();
    EmbeddedCanvas canvasNoise  = new EmbeddedCanvas();
    EmbeddedCanvas canvasGain   = new EmbeddedCanvas();
    EmbeddedCanvas canvasCharge = new EmbeddedCanvas();
    EmbeddedCanvas canvasMIP    = new EmbeddedCanvas();
    EmbeddedCanvas canvasMatch  = new EmbeddedCanvas();
    EmbeddedCanvas canvasTime   = new EmbeddedCanvas();
    
    public EmbeddedCanvas canvasHODOEvent  = new EmbeddedCanvas();
    
    DetectorShapeTabView view = new DetectorShapeTabView();
    HashTable  summaryTable   = null;
    ColorPalette      palette = new ColorPalette();
    
    //=================================
    //     HISTOGRAMS, GRAPHS
    //=================================
    
    //---------------
    // Event Viewing
    // raw pulse
    DetectorCollection<H1D> H_WAVE = new DetectorCollection<H1D>();
    // baseline subtracted pulse calibrated to voltage and time
    DetectorCollection<H1D> H_CWAVE = new DetectorCollection<H1D>();
    // '' calibrated to no. photoelectrons and time
    DetectorCollection<H1D> H_NPE = new DetectorCollection<H1D>();
    
    DetectorCollection<H1D> H_MAXV = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE_CHARGE = new DetectorCollection<H1D>();
    
    DetectorCollection<H1D> H_NPE_INT   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NPE_NOISE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NPE_MATCH = new DetectorCollection<H1D>();
    
    DetectorCollection<H1D> H_FADCSAMPLE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_FADCSAMPLEdiff = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_fADC = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_fADC   = new DetectorCollection<H1D>();
    
    DetectorCollection<F1D> mylandau = new DetectorCollection<F1D>();
    DetectorCollection<F1D> mygauss = new DetectorCollection<F1D>();
    DetectorCollection<F1D> myfunctNoise1 = new DetectorCollection<F1D>();
    DetectorCollection<F1D> myfunctNoise2 = new DetectorCollection<F1D>();
    DetectorCollection<F1D> myfunctCosmic = new DetectorCollection<F1D>();
    //DetectorCollection<F1D> myfunctGain   = new DetectorCollection<F1D>();

    DetectorCollection<Integer> dcHits = new DetectorCollection<Integer>();
    
    H1D H_fADC_N     = null;
    H1D H_WMAX       = null;
    H1D H_CHARGE_MAX = null;
    H1D H_COSMIC_N   = null;
    
    //=================================
    //           ARRAYS
    //=================================
    
    private double meanNPE[][][];
    private double errNPE[][][]; 
    private double gain[][][]; 
    private double errGain[][][];
    
    private double npeEvent[][][];
    
    //=================================
    //           CONSTANTS
    //=================================
    
    int    fADCBins      = 4096;
    double voltageMax    = 2000; // for run < 230 max = 1000 mV
    double LSB           = voltageMax/fADCBins;
    
    double thrshNPE      = 10.;
    double thrshNoiseNPE = 0.5;
    
    double voltsPerSPE   = 38.; // approximate for now
    
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
    
    final int CosmicQXMin[]  = {200,300}; 
    final int CosmicQXMax[]  = {5200,5300};
    
    final int CosmicNPEXMin[]  = {0,3,5};
    final int CosmicNPEXMax[]  = {200,53,85};
    
    
    //=================================
    //           VARIABLES
    //=================================
    
    double   tile_size = 15;
    int      nProcessed = 0;
    
    private int tabSelect = 0;
    private int comSelect = 0;
    private int secSelect = 0;
    private int laySelect = 0;
    
    final private int tabIndexEvent  = 0;
    final private int tabIndexNoise  = 1;
    final private int tabIndexGain   = 2;
    final private int tabIndexCharge = 3;
    final private int tabIndexMIP    = 4;
    final private int tabIndexMatch  = 5;
    final private int tabIndexTime   = 6;
    final private int tabIndexTable  = 7;
        
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
    
    public void initPanel() {
        
        JSplitPane splitPane = new JSplitPane();
        
        this.initTable();
	
        HashTableViewer canvasTable = new HashTableViewer(summaryTable);
        canvasTable.addListener(this);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Event"  ,this.canvasEvent);
        tabbedPane.add("Noise"  ,this.canvasNoise);
	tabbedPane.add("Gain"   ,this.canvasGain);
        tabbedPane.add("Charge" ,this.canvasCharge);
	tabbedPane.add("MIP"    ,this.canvasMIP);
	tabbedPane.add("Match"  ,this.canvasMatch);
	tabbedPane.add("Time"   ,this.canvasTime);
        tabbedPane.add("Table"  ,canvasTable);
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
        
        JButton tableBtn = new JButton("Table");
        tableBtn.addActionListener(this);
        buttonPane.add(tableBtn);
        
        ButtonGroup group = new ButtonGroup();
        
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
	
        this.canvasNoise.setGridX(false);
        this.canvasNoise.setGridY(false);
        this.canvasNoise.setAxisFontSize(10);
        this.canvasNoise.setTitleFontSize(16);
        this.canvasNoise.setAxisTitleFontSize(14);
        this.canvasNoise.setStatBoxFontSize(8);
        this.canvasNoise.divide(2,2);
	
        this.canvasGain.setGridX(false);
        this.canvasGain.setGridY(false);
        this.canvasGain.setAxisFontSize(10);
        this.canvasGain.setTitleFontSize(16);
        this.canvasGain.setAxisTitleFontSize(14);
        this.canvasGain.setStatBoxFontSize(2);
	this.canvasGain.divide(3,3);
	
        this.canvasCharge.setGridX(false);
        this.canvasCharge.setGridY(false);
        this.canvasCharge.setAxisFontSize(10);
        this.canvasCharge.setTitleFontSize(16);
        this.canvasCharge.setAxisTitleFontSize(14);
        this.canvasCharge.setStatBoxFontSize(2);
	this.canvasCharge.divide(2,2);
	
        this.canvasMIP.setGridX(false);
        this.canvasMIP.setGridY(false);
        this.canvasMIP.setAxisFontSize(10);
        this.canvasMIP.setTitleFontSize(16);
        this.canvasMIP.setAxisTitleFontSize(14);
        this.canvasMIP.setStatBoxFontSize(2);
	this.canvasMIP.divide(3,3);
	
        this.canvasMatch.setGridX(true);
        this.canvasMatch.setGridY(true);
        this.canvasMatch.setAxisFontSize(10);
        this.canvasMatch.setTitleFontSize(16);
        this.canvasMatch.setAxisTitleFontSize(14);
        this.canvasMatch.setStatBoxFontSize(8);
	this.canvasMatch.divide(2,2);

        this.canvasTime.setGridX(false);
        this.canvasTime.setGridY(false);
        this.canvasTime.setAxisFontSize(10);
        this.canvasTime.setTitleFontSize(16);
        this.canvasTime.setAxisTitleFontSize(14);
        this.canvasTime.setStatBoxFontSize(8);
        this.canvasTime.divide(2,2);
    
    }
    
    private void initTable() {
        summaryTable = new HashTable(3,
                                     "NPE:d",
				     "NPE Error:d",
				     "Gain:d",
				     "Gain Error:d");
        
	double[] summaryInitialValues = {-1,-1,-1,-1};
	
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
		    summaryTable.addConstrain(5, 40.0, 70.0);
		    
                }
            }
        }
        
    } // end of : private void initTable() {
    
    
    public FTHODOViewerModule(){
        this.detectorPanel=null;
        this.decoder=null;
    }
    
    public void initDetector(){
        DetectorShapeView2D viewFTHODO = this.drawDetector(0.0, 0.0);
        this.view.addDetectorLayer(viewFTHODO);
        
        DetectorShapeView2D viewPaddles = this.drawPaddles(0.0, 0.0);
        this.view.addDetectorLayer(viewPaddles);

	DetectorShapeView2D viewChannels = this.drawChannels(0.0, 0.0);
        this.view.addDetectorLayer(viewChannels);
	
        view.addDetectorListener(this);
    }
    
    public int getComp4ChMez(int ch,
			     int mez){
	
	int compM[][] = {{1 ,5 ,8 ,4 ,2 ,3 ,1 ,9 ,1 ,9 ,2 ,3 ,8 ,4 ,1 ,5 },
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

	int comp = compM[mez-1][ch];
	
	return comp;
	
    }
    
    public int getSect4ChMez(int ch,
			     int mez){
	
	int sectM[][] = {{2,1,8,8,8,8,6,6,6,6,8,8,8,8,2,1},
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
	
	int sect = sectM[mez-1][ch];
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
        DetectorShapeView2D viewChannels = new DetectorShapeView2D("HODO ELEC");
        
        int nChannels = 16;
	int nMezzPlus = 16;
		
	int sec = 1;
	int com = 0;
	int lay = 1;
	
	int width = 10;
	
	for(int iMez = 1 ; iMez < nMezzPlus ; iMez++){
	    lay = 1;
	    for(int iCh=0; iCh < nChannels; iCh++) {
		
		if( iCh > 7 ) lay = 2;
		
		if( iMez == 15 && channelIsEmpty(iCh)) continue;
		
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
        DetectorShapeView2D viewFTHODO = new DetectorShapeView2D("FTHODO");
        
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
        double zcenter = 0;
        
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
                    if     (quadrant==0) xcenter = xx[element];
                    else if(quadrant==1) xcenter =-yy[element];
                    else if(quadrant==2) xcenter =-xx[element];
                    else if(quadrant==3) xcenter = yy[element];
                    
		    // calculate the y-element of the center of each tile
                    if     (quadrant==0) ycenter = yy[element] + layerOffsetY[layerI];
                    else if(quadrant==1) ycenter = xx[element] + layerOffsetY[layerI];
                    else if(quadrant==2) ycenter =-yy[element] + layerOffsetY[layerI];
                    else if(quadrant==3) ycenter =-xx[element] + layerOffsetY[layerI];
                    
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
                    if(quadrant==0)      ycenter = yy[element];
                    else if(quadrant==1) ycenter = xx[element];
                    else if(quadrant==2) ycenter =-yy[element];
                    else if(quadrant==3) ycenter =-xx[element];
                    
                    shape2.setColor(0, 0, 0, 0);
                    
                    shape2.getShapePath().translateXYZ(xcenter, zcenter, 0);
                    
                    viewFTHODO.addShape(shape2);
                    
                }
            }
        }
        
	return viewFTHODO;
    }
    
    public void actionPerformed(ActionEvent e) {
	
	//System.out.println("ACTION = " + e.getActionCommand());
        
	if (e.getActionCommand().compareTo("Reset") == 0) {
            resetHistograms();
        }
        
        if (e.getActionCommand().compareTo("Table") == 0) {
            updateTable();
        }
        if (e.getActionCommand().compareTo("Fit") == 0) {
            //!!!
	    fitNoiseHistograms();
        }
        
    }
    
    private void fitNoiseHistograms() {
        
	int    binNmax = 0;
        double maxCont = 0;
        
	HistogramParam HistParam =  new HistogramParam();
        
	// Do the fitting for all components
	for (int index = 0; index < 232; index++) {
	    
	    HistParam.setAll(index);
            
	    int[] sLC = {HistParam.getSect(),
			 HistParam.getLayer(),
			 HistParam.getComp()};
            
	    H1D HNS = H_NOISE_CHARGE.get(sLC[0], sLC[1], sLC[2]);
            H1D HCS = H_COSMIC_CHARGE.get(sLC[0], sLC[1], sLC[2]);
            
	    initFitNoiseParams(sLC[0], sLC[1], sLC[2], HNS, HCS);
            
            if (myfunctNoise1.hasEntry(sLC[0], sLC[1], sLC[2])){
        
		H_NOISE_CHARGE.get(sLC[0], sLC[1], sLC[2]).
		    fit(myfunctNoise1.get(sLC[0], sLC[1], sLC[2]),"NR");
                
		System.out.println("Fitted Noise1 Index = " + index +
				   " Sector =" + sLC[0] +
				   " Layer = " + sLC[1] +
				   " Component="+ sLC[2]);
            }
	    
            if (myfunctNoise2.hasEntry(sLC[0], sLC[1], sLC[2])){

                H_NOISE_CHARGE.get(sLC[0], sLC[1], sLC[2]).
		    fit(myfunctNoise2.get(sLC[0], sLC[1], sLC[2]),"NR");
                
		System.out.println("Fitted Noise Index = " + index +
				   " Sector = " + sLC[0] + 
				   " Layer = " + sLC[1] +
				   " Component="+ sLC[2]);
            }
            
	    if(myfunctCosmic.hasEntry(sLC[0], sLC[1], sLC[2])){
                
		H_COSMIC_CHARGE.get(sLC[0], sLC[1], sLC[2]).
		    fit(myfunctCosmic.get(sLC[0], sLC[1], sLC[2]),"NR");
                
		System.out.println("Fitted Cosminc Index = " + index +
				   " Sector = " + sLC[0] +
				   " Layer = " + sLC[1] +
				   " Component = " + sLC[2]);
            }
        
	} // end of : for (int index = 0; index < 232; index++) {
        
	boolean flag_parnames = true;

	// Print out fit results
        for(int index = 0; index < 232; index++) {

            HistParam.setAll(index);
            
	    int[] sLC = {HistParam.getSect(),
			 HistParam.getLayer(),
			 HistParam.getComp()};
            
	    if(myfunctNoise1.hasEntry(sLC[0], sLC[1], sLC[2]) && 
	       myfunctNoise2.hasEntry(sLC[0], sLC[1], sLC[2])) {
                
		if(flag_parnames) {
                    System.out.println("Index\t Sector\t Layer\t Component\t " +
				       "amp\t mean\t sigma\t " + 
				       "amp\t mean\t sigma\t " + 
				       "amp\t mean\t sigma");
                    
		    flag_parnames=false;
		}
                
		System.out.print(index+ "\t" + sLC[0] + "\t " +sLC[1] + "\t "+sLC[2] + "\t ");
                
		if(myfunctNoise1.hasEntry(sLC[0], sLC[1], sLC[2])){
                    
		    for(int i=2; i<myfunctNoise1.get(sLC[0], sLC[1], sLC[2]).getNParams(); i++)
                        System.out.format("%.2f\t ",
					  myfunctNoise1.get(sLC[0], sLC[1], sLC[2]).getParameter(i));
                } 
		else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
                
		if(myfunctNoise2.hasEntry(sLC[0], sLC[1], sLC[2])){
		    
                    for(int i=0; i<myfunctNoise2.get(sLC[0], sLC[1], sLC[2]).getNParams(); i++)
                        System.out.format("%.2f\t ",
					  myfunctNoise2.get(sLC[0], sLC[1], sLC[2]).getParameter(i));
                }
                else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
		
                if(myfunctCosmic.hasEntry(sLC[0], sLC[1], sLC[2])){
                    for(int i=0; i<myfunctCosmic.get(sLC[0], sLC[1], sLC[2]).getNParams(); i++)
                        System.out.format("%.2f\t ",myfunctCosmic.get(sLC[0], sLC[1], sLC[2]).getParameter(i));
                }
                else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
                
		System.out.format("\n");
            }
        } // end of: for(int index = 0; index < 23
        
    }
    
    private void initFitNoiseParams(int sec, 
				    int lay, 
				    int com,
				    H1D hnoisetofit,
				    H1D hchargetofit) {
        
        double ampl = hnoisetofit.getBinContent(hnoisetofit.getMaximumBin());
        double mean = hnoisetofit.getMaximumBin()*3+10;
        double std  = 5.0;
	
        if (hnoisetofit.getEntries()>100){

	    myfunctNoise1.add(sec, lay, com, new F1D("exp+gaus", 10,  mean+25));
            myfunctNoise1.get(sec, lay, com).setParameter(0, ampl/5);
            myfunctNoise1.get(sec, lay, com).setParameter(1, -0.001);
            myfunctNoise1.get(sec, lay, com).setParameter(2, ampl);
            myfunctNoise1.get(sec, lay, com).setParameter(3, mean);
            myfunctNoise1.get(sec, lay, com).setParameter(4, std*3);
            myfunctNoise1.get(sec, lay, com).setParLimits(0, ampl/4.0, ampl);
            myfunctNoise1.get(sec, lay, com).setParLimits(1, -5, -0.0001);
            myfunctNoise1.get(sec, lay, com).setParLimits(2, ampl/2, ampl*2);
            myfunctNoise1.get(sec, lay, com).setParLimits(3, mean-25, mean+25);
            myfunctNoise1.get(sec, lay, com).setParLimits(4, 1, std*3.0);

            if (hnoisetofit.integral(23, 45)>50){
                myfunctNoise2.add(sec, lay, com, new F1D("gaus", mean+20, mean+100));
                myfunctNoise2.get(sec, lay, com).setParameter(0, ampl/5.0);
                myfunctNoise2.get(sec, lay, com).setParameter(1, mean+50);
                myfunctNoise2.get(sec, lay, com).setParameter(2, std);
                myfunctNoise2.get(sec, lay, com).setParLimits(0, 1, ampl/2.0);
                myfunctNoise2.get(sec, lay, com).setParLimits(1, mean+20, mean+100);
                myfunctNoise2.get(sec, lay, com).setParLimits(2, 1, std*3.0);
            }
        }

	int integralLowBin  = (500 - CosmicQXMin[lay-1])*NBinsCosmic;
	    integralLowBin  = integralLowBin/(CosmicQXMax[lay-1]-CosmicQXMin[lay-1]);
	    
	int integralHighBin = NBinsCosmic-1;
	
	if (hchargetofit.integral(integralLowBin,integralHighBin ) > 25){

            ampl = 0;
            mean = 0;
        
	    for (int i = integralLowBin; i < integralHighBin; i++){
                if (hchargetofit.getBinContent(i) > ampl){
                   
		    ampl = hchargetofit.getBinContent(i);
                    
		    mean = i*(CosmicQXMax[lay-1]-CosmicQXMin[lay-1]);
		    mean = mean/NBinsCosmic + CosmicQXMin[lay-1];
		}
            }
	    
	    myfunctCosmic.add(sec, lay, com, new F1D("landau", 500, 4500));
            myfunctCosmic.get(sec, lay, com).setParameter(0, ampl);
            myfunctCosmic.get(sec, lay, com).setParameter(1, mean);
            myfunctCosmic.get(sec, lay, com).setParameter(2, 150);
            myfunctCosmic.get(sec, lay, com).setParLimits(0, 0, ampl*2.0);
            myfunctCosmic.get(sec, lay, com).setParLimits(1, mean-400, mean+400);
            myfunctCosmic.get(sec, lay, com).setParLimits(2, 50, 1500);
        }
	
    }
    
    //=======================================================
    //               Detector Selected 
    //=======================================================
    public void detectorSelected(DetectorDescriptor desc) {
	//System.out.println("SELECTED = " + desc);
	
	String  detector;
	boolean doThings;
	
	if ( desc.getType() == DetectorType.FTCAL ){
	    detector = "FTCAL";
	}
	else if ( desc.getType() == DetectorType.FTHODO ){
	    detector = "FTHODO";
	}
	else{
	    detector = "Unidentified";
	    doThings = true;
	}
	
	System.out.println(" Detector = "  + detector);
	
        secSelect = desc.getSector();
        laySelect = desc.getLayer();
        comSelect = desc.getComponent();
	
	System.out.println(" Sector    = " +
			   secSelect    +
			   " Layer     = "  +
			   laySelect    +
			   " Component = " +
			   comSelect);
	//!! implement
	//getChan4SLC(secSelect,laySelect,comSelect);
	
	// map [1,2] to [0,1]
        int selectedLayerCDIndex = laySelect-1;
	// map [1,2] to [1,0]
        int otherLayerCDIndex    = laySelect%2;
	// map [1,2] to [2,1]
        int otherLayer           = (laySelect%2)+1;
        
	// skip calorimeter events that are not
	// from the trigger paddles
        boolean skip = false;
        
	// paddles (calorimeter)
        if(laySelect==0){
	    
	    // only plot paddles
            if(comSelect < 501)
                skip = true;
	    
            selectedLayerCDIndex = 0;
            otherLayerCDIndex    = 1;
            otherLayer = 0;
            
        }
        
        int selectedLayerCDIndexLeft =  2*selectedLayerCDIndex;
        int otherLayerCDIndexLeft    =  2*otherLayerCDIndex;
        
        int selectedLayerCDIndexRight = selectedLayerCDIndexLeft + 1;
        int otherLayerCDIndexRight    = otherLayerCDIndexLeft    + 1;
        
        
        if(!skip){
            
	    //============================================================
	    // Combined View
            
            if(comSelect > 500){
		
                int nPaddles = 4;
                
                this.canvasHODOEvent.divide(nPaddles, 1);
                
                for (int ipaddle = 0  ; ipaddle < nPaddles ; ipaddle++){
                    
                    canvasHODOEvent.cd(ipaddle);
                    
                    if(H_WAVE.hasEntry(secSelect,laySelect,501+ipaddle))
                        
                        this.canvasHODOEvent.draw(H_WAVE.get(secSelect,
                                                             laySelect,
                                                             501+ipaddle));
                }
                
            }
            else{
                
                this.canvasHODOEvent.divide(2, 1);
                
                canvasHODOEvent.cd(selectedLayerCDIndex);
                if(H_WAVE.hasEntry(secSelect,laySelect,comSelect))
                    this.canvasHODOEvent.draw(H_WAVE.get(secSelect,
                                                         laySelect,
                                                         comSelect));
                
                canvasHODOEvent.cd(otherLayerCDIndex);
                if(H_WAVE.hasEntry(secSelect,otherLayer,comSelect))
                    this.canvasHODOEvent.draw(H_WAVE.get(secSelect,
                                                         otherLayer,
                                                         comSelect));
            }
            
            
            
	    //============================================================
	    // Event Tab Selected
            if      ( tabSelect == this.tabIndexEvent ) {
		
                this.canvasEvent.divide(2, 2);
                
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
		// raw fADC pulse
                canvasEvent.cd(selectedLayerCDIndexLeft);
                
                if(H_WAVE.hasEntry(secSelect,laySelect,comSelect)){
                    this.canvasEvent.draw(H_WAVE.get(secSelect,
                                                     laySelect,
                                                     comSelect));
                    
                }
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
		// raw fADC pulse
                canvasEvent.cd(otherLayerCDIndexLeft);
                
                if(H_WAVE.hasEntry(secSelect,
                                   otherLayer,
                                   comSelect))
                    this.canvasEvent.draw(H_WAVE.get(secSelect,
                                                     otherLayer,
                                                     comSelect));
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
		// calibrated fADC pulse
                canvasEvent.cd(selectedLayerCDIndexRight);
                
                if(H_CWAVE.hasEntry(secSelect,
                                    laySelect,
                                    comSelect))
                    this.canvasEvent.draw(H_CWAVE.get(secSelect,
                                                      laySelect,
                                                      comSelect));
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
		// calibrated fADC pulse
                canvasEvent.cd(otherLayerCDIndexRight);
                if(H_CWAVE.hasEntry(secSelect,
                                    otherLayer,
                                    comSelect))
                    this.canvasEvent.draw(H_CWAVE.get(secSelect,
                                                      otherLayer,
                                                      comSelect));
                
            } 
              //============================================================
              // Noise Tab Selected
            if      ( tabSelect == this.tabIndexNoise ) {
                		
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
		// calibrated fADC pulse
                canvasNoise.cd(selectedLayerCDIndexLeft);
                
                if(H_CWAVE.hasEntry(secSelect,laySelect,comSelect)){
                    this.canvasNoise.draw(H_CWAVE.get(secSelect,
                                                      laySelect,
                                                      comSelect));
                    
                }
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
		// calibrated fADC pulse
                canvasNoise.cd(otherLayerCDIndexLeft);
                
                if(H_CWAVE.hasEntry(secSelect,
                                    otherLayer,
                                    comSelect))
                    this.canvasNoise.draw(H_CWAVE.get(secSelect,
                                                      otherLayer,
                                                      comSelect));
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
		// accumulated noise charge
                canvasNoise.cd(selectedLayerCDIndexRight);
                
                if(H_NOISE_CHARGE.hasEntry(secSelect,
                                           laySelect,
                                           comSelect))
                    this.canvasNoise.draw(H_NOISE_CHARGE.get(secSelect,
                                                             laySelect,
                                                             comSelect));
                if(myfunctNoise1.hasEntry(secSelect,
                                          laySelect,
                                          comSelect))
                    this.canvasNoise.draw(myfunctNoise1.get(secSelect,
                                                            laySelect,
                                                            comSelect),"same");
                if(myfunctNoise2.hasEntry(secSelect,
                                          laySelect,
                                          comSelect))
                    this.canvasNoise.draw(myfunctNoise2.get(secSelect,
                                                            laySelect,
                                                            comSelect),"same");
                
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
		// calibrated fADC pulse
                canvasNoise.cd(otherLayerCDIndexRight);
                if(H_NOISE_CHARGE.hasEntry(secSelect,
                                           otherLayer,
                                           comSelect))
                    this.canvasNoise.draw(H_NOISE_CHARGE.get(secSelect,
                                                             otherLayer,
                                                             comSelect));
                if(myfunctNoise1.hasEntry(secSelect,
                                          otherLayer,
                                          comSelect))
                    this.canvasNoise.draw(myfunctNoise1.get(secSelect,
                                                            otherLayer,
                                                            comSelect),"same");
                if(myfunctNoise2.hasEntry(secSelect,
                                          otherLayer,
                                          comSelect))
                    this.canvasNoise.draw(myfunctNoise2.get(secSelect,
                                                            otherLayer,
                                                            comSelect),"same");
                
                
                
                
            }
	    
	    //======================================================================
	    // Charge Tab Selected
            if      ( tabSelect == this.tabIndexCharge ) {
		
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
                canvasCharge.cd(selectedLayerCDIndexLeft);
                
                if(H_NOISE_CHARGE.hasEntry(secSelect,laySelect,comSelect)){
                    this.canvasCharge.draw(H_NOISE_CHARGE.get(secSelect,
                                                              laySelect,
                                                              comSelect));
                    if(myfunctNoise1.hasEntry(secSelect,
                                              laySelect,
                                              comSelect))
                        this.canvasCharge.draw(myfunctNoise1.get(secSelect,
                                                                 laySelect,
                                                                 comSelect),"same");
                    if(myfunctNoise2.hasEntry(secSelect,
                                              laySelect,
                                              comSelect))
                        this.canvasCharge.draw(myfunctNoise2.get(secSelect,
                                                                 laySelect,
                                                                 comSelect),"same");
                    
                }
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
                canvasCharge.cd(otherLayerCDIndexLeft);
                
                if(H_NOISE_CHARGE.hasEntry(secSelect,
                                           otherLayer,
                                           comSelect))
                    this.canvasCharge.draw(H_NOISE_CHARGE.get(secSelect,
                                                              otherLayer,
                                                              comSelect));
                if(myfunctNoise1.hasEntry(secSelect,
                                          otherLayer,
                                          comSelect))
                    this.canvasCharge.draw(myfunctNoise1.get(secSelect,
                                                             otherLayer,
                                                             comSelect),"same");
                if(myfunctNoise2.hasEntry(secSelect,
                                          otherLayer,
                                          comSelect))
                    this.canvasCharge.draw(myfunctNoise2.get(secSelect,
                                                             otherLayer,
                                                             comSelect),"same");
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
                canvasCharge.cd(selectedLayerCDIndexRight);
		
                if(H_COSMIC_CHARGE.hasEntry(secSelect,
                                            laySelect,
                                            comSelect)){
                    this.canvasCharge.draw(H_COSMIC_CHARGE.get(secSelect,
                                                               laySelect,
                                                               comSelect));
                    if(myfunctCosmic.hasEntry(secSelect,
                                              laySelect,
                                              comSelect))
                        this.canvasCharge.draw(myfunctCosmic.get(secSelect,
                                                                 laySelect,
                                                                 comSelect),"same");
                }
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
                canvasCharge.cd(otherLayerCDIndexRight);
                if(H_COSMIC_CHARGE.hasEntry(secSelect,
                                            otherLayer,
                                            comSelect)){
                    this.canvasCharge.draw(H_COSMIC_CHARGE.get(secSelect,
                                                               otherLayer,
                                                               comSelect));
                    if(myfunctCosmic.hasEntry(secSelect,
                                              otherLayer,
                                              comSelect))
                        this.canvasCharge.draw(myfunctCosmic.get(secSelect,
                                                                 otherLayer,
                                                                 comSelect),"same");
                }

		// if(H_NPE_INT.hasEntry(secSelect,
		// 		      laySelect,
		// 		      comSelect)){
                //     this.canvasCharge.draw(H_NPE_INT.get(secSelect,
		// 					 laySelect,
		// 					 comSelect));
		    
		// }
                
		// //----------------------------------------
		// // right top (bottom) for thin (thick) layer
                // canvasCharge.cd(otherLayerCDIndexRight);
                // if(H_NPE_INT.hasEntry(secSelect,
		// 		      otherLayer,
		// 		      comSelect)){
                //     this.canvasCharge.draw(H_NPE_INT.get(secSelect,
		// 					 otherLayer,
		// 					 comSelect));
                // }
                
            }
            if ( tabSelect == this.tabIndexMIP ) {
		
		int sector2CD[] = {4,0,1,2,5,8,7,6,3};
			
		int cd = sector2CD[secSelect];
		canvasMIP.cd(cd);
		
		int    nComponents[] = {9,20};
		
		GraphErrors[] G_NPE = null;
		
		double dummyNPE[][] = null;
		double compArr[]    = null;
		
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
		
		if(secSelect%2==1)
		    evenSecSelect = false;
		
		if(evenSecSelect){
		    for ( int i = 0 ; i < p15EvenI.length ; i++){
			if( comSelect == p15EvenI[i] ){
			    plotP30 = false;
			    break;
			}
		    }
		}
		else{
		    for ( int i = 0 ; i < p15OddI.length ; i++){
			if( comSelect == p15OddI[i] ){
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
		
		for( int lM = 0 ; lM < 2 ; lM++){
		    for (int c = 0 ; c < p30EvenI.length ; c++){
			p30EvenNPE[lM][c] = meanNPE[secSelect][lM+1][p30EvenI[c]];
			p30EvenERR[lM][c] = errNPE[secSelect][lM+1][p30EvenI[c]];
		    }
		    for (int c = 0 ; c < p15EvenI.length ; c++){
			p15EvenNPE[lM][c] = meanNPE[secSelect][lM+1][p15EvenI[c]];
			p15EvenERR[lM][c] = errNPE[secSelect][lM+1][p15EvenI[c]];
		    }
		    
		    for (int c = 0 ; c < p30OddI.length ; c++){
			p30OddNPE[lM][c] = meanNPE[secSelect][lM+1][p30OddI[c]];
			p30OddERR[lM][c] = errNPE[secSelect][lM+1][p30OddI[c]];
		    }
		    for (int c = 0 ; c < p15OddI.length ; c++){
			p15OddNPE[lM][c] = meanNPE[secSelect][lM+1][p15OddI[c]];
			p15OddERR[lM][c] = errNPE[secSelect][lM+1][p15OddI[c]];
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
		    
		    G_NPE[layerM].setTitle(" "); 
		    G_NPE[layerM].setXTitle("component");
		    G_NPE[layerM].setYTitle("NPE mean ");
		    G_NPE[layerM].setMarkerSize(5); 
		    G_NPE[layerM].setMarkerColor(layerM+1); // 0-9 for given palette
		    G_NPE[layerM].setMarkerStyle(layerM+1); // 1 or 2
		    
		}
		canvasMIP.draw(G_NPE[0]);
		canvasMIP.draw(G_NPE[1],"same");
		
	    }
	    //======================================================================
	    // Match Tab Selected
            if      ( tabSelect == this.tabIndexMatch ) {
		
                this.canvasMatch.divide(2, 2);
                
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
                canvasMatch.cd(selectedLayerCDIndexLeft);
                
                if(H_NOISE_CHARGE.hasEntry(secSelect,laySelect,comSelect)){
                    this.canvasMatch.draw(H_NOISE_CHARGE.get(secSelect,
							     laySelect,
							     comSelect));
                    if(myfunctNoise1.hasEntry(secSelect,
                                              laySelect,
                                              comSelect))
                        this.canvasMatch.draw(myfunctNoise1.get(secSelect,
								laySelect,
								comSelect),"same");
                    if(myfunctNoise2.hasEntry(secSelect,
                                              laySelect,
                                              comSelect))
                        this.canvasMatch.draw(myfunctNoise2.get(secSelect,
								laySelect,
								comSelect),"same");
                    
                }
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
                canvasMatch.cd(otherLayerCDIndexLeft);
                
                if(H_NOISE_CHARGE.hasEntry(secSelect,
                                           otherLayer,
                                           comSelect))
                    this.canvasMatch.draw(H_NOISE_CHARGE.get(secSelect,
							     otherLayer,
							     comSelect));
                if(myfunctNoise1.hasEntry(secSelect,
                                          otherLayer,
                                          comSelect))
                    this.canvasMatch.draw(myfunctNoise1.get(secSelect,
							    otherLayer,
							    comSelect),"same");
                if(myfunctNoise2.hasEntry(secSelect,
                                          otherLayer,
                                          comSelect))
                    this.canvasMatch.draw(myfunctNoise2.get(secSelect,
							    otherLayer,
							    comSelect),"same");
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
                canvasMatch.cd(selectedLayerCDIndexRight);
		
		if(H_NPE_MATCH.hasEntry(secSelect,
					laySelect,
					comSelect)){
                    this.canvasMatch.draw(H_NPE_MATCH.get(secSelect,
							  laySelect,
							  comSelect));
		}
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
                canvasMatch.cd(otherLayerCDIndexRight);
                if(H_NPE_MATCH.hasEntry(secSelect,
					otherLayer,
					comSelect)){
                    this.canvasMatch.draw(H_NPE_MATCH.get(secSelect,
							  otherLayer,
							  comSelect));
                }
                
            }
        } // end of: if(!skip){..
        
    } // end of: public void detectorSelected(DetectorD....
    
    public Color getComponentStatus(int sector, int layer, int component) {
        
        int sector_count[] = {0,9,29,38,58,67,87,96};
        int index;
        
        if(layer==0) index = component;
        else index = (layer - 1 ) *116+sector_count[sector-1]+component;
        Color col = new Color(100,100,100);
        
        if(H_WMAX.getBinContent(index)>cosmicsThrsh) {
            col = palette.getColor3D(H_WMAX.getBinContent(index), 4000, true);
	    //            col = new Color(200, 0, 200);
        }
        return col;
    }
    
    // for all shapes made this is executed
    // for every event and every action
    public void update(DetectorShape2D shape) {
        int sector    = shape.getDescriptor().getSector();
        int layer     = shape.getDescriptor().getLayer();
        int component = shape.getDescriptor().getComponent();
        
        int sector_count[] = {0,9,29,38,58,67,87,96};
        
        int index;
        
        if(shape.getDescriptor().getType() == DetectorType.FTCAL){
            sector = 0;
            layer  = 0;
            index = component;
        }
        else
            index = (layer-1)*116+sector_count[sector-1]+component;
        
	// 	System.out.println("update: layer = " + layer +
	// 			   ", sector = " + sector +
	// 			   ", component = " + component );
        
	// shape.setColor(200, 200, 200);
	// System.out.println("Bin Content n" +index + "="+ H_WMAX.getBinContent(index));
        
        double waveMax     = H_WMAX.getBinContent(index);
        int    signalAlpha = (int)(waveMax-1)*128/4096 + 128;
        
        int    noiseAlpha  = 255;
        if( (signalAlpha-128) < 25)
            noiseAlpha = (signalAlpha-128)*10;
        
	if( tabSelect == tabIndexEvent ) {
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
	else if( tabSelect == tabIndexNoise ) {
            if      ( waveMax > noiseThrsh) {
                shape.setColor(255, 255, 0, (256/4)-1);
            }
            if      ( waveMax > 2*noiseThrsh) {
                shape.setColor(255, 255, 0, (256/2)-1);
            }
        }
        else if( tabSelect == tabIndexMIP ){
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
    
    private double getGain(int s, int l, int c){
	
	double gain = 0.0;
	
	if(myfunctNoise1.hasEntry(s, l, c) &&
	   myfunctNoise2.hasEntry(s, l, c)){
	    
	    double n2 = myfunctNoise2.get(s,
					  l,
					  c).getParameter(1);
	    
	    double n1 = myfunctNoise1.get(s,
					  l,
					  c).getParameter(3);
	    	    
	    gain = n2 - n1;
	    
	}
	
	if (gain < 30.0 || 
	    gain > 70.0)
	    gain = 0.0;
	
	return gain;
    }
    
    private double getGainError(int s, int l, int c){
	
	double gainError = 0.0;
	
	if(myfunctNoise1.hasEntry(s, l, c) &&
	   myfunctNoise2.hasEntry(s, l, c)){
	    
	    double n2Error = myfunctNoise2.get(s,
					       l,
					       c).getParError(1);
	    
	    double n1Error = myfunctNoise1.get(s,
					       l,
					       c).getParError(3);
	    
	    gainError    = n2Error - n1Error;
	    
	}
	
	return gainError;
	
    }
    
    private double getQMean(int s, int l, int c){
	
	double qMean = 0.0;
	
	if(myfunctCosmic.hasEntry(s, l, c)){
	    
	    qMean = myfunctCosmic.get(s,
				      l,
				      c).getParameter(1);
	    
	}
	
	return qMean;
	
    }

    private double getQMeanError(int s, int l, int c){
	
	double qMeanError = 0.0;
	
	if(myfunctCosmic.hasEntry(s, l, c)){
	    
	    qMeanError = myfunctCosmic.get(s,
					   l,
					   c).getParError(1);
	    
	}
	
	return qMeanError;
	
    }
    
    private double getNpeMean(int s, int l, int c){
	
	double npeMean = 0.0;
	
	if( getGain(s,l,c) > 0.0 )
	    npeMean = getQMean(s,l,c)/getGain(s,l,c);
	
	return npeMean;
	
    }

    private double getNpeError(int s, int l, int c){
	
	double npeError = 0.0;
	
	if( getQMean(s,l,c) > 0.0 &&
	    getGain(s,l,c)  > 0.0 )
	    
	npeError = getQMeanError(s,l,c)*getQMeanError(s,l,c);
	
	npeError = npeError / ( getQMean(s,l,c)*getQMean(s,l,c));
	npeError = npeError + 
	    (getGainError(s,l,c)*getGainError(s,l,c)/
	     (getGain(s,l,c)*getGain(s,l,c)));
	
	npeError = sqrt(npeError);
	npeError = getNpeMean(s,l,c)*npeError;
	
	return npeError;
	
    }

    private void updateTable() {
	
	for (int s = 1; s < 9; s++) {
            for (int l = 1; l < 3; l++) {
                for ( int c = 1 ; c < 21 ; c++){
		    		    
                    if(s%2==1 && c > 9 ) continue;
		    
		    meanNPE[s][l][c] = getNpeMean(s,l,c);
		    errNPE[s][l][c]  = getNpeError(s,l,c);
		    gain[s][l][c]    = getGain(s,l,c);
		    errGain[s][l][c] = getGainError(s,l,c);
		    
		    summaryTable.setValueAtAsDouble(0,
						    meanNPE[s][l][c],
						    s,
						    l,
						    c);

		    summaryTable.setValueAtAsDouble(1,
						    errNPE[s][l][c],
						    s,
						    l,
						    c);
		    
    
		    summaryTable.setValueAtAsDouble(2,
						    gain[s][l][c],
						    s,
						    l,
						    c);

		    summaryTable.setValueAtAsDouble(3,
						    errGain[s][l][c],
						    s,
						    l,
						    c);
		    
		}
	    }
	}
        summaryTable.show();
     	this.view.repaint();
	
    } // end of: private void updateTable() {
    
    public void stateChanged(ChangeEvent e) {
        JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
        tabSelect = sourceTabbedPane.getSelectedIndex();
        System.out.println("Tab changed to: " +
                           sourceTabbedPane.getTitleAt(tabSelect) +
                           " with index " + tabSelect);
        
        if(tabSelect == tabIndexTable)
            this.updateTable();
        
	this.view.repaint();
    }
    
    public void initHistograms() {
        
	this.initArrays();
	
	HistogramParam HistPara = new HistogramParam();
	
        for (int index = 0; index < 232; index++) {
            
            HistPara.setAll(index);
            
            int[] sLC = {HistPara.getSect(),
			 HistPara.getLayer(),
			 HistPara.getComp()};
            
	    // Non-accumulated
	    //
	    // 0 - waveforms
	    // 1 - waveforms calibrated in time and voltage
	    // 2 - voltage / npe voltage peak (40 mV for now)
            
            H_WAVE.add(sLC[0],
                       sLC[1],
                       sLC[2],
                       new H1D(DetectorDescriptor.getName("WAVE",
                                                          sLC[0],
                                                          sLC[1],
                                                          sLC[2]),
                               HistPara.getTitle(), 100, 0.0, 100.0));
            H_WAVE.get(sLC[0],
                       sLC[1],
                       sLC[2]).setFillColor(4);
            H_WAVE.get(sLC[0],
                       sLC[1],
                       sLC[2]).setXTitle("fADC Sample");
            H_WAVE.get(sLC[0],
                       sLC[1],
                       sLC[2]).setYTitle("fADC Counts");
            
            H_CWAVE.add(sLC[0],
                        sLC[1],
                        sLC[2],
                        new H1D(DetectorDescriptor.getName("Calibrated",
                                                           sLC[0],
                                                           sLC[1],
                                                           sLC[2]),
                                HistPara.getTitle(), 100, 0.0, 400.0));
            H_CWAVE.get(sLC[0],
                        sLC[1],
                        sLC[2]).setFillColor(3);
            H_CWAVE.get(sLC[0],
                        sLC[1],
                        sLC[2]).setXTitle("Time (ns)");
            H_CWAVE.get(sLC[0],
                        sLC[1],
                        sLC[2]).setYTitle("Voltage (mV)");
            
            H_NPE.add(sLC[0],sLC[1], sLC[2],
                      new H1D(DetectorDescriptor.getName("NPE",
                                                         sLC[0],
                                                         sLC[1],
                                                         sLC[2]),
                              HistPara.getTitle(), 100, 0.0, 400.0));
            H_NPE.get(sLC[0],sLC[1],
                      sLC[2]).setFillColor(4);
            H_NPE.get(sLC[0],
                      sLC[1],
                      sLC[2]).setXTitle("Time (ns)");
            H_NPE.get(sLC[0],
                      sLC[1],
                      sLC[2]).setYTitle("Voltage / SPE Voltage");
            
            
	    //
	    // Accumulated
	    //
	    // 10 - Max Pulse Voltage
	    // 11 - Charge
	    // ...
            
            
	    
		
	    H_COSMIC_CHARGE.add(sLC[0],
				sLC[1],
				sLC[2],
				new H1D(DetectorDescriptor.getName("Cosmic Charge",
								   sLC[0],
								   sLC[1],
								   sLC[2]),
					HistPara.getTitle(),
					NBinsCosmic,
					CosmicQXMin[sLC[1]-1],
					CosmicQXMax[sLC[1]-1]));
            
	    H_COSMIC_CHARGE.get(sLC[0],
                                sLC[1],
                                sLC[2]).setFillColor(3);
            H_COSMIC_CHARGE.get(sLC[0],
                                sLC[1],
                                sLC[2]).setXTitle("Charge (pC)");
            H_COSMIC_CHARGE.get(sLC[0],
                                sLC[1],
                                sLC[2]).setYTitle("Counts");
	    
            
            H_NOISE_CHARGE.add(sLC[0],
                               sLC[1],
                               sLC[2],
                               new H1D(DetectorDescriptor.getName("Noise Charge",
                                                                  sLC[0],
                                                                  sLC[1],
                                                                  sLC[2]),
                                       HistPara.getTitle(), 100, 10.0, 310.0));
            H_NOISE_CHARGE.get(sLC[0],
                               sLC[1],
                               sLC[2]).setFillColor(5);
            H_NOISE_CHARGE.get(sLC[0],
                               sLC[1],
                               sLC[2]).setXTitle("Charge (pC)");
            H_NOISE_CHARGE.get(sLC[0],
                               sLC[1],
                               sLC[2]).setYTitle("Counts");
	    
	    H_NPE_INT.add(sLC[0],
			  sLC[1],
			  sLC[2],
			  new H1D(DetectorDescriptor.getName("NPE integrated",
							     sLC[0],
							     sLC[1],
							     sLC[2]),
				  HistPara.getTitle(), 100,
				  CosmicNPEXMin[sLC[1]],
				  CosmicNPEXMax[sLC[1]]));
			  
            H_NPE_INT.get(sLC[0],
			  sLC[1],
			  sLC[2]).setFillColor(6);
            H_NPE_INT.get(sLC[0],
			  sLC[1],
			  sLC[2]).setXTitle("npe (peak/gain)");
            H_NPE_INT.get(sLC[0],
			  sLC[1],
			  sLC[2]).setYTitle("Counts");

	    H_NPE_MATCH.add(sLC[0],
			    sLC[1],
			    sLC[2],
			    new H1D(DetectorDescriptor.getName("NPE int, matched layers",
							       sLC[0],
							       sLC[1],
							       sLC[2]),
				    HistPara.getTitle(), 100,
				    0,
				    CosmicNPEXMax[sLC[1]]));
			  
            H_NPE_MATCH.get(sLC[0],
			    sLC[1],
			    sLC[2]).setFillColor(7);
            H_NPE_MATCH.get(sLC[0],
			    sLC[1],
			    sLC[2]).setXTitle("npe (peak/gain)");
            H_NPE_MATCH.get(sLC[0],
			    sLC[1],
			    sLC[2]).setYTitle("Counts");

	    
            
            H_fADC.add(sLC[0],sLC[1], sLC[2],
                       new H1D(DetectorDescriptor.getName("fADC", sLC[0],sLC[1],sLC[2]),
                               HistPara.getTitle(), 100, 0.0, 100.0));
            
            H_NOISE.add(sLC[0],sLC[1], sLC[2],
                        new H1D(DetectorDescriptor.getName("Noise", sLC[0],sLC[1],sLC[2]),
                                HistPara.getTitle(), 200, 0.0, 10.0));
            H_NOISE.get(sLC[0],sLC[1], sLC[2]).setFillColor(4);
            H_NOISE.get(sLC[0],sLC[1], sLC[2]).setXTitle("RMS (mV)");
            H_NOISE.get(sLC[0],sLC[1], sLC[2]).setYTitle("Counts");
            
            
            
            H_MAXV.add(sLC[0],sLC[1], sLC[2],
                       new H1D(DetectorDescriptor.getName("WAVEMAX",
                                                          sLC[0],
                                                          sLC[1],
                                                          sLC[2]),
                               HistPara.getTitle(), 150, 0.0, 150));
            H_MAXV.get(sLC[0],sLC[1], sLC[2]).setFillColor(4);
            H_MAXV.get(sLC[0],sLC[1], sLC[2]).setXTitle("Waveform Max (mV)");
            H_MAXV.get(sLC[0],sLC[1], sLC[2]).setYTitle("Counts");
            
            H_FADCSAMPLE.add(sLC[0],sLC[1], sLC[2], new H1D(DetectorDescriptor.getName("FADCSAMPLE", sLC[0],sLC[1],sLC[2]), HistPara.getTitle(), 100, 0.0, 400));
            H_FADCSAMPLE.get(sLC[0],sLC[1], sLC[2]).setFillColor(4);
            H_FADCSAMPLE.get(sLC[0],sLC[1], sLC[2]).setXTitle("time (ns) ");
            H_FADCSAMPLE.get(sLC[0],sLC[1], sLC[2]).setYTitle("Counts");
            
            if (sLC[1]==1){
                H_FADCSAMPLEdiff.add(sLC[0],sLC[1], sLC[2], new H1D(DetectorDescriptor.getName("FADCSAMPLEdiff", sLC[0],sLC[1],sLC[2]), HistPara.getTitle(), 14, -28, 28));
                H_FADCSAMPLEdiff.get(sLC[0],sLC[1], sLC[2]).setFillColor(4);
                H_FADCSAMPLEdiff.get(sLC[0],sLC[1], sLC[2]).setXTitle("#Delta time (ns) ");
                H_FADCSAMPLEdiff.get(sLC[0],sLC[1], sLC[2]).setYTitle("Counts");
            }
            
            
            H_COSMIC_fADC.add(sLC[0],sLC[1], sLC[2], new H1D(DetectorDescriptor.getName("Cosmic fADC", sLC[0],sLC[1],sLC[2]), HistPara.getTitle(), 100, 0.0, 100.0));
            H_COSMIC_fADC.get(sLC[0],sLC[1], sLC[2]).setFillColor(3);
            H_COSMIC_fADC.get(sLC[0],sLC[1], sLC[2]).setXTitle("fADC Sample");
            H_COSMIC_fADC.get(sLC[0],sLC[1], sLC[2]).setYTitle("fADC Counts");
            
            
            mylandau.add(sLC[0],sLC[1], sLC[2], new F1D("landau", 0.0, 80.0));
            mygauss.add(sLC[0],sLC[1], sLC[2], new F1D("gaus", -20, 20.0));
            
        }
        
	// calorimeter
        for (int index = 0; index < 505; index++) {
            
            HistPara.setAll(index);
            
            int[] sLC = {0,
			 0,
			 index};
            
	    // Non-accumulated
	    //
	    // 0 - waveforms
	    // 1 - waveforms calibrated in time and voltage
	    // 2 - voltage / npe voltage peak (40 mV for now)
            
            H_WAVE.add(sLC[0],
                       sLC[1],
                       sLC[2],
                       new H1D(DetectorDescriptor.getName("WAVE",
                                                          sLC[0],
                                                          sLC[1],
                                                          sLC[2]),
                               HistPara.getTitle(), 100, 0.0, 100.0));
            H_WAVE.get(sLC[0],
                       sLC[1],
                       sLC[2]).setFillColor(4);
            H_WAVE.get(sLC[0],
                       sLC[1],
                       sLC[2]).setXTitle("fADC Sample");
            H_WAVE.get(sLC[0],
                       sLC[1],
                       sLC[2]).setYTitle("fADC Counts");
            
            H_CWAVE.add(sLC[0],
                        sLC[1],
                        sLC[2],
                        new H1D(DetectorDescriptor.getName("Calibrated",
                                                           sLC[0],
                                                           sLC[1],
                                                           sLC[2]),
                                HistPara.getTitle(), 100, 0.0, 400.0));
            H_CWAVE.get(sLC[0],
                        sLC[1],
                        sLC[2]).setFillColor(3);
            H_CWAVE.get(sLC[0],
                        sLC[1],
                        sLC[2]).setXTitle("Time (ns)");
            H_CWAVE.get(sLC[0],
                        sLC[1],
                        sLC[2]).setYTitle("Voltage (mV)");
            
            H_NPE.add(sLC[0],sLC[1], sLC[2],
                      new H1D(DetectorDescriptor.getName("NPE",
                                                         sLC[0],
                                                         sLC[1],
                                                         sLC[2]),
                              HistPara.getTitle(), 100, 0.0, 400.0));
            H_NPE.get(sLC[0],sLC[1],
                      sLC[2]).setFillColor(4);
            H_NPE.get(sLC[0],
                      sLC[1],
                      sLC[2]).setXTitle("Time (ns)");
            H_NPE.get(sLC[0],
                      sLC[1],
                      sLC[2]).setYTitle("Voltage / SPE Voltage");
            
            
	    //
	    // Accumulated
	    //
	    // 10 - Max Pulse Voltage
	    // 11 - Charge
	    // ...
	    
            
            H_COSMIC_CHARGE.add(sLC[0],
                                sLC[1],
                                sLC[2],
                                new H1D(DetectorDescriptor.getName("Cosmic Charge",
                                                                   sLC[0],
                                                                   sLC[1],
                                                                   sLC[2]),
                                        HistPara.getTitle(),
					NBinsCosmic,
					0,
					200));
            H_COSMIC_CHARGE.get(sLC[0],
                                sLC[1],
                                sLC[2]).setFillColor(3);
            H_COSMIC_CHARGE.get(sLC[0],
                                sLC[1],
                                sLC[2]).setXTitle("Charge (pC)");
            H_COSMIC_CHARGE.get(sLC[0],
                                sLC[1],
                                sLC[2]).setYTitle("Counts");
            
            
            H_NOISE_CHARGE.add(sLC[0],
                               sLC[1],
                               sLC[2],
                               new H1D(DetectorDescriptor.getName("Noise Charge",
                                                                  sLC[0],
                                                                  sLC[1],
                                                                  sLC[2]),
                                       HistPara.getTitle(), 100, 10.0, 310.0));
            H_NOISE_CHARGE.get(sLC[0],
                               sLC[1],
                               sLC[2]).setFillColor(5);
            H_NOISE_CHARGE.get(sLC[0],
                               sLC[1],
                               sLC[2]).setXTitle("Charge (pC)");
            H_NOISE_CHARGE.get(sLC[0],
                               sLC[1],
                               sLC[2]).setYTitle("Counts");
	    
            
	    H_NPE_INT.add(sLC[0],
			  sLC[1],
			  sLC[2],
			  new H1D(DetectorDescriptor.getName("NPE integrated",
							     sLC[0],
							     sLC[1],
							     sLC[2]),
				  HistPara.getTitle(), 100,
				  CosmicNPEXMin[sLC[1]],
				  CosmicNPEXMax[sLC[1]]));
			  	    
            H_NPE_INT.get(sLC[0],
			  sLC[1],
			  sLC[2]).setFillColor(6);
            H_NPE_INT.get(sLC[0],
			  sLC[1],
			  sLC[2]).setXTitle("npe (peak/gain)");
            H_NPE_INT.get(sLC[0],
			  sLC[1],
			  sLC[2]).setYTitle("Counts");
	    
	    
	    H_NPE_MATCH.add(sLC[0],
			    sLC[1],
			    sLC[2],
			    new H1D(DetectorDescriptor.getName("NPE int, matched layers",
							       sLC[0],
							       sLC[1],
							       sLC[2]),
				    HistPara.getTitle(), 100,
				    0,
				    CosmicNPEXMax[sLC[1]]));
			  
            H_NPE_MATCH.get(sLC[0],
			    sLC[1],
			    sLC[2]).setFillColor(7);
            H_NPE_MATCH.get(sLC[0],
			    sLC[1],
			    sLC[2]).setXTitle("npe (peak/gain)");
            H_NPE_MATCH.get(sLC[0],
			    sLC[1],
			    sLC[2]).setYTitle("Counts");


            H_fADC.add(sLC[0],sLC[1], sLC[2],
                       new H1D(DetectorDescriptor.getName("fADC", sLC[0],sLC[1],sLC[2]),
                               HistPara.getTitle(), 100, 0.0, 100.0));
            
            H_NOISE.add(sLC[0],sLC[1], sLC[2],
                        new H1D(DetectorDescriptor.getName("Noise", sLC[0],sLC[1],sLC[2]),
                                HistPara.getTitle(), 200, 0.0, 10.0));
            H_NOISE.get(sLC[0],sLC[1], sLC[2]).setFillColor(4);
            H_NOISE.get(sLC[0],sLC[1], sLC[2]).setXTitle("RMS (mV)");
            H_NOISE.get(sLC[0],sLC[1], sLC[2]).setYTitle("Counts");
            
            
            
            H_MAXV.add(sLC[0],sLC[1], sLC[2],
                       new H1D(DetectorDescriptor.getName("WAVEMAX",
                                                          sLC[0],
                                                          sLC[1],
                                                          sLC[2]),
                               HistPara.getTitle(), 150, 0.0, 150));
            H_MAXV.get(sLC[0],sLC[1], sLC[2]).setFillColor(4);
            H_MAXV.get(sLC[0],sLC[1], sLC[2]).setXTitle("Waveform Max (mV)");
            H_MAXV.get(sLC[0],sLC[1], sLC[2]).setYTitle("Counts");
            
            H_FADCSAMPLE.add(sLC[0],sLC[1], sLC[2], new H1D(DetectorDescriptor.getName("FADCSAMPLE", sLC[0],sLC[1],sLC[2]), HistPara.getTitle(), 100, 0.0, 400));
            H_FADCSAMPLE.get(sLC[0],sLC[1], sLC[2]).setFillColor(4);
            H_FADCSAMPLE.get(sLC[0],sLC[1], sLC[2]).setXTitle("time (ns) ");
            H_FADCSAMPLE.get(sLC[0],sLC[1], sLC[2]).setYTitle("Counts");
            
            if (sLC[1]==1){
                H_FADCSAMPLEdiff.add(sLC[0],sLC[1], sLC[2], new H1D(DetectorDescriptor.getName("FADCSAMPLEdiff", sLC[0],sLC[1],sLC[2]), HistPara.getTitle(), 14, -28, 28));
                H_FADCSAMPLEdiff.get(sLC[0],sLC[1], sLC[2]).setFillColor(4);
                H_FADCSAMPLEdiff.get(sLC[0],sLC[1], sLC[2]).setXTitle("#Delta time (ns) ");
                H_FADCSAMPLEdiff.get(sLC[0],sLC[1], sLC[2]).setYTitle("Counts");
            }
            
            
            H_COSMIC_fADC.add(sLC[0],sLC[1], sLC[2], new H1D(DetectorDescriptor.getName("Cosmic fADC", sLC[0],sLC[1],sLC[2]), HistPara.getTitle(), 100, 0.0, 100.0));
            H_COSMIC_fADC.get(sLC[0],sLC[1], sLC[2]).setFillColor(3);
            H_COSMIC_fADC.get(sLC[0],sLC[1], sLC[2]).setXTitle("fADC Sample");
            H_COSMIC_fADC.get(sLC[0],sLC[1], sLC[2]).setYTitle("fADC Counts");
            
            
            mylandau.add(sLC[0],sLC[1], sLC[2], new F1D("landau", 0.0, 80.0));
            mygauss.add(sLC[0],sLC[1], sLC[2], new F1D("gaus", -20, 20.0));
            
        }
        
        H_fADC_N   = new H1D("fADC", 504, 0, 504);
        H_WMAX     = new H1D("WMAX", 504, 0, 504);
        H_COSMIC_N = new H1D("EVENT", 504, 0, 504);
        
    }
    
    
    
    public void resetHistograms() {
        HistogramParam HistPara =  new HistogramParam();
        
	// hodoscope
        for (int index = 0; index < 232; index++) {
            
            HistPara.setAll(index);
            
            int[] sLC = {HistPara.getSect(),
			 HistPara.getLayer(),
			 HistPara.getComp()};
            
            H_COSMIC_CHARGE.get(sLC[0],
                                sLC[1],
                                sLC[2]).reset();
            
            H_NOISE_CHARGE.get(sLC[0],
                               sLC[1],
                               sLC[2]).reset();
            
	    H_NPE_INT.get(sLC[0],
			  sLC[1],
			  sLC[2]).reset();

	    H_NPE_MATCH.get(sLC[0],
			    sLC[1],
			    sLC[2]).reset();
	    

	    H_fADC.get(sLC[0],
                       sLC[1],
                       sLC[2]).reset();
            
            H_NOISE.get(sLC[0],
                        sLC[1],
                        sLC[2]).reset();
            
            H_fADC_N.reset();
            
            H_COSMIC_fADC.get(sLC[0],
                              sLC[1],
                              sLC[2]).reset();
            
            H_COSMIC_N.reset();
        }
	// calorimeter
        for (int index = 0; index < 501; index++) {
            
            HistPara.setAll(index);
            
            int[] sLC = {0,
			 0,
			 HistPara.getComp()};
            
            H_COSMIC_CHARGE.get(sLC[0],
                                sLC[1],
                                sLC[2]).reset();
            
            H_NOISE_CHARGE.get(sLC[0],
                               sLC[1],
                               sLC[2]).reset();

	    H_NPE_INT.get(sLC[0],
			  sLC[1],
			  sLC[2]).reset();

	    H_NPE_MATCH.get(sLC[0],
			    sLC[1],
			    sLC[2]).reset();

            H_fADC.get(sLC[0],
                       sLC[1],
                       sLC[2]).reset();
            
            H_NOISE.get(sLC[0],
                        sLC[1],
                        sLC[2]).reset();
            
            H_fADC_N.reset();
            
            H_COSMIC_fADC.get(sLC[0],
                              sLC[1],
                              sLC[2]).reset();
            
            H_COSMIC_N.reset();
        }
        
        
        
	
    }
    

    public void initArrays() {
	
	meanNPE  = new double[9][3][21];
        errNPE   = new double[9][3][21];
	gain     = new double[9][3][21];
	errGain  = new double[9][3][21];
	
	npeEvent = new double[9][3][21];
	
	for (int s = 0; s < 9; s++) {
            for (int l = 0; l < 3; l++) {
                for ( int c = 0 ; c < 21 ; c++){
                    this.meanNPE[s][l][c]  = 0.0;
		    this.errNPE[s][l][c]   = 0.0;
		    this.gain[s][l][c]     = 0.0;
		    this.errGain[s][l][c]  = 0.0;
		    this.npeEvent[s][l][c] = 0.0;
		}
	    }
	}


    }
    
    
    
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
        
        H_WMAX.reset();
        
        int[][][] timediff = new int[8][2][20];
        
        int nPosADC;
        int nNegADC;
	
	
	//=-=-=-=-=-=-=-=-=-=-=-=-=-        
	// Loop One
	for (DetectorCounter counter : counters) {
	    
	    if(detType!=1) break;
	    
	    fadcFitter.fit(counter.getChannels().get(0));
	    
            int sector     = counter.getDescriptor().getSector();
            int layer      = counter.getDescriptor().getLayer();
            int component  = counter.getDescriptor().getComponent();
	    
	    short pulse[] = counter.getChannels().get(0).getPulse();
	    
	    double npeWave;
	    
	    int index;
            int sector_count[] = {0,9,29,38,58,67,87,96};
	    if(counter.getDescriptor().getType() == DetectorType.FTHODO)
                index = (layer -1 ) *116+sector_count[sector-1]+component;
            
            else{
                index  = component;
                sector = 0;
                layer  = 0;
            }
            	    
	    H_fADC_N.fill(index);

	    //npeWave = pulse[i]*LSB/voltsPerSPE;

            double calibratedWave;
	    double baselineSubRaw;
	    double npeEventSLC;

	    // reset non-accumulating histograms
	    H_WAVE.get(sector, layer, component).reset();
	    H_CWAVE.get(sector, layer, component).reset();
	    H_NPE.get(sector, layer, component).reset();
	    
	    npeEvent[sector][layer][component] = 0.0;
	    
	    if(gain[sector][layer][component] > 0.0){
		
		npeEvent[sector][layer][component] = counter.getChannels().
		    get(0).getADC().get(0)*LSB*4.0/50./gain[sector][layer][component];
		
		npeEventSLC = npeEvent[sector][layer][component];
		
		H_NPE_INT.get(sector, layer, component).fill(npeEventSLC);
	    
	    }
	    // Loop through fADC bins filling histograms
            for (int i = 0;
                 i < Math.min(pulse.
			      length,
                              H_fADC.
			      get(sector,
				  layer,
				  component).
			      getAxis().
			      getNBins());
                 i++) {
		
		//System.out.println("pulse[" + i + "] = " + pulse[i]);
                
		H_WAVE.get(sector, layer, component).fill(i, pulse[i]);
                
                baselineSubRaw = pulse[i] - fadcFitter.getPedestal() + 10.0;
                
                H_fADC.get(sector, layer, component).fill(i,baselineSubRaw);
                
                calibratedWave = (pulse[i]-fadcFitter.getPedestal())*LSB + 5.0;
                
                H_CWAVE.get(sector,
                            layer,
                            component).fill(i*4,calibratedWave);
		
		
		npeWave = (pulse[i] - fadcFitter.getPedestal())*LSB/voltsPerSPE + 1;
		
		H_NPE.get(sector, layer, component).fill(i*4, npeWave);
	    }
	    
	    H_WMAX.fill(index,fadcFitter.getWave_Max()-fadcFitter.getPedestal());
	    
	}


	//=-=-=-=-=-=-=-=-=-=-=-=-=-        
	// Loop Two
	for (DetectorCounter counter : counters) {
	    
	    fadcFitter.fit(counter.getChannels().get(0));
	    
            int sector     = counter.getDescriptor().getSector();
            int layer      = counter.getDescriptor().getLayer();
            int component  = counter.getDescriptor().getComponent();
	    
	    int otherLayer = (layer%2)+1;
	    
	    // System.out.println("sector: " + sector + "  layer:" + layer + "  component:" + component);

	    // System.out.println(counters.size() + " " + icounter + " " + counter.getDescriptor().getComponent());
	    // System.out.println(counter);
                        
            //short pulse[] = counter.getChannels().get(0).getPulse();
	    
                        
	    int index;
	    int sector_count[] = {0,9,29,38,58,67,87,96};
	    if(counter.getDescriptor().getType() == DetectorType.FTHODO)
                index = (layer -1 ) *116+sector_count[sector-1]+component;
            
            else{
                index  = component;
                sector = 0;
                layer  = 0;
            }
	    
	    H_fADC_N.fill(index);
	    
            H_COSMIC_CHARGE.get(sector, layer, component)
		.fill(counter.getChannels().get(0).getADC().get(0)*LSB*4.0/50);
            
            H_NOISE_CHARGE.get(sector, layer, component)
		.fill(counter.getChannels().get(0).getADC().get(0)*LSB*4.0/50);
	    
	    // Matching Hits in layers
	    if( detType == 1                         && 
		gain[sector][layer][component] > 0.0 && 
		gain[sector][otherLayer][component] > 0.0){
		
		double npeOtherLayer = npeEvent[sector][otherLayer][component];
				
		double meanNPEOther    = meanNPE[sector][otherLayer][component];
		double errNPEOther     = errNPE[sector][otherLayer][component];
		double npeLowLimOther  = meanNPEOther - abs(errNPEOther);
		
		if ( npeLowLimOther < 5.0 )
		    npeLowLimOther = 5.0;
		
		if ( npeOtherLayer > npeLowLimOther ){
		    
		    // System.out.println(" =--=--=--=--=--=--=--=--=--=--" );
		    // System.out.println(" sector        = " + sector);
		    // System.out.println(" layer         = " + layer);
		    // System.out.println(" component     = " + component);
		    // System.out.println(" otherLayer    = " + otherLayer);
		    // System.out.println(" npeOtherLayer = " + npeOtherLayer);
		    // System.out.println(" npeLow        = " + npeLow);
		    // System.out.println(" =--=--=--=--=--=--=--=--=--=--" );
		    
		    H_NPE_MATCH.get(sector, layer, component)
			.fill(npeEvent[sector][layer][component]);
		}
	    }
	    	    
	    // 	    if(sector == 5 && layer == 1 && component == 5) {
	    // 		System.out.println(" nProcessed = " + nProcessed);
	    // 		System.out.println(" ADC        = " + counter.getChannels().get(0).getADC());
	    // 	    }
            
            if (fadcFitter.getWave_Max()-fadcFitter.getPedestal()>10)
                H_MAXV.get(sector, layer, component).fill((fadcFitter.getWave_Max()-fadcFitter.getPedestal())*LSB);
            
            if (layer > 0 && fadcFitter.getADCtime()>0){
                H_FADCSAMPLE.get(sector, layer, component).fill(fadcFitter.getADCtime()*4.0);
                timediff[sector-1][layer-1][component-1]=fadcFitter.getADCtime()*4;
            }
	    
            
            if(fadcFitter.getWave_Max()-fadcFitter.getPedestal()>cosmicsThrsh)
		// System.out.println("   Component #" + component + " is above threshold, max=" + fadcFitter.getWave_Max() + " ped=" + fadcFitter.getPedestal());
                H_NOISE.get(sector, layer, component).fill(fadcFitter.getRMS());
            
	    
        } // end of: for (DetectorCounter counter : counters) {
        
        
        for (int isect = 0; isect < 8; isect++) {
            for (int icomponent = 0; icomponent < 20; icomponent++) {
                if ((isect+1)%2==1 && icomponent>8)
                    continue;
                if (timediff[isect][1][icomponent] > 0 &&
                    timediff[isect][0][icomponent] > 0)
                    H_FADCSAMPLEdiff.get(isect+1, 1, icomponent+1).fill(timediff[isect][1][icomponent]-timediff[isect][0][icomponent]);
            }
        }
        
	//=======================================================
	//             DRAW HISTOGRAMS PER EVENT
	//=======================================================
	//   User chooses which histogram/s to display
        
	// map [1,2] to [0,1]
        int selectedLayerCDIndex = laySelect-1;
	// map [1,2] to [1,0]
        int otherLayerCDIndex    = laySelect%2;
	// map [1,2] to [2,1]
        int otherLayer           = (laySelect%2)+1;
        
        boolean skip = false;
        
	// paddles (calorimeter )
        if(laySelect==0) {
            if(comSelect < 501) skip = true;
            selectedLayerCDIndex = 0;
            otherLayerCDIndex    = 1;
            otherLayer = 0;
        }
        
        int selectedLayerCDIndexLeft =  2*selectedLayerCDIndex;
        int otherLayerCDIndexLeft    =  2*otherLayerCDIndex;
        
        int selectedLayerCDIndexRight = selectedLayerCDIndexLeft + 1;
        int otherLayerCDIndexRight    = otherLayerCDIndexLeft    + 1;
        
	//============================================================
	// Combined View
        
        
        if(!skip){
	    
	    //============================================================
	    // Event Tab Selected
            
            if      ( tabSelect == tabIndexEvent && (nProcessed%repaintFrequency==0) ) {
                
                this.canvasEvent.divide(2, 2);
                
                
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
		// raw fADC pulse
                canvasEvent.cd(selectedLayerCDIndexLeft);
                
                if(H_WAVE.hasEntry(secSelect,laySelect,comSelect)){
                    this.canvasEvent.draw(H_WAVE.get(secSelect,
                                                     laySelect,
                                                     comSelect));
                    
                }
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
		// raw fADC pulse
                canvasEvent.cd(otherLayerCDIndexLeft);
		
                if(H_WAVE.hasEntry(secSelect,
                                   otherLayer,
                                   comSelect))
                    this.canvasEvent.draw(H_WAVE.get(secSelect,
                                                     otherLayer,
                                                     comSelect));
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
		// calibrated fADC pulse
                canvasEvent.cd(selectedLayerCDIndexRight);
                
                if(H_CWAVE.hasEntry(secSelect,
                                    laySelect,
                                    comSelect))
                    this.canvasEvent.draw(H_CWAVE.get(secSelect,
                                                      laySelect,
                                                      comSelect));
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
		// calibrated fADC pulse
                canvasEvent.cd(otherLayerCDIndexRight);
                if(H_CWAVE.hasEntry(secSelect,
                                    otherLayer,
                                    comSelect))
                    this.canvasEvent.draw(H_CWAVE.get(secSelect,
                                                      otherLayer,
                                                      comSelect));
                
            } 
	    //======================================================================
	    // Noise Tab Selected
            if      ( tabSelect == tabIndexNoise && 
		      (nProcessed%(10*repaintFrequency)==0) ) {
                
                this.canvasNoise.divide(2, 2);
                
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
                canvasNoise.cd(selectedLayerCDIndexLeft);
                
                if(H_CWAVE.hasEntry(secSelect,laySelect,comSelect)){
                    this.canvasNoise.draw(H_CWAVE.get(secSelect,
                                                      laySelect,
                                                      comSelect));
                    
                }
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
                canvasNoise.cd(otherLayerCDIndexLeft);
                
                if(H_CWAVE.hasEntry(secSelect,
                                    otherLayer,
                                    comSelect))
                    this.canvasNoise.draw(H_CWAVE.get(secSelect,
                                                      otherLayer,
                                                      comSelect));
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
                canvasNoise.cd(selectedLayerCDIndexRight);
                
                if(H_NOISE_CHARGE.hasEntry(secSelect,
                                           laySelect,
                                           comSelect))
                    this.canvasNoise.draw(H_NOISE_CHARGE.get(secSelect,
                                                             laySelect,
                                                             comSelect));
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
                canvasNoise.cd(otherLayerCDIndexRight);
                if(H_NOISE_CHARGE.hasEntry(secSelect,
                                           otherLayer,
                                           comSelect))
                    this.canvasNoise.draw(H_NOISE_CHARGE.get(secSelect,
                                                             otherLayer,
                                                             comSelect));
                
            } 
              //======================================================================
              // Charge Tab Selected
            if      ( tabSelect == tabIndexCharge  &&
		      (nProcessed%(10*repaintFrequency)==0) ) {
                
                this.canvasCharge.divide(2, 2);
                
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
                canvasCharge.cd(selectedLayerCDIndexLeft);
                
                if(H_NOISE_CHARGE.hasEntry(secSelect,laySelect,comSelect)){
                    this.canvasCharge.draw(H_NOISE_CHARGE.get(secSelect,
                                                              laySelect,
                                                              comSelect));
                    
                }
		//----------------------------------------
		// left top (bottom) for thin (thick) layer
                canvasCharge.cd(otherLayerCDIndexLeft);
                
                if(H_NOISE_CHARGE.hasEntry(secSelect,
                                           otherLayer,
                                           comSelect))
                    this.canvasCharge.draw(H_NOISE_CHARGE.get(secSelect,
                                                              otherLayer,
                                                              comSelect));
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
                canvasCharge.cd(selectedLayerCDIndexRight);
                
                if(H_COSMIC_CHARGE.hasEntry(secSelect,
                                            laySelect,
                                            comSelect))
                    this.canvasCharge.draw(H_COSMIC_CHARGE.get(secSelect,
                                                               laySelect,
                                                               comSelect));
                
		//----------------------------------------
		// right top (bottom) for thin (thick) layer
                canvasCharge.cd(otherLayerCDIndexRight);
                if(H_COSMIC_CHARGE.hasEntry(secSelect,
                                            otherLayer,
                                            comSelect))
                    this.canvasCharge.draw(H_COSMIC_CHARGE.get(secSelect,
                                                               otherLayer,
                                                               comSelect));
                // if(H_NPE_INT.hasEntry(secSelect,
		// 		      laySelect,
		// 		      comSelect))
                //     this.canvasCharge.draw(H_NPE_INT.get(secSelect,
		// 					 laySelect,
		// 					 comSelect));
                
		// //----------------------------------------
		// // right top (bottom) for thin (thick) layer
                // canvasCharge.cd(otherLayerCDIndexRight);
                // if(H_NPE_INT.hasEntry(secSelect,
		// 		      otherLayer,
		// 		      comSelect))
                //     this.canvasCharge.draw(H_NPE_INT.get(secSelect,
		// 					 otherLayer,
		// 					 comSelect));
                
            } 
	    //======================================================================
            
            if(nProcessed%repaintFrequency==0)
                this.view.repaint();
        }
	
	
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
    
    public class HistogramParam extends Object {
        
        public int    layer;
        public int    quadrant;
        public int    element;
        public int    sector;
        public int    component;
        public String title;
        public String layerStr;
        
        public void setAll(int index){
            
            if(index < 500){
                layer = index/116 + 1;
                
                if (layer==1)
                    layerStr = "Thin";
                else
                    layerStr = "Thick";
                
		// (map indices in both layers to [0,115])
		//  /map indices to quadrants [0,3]
                quadrant = (index-(layer-1)*116) / 29;  
                
		// map indices to [0,28]
                element = index - quadrant * 29 -(layer-1)*116; 
                
		// map quadrant to sectors [1,8]
		// map element to tiles [1,9] or
		// map element to tiles [1,20]
                
                if (element<9) {
                    sector  = quadrant*2 + 1;
                    component = element + 1;
                }
                else {
                    sector  = quadrant*2 +2;
                    component = element + 1 - 9;
                }
                title =  " " + layerStr + " S" + sector + " C" + component ;
                
            }
            else{
                layer = 0;
                
                quadrant = 0;
                element  = index;
                component = index;
                sector = 0;
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
        
        
        public int getLayer(){
            return layer;
        }
        
        public int getQuad(){
            return quadrant;
        }
        
        public int getElem(){
            return element;
        }
        
        public int getSect(){
            return sector;
        }
        
        public int getComp(){
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
            double pedlow = 0.0;    //pedestal calculated using bins ped_i1, ped_i2
            double pedhigh = 0.0;   //pedestal calculated using bins ped_j1, ped_j2
            int fadctimethre=0;
            double noise = 0;
            double wmax=0;
            
	    
	    for (int bin = ped_i1; bin < ped_i2; bin++) {
                pedlow += pulse[bin];
                noise += pulse[bin] * pulse[bin];
            }
            
            for (int bin = ped_j1; bin < ped_j2; bin++) {
                pedhigh += pulse[bin];
            }
            if (pedlow<pedhigh)
                pedestal = pedlow / (ped_i2 - ped_i1);
            else
                pedestal = pedhigh / (ped_j2 - ped_j1);
            
            
            for (int bin=0; bin<pulse.length; bin++) {
                if(pulse[bin]>wmax)
                    wmax=pulse[bin];
            }
            for (int bin=0; bin<pulse.length; bin++) {
                if(pulse[bin]-pedestal>100 && wmax-pedestal>cosmicsThrsh)
                    if (fadctimethre==0)
                        fadctimethre=bin;            
            }
            
	    // Returns the smallest pedestal value. 
	    // Works better if peak is close to the beginning of the histogram.
            
            rms = LSB * Math.sqrt(noise / (ped_i2 - ped_i1) - pedestal * pedestal);
            wave_max=wmax;
            fadctime=fadctimethre;
        }
        
    }
    
}

