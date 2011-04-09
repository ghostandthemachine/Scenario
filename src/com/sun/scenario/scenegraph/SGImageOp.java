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
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

/**
 * @author Chris Campbell
 */
public class SGImageOp extends SGFilter {
    
    private BufferedImageOp[] imageOps;
    
    public SGImageOp() {
    }
    
    public BufferedImageOp[] getImageOps() {
        return imageOps;
    }
    
    public void setImageOps(BufferedImageOp... imageOps) {
        this.imageOps = imageOps;
        markDirty(true);
    }
    
    @Override
    public boolean canSkipRendering() {
        return (imageOps == null);
    }
    
    /**
     * Returns true if the bounds of this filter node are (potentially)
     * larger than the bounds of its child, false otherwise.
     * 
     * @return whether the bounds of this node expand outside the child bounds
     */
    @Override
    public boolean canExpandBounds() {
        return (imageOps != null);
    }

    @Override
    public int needsSourceContent() {
        // we always need to render into an intermediate BufferedImage first
        return TRANSFORMED;
    }
    
    @Override
    public void setupRenderGraphics(Graphics2D g) {
        // nothing to do here...
    }
    
    @Override
    public void renderFinalImage(Graphics2D g, SGSourceContent srcContent) {
        BufferedImage src = (BufferedImage)srcContent.getTransformedImage();
        BufferedImage dst = null;

        // TODO: this is dumb for now (doesn't take op bounds into account);
        // this class will likely be removed in favor of SGEffect in the
        // near future anyway...
        for (int i = 0; i < imageOps.length; i++) {
            BufferedImageOp op = imageOps[i];
            src = op.filter(src, dst);
        }
        g.drawImage(src, 0, 0, null);
    }

    @Override
    boolean hasOverlappingContents() {
        return (imageOps != null) ? false : super.hasOverlappingContents();
    }
}
