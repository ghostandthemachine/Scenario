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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;

/**
 * A scene graph node that renders a Shape.
 * 
 * @author Chet Haase
 * @author Hans Muller
 */
public class SGShape extends SGAbstractShape {
    private Shape shape;
    private Shape cachedStrokeShape;
    private Object antialiasingHint = RenderingHints.VALUE_ANTIALIAS_DEFAULT;
    
    /**
     * Returns a reference to (not a copy of) the {@code Shape} of this node.
     * The default value of this property is null.
     * <p>
     * Typically the {@code shape} property will be set once when the
     * {@code SGShape} is first constructed.  If thereafter the {@code shape}
     * object is modified, it is the user's responsibility to call
     * {@code setShape()} to ensure that the node state is properly updated.
     *
     * @return the {@code Shape} of this node
     */
    @Override
	public final Shape getShape() { 
        return shape;
    }

    /**
     * Sets the {@code Shape} of this node.
     * <p>
     * Typically the {@code shape} property will be set once when the
     * {@code SGShape} is first constructed.  If thereafter the {@code shape}
     * object is modified, it is the user's responsibility to call
     * {@code setShape()} to ensure that the node state is properly updated.
     *
     * @param shape the {@code Shape} of this node
     */
    public void setShape(Shape shape) {
        this.shape = shape;
        cachedStrokeShape = null;
        repaint(true);
    }

    /**
     * Returns the {@code KEY_ANTIALIASING} rendering hint.
     * The {@code hint} will be
     * one of: {@code RenderingHints.VALUE_ANTIALIAS_ON}, 
     * {@code RenderingHints.VALUE_ANTIALIAS_OFF}, 
     * {@code RenderingHints.VALUE_ANTIALIAS_DEFAULT}.
     * 
     * @return the {@code KEY_ANTIALIASING} hint
     * @see java.awt.RenderingHints
     */
    public Object getAntialiasingHint() {
        return this.antialiasingHint;
    }

    /**
     * Sets the {@code KEY_ANTIALIASING} rendering hint. The {@code hint} must be
     * one of: {@code RenderingHints.VALUE_ANTIALIAS_ON}, 
     * {@code RenderingHints.VALUE_ANTIALIAS_OFF}, 
     * {@code RenderingHints.VALUE_ANTIALIAS_DEFAULT}.  The default is
     * {@code VALUE_ANTIALIAS_DEFAULT}.
     * 
     * @see java.awt.RenderingHints
     * @see java.awt.Graphics2D
     */
    public void setAntialiasingHint(Object hint) {
        if (!RenderingHints.KEY_ANTIALIASING.isCompatibleValue(hint)) {
            // Note that KEY_ANTIALIASING.isCompatibleValue also rejects null
            throw new IllegalArgumentException("invalid hint");
        }
        this.antialiasingHint = hint;
        repaint(false);
    }

    @Override
    public void paint(Graphics2D g) {
        if (shape == null) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasingHint);
        if ((mode == Mode.FILL) || (mode == Mode.STROKE_FILL)) {
            g.setPaint(fillPaint);
            if (DO_PAINT) {
                g.fill(shape);
            }
        }
        if ((mode == Mode.STROKE) || (mode == Mode.STROKE_FILL)) {
            g.setPaint(drawPaint);
            g.setStroke(drawStroke);
            if (DO_PAINT) {
                try {
                    g.draw(shape);
                } catch (Throwable t) {
                    // Workaround for JDK bug 6670624
                    // We may get a Ductus PRError (extends RuntimeException)
                    // or we may get an InternalError (exends Error)
                    // The only common superclass of the two is Throwable...
                }
            }
        }
    }

    @Override
    public void setDrawStroke(Stroke drawStroke) {
        super.setDrawStroke(drawStroke);
        cachedStrokeShape = null;
    }

    private static final int AT_IDENT = 0;
    private static final int AT_TRANS = 1;
    private static final int AT_GENERAL = 2;
    private static int classify(AffineTransform at) {
        if (at == null) return AT_IDENT;
        switch (at.getType()) {
            case AffineTransform.TYPE_IDENTITY:
                return AT_IDENT;
            case AffineTransform.TYPE_TRANSLATION:
                return AT_TRANS;
            default:
                return AT_GENERAL;
        }
    }

    private Rectangle2D getBounds(AffineTransform at, Line2D l) {
        if (mode == Mode.FILL) {
            return new Rectangle2D.Float(0, 0, -1, -1);
        }
        if (drawStroke instanceof BasicStroke) {
            BasicStroke bs = (BasicStroke) drawStroke;
            float x1 = (float) l.getX1();
            float y1 = (float) l.getY1();
            float x2 = (float) l.getX2();
            float y2 = (float) l.getY2();
            float wpad = bs.getLineWidth() / 2f;
            int atclass = classify(at);
            if (atclass <= AT_TRANS) {
                float xpad, ypad;
                // TODO - if we used STROKE_PURE mode then this could be
                // max(wpad, 0.5f), but STROKE_NORM (the default) can move
                // line locations by up to a half a pixel so it is best to add.
                wpad += 0.5f;
                if (atclass == AT_TRANS) {
                    float tx = (float) at.getTranslateX();
                    float ty = (float) at.getTranslateY();
                    x1 += tx;
                    y1 += ty;
                    x2 += tx;
                    y2 += ty;
                }
                if (x1 == x2 && y1 != y2) {
                    ypad = wpad;
                    xpad = bs.getEndCap() == BasicStroke.CAP_BUTT ? 0f : wpad;
                } else if (y1 == y2 && x1 != x2) {
                    xpad = wpad;
                    ypad = bs.getEndCap() == BasicStroke.CAP_BUTT ? 0f : wpad;
                } else {
                    if (bs.getEndCap() == BasicStroke.CAP_SQUARE) {
                        wpad *= Math.sqrt(2);
                    }
                    xpad = ypad = wpad;
                }
                if (x1 > x2) { float t = x1; x1 = x2; x2 = t; }
                if (y1 > y2) { float t = y1; y1 = y2; y2 = t; }
                return new Rectangle2D.Float(x1-xpad, y1-ypad,
                                             x2+xpad*2f, y2+ypad*2f);
            }
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len == 0) {
                dx = wpad;
                dy = 0;
            } else {
                dx = wpad * dx / len;
                dy = wpad * dy / len;
            }
            float ecx, ecy;
            if (bs.getEndCap() != BasicStroke.CAP_BUTT) {
                // TODO - CAP_ROUND would not require as much padding
                // but how much less would depend on analyzing the transform
                ecx = dx;
                ecy = dy;
            } else {
                ecx = ecy = 0;
            }
            float corners[] = {
                x1-dy-ecx, y1+dx-ecy,
                x1+dy-ecx, y1-dx-ecy,
                x2+dy+ecx, y2-dx+ecy,
                x2-dy+ecx, y2+dx+ecy,
            };
            at.transform(corners, 0, corners, 0, 4);
            x1 = Math.min(Math.min(corners[0], corners[2]),
                                  Math.min(corners[4], corners[6]));
            y1 = Math.min(Math.min(corners[1], corners[3]),
                                  Math.min(corners[5], corners[7]));
            x2 = Math.max(Math.max(corners[0], corners[2]),
                                  Math.max(corners[4], corners[6]));
            y2 = Math.max(Math.max(corners[1], corners[3]),
                                  Math.max(corners[5], corners[7]));
            x1 -= 0.5f;
            y1 -= 0.5f;
            x2 += 0.5f;
            y2 += 0.5f;
            return new Rectangle2D.Float(x1, y1, x2-x1, y2-y1);
        }
        return null;
    }

    private Rectangle2D getBounds(AffineTransform at, RoundRectangle2D rr) {
        float upad, dpad;
        int atclass = classify(at);
        if (mode == Mode.FILL) {
            upad = dpad = 0;
        } else {
            if (drawStroke instanceof BasicStroke) {
                BasicStroke bs = (BasicStroke) drawStroke;
                upad = bs.getLineWidth() / 2f;
                if (bs.getLineJoin() == BasicStroke.JOIN_MITER) {
                    // There are a couple of cases where MITER can affect us.
                    // The most extreme case is when the roundrect is very
                    // thin in one dimension causing the arcs to meet and to
                    // also be clamped to very thin dimensions - this can
                    // cause a MITER to shoot out off the thin ends out to
                    // miterLimit.
                    // The other more minor case is when the arc sizes are
                    // very small or 0 and the roundrectangle becomes
                    // essentially a rectangle - in that case the sharp
                    // corner of the rectangle can shoot out by a factor
                    // of sqrt(2).
                    // TODO - only pad for sqrt(2) if the arcSizes are very
                    // small
                    // TODO - Miter limit is an extreme amount of padding
                    // and only matters for extremely thin roundrects
                    upad *= bs.getMiterLimit();
                }
                dpad = 0.5f;
            } else {
                return null;
            }
        }
        // TODO - we can shave off the corners if the arcWidth and arcHeight
        // are large enough by examining the transform
        return getBounds(at, atclass, upad, dpad, rr);
    }

    private Rectangle2D getBounds(AffineTransform at, Ellipse2D e) {
        float upad, dpad;
        if (mode == Mode.FILL) {
            upad = dpad = 0;
        } else {
            if (drawStroke instanceof BasicStroke) {
                BasicStroke bs = (BasicStroke) drawStroke;
                upad = bs.getLineWidth() / 2f;
                if (bs.getLineJoin() == BasicStroke.JOIN_MITER) {
                    // Miter limit is an extreme amount of padding
                    // and only matters for extremely thin ovals
                    if (e.getWidth() * 10 < e.getHeight() ||
                        e.getHeight() * 10 < e.getWidth())
                    {
                        upad *= bs.getMiterLimit();
                    }
                }
                dpad = 0.5f;
            } else {
                return null;
            }
        }
        // TODO - we can shave off the corners by examining the transform
        // and the aspect ratio of the ellipse
        return getBounds(at, classify(at), upad, dpad, e);
    }

    private Rectangle2D getBounds(AffineTransform at, Rectangle2D r) {
        float upad, dpad;
        int atclass = classify(at);
        if (mode == Mode.FILL) {
            upad = dpad = 0;
        } else {
            if (drawStroke instanceof BasicStroke) {
                BasicStroke bs = (BasicStroke) drawStroke;
                upad = bs.getLineWidth() / 2f;
                if (bs.getLineJoin() == BasicStroke.JOIN_MITER && atclass > AT_TRANS) {
                    upad *= Math.sqrt(2);
                }
                dpad = 0.5f;
            } else {
                return null;
            }
        }
        return getBounds(at, atclass, upad, dpad, r);
    }
    
    private Rectangle2D getBounds(AffineTransform at, int atclass,
                                  float upad, float dpad,
                                  RectangularShape r)
    {
        float x1 = (float) r.getWidth();
        float y1 = (float) r.getHeight();
        if (x1 < 0 || y1 < 0) {
            return new Rectangle2D.Float(0, 0, -1, -1);
        }
        float x0 = (float) r.getX();
        float y0 = (float) r.getY();
        x1 += x0;
        y1 += y0;
        if (atclass <= AT_TRANS) {
            if (atclass == AT_TRANS) {
                float tx = (float) at.getTranslateX();
                float ty = (float) at.getTranslateY();
                x0 += tx;
                y0 += ty;
                x1 += tx;
                y1 += ty;
            }
            // TODO - only pad by upad or dpad, depending on transform
            dpad += upad;
        } else {
            // TODO - only pad by upad or dpad, depending on transform
            x0 -= upad;
            y0 -= upad;
            x1 += upad;
            y1 += upad;
            // TODO - only transform 2 corners if quadrant rotation
            double corners[] = { x0, y0, x1, y0, x1, y1, x0, y1 };
            at.transform(corners, 0, corners, 0, 4);
            x0 = (float) Math.min(Math.min(corners[0], corners[2]),
                                  Math.min(corners[4], corners[6]));
            y0 = (float) Math.min(Math.min(corners[1], corners[3]),
                                  Math.min(corners[5], corners[7]));
            x1 = (float) Math.max(Math.max(corners[0], corners[2]),
                                  Math.max(corners[4], corners[6]));
            y1 = (float) Math.max(Math.max(corners[1], corners[3]),
                                  Math.max(corners[5], corners[7]));
        }
        x0 -= dpad;
        y0 -= dpad;
        x1 += dpad;
        y1 += dpad;
        return new Rectangle2D.Float(x0, y0, x1-x0, y1-y0);
    }

    @Override 
    public final Rectangle2D getBounds(AffineTransform at) {
        Shape s = getShape();
        if (s == null) {
            // REMIND: Use -1 w/h or just plain 0,0,0,0?
            return new Rectangle2D.Float(0, 0, -1, -1);
        }
        Rectangle2D r;
        if (s instanceof Rectangle2D) {
            r = getBounds(at, (Rectangle2D) s);
        } else if (s instanceof Ellipse2D) {
            r = getBounds(at, (Ellipse2D) s);
        } else if (s instanceof RoundRectangle2D) {
            r = getBounds(at, (RoundRectangle2D) s);
        } else if (s instanceof Line2D) {
            r = getBounds(at, (Line2D) s);
        } else {
            // TODO - optimize for Arc2D
            r = null;
        }
        if (r != null) {
            return r;
        }
        boolean includeShape = (mode != Mode.STROKE);
        boolean includeStroke = (mode != Mode.FILL);
        float bbox[] = {
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
        };
        if (includeShape) {
            accumulate(bbox, s, at);
        }
        if (includeStroke) {
            if (cachedStrokeShape == null) {
                cachedStrokeShape = drawStroke.createStrokedShape(s);
            }
            accumulate(bbox, cachedStrokeShape, at);
            if (drawStroke instanceof BasicStroke) {
                // Account for "minimum pen size" by expanding by 0.5 device
                // pixels all around...
                bbox[0] -= 0.5f;
                bbox[1] -= 0.5f;
                bbox[2] += 0.5f;
                bbox[3] += 0.5f;
            }
        }
        if (bbox[2] < bbox[0] || bbox[3] < bbox[1]) {
            // They are probably +/-INFINITY which would yield NaN if subtracted
            // Let's just return a "safe" empty bbox...
            return new Rectangle2D.Float(0, 0, -1, -1);
        }
        return new Rectangle2D.Float(bbox[0], bbox[1],
                                     bbox[2]-bbox[0],
                                     bbox[3]-bbox[1]);
    }
    
    private static final int coordsPerSeg[] = { 2, 2, 4, 6, 0 };
    private void accumulate(float bbox[], Shape s, AffineTransform at) {
        if (at == null || at.isIdentity()) {
            // The shape itself will often have a more optimal algorithm
            // to calculate the untransformed bounds...
            Rectangle2D r2d = s.getBounds2D();
            if (bbox[0] > r2d.getMinX()) bbox[0] = (float) r2d.getMinX();
            if (bbox[1] > r2d.getMinY()) bbox[1] = (float) r2d.getMinY();
            if (bbox[2] < r2d.getMaxX()) bbox[2] = (float) r2d.getMaxX();
            if (bbox[3] < r2d.getMaxY()) bbox[3] = (float) r2d.getMaxY();
            return;
        }
        PathIterator pi = s.getPathIterator(at);
        float coords[] = new float[6];
        while (!pi.isDone()) {
            int numcoords = coordsPerSeg[pi.currentSegment(coords)];
            for (int i = 0; i < numcoords; i++) {
                float v = coords[i];
                int off = (i & 1); // 0 for X, 1 for Y coords
                if (bbox[off+0] > v) bbox[off+0] = v;
                if (bbox[off+2] < v) bbox[off+2] = v;
            }
            pi.next();
        }
    }

    @Override 
    public boolean contains(Point2D point) { 
        return (shape == null) ? false : shape.contains(point);
    }
}
