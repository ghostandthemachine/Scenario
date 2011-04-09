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
import java.awt.Composite;
import java.awt.Graphics2D;

/**
 * @author Chris Campbell
 */
public class SGComposite extends SGFilter {
    
    public enum Mode { SRC_OVER }

    /**
     * Specifies the behavior to be used to optimize the rendering
     * of descendants of {@link SGComposite} nodes which may contain
     * multiple overlapping elements.
     * <p>
     * The {@code SGComposite} filter node is used to make an entire
     * sub-tree of a scene translucent so that parts of the scene
     * underneath those children become partially visible through the
     * children.
     * Care needs to be taken, though, if the children overlap each
     * other, or if elements of the children overlap themselves or
     * other children as in the case of the stroke and fill of an
     * {@link SGShape} or {@link SGText} node.
     * <p>
     * Since overlapping elements of the children would not be visible
     * through each other when the descendants are rendered fully
     * opaque, it may or may not be desireable for those overlapping
     * elements within the children to be visible through each other
     * when the sub-tree is made translucent.
     * <p>
     * Rendering the descendants in the regular recursive manner by which
     * opaque scenes are rendered, but with translucency applied, would
     * result in overlapping parts of the children showing through each
     * other.
     * Rendering the descendants together as a group (typically using an
     * intermediate image in the process) and then compositing them as
     * a collection on top of the underlying parts of the rest of the
     * scene will make the descendants behave more like a self-contained
     * sub-scene and may provide the least surprising results, but can
     * take longer to render.
     * The {@code OverlapBehavior} setting can be used to customize the
     * effect for either visual preference, or for performance considerations.
     * <p>
     * The default behavior is {@code AUTOMATIC}.
     */
    public enum OverlapBehavior {
        /**
         * The descendent tree is scanned to determine if there are
         * multiple overlapping graphical elements present and the
         * appropriate behavior (generally {@link #FLATTEN} for the
         * overlapping case or {@link #LAYER} for the non-overlapping
         * case) is used as needed on the fly for the best blend of
         * quality and performance.
         * <p>
         * This behavior produces the least surprising output in the
         * safest and quickest manner and so is the default behavior for
         * {@link SGComposite} nodes.
         * In most cases the scan can discover what it needs to know to
         * make the determination from a fairly shallow scan into the
         * descendant nodes.
         */
        AUTOMATIC,

        /**
         * The sub-tree is always rendered as a single flattened visual
         * collective using an intermediate image or similar technique
         * to prevent bleed-through.
         * <p>
         * While the {@link #AUTOMATIC} behavior will use this behavior
         * as needed, this behavior can be selected manually to avoid the
         * render-time cost of scanning the sub-tree in cases where the
         * tree has a relatively static configuration that is known to
         * contain overlapping sub-elements that require this treatment.
         */
        FLATTEN,

        /**
         * The composite mode of the graphics is modified as indicated
         * and the sub-tree is rendered in the regular recursive manner,
         * allowing overlapping layers of the descendants to show through
         * each other.
         * <p>
         * While the {@link #AUTOMATIC} behavior can easily and safely
         * determine whether there might be any visible difference in
         * using this behavior, this behavior can be selected manually
         * either to avoid the render-time cost associated with scanning
         * the tree or constructing and using an intermediate image, or
         * in cases where the bleed-through effect that may occur with
         * overlapping descendants is a desired visual style.
         */
        LAYER
    }

    private float opacity = 1f;
    private Mode mode = Mode.SRC_OVER;
    private OverlapBehavior overlapbehavior = OverlapBehavior.AUTOMATIC;

    public SGComposite() {
    }
    
    public SGComposite(float opacity, SGNode child) {
        setOpacity(opacity);
        setChild(child);
    }
    
    public float getOpacity() {
        return opacity;
    }
    
    public void setOpacity(float opacity) {
        this.opacity = opacity;
        markDirty(false);
    }
    
    public Mode getMode() {
        return mode;
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
        markDirty(false);
    }

    /**
     * Gets the current {@code OverlapBehavior} to be
     * used to optimize rendering in the case of overlapping elements
     * in the sub-tree.
     * @return the current {@code OverlapBehavior}
     */
    public OverlapBehavior getOverlapBehavior() {
        return overlapbehavior;
    }

    /**
     * Sets the {@code OverlapBehavior} to be used to optimize rendering
     * in the case of overlapping elements in the sub-tree.
     * @param overlapbehavior the desired {@code OverlapBehavior}
     */
    public void setOverlapBehavior(OverlapBehavior overlapbehavior) {
        this.overlapbehavior = overlapbehavior;
        markDirty(false);
    }

    @Override
    public boolean canSkipRendering() {
        return (mode == Mode.SRC_OVER && opacity == 0f);
    }
    
    @Override
    public boolean canSkipChildren() {
        return canSkipRendering();
    }
    
    @Override
    public int needsSourceContent() {
        SGNode child = getChild();
        boolean needsSource;
        if (child == null) {
            needsSource = false;
        } else {
            if (opacity < 1f) {
                switch (overlapbehavior) {
                    case AUTOMATIC:
                        needsSource = child.hasOverlappingContents();
                        break;
                    case FLATTEN:
                        needsSource = true;
                        break;
                    case LAYER:
                        needsSource = false;
                        break;
                    default:
                        needsSource = true;
                        break;
                }
            } else {
                needsSource = false;
            }
        }
        return needsSource ? TRANSFORMED : NONE;
    }
    
    @Override
    public void setupRenderGraphics(Graphics2D g) {
        if (needsSourceContent() == NONE) {
            g.setComposite(makeComposite(g));
        }
    }
    
    @Override
    public void renderFinalImage(Graphics2D g, SGSourceContent srcContent) {
        if (opacity < 1f) {
            g.setComposite(makeComposite(g));
        }
        g.drawImage(srcContent.getTransformedImage(), 0, 0, null);
    }
    
    private Composite makeComposite(Graphics2D g) {
        int rule;
        switch (mode) {
        case SRC_OVER:
            rule = AlphaComposite.SRC_OVER;
            break;
        default:
            throw new InternalError("unknown Mode: "+mode);
        }
        AlphaComposite ac = (AlphaComposite) g.getComposite();
        if (ac.getRule() != rule) {
            throw new InternalError("mixed AlphaComposite modes");
        }
        float alpha = this.opacity * ac.getAlpha();
        return AlphaComposite.getInstance(rule, alpha);
    }

    @Override
    boolean hasOverlappingContents() {
        return (mode != Mode.SRC_OVER) ? true : super.hasOverlappingContents();
    }
}
