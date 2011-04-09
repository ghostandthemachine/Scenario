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

package com.sun.scenario.scenegraph.event;

import java.util.EventObject;

import com.sun.scenario.scenegraph.SGNode;

/**
 * An event that is generated when a change is made to the bounds of a node.
 *
 * @author Chris Campbell
 */
public class SGNodeEvent extends EventObject {
    
    /**
     * Private constructor.
     */
    private SGNodeEvent(SGNode node) {
        super(node);
    }
    
    /**
     * Creates and returns a new {@code SGNodeEvent} for the given
     * {@code SGNode} source.
     */
    public static SGNodeEvent createBoundsChangedEvent(SGNode node) {
        return new SGNodeEvent(node);
    }
    
    /**
     * Returns the {@code SGNode} that generated this event.
     *
     * @return the {@code SGNode} that generated this event
     */
    public SGNode getNode() {
        return (SGNode)getSource();
    }

    /**
     * Returns true if this event has been consumed; false otherwise.
     *
     * @return true if this event has been consumed; false otherwise
     */
    public boolean isConsumed() {
        // TODO: is this method needed?
        return false;
    }
}
