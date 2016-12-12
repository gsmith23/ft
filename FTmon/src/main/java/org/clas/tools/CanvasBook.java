/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import org.root.base.IDataSet;
import org.root.basic.EmbeddedCanvas;


/**
 *
 * @author louiseclark & fanchini
 */


public class CanvasBook extends JPanel implements ActionListener {
    
    private List<IDataSet> container = new ArrayList<IDataSet>();
    private List<String>   options   = new ArrayList<String>();
    private EmbeddedCanvas canvas    = new EmbeddedCanvas();
    private int            nDivisionsX = 1;
    private int            nDivisionsY = 1;
    private int            currentPosition = 0;
    private boolean backward = false; 
    JComboBox comboDivide = null;
    private int elements =1;
        
        
    public CanvasBook(int dx, int dy){
        
	//        TStyle.setFrameFillColor(250, 250, 255);
        this.setLayout(new BorderLayout());
        
        this.nDivisionsX = dx;
        this.nDivisionsY = dy;
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        
        JButton  buttonPrev = new JButton("< Previous");
        JButton  buttonNext = new JButton("Next >");
        JButton  buttonSave = new JButton("Print...");
        buttonNext.addActionListener(this);
        buttonPrev.addActionListener(this);
        buttonSave.addActionListener(this);
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(buttonPrev);
        buttonPanel.add(buttonNext);
        buttonPanel.add(buttonSave);
        canvas.divide(this.nDivisionsX, this.nDivisionsY);
        
    
        JPanel canvasPane = new JPanel();
        canvasPane.setLayout(new BorderLayout());
        canvasPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        canvasPane.add(canvas,BorderLayout.CENTER);
        canvasPane.setPreferredSize(new Dimension(1100,900));
        this.add(canvasPane,BorderLayout.CENTER);
        this.add(buttonPanel,BorderLayout.PAGE_END);
        
    }

    public void initCanvas() {
        // event canvas
        this.canvas.setGridX(false);
        this.canvas.setGridY(false);
        this.canvas.setAxisFontSize(10);
        this.canvas.setTitleFontSize(16);
        this.canvas.setAxisTitleFontSize(14);
        this.canvas.setStatBoxFontSize(8);
    }

    public void add(IDataSet ds, String opt){
        this.container.add(ds);
        this.options.add(opt);
    }
    
    public void drawNextBack(boolean back){
        int npads   = this.nDivisionsX*this.nDivisionsY;
        int counter = 0, npp=1;
        findelement();
        if(back){
            this.currentPosition-=(2*this.elements*npads);            
            if(this.currentPosition>=this.container.size() ){
                this.currentPosition = this.container.size()-(2*this.elements*npads);
            }      
            else if (this.currentPosition<=0){ 
                if(this.currentPosition<=-1*npads){
                    this.currentPosition= this.container.size()-(1*this.elements*npads);
                }
                else{     
		    this.currentPosition = 0;
                }
                findelement();
            }
        }
        else{
            if(this.currentPosition>=this.container.size() || this.currentPosition<=0){
		this.currentPosition = 0;
		findelement();
            }
	}
       
         
        canvas.divide(this.nDivisionsX,this.nDivisionsY);
        canvas.cd(counter);
        while(this.currentPosition<this.container.size()&&counter<npads){
            IDataSet ds = this.container.get(this.currentPosition);
            String   op = this.options.get(this.currentPosition);    
       
            if(op.contains("same")==false){
                //System.out.println(" (    ) "  + this.currentPosition + 
                //        " on pad " + counter+"   "+ds.getName());
                canvas.cd(counter);
                this.initCanvas();
                canvas.draw(ds,op);
                
            } else {
                //System.out.println(" (same) "  + this.currentPosition + 
                //        " on pad " + counter+"   "+ds.getName());
                canvas.draw(ds, "op+same");
            }
            
            if(npp==this.elements){
                counter++;
                npp=0;
            }
            this.currentPosition++;
            npp++;
        }
    }
   
    public void findelement(){
        int first=0;
        this.elements=1;
        for(int i=this.currentPosition; i<this.container.size(); i++){
            if(this.options.get(i).contains("same") && first<=1){
		this.elements++;
            }
            else if(this.options.get(i).contains("same")==false)first++;
            else break;
        }        
    }
    
    public void reset(){
        this.currentPosition = 0;        
    }

    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().compareTo("Next >")==0){
            this.backward = false;
            this.drawNextBack(backward);
        }
        
        if(e.getActionCommand().compareTo("< Previous")==0){
	    this.backward = true;
	    this.drawNextBack(backward);
        }
        
        if (e.getActionCommand().compareTo("Print...") == 0) {
            this.printToFile();
           
        }
    }
    
    private void printToFile() {
        // Does not work properly //
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("File.PNG"));
        int returnValue = fc.showSaveDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            String outputFileName = fc.getSelectedFile().getAbsolutePath();
            this.canvas.save(outputFileName);
            System.out.println("Saving calibration results to: " + outputFileName);
        }
    }  
    
    
}

