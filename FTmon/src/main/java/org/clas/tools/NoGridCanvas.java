/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import org.root.basic.EmbeddedCanvas;

/**
 *
 * @author devita
 */
public class NoGridCanvas extends EmbeddedCanvas {

    public NoGridCanvas() {  
        super();
        this.setGridX(false);
        this.setGridY(false);
        this.initStyle();
    }
    
    public NoGridCanvas(int rows, int cols) {  
        super();
        this.divideCanvas(rows, cols);
    }
    
    public final void initStyle() {
        this.setAxisFontSize(10);
        this.setTitleFontSize(16);
        this.setAxisTitleFontSize(14);
        this.setStatBoxFontSize(8);        
    }
    
    public final void divideCanvas(int rows, int cols) {
        this.divide(rows,cols);
        for(int loop = 0; loop < cols*rows; loop++){
            this.cd(loop);
            this.setGridX(false);
            this.setGridY(false);
            this.initStyle();        
        }
    }
   
    
}