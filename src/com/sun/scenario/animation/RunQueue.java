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

import com.sun.scenario.animation.Animation.Status;

final class RunQueue extends AnimationList<Clip, RunQueue>
    implements Iterable<RunQueue>
{
    private Status status;
    private long pauseTime;

    /** Creates a new instance of RunQueue */
    public RunQueue() {
    }
    
    private RunQueue(Clip clip, long tstart) {
        super(clip, tstart);
        status = Status.SCHEDULED;
    }

    @Override
	protected RunQueue makeEntry(Clip clip, long tstart) {
        return new RunQueue(clip, tstart);
    }
    
    final void adjustStartTime(long tdelta) {
        this.t += tdelta;
    }
    
    final Status getStatus() {
        return this.status;
    }

    /**
     * Set the appropriate status for the item so we can correctly
     * return to its prior status (SCHEDULED or RUNNING) and snapshot
     * the pause time to adjust its clock when it later resumes.
     * Do nothing if the item is already PAUSED, STOPPED, or CANCELED.
     *
     * @param tpause the time that the pause occured
     */
    final synchronized void pause(long tpause) {
        if (status == Status.SCHEDULED) {
            status = Status.SCHEDULEPAUSED;
        } else if (status == Status.RUNNING) {
            status = Status.PAUSED;
        } else {
            // STOPPED, CANCELED or already PAUSED
            return;
        }
        this.pauseTime = tpause;
    }

    /**
     * Set the appropriate status for an item that was paused back
     * to the state it was in before the pause and adjust the clock
     * to ignore the time period when it was paused.
     *
     * @param tresume the time that the resume occured
     */
    final synchronized void resume(long tresume) {
        Status newStatus;
        if (status == Status.SCHEDULEPAUSED) {
            newStatus = Status.SCHEDULED;
        } else if (status == Status.PAUSED) {
            newStatus = Status.RUNNING;
        } else {
            // Not PAUSED in any state
            return;
        }
        // Adjust time, then change state...
        adjustStartTime(tresume - pauseTime);
        this.status = newStatus;
    }

    /**
     * Set the appropriate new Status for this entry in the runList
     * when it reaches its start time.
     * Status is 99.9% likely to be SCHEDULED, but in case a
     * stop/cancel/pause happens at an inappropriate time, we
     * make sure the right new status is set.
     *
     * @return the new Status
     */
    final synchronized Status began() {
        // assert(status != Status.RUNNING && status != Status.PAUSED);
        if (status == Status.SCHEDULED) {
            status = Status.RUNNING;
        } else if (status == Status.SCHEDULEPAUSED) {
            status = Status.PAUSED;
        }
        // Else was STOPPED or CANCELED, leave status alone
        return status;
    }
}
