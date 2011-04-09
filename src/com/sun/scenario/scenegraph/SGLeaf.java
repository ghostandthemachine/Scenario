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
import java.awt.geom.Rectangle2D;

import com.sun.scenario.settings.Settings;


/**
 * Base class for nodes that can paint and handle mouse/keyboard input.
 * 
 * @author Chet Haase
 * @author Hans Muller
 */

public abstract class SGLeaf extends SGNode {
    private Rectangle2D subregionBounds;

    final static boolean DO_PAINT;
    static {
        String pkg = SGLeaf.class.getPackage().getName();
        DO_PAINT = !Settings.getBoolean(pkg + ".skippaint");
    }

    public abstract void paint(Graphics2D g);
    
    /*
     * Dirty state/region management below...
     */
    
    /**
     * This method must be called whenever a change is made to a node that
     * affects its visual state.  If {@code boundsChanged} is true, it
     * indicates that there has also been a change in the overall bounds
     * of the node, and therefore both the former and current bounds of
     * the node need to be repainted.
     * <p>
     * Usage example (change in visual/bounds state):
     * <pre>
     *     public void setThickness(float thickness) {
     *         this.thickness = thickness;
     *         repaint(true);
     *     }
     * </pre>
     * <p>
     * Usage example (change in visual state only):
     * <pre>
     *     public void setColor(Color color) {
     *         this.color = color;
     *         repaint(false);
     *     }
     * </pre>
     *
     * @param boundsChanged if true, a change in the overall node bounds
     *     has been made; if false, only a change in the node's visual
     *     state has been made
     */
    protected final void repaint(boolean boundsChanged) {
        markDirty(boundsChanged);
    }

    /**
     * This method must be called whenever a change is made to a node that
     * affects only a subregion of its overall visual state.  Calling this
     * method is similar to calling {@code repaint(false)}, but potentially
     * more efficient in cases where only a small portion of this node is
     * changing at any given time.
     * <p>
     * Usage example:
     * <pre>
     *     public void setIndicatorEnabled(boolean b) {
     *         this.indicatorEnabled = indicatorEnabled;
     *         repaint(indicatorBounds);
     *     }
     * </pre>
     *
     * @param subregionBounds a rectangle representing the subregion (in
     *     the untransformed coordinate space of this leaf node) that
     *     needs to be repainted
     * @throws IllegalArgumentException if {@code subregionBounds} is null
     */
    protected final void repaint(Rectangle2D subregionBounds) {
        if (subregionBounds == null) {
            throw new IllegalArgumentException("subregion bounds must be non-null");
        }
        Rectangle2D oldBounds = this.subregionBounds;
        Rectangle2D newBounds = accumulate(oldBounds, subregionBounds, false);
        if (oldBounds == null && newBounds != null) {
            markSubregionDirty();
        }
        this.subregionBounds = newBounds;
    }
    
    final Rectangle2D getSubregionBounds() {
        return subregionBounds;
    }
    
    @Override
    void clearDirty() {
        super.clearDirty();
        this.subregionBounds = null;
    }
}
