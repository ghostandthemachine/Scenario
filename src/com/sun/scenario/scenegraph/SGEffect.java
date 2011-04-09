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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.sun.scenario.effect.Effect;

/**
 * @author Chris Campbell
 */
public class SGEffect extends SGFilter {

    private final EffectListener listener = new EffectListener();
    private Effect effect;
    
    public SGEffect() {
    }
    
    private class EffectListener implements PropertyChangeListener {
        @Override
		public void propertyChange(PropertyChangeEvent evt) {
            markDirty(true);
        }
    }
    
    public Effect getEffect() {
        return effect;
    }
    
    public void setEffect(Effect effect) {
        if (this.effect != null) {
            this.effect.removePropertyChangeListener(listener);
        }
        this.effect = effect;
        if (this.effect != null) {
            this.effect.addPropertyChangeListener(listener);
        }
        markDirty(true);
    }
    
    @Override
    public Rectangle2D getBounds(AffineTransform xform) {
        SGNode child = getChild();
        if (child == null) {
            // just an empty rectangle
            return new Rectangle2D.Float();
        }
        Rectangle2D childXformBounds = child.getBounds(xform);
        if (effect == null) {
            return childXformBounds;
        }
        // TODO: this is a little weird...
        Rectangle2D childUnxformBounds = child.getBounds();
        SGSourceContent srcContent =
            new SGSourceContent(xform,
                                null, childUnxformBounds,
                                null, childXformBounds);
        effect.setSourceContent(srcContent);
        return effect.getBounds();
    }

    // TODO: sort out the whole calculateAccumBounds()/getBounds() mess...
    @Override
    Rectangle2D calculateAccumBounds() {
        return getBounds(getCumulativeTransform());
    }

    @Override
    public boolean canSkipRendering() {
        return (effect == null);
    }
    
    /**
     * Returns true if the bounds of this filter node are (potentially)
     * larger than the bounds of its child, false otherwise.
     * 
     * @return whether the bounds of this node expand outside the child bounds
     */
    @Override
    public boolean canExpandBounds() {
        // TODO: ask the Effect whether it expands the bounds
        return (effect != null);
    }
        
    @Override
    public int needsSourceContent() {
        int flags = effect.needsSourceContent();
        int retval = NONE; 
        if ((flags & Effect.TRANSFORMED) != 0)   retval |= TRANSFORMED;
        if ((flags & Effect.UNTRANSFORMED) != 0) retval |= UNTRANSFORMED;
        return retval;
    }

    @Override
    public void setupRenderGraphics(Graphics2D g) {
        // nothing to do here...
    }
    
    @Override
    public void renderFinalImage(Graphics2D g, SGSourceContent srcContent) {
        effect.setSourceContent(srcContent);
        Rectangle2D effectBounds = effect.getTransformedBounds();
        Rectangle2D childBounds = getChild().getBounds(srcContent.getTransform());
        int x = (int)(effectBounds.getX() - childBounds.getX());
        int y = (int)(effectBounds.getY() - childBounds.getY()); 
        effect.render(g, x, y, true);
        effect.setSourceContent(null);
    }

    @Override
    boolean hasOverlappingContents() {
        return (effect != null) ? false : super.hasOverlappingContents();
    }
}
