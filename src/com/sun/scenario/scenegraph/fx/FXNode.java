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

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.SwingConstants;

import com.sun.scenario.effect.Effect;
import com.sun.scenario.scenegraph.SGAlignment;
import com.sun.scenario.scenegraph.SGClip;
import com.sun.scenario.scenegraph.SGComposite;
import com.sun.scenario.scenegraph.SGEffect;
import com.sun.scenario.scenegraph.SGFilter;
import com.sun.scenario.scenegraph.SGNode;
import com.sun.scenario.scenegraph.SGRenderCache;
import com.sun.scenario.scenegraph.SGTransform;
import com.sun.scenario.scenegraph.SGWrapper;

/**
 * @author Chris Campbell
 */
public class FXNode extends SGWrapper {
    private SGNode rootNode;

    private SGAlignment alignmentNode;
    private SGTransform.Affine affineNode;
    private SGTransform.Translate translateNode;
    private SGClip clipNode;
    private SGComposite compositeNode;
    private SGRenderCache cacheNode;
    private SGEffect effectNode;
    private SGNode leafNode;

    public FXNode(SGNode leaf) {
        leafNode = leaf;
        
        updateTree();
    }
    
    private SGNode addFilter(SGFilter filter, SGNode child) {
        if (filter != null) {
            filter.setChild(child);
            child = filter;
        }
        return child;
    }

    private void updateTree() {
        SGNode root = leafNode;
        root = addFilter(effectNode, root);
        root = addFilter(cacheNode, root);
        root = addFilter(compositeNode, root);
        root = addFilter(clipNode, root);
        root = addFilter(affineNode, root);
        root = addFilter(translateNode, root);
        root = addFilter(alignmentNode, root);
        if (this.rootNode != root) {
            this.rootNode = root;
            initParent();
        }
    }
    
    public SGNode getLeaf() {
        return leafNode;
    }
    
    @Override
    protected SGNode getRoot() {
        return rootNode;
    }
    
    public float getOpacity() {
        return compositeNode == null ? 1f : compositeNode.getOpacity();
    }
    
    public void setOpacity(float opacity) {
        if (opacity == 1f) {
            compositeNode = null;
            updateTree();
        } else {
            if (compositeNode == null) {
                compositeNode = new SGComposite();
                updateTree();
            }
            compositeNode.setOpacity(opacity);
        }
    }
    
    public Effect getEffect() {
        return effectNode == null ? null : effectNode.getEffect();
    }
    
    public void setEffect(Effect effect) {
        if (effect == null) {
            effectNode = null;
            updateTree();
        } else {
            if (effectNode == null) {
                effectNode = new SGEffect();
                updateTree();
            }
            effectNode.setEffect(effect);
        }
    }
    
    public Shape getClip() {
        return clipNode == null ? null : clipNode.getShape();
    }

    public void setClip(Shape clip) {
        if (!isClipAntialiased() && clip == null) {
            if (clipNode != null) {
                clipNode = null;
                updateTree();
            }
            return;
        }
        if (clipNode == null) {
            clipNode = new SGClip();
            updateTree();
        }
        clipNode.setShape(clip);
    }
    
    public boolean isClipAntialiased() {
        return clipNode == null ? false : clipNode.isAntialiased();
    }
    
    public void setClipAntialiased(boolean aa) {
        if (getClip() == null && !aa) {
            if (clipNode != null) {
                clipNode = null;
                updateTree();
            }
            return;
        }
        if (clipNode == null) {
            clipNode = new SGClip();
            updateTree();
        }
        clipNode.setAntialiased(aa);
    }

    // Variables to store the decomposed transform chain attributes
    private static final ValPair zeroPair = new ValPair.Default(0, 0);
    private static final ValPair unitPair = new ValPair.Default(1, 1);
    private ValPair translation = zeroPair;
    private ValPair anchor = zeroPair;
    private double rotation;
    private ValPair scale = unitPair;
    private ValPair shear = zeroPair;
    private AffineTransform affine;

    // TODO: Have a non-default ValPair return its default instance
    // when it gets set back to the default values - a minor optimization
    // which might conserve memory when a pair of values is temporarily
    // used and then reset back to the default values...
    private static class ValPair {
        private double x;
        private double y;
        private ValPair defaultval;

        /**
         * This constructor is used to construct a default instance.
         */
        protected ValPair(double x, double y) {
            this.x = x;
            this.y = y;
            this.defaultval = this;
        }

        /**
         * This constructor is used to create a non-default instance from
         * a default instance.
         */
        protected ValPair(ValPair vp) {
            this.x = vp.x;
            this.y = vp.y;
            this.defaultval = vp;
        }

        public boolean isDefault() {
            return this.x == defaultval.x && this.y == defaultval.y;
        }

        public final double getX() {
            return x;
        }

        public ValPair setX(double x) {
            this.x = x;
            return this;
        }

        public final double getY() {
            return y;
        }

        public ValPair setY(double y) {
            this.y = y;
            return this;
        }

        public ValPair setFromPoint(Point2D p) {
            this.x = p.getX();
            this.y = p.getY();
            return this;
        }

        public Point2D getAsPoint() {
            return new Point2D.Double(x, y);
        }
        
        private static class Default extends ValPair {
            public Default(double x, double y) {
                super(x, y);
            }

            @Override
            public boolean isDefault() {
                return true;
            }

            private ValPair newInstance() {
                return new ValPair(this);
            }

            @Override
            public ValPair setX(double x) {
                return (getX() == x) ? this : newInstance().setX(x);
            }

            @Override
            public ValPair setY(double y) {
                return (getY() == y) ? this : newInstance().setY(y);
            }

            @Override
            public ValPair setFromPoint(Point2D p) {
                return setX(p.getX()).setY(p.getY());
            }
        }
    }

    public double getTranslateX() {
        return translation.getX();
    }

    public void setTranslateX(double tx) {
        translation = translation.setX(tx);
        updateTransformNode();
    }

    public double getTranslateY() {
        return translation.getY();
    }

    public void setTranslateY(double ty) {
        translation = translation.setY(ty);
        updateTransformNode();
    }

    public Point2D getTranslation() {
        return translation.getAsPoint();
    }

    public void setTranslation(Point2D p) {
        translation = translation.setFromPoint(p);
        updateTransformNode();
    }

    public double getAnchorX() {
        return anchor.getX();
    }

    public void setAnchorX(double ax) {
        anchor = anchor.setX(ax);
        updateTransformNode();
    }

    public double getAnchorY() {
        return anchor.getY();
    }

    public void setAnchorY(double ay) {
        anchor = anchor.setY(ay);
        updateTransformNode();
    }

    public Point2D getAnchor() {
        return anchor.getAsPoint();
    }

    public void setAnchor(Point2D p) {
        anchor = anchor.setFromPoint(p);
        updateTransformNode();
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rot) {
        this.rotation = rot;
        updateTransformNode();
    }

    public double getScaleX() {
        return scale.getX();
    }

    public void setScaleX(double sx) {
        scale = scale.setX(sx);
        updateTransformNode();
    }

    public double getScaleY() {
        return scale.getY();
    }

    public void setScaleY(double sy) {
        scale = scale.setY(sy);
        updateTransformNode();
    }

    public Point2D getScale() {
        return scale.getAsPoint();
    }

    public void setScale(Point2D p) {
        scale = scale.setFromPoint(p);
        updateTransformNode();
    }

    public double getShearX() {
        return shear.getX();
    }

    public void setShearX(double shx) {
        shear = shear.setX(shx);
        updateTransformNode();
    }

    public double getShearY() {
        return shear.getY();
    }

    public void setShearY(double shy) {
        shear = shear.setY(shy);
        updateTransformNode();
    }

    public Point2D getShear() {
        return shear.getAsPoint();
    }

    public void setShear(Point2D p) {
        shear = shear.setFromPoint(p);
        updateTransformNode();
    }

    public AffineTransform getTransform() {
        if (affine == null) {
            return new AffineTransform();
        } else {
            return new AffineTransform(affine);
        }
    }

    public void setTransform(AffineTransform transform) {
        if (transform == null || transform.isIdentity()) {
            if (affine == null) {
                return;
            }
            affine = null;
        } else {
            if (affine == null) {
                affine = new AffineTransform();
            }
            affine.setTransform(transform);
        }
        updateTransformNode();
    }

    public AffineTransform getCompositeTransform() {
        if (affineNode != null) {
            return affineNode.getAffine();
        }
        return AffineTransform.getTranslateInstance(translation.getX(),
                                                    translation.getY());
    }

    private static AffineTransform scratch = new AffineTransform();
    private void updateTransformNode() {
        if (rotation == 0 &&
            scale.isDefault() &&
            shear.isDefault() &&
            affine == null)
        {
            updateTranslate();
        } else {
            updateAffine();
        }
    }
    
    private void updateTranslate() {
        boolean update = affineNode != null;
        affineNode = null;
        if (translation.isDefault()) {
            update = update || translateNode != null;
            translateNode = null;
        } else {
            if (translateNode == null) {
                translateNode = SGTransform.createTranslation(0, 0, null);
                update = true;
            }
            translateNode.setTranslation(translation.getX(), translation.getY());
        }
        if (update) {
            updateTree();
        }
    }

    private void updateAffine() {
        boolean update = translateNode != null;
        translateNode = null;
        if (affineNode == null) {
            affineNode = SGTransform.createAffine(null, clipNode);
            update = true;
        }
        synchronized (scratch) {
            scratch.setToIdentity();
            if (!translation.isDefault()) {
                scratch.translate(translation.getX(), translation.getY());
            }
            if (!anchor.isDefault()) {
                scratch.translate(anchor.getX(), anchor.getY());
            }
            if (rotation != 0) {
                scratch.rotate(rotation);
            }
            if (!scale.isDefault()) {
                scratch.scale(scale.getX(), scale.getY());
            }
            if (!shear.isDefault()) {
                scratch.shear(shear.getX(), shear.getY());
            }
            if (!anchor.isDefault()) {
                scratch.translate(-anchor.getX(), -anchor.getY());
            }
            if (affine != null) {
                scratch.concatenate(affine);
            }
            affineNode.setAffine(scratch);
        }
        if (update) {
            updateTree();
        }
    }

    public int getHorizontalAlignment() {
        return alignmentNode == null
                ? SwingConstants.LEADING
                : alignmentNode.getHorizontalAlignment();
    }

    public void setHorizontalAlignment(int halign) {
        if (halign == SwingConstants.LEADING &&
            getVerticalAlignment() == SwingConstants.TOP)
        {
            if (alignmentNode != null) {
                alignmentNode = null;
                updateTree();
            }
            return;
        }
        if (alignmentNode == null) {
            alignmentNode = new SGAlignment();
            updateTree();
        }
        alignmentNode.setHorizontalAlignment(halign);
    }
    
    public int getVerticalAlignment() {
        return alignmentNode == null
                ? SwingConstants.TOP
                : alignmentNode.getVerticalAlignment();
    }
    
    public void setVerticalAlignment(int valign) {
        if (getHorizontalAlignment() == SwingConstants.LEADING &&
            valign == SwingConstants.TOP)
        {
            if (alignmentNode != null) {
                alignmentNode = null;
                updateTree();
            }
            return;
        }
        if (alignmentNode == null) {
            alignmentNode = new SGAlignment();
            updateTree();
        }
        alignmentNode.setVerticalAlignment(valign);
    }
    
    public boolean isCachedAsBitmap() {
        return cacheNode == null ? false : cacheNode.isEnabled();
    }
    
    public void setCachedAsBitmap(boolean cached) {
        if (cached == (cacheNode != null)) {
            return;
        }
        cacheNode = cached ? new SGRenderCache() : null;
        updateTree();
    }

    @Override
    public void remove(SGNode node) {
        //we never remove nodes from FXNode
    }
}
