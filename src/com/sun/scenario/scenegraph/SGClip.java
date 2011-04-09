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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * @author Chris Campbell
 */
public class SGClip extends SGFilter {

    // Soft clipping requires AlphaComposite.SrcIn, but that is not
    // available on AGUI, so fall back on hard clipping when not available
    private static boolean aaAvailable;
    static {
        try {
            aaAvailable = (AlphaComposite.SrcIn != AlphaComposite.Src);
        } catch (NoSuchFieldError e) {
        }
    }
    
    // TODO: override contains() to return true only when transformed
    // clip shape contains point and child.contains() returns true...
    
    private Shape shape;
    private boolean antialiased;
    private AffineTransform filterXform;
    
    public SGClip() {
    }
    
    public final Shape getShape() {
        return shape;
    }
    
    public void setShape(Shape shape) {
        this.shape = shape;
        markDirty(false);
    }
    
    private boolean useAntialiasing() {
        return antialiased && aaAvailable;
    }
    
    public final boolean isAntialiased() {
        return antialiased;
    }
    
    public void setAntialiased(boolean antialiased) {
        this.antialiased = antialiased;
        markDirty(false);
    }
    
    @Override
    public Rectangle2D getBounds(AffineTransform xform) {
        SGNode child = getChild();
        if (child == null) {
            // just an empty rectangle
            return new Rectangle2D.Float();
        }
        Rectangle2D childXformBounds = child.getBounds(xform);
        if (shape == null) {
            return childXformBounds;
        }
        Rectangle2D shapeBounds = shape.getBounds2D();
        if (xform != null) {
            shapeBounds = xform.createTransformedShape(shapeBounds).getBounds2D();
        }
        return shapeBounds.createIntersection(childXformBounds);
    }

    // TODO: sort out the whole calculateAccumBounds()/getBounds() mess...
    @Override
    Rectangle2D calculateAccumBounds() {
        return getBounds(getCumulativeTransform());
    }

    @Override
    public boolean canSkipRendering() {
        return (shape == null);
    }
    
    @Override
    public int needsSourceContent() {
        return useAntialiasing() ? TRANSFORMED : NONE;
    }
    
    @Override
    public void setupRenderGraphics(Graphics2D g) {
        if (!useAntialiasing()) {
            g.clip(shape);
        } else {
            // capture the current transform; we'll need it below when
            // setting up the soft clip in renderFinalImage()
            filterXform = g.getTransform();
        }
    }
    
    @Override
    public void renderFinalImage(Graphics2D g, SGSourceContent srcContent) {
        assert useAntialiasing();
        
        // Create a translucent intermediate image in which we can
        // perform the soft clipping
        Image src = srcContent.getTransformedImage();
        int srcw = src.getWidth(null);
        int srch = src.getHeight(null);
        GraphicsConfiguration gc = g.getDeviceConfiguration();
        BufferedImage tmp =
            gc.createCompatibleImage(srcw, srch, Transparency.TRANSLUCENT);
        Graphics2D gtmp = tmp.createGraphics();

        // Clear the image so all pixels have zero alpha
        gtmp.setComposite(AlphaComposite.Clear);
        gtmp.fillRect(0, 0, srcw, srch);

        // Render our clip shape into the image.  Note that we enable
        // antialiasing to achieve the soft clipping effect.
        gtmp.setComposite(AlphaComposite.Src);
        gtmp.setTransform(filterXform);
        gtmp.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
        gtmp.setColor(Color.WHITE);
        gtmp.fill(shape);
        gtmp.setTransform(new AffineTransform());

        // Here's the trick... We use SrcIn, which effectively
        // uses the alpha value as a coverage value for each pixel
        // stored in the destination.  For the areas outside our
        // clip shape, the destination alpha will be zero, so nothing
        // is rendered in those areas.  For the areas inside our
        // clip shape, the destination alpha will be fully opaque,
        // so the full color is rendered.  At the edges, the original
        // antialiasing is carried over to give us the desired soft
        // clipping effect.
        gtmp.setComposite(AlphaComposite.SrcIn);
        gtmp.drawImage(src, 0, 0, null);
        gtmp.dispose();
        
        // Render the final clipped image to the destination
        g.drawImage(tmp, 0, 0, null);
    }

    @Override 
    public boolean contains(Point2D point) {
        return (shape == null) ? super.contains(point) : shape.contains(point);
    }

    @Override
    boolean hasOverlappingContents() {
        if (shape != null) {
            if (isAntialiased()) {
                return false;
            }
        }
        return super.hasOverlappingContents();
    }
}
