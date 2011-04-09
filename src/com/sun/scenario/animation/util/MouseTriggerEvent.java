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
 * Mouse Enter/Exit/Press/Release/Click events
 *
 * @author Chet
 */
public class MouseTriggerEvent extends TriggerEvent {
    /**
     * Event fired when mouse enters
     */
    public static final MouseTriggerEvent ENTER = 
            new MouseTriggerEvent("Entered");
    /**
     * Event fired when mouse exits
     */
    public static final MouseTriggerEvent EXIT = 
            new MouseTriggerEvent("Exit");
    /**
     * Event fired when mouse button is pressed
     */
    public static final MouseTriggerEvent PRESS = 
            new MouseTriggerEvent("Press");
    /**
     * Event fired when mouse button is released
     */
    public static final MouseTriggerEvent RELEASE = 
            new MouseTriggerEvent("Release");
    /**
     * Event fired when mouse is clicked
     */
    public static final MouseTriggerEvent CLICK = 
            new MouseTriggerEvent("Click");

    /**
     * Protected constructor; this helps ensure type-safe use of 
     * pre-define TriggerEvent objects.
     */
    private MouseTriggerEvent(String name) {
        super(name);
    }

    /**
     * This method finds the opposite of the current event.: <BR/>
     * ENTER -> EXIT <BR/>
     * EXIT -> ENTER <BR/>
     * PRESS -> RELEASE <BR/>
     * RELEASE -> PRESS <BR/>
     * Note that CLICK has no obvious opposite so
     * it simply returns CLICK (this method should probably not be called
     * for that case).
     * 
     */
    @Override
	public TriggerEvent getOppositeEvent() {
        if (this == MouseTriggerEvent.ENTER) {
            return MouseTriggerEvent.EXIT;
        } else if (this == MouseTriggerEvent.EXIT) {
            return MouseTriggerEvent.ENTER;
        } else if (this == MouseTriggerEvent.PRESS) {
            return MouseTriggerEvent.RELEASE;
        } else if (this == MouseTriggerEvent.RELEASE) {
            return MouseTriggerEvent.PRESS;
        }
        // Possible to reach here for REPEAT action (but probably should not
        // have been called with this event)
        return this;
    }
}
