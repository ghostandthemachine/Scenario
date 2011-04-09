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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.font.LineMetrics;

/**
 * A scene graph node that renders a single line of text.
 * 
 * @author Chet Haase
 * @author Hans Muller
 */
public class SGText extends SGAbstractShape {
    private static Font defaultFont = new Font("Serif", Font.PLAIN, 12);

    public enum VAlign { BASELINE, TOP, BOTTOM }

    private Font font = defaultFont;
    private String text;
    private Object antialiasingHint = RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
    private final Point2D.Float location = new Point2D.Float();
    private VAlign verticalAlignment = VAlign.BASELINE;

    private AffineTransform lastTx = new AffineTransform();
    private boolean lastAA;
    private Rectangle2D rawBounds;

    @Override
	public final Shape getShape() {
        return getOutline(getFontRenderContext(null));
    }
    
    public final String getText() { 
        return text;
    }
            
    public void setText(String text) { 
        this.text = text;
        this.rawBounds = null;
        repaint(true);
    }
    
    public final Font getFont() { 
        return font;
    }

    public void setFont(Font font) { 
        this.font = font;
        this.rawBounds = null;
        repaint(true);
    }
    
    /**
     * Defines the origin of the text.
     * The exact location where the text is rendered/measured depends on the
     * value of the vertical alignment property of this node.
     * Assuming the transform is null, this is the location in the parent
     * node where the string will be drawn.
     * If return value {@code rv} is non-null, it will be set and returned.
     * Otherwise a new Point will be allocated and returned.
     * 
     * @param rv the return value or null
     * @return the location where the text will be drawn
     */
    public final Point2D getLocation(Point2D rv) { 
        if (rv == null) {
            rv = new Point2D.Float();
        }
        rv.setLocation(location);
        return rv;
    }

    /**
     * This no-arg getter is equivalent to calling <code>getLocation(null)
     * </code>.
     *
     * @return the location where the text will be drawn
     * @see #getLocation(Point2D)
     */
    public final Point2D getLocation() {
        return getLocation(null);
    }

    public void setLocation(Point2D location) {
        if (location == null) {
            throw new IllegalArgumentException("null location");
        }
        this.location.setLocation(location);
        repaint(true);
    }
    
    public final VAlign getVerticalAlignment() {
        return verticalAlignment;
    }
    
    public void setVerticalAlignment(VAlign verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
        rawBounds = null;
        repaint(true);
    }

    public final Object getAntialiasingHint() {
        return antialiasingHint;
    }

    /**
     * Set the {@code KEY_TEXT_ANTIALIASING} rendering hint. The {@code hint} must be
     * one of: {@code RenderingHints.VALUE_TEXT_ANTIALIAS_ON}, 
     * {@code RenderingHints.VALUE_TEXT_ANTIALIAS_OFF}, 
     * {@code RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT}.  The default is
     * {@code VALUE_TEXT_ANTIALIAS_DEFAULT}.
     * 
     * @see java.awt.RenderingHints
     * @see java.awt.Graphics2D
     */
    public void setAntialiasingHint(Object hint) {
        if (!RenderingHints.KEY_TEXT_ANTIALIASING.isCompatibleValue(hint)) {
            throw new IllegalArgumentException("invalid hint");
        }
        antialiasingHint = hint;
        rawBounds = null;
        repaint(false);
    }
    
    @Override
    public void paint(Graphics2D g) {
        if (text != null && font != null) {
            g.setFont(font);

            if (mode == Mode.FILL) {
                // use drawString() for better quality and performance
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                   antialiasingHint);
                float y = location.y;
                if (verticalAlignment != VAlign.BASELINE) {
                    y += getYAdjustment(g.getFontRenderContext());
                }
                g.setPaint(fillPaint);
                if (DO_PAINT) {
                    g.drawString(text, location.x, y);
                }
            } else {
                // since a stroke is involved, we need to use outlines
                if (antialiasingHint == RenderingHints.VALUE_TEXT_ANTIALIAS_ON) {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                       RenderingHints.VALUE_ANTIALIAS_ON);
                }
                Shape outline = getOutline(g.getFontRenderContext());
                if (mode == Mode.STROKE_FILL) {
                    g.setPaint(fillPaint);
                    if (DO_PAINT) {
                        g.fill(outline);
                    }
                }
                g.setPaint(drawPaint);
                g.setStroke(drawStroke);
                if (DO_PAINT) {
                    g.draw(outline);
                }
            }
        }
    }
    
    @Override
    public final Rectangle2D getBounds(AffineTransform transform) {
        if (font == null || text == null) {
            return new Rectangle2D.Float(location.x, location.y, 0f, 0f);
        }

        Rectangle2D rbounds = getRawBounds(transform);

        // offset visual bounds by the text location
        Rectangle2D bounds = new Rectangle2D.Float();
        bounds.setRect(rbounds.getX() + location.x,
                       rbounds.getY() + location.y,
                       rbounds.getWidth(), rbounds.getHeight());

        if (transform != null && !transform.isIdentity()) {
            // transform the bounds according to the given AffineTransform
            bounds = transform.createTransformedShape(bounds).getBounds2D();
        }

        // now that we are in the transformed/device space, add a small
        // amount of padding to the edges of the bounding box (this is
        // somewhat arbitrary, but it's been determined that these padding
        // values are sufficient for all kinds of transformed and
        // non-transformed text)
        bounds.setRect(bounds.getX() - 2,
                       bounds.getY() - 2,
                       bounds.getWidth() + 4,
                       bounds.getHeight()+ 4);

        return bounds;
    }

    private final Rectangle2D getRawBounds(AffineTransform transform) {
        boolean aa =
            !(antialiasingHint == RenderingHints.VALUE_TEXT_ANTIALIAS_OFF ||
              antialiasingHint == RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        if (rawBounds != null && lastAA == aa) {
            if (transform == null || transform.isIdentity()) {
                if (lastTx.isIdentity()) {
                    return rawBounds;
                }
            } else {
                if (lastTx.getScaleX() == transform.getScaleX() &&
                    lastTx.getShearY() == transform.getShearY() &&
                    lastTx.getShearX() == transform.getShearX() &&
                    lastTx.getScaleY() == transform.getScaleY())
                {
                    return rawBounds;
                }
            }
        }

        if (transform == null) {
            lastTx.setToIdentity();
        } else {
            lastTx.setTransform(transform.getScaleX(), transform.getShearY(),
                                transform.getShearX(), transform.getScaleY(),
                                0, 0);
        }
        lastAA = aa;

        /*
         * It has been determined that TextLayout/GlyphVector.getPixelBounds()
         * does not return a perfect bounding box (i.e. does not entirely
         * enclose the all touched pixels) in some transform situations.  (On
         * Apple's JDK, these methods have even more serious issues when a
         * transform is in effect.)  Therefore, we simulate the behavior of
         * that method here by using GlyphVector.getVisualBounds().  That
         * method returns the untransformed bounding box in user space, but
         * it does not necessarily encompass all touched pixels as
         * getPixelBounds() is supposed to.  To get an accurate bounding box,
         * we need to transform the visual bounds according to the given
         * transform object, and then make slight adjustments to the
         * transformed bounds, which will account for minor differences
         * between the logical and rasterized text shapes.
         */
        FontRenderContext frc = new FontRenderContext(lastTx, aa, false);
        if (mode == Mode.FILL || drawStroke == null) {
            // no stroke to worry about, so just use visual bounds directly
            rawBounds = getGlyphVector(frc).getVisualBounds();
        } else {
            // there's a stroke, so we need to get the complete bounds
            // of the stroked outline
            Shape s = getOutline(frc, 0, 0);
            s = drawStroke.createStrokedShape(s);
            rawBounds = s.getBounds2D();
        }
        rawBounds.setRect(rawBounds.getX(), rawBounds.getY()+getYAdjustment(frc),
                          rawBounds.getWidth(), rawBounds.getHeight());

        return rawBounds;
    }
    
    private GlyphVector getGlyphVector(FontRenderContext frc) {
        if (font == null) {
            throw new IllegalStateException("no font specified");
        }
        if (text == null) {
            throw new IllegalStateException("no text specified");
        }
        return font.createGlyphVector(frc, text);
    }
    
    private FontRenderContext getFontRenderContext(AffineTransform transform) {
        // TODO: The following was changed to use the boolean constructor
        // for running on pre-1.6 JDKs.  If we add support for LCD text later,
        // we'll need to change the following back to using the hints directly.
        //return 
        //    new FontRenderContext(transform, antialiasingHint,
        //                          RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
        boolean aa =
            !(antialiasingHint == RenderingHints.VALUE_TEXT_ANTIALIAS_OFF ||
              antialiasingHint == RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        return new FontRenderContext(transform, aa, false);
    }

    private float getYAdjustment(FontRenderContext frc) {
        if (font == null) {
            throw new IllegalStateException("no font specified");
        }
        if (text == null) {
            throw new IllegalStateException("no text specified");
        }

        if (verticalAlignment != VAlign.BASELINE) {
            LineMetrics lm = font.getLineMetrics(text, frc);
            if (verticalAlignment == VAlign.TOP) {
                // TODO: This doesn't make much sense, but it seems to be
                // the calculation used by Jazz, so for now we'll stick with
                // it for compatibility reasons.  Ideally we would just use
                // +lm.getAscent() here, but Java 2D tends to report an
                // abnormally high ascent (usually about 5 or so pixels
                // higher than it should be), so we use the descent as a
                // hacky heuristic to bring this adjustment in line with
                // the expected result.  If we can fix Java 2D to return
                // a more reasonable answer for getAscent(), then we should
                // change this code to simply return getAscent().
                return lm.getAscent() - lm.getDescent();
            } else {
                return -lm.getDescent();
            }
        }
        return 0f;
    }
    
    // TODO: cache the outline
    private Shape getOutline(FontRenderContext frc) {
        return getOutline(frc, location.x, location.y + getYAdjustment(frc));
    }

    private Shape getOutline(FontRenderContext frc, float x, float y) {
        GlyphVector gv = getGlyphVector(frc);
        return gv.getOutline(x, y);
    }
}
