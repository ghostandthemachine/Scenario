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
import java.util.List;

public class KeyFrames<T> extends TimingTargetAdapter {

    /**
     * Returns a new {@code KeyFrames} instance for the given target property
     * and set of key values.
     * The key times will be automatically generated and evenly divided
     * among all key frames.
     *
     * @param property the target property of the animation
     * @param keyValues the set of key values in the animation
     * @return a new {@code KeyFrames} instance
     */
    public static <T> KeyFrames<T> create(Property<T> property, T... keyValues) {
        return new KeyFrames(property, keyValues);
    }
    
    /**
     * Returns a new {@code KeyFrames} instance for the given target property
     * and set of key values.
     * The key times will be automatically generated and evenly divided
     * among all key frames.
     * <p>
     * This is a convenience method that is equivalent to calling:
     * <pre>
     *     KeyFrames.create(new BeanProperty&lt;T&gt;(target, property), keyValues);
     * </pre>
     *
     * @param target the target object of the animation
     * @param property the property on the target object to be animated
     * @param keyValues the set of key values in the animation
     * @return a new {@code KeyFrames} instance
     */
    public static <T> KeyFrames<T> create(Object target, String property, T... keyValues) {
        BeanProperty<T> bp = new BeanProperty<T>(target, property);
        return new KeyFrames(bp, keyValues);
    }

    /**
     * Returns a new {@code KeyFrames} instance for the given target property
     * and set of {@code KeyFrame}s.
     * The key frames must be specified such that their key times are in
     * strictly increasing order.
     *
     * @param property the target property of the animation
     * @param keyFrames the set of key frames in the animation
     * @return a new {@code KeyFrames} instance
     */
    public static <T> KeyFrames<T> create(Property<T> property, KeyFrame<T>... keyFrames) {
        return new KeyFrames(property, keyFrames);
    }

    /**
     * Returns a new {@code KeyFrames} instance for the given target property
     * and set of {@code KeyFrame}s.
     * The key frames must be specified such that their key times are in
     * strictly increasing order.
     * <p>
     * This is a convenience method that is equivalent to calling:
     * <pre>
     *     KeyFrames.create(new BeanProperty&lt;T&gt;(target, property), keyFrames);
     * </pre>
     *
     * @param target the target object of the animation
     * @param property the property on the target object to be animated
     * @param keyFrames the set of key frames in the animation
     * @return a new {@code KeyFrames} instance
     */
    public static <T> KeyFrames<T> create(Object target, String property, KeyFrame<T>... keyFrames) {
        BeanProperty<T> bp = new BeanProperty<T>(target, property);
        return new KeyFrames(bp, keyFrames);
    }
    
    private final Property<T> property;
    private final List<KeyFrame<T>> keyFrames;

    private KeyFrames(Property<T> property) {
        this.property = property;
        this.keyFrames = new ArrayList<KeyFrame<T>>();
    }        
    
    private KeyFrames(Property<T> property, T... keyValues) {
        this(property);
        
        if (keyValues.length == 1) {
            // this is a "to" animation, skip initial moveTo()
            T val = keyValues[0];
            keyFrames.add(null);
            keyFrames.add(KeyFrame.create(1f, val));
        } else {
            for (int i = 0; i < keyValues.length; i++) {
                float time = ((float)i) / (keyValues.length-1);
                keyFrames.add(KeyFrame.create(time, keyValues[i]));
            }
        }
    }
    
    private KeyFrames(Property<T> property, KeyFrame<T>... kfs) {
        this(property);

        // if only one keyframe is provided, or if the first keyframe
        // is not at t==0, treat it as a "to" animation (the initial value
        // will be derived at begin time)
        if (kfs.length == 1) {
            keyFrames.add(null);
            keyFrames.add(kfs[0]);
        } else if (kfs[0].getTime() > 0f) {
            keyFrames.add(null);
        }
        
        for (KeyFrame<T> kf : kfs) {
            // TODO: ensure times are provided in strictly increasing order
            // (or maybe sort them?)
            keyFrames.add(kf);
        }
        
        KeyFrame<T> last = kfs[kfs.length-1];
        if (last.getTime() < 1f) {
            // synthesize a keyframe for t==1 by copying the value from the
            // last keyframe given
            keyFrames.add(KeyFrame.create(1f, last.getValue()));
        }
    }
    
    public Property<T> getProperty() {
        return property;
    }
    
    public T getValue(float t) {
        if (t < 0f) {
            t = 0f;
        } else if (t > 1f) {
            t = 1f;
        }

        KeyFrame<T> kf1 = keyFrames.get(0);
        if (kf1 == null) {
            // TODO: this is a workaround for a problem where begin() is
            // sometimes not called prior to the first call to getValue()
            kf1 = KeyFrame.create(0f, getProperty().getValue());
            keyFrames.set(0, kf1);
        }
        KeyFrame<T> kf2 = null;
        float segT = 0f;
        float prevT = 0f;
        for (int i = 1; i < keyFrames.size(); i++) {
            kf2 = keyFrames.get(i);
            segT = kf2.getTime();
            if (t <= segT) {
                // answer lies somewhere in this segment
                segT = (t - prevT) / (segT - prevT);
                break;
            }
            prevT = segT;
            kf1 = kf2;
        }

        // filter the segT value through the interval's interpolator
        segT = kf2.getInterpolator().interpolate(segT);

        // finally, return the interpolated value within the chosen evaluator
        return kf2.getEvaluator().evaluate(kf1.getValue(), kf2.getValue(), segT);
    }

    @Override
    public void timingEvent(float fraction, long totalElapsed) {
        property.setValue(getValue(fraction));
    }
    
    @Override
    public void begin() {
        if (keyFrames.get(0) == null) {
            KeyFrame<T> kf = KeyFrame.create(0f, getProperty().getValue());
            keyFrames.set(0, kf);
        }
    }
}
