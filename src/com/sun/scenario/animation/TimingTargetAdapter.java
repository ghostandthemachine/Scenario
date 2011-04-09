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

package com.sun.scenario.animation;

/**
 * Implements the {@link TimingTarget} interface, providing stubs for all
 * TimingTarget methods.  Subclasses may extend this adapter rather than
 * implementing the TimingTarget interface if they only care about a 
 * subset of the events that TimingTarget provides.  For example, 
 * sequencing animations may only require monitoring the 
 * {@link TimingTarget#end} method, so subclasses of this adapter
 * may ignore the other methods such as timingEvent.
 *
 * @author Chet
 */
public class TimingTargetAdapter implements TimingTarget {

    @Override
	public void timingEvent(float fraction, long totalElapsed) {}

    @Override
	public void begin() {}
    
    @Override
	public void end() {}
}
