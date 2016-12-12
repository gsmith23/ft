/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.histogram.H1D;

/**
 *
 * @author devita
 */
public class FTApplication implements ActionListener {

    private String             appName    = null;
    private FTDetector         detector   = null;
    private FTDataSet          dataSet    = null;
    private List<NoGridCanvas> canvases   = new ArrayList<NoGridCanvas>();
    private JPanel             radioPane  = new JPanel();
    private List<String>       fields     = new ArrayList<String>();
    private List<FTParameter>  parameters = new ArrayList<FTParameter>();
    private ExtendedFADCFitter eFADCFitter;
    private String             buttonSelect;
    private int                buttonIndex;
    private String             canvasSelect;
    private int                canvasIndex;
    
    public FTApplication(FTDetector d) {
        this.detector = d;
        this.dataSet  = new FTDataSet(d);
    }
  
    public FTApplication(FTDetector d, String name) {
        this.appName  = name;
        this.detector = d;
        this.dataSet  = new FTDataSet(d);
        this.addCanvas(name);
    }
  
    public FTApplication(FTDetector d, String name, String... fields) {
        this.appName  = name;
        this.detector = d;
        this.dataSet  = new FTDataSet(d);
        this.addFields(fields);
        this.addCanvas(name);
    } 
    
    public FTDetector getDetector() {
        return this.detector;
    }
    
    public String getName() {
        return this.appName;
    }
    
    public void setName(String name) {
        this.appName=name;
    }
    
    public final void addCanvas(String name) {
        NoGridCanvas c = new NoGridCanvas();
        this.canvases.add(c);
        this.canvases.get(this.canvases.size()-1).setName(name);
    }
    
    public NoGridCanvas getCanvas(int index) {
        return this.canvases.get(index);
    }
    
    public NoGridCanvas getCanvas(String name) {
        int index=0;
        for(int i=0; i<this.canvases.size(); i++) {
            if(this.canvases.get(i).getName() == name) {
                index=i;
                break;
            }
        }
        return this.canvases.get(index);
    }

    public List<FTParameter> getParameters() {
        return this.parameters;
    }  

    public FTParameter getParameter(int index) {
        return this.parameters.get(index);
    }  

    public FTParameter getParameter(String name) {
        FTParameter par = null;
        for(int i=0; i<this.parameters.size(); i++) {
            if(name == this.parameters.get(i).getName()) {
                par = this.parameters.get(i);
                break;
            }
        }
        return par;
    }  
    
    public FTParameter getSelectedParameter() {
        return this.parameters.get(buttonIndex);
    } 
    
    public void setRadioPane(JPanel radioPane) {
        this.radioPane = radioPane;
    }

    public JPanel getRadioPane() {
        return radioPane;
    }
    
    public String getButtonSelect() {
        return buttonSelect;
    }
    
    public int getButtonIndex() {
        return buttonIndex;
    }
    
    public String getCanvasSelect() {
        if(canvasSelect == null) {
            canvasIndex  = 0;
            canvasSelect = this.canvases.get(0).getName();
        }
        return canvasSelect;
    }
    
    public void setCanvasSelect(String name) {
        canvasIndex  = 0;
        canvasSelect = this.canvases.get(0).getName();
        for(int i=0; i<canvases.size(); i++) {
            if(canvases.get(i).getName() == name) {
                canvasIndex = i;
                canvasSelect = name;
                break;
            }
        }
    }
    
    public void setCanvasIndex(int index) {
        if(index>=0 && index < this.canvases.size()) {
            canvasIndex  = index;
            canvasSelect = this.canvases.get(index).getName();
        }
        else {
            canvasIndex  = 0;
            canvasSelect = this.canvases.get(0).getName();
        }
    }

    public FTDataSet getData() {
        return dataSet;
    }
    
    public final void addFields(String... fields){
        for(String item : fields){
            this.fields.add(item);
            this.parameters.add(new FTParameter(item));
        }
        this.setRadioButtons();
    }
    
    public List<String> getFields(){
        return this.fields;
    }
    
    public void setRadioButtons() {
        this.radioPane.setLayout(new FlowLayout());
	ButtonGroup bG = new ButtonGroup();
        for (String field : this.fields) {
	    //            System.out.println(field);
            String item = field;
            // add buttons named as "fields" to the button group and panel
            JRadioButton b = new JRadioButton(item);
            if(bG.getButtonCount()==0) b.setSelected(true);
            b.addActionListener(this);
            this.radioPane.add(b);
            bG.add(b);
        }
    }   
    
    public Color getColor(int key) {
        Color col = new Color(100, 100, 100);
        return col;
    }

    public double getFieldValue(int index, int key) {
        //System.out.println("Erica ftApp 1: "+index+" "+key+" "+this.parameters.get(index).getValue());
        return this.parameters.get(index).getValue();
    }

    public double getFieldValue(String name, int key) {
        int index=-1;
        for (int i=0; i<this.fields.size(); i++) {
            if (name == this.fields.get(i)) {
                index=i;
                break;
            }
        }
        //System.out.println("Erica ftApp 2: "+name+" "+key+" "+index+" "+this.getFieldValue(index,key));
        if(index==-1) return index;
        else          return this.getFieldValue(index,key);
        
    }
        
    public void actionPerformed(ActionEvent e) {
	//        System.out.println(this.getName() + " application radio button set to: " + e.getActionCommand());
        buttonSelect=e.getActionCommand();
        for(int i=0; i<this.fields.size(); i++) {
            if(buttonSelect == this.fields.get(i)) {
                buttonIndex=i;
                break;
            }
        }
    }
    
    public void resetCollections() {   
    }
    
    public void fitCollections() {
    }
    
    public void customizeFit(int key) {   
    }
    
    public void fitBook(DetectorCollection<H1D> h, DetectorCollection<F1D> f){
       
        JFrame     frame = new JFrame();
        CanvasBook book  = new CanvasBook(4,4);
        
        for(int key : this.detector.getDetectorComponents()) {
            if(h.hasEntry(0, 0, key)) {
                book.add(h.get(0,0,key)," ");
                book.add(f.get(0,0,key),"same"); 
            }
	}
        
        book.reset();
        frame.add(book);
        frame.pack();
        frame.setVisible(true);
        book.drawNextBack(false);
        
    }

    public ExtendedFADCFitter getFitter() {
        return eFADCFitter;
    }
    
    public void setFitter(ExtendedFADCFitter efitter) {
        this.eFADCFitter = efitter;
    }
    
    public void saveToFile(String hipoFileName) {
        
    }
        
}