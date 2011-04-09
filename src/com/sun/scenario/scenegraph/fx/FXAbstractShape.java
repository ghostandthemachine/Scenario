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

import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;

import com.sun.scenario.scenegraph.SGAbstractShape;
import com.sun.scenario.scenegraph.SGAbstractShape.Mode;

/**
 * @author Chris Campbell
 */
public class FXAbstractShape extends FXNode {

    private SGAbstractShape shapeNode;
    
    FXAbstractShape(SGAbstractShape shape) {
        super(shape);
        this.shapeNode = shape;
    }
    
    public final Shape getShape() {
        return shapeNode.getShape();
    }
    
    public final Mode getMode() { 
        return shapeNode.getMode();
    }

    public final void setMode(Mode mode) { 
        shapeNode.setMode(mode);
    }

    public final Paint getDrawPaint() {
        return shapeNode.getDrawPaint();
    }

    public void setDrawPaint(Paint drawPaint) {
        shapeNode.setDrawPaint(drawPaint);
    }

    public final Paint getFillPaint() {
        return shapeNode.getFillPaint();
    }

    public void setFillPaint(Paint fillPaint) {
        shapeNode.setFillPaint(fillPaint);
    }

    public final Stroke getDrawStroke() {
        return shapeNode.getDrawStroke();
    }

    public void setDrawStroke(Stroke drawStroke) {
        shapeNode.setDrawStroke(drawStroke);
    }
}
