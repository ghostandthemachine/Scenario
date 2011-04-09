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

/**
 * Superclass for all TriggerEvents used in the Trigger classes.  The methods
 * here are mostly protected; it is expected that callers will not use this
 * class directly, but will instead use subclasses with pre-defined event
 * types. The purpose of this superclass is to provide the ability for 
 * {@link Trigger} to treat event types generically, rather than to have
 * all even logic in the subclasses of Trigger.
 *
 * @author Chet
 */
public class TriggerEvent {
    
    /**
     * The ID of events are simple strings.  It is expected that subclasses
     * will define static objects that callers will use instead of users
     * having to manually create TriggerEvent objects from strings directly
     */
    private String name;
    
    /**
     * Protected constructor; this helps ensure type-safe use of 
     * pre-define TriggerEvent objects.
     */
    protected TriggerEvent(String name) {
        this.name = name;
    }
        
    /**
     * This method returns the 'opposite' event from itself. This is used by
     * {@link Trigger} in running an auto-reversing animation, to determine 
     * whether an opposite event has occurred (and whether to stop/reverse
     * the animation).  Note that some events may have no opposite.
     * Default behavior returns same event; subclasses with multiple/opposite
     * events must override to do the right thing here.
     */
    public TriggerEvent getOppositeEvent() {
        return this;
    }
}
