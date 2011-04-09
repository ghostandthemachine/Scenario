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

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.EventListener;

import com.sun.scenario.scenegraph.SGNode;

/**
 * @see SGMouseAdapter
 * @author Hans Muller
 */
public interface SGMouseListener extends EventListener {
    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a node.
     */
    public void mouseClicked(MouseEvent e, SGNode node);

    /**
     * Invoked when a mouse button has been pressed on a node.
     */
    public void mousePressed(MouseEvent e, SGNode node);

    /**
     * Invoked when a mouse button has been released on a node.
     */
    public void mouseReleased(MouseEvent e, SGNode node);

    /**
     * Invoked when the mouse enters a node.
     */
    public void mouseEntered(MouseEvent e, SGNode node);

    /**
     * Invoked when the mouse exits a node.
     */
    public void mouseExited(MouseEvent e, SGNode node);
    
    /**
     * Invoked when a mouse button is pressed on a node and then
     * dragged.  <code>MOUSE_DRAGGED</code> events will continue to be
     * delivered to the node where the drag originated until the
     * mouse button is released (regardless of whether the mouse position
     * is within the bounds of the node).
     * <p>
     * Due to platform-dependent Drag&Drop implementations,
     * <code>MOUSE_DRAGGED</code> events may not be delivered during a native
     * Drag&Drop operation.
     */
    public void mouseDragged(MouseEvent e, SGNode node);
    
    /**
     * Invoked when the mouse cursor has been moved onto a node
     * but no buttons have been pushed.
     */
    public void mouseMoved(MouseEvent e, SGNode node);
    
    /**
     * Invoked when the mouse wheel is rotated.
     * @see MouseWheelEvent
     */
    public void mouseWheelMoved(MouseWheelEvent e, SGNode node);
}
