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
 * Defines a key time/value pair for one element in a string of
 * {@code KeyFrames}.
 * <p>
 * By default, linear timing and linear motion is used in the interval
 * between two {@code KeyFrame} objects.
 * The developer can control the timing and/or motion behavior
 * for a particular interval by providing an {@code Interpolator} and/or
 * {@code Evaluator} instance to one of the factory methods.
 * The {@code Interpolator} and {@code Evaluator} properties of a
 * {@code KeyFrame} control the timing/motion for the interval between
 * that {@code KeyFrame} and the one preceding it.
 * Therefore, these properties will be effectively ignored if either is
 * specified for the first {@code KeyFrame} in a {@code KeyFrames} instance.
 *
 * @see KeyFrames
 * @author Chet Haase
 */
public class KeyFrame<T> {
    
    private float time;
    private T value;
    private Interpolator interpolator;
    private Evaluator<T> evaluator;
    
    /**
     * Returns a new {@code KeyFrame} instance for the given time/value pair.
     * Linear timing and motion is used for the preceding interval.
     *
     * @param time the time of this key frame, in the range [0,1]
     * @param value the value at this key frame
     * @return a new {@code KeyFrame} instance
     */
    public static <T> KeyFrame<T> create(float time, T value) {
        return new KeyFrame(time, value, null, null);
    }

    /**
     * Returns a new {@code KeyFrame} instance for the given time/value pair.
     * Linear motion is used for the preceding interval, and timing is
     * controlled by the provided {@code Interpolator} instance.
     *
     * @param time the time of this key frame, in the range [0,1]
     * @param value the value at this key frame
     * @param interpolator the {@code Interpolator} that controls the timing
     * of the preceding interval; if null, the default linear
     * {@code Interpolator} is used
     * @return a new {@code KeyFrame} instance
     */
    public static <T> KeyFrame<T> create(float time, T value, 
                                         Interpolator interpolator)
    {
        return new KeyFrame(time, value, interpolator, null);
    }

    /**
     * Returns a new {@code KeyFrame} instance for the given time/value pair.
     * Linear timing is used for the preceding interval, and motion is
     * controlled by the provided {@code Evaluator} instance.
     *
     * @param time the time of this key frame, in the range [0,1]
     * @param value the value at this key frame
     * @param evaluator the {@code Evaluator} that controls the motion
     * of the preceding interval; if null, the default linear
     * {@code Evaluator} is used
     * @return a new {@code KeyFrame} instance
     */
    public static <T> KeyFrame<T> create(float time, T value,
                                         Evaluator<T> evaluator)
    {
        return new KeyFrame(time, value, null, evaluator);
    }

    /**
     * Returns a new {@code KeyFrame} instance for the given time/value pair.
     * Timing and motion for the preceding interval are controlled by the
     * provided {@code Interpolator} and {@code Evaluator} instances,
     * respectively.
     *
     * @param time the time of this key frame, in the range [0,1]
     * @param value the value at this key frame
     * @param interpolator the {@code Interpolator} that controls the timing
     * of the preceding interval; if null, the default linear
     * {@code Interpolator} is used
     * @param evaluator the {@code Evaluator} that controls the motion
     * of the preceding interval; if null, the default linear
     * {@code Evaluator} is used
     * @return a new {@code KeyFrame} instance
     */
    public static <T> KeyFrame<T> create(float time, T value,
                                         Interpolator interpolator,
                                         Evaluator<T> evaluator)
    {
        return new KeyFrame(time, value, interpolator, evaluator);
    }

    /**
     * Convenience method for constructing a {@code KeyFrame} that
     * goes to {@code value} along a cubic path defined by the
     * control points {@code ctrlPt1} and {@code ctrlPt2}.
     * This method is equivalent to:
     * <pre>
     *     MotionPath<T> mp = MotionPath.create(value.getClass());
     *     mp.cubicTo(ctrlPt1, ctrlPt2, value);
     *     KeyFrame kf = KeyFrame.create(time, value, mp.createEvaluator());
     * </pre>
     *
     * @param time the time of this key frame, in the range [0,1]
     * @param ctrlPt1 first control point for the cubic curve defining motion
     * for the preceding interval
     * @param ctrlPt2 second control point for the cubic curve defining motion
     * for the preceding interval
     * @param value the value at this key frame
     * @return a new {@code KeyFrame} instance
     */
    public static <T> KeyFrame<T> create(float time, T ctrlPt1, T ctrlPt2, T value) {
        MotionPath<T> mp = MotionPath.create(value.getClass());
        mp.cubicTo(ctrlPt1, ctrlPt2, value);
        return KeyFrame.create(time, value, mp.createEvaluator());
    }
    
    /**
     * Private constructor.
     */
    private KeyFrame(float time, T value,
                     Interpolator interpolator, Evaluator<T> evaluator)
    {
        this.time = time;
        this.value = value;
        if (interpolator != null) {
            this.interpolator = interpolator;
        } else {
            this.interpolator = Interpolators.getLinearInstance();
        }
        if (evaluator != null) {
            this.evaluator = evaluator;
        } else {
            // TODO: what if value is null (in case of "to" animations)?
            this.evaluator = Evaluators.getLinearInstance(value.getClass());
        }
    }
    
    /**
     * Returns the value at this key frame.
     * @return the value at this key frame
     */
    public T getValue() {
        return value;
    }

    /**
     * Returns the time of this key frame (a value in the range [0,1]).
     * @return the time of this key frame
     */
    public float getTime() {
        return time;
    }
    
    /**
     * Returns the {@code Interpolator} that controls the timing for the
     * interval between the previous {@code KeyFrame} and this one.
     * @return the {@code Interpolator} for this {@code KeyFrame}
     */
    public Interpolator getInterpolator() {
        return interpolator;
    }

    /**
     * Returns the {@code Evaluator} that controls the motion for the
     * interval between the previous {@code KeyFrame} and this one.
     * @return the {@code Evaluator} for this {@code KeyFrame}
     */
    public Evaluator<T> getEvaluator() {
        return evaluator;
    }
}
