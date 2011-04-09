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
 *
 * @author Chet
 */
public abstract class Animation {
    
    static enum Status {
        SCHEDULED,
        SCHEDULEPAUSED,
        PAUSED,
        RUNNING,
        STOPPED,
        CANCELED
    };

    /**
     * Private utility to throw an exception if the animation is running.  This
     * is used by all of the property-setting methods to ensure that the
     * properties are not being changed mid-stream.
     */
    final void throwExceptionIfRunning() {
        if (isRunning()) {
            throw new IllegalStateException("Cannot perform this operation " +
                                            "while Clip is running");
        }
    }

    abstract void begin();
    
    abstract void end();
    
    abstract Status timePulse(long elapsedTime);

    /**
     * Starts this {@code Animation} running as soon as possible.
     */
    public void start() {
        startAt(MasterTimer.milliTime());
    }

    /**
     * Animates this {@code Animation} from its start state to
     * the state it would have after {@code t} milliseconds.
     */
    public void animateTo(long t) {
        RunQueue runq = new RunQueue();
        scheduleTo(0, t, runq);
        while (runq.next != null) {
            RunQueue cur = runq.next;
            runq.next = cur.next;
            cur.v.runTo(cur.t, t, runq);
        }
    }
    
    abstract void scheduleTo(long trel, long tend, RunQueue runq);

    /**
     * Starts this {@code Animation} running as close as possible to the
     * specified time as compared to MasterTimer.milliTime().
     */
    abstract void startAt(long when);

    /**
     * Returns {@code true} if this {@code Animation} is currently
     * running or on the queue to be run at a future time.
     */
    public abstract boolean isRunning();

    /**
     * Pauses this {@code Animation}.
     * It will remain paused until {@link #resume()} is called.
     */
    public abstract void pause();

    /**
     * Resumes this {@code Animation} if previously paused with the
     * {@link #pause} method.
     */
    public abstract void resume();

    /**
     * Stops this Timeline without firing off any dependent Animation
     * objects that were scheduled to start when this one ended.
     */
    public abstract void cancel();

    /**
     * Stops this Timeline and fires off any dependent Animation
     * objects that were scheduled to start when this one ended.
     */
    public abstract void stop();
}
