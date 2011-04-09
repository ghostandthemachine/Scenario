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

import com.sun.scenario.animation.Clip;


/**
 * This abstract class should be overridden by any class wanting to
 * implement a new Trigger.  The subclass will define the events to trigger
 * off of and any listeners to handle those events. That subclass will call
 * either {@link #fire()} or {@link #fire(TriggerEvent)} to start the
 * animator based on an event that occurred.
 * <p>
 * Subclasses should call one of the constructors in Trigger, according to
 * whether they want Trigger to discern between different TriggerEvents
 * and whether they want Trigger to auto-reverse the animation based on
 * opposite TriggerEvents.  
 * <p>
 * Subclasses should call one of the <code>fire</code> methods based on
 * whether they want Trigger to perform any event logic or simply start
 * the animation.
 *
 * @author Chet
 */
public abstract class Trigger {

    private boolean disarmed = false;
    private Clip animator;
    private TriggerEvent triggerEvent;
    private boolean autoReverse = false;

    /**
     * Creates a Trigger that will start the animator when {@link #fire()}
     * is called. Subclasses call this method to set up a simple Trigger
     * that will be started by calling {@link #fire()}, and will have
     * no dependency upon the specific {@link TriggerEvent} that must have
     * occurred to start the animator.
     * 
     * @param animator the Clip that will start when the Trigger
     * is fired
     */
    protected Trigger(Clip animator) {
        this(animator, null);
    }
    
    /**
     * Creates a Trigger that will start the animator when 
     * {@link #fire(TriggerEvent)} is called with an event that equals
     * triggerEvent.
     * 
     * @param animator the Clip that will start when the Trigger
     * is fired
     * @param triggerEvent the TriggerEvent that must occur for this
     * Trigger to fire
     */
    protected Trigger(Clip animator, TriggerEvent triggerEvent) {
        this(animator, triggerEvent, false);
    }
    
    /**
     * Creates a Trigger that will start the animator when 
     * {@link #fire(TriggerEvent)} is called with an event that equals
     * triggerEvent. Also, automatically stops and reverses animator when 
     * opposite event occurs, and stops reversing animator likewise
     * when triggerEvent occurs.
     * 
     * @param animator the Clip that will start when the Trigger
     * is fired
     * @param triggerEvent the TriggerEvent that must occur for this
     * Trigger to fire
     * @param autoReverse flag to determine whether the animator should
     * stop and reverse based on opposite triggerEvents.
     * @see TriggerEvent#getOppositeEvent()
     */
    protected Trigger(Clip animator, TriggerEvent triggerEvent,
            boolean autoReverse) {
        this.animator = animator;
        this.triggerEvent = triggerEvent;
        this.autoReverse = autoReverse;
    }
    
    /**
     * This method disables this Trigger and effectively noop's any actions
     * that would otherwise occur
     */
    public void disarm() {
        disarmed = true;
    }

    /**
     * Called by subclasses to start the animator if currentEvent equals
     * the event that the Trigger is based upon.  Also, if the Trigger is
     * set to autoReverse, stops and reverses the animator running in the
     * opposite direction as appropriate.
     * @param currentEvent the {@link TriggerEvent} that just occurred, which
     * will be compared with the TriggerEvent used to construct this Trigger
     * and determine whether the animator should be started or reversed
     */
    protected void fire(TriggerEvent currentEvent) {
        if (disarmed) {
            return;
        }
        if (currentEvent == triggerEvent) {
            // event occurred; fire the animation
            if (autoReverse) {
                // TODO: this probably isn't correct yet
                if (animator.getDirection() == Clip.Direction.BACKWARD) {
                    animator.setDirection(Clip.Direction.FORWARD);
                }
            }
            fire();
        } else if (triggerEvent != null && 
                currentEvent == triggerEvent.getOppositeEvent()) {
            // Opposite event occurred - run reverse anim if autoReverse
            if (autoReverse) {
                if (animator.getDirection() == Clip.Direction.FORWARD) {
                    animator.setDirection(Clip.Direction.BACKWARD);
                } else {
                    animator.setDirection(Clip.Direction.FORWARD);
                }
                fire();
            }
        }
    }
    
    /**
     * Utility method called by subclasses to start the animator.  This variant
     * assumes that there need be no check of the TriggerEvent that fired,
     * which is useful for subclasses with simple events.
     */
    protected void fire() {
        if (disarmed) {
            return;
        }
        if (animator.isRunning()) {
            animator.stop(); // TODO: is this still necessary?
        }
        animator.start();
    }
}
