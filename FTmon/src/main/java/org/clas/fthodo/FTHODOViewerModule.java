package org.clas.fthodo;


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


public class FTHODOViewerModule implements IDetectorListener,ActionListener{

    JPanel detectorPanel;
    EventDecoder decoder;
    
    ColorPalette palette = new ColorPalette();
    //=================================
    //           HISTOGRAMS
    //=================================
    
    DetectorCollection<H1D> H_WAVE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_CWAVE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NPE = new DetectorCollection<H1D>();
    
    DetectorCollection<H1D> H_MAXV = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE_CHARGE = new DetectorCollection<H1D>();
    
    
    DetectorCollection<H1D> H_FADCSAMPLE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_FADCSAMPLEdiff = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_fADC = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_fADC   = new DetectorCollection<H1D>();
    
    DetectorCollection<F1D> mylandau = new DetectorCollection<F1D>();
    DetectorCollection<F1D> mygauss = new DetectorCollection<F1D>();

    DetectorCollection<Integer> dcHits = new DetectorCollection<Integer>();
    
    H1D H_fADC_N   = null;
    H1D H_WMAX     = null;
    H1D H_COSMIC_N = null;

    EmbeddedCanvas canvas = new EmbeddedCanvas();
    DetectorShapeTabView view = new DetectorShapeTabView();    
    
    //=================================
    //           CONSTANTS
    //=================================
    
    int    fADCBins      = 4096;
    double voltageMax    = 2000; // for run < 230 max = 1000 mV
    double LSB           = voltageMax/fADCBins; // cast?
    double npeThrsh      = 10.;
    double voltsPerSPE   = 40.; // approximate for now
    double voltageThrsh  = npeThrsh*voltsPerSPE;
    double fADCThreshold = voltageThrsh/LSB;
    
    int ped_i1 = 4;
    int ped_i2 = 24;
    int pul_i1 = 30;
    int pul_i2 = 70;

    int ped_j1 = 79;
    int ped_j2 = 99;
    
    //=================================
    //           VARIABLES
    //=================================

    double[] crystalID;
    double[] noiseRMS;
    double[] cosmicCharge;
    
    double   crystal_size = 15;
    int      nProcessed = 0;
    
    private int plotSelect      = 0;  
    private int componentSelect = 0;
    private int secSelect       = 0;
    private int layerSelect     = 0;

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
    
    
    public FTHODOViewerModule(){
        this.detectorPanel=null;
    }
  
    
    
    public void initDetector(){
        DetectorShapeView2D viewFTHODO = this.drawDetector(0.0, 0.0);
        this.view.addDetectorLayer(viewFTHODO);
        view.addDetectorListener(this);
    }
    
    public DetectorShapeView2D drawDetector(double x0, double y0) {    
        DetectorShapeView2D viewFTHODO = new DetectorShapeView2D("FTHODO");
        
	// sectors 1-8 for each layer. 
	// detector symmetry is fourfold
	// with elements 0-28 for each quarter.
	int sector;  
	
	// Tile component 
	// 1-9 for odd sectors 
	// 1-20 for even
        int component; 
	
	// y-offset to place thin and thick layer on same pane
	double[] layerOffsetY = {-180.0,180.0}; 
	// size of elements of symmetry sector 0-28
	double[] tileSize = {15.0,30.0,15.0,30.0,30.0,30.0,30.0,30.0,15.0,
			   30.0,30.0,30.0,30.0,30.0,30.0,30.0,30.0,30.0,30.0,
			   30.0,30.0,15.0,15.0,15.0,15.0,15.0,15.0,15.0,15.0}; 
	
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
	
	// two layers: c==0 for thin and c==1 for thick
        for (int layer_c=0; layer_c<2; layer_c++){ 
	    // 4 symmetry sectors per layer (named quadrant) from 0-3
            for (int quadrant=0; quadrant<4; quadrant++) {  
		// 29 elements per symmetry sector
                for (int element = 0; element < 29; element++) { 
                    // sector is odd for first 9 elements 
		    // and even for the rest
		    if (element<9) {    
			sector = quadrant*2 +1;
			// component number for odd sector is 1-9
			component = element + 1; 
                    }
                    else  {
			sector = quadrant*2 +2;
			// component number for even sector is 1-20
                        component = element + 1 - 9; 
                    }
		    
		    // // calculate the x-element of the center of each crystal;
// 		    xcenter = p_R[element] ;
// 		    xcenter = xcenter * Math.sin(p_theta[element]+Math.PI /2 *quadrant);
// 		    xcenter = xcenter * 10.;  
		    
		    // //============================================================
		    if(quadrant==0)      xcenter = xx[element];
		    else if(quadrant==1) xcenter =-yy[element];
		    else if(quadrant==2) xcenter =-xx[element];
		    else if(quadrant==3) xcenter = yy[element];
		    //============================================================
                    
		    
		    // // calculate the y-element of the center of each crystal
// 		    ycenter = -p_R[element] ;
// 		    ycenter = ycenter * Math.cos(p_theta[element]+Math.PI /2 *quadrant);
// 		    if(layer_c==0 && quadrant==1) System.out.println(xcenter + " " + ycenter*10);
// 		    ycenter = ycenter * 10 + layerOffsetY[layer_c];
		    
		    
		    // //============================================================
		    if(quadrant==0)       ycenter = yy[element] + layerOffsetY[layer_c];
                     else if(quadrant==1) ycenter = xx[element] + layerOffsetY[layer_c];
                     else if(quadrant==2) ycenter =-yy[element] + layerOffsetY[layer_c];
                     else if(quadrant==3) ycenter =-xx[element] + layerOffsetY[layer_c];
 		    //============================================================
		    
		    // Sectors 1-8 
		    // (sect=1: upper left - clockwise); 
		    // layers 1-2 (thin==1, thick==2); 
		    // crystals (1-9 for odd and 1-20 for even sectors)
                    DetectorShape2D shape = new DetectorShape2D(DetectorType.FTHODO,
								sector, 
								layer_c+1,component);
		    // defines the 2D bars dimensions using the element size
                    shape.createBarXY(tileSize[element], tileSize[element]);  
		    
		    // defines the placements of the 2D bar according to the 
		    // xcenter and ycenter calculated above
                    shape.getShapePath().translateXYZ(xcenter, ycenter, 0.0); 
                    //shape.setColor(0, 145, 0);   
		    shape.setColor(0, 0, 125);   
                    viewFTHODO.addShape(shape);  
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
	
        if (e.getActionCommand().compareTo("Waveforms") == 0) {
            plotSelect = 0;
//            resetCanvas();
        }
        else if (e.getActionCommand().compareTo("Calibrated") == 0) {
            plotSelect = 1;
//            resetCanvas();
        }
        else if (e.getActionCommand().compareTo("NPE Wave") == 0) {
            plotSelect = 2;
//            resetCanvas();
        }
        else if (e.getActionCommand().compareTo("Max") == 0) {
            plotSelect = 10;
	}
        else if (e.getActionCommand().compareTo("Charge") == 0) {
            plotSelect = 11;
	}
	
	// IN PROGRESS
    //     if (e.getActionCommand().compareTo("Fit Timing") == 0) {
	//             plotSelect = 12;
	//             fitTimingdiff();
	//         }
          
    }

    private void resetCanvas() {
        this.canvas.divide(1, 2);
        canvas.cd(0);
    }

    
        private void fitHistograms() {
        for(int component=0; component< 22*22; component++) {
            if(H_COSMIC_CHARGE.hasEntry(0, 0, component)) {
                if(H_COSMIC_CHARGE.get(0, 0, component).getEntries()>200) {
                    H1D hcosmic = H_COSMIC_CHARGE.get(0,0,component);
                    initLandauFitPar(component,hcosmic);
                    hcosmic.fit(mylandau.get(0, 0, component));
                }
            }
        }
        boolean flag_parnames=true;
        for(int component=0; component< 22*22; component++) {
            if(mylandau.hasEntry(0, 0, component)) {
                if(flag_parnames) {
                    //System.out.println("Component\t amp\t mean\t sigma\t p0\t p1\t Chi2");
                    flag_parnames=false;
                }
                //System.out.print(component + "\t\t ");
                //for(int i=0; i<mylandau.get(0, 0, component).getNParams(); i++) 
		    //System.out.format("%.2f\t ",mylandau.get(0, 0, component).getParameter(i));
		    //if(mylandau.get(0, 0, component).getNParams()==3) System.out.print("0.0\t 0.0\t");
                //if(mylandau.get(0, 0, component).getParameter(0)>0)
                    //System.out.format("%.2f\n",mylandau.get(0, 0, component).getChiSquare(H_COSMIC_CHARGE.get(0,0,component).getDataSet())
		    ///mylandau.get(0, 0, component).getNDF(H_COSMIC_CHARGE.get(0,0,component).getDataSet()));
		    //elseSystem.out.format("0.0\n"); 
	    }
	}
    }
    
  private void initLandauFitPar(int component, H1D hcosmic) {
        if(hcosmic.getBinContent(0)==0) mylandau.add(0, 0, component, new F1D("landau",     0.0, 80.0));
        else                            mylandau.add(0, 0, component, new F1D("landau+exp", 0.0, 80.0));
        if(hcosmic.getBinContent(0)<10) {
            mylandau.get(0, 0, component).setParameter(0, hcosmic.getBinContent(hcosmic.getMaximumBin()));
        }
        else {
            mylandau.get(0, 0, component).setParameter(0, 10);
        }
        mylandau.get(0, 0, component).setParameter(1,hcosmic.getMean());
        mylandau.get(0, 0, component).setParameter(2,5);
        if(hcosmic.getBinContent(0)!=0) {
            mylandau.get(0, 0, component).setParameter(3,hcosmic.getBinContent(0));
            mylandau.get(0, 0, component).setParameter(4, -0.2);
        }
    }
    

    private void fitTimingdiff() {
        for (int comp = 0; comp < 116; comp++) {
            int layer_c = 1;
            int iQuad_c = (comp-(layer_c-1)*116) / 29;
            int iElem_c = comp - iQuad_c * 29 -(layer_c-1)*116;
            int sec_c;
            int crys_c;
            if (iElem_c<9) {
                sec_c = iQuad_c*2 +1;
                crys_c = iElem_c + 1;
            }
            else {
                sec_c = iQuad_c*2 +2;
                crys_c = iElem_c + 1 -9;
            }
            if(H_FADCSAMPLEdiff.hasEntry(sec_c, 1, crys_c)&& H_FADCSAMPLEdiff.get(sec_c, 1, crys_c).getEntries()>10) {
                H1D htimediff = H_FADCSAMPLEdiff.get(sec_c, 1, crys_c);
                initGausFitPar(sec_c, 1, crys_c, htimediff);
                H_FADCSAMPLEdiff.get(sec_c, 1, crys_c).fit(mygauss.get(sec_c, 1, crys_c));
            }
        }
    }
    private void initGausFitPar(int sec_c, int lay_c, int crys_c, H1D htimediff) {
        mygauss.add(sec_c, lay_c , crys_c, new F1D("gaus", -20.0, 20.0));
        double ampldiff;
        double sddiff;
        double meandif;
        
        ampldiff=htimediff.getEntries()/1.5;
        meandif=htimediff.getMean();
        sddiff=htimediff.getRMS();
        mygauss.get(sec_c, lay_c , crys_c).setParameter(0, ampldiff);
        mygauss.get(sec_c, lay_c , crys_c).setParameter(1, meandif);
        mygauss.get(sec_c, lay_c , crys_c).setParameter(2, sddiff);
    }

    public void detectorSelected(DetectorDescriptor desc) {
	//System.out.println("SELECTED = " + desc);
        componentSelect = desc.getComponent();
        secSelect = desc.getSector();
        layerSelect = desc.getLayer();
	//System.out.println("Sector=" + secSelect + " Layer=" +layerSelect + " Component=" + componentSelect);
	
        if      ( plotSelect == 0 ) {
//            this.canvas.divide(1, 2);
	    canvas.cd(layerSelect-1);
            
	    if(H_WAVE.hasEntry(secSelect,layerSelect,componentSelect))
                this.canvas.draw(H_WAVE.get(secSelect,layerSelect,componentSelect));
            
	    canvas.cd(layerSelect%2);
            
	    if(H_WAVE.hasEntry(secSelect,
			       (layerSelect%2)+1,
			       componentSelect))
                this.canvas.draw(H_WAVE.get(secSelect,
					    (layerSelect%2)+1, 
					    componentSelect));
        }
	else if ( plotSelect == 1 ){
//	    this.canvas.divide(1, 2);
            canvas.cd(layerSelect-1);
            if(H_CWAVE.hasEntry(secSelect,
				layerSelect,
				componentSelect))
                this.canvas.draw(H_CWAVE.get(secSelect,
					     layerSelect,
					     componentSelect));
            canvas.cd(layerSelect%2);
            if(H_CWAVE.hasEntry(secSelect,
				(layerSelect%2)+1,
				componentSelect))
                this.canvas.draw(H_CWAVE.get(secSelect,
					     (layerSelect%2)+1,
					     componentSelect));
	}
	else if ( plotSelect == 2 ){
//	    this.canvas.divide(1, 2);
            canvas.cd(layerSelect-1);
            if(H_NPE.hasEntry(secSelect,
			      layerSelect,
			      componentSelect))
                this.canvas.draw(H_NPE.get(secSelect,
					   layerSelect,
					   componentSelect),"S");
            canvas.cd(layerSelect%2);
            if(H_NPE.hasEntry(secSelect,
			      (layerSelect%2)+1,
			      componentSelect))
                this.canvas.draw(H_NPE.get(secSelect,
					   (layerSelect%2)+1,
					   componentSelect),"S");
	}
	else if ( plotSelect == 10 ) {
//            this.canvas.divide(1, 2);
            canvas.cd(layerSelect-1);
            if(H_MAXV.hasEntry(secSelect,
			       layerSelect,
			       componentSelect))
                this.canvas.draw(H_MAXV.get(secSelect,
					    layerSelect,
					    componentSelect));
            canvas.cd(layerSelect%2);
            if(H_MAXV.hasEntry(secSelect,
			       (layerSelect%2)+1, 
			       componentSelect))
                this.canvas.draw(H_MAXV.get(secSelect,
					    (layerSelect%2)+1, 
					    componentSelect));
        }
	else if ( plotSelect == 11 ) {
//            this.canvas.divide(1, 2);
            
	    canvas.cd(layerSelect-1);
            if(H_COSMIC_CHARGE.hasEntry(secSelect,
					layerSelect,
					componentSelect)){
                
		this.canvas.setLogY(true);
		
		this.canvas.draw(H_COSMIC_CHARGE.get(secSelect,
						     layerSelect,
						     componentSelect),"S");
		
	    }
            // noise of same layer component in other division
	    canvas.cd(layerSelect%2);
            if(H_NOISE_CHARGE.hasEntry(secSelect,
				       (layerSelect), 
				       componentSelect)){
                
		this.canvas.setLogY(true);
		this.canvas.draw(H_NOISE_CHARGE.get(secSelect,
						    (layerSelect), 
						    componentSelect),"S");
		
		
	    }
	}
	
	
    // IN PROGRESS
    //     else if (plotSelect == 2 ) {
//             this.canvas.divide(1, 3);
//             canvas.cd(layerSelect-1);
//             if(H_FADCSAMPLE.hasEntry(secSelect,layerSelect,componentSelect))
//                 this.canvas.draw(H_FADCSAMPLE.get(secSelect,layerSelect,componentSelect));
//             canvas.cd(layerSelect%2);
//             if(H_FADCSAMPLE.hasEntry(secSelect,(layerSelect%2)+1, componentSelect))
//                 this.canvas.draw(H_FADCSAMPLE.get(secSelect,(layerSelect%2)+1, componentSelect));
//             canvas.cd(2);
//             if(H_FADCSAMPLEdiff.hasEntry(secSelect,1, componentSelect))
//                 this.canvas.draw(H_FADCSAMPLEdiff.get(secSelect,1, componentSelect));
//         }
//         else if (plotSelect == 3 ) {
//             this.canvas.divide(1, 1);
//             canvas.cd(0);
//             if(H_FADCSAMPLEdiff.hasEntry(secSelect,1, componentSelect)){
//                 this.canvas.draw(H_FADCSAMPLEdiff.get(secSelect,1, componentSelect));
//                 this.canvas.draw(mygauss.get(secSelect,1, componentSelect), "same");
//             }
//         }
//         else if (plotSelect == 4 ) {

    }

    public Color getComponentStatus(int sector, int layer, int component) {
        int sector_count[] = {0,9,29,38,58,67,87,96};
        int index = (layer -1 ) *116+sector_count[sector-1]+component;
        Color col = new Color(100,100,100);
        if(H_WMAX.getBinContent(index)>fADCThreshold) {
            col = palette.getColor3D(H_WMAX.getBinContent(index), 4000, true);           
//            col = new Color(200, 0, 200);
        }
        return col;
    }

    public void update(DetectorShape2D shape) {
        int sector    = shape.getDescriptor().getSector();
        int layer     = shape.getDescriptor().getLayer();
        int component = shape.getDescriptor().getComponent();
        int sector_count[] = {0,9,29,38,58,67,87,96};
        int index = (layer -1 ) *116+sector_count[sector-1]+component;
	
	//shape.setColor(200, 200, 200);
	//     System.out.println("Bin Content n" +index + "="+ H_WMAX.getBinContent(index));
        
	if(plotSelect==0 || plotSelect==1 ||plotSelect==2) {
            if(H_WMAX.getBinContent(index)>fADCThreshold) {
                shape.setColor(200, 0, 200);
            }
            else {
                shape.setColor(255, 255, 255);
            }
        }
        if(plotSelect==4) {
            if(H_WMAX.getBinContent(index)*LSB > fADCThreshold) {
                shape.setColor(200, 0, 200);
            }
	    //             else if((H_WMAX.getBinContent(index)-pedestal)*LSB  > speThresh) {
// 		//shape.setColor(100, 0, 0);
//                 shape.setColor(100, 0, 0);
// 		// 		int value = (int)H_WMAX.getBinContent(index);
// 		// 		shape.setColor(value, 0, 0);
//             }
            else {
                shape.setColor(255, 255, 255);
            }
        }
    }



    public void initHistograms() {
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
			sLC[2]).setFillColor(4);
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
					HistPara.getTitle(), 100, 500.0, 4500.0));
            H_COSMIC_CHARGE.get(sLC[0],
				sLC[1], 
				sLC[2]).setFillColor(0);
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
				       HistPara.getTitle(), 100, 10.0, 510.0));
            H_NOISE_CHARGE.get(sLC[0],
			       sLC[1], 
			       sLC[2]).setFillColor(0);
            H_NOISE_CHARGE.get(sLC[0],
			       sLC[1], 
			       sLC[2]).setXTitle("Charge (pC)");
            H_NOISE_CHARGE.get(sLC[0],
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
        H_fADC_N   = new H1D("fADC", 232, 0, 232);
        H_WMAX     = new H1D("WMAX", 232, 0, 232);
        H_COSMIC_N = new H1D("EVENT", 232, 0, 232);
        
        crystalID       = new double[332];
        noiseRMS        = new double[332];
        int ipointer=0;

        
    }



    public void resetHistograms() {
       	HistogramParam HistPara =  new HistogramParam();
	
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



    public void processDecodedEvent() {
        nProcessed++;
        
    	List<DetectorCounter> counters = decoder.getDetectorCounters(DetectorType.FTHODO);
        
        //System.out.println("event #: " + nProcessed);
	
	FTHODOViewerModule.MyADCFitter fadcFitter = new FTHODOViewerModule.MyADCFitter();
        
	H_WMAX.reset();
        
	int[][][] timediff = new int[8][2][20];
        
	int nPosADC;
	int nNegADC;
	
        for (DetectorCounter counter : counters) {
	    
	    // int[] sLC = {counter.getDescriptor().getSector(),
// 			 counter.getDescriptor().getLayer(),
// 			 counter.getDescriptor().getComponent()}
	    	    
            int sector = counter.getDescriptor().getSector();
            int layer = counter.getDescriptor().getLayer();
            int component = counter.getDescriptor().getComponent();
	    
	    //System.out.println("sector: " + sector + "  layer:" + layer + "  component:" + component);
        
	    int sector_count[] = {0,9,29,38,58,67,87,96};
            int index = (layer -1 ) *116+sector_count[sector-1]+component;
	    
	    // System.out.println(counters.size() + " " + icounter + " " + counter.getDescriptor().getComponent());
	    // System.out.println(counter);
            
            fadcFitter.fit(counter.getChannels().get(0));
            
	    short pulse[] = counter.getChannels().get(0).getPulse();
            
	    H_fADC_N.fill(index);
            
	    
	    // reset non-accumulating histograms
	    H_WAVE.get(sector, layer, component).reset();
            H_CWAVE.get(sector, layer, component).reset();
	    H_NPE.get(sector, layer, component).reset();
	    
	    // Loop through fADC bins filling histograms
            for (int i = 0; 
		 i < Math.min(pulse.length, 
			      H_fADC.get(sector, 
					 layer, 
					 component).getAxis().getNBins());
		 i++) {
		H_WAVE.get(sector, layer, component).fill(i, pulse[i]);
                H_CWAVE.get(sector, layer, component).fill(i*4, (pulse[i] - fadcFitter.getPedestal())*LSB );
		
		//H_NPE.get(sector, layer, component).fill(i*4, (pulse[i] - fadcFitter.getPedestal())*LSB/voltsPerSPE );
		H_NPE.get(sector, layer, component).fill(i*4, pulse[i]*LSB/voltsPerSPE );

                H_fADC.get(sector, layer, component).fill(i, pulse[i] - fadcFitter.getPedestal() + 10.0);
	    }
	    
	    // Fill histograms with single value per event
	    
	    H_COSMIC_CHARGE.get(sector, layer, component)
		.fill(counter.getChannels().get(0).getADC().get(0)*LSB*4.0/50);
	    
	    H_NOISE_CHARGE.get(sector, layer, component)
		.fill(counter.getChannels().get(0).getADC().get(0)*LSB*4.0/50);
	    
// 	    if(sector == 5 && layer == 1 && component == 5) {
// 		System.out.println(" nProcessed = " + nProcessed);
// 		System.out.println(" ADC        = " + counter.getChannels().get(0).getADC());
// 	    }
	    
	    if (fadcFitter.getWave_Max()-fadcFitter.getPedestal()>10)
                H_MAXV.get(sector, layer, component).fill((fadcFitter.getWave_Max()-fadcFitter.getPedestal())*LSB);
            
	    if (fadcFitter.getADCtime()>0){
                H_FADCSAMPLE.get(sector, layer, component).fill(fadcFitter.getADCtime()*4.0);
                timediff[sector-1][layer-1][component-1]=fadcFitter.getADCtime()*4;
            }
            
	    H_WMAX.fill(index,fadcFitter.getWave_Max()-fadcFitter.getPedestal());
	    
	    if(fadcFitter.getWave_Max()-fadcFitter.getPedestal()>fADCThreshold)
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
	
	// User chooses which histogram/s to display
        if      (plotSelect == 0 ) {
            canvas.cd(layerSelect-1);
            if(H_WAVE.hasEntry(secSelect,
			       layerSelect,
			       componentSelect))
                this.canvas.draw(H_WAVE.get(secSelect,
					    layerSelect,
					    componentSelect));
            canvas.cd(layerSelect%2);
            if(H_WAVE.hasEntry(secSelect,
			       (layerSelect%2)+1,
			       componentSelect))
                this.canvas.draw(H_WAVE.get(secSelect,
					    (layerSelect%2)+1,
					    componentSelect));
        }
	else if (plotSelect == 1 ) {
            canvas.cd(layerSelect-1);
            if(H_CWAVE.hasEntry(secSelect,
				layerSelect,
				componentSelect))
                this.canvas.draw(H_CWAVE.get(secSelect,
					     layerSelect,
					     componentSelect));
            canvas.cd(layerSelect%2);
            if(H_CWAVE.hasEntry(secSelect,
				(layerSelect%2)+1,
				componentSelect))
                this.canvas.draw(H_CWAVE.get(secSelect,
					     (layerSelect%2)+1,
					     componentSelect));
        }
	else if (plotSelect == 2 ) {
            canvas.cd(layerSelect-1);
            if(H_NPE.hasEntry(secSelect,
			      layerSelect,
			      componentSelect))
                this.canvas.draw(H_NPE.get(secSelect,
					   layerSelect,
					   componentSelect));
            canvas.cd(layerSelect%2);
            if(H_NPE.hasEntry(secSelect,
			      (layerSelect%2)+1,
			      componentSelect))
                this.canvas.draw(H_NPE.get(secSelect,
					   (layerSelect%2)+1,
					   componentSelect));
        }
	else if (plotSelect == 10 ) {
            canvas.cd(layerSelect-1);
            if(H_MAXV.hasEntry(secSelect,
			       layerSelect,
			       componentSelect))
                this.canvas.draw(H_MAXV.get(secSelect,
					    layerSelect,
					    componentSelect));
            canvas.cd(layerSelect%2);
            if(H_MAXV.hasEntry(secSelect,
			       (layerSelect%2)+1,
			       componentSelect))
                this.canvas.draw(H_MAXV.get(secSelect,
					    (layerSelect%2)+1,
					    componentSelect));
        }
	// else if ( plotSelect == 11 ) {
//             this.canvas.divide(1, 2);
//             canvas.cd(layerSelect-1);
//             if(H_COSMIC_CHARGE.hasEntry(secSelect,
// 					layerSelect,
// 					componentSelect))
//                 this.canvas.draw(H_COSMIC_CHARGE.get(secSelect,
// 						     layerSelect,
// 						     componentSelect));
//             canvas.cd(layerSelect%2);
//             if(H_COSMIC_CHARGE.hasEntry(secSelect,
// 					(layerSelect%2)+1, 
// 					componentSelect))
//                 this.canvas.draw(H_COSMIC_CHARGE.get(secSelect,
// 						     (layerSelect%2)+1, 
// 						     componentSelect));
//         }

	// else if (plotSelect == 2 ||plotSelect ==  3 ) {
//             this.canvas.divide(1, 3);
//             canvas.cd(layerSelect-1);
//             if(H_FADCSAMPLE.hasEntry(secSelect,layerSelect,componentSelect))
//                 this.canvas.draw(H_FADCSAMPLE.get(secSelect,layerSelect,componentSelect));
//             canvas.cd(layerSelect%2);
//             if(H_FADCSAMPLE.hasEntry(secSelect,(layerSelect%2)+1,componentSelect))
//                 this.canvas.draw(H_FADCSAMPLE.get(secSelect,(layerSelect%2)+1,componentSelect));
//             canvas.cd(2);
//             if(H_FADCSAMPLEdiff.hasEntry(secSelect,1,componentSelect))
//                 this.canvas.draw(H_FADCSAMPLEdiff.get(secSelect,1,componentSelect));
//         }
	
	//        else if (plotSelect == 3 ) {
//            this.canvas.divide(1, 1);
//            canvas.cd(0);
//            if(H_FADCSAMPLEdiff.hasEntry(secSelect,1,componentSelect))
//                this.canvas.draw(H_FADCSAMPLEdiff.get(secSelect,1,componentSelect));
//        }

        
	//this.dcHits.show();
        this.view.repaint();

        
        
    }



    public void initPanel() {

        JSplitPane splitPane = new JSplitPane();

        splitPane.setLeftComponent(this.view);
  
        
        JPanel canvasPane = new JPanel();
        canvasPane.setLayout(new BorderLayout());

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        
        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(this);
        
	buttonPane.add(resetBtn);
        

	ButtonGroup group = new ButtonGroup();

	//=================================
	//      PLOTTING OPTIONS
	//=================================

	//
	// Non-accumulated
        //
	// 0 - waveforms
	// 1 - waveforms calibrated in time and voltage
	// 2 - voltage / npe voltage peak (40 mV for now)

	JRadioButton wavesRb     = new JRadioButton("Waveforms");  // raw pulse
	JRadioButton cWavesRb    = new JRadioButton("Calibrated"); // ns/mV
	JRadioButton npeWavesRb  = new JRadioButton("NPE Wave"); // voltage / spe voltage
	
        group.add(wavesRb);
        buttonPane.add(wavesRb);
        wavesRb.setSelected(true);
        wavesRb.addActionListener(this);
        
        group.add(cWavesRb);
        buttonPane.add(cWavesRb);
//        cWavesRb.setSelected(true);
        cWavesRb.addActionListener(this);
        
	group.add(npeWavesRb);
        buttonPane.add(npeWavesRb);
        //npeWavesRb.setSelected(true);
        npeWavesRb.addActionListener(this);
        
	//
	// Accumulated
	//
	// 10 - Max Pulse Voltage
	// 11 - Charge
	// ...
	
	JRadioButton maxVoltRb  = new JRadioButton("Max"); // pulse max in mV
        JRadioButton chargeRb   = new JRadioButton("Charge"); // integral in pF
	
	group.add(maxVoltRb);
	buttonPane.add(maxVoltRb);
        //maxVoltRb.setSelected(true);
        maxVoltRb.addActionListener(this);
	
	group.add(chargeRb);
	buttonPane.add(chargeRb);
        //chargeRb.setSelected(true);
        chargeRb.addActionListener(this);



	
	//=======================================================
	//=======================================================
	// IN PROGRESS

	// JRadioButton fadcsampleRb  = new JRadioButton("fADC time");
//         JRadioButton fitRb  = new JRadioButton("Fit Timing");
//         group.add(fitRb);
//         buttonPane.add(fitRb);
//         //fitRb.setSelected(true);
//         fitRb.addActionListener(this);
        
//         group.add(fadcsampleRb);
//         buttonPane.add(fadcsampleRb);
//         //fadcsampleRb.setSelected(true);
//         fadcsampleRb.addActionListener(this);

	
        //=======================================================
	//=======================================================
        this.canvas.divide(1, 2);
        this.canvas.cd(0);
        this.canvas.setGridX(false);
        this.canvas.setGridY(false);
        this.canvas.setAxisFontSize(10);
        this.canvas.setTitleFontSize(16);
        this.canvas.setAxisTitleFontSize(14);
        this.canvas.setStatBoxFontSize(8);
        this.canvas.cd(1);
        this.canvas.setGridX(false);
        this.canvas.setGridY(false);
        this.canvas.setAxisFontSize(10);
        this.canvas.setTitleFontSize(16);
        this.canvas.setAxisTitleFontSize(14);
        this.canvas.setStatBoxFontSize(8);
    
	canvasPane.add(this.canvas, BorderLayout.CENTER);
	canvasPane.add(buttonPane, BorderLayout.PAGE_END);

	JTabbedPane tabbedPane = new JTabbedPane();
	
// 	JPanel canvasPane = new JPanel();
// 	canvasPane.setLayout(new BorderLayout());
	
// 	JPanel buttonPane = new JPanel();
//         buttonPane.setLayout(new FlowLayout());

// 	JButton resetBtn = new JButton("Reset");
//         resetBtn.addActionListener(this);
//         buttonPane.add(resetBtn);

	JButton fitBtn = new JButton("Fit");
        fitBtn.addActionListener(this);
        buttonPane.add(fitBtn);
	
		
        //=======================================================
	//=======================================================

	
        splitPane.setLeftComponent(this.view);
        splitPane.setRightComponent(canvasPane);
	splitPane.setDividerLocation(400);
	
        this.detectorPanel.add(splitPane, BorderLayout.CENTER);
	

    } // end of: public void initPanel() {



    public void initDecoder() {
        decoder.addFitter(DetectorType.FTHODO,
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

            layer = index/116 + 1;    

            if (layer==1)
                layerStr = "thin";
            else
                layerStr = "thick";

            // (map indices in both layers to [0,115])
            //  /map indices to quadrants [0,3]
            quadrant = (index-(layer-1)*116) / 29;  

            // map indices to [0,28]
            element = index - quadrant * 29 -(layer-1)*116; 

            // map quadrant to sectors [1,8]
            // map element to crystals [1,9] or
            // map element to crystals [1,20]

            if (element<9) {
                sector  = quadrant*2 +1;
                component = element + 1;
            }
            else {
                sector  = quadrant*2 +2;
                component = element + 1 -9;
            }

            title =  " " +layerStr +" layer, sector" + sector + ", crystal" + component ;
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
                if(pulse[bin]-pedestal>100 && wmax-pedestal>fADCThreshold)
                    if (fadctimethre==0)
                        fadctimethre=bin;            
            }
            
                //Returns the smallest pedestal value. Works better if peak is close to the beginning of the histogram.
            
            rms = LSB * Math.sqrt(noise / (ped_i2 - ped_i1) - pedestal * pedestal);
            wave_max=wmax;
            fadctime=fadctimethre;
        }
        
    }

}



