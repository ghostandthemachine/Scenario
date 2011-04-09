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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;

import com.sun.scenario.animation.Clip;

/**
 * FocusTrigger handles focus events
 * and triggers an animation based on those events.
 * For example, to have anim start when component receives an 
 * IN event, one might write the following:
 * <pre>
 *     FocusTrigger trigger = 
 *         FocusTrigger.addTrigger(component, anim, FocusTriggerEvent.IN);
 * </pre>
 * 
 * 
 * 
 * @author Chet
 */
public class FocusTrigger extends Trigger implements FocusListener {

    /**
     * Creates a non-auto-reversing FocusTrigger and adds it as a FocusListener
     * to the component.
     * 
     * @param component component that will generate FocusEvents for this
     * trigger
     * @param animator the AnCliphat will start when the event occurs
     * @param event the FocusTriggerEvent that will cause the action to fire
     * @return FocusTrigger the resulting trigger
     */
    public static FocusTrigger addTrigger(JComponent component, 
            Clip animator, FocusTriggerEvent event) {
        return addTrigger(component, animator, event, false);
    }
    
    /**
     * Creates a FocusTrigger and adds it as a FocusListener
     * to the component.
     * 
     * 
     * @param component component that will generate FocusEvents for this
     * trigger
     * @param animator the AClipthat will start when the event occurs
     * @param event the FocusTriggerEvent that will cause the action to fire
     * @param autoReverse flag to determine whether the animator should
     * stop and reverse based on opposite triggerEvents.
     * @return FocusTrigger the resulting trigger
     */
    public static FocusTrigger addTrigger(JComponent component, 
            Clip animator, FocusTriggerEvent event, boolean autoReverse) {
        FocusTrigger trigger = new FocusTrigger(animator, event, autoReverse);
        component.addFocusListener(trigger);
        return trigger;
    }
    
    /**
     * Creates a non-auto-reversing FocusTrigger, which should be added
     * to a Component that will generate the focus events of interest.
     * 
     * @param animator the Clip that will start when the event occurs
     * @param event the FocusTriggerEvent that will cause the action to fire
     */
    public FocusTrigger(Clip animator, FocusTriggerEvent event) {
        this(animator, event, false);
    }

    /**
     * Creates a FocusTrigger, which should be added
     * to a Component that will generate the focus events of interest.
     * 
     * @param animator the Clip that will start when the event occurs
     * @param event the FocusTriggerEvent that will cause the action to fire
     * @param autoReverse flag to determine whether the animator should
     * stop and reverse based on opposite triggerEvents.
     */
    public FocusTrigger(Clip animator, FocusTriggerEvent event, 
            boolean autoReverse) {
        super(animator, event, autoReverse);
    }

    /**
     * Called by the object which added this trigger as a FocusListener.
     * This method starts the animator if the trigger is waiting for a 
     * IN event.
     */
    @Override
	public void focusGained(FocusEvent e) {
        fire(FocusTriggerEvent.IN);
    }

    /**
     * Called by the object which added this trigger as a FocusListener.
     * This method starts the animator if the trigger is waiting for a 
     * OUT event.
     */
    @Override
	public void focusLost(FocusEvent e) {
        fire(FocusTriggerEvent.OUT);
    }
    
}
