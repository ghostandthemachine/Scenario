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

import java.util.List;

/**
 * Base class for nodes that contain one or more children.
 * 
 * @author Chris Campbell
 */
public abstract class SGParent extends SGNode {

    public abstract List<SGNode> getChildren();
    public abstract void remove(SGNode node);
    
    /*
     * Dirty state/region management below...
     */
    
    @Override
    void clearDirty() {
        if (!isDirty()) {
            // no need to visit descendents
            return;
        }
        
        // clear this node's dirty state
        super.clearDirty();
        
        // then do the same for all its children (and so on, down the tree)
        for (SGNode child : getChildren()) {
            child.clearDirty();
        }
    }
    
    // TODO: figure out a way to do this more lazily
    @Override
    void invalidateAccumBounds() {
        // invalidate this node's cached accumulated transform/bounds
        super.invalidateAccumBounds();
        
        // then do the same for all its children (and so on, down the tree)
        for (SGNode child : getChildren()) {
            child.invalidateAccumBounds();
        }
    }
}
