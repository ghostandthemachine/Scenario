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

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines a transformed coordinate system for a list of SGNodes.
 * 
 * @author Chet Haase
 * @author Hans Muller
 */
public class SGGroup extends SGParent {
    private List<SGNode> children;
    private List<SGNode> childrenUnmodifiable;

    @Override
	public final List<SGNode> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        else {
            if (childrenUnmodifiable == null) {
                childrenUnmodifiable = Collections.unmodifiableList(children);
            }
            return childrenUnmodifiable;
        }
    }

    public void add(int index, SGNode child) { 
        if (child == null) {
            throw new IllegalArgumentException("null child");
        }
        if (children == null) {
            children = new ArrayList<SGNode>(1); // common case: one child
        }
        if ((index < -1) || (index > children.size())) {
            throw new IndexOutOfBoundsException("invalid index");
        }
        
        SGParent oldParent = child.getParent();
        if (oldParent == this) {
            children.remove(child);
        } else if (oldParent != null) {
            oldParent.remove(child);
        }
        if (index == -1) {
            children.add(child);
        }
        else {
            children.add(index, child);
        }
        child.setParent(this);
        
        // mark the current bounds dirty (and force repaint of former
        // bounds as well)
        markDirty(true); 
        FocusHandler.addNotify(child);
    }
    
    public final void add(SGNode child) {
        add(-1, child);
    }

    @Override
    public void remove(SGNode child) {
        if (child == null) {
            throw new IllegalArgumentException("null child");
        }
        if (children != null) {
            FocusHandler.removeNotify(child);
            children.remove(child);
            child.setParent(null);
            
            // mark the current bounds dirty (and force repaint of former
            // bounds as well)
            markDirty(true);
            updateCursor();
        }
    }
    
    public final void remove(int index) {
        if (children != null) {
            SGNode child = children.get(index);
            if (child != null) {
                remove(child);
            }
        }
    }
    
    @Override
    public final Rectangle2D getBounds(AffineTransform transform) {
        Rectangle2D bounds = null;
        if (isVisible() && children != null && !children.isEmpty()) {
            // for now, just create the union of all the bounding boxes
            // of all the children; later, we may want to create something
            // more minimal, such as the overall convex hull, or a
            // Region/Area object containing only the actual child bounds
            for (int i = 0; i < children.size(); i++) {
                SGNode child = children.get(i);
                if (child.isVisible()) {
                    Rectangle2D rc = child.getBounds(transform);
                    bounds = accumulate(bounds, rc, true);
                }
            }
        }
        if (bounds == null) {
            // just an empty rectangle
            bounds = new Rectangle2D.Float();
        }
        return bounds;
    }

    @Override
    boolean hasOverlappingContents() {
        int n = (children == null ? 0 : children.size());
        if (n == 1) {
            return children.get(0).hasOverlappingContents();
        }
        return (n != 0);
    }
}
