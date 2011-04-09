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
import java.awt.geom.Rectangle2D;

/**
 * A scene graph node that caches the rendering of a subtree in a
 * BufferedImage for performance.
 * The cached image will be recomputed if the subtree is changed, or
 * if the non-translation components of the cumulative transform of
 * the node tree change.
 * <p>
 * If a {@code SGRenderCache} node is translated by an integer number
 * of whole pixels then it does not need to be invalidated since the
 * rendering of the subtree would not change under such circumstances.
 * <p>
 * While the exact rendering of the subtree might be affected by a
 * non-integer translation factor, for many purposes the subtle
 * effects of the sub-pixel registration will not be important.
 * Thus, for performance reasons the cached image will not be
 * recomputed by default as a result of sub-pixel changes in the
 * translation factors unless the {@code pixelAccurate} attribute
 * is set to true.
 * <p>
 * The {@code SGRenderCache} node supports an {@code enabled} attribute
 * which controls whether or not it will cache the subtree.
 * This convenience attribute allows a scene graph to include these
 * nodes everywhere that caching might be desired and then only turn
 * the caches on under key performance conditions.
 * <p>
 * The {@code SGRenderCache} node also supports an {@code interpolationHint}
 * attribute that allows a scene graph to specify the scaling interpolation
 * used when rendering the cached image.
 * The default setting of {@code NEAREST_NEIGHBOR} is appropriate under most
 * circumstances where cached nodes do not move, or are translated only by
 * integer pixel amounts and is by far the fastest setting.
 * A {@code BILINEAR} setting can be used to mask many of the artifacts that
 * may occur when sub-pixel translation values are ignored at a much lower
 * cost than recomputing the cached image.
 * 
 * @see RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR
 * @see RenderingHints#VALUE_INTERPOLATION_BILINEAR
 * @see #setEnabled
 * @see #setSubpixelAccurate
 * @see #setInterpolationHint
 */
public class SGRenderCache extends SGFilter {
    public static SGRenderCache createCache(SGNode n) {
        SGRenderCache cache = new SGRenderCache();
        if (n != null) {
            cache.setChild(n);
        }
        return cache;
    }

    private Image cachedImage;
    private double cachedX;
    private double cachedY;
    private AffineTransform cachedXform;
    private boolean checkXform;

    private boolean enabled = true;
    private boolean subpixelaccurate;
    private Object filterHint =
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

    /**
     * Returns true if the caching of the subtree as an image is enabled.
     * 
     * @return true if caching is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the attribute which controls whether an image is used to
     * cache the rendering of the subtree.
     * The default value is true.
     * 
     * @param enabled true if caching is desired
     */
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            cachedImage = null;
            // enabling/disabling the node could theoretically change the way
            // it renders so we mark the node dirty to trigger a repaint.
            markDirty(false);
        }
    }

    /**
     * Returns true if sub-pixel translation factors invalidate the
     * cached image.
     * 
     * @return true if sub-pixel accuracy is being enforced
     */
    public boolean isSubpixelAccurate() {
        return subpixelaccurate;
    }

    /**
     * Sets the attribute that controls whether sub-pixel changes in the
     * translation components should invalidate the cached image.
     * The default value is false for best performance.
     * <p>
     * Setting this attribute to true will produce the highest quality of
     * rendering for the cached subtree at the expense of having to recompute
     * the cached image even for very subtle sub-pixel translations.
     * The value of this attribute will be irrelevant if the node is never
     * translated, or if it is always translated by only whole pixel distances.
     * 
     * @param accurate true if sub-pixel accuracy of the translation components
     *                      is important enough to invalidate the cached image
     */
    public void setSubpixelAccurate(boolean accurate) {
        if (this.subpixelaccurate != accurate) {
            this.subpixelaccurate = accurate;
            cachedImage = null;
            markDirty(false);
        }
    }

    /**
     * Returns the value for the {@link RenderingHints#KEY_INTERPOLATION}
     * hint being used when rendering the cached image in lieue of
     * rendering the subtree.
     * 
     * @return the hint used for cached image interpolation
     */
    public Object getInterpolationHint() {
        return filterHint;
    }

    /**
     * Sets the attribute which controls the type of image interpolation
     * to be used when rendering the cached image in lieue of rendering
     * the subtree directly.
     * The attribute must be one of
     * {@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     * {@link RenderingHints#VALUE_INTERPOLATION_BILINEAR}, or
     * {@link RenderingHints#VALUE_INTERPOLATION_BICUBIC}.
     * The default value is
     * {@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR}
     * for performance reasons.
     * 
     * @param hint the hint to be used for cached image interpolation
     */
    public void setInterpolationHint(Object hint) {
        if (filterHint != hint) {
            if (!RenderingHints.KEY_INTERPOLATION.isCompatibleValue(hint)) {
                // Note that KEY_INTERPOLATION.isCompatibleValue also rejects null
                throw new IllegalArgumentException("invalid hint");
            }
            filterHint = hint;
            markDirty(false);
        }
    }

    /**
     * This method comes down the tree so it may be due to a benign
     * transform change.  We need to check the transform before we
     * decide to invalidate the cached image...
     */
    @Override
    void invalidateAccumBounds() {
        super.invalidateAccumBounds();
        checkXform = true;
    }

    @Override
    void markDirty(int state) {
        super.markDirty(state);
        cachedImage = null;
    }

    /**
     * Check to see if the cached image needs to be invalidated due to
     * an incompatible change in the accumulated transform.  This operation
     * is performed lazily by the needsSourceContent() method after any
     * accumulated transform changes.
     */
    private void checkAccumTransform() {
        AffineTransform at = getCumulativeTransform();
        if (cachedXform == null ||
            at.getScaleX() != cachedXform.getScaleX() ||
            at.getScaleY() != cachedXform.getScaleY() ||
            at.getShearX() != cachedXform.getShearX() ||
            at.getShearY() != cachedXform.getShearY())
        {
            // REMIND: What about sub-pixel translates?
            cachedImage = null;
        } else if (subpixelaccurate) {
            if (fract(at.getTranslateX()) != fract(cachedXform.getTranslateX()) ||
                fract(at.getTranslateY()) != fract(cachedXform.getTranslateY()))
            {
                cachedImage = null;
            }
        }
    }

    private static double fract(double v) {
        return v - Math.floor(v);
    }

    @Override
    public int needsSourceContent() {
        if (enabled) {
            if (checkXform) {
                checkXform = false;
                checkAccumTransform();
            }
            if (cachedImage == null) {
                return TRANSFORMED;
            } else {
                return CACHED;
            }
        } else {
            return NONE;
        }
    }

    @Override
    public Rectangle2D getBounds(AffineTransform xform) {
        SGNode child = getChild();
        if (child == null) {
            // just an empty rectangle
            return new Rectangle2D.Float();
        }
        Rectangle2D cb = child.getBounds(xform);
        if (enabled) {
            // Pad the bounds by 1 pixel to account for sub-pixel jittering
            // when we try to reuse the same cached image when the sub-pixel
            // translation components change.
            // REMIND: Perhaps we should only do this if subpixelAccurate=false?
            cb.setRect(cb.getX()-1, cb.getY()-1, cb.getWidth()+2, cb.getHeight()+2);
        }
        return cb;
    }

    @Override
    Rectangle2D calculateAccumBounds() {
        return getBounds(getCumulativeTransform());
    }

    @Override
    public void renderFromCache(Graphics2D g) {
        AffineTransform at = g.getTransform();
        double x = at.getTranslateX()+cachedX;
        double y = at.getTranslateY()+cachedY;
        g.setTransform(AffineTransform.getTranslateInstance(x, y));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, filterHint);
        g.drawImage(cachedImage, 0, 0, null);
    }

    @Override
    public void renderFinalImage(Graphics2D g, SGSourceContent srcContent) {
        cachedImage = srcContent.getTransformedImage();
        Rectangle2D srcBounds = srcContent.getTransformedBounds();
        AffineTransform curTx = srcContent.getTransform();
        cachedXform = new AffineTransform(curTx);
        cachedX = srcBounds.getX() - curTx.getTranslateX();
        cachedY = srcBounds.getY() - curTx.getTranslateY();
        g.drawImage(cachedImage, 0, 0, null);
    }

    @Override
    boolean hasOverlappingContents() {
        return enabled ? false : super.hasOverlappingContents();
    }
}
