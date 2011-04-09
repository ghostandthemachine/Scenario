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
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;

/**
 * A scene graph node that renders an Image.
 * 
 * @author Chet Haase
 * @author Hans Muller
 */
public class SGImage extends SGLeaf {
    private Image image;
    private final Point2D.Float location = new Point2D.Float();
    private Object interpolationHint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
    private boolean smoothTranslation = false;
    private final ImageObserver observer = new SGImageObserver();

    public final Image getImage() { 
        return image;
    }

    public void setImage(Image image) {
        boolean boundsChanged;
        if (this.image != null && image != null) {
            boundsChanged =
                (image.getWidth(null) != this.image.getWidth(null)) ||
                (image.getHeight(null) != this.image.getHeight(null));
        } else {
            boundsChanged = true;
        }
        this.image = image;
        repaint(boundsChanged);
    }

    /**
     * Defines the image's bound's origin.  Assuming the transform is null,
     * this is the location in the parent node where the image will be drawn.
     * If return value {@code rv} is non-null, it will be set and returned.
     * Otherwise a new Point will be allocated and returned.
     * 
     * @param rv the return value or null
     * @return the location where the image will be drawn
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

    /**
     * Returns the {@code KEY_INTERPOLATION} rendering hint.
     * The {@code hint} will be
     * one of: {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, 
     * {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR}, 
     * {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC}.
     * 
     * @return the {@code KEY_INTERPOLATION} hint
     * @see java.awt.RenderingHints
     */
    public final Object getInterpolationHint() {
        return interpolationHint;
    }

    /**
     * Sets the {@code KEY_INTERPOLATION} rendering hint. The {@code hint} must be
     * one of: {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, 
     * {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR}, 
     * {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC}.
     * The default is {@code VALUE_INTERPOLATION_BILINEAR}.
     * 
     * @see java.awt.RenderingHints
     * @see java.awt.Graphics2D
     */
    public void setInterpolationHint(Object hint) {
        if (!RenderingHints.KEY_INTERPOLATION.isCompatibleValue(hint)) {
            // Note that KEY_INTERPOLATION.isCompatibleValue also rejects null
            throw new IllegalArgumentException("invalid hint");
        }
        interpolationHint = hint;
        repaint(false);
    }
    
    /**
     * Returns whether the interpolation hint will be honored for
     * non-integral translations.
     * 
     * @return true if the interpolation hint should be honored for
     * non-integral translations; false otherwise
     */
    public final boolean getSmoothTranslation() {
        return smoothTranslation;
    }
    
    /**
     * Sets whether the interpolation hint will be honored for
     * non-integral translations.  The default is false.  Setting
     * this to true may improve visual quality of non-integral translations,
     * but may negatively impact performance on some systems.
     * <p>
     * This setting only has an impact if the current interpolation hint
     * is either
     * {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR} or
     * {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC}.
     * 
     * @param smooth if true, the interpolation hint will be honored
     * for non-integral translations
     */
    public void setSmoothTranslation(boolean smooth) {
        this.smoothTranslation = smooth;
        repaint(false);
    }

    @Override
    public void paint(Graphics2D g) {
        if (image != null) {
            g.translate(location.getX(), location.getY());
            Object hint = interpolationHint;
            if (!smoothTranslation &&
                hint != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            {
                // Workaround for JDK bug 6570870: force NEAREST_NEIGHBOR
                // for non-integral translations to avoid slow paths
                AffineTransform xform = g.getTransform();
                if (xform.isIdentity() ||
                    xform.getType() == AffineTransform.TYPE_TRANSLATION)
                {
                    hint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
                }
            }
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            if (DO_PAINT) {
                g.drawImage(image, 0, 0, observer);
            }
        }
    }

    private class SGImageObserver implements ImageObserver {
        @Override
		public boolean imageUpdate(Image img, int infoflags,
                                   int x, int y, int w, int h)
        {
            boolean ret = false;
            if (img == image && isVisible()) {
                if ((infoflags & (FRAMEBITS | ALLBITS | SOMEBITS)) != 0) {
                    markDirty(false);
                }
                ret = (infoflags & (ALLBITS | ABORT | ERROR)) == 0;
            }
            return ret;
        }
    }

    @Override
    public final Rectangle2D getBounds(AffineTransform transform) {
        if (image == null) {
            return new Rectangle2D.Float();
        }
        float x = location.x;
        float y = location.y;
        float w = image.getWidth(null);
        float h = image.getHeight(null);
        if (transform != null && !transform.isIdentity()) {
            if (transform.getShearX() == 0 && transform.getShearY() == 0) {
                // No rotations...
                if (transform.getScaleX() == 1 && transform.getScaleY() == 1) {
                    // just a translation...
                    x += transform.getTranslateX();
                    y += transform.getTranslateY();
                } else {
                    float coords[] = { x, y, x+w, y+h };
                    transform.transform(coords, 0, coords, 0, 2);
                    x = Math.min(coords[0], coords[2]);
                    y = Math.min(coords[1], coords[3]);
                    w = Math.max(coords[0], coords[2]) - x;
                    h = Math.max(coords[1], coords[3]) - y;
                }
            } else {
                float coords[] = { x, y, x+w, y, x, y+h, x+w, y+h };
                transform.transform(coords, 0, coords, 0, 4);
                x = w = coords[0];
                y = h = coords[1];
                for (int i = 2; i < coords.length; i += 2) {
                    if (x > coords[i]) x = coords[i];
                    if (w < coords[i]) w = coords[i];
                    if (y > coords[i+1]) y = coords[i+1];
                    if (h < coords[i+1]) h = coords[i+1];
                }
                w -= x;
                h -= y;
            }
        }
        return new Rectangle2D.Float(x, y, w, h);
    }

    @Override
    public boolean hasOverlappingContents() {
        return false;
    }
}
