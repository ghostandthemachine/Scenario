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
import java.util.Collections;
import java.util.List;

/**
 * Base class for nodes that maintain an internal graph of nodes.
 * 
 * @author Chris Campbell
 */
public abstract class SGWrapper extends SGParent {

    private List<SGNode> singletonList;
    
    protected abstract SGNode getRoot();
    
    protected void initParent() {
        // TODO: this is a hack; we could just make it the responsibility
        // of the subclass to do this, but SGNode.setParent() is package-private
        getRoot().setParent(this);
        markDirty(true);
    }
    
    @Override
	public List<SGNode> getChildren() {
        SGNode root = getRoot();
        if (root == null) {
            return Collections.emptyList();
        } else {
            if (singletonList == null || singletonList.get(0) != root) {
                singletonList = Collections.singletonList(root);
            }
            return singletonList;
        }
    }
    
    @Override
    public Rectangle2D getBounds(AffineTransform transform) {
        return getRoot().getBounds(transform);
    }

    @Override
    boolean hasOverlappingContents() {
        return getRoot().hasOverlappingContents();
    }
}
