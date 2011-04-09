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
 * @author Chris Campbell
 */
public final class Timeline extends Animation {
    private Schedule targets;
    private boolean mightBeRunning;

    public Timeline() {
        targets = new Schedule();
    }

    /**
     * Returns true if there are no active or scheduled jobs.
     *
     * @return true if there are no active or scheduled jobs.
     */
    public static boolean isIdle() {
        return MasterTimer.isIdle();
    }

    /**
     * Adds a job to be processed after every frame of the animation.
     */
    public static void addFrameJob(FrameJob job) {
        MasterTimer.addFrameJob(job);
    }
    
    /**
     * Removes an existing job from the list of jobs processed after every
     * frame of the animation.
     */
    public static void removeFrameJob(FrameJob job) {
        MasterTimer.removeFrameJob(job);
    }

    /**
     * Schedules the specified {@link Animation} on the list of animations
     * for this {@code Timeline} at the start of the {@code Timeline}.
     * New clips may only be scheduled on this {@code Timeline} when it
     * is in the stopped state.
     * This is equivalent to calling {@link #schedule(Animation, long)
     * schedule(anim, 0)}.
     * 
     * @param anim the {@code Animation} to be added to this {@code Timeline}
     * @see #schedule(Animation, long)
     * @see #animations()
     */
    public void schedule(Animation anim) {
        schedule(anim, 0);
    }

    /**
     * Schedules the specified {@link Animation} on the list of animations
     * for this {@code Timeline} at the specified offset from its starting
     * point.
     * New clips may only be scheduled on this {@code Timeline} when it
     * is in the stopped state.
     * REMIND: Should we allow negative offsets?
     * 
     * @param anim the {@code Animation} to be added to this {@code Timeline}
     * @param offset number of milliseconds from the start time of the
     *               {@code Timeline} when this {@code Animation} should start.
     * @see #schedule(Animation)
     * @see #entries()
     */
    public synchronized void schedule(Animation anim, long offset) {
        throwExceptionIfRunning();
        targets.insert(anim, offset);
    }

    /**
     * Returns an iterable list of the {@link Animation} objects scheduled
     * on this {@code Timeline}.
     * The {@code Animation} objects are iterated directly from this
     * Iterable without any information about their scheduled start time
     * relative to the {@code Timeline}.
     * Use {@link #entries()} to iterate the animations along with
     * their scheduled start times.
     * The returned Iterable can be used directly in a foreach construct:
     * <pre>
     *     for (Animation a : timeline.animations()) {...}
     * </pre>
     * 
     * @return an Iterable useful for iterating the Animations
     * @see #entries()
     */
    public Iterable<Animation> animations() {
        return targets.animations(this);
    }

    /**
     * Returns an iterable list of the entries of animations on this
     * {@code Timeline}.
     * {@link ScheduledAnimation} instances are iterated from this
     * Iterable so that both the animation and the time at which it is
     * scheduled to start can be retrieved.
     * The Animation objects themselves can be iterated directly, without
     * start time information, using the {@link #animations()} method.
     * The returned Iterable can be used directly in a foreach construct:
     * <pre>
     *     for (ScheduledAnimation sa : timeline.entries()) {
     *         Animation a = sa.getAnimation();
     *         long startTime = sa.getTime();
     *     }
     * </pre>
     * 
     * @return an Iterable useful for iterating the scheduled animations
     * @see #animations()
     */
    public Iterable<? extends ScheduledAnimation> entries() {
        return targets.entries(this);
    }

    /**
     * Removes an entry for the specified {@link Animation} from the list
     * of animations for this {@code Timeline} and returns the time at
     * which it had been scheduled to start, relative to the start of
     * this {@code Timeline}.
     * New clips may only be removed from this Timeline when it is in the
     * stopped state.
     * 
     * @param anim the {@code Animation} to be removed from this
     *             {@code Timeline}
     * @return the time offset when the {@code Animation} was scheduled
     *         to start.
     */
    public synchronized long unschedule(Animation anim) {
        throwExceptionIfRunning();
        Schedule s = targets.remove(anim);
        return (s == null) ? -1L : s.getTime();
    }
    
    /**
     * Removes all animations from this {@code Timeline}.
     */
    public synchronized void unscheduleAll() {
        targets.clear();
    }
    
    @Override
    void begin() {}
    
    @Override
    void end() {}
    
    @Override
    Status timePulse(long elapsedTime) { return Status.CANCELED; }

    @Override
	void scheduleTo(long trel, long tend, RunQueue runq) {
        throwExceptionIfRunning();
        targets.transferUpTo(trel, tend, runq);
    }

    @Override
    synchronized void startAt(long when) {
        throwExceptionIfRunning();
        mightBeRunning = true;
        targets.scheduleRelativeTo(when);
    }

    /**
     * Indicates whether any of the clips on this timeline are currently
     * running.
     * This should only be used as a heuristic to applications because in
     * some circumstances the Timeline may be in the process of shutting
     * down and this method will still return true.
     */
    @Override
    public synchronized boolean isRunning() {
        if (mightBeRunning) {
            for (Schedule a : targets) {
                if (a.getAnimation().isRunning()) {
                    return true;
                }
            }
            mightBeRunning = false;
        }
        return false;
    }

    @Override
    public synchronized void pause() {
        for (Schedule a : targets) {
            a.getAnimation().pause();
        }
    }

    @Override
    public synchronized void resume() {
        for (Schedule a : targets) {
            a.getAnimation().resume();
        }
    }

    @Override
    public synchronized void cancel() {
        for (Schedule a : targets) {
            a.getAnimation().cancel();
        }
    }

    @Override
    public synchronized void stop() {
        for (Schedule a : targets) {
            a.getAnimation().stop();
        }
    }
}
