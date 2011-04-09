/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.scenario.scenegraph.fx;

import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

import com.sun.scenario.scenegraph.SGText;
import com.sun.scenario.scenegraph.SGText.VAlign;

/**
 * @author Chris Campbell
 */
public class FXText extends FXAbstractShape {

    private SGText textNode;
    
    public FXText() {
        super(new SGText());
        this.textNode = (SGText)getLeaf();
        this.textNode.setAntialiasingHint(RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    
    public final String getText() { 
        return textNode.getText();
    }
            
    public void setText(String text) { 
        textNode.setText(text);
    }
    
    public final Font getFont() { 
        return textNode.getFont();
    }

    public void setFont(Font font) {
        textNode.setFont(font);
    }
    
    public final Point2D getLocation(Point2D rv) { 
        return textNode.getLocation(rv);
    }

    public final Point2D getLocation() {
        return textNode.getLocation(null);
    }

    public void setLocation(Point2D location) {
        textNode.setLocation(location);
    }
    
    public final VAlign getVerticalTextAlignment() {
        return textNode.getVerticalAlignment();
    }
    
    public void setVerticalTextAlignment(VAlign verticalAlignment) {
        textNode.setVerticalAlignment(verticalAlignment);
    }

    public final Object getAntialiasingHint() {
        return textNode.getAntialiasingHint();
    }

    public void setAntialiasingHint(Object hint) {
        textNode.setAntialiasingHint(hint);
    }
}
