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
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import com.sun.scenario.scenegraph.SGComponent.SGShell;

/**
 * RepaintManager to intercept repaint requests from embedded swing components.    
 * 
 * @author Igor Kushnirskiy
 */
class SGComponentRepaintManager extends RepaintManager {
        private RepaintManager delegate;
        private final static Rectangle NULL_RECTANGLE = new Rectangle();
        private final static Dimension NULL_DIMENSION = new Dimension();
        public SGComponentRepaintManager(RepaintManager repaintManager) {
            delegate = repaintManager;
        }
        
        void setDelegate(RepaintManager manager) {
            this.delegate = manager;
        }
        
        @Override
        public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
            final SGShell shell = 
                (SGShell) SwingUtilities.getAncestorOfClass(SGShell.class, c);
            if (shell != null) {
                Point p = 
                    SwingUtilities.convertPoint(c, new Point(x, y), shell);
                shell.repaint(p.x, p.y, w, h);
            } else if (delegate != null) {
                delegate.addDirtyRegion(c, x, y, w, h);
            }
        }
        
//        @Override // this method is since 1.6
//        public void addDirtyRegion(Applet applet, int x, int y, int w, int h) {
//            final SGShell shell = 
//                (SGShell) SwingUtilities.getAncestorOfClass(SGShell.class, applet);
//            if (shell != null) {
//                Point p = 
//                    SwingUtilities.convertPoint(applet, new Point(x, y), shell);
//                shell.repaint(p.x, p.y, w, h);
//            } else if (delegate != null) {
//                delegate.addDirtyRegion(applet, x, y, w, h);
//            }
//        }

        @Override
        public void addInvalidComponent(JComponent invalidComponent) {
            if (delegate == null 
                    || SwingUtilities.getAncestorOfClass(
                            SGShell.class, invalidComponent) != null) {
                //SGShell.invalidate method validates the sub-tree
                return;
            } else if (delegate != null) {
                delegate.addInvalidComponent(invalidComponent);
            }
        }

        @Override
        public Rectangle getDirtyRegion(JComponent component) {
            if (delegate != null) {
                return delegate.getDirtyRegion(component);
            } else {
                return NULL_RECTANGLE;
            }
        }

        @Override
        public Dimension getDoubleBufferMaximumSize() {
            if (delegate != null) {
                return delegate.getDoubleBufferMaximumSize();
            } else {
                assert false;
                return NULL_DIMENSION;
            }
        }

        @Override
        public Image getOffscreenBuffer(Component c, int proposedWidth,
                int proposedHeight) {
            if (delegate != null) {
                return delegate.getOffscreenBuffer(c, proposedWidth, proposedHeight);
            } else {
                assert false;
                return null;
            }
        }

        @Override
        public Image getVolatileOffscreenBuffer(Component c, int proposedWidth,
                int proposedHeight) {
            if (delegate != null) {
                return delegate.getVolatileOffscreenBuffer(c, proposedWidth,
                        proposedHeight);
            } else {
                assert false;
                return null;
            }
        }

        @Override
        public boolean isCompletelyDirty(JComponent component) {
            if (delegate != null) {
                return delegate.isCompletelyDirty(component);
            } else {
                assert false;
                return false;
            }
        }

        @Override
        public boolean isDoubleBufferingEnabled() {
            if (delegate != null) {
                return delegate.isDoubleBufferingEnabled();
            } else {
                assert false;
                return false;
            }
        }

        @Override
        public void markCompletelyClean(JComponent component) {
            if (delegate != null) {
                delegate.markCompletelyClean(component);
            }
        }

        @Override
        public void markCompletelyDirty(JComponent component) {
            final SGShell shell = 
                (SGShell) SwingUtilities.getAncestorOfClass(
                        SGShell.class, component);
            if (shell != null) {
                Point p = 
                    SwingUtilities.convertPoint(
                            component, new Point(0, 0), shell);
                shell.repaint(
                        p.x, p.y, component.getWidth(), component.getHeight());
                return;
              } else if (delegate != null) {
                  delegate.markCompletelyDirty(component);
              }
        }

        @Override
        public void paintDirtyRegions() {
            if (delegate != null) {
                delegate.paintDirtyRegions();
            } else {
                assert false;
            }
        }

        @Override
        public void removeInvalidComponent(JComponent component) {
            if (delegate != null) {
                delegate.removeInvalidComponent(component);
            } else {
                assert false;
            }
        }

        @Override
        public void setDoubleBufferingEnabled(boolean flag) {
            if (delegate != null) {
                delegate.setDoubleBufferingEnabled(flag);
            } else {
                assert false;
            }
        }

        @Override
        public void setDoubleBufferMaximumSize(Dimension d) {
            if (delegate != null) {
                delegate.setDoubleBufferMaximumSize(d);
            } else {
                assert false;
            }
        }

        @Override
        public void validateInvalidComponents() {
            if (delegate != null) {
                delegate.validateInvalidComponents();
            } else {
                assert false;
            }
        }
    }
