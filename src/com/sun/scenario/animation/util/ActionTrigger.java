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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

import com.sun.scenario.animation.Clip;

/**
 * ActionTrigger handles action events and
 * starts the animator when actions occur.
 * For example, to have anim start when a button is clicked, 
 * one might write the following:
 * <pre>
 *     ActionTrigger trigger = ActionTrigger.addTrigger(button, anim);
 * </pre>
 *
 * @author Chet
 */
public class ActionTrigger extends Trigger implements ActionListener {
    
    /**
     * Creates an ActionTrigger and adds it as a listener to object.
     * 
     * @param object an object that will be used as an event source for
     * this trigger. This object must have the method addActionListener.
     * @param animator the AnCliphat start when the event occurs
     * @return ActionTrigger the resulting trigger
     * @throws IllegalArgumentException if object has no 
     * <code>addActionListener()</code>
     */
    public static ActionTrigger addTrigger(Object object, Clip animator) {
        ActionTrigger trigger = new ActionTrigger(animator);
        try {
            Method addListenerMethod = 
                    object.getClass().getMethod("addActionListener",
                    ActionListener.class);
            addListenerMethod.invoke(object, trigger);
        } catch (Exception e) {
            throw new IllegalArgumentException("Problem adding listener" +
                    " to object: " + e);
        }
        return trigger;
    }
    
    /**
     * Creates an ActionTrigger that will start the animator upon receiving
     * any ActionEvents. It should be added to any suitable object with
     * an addActionListener method.
     * 
     * @param animator the Clip that start when the event occurs
     */
    public ActionTrigger(Clip animator) {
        super(animator);
    }
    
    /**
     * Called by an object generating ActionEvents to which this
     * trigger was added as an ActionListener. This starts the Clip.
     */
    @Override
	public void actionPerformed(ActionEvent ae) {
        fire();
    }
    
}
