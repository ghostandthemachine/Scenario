package com.test;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JFrame;

import com.sun.scenario.scenegraph.JSGPanel;
import com.sun.scenario.scenegraph.SGGroup;
import com.sun.scenario.scenegraph.fx.FXShape;

/**
 *
 * @author jon rose
 */
public class Test {

    public Test() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {


        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(800, 800));
        frame.pack();
        frame.setVisible(true);
        frame.setTitle("Scenario interface tests");
        frame.setLayout(new BorderLayout());

        JSGPanel master = new JSGPanel();
        
        SGGroup root = new SGGroup();
        
        
        FXShape shape = new FXShape();
        shape.setShape(new RoundRectangle2D.Double(0,0,150d, 100d, 10d, 10d));
        shape.setFillPaint(Color.red);

        root.add(shape);

        master.setScene(root);
        master.setBackground(Color.white);

        frame.add(master);


    }
}
