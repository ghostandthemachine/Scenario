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

package com.sun.scenario.scenegraph;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.BasicStroke;

/**
 * An abstract scene graph node that is the base class of SGShape and SGText.
 * 
 * @author Chris Campbell
 */
public abstract class SGAbstractShape extends SGLeaf {
    private static final Stroke defaultStroke = new BasicStroke(1f);
    
    public enum Mode {STROKE, FILL, STROKE_FILL};
    
    Mode mode = Mode.FILL;
    Paint drawPaint = Color.WHITE;
    Paint fillPaint = Color.BLACK;
    Stroke drawStroke = defaultStroke;

    public abstract Shape getShape();

    public final Mode getMode() { 
        return mode;
    }

    public final void setMode(Mode mode) { 
        if (mode == null) {
            throw new IllegalArgumentException("null mode");
        }
        this.mode = mode;
        repaint(true);
    }

    public final Paint getDrawPaint() {
        return drawPaint;
    }

    public void setDrawPaint(Paint drawPaint) {
        if (drawPaint == null) {
            throw new IllegalArgumentException("null drawPaint");
        }
        this.drawPaint = drawPaint;
        repaint(false);
    }

    public final Paint getFillPaint() {
        return fillPaint;
    }

    public void setFillPaint(Paint fillPaint) {
        if (fillPaint == null) {
            throw new IllegalArgumentException("null fillPaint");
        }
        this.fillPaint = fillPaint;
        repaint(false);
    }

    public final Stroke getDrawStroke() {
        return drawStroke;
    }

    public void setDrawStroke(Stroke drawStroke) {
        if (drawStroke == null) {
            throw new IllegalArgumentException("null drawStroke");
        }
        this.drawStroke = drawStroke;
        repaint(true);
    }

    @Override
    boolean hasOverlappingContents() {
        return getMode() == Mode.STROKE_FILL;
    }
}
