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

package com.sun.scenario.animation.util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;

import com.sun.scenario.animation.Clip;

/**
 * MouseTrigger handles mouse events
 * and triggers an animation based on those events.
 * For example, to have anim start when component receives an
 * ENTER event, one might write the following:
 * <pre>
 *     MouseTrigger trigger = 
 *         MouseTrigger.addTrigger(component, anim, MouseTriggerEvent.ENTER);
 * </pre>
 * 
 * 
 * 
 * @author Chet
 */
public class MouseTrigger extends Trigger implements MouseListener {
    
    /**
     * Creates a non-auto-reversing MouseTrigger and adds it as a 
     * listener to component.
     * 
     * 
     * @param component component that will generate MouseEvents for this
     * trigger
     * @param animator the AClipthat will start when the event occurs
     * @param event the MouseTriggerEvent that will cause the action to fire
     * @return MouseTrigger the resulting trigger
     */
    public static MouseTrigger addTrigger(JComponent component,
            Clip animator, MouseTriggerEvent event) {
        return addTrigger(component, animator, event, false);
    }
    
    /**
     * Creates a MouseTrigger and adds it as a listener to component.
     * 
     * 
     * @param component component that will generate MouseEvents for this
     * trigger
     * @param animator the AClipthat will start when the event occurs
     * @param event the FocusTriggerEvent that will cause the action to fire
     * @param autoReverse flag to determine whether the animator should
     * stop and reverse based on opposite triggerEvents.
     * @return FocusTrigger the resulting trigger
     */
    public static MouseTrigger addTrigger(JComponent component,
            Clip animator, MouseTriggerEvent event, boolean autoReverse) {
        MouseTrigger trigger = new MouseTrigger(animator, event, autoReverse);
        component.addMouseListener(trigger);
        return trigger;
    }
    
    /**
     * Creates a non-auto-reversing MouseTrigger, which should be added
     * to a Component that will generate the mouse events of interest
     */
    public MouseTrigger(Clip animator, MouseTriggerEvent event) {
        this(animator, event, false);
    }

    /**
     * Creates a MouseTrigger, which should be added
     * to a Component that will generate the mouse events of interest
     */
    public MouseTrigger(Clip animator,
            MouseTriggerEvent event, boolean autoReverse) {
        super(animator, event, autoReverse);
    }

    /**
     * Called by the object which added this trigger as a MouseListener.
     * This method starts the animator if the trigger is waiting for an
     * ENTER event.
     */
    @Override
	public void mouseEntered(MouseEvent e) {
        fire(MouseTriggerEvent.ENTER);
    }

    /**
     * Called by the object which added this trigger as a MouseListener.
     * This method starts the animator if the trigger is waiting for an
     * EXIT event.
     */
    @Override
	public void mouseExited(MouseEvent e) {
        fire(MouseTriggerEvent.EXIT);
    }

    /**
     * Called by the object which added this trigger as a MouseListener.
     * This method starts the animator if the trigger is waiting for a
     * PRESS event.
     */
    @Override
	public void mousePressed(MouseEvent e) {
        fire(MouseTriggerEvent.PRESS);
    }

    /**
     * Called by the object which added this trigger as a MouseListener.
     * This method starts the animator if the trigger is waiting for a
     * RELEASE event.
     */
    @Override
	public void mouseReleased(MouseEvent e) {
        fire(MouseTriggerEvent.RELEASE);
    }

    /**
     * Called by the object which added this trigger as a MouseListener.
     * This method starts the animator if the trigger is waiting for a
     * CLICK event.
     */
    @Override
	public void mouseClicked(MouseEvent e) {
        fire(MouseTriggerEvent.CLICK);
    }
    
}
