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
    
    EmbeddedCanvas canvasEvent  = new EmbeddedCanvas();
    EmbeddedCanvas canvasNoise  = new EmbeddedCanvas();
    EmbeddedCanvas canvasGain   = new EmbeddedCanvas();
    EmbeddedCanvas canvasCharge = new EmbeddedCanvas();
    EmbeddedCanvas canvasMIP    = new EmbeddedCanvas();
    EmbeddedCanvas canvasMatch  = new EmbeddedCanvas();
    EmbeddedCanvas canvasTime   = new EmbeddedCanvas();
    
    public EmbeddedCanvas canvasHODOEvent  = new EmbeddedCanvas();
    
    DetectorShapeTabView view    = new DetectorShapeTabView();
    ColorPalette         palette = new ColorPalette();
    HashTable            summaryTable = null;

    //=================================
    //     HISTOGRAMS, GRAPHS
    //=================================
    
    //---------------
    // Event-by-Event
    // raw pulse
    DetectorCollection<H1D> H_WAVE = new DetectorCollection<H1D>();
    // baseline subtracted pulse calibrated to voltage and time
    DetectorCollection<H1D> H_CWAVE = new DetectorCollection<H1D>();
    // '' calibrated to no. photoelectrons and time
    DetectorCollection<H1D> H_NPE = new DetectorCollection<H1D>();
    
    // Accumulated
    DetectorCollection<H1D> H_MAXV = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_MIP_Q = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE_Q = new DetectorCollection<H1D>();
    
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
    DetectorCollection<F1D> f1Noise1 = new DetectorCollection<F1D>();
    DetectorCollection<F1D> f1Noise2 = new DetectorCollection<F1D>();
    DetectorCollection<F1D> f1MIP = new DetectorCollection<F1D>();

    DetectorCollection<Integer> dcHits = new DetectorCollection<Integer>();
    
    H1D H_W_MAX       = null;
    H1D H_V_MAX      = null;
    H1D H_NPE_MAX    = null;
    H1D H_CHARGE_MAX = null;
    
    //=================================
    //           ARRAYS
    //=================================
    
    private double meanNPE[][][];
    private double errNPE[][][]; 
    private double sigmaNPE[][][];
    
    private double gain[][][]; 
    private double errGain[][][];
    
    private double npeEvent[][][];
    
    //=================================
    //           CONSTANTS
    //=================================
    
    int    fADCBins      = 4096;
    double voltageMax    = 2000;
    double LSB           = voltageMax/fADCBins;
    
    double thrshNPE      = 10.;
    double thrshNoiseNPE = 0.5;
    
    double voltsPerSPE   = 38.; 
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
    final int CosmicNPEXMax[]  = {200,53,85};
    
    
    //=================================
    //           VARIABLES
    //=================================
    
    double   tile_size = 15;
    int      nProcessed = 0;
    
    private int tabSelect = 0;
    private int comSel = 0;
    private int secSel = 0;
    private int laySel = 0;
    
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
        
        JButton variablesBtn = new JButton("Variables");
        variablesBtn.addActionListener(this);
        buttonPane.add(variablesBtn);
        
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
        this.canvasEvent.divide(3,2);
	
        this.canvasNoise.setGridX(false);
        this.canvasNoise.setGridY(false);
        this.canvasNoise.setAxisFontSize(10);
        this.canvasNoise.setTitleFontSize(16);
        this.canvasNoise.setAxisTitleFontSize(14);
        this.canvasNoise.setStatBoxFontSize(8);
        this.canvasNoise.divide(3,2);
	
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
		
	int sec;
	int com;
	int lay;
	
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
	if (e.getActionCommand().compareTo("Variables") == 0) {
            updateVariables();
        }
        if (e.getActionCommand().compareTo("Fit") == 0) {
	    fitNoiseHistograms();
        }
        
    }
    
    private void fitNoiseHistograms() {
        
	int    binNmax = 0;
        double maxCont = 0;
        
	HistPara HP =  new HistPara();
	
	// Do the fitting for all components
	for (int index = 0; index < 232; index++) {
	    
	    HP.setAllParameters(index,'h');
            
	    H1D HNS = H_NOISE_Q.get(HP.getS(), HP.getL(), HP.getC());
            H1D HCS = H_MIP_Q.get(HP.getS(), HP.getL(), HP.getC());
            
	    initFitNoiseParams(HP.getS(), HP.getL(), HP.getC(), HNS, HCS);
            
            if (f1Noise1.hasEntry(HP.getS(), HP.getL(), HP.getC())){
        
		H_NOISE_Q.get(HP.getS(), HP.getL(), HP.getC()).
		    fit(f1Noise1.get(HP.getS(), HP.getL(), HP.getC()),"NR");
                
		System.out.println("Fitted Noise1 Index = " + index +
				   " Sector =" + HP.getS() +
				   " Layer = " + HP.getL() +
				   " Component="+ HP.getC());
            }
	    
            if (f1Noise2.hasEntry(HP.getS(), HP.getL(), HP.getC())){
		
                H_NOISE_Q.get(HP.getS(), HP.getL(), HP.getC()).
		    fit(f1Noise2.get(HP.getS(), HP.getL(), HP.getC()),"NR");
                
		System.out.println("Fitted Noise Index = " + index +
				   " Sector = " + HP.getS() + 
				   " Layer = " + HP.getL() +
				   " Component="+ HP.getC());
            }
            
	    if(f1MIP.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                
		H_MIP_Q.get(HP.getS(), HP.getL(), HP.getC()).
		    fit(f1MIP.get(HP.getS(), HP.getL(), HP.getC()),"NR");
                
		System.out.println("Fitted Cosminc Index = " + index +
				   " Sector = " + HP.getS() +
				   " Layer = " + HP.getL() +
				   " Component = " + HP.getC());
            }
        
	} // end of : for (int index = 0; index < 232; index++) {
        
	boolean flag_parnames = true;

	// Print out fit results
        for(int index = 0; index < 232; index++) {

            HP.setAllParameters(index,'h');
            
	    if(f1Noise1.hasEntry(HP.getS(), HP.getL(), HP.getC()) && 
	       f1Noise2.hasEntry(HP.getS(), HP.getL(), HP.getC())) {
		
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
                
		if(f1Noise1.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                    
		    for(int i=2; i<f1Noise1.get(HP.getS(), HP.getL(), HP.getC()).getNParams(); i++)
                        System.out.format("%.2f\t ",
					  f1Noise1.get(HP.getS(), HP.getL(), HP.getC()).getParameter(i));
                } 
		else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
                
		if(f1Noise2.hasEntry(HP.getS(), HP.getL(), HP.getC())){
		    
                    for(int i=0; i<f1Noise2.get(HP.getS(), HP.getL(), HP.getC()).getNParams(); i++)
                        System.out.format("%.2f\t ",
					  f1Noise2.get(HP.getS(), HP.getL(), HP.getC()).getParameter(i));
                }
                else {
                    System.out.print(0 + "\t" + 0 + "\t " +0+ "\t");
                }
		
                if(f1MIP.hasEntry(HP.getS(), HP.getL(), HP.getC())){
                    for(int i=0; i<f1MIP.get(HP.getS(), HP.getL(), HP.getC()).getNParams(); i++)
                        System.out.format("%.2f\t ",f1MIP.get(HP.getS(), HP.getL(), HP.getC()).getParameter(i));
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

	    f1Noise1.add(sec, lay, com, new F1D("exp+gaus", 10,  mean+25));
            f1Noise1.get(sec, lay, com).setParameter(0, ampl/5);
            f1Noise1.get(sec, lay, com).setParameter(1, -0.001);
            f1Noise1.get(sec, lay, com).setParameter(2, ampl);
            f1Noise1.get(sec, lay, com).setParameter(3, mean);
            f1Noise1.get(sec, lay, com).setParameter(4, std*3);
            f1Noise1.get(sec, lay, com).setParLimits(0, ampl/4.0, ampl);
            f1Noise1.get(sec, lay, com).setParLimits(1, -5, -0.0001);
            f1Noise1.get(sec, lay, com).setParLimits(2, ampl/2, ampl*2);
            f1Noise1.get(sec, lay, com).setParLimits(3, mean-25, mean+25);
            f1Noise1.get(sec, lay, com).setParLimits(4, 1, std*3.0);

            if (hnoisetofit.integral(23, 45)>50){
                f1Noise2.add(sec, lay, com, new F1D("gaus", mean+20, mean+100));
                f1Noise2.get(sec, lay, com).setParameter(0, ampl/5.0);
                f1Noise2.get(sec, lay, com).setParameter(1, mean+50);
                f1Noise2.get(sec, lay, com).setParameter(2, std);
                f1Noise2.get(sec, lay, com).setParLimits(0, 1, ampl/2.0);
                f1Noise2.get(sec, lay, com).setParLimits(1, mean+20, mean+100);
                f1Noise2.get(sec, lay, com).setParLimits(2, 1, std*3.0);
            }
        }

	int integralLowBin  = (500 - CosmicQXMin[lay])*NBinsCosmic;
	integralLowBin  = integralLowBin/(CosmicQXMax[lay]-CosmicQXMin[lay]);
	    
	int integralHighBin = NBinsCosmic-1;
	
	if (hchargetofit.integral(integralLowBin,integralHighBin ) > 25){

            ampl = 0;
            mean = 0;
        
	    for (int i = integralLowBin; i < integralHighBin; i++){
                if (hchargetofit.getBinContent(i) > ampl){
                   
		    ampl = hchargetofit.getBinContent(i);
                    
		    mean = i*(CosmicQXMax[lay]-CosmicQXMin[lay]);
		    mean = mean/NBinsCosmic + CosmicQXMin[lay];
		}
            }
	    
	    f1MIP.add(sec, lay, com, new F1D("landau", 500, 4500));
            f1MIP.get(sec, lay, com).setParameter(0, ampl);
            f1MIP.get(sec, lay, com).setParameter(1, mean);
            f1MIP.get(sec, lay, com).setParameter(2, 150);
            f1MIP.get(sec, lay, com).setParLimits(0, 0, ampl*2.0);
            f1MIP.get(sec, lay, com).setParLimits(1, mean-400, mean+400);
            f1MIP.get(sec, lay, com).setParLimits(2, 50, 1500);
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
                
		if(H_WAVE.hasEntry(secSel,laySel,501+ipaddle))
		    
		    this.canvasHODOEvent.draw(H_WAVE.get(secSel,
							 laySel,
							 501+ipaddle));
	    }
	    
	}
	else{
	    
	    this.canvasHODOEvent.divide(2, 1);
	    
	    canvasHODOEvent.cd(layCD);
	    if(H_WAVE.hasEntry(secSel,laySel,comSel))
		this.canvasHODOEvent.draw(H_WAVE.get(secSel,
						     laySel,
						     comSel));
	    
	    canvasHODOEvent.cd(oppCD);
	    if(H_WAVE.hasEntry(secSel,oppSel,comSel))
		this.canvasHODOEvent.draw(H_WAVE.get(secSel,
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
        
	if(H_WAVE.hasEntry(secSel,laySel,comSel)){
	    this.canvasEvent.draw(H_WAVE.get(secSel,
					     laySel,
					     comSel));
	    
	}
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	// raw fADC pulse
	canvasEvent.cd(oppCDL);
        
	if(H_WAVE.hasEntry(secSel,
			   oppSel,
			   comSel))
	    this.canvasEvent.draw(H_WAVE.get(secSel,
					     oppSel,
					     comSel));
	
	//----------------------------------------
	// middle top (bottom) for thin (thick) layer
	// calibrated fADC pulse
	canvasEvent.cd(layCDM);
        
	if(H_CWAVE.hasEntry(secSel,
			    laySel,
			    comSel))
	    this.canvasEvent.draw(H_CWAVE.get(secSel,
					      laySel,
					      comSel));
	
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
	// calibrated fADC pulse
	canvasEvent.cd(oppCDM);
	if(H_CWAVE.hasEntry(secSel,
			    oppSel,
			    comSel))
	    this.canvasEvent.draw(H_CWAVE.get(secSel,
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
        int layCDM = layCDL + 1;
	// map [3,0] to [4,1]
        int oppCDM = oppCDL + 1;
	
	// map [0,3] to [2,5]
	int layCDR = layCDL + 2;
	// map [3,0] to [5,2]
	int oppCDR = oppCDL + 2;

	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	// calibrated fADC pulse
	canvasNoise.cd(layCDL);
        
	if(H_CWAVE.hasEntry(secSel,laySel,comSel)){
	    this.canvasNoise.draw(H_CWAVE.get(secSel,
					      laySel,
					      comSel));
	    
	}
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	// calibrated fADC pulse
	canvasNoise.cd(oppCDL);
        
	if(H_CWAVE.hasEntry(secSel,oppSel,comSel))
	    this.canvasNoise.draw(H_CWAVE.get(secSel,
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
	    
	}
	
	//----------------------------------------
	// middle top (bottom) for thin (thick) layer
	// calibrated fADC pulse
	
	canvasNoise.cd(oppCDM);
	
	if(H_MAXV.hasEntry(secSel,oppSel,comSel)){
	    this.canvasNoise.draw(H_MAXV.get(secSel,
					     oppSel,
					     comSel));
	    
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
	if(f1Noise1.hasEntry(secSel,
			     laySel,
			     comSel))
	    this.canvasNoise.draw(f1Noise1.get(secSel,
					       laySel,
					       comSel),"same");
	if(f1Noise2.hasEntry(secSel,
			     laySel,
			     comSel))
	    this.canvasNoise.draw(f1Noise2.get(secSel,
					       laySel,
					       comSel),"same");
	
        
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
	if(f1Noise1.hasEntry(secSel,
			     oppSel,
			     comSel))
	    this.canvasNoise.draw(f1Noise1.get(secSel,
					       oppSel,
					       comSel),"same");
	if(f1Noise2.hasEntry(secSel,
			     oppSel,
			     comSel))
	    this.canvasNoise.draw(f1Noise2.get(secSel,
					       oppSel,
					       comSel),"same");
	
    }
    
    void drawCanvasCharge(int secSel,
			  int laySel,
			  int comSel,
			  int oppSel,
			  int layCDL,
			  int layCDR,
			  int oppCDL,
			  int oppCDR){
	
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	canvasCharge.cd(layCDL);
        
	if(H_NOISE_Q.hasEntry(secSel,laySel,comSel)){
	    this.canvasCharge.draw(H_NOISE_Q.get(secSel,
						      laySel,
						      comSel));
	    if(f1Noise1.hasEntry(secSel,
				      laySel,
				      comSel))
		this.canvasCharge.draw(f1Noise1.get(secSel,
							 laySel,
							 comSel),"same");
	    if(f1Noise2.hasEntry(secSel,
				      laySel,
				      comSel))
		this.canvasCharge.draw(f1Noise2.get(secSel,
							 laySel,
							 comSel),"same");
	    
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
	if(f1Noise1.hasEntry(secSel,
				  oppSel,
				  comSel))
	    this.canvasCharge.draw(f1Noise1.get(secSel,
						     oppSel,
						     comSel),"same");
	if(f1Noise2.hasEntry(secSel,
				  oppSel,
				  comSel))
	    this.canvasCharge.draw(f1Noise2.get(secSel,
						     oppSel,
						     comSel),"same");
	
	//----------------------------------------
	// right top (bottom) for thin (thick) layer
	canvasCharge.cd(layCDR);
	
	if(H_MIP_Q.hasEntry(secSel,
				    laySel,
				    comSel)){
	    this.canvasCharge.draw(H_MIP_Q.get(secSel,
						       laySel,
						       comSel));
	    if(f1MIP.hasEntry(secSel,
				      laySel,
				      comSel))
		this.canvasCharge.draw(f1MIP.get(secSel,
							 laySel,
							 comSel),"same");
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
	    if(f1MIP.hasEntry(secSel,
				      oppSel,
				      comSel))
		this.canvasCharge.draw(f1MIP.get(secSel,
							 oppSel,
							 comSel),"same");
	}
    }
    
    void drawCanvasMatch(int secSel,
			 int laySel,
			 int comSel,
			 int oppSel,
			 int layCDL,
			 int layCDR,
			 int oppCDL,
			 int oppCDR){
	
	//----------------------------------------
	// left top (bottom) for thin (thick) layer
	canvasMatch.cd(layCDL);
        
	if(H_NOISE_Q.hasEntry(secSel,laySel,comSel)){
	    this.canvasMatch.draw(H_NOISE_Q.get(secSel,
						     laySel,
						     comSel));
	    if(f1Noise1.hasEntry(secSel,
				      laySel,
				      comSel))
		this.canvasMatch.draw(f1Noise1.get(secSel,
							laySel,
							comSel),"same");
	    if(f1Noise2.hasEntry(secSel,
				      laySel,
				      comSel))
		this.canvasMatch.draw(f1Noise2.get(secSel,
							laySel,
							comSel),"same");
	    
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
	if(f1Noise1.hasEntry(secSel,
				  oppSel,
				  comSel))
	    this.canvasMatch.draw(f1Noise1.get(secSel,
						    oppSel,
						    comSel),"same");
	if(f1Noise2.hasEntry(secSel,
				  oppSel,
				  comSel))
	    this.canvasMatch.draw(f1Noise2.get(secSel,
						    oppSel,
						    comSel),"same");
	
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
	
	GraphErrors[] G_NPE = null;
	
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
	
	G_NPE    = new GraphErrors[2];
	String title;
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
	
	GraphErrors[] G_Gain = null;
	
	
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
	
	for( int lM = 0 ; lM < 2 ; lM++){
	}
	
	G_Gain    = new GraphErrors[2];
	String title; 
	
	for( int lM = 0 ; lM < 2 ; lM++){
	    
	    for (int c = 0 ; c < evenI.length ; c++){
		evenGain[lM][c]    = gain[secSel][lM+1][evenI[c]];
		evenGainErr[lM][c] = errGain[secSel][lM+1][evenI[c]];
	    }
	    for (int c = 0 ; c < oddI.length ; c++){
		oddGain[lM][c]    = gain[secSel][lM+1][oddI[c]];
		oddGainErr[lM][c] = errGain[secSel][lM+1][oddI[c]];
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
	    
	    
	    title = "sector " + secSel;
	    G_Gain[lM].setTitle(title); 
	    G_Gain[lM].setXTitle("component");
	    G_Gain[lM].setYTitle("gain (pC)");
	    G_Gain[lM].setMarkerSize(5); 
	    G_Gain[lM].setMarkerColor(lM+1); // 0-9 for given palette
	    G_Gain[lM].setMarkerStyle(lM+1); // 1 or 2
	    
	}
	
	int nXBins[] = {20,9};
	int nYBins = 100;
	double[] xLimits = {0.5,(double)nXBins[secSel%2]+0.5};
	double[] yLimits = {0.0,100.};
	
	H1D H1 = new H1D("H1","component","gain (pC)",nXBins[secSel%2],xLimits[0],xLimits[1]);
	canvasGain.draw(H1);
	canvasGain.draw(G_Gain[0],"same");
	canvasGain.draw(G_Gain[1],"same");
	H1.getYaxis().set(nYBins,yLimits[0],yLimits[1]);
	canvasGain.draw(H1,"same");
    }
	
    

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
			
	// only process paddles if cal selected
	if(calSel && comSel < 500){
	    return;
	}
	
	if(!hodSelect && !calSel){
	    return;
	}
		
	//!! Construct these methods
	// getChan4SLC(secSel,laySel,comSel);
	// getMezz4SLC(secSel,laySel,comSel);
	
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
	    
	    drawCanvasEvent(secSel,
			    laySel,
			    comSel);
	    
		
	}
	else if ( tabSelect == this.tabIndexNoise ) {
	    
	    drawCanvasNoise(secSel,
			    laySel,
			    comSel);
			    
	}
	else if ( tabSelect == this.tabIndexGain ) {
	    
	    drawCanvasGain(secSel,
			   laySel,
			   comSel);	    
	}
	else if ( tabSelect == this.tabIndexCharge ) {
	    
	    drawCanvasCharge(secSel,
			     laySel,
			     comSel,
			     oppSel,
			     layCDL,
			     layCDR,
			     oppCDL,
			     oppCDR);
	}
	else if ( tabSelect == this.tabIndexMIP ) {
	    
	    drawCanvasMIP(secSel,
			  laySel,
			  comSel);	    
	}
	else if ( tabSelect == this.tabIndexMatch ) {
	    
	    drawCanvasMatch(secSel,
			    laySel,
			    comSel,
			    oppSel,
			    layCDL,
			    layCDR,
			    oppCDL,
			    oppCDR);
	}
	
	
    } // end of: public void detectorSelected(DetectorD....
    
    
    public Color getComponentStatus(int sec, int lay, int com) {
                
        int index = com;
        
        if (lay > 0) // cal layer is always 0
	    index = getIndex4SLC(sec,lay,com);
	
	Color col = new Color(100,100,100);
        
        if(H_W_MAX.getBinContent(index) > cosmicsThrsh) {
            col = palette.getColor3D(H_W_MAX.getBinContent(index), 4000, true);
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
        else
            index = getIndex4SLC(sec,lay,com);
        
        double waveMax     = H_W_MAX.getBinContent(index);
	double npeWaveMax  = H_NPE_MAX.getBinContent(index);
	
	// map [0,4095] to [0,255]
	int    signalAlpha = (int)(waveMax)/16;
	
	// map [0,20] to [0,255]
	int    noiseAlpha  = min((int)(npeWaveMax/20*255),255);
	
	
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
	else if( tabSelect == tabIndexNoise ||
		 tabSelect == tabIndexGain ) {
            if      ( waveMax > noiseThrsh) {
                shape.setColor(255, 255, 0, (256/4)-1);
            }
            if      ( waveMax > 2*noiseThrsh) {
                shape.setColor(255, 255, 0, (256/2)-1);
            }
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
    
    private double getGain(int s, int l, int c){
	
	double gain = 0.0;
	
	if(f1Noise1.hasEntry(s, l, c) &&
	   f1Noise2.hasEntry(s, l, c)){
	    
	    double n2 = f1Noise2.get(s,l,c).getParameter(1);
	    double n1 = f1Noise1.get(s,l,c).getParameter(3);
	    
	    gain = n2 - n1;
	    
	}
	
	if (gain < 30.0 || 
	    gain > 90.0)
	    gain = 0.0;
	
	return gain;
    }
    
    private double getGainError(int s, int l, int c){
	
	double gainError = 0.0;
	
	if(f1Noise1.hasEntry(s, l, c) &&
	   f1Noise2.hasEntry(s, l, c)){
	    
	    double n2Error = f1Noise2.get(s,l,c).getParError(1);
	    double n1Error = f1Noise1.get(s,l,c).getParError(3);
	    gainError    = n2Error*n2Error + n1Error*n1Error;
	    gainError    = sqrt(gainError);
	    
	}
	
	return gainError;
	
    }
    
    private double getQMean(int s, int l, int c){
	
	double qMean = 0.0;
	
	if(f1MIP.hasEntry(s, l, c))
	    qMean = f1MIP.get(s,l,c).getParameter(1);
	
	return qMean;
	
    }

    private double getQMeanError(int s, int l, int c){
	
	double qMeanError = 0.0;
	
	if(f1MIP.hasEntry(s, l, c))
	    qMeanError = f1MIP.get(s,l,c).getParError(1);
	
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
    
    // private double getNpeStd(int s, int l, int c){
	
    // 	double npeMean = 0.0;
    // 	if( get > 0.0 )
    // 	    npeMean = getQMean(s,l,c)/getGain(s,l,c);
	
    // 	return npeMean;
	
    // }


    private void updateVariables() {
	
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
            this.updateVariables();
        
	this.view.repaint();
    }
    
    public void initHistograms() {
        
	this.initArrays();
	
	// hodoscope
	for (int index = 0; index < 232; index++) 
	    setHistograms(index,'h');

	// calorimeter
        for (int index = 0; index < 505; index++)
	    setHistograms(index,'c');

        H_W_MAX     = new H1D("H_W_MAX", 504, 0, 504);
	H_V_MAX    = new H1D("H_V_MAX", 504, 0, 2000);
	H_NPE_MAX  = new H1D("H_NPE_MAX", 500, 0, 50);

    }

    private void setHistograms(int index, char detector){
	
	HistPara HP = new HistPara();

	HP.setAllParameters(index,detector);
		
	//----------------------------
	// Event-by-Event Histograms
	
	H_WAVE.add(HP.getS(),HP.getL(),HP.getC(),
		   new H1D(DetectorDescriptor.
			   getName("WAVE",HP.getS(),HP.getL(),HP.getC()),
			   HP.getTitle(),100, 0.0, 100.0));
	H_WAVE.get(HP.getS(),HP.getL(),HP.getC()).setFillColor(4);
	H_WAVE.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("fADC Sample");
	H_WAVE.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("fADC Counts");
	
	H_CWAVE.add(HP.getS(),HP.getL(),HP.getC(),
		    new H1D(DetectorDescriptor.
			    getName("Calibrated",HP.getS(),HP.getL(),HP.getC()),
			    HP.getTitle(), 100, 0.0, 400.0));
	H_CWAVE.get(HP.getS(),HP.getL(),HP.getC()).setFillColor(3);
	H_CWAVE.get(HP.getS(),HP.getL(),HP.getC()).setXTitle("Time (ns)");
	H_CWAVE.get(HP.getS(),HP.getL(),HP.getC()).setYTitle("Voltage (mV)");
	
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
	
	H_NOISE.add(HP.getS(),HP.getL(), HP.getC(),
		    new H1D(DetectorDescriptor.
			    getName("Noise", HP.getS(),HP.getL(),HP.getC()),
			    HP.getTitle(), 200, 0.0, 10.0));
	H_NOISE.get(HP.getS(),HP.getL(), HP.getC()).setFillColor(4);
	H_NOISE.get(HP.getS(),HP.getL(), HP.getC()).setXTitle("RMS (mV)");
	H_NOISE.get(HP.getS(),HP.getL(), HP.getC()).setYTitle("Counts");
	
	H_MAXV.add(HP.getS(),HP.getL(), HP.getC(),
		   new H1D(DetectorDescriptor.
			   getName("WAVEMAX",
				   HP.getS(),HP.getL(),HP.getC()),
			   HP.getTitle(), 150, 0.0, 150));
	H_MAXV.get(HP.getS(),HP.getL(), HP.getC()).setFillColor(4);
	H_MAXV.get(HP.getS(),HP.getL(), HP.getC()).setXTitle("Waveform Max (mV)");
	H_MAXV.get(HP.getS(),HP.getL(), HP.getC()).setYTitle("Counts");
	
	H_FADCSAMPLE.add(HP.getS(),HP.getL(), HP.getC(), 
			 new H1D(DetectorDescriptor.
				 getName("FADCSAMPLE", 
					 HP.getS(),HP.getL(),HP.getC()), 
				 HP.getTitle(), 100, 0.0, 400));
	
	H_FADCSAMPLE.get(HP.getS(),HP.getL(), HP.getC()).setFillColor(4);
	H_FADCSAMPLE.get(HP.getS(),HP.getL(), HP.getC()).setXTitle("time (ns) ");
	H_FADCSAMPLE.get(HP.getS(),HP.getL(), HP.getC()).setYTitle("Counts");
        
	if (HP.getL()==1){
	    H_FADCSAMPLEdiff.add(HP.getS(),HP.getL(), HP.getC(), 
				 new H1D(DetectorDescriptor.
					 getName("FADCSAMPLEdiff", 
						 HP.getS(),HP.getL(),HP.getC()), 
					 HP.getTitle(), 14, -28, 28));
	    H_FADCSAMPLEdiff.get(HP.getS(),HP.getL(), HP.getC()).setFillColor(4);
	    H_FADCSAMPLEdiff.get(HP.getS(),HP.getL(), HP.getC()).setXTitle("#Delta time (ns) ");
	    H_FADCSAMPLEdiff.get(HP.getS(),HP.getL(), HP.getC()).setYTitle("Counts");
	}
	
        
	H_COSMIC_fADC.add(HP.getS(),HP.getL(), HP.getC(), 
			  new H1D(DetectorDescriptor.
				  getName("Cosmic fADC", 
					  HP.getS(),HP.getL(),HP.getC()), 
				  HP.getTitle(), 100, 0.0, 100.0));
	H_COSMIC_fADC.get(HP.getS(),HP.getL(), HP.getC()).setFillColor(3);
	H_COSMIC_fADC.get(HP.getS(),HP.getL(), HP.getC()).setXTitle("fADC Sample");
	H_COSMIC_fADC.get(HP.getS(),HP.getL(), HP.getC()).setYTitle("fADC Counts");
	
	mylandau.add(HP.getS(),HP.getL(), HP.getC(), new F1D("landau", 0.0, 80.0));
	mygauss.add(HP.getS(),HP.getL(), HP.getC(), new F1D("gaus", -20, 20.0));
        
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
	
	H_NOISE.get(HP.getS(),
		    HP.getL(),
		    HP.getC()).reset();
	
        
	H_COSMIC_fADC.get(HP.getS(),
			  HP.getL(),
			  HP.getC()).reset();
	
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
        
        int[][][] timediff = new int[8][2][20];
        
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
	    H_WAVE.get(sec, lay, com).reset();
	    H_CWAVE.get(sec, lay, com).reset();
	    H_NPE.get(sec, lay, com).reset();

	    
	    //===============================
	    // npe for this event only
	    // to be use in loop two below
	    npeEvent[sec][lay][com] = 0.0;
	    
	    if(gain[sec][lay][com] > 0.0){
		
		npeEvent[sec][lay][com] = counter.getChannels().
		    get(0).getADC().get(0)*LSB*4.0/50./gain[sec][lay][com];
		
		H_NPE_INT.get(sec, lay, com).fill(npeEvent[sec][lay][com]);
		
	    }
	    
	    // Loop through fADC bins filling event-by-event histograms
            for (int i = 0;
                 i < min(pulse.length,
			 H_fADC.get(sec,lay,com).getAxis().getNBins());
                 i++) {
		
                
		H_WAVE.get(sec,lay,com).fill(i, pulse[i]);
                
                baselineSubRaw = pulse[i] - fadcFitter.getPedestal() + 10.0;
		H_fADC.get(sec,lay,com).fill(i,baselineSubRaw);
                
                calibratedWave = (pulse[i]-fadcFitter.getPedestal())*LSB + 5.0;
		H_CWAVE.get(sec,lay,com).fill(i*4,calibratedWave);
		
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
                sec  = 0;
                lay  = 0;
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
		double errNPEOther    = errNPE[sec][opp][com];
		double npeLowLimOther = meanNPEOther - abs(errNPEOther);
		
		if ( npeLowLimOther < 5.0 )
		    npeLowLimOther = 5.0;
		
		if ( npeOtherLayer > npeLowLimOther )
		    H_NPE_MATCH.get(sec, lay, com)
			.fill(npeEvent[sec][lay][com]);
		
		
	    }
	    	    
            if (fadcFitter.getWave_Max()-fadcFitter.getPedestal() > 10)
                H_MAXV.get(sec, lay, com).
		    fill((fadcFitter.getWave_Max()-fadcFitter.getPedestal())*LSB);
            
            if (lay > 0 && fadcFitter.getADCtime() > 0){
                H_FADCSAMPLE.get(sec, lay, com).fill(fadcFitter.getADCtime()*4.0);
                timediff[sec-1][lay-1][com-1]=fadcFitter.getADCtime()*4;
            }
            
            if(fadcFitter.getWave_Max()-fadcFitter.getPedestal()>cosmicsThrsh)
                H_NOISE.get(sec, lay, com).fill(fadcFitter.getRMS());
            
	    double waveMax = fadcFitter.getWave_Max()-fadcFitter.getPedestal();
	    double voltMax = waveMax/voltsPerSPE;
	    double npeMax  = waveMax/binsPerSPE;

	    
	    H_W_MAX.fill(index,waveMax);
	    H_V_MAX.fill(index,voltMax);
	    H_NPE_MAX.fill(index,npeMax);
	    	    
	} // end of: for (DetectorCounter counter : counters) {
    
	
        

        
        for (int isect = 0; isect < 8; isect++) {
            for (int icom = 0; icom < 20; icom++) {
                if ((isect+1)%2==1 && icom>8)
                    continue;
                if (timediff[isect][1][icom] > 0 &&
                    timediff[isect][0][icom] > 0)
                    H_FADCSAMPLEdiff.get(isect+1, 1, icom+1).
			fill(timediff[isect][1][icom]-timediff[isect][0][icom]);
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
            layCD = 0;
            oppCD    = 1;
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
	else if( tabSelect == tabIndexCharge  &&
		 (nProcessed%(10*repaintFrequency)==0) ) {
	    
	    drawCanvasCharge(secSel,
			     laySel,
			     comSel,
			     oppSel,
			     layCDL,
			     layCDR,
			     oppCDL,
			     oppCDR);
	    
	}
	//======================================================================
        
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