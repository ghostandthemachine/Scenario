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

import java.awt.Container;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import com.sun.scenario.animation.FrameJob;
import com.sun.scenario.animation.Timeline;

/**
 * "RepaintManager" for all JSGPanels. repaints happen from bottom to the top. 
 * 
 * @author Igor Kushnirskiy
 */ 
class JSGPanelRepainter {
    Set<JSGPanel> dirtyPanels = new HashSet<JSGPanel>();

    private final FrameJob frameJob;
    
    private final static JSGPanelRepainter instance =
        new JSGPanelRepainter();
    static JSGPanelRepainter getJSGPanelRepainter() {
        return instance;
    }
    
    private JSGPanelRepainter() {
        frameJob = new FrameDisplay();
        Timeline.addFrameJob(frameJob);
    }
    
    void addDirtyPanel(JSGPanel panel) {
        dirtyPanels.add(panel);
        frameJob.wakeUp();
    }
    /**
     * Returns bottom-most panel from the set of dirtyPanels. The panel is 
     * bottom-most iff there is no dirty panel which has this one as an 
     * ancestor.
     */
    private JSGPanel getBottomPanel() {
        if (dirtyPanels.size() == 1) {
            return dirtyPanels.iterator().next();
        }
        //collect all parents
        Set<Container> parents = new HashSet<Container>();
        for (JSGPanel panel : dirtyPanels) {
            for (Container container = panel.getParent(); container != null;
                   container = container.getParent()) {
                if (parents.contains(container)) {
                    //we have all the ancestors of this container too
                    break;
                }
                parents.add(container);
            }
        }
        //find leaf dirtyPanel
        for (JSGPanel panel : dirtyPanels) {
            if (! parents.contains(panel)) {
                return panel;
            }
        }
        return null;
    }
    
    //trigger bottom-up painting
    void repaintAll() {
        while (dirtyPanels.size() > 0) {
            JSGPanel panel = getBottomPanel();
            dirtyPanels.remove(panel);
            //check if panel is topmost JSGPanel
            boolean immediately = 
                SwingUtilities.getAncestorOfClass(JSGPanel.class, panel) == null;
            panel.repaintDirtyRegions(immediately);
        }
    }
    
    private class FrameDisplay extends FrameJob {
        public void run() {
            repaintAll();
        }
    }
}
