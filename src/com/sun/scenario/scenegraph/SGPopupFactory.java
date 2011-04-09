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

import java.awt.Component;
import java.awt.Point;
import java.awt.geom.AffineTransform;

import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;

import com.sun.scenario.scenegraph.SGComponent.SGShell;

/**
 * PopupFactory for embedded components. It creates SGComponent node for the
 * popup contents and adds it to the top most scene-graph's sceneGroup.
 * 
 * @author Igor Kushnirskiy
 */
class SGPopupFactory extends PopupFactory {
    static class SGPopup extends Popup {
        private final SGComponent sgComponent;
        private final SGTransform.Affine sgTransform ;
        private SGPopup(Component owner, Component contents, int x, int y) {
            sgComponent = new SGComponent();
            sgTransform = SGTransform.createAffine(null, sgComponent);
            
            sgComponent.setComponent(contents);
            AffineTransform accTransform = new AffineTransform();
            SGGroup topSGGroup = null;
            Point p = new Point(x, y);
            SGShell sgShell = (SGShell) SwingUtilities.getAncestorOfClass(
                    SGShell.class, owner);
            SwingUtilities.convertPointFromScreen(p, sgShell);
            while (sgShell != null) {
                SGLeaf leaf = ((SGShell) sgShell).getNode();
                accTransform.preConcatenate(
                  AffineTransform.getTranslateInstance(p.getX(), p.getY()));
                accTransform.preConcatenate(leaf.getCumulativeTransform());
                JSGPanel jsgPanel = leaf.getPanel();
                topSGGroup = jsgPanel.getSceneGroup();
                // check if it is an embedded JSGPanel
                SGShell parent = (SGShell) SwingUtilities.getAncestorOfClass(
                        SGShell.class, jsgPanel);
                if (parent != null) {
                    p = SwingUtilities.convertPoint(sgShell, 0, 0, parent);
                }
                sgShell = parent;
            }
            if (topSGGroup != null) {
                sgTransform.setAffine(accTransform);
                sgTransform.setVisible(false);
                topSGGroup.add(sgTransform);
            }
        }
        @Override
        public void show() {
            sgTransform.setVisible(true);
        }
        @Override
        public void hide() {
            SGParent parent = sgTransform.getParent();
            if (parent != null) {
                parent.remove(sgTransform);
            }
            sgComponent.setComponent(null);
        }
    }
    SGPopupFactory() {
        
    }
    @Override
    public Popup getPopup(Component owner,
            Component contents, int x, int y)
            throws IllegalArgumentException {
        if (SwingUtilities.getAncestorOfClass(SGShell.class, owner) != null) {
            SGPopup popup = new SGPopup(owner, contents, x, y);
            return popup;
        } else {
            return super.getPopup(owner, contents, x, y);
        }
    } 

}
