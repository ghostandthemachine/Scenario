/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * This class acts as the delegate for the Paint interface so that
 * existing Paint implementations that are based on coordinates in
 * user space can be reused to render patterns that are relative to
 * the bounding box of the Shape being rendered.
 * 
 * @author flar
 */
public class ProportionalPaint implements Paint {
    private Paint origPaint;

    public ProportionalPaint(Paint origPaint) {
        this.origPaint = origPaint;
    }

    @Override
	public int getTransparency() {
        return origPaint.getTransparency();
    }

    @Override
	public PaintContext createContext(ColorModel cm,
                                      Rectangle devBounds,
                                      Rectangle2D usrBounds,
                                      AffineTransform at,
                                      RenderingHints hints)
    {
        AffineTransform at2 = new AffineTransform(at);
        at2.translate(usrBounds.getX(), usrBounds.getY());
        at2.scale(usrBounds.getWidth(), usrBounds.getHeight());
        return origPaint.createContext(cm, devBounds,
                                       new Rectangle(0, 0, 1, 1),
                                       at2, hints);
    }
}
