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

import java.util.ArrayList;

/**
 * This class controls animations.  Its constructors and various
 * set methods control the parameters under which animations are run,
 * and the other methods support starting and stopping the animation.
 * The parameters of this class use the concepts of a "cycle" (the base
 * animation) and an "envelope" that controls how the cycle is started,
 * ended, and repeated.
 * <p>
 * Most of the methods here are simple getters/setters for the properties
 * used by Clip.  Typical animations will simply use one of the 
 * factory methods (depending on whether you are constructing a repeating
 * animation), optionally call any of the <code>set*</code> methods to alter
 * any of the other parameters, and then call start() to run the animation.
 * For example, this animation will run for 1 second, calling your
 * {@link TimingTarget} with timing events when the animation is started,
 * running, and stopped:
 * <pre>
 *  Clip clip = Clip.create(1000, property, startVal, endVal);
 *  clip.start();
 * </pre>
 * The following variation will run a half-second animation 4 times, 
 * reversing direction each time:
 * <pre>
 *  Clip clip = Clip.create(500, 4, property, startVal, endVal);
 *  clip.start();
 * </pre>
 * More complex animations can be created through using the properties
 * in Clip, such as {@link Clip#setInterpolator interpolation}.
 * Animations involving multiple animations that run at preset scheduled
 * times relative to each other can be collected into a {@link Timeline}
 * object and started, stopped, and paused all at once.
 */
public final class Clip extends Animation {

    // Clips may have multiple targets
    private ArrayList<TimingTarget> targets = new ArrayList<TimingTarget>();

    private Status desiredStatus = Status.STOPPED;  // Used for stop/cancel

    // Private variables to hold the internal "envelope" values that control
    // how the cycle is started, ended, and repeated.
    private float repeatCount = 1.0f;
    private RepeatBehavior repeatBehavior = RepeatBehavior.REVERSE;
    private EndBehavior endBehavior = EndBehavior.HOLD;
    
    // Private variables to hold the internal values of the base
    // animation (the cycle)
    private long duration;
    private int resolution = 20;    
    private Direction direction = Direction.FORWARD; // Direction of each cycle
    private Interpolator interpolator = Interpolators.getEasingInstance();

    /** The list of clips that are to begin when this clip begins. */
    private Schedule relBegin;
    /** The list of clips that are to begin when this clip ends. */
    private Schedule relEnd;

    /**
     * EndBehavior determines what happens at the end of the animation.
     * @see #setEndBehavior
     */
    public static enum EndBehavior {
        /** Timing sequence will maintain its final value at the end */
	HOLD,
        /** Timing sequence should reset to the initial value at the end */
	RESET,
    };

    /**
     * Direction is used to set the initial direction in which the
     * animation starts.
     * 
     * @see #setDirection
     */
    public static enum Direction {
        /** Cycle proceeds forward */
	FORWARD,
        /** Cycle proceeds backward */
	BACKWARD,
    };
    
    /**
     * RepeatBehavior determines how each successive cycle will flow.
     * @see #setRepeatBehavior
     */
    public static enum RepeatBehavior {
        /** 
         * Each repeated cycle proceeds in the same direction as the 
         * previous one 
         */
	LOOP,
        /** 
         * Each cycle proceeds in the opposite direction as the 
         * previous one
         */
	REVERSE
    };
    
    /**
     * Used to specify unending duration or repeatCount
     * @see #setDuration
     * @see #setRepeatCount
     * */
    public static final int INDEFINITE = -1;

    private void validateRepeatCount(float repeatCount) {
        if (repeatCount < 0 && repeatCount != INDEFINITE) {
            throw new IllegalArgumentException("repeatCount (" + repeatCount + 
                    ") cannot be < 0");
        }
    }

    /**
     * Returns a new {@code Clip} instance that drives the given target over
     * the specified duration.
     *
     * @param duration the duration of the animation, in milliseconds, or
     *     {@code Clip.INDEFINITE}
     * @param target the target of the animation (e.g. a {@code KeyFrames}
     *     instance)
     * @return a new {@code Clip} instance
     */
    public static Clip create(long duration, TimingTarget target) {
        return new Clip(duration, 1.0f, target);
    }

    /**
     * Returns a new {@code Clip} instance for the given duration and key values.
     *
     * @param duration the duration of the animation, in milliseconds, or
     *     {@code Clip.INDEFINITE}
     * @param property the target property of the animation
     * @param keyValues the set of key values in the animation
     * @return a new {@code Clip} instance
     */
    public static <T> Clip create(long duration,
                                  Property<T> property,
                                  T... keyValues)
    {
        KeyFrames<T> kf = KeyFrames.create(property, keyValues);
        return new Clip(duration, 1.0f, kf);
    }
    
    /**
     * Returns a new {@code Clip} instance for the given duration and key values.
     * <p>
     * This is a convenience method that is equivalent to calling:
     * <pre>
     *     Clip.create(duration, new BeanProperty&lt;T&gt;(target, property), keyValues);
     * </pre>
     *
     * @param duration the duration of the animation, in milliseconds, or
     *     {@code Clip.INDEFINITE}
     * @param target the target object of the animation
     * @param property the property on the target object to be animated
     * @param keyValues the set of key values in the animation
     * @return a new {@code Clip} instance
     */
    public static <T> Clip create(long duration,
                                  Object target, String property,
                                  T... keyValues)
    {
        BeanProperty<T> bp = new BeanProperty<T>(target, property);
        return create(duration, bp, keyValues);
    }
    
    /**
     * Returns a new {@code Clip} instance that drives the given target over
     * the specified duration and repeat count.
     * The {@code repeatCount} may be {@link #INDEFINITE} for animations that
     * repeat indefinitely, but must otherwise be >= 0.
     *
     * @param duration the duration of the animation, in milliseconds, or
     *     {@code Clip.INDEFINITE}
     * @param repeatCount the number of times to repeat the animation, or
     *     {@code Clip.INDEFINITE}
     * @param target the target of the animation (e.g. a {@code KeyFrames}
     *     instance)
     * @return a new {@code Clip} instance
     * @throws IllegalArgumentException if {@code repeatCount} is not
     *         INDEFINITE or >= 0
     */
    public static Clip create(long duration, float repeatCount,
                              TimingTarget target)
    {
        return new Clip(duration, repeatCount, target);
    }
    
    /**
     * Returns a new {@code Clip} instance for the given duration, repeat count,
     * and key values.
     * The {@code repeatCount} may be {@link #INDEFINITE} for animations that
     * repeat indefinitely, but must otherwise be >= 0.
     *
     * @param duration the duration of the animation, in milliseconds, or
     *     {@code Clip.INDEFINITE}
     * @param repeatCount the number of times to repeat the animation, or
     *     {@code Clip.INDEFINITE}
     * @param property the target property of the animation
     * @param keyValues the set of key values in the animation
     * @return a new {@code Clip} instance
     * @throws IllegalArgumentException if {@code repeatCount} is not
     *         INDEFINITE or >= 0
     */
    public static <T> Clip create(long duration, float repeatCount, 
                                  Property<T> property,
                                  T... keyValues)
    {
        KeyFrames<T> kf = KeyFrames.create(property, keyValues);
        return new Clip(duration, repeatCount, kf);
    }
    
    /**
     * Returns a new {@code Clip} instance for the given duration, repeat count,
     * and key values.
     * <p>
     * This is a convenience method that is equivalent to calling:
     * <pre>
     *     Clip.create(duration, repeatCount, new BeanProperty&lt;T&gt;(target, property), keyValues);
     * </pre>
     * The {@code repeatCount} may be {@link #INDEFINITE} for animations that
     * repeat indefinitely, but must otherwise be >= 0.
     *
     * @param duration the duration of the animation, in milliseconds, or
     *     {@code Clip.INDEFINITE}
     * @param repeatCount the number of times to repeat the animation, or
     *     {@code Clip.INDEFINITE}
     * @param target the target object of the animation
     * @param property the property on the target object to be animated
     * @param keyValues the set of key values in the animation
     * @return a new {@code Clip} instance
     * @throws IllegalArgumentException if {@code repeatCount} is not
     *         INDEFINITE or >= 0
     */
    public static <T> Clip create(long duration, float repeatCount,
                                  Object target, String property,
                                  T... keyValues)
    {
        BeanProperty<T> bp = new BeanProperty<T>(target, property);
        return create(duration, repeatCount, bp, keyValues);
    }
    
    /**
     * Private constructor.
     */
    private Clip(long duration, float repeatCount, TimingTarget target) {
        validateRepeatCount(repeatCount);
        this.duration = duration;
        this.repeatCount = repeatCount;
        addTarget(target);
    }

    /**
     * Returns the direction of this animation.
     * @return the direction of this animation
     */
    public Direction getDirection() {
        return direction;
    }
    
    /**
     * Sets the direction of this animation.  The default direction
     * is {@link Direction#FORWARD FORWARD}.
     * 
     * @param direction the direction of this animation
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }
    
    /**
     * Returns the interpolator for the animation.
     * @return interpolator that the initial animation cycle uses
     */
    public Interpolator getInterpolator() {
        return interpolator;
    }
    
    /**
     * Sets the interpolator for the animation cycle.  The default 
     * interpolator is {@link Interpolators#getEasingInstance()}.
     * @param interpolator the interpolation to use each animation cycle
     * @throws IllegalStateException if animation is already running; this
     * parameter may only be changed prior to starting the animation or 
     * after the animation has ended
     * @see #isRunning()
     */
    public void setInterpolator(Interpolator interpolator) {
        throwExceptionIfRunning();
        this.interpolator = interpolator;
    }
    
    /**
     * Adds a TimingTarget to the list of targets that get notified of each
     * timingEvent.  This can be done at any time before, during, or after the
     * animation has started or completed; the new target will begin
     * having its TimingTarget methods called as soon as it is added.
     * If <code>target</code> is already on the list of targets in this Clip, it
     * is not added again (there will be only one instance of any given
     * target in any Clip's list of targets).
     * 
     * @param target TimingTarget to be added to the list of targets that
     * get notified by this Clip of all timing events. Target cannot
     * be null.
     */
    public void addTarget(TimingTarget target) {
        synchronized (targets) {
            if (!targets.contains(target)) {
                targets.add(target);
            }
        }
    }
    
    /**
     * Removes the specified TimingTarget from the list of targets that get
     * notified of each timingEvent.  This can be done at any time before,
     * during, or after the animation has started or completed; the 
     * target will cease having its TimingTarget methods called as soon
     * as it is removed.
     * 
     * @param target TimingTarget to be removed from the list of targets that
     * get notified by this Clip of all timing events.
     */
    public void removeTarget(TimingTarget target) {
        synchronized (targets) {
            targets.remove(target);
        }
    }
    
    /**
     * Returns the current resolution of the animation. This helps 
     * determine the maximum frame rate at which the animation will run.
     * @return the resolution, in milliseconds, of the timer
     */
    public int getResolution() {
	return resolution;
    }
    
    /**
     * Sets the resolution of the animation
     * @param resolution the amount of time between timing events of the
     * animation, in milliseconds.  Note that the actual resolution may vary,
     * according to the resolution of the timer used by the framework as well
     * as system load and configuration; this value should be seen more as a
     * minimum resolution than a guaranteed resolution.
     * @throws IllegalArgumentException resolution must be >= 0
     * @throws IllegalStateException if animation is already running; this
     * parameter may only be changed prior to starting the animation or 
     * after the animation has ended
     * @see #isRunning()
     */
    public void setResolution(int resolution) {
        if (resolution < 0) {
            throw new IllegalArgumentException("resolution must be >= 0");
        }
        throwExceptionIfRunning();
        this.resolution = resolution;
        //timer.setResolution(resolution);
    }
    
    /**
     * Returns the duration of the animation.
     * 
     * @return the length of the animation, in milliseconds. A
     * return value of -1 indicates an {@link #INDEFINITE} duration.
     */
    public long getDuration() {
	return duration;
    }
    
    /**
     * Sets the duration for the animation
     * 
     * @param duration the length of the animation, in milliseconds.  This
     * value can also be {@link #INDEFINITE}, meaning the animation will run
     * until manually stopped.
     * @see #isRunning()
     * @see #stop()
     * @throws IllegalStateException if animation is already running; this
     * parameter may only be changed prior to starting the animation or 
     * after the animation has ended
     */
    public void setDuration(long duration) {
        throwExceptionIfRunning();
        this.duration = duration;
    }

    /**
     * Returns the number of times the animation cycle will repeat.
     * @return the number of times the animation cycle will repeat.
     */
    public float getRepeatCount() {
	return repeatCount;
    }
    
    /**
     * Sets the number of times the animation cycle will repeat. The default
     * value is 1.
     * This value may be {@link #INDEFINITE} for animations that repeat 
     * indefinitely, but must otherwise be >= 0.
     * The value may be fractional if the animation should stop at some
     * fractional point.
     * This parameter may only be changed prior to starting the animation or 
     * after the animation has ended.
     * 
     * @param repeatCount Number of times the animation cycle will repeat.
     * @see #isRunning()
     * @throws IllegalArgumentException if {@code repeatCount} is not
     *         INDEFINITE or >= 0
     * @throws IllegalStateException if animation is already running
     */
    public void setRepeatCount(float repeatCount) {
        validateRepeatCount(repeatCount);
        throwExceptionIfRunning();
        this.repeatCount = repeatCount;

    }

    /**
     * Returns the {@link RepeatBehavior} of the animation. The default
     * behavior is REVERSE, meaning that the animation will reverse direction
     * at the end of each cycle.
     * @return whether the animation will repeat in the same
     * direction or will reverse direction each time.
     */
    public RepeatBehavior getRepeatBehavior() {
	return repeatBehavior;
    }
    
    /**
     * Sets the {@link RepeatBehavior} of the animation.
     * @param repeatBehavior the behavior for each successive cycle in the
     * animation.  A null behavior is equivalent to specifying the default:
     * REVERSE.  The default behaviors is HOLD.
     * @throws IllegalStateException if animation is already running; this
     * parameter may only be changed prior to starting the animation or 
     * after the animation has ended
     * @see #isRunning()
     */
    public void setRepeatBehavior(RepeatBehavior repeatBehavior) {
        throwExceptionIfRunning();
        this.repeatBehavior = (repeatBehavior != null) ? 
            repeatBehavior : RepeatBehavior.REVERSE;
    }

    /**
     * Returns the {@link EndBehavior} of the animation, either HOLD to 
     * retain the final value or RESET to take on the initial value. The 
     * default behavior is HOLD.
     * @return the behavior at the end of the animation
     */
    public EndBehavior getEndBehavior() {
	return endBehavior;
    }

    /**
     * Sets the behavior at the end of the animation.
     * @param endBehavior the behavior at the end of the animation, either
     * HOLD or RESET.  A null value is equivalent to the default value of
     * HOLD.
     * @throws IllegalStateException if animation is already running; this
     * parameter may only be changed prior to starting the animation or 
     * after the animation has ended
     * @see #isRunning
     */
    public void setEndBehavior(EndBehavior endBehavior) {
        throwExceptionIfRunning();
        this.endBehavior = endBehavior;
    }

    /**
     * Adds the specified Animation to start when this
     * Clip reaches the {@code begin()} state.
     * This is equivalent to calling
     * {@link #addBeginAnimation(Animation, long)
     * addBeginAnimation(beginAnim, 0)}.
     * 
     * @param beginAnim the target which should start based on the
     *                  begin state of this {@code Clip}
     * @see #addBeginAnimation(Animation, long)
     * @see #beginAnimations()
     */
    public void addBeginAnimation(Animation beginAnim) {
        // REMIND: will this handle starting target when source is already running?
        addBeginAnimation(beginAnim, 0);
    }

    /**
     * Adds the specified Animation to start {@code offset}
     * milliseconds after this Clip reaches the {@code begin()} state.
     * 
     * @param beginAnim the target which should be scheduled based on the
     *                  begin state of this {@code Clip}
     * @param offset the number of milliseconds after the begin event when 
     *               {@code target} should start
     * @see #addBeginAnimation(Animation)
     * @see #beginEntries()
     */
    public void addBeginAnimation(Animation beginAnim, long offset) {
        // REMIND: handle negative offset
        throwExceptionIfRunning();
        Schedule rB = getRelBegin();
        synchronized (rB) {
            rB.insert(beginAnim, offset);
        }
    }

    private synchronized Schedule getRelBegin() {
        if (relBegin == null) {
            relBegin = new Schedule();
        }
        return relBegin;
    }

    /**
     * Returns an iterable list of the Animation objects scheduled to
     * start when this Clip reaches the {@code begin()} state.
     * The Animations are iterated directly from this Iterable without
     * any information about scheduling offsets that may have been
     * specified for them.
     * Use {@link #beginEntries()} to iterate the animations along with
     * their scheduling offsets.
     * The returned Iterable can be used directly in a foreach construct:
     * <pre>
     *     for (Animation a : clip.beginAnimations()) {...}
     * </pre>
     * 
     * @return an Iterable useful for iterating the Animations
     * @see #beginEntries()
     */
    public Iterable<Animation> beginAnimations() {
        return getRelBegin().animations(this);
    }

    /**
     * Returns an iterable list of the scheduled animations that will
     * start when this Clip reaches the {@code begin()} state.
     * {@link ScheduledAnimation} instances are iterated from this
     * Iterable so that both the animation and the time offset it
     * is scheduled to start at can be retrieved.
     * The Animation objects themselves can be iterated directly, without
     * time offset information, using the {@link #beginAnimations()} method.
     * The returned Iterable can be used directly in a foreach construct:
     * <pre>
     *     for (ScheduledAnimation sa : clip.beginEntries()) {
     *         Animation a = sa.getAnimation();
     *         long timeOffset = sa.getTime();
     *     }
     * </pre>
     * 
     * @return an Iterable useful for iterating the scheduled animations
     * @see #beginAnimations()
     */
    public Iterable<? extends ScheduledAnimation> beginEntries() {
        return getRelBegin().entries(this);
    }

    void scheduleBeginAnimations(long tBegin) {
        if (relBegin != null) {
            relBegin.scheduleRelativeTo(tBegin);
        }
    }

    /**
     * Removes the specified Animation from the list of Animations to
     * be started when this Clip reaches the {@code begin()} state.
     *
     * @param beginAnim the target to be removed from the begin animations
     */
    public void removeBeginAnimation(Animation beginAnim) {
        throwExceptionIfRunning();
        if (relBegin != null) {
            synchronized (relBegin) {
                relBegin.remove(beginAnim);
            }
        }
    }
    // REMIND: Also provide clearBeginDependents?

    /**
     * Adds the specified Animation to start when this
     * Clip reaches the {@code end()} state.
     * This is equivalent to calling
     * {@link #addEndAnimation(Animation, long)
     * addEndAnimation(endAnim, 0)}.
     * 
     * @param endAnim the target which should start based on the
     *                end state of this {@code Clip}
     * @see #addEndAnimation(Animation, long)
     * @see #endAnimations()
     */
    public void addEndAnimation(Animation endAnim) {
        addEndAnimation(endAnim, 0);
    }
    
    /**
     * Adds the specified Animation to start {@code offset}
     * milliseconds after this Clip reaches the {@code end()} state.
     * 
     * @param endAnim the target which should be scheduled based on the
     *                end state of this {@code Clip}
     * @param offset the number of milliseconds after the end event when 
     *               {@code target} should start
     * @see #addEndAnimation(Animation)
     * @see #endEntries()
     */
    public void addEndAnimation(Animation endAnim, long offset) {
        // REMIND: handle negative offset
        throwExceptionIfRunning();
        Schedule rE = getRelEnd();
        synchronized (rE) {
            rE.insert(endAnim, offset);
        }
    }

    private synchronized Schedule getRelEnd() {
        if (relEnd == null) {
            relEnd = new Schedule();
        }
        return relEnd;
    }

    /**
     * Returns an iterable list of the Animation objects scheduled to
     * start when this Clip reaches the {@code end()} state.
     * The Animations are iterated directly from this Iterable without
     * any information about scheduling offsets that may have been
     * specified for them.
     * Use {@link #endEntries()} to iterate the animations along with
     * their scheduling offsets.
     * The returned Iterable can be used directly in a foreach construct:
     * <pre>
     *     for (Animation a : clip.endAnimations()) {...}
     * </pre>
     * 
     * @return an Iterable useful for iterating the Animations
     * @see #endEntries()
     */
    public Iterable<Animation> endAnimations() {
        return getRelEnd().animations(this);
    }

    /**
     * Returns an iterable list of the scheduled animations that will
     * start when this Clip reaches the {@code end()} state.
     * {@link ScheduledAnimation} instances are iterated from this
     * Iterable so that both the animation and the time offset it
     * is scheduled to start at can be retrieved.
     * The Animation objects themselves can be iterated directly, without
     * time offset information, using the {@link #endAnimations()} method.
     * The returned Iterable can be used directly in a foreach construct:
     * <pre>
     *     for (ScheduledAnimation sa : clip.endEntries()) {
     *         Animation a = sa.getAnimation();
     *         long timeOffset = sa.getTime();
     *     }
     * </pre>
     * 
     * @return an Iterable useful for iterating the scheduled animations
     * @see #endAnimations()
     */
    public Iterable<? extends ScheduledAnimation> endEntries() {
        return getRelEnd().entries(this);
    }

    void scheduleEndAnimations(long tEnd) {
        if (relEnd != null) {
            relEnd.scheduleRelativeTo(tEnd);
        }
    }

    /**
     * Removes the {@code target} Animation from the list of Animations to
     * be started when this Clip reaches the {@code end()} state.
     *
     * @param endAnim the target to be removed from the end animations
     */
    public void removeEndAnimation(Animation endAnim) {
        throwExceptionIfRunning();
        if (relEnd != null) {
            synchronized (relEnd) {
                relEnd.remove(endAnim);
            }
        }
    }
    // REMIND: Also provide clearEndDependents?

    @Override
	void scheduleTo(long trel, long tend, RunQueue runq) {
        throwExceptionIfRunning();
        runq.insert(this, trel);
    }

    void runTo(long tstart, long tcur, RunQueue runq) {
        begin();
        if (relBegin != null) {
            relBegin.transferUpTo(tstart, tcur, runq);
        }
        desiredStatus = Status.RUNNING;
        Status s = timePulse(tcur-tstart);
        desiredStatus = Status.STOPPED;
        if (s == Status.STOPPED) {
            stop();
            if (relEnd != null) {
                long tend = tstart +
                    (long) (getDuration() * (double) getRepeatCount());
                relEnd.transferUpTo(tend, tcur, runq);
            }
        }
    }

    @Override
    void startAt(long when) {
        throwExceptionIfRunning();
        desiredStatus = Status.RUNNING;
        MasterTimer.addToRunQueue(this, when);
    }

    /**
     * Returns whether this Clip object or any of its dependent Clips
     * are currently running
     */
    @Override
	public boolean isRunning() {
        if (desiredStatus == Status.RUNNING) {
            return true;
        }
        // REMIND: Should dependents be considered here?
        // REMIND: If so, do we need a "mightBeRunning" boolean?
        if (relBegin != null) {
            for (Schedule a : relBegin) {
                if (a.getAnimation().isRunning()) return true;
            }
        }
        if (relEnd != null) {
            for (Schedule a : relEnd) {
                if (a.getAnimation().isRunning()) return true;
            }
        }
	return false;
    }

    /**
     * This method is optional; animations will always stop on their own
     * if Clip is provided with appropriate values for
     * duration and repeatCount in the constructor.  But if the application 
     * wants to stop the timer mid-stream, this is the method to call.
     * This call will result in calls to the <code>end()</code> method
     * of all TimingTargets of this Clip.
     * 
     * @see #cancel()
     */
    @Override
	public void stop() {
        desiredStatus = Status.STOPPED;
        MasterTimer.removeFromRunQueue(this);
    }

    /**
     * This method is like the {@link #stop} method, only this one will
     * not result in a calls to the <code>end()</code> method in all 
     * TimingTargets of this Animation; it simply cancels the Clip
     * immediately.
     * 
     * @see #stop()
     */
    @Override
	public void cancel() {
        desiredStatus = Status.CANCELED;
        MasterTimer.removeFromRunQueue(this);
        if (relBegin != null) {
            for (Schedule a : relBegin) {
                a.getAnimation().cancel();
            }
        }
        if (relEnd != null) {
            for (Schedule a : relEnd) {
                a.getAnimation().cancel();
            }
        }
    }
    
    /**
     * This method pauses a running animation.  No further events are sent to
     * TimingTargets. A paused animation may be resumed by calling the
     * {@link #resume} method.  Pausing a non-running animation has no effect.
     * REMIND: what about dependent clips if we are already stopped?
     * 
     * @see #resume()
     * @see #isRunning()
     */
    @Override
	public void pause() {
        MasterTimer.pause(this);
    }
    
    /**
     * This method resumes a paused animation.  Resuming an animation that
     * is not paused has no effect.
     *
     * @see #pause()
     */
    @Override
	public void resume() {
        MasterTimer.resume(this);
    }

    /**
     * Called from Timeline to update this clip's target property
     * according to the current elapsed time.
     *
     * @param totalElapsed the total number of milliseconds that have
     *     elapsed relative to the one true "t0"
     * @return the relative status of the clip according to the current
     *     total elapsed time
     */
    @Override
    Status timePulse(long totalElapsed) {
        if (desiredStatus != Status.RUNNING) return desiredStatus;
        long dur = duration;
        double rep = repeatCount;  // promote for better long calculations
        if (dur == INDEFINITE || rep == INDEFINITE) {
            process(totalElapsed);
        } else {
            dur = (long)(dur * rep);
            if (totalElapsed < dur) {
                process(totalElapsed);
            } else {
                // passing "dur" ensures that the property controlled
                // by the Clip is moved to its ending position before
                // going inactive
                process(dur);
                desiredStatus = Status.STOPPED;
            }
        }
        return desiredStatus;
    }
    
    /**
     * Turns the elapsed time into a fraction, which is then sent out as
     * a timingEvent to all targets of this Clip
     */
    private void process(long timeElapsed) {
        float fraction;
        if (duration == INDEFINITE) {
            fraction = 0f;
        } else if (duration == 0) {
            fraction = 1f;
        } else {
            // Divide and modulus in double for precision with longs
            // Then convert to float once the fraction is known
            double iterationCount = ((double) timeElapsed) / duration;
            if (repeatBehavior == RepeatBehavior.REVERSE) {
                iterationCount = iterationCount % 2.0;
                if (iterationCount > 1.0) {
                    // Reverse the direction
                    iterationCount = 2.0 - iterationCount;
                }
            } else {
                iterationCount = iterationCount % 1.0;
            }
            fraction = (float) iterationCount;
        }
        if (direction == Direction.BACKWARD) {
            // If this is a reversing cycle, want to know inverse
            // fraction; how much from start to finish, not
            // finish to start
            fraction = 1f - fraction;
        }
        fraction = interpolator.interpolate(fraction);
        
        fireTimingEvent(fraction, timeElapsed);
    }

    /**
     * Internal timingEvent method that sends out the event to all targets
     */
    private void fireTimingEvent(float fraction, long totalElapsed) {
        synchronized (targets) {
            for (int i = 0; i < targets.size(); ++i) {
                TimingTarget target = targets.get(i);
                target.timingEvent(fraction, totalElapsed);
            }
        }
    }
    
    /**
     * Internal begin event that sends out the event to all targets
     */
    @Override
    void begin() {
        synchronized (targets) {
            for (int i = 0; i < targets.size(); ++i) {
                TimingTarget target = targets.get(i);
                target.begin();
            }
        }
    }
    
    /**
     * Internal end event that sends out the event to all targets
     */
    @Override
    void end() {
        float resetFraction = (direction == Direction.BACKWARD) ? 1f : 0f;
        long totalElapsed = (long)(duration * ((double) repeatCount));
        synchronized (targets) {
            for (int i = 0; i < targets.size(); ++i) {
                TimingTarget target = targets.get(i);
                if (endBehavior == EndBehavior.RESET) {
                    target.timingEvent(resetFraction, totalElapsed);
                }
                target.end();
            }
        }
    }
}
