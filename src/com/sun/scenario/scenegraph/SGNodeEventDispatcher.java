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

import java.util.HashSet;
import java.util.Set;

/**
 * @author Chris Campbell
 */
class SGNodeEventDispatcher {

    private static Set<SGNode> pendingNodeEvents = new HashSet<SGNode>();

    static void addNodeEvent(SGNode node) {
        pendingNodeEvents.add(node);
    }
    
    static boolean hasPendingEvents() {
        return !pendingNodeEvents.isEmpty();
    }

    static void dispatchPendingNodeEvents() {
        if (pendingNodeEvents.isEmpty()) {
            // nothing to do...
            return;
        }
        
        // we will iterate over a local reference to the set of pending
        // node events, but we create a new set here so that any new events
        // triggered by listener code will be processed separately
        // (we will process those on the next recursive pass)
        Set<SGNode> pendingTemp = pendingNodeEvents;
        pendingNodeEvents = new HashSet<SGNode>();
        for (SGNode node : pendingTemp) {
            node.dispatchNodeEvent();
        }
        pendingTemp.clear();
        
        // now repeat the process in case other bounds changes were
        // triggered by the last pass
        dispatchPendingNodeEvents();
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private SGNodeEventDispatcher() {
    }
}
