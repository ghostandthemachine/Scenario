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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * This class enables automating animation of bean properties.
 * <p>
 * For example, here is an animation of the "background" property
 * of some object "obj" from blue to red over a period of one second:
 * <pre>
 *  BeanProperty bp = new BeanProperty(obj, "background");
 *  Clip clip = Clip.create(1000, bp, Color.BLUE, Color.RED);
 *  getTimeline().schedule(clip);
 * </pre>
 * <p>
 * More complex animations can be created by passing in multiple values
 * for the property to take on, for example:
 * <pre>
 *  Clip clip = Clip.create(1000, bp, Color.BLUE, Color.RED, Color.GREEN);
 *  getTimeline().schedule(clip);
 * </pre>
 * It is also possible to define more involved and tightly-controlled
 * steps in the animation, including the times between the values and
 * how the values are interpolated by using the constructor that takes
 * a {@link com.sun.scenario.animation.KeyFrames} object.  
 * KeyFrames defines the fractional times at which
 * an object takes on specific values, the values to assume at those times,
 * and the method of interpolation between those values.  For example,
 * here is the same animation as above, specified through KeyFrames, where the
 * RED color will be set 10% of the way through the animation (note that
 * we are not setting an Interpolator, so the timing intervals will use the
 * default linear Interpolator):
 * <pre>
 *  KeyFrame kf0 = new KeyFrame(0.0f, Color.BLUE);
 *  KeyFrame kf1 = new KeyFrame(0.1f, Color.RED);
 *  KeyFrame kf2 = new KeyFrame(1.0f, Color.GREEN);
 *  KeyFrames frames = new KeyFrames(kf0, kf1, kf2);
 *  BeanProperty bp = new BeanProperty(obj, "background");
 *  Clip clip = Clip.create(1000, bp, frames);
 *  getTimeline.schedule(clip);
 * </pre>
 * 
 * @author Chet
 */
public class BeanProperty<T> implements Property<T> {

    private Object object;
    private String propertyName;
    private Method propertySetter;
    private Method propertyGetter;

    /**
     * Constructor for a BeanProperty object.
     * 
     * @param object the object whose property will be animated
     * @param propertyName the name of the property to be animated.  For
     * any propertyName "foo" there must be an accessible "setFoo" method
     * and "getFoo" method on the object.
     * @throws IllegalArgumentException if appropriate set/get methods
     * cannot be found for propertyName.
     */
    public BeanProperty(Object object, String propertyName) {
        this.object = object;
        this.propertyName = propertyName;
        try {
            setupMethodInfo();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Bad property name (" +
                    propertyName +"): could not find " +
                    "an appropriate setter or getter method for that property");
        }
    }
    
    /**
     * Translates the property name used in the PropertyRange object into
     * the appropriate Method in the Object to be modified.  This uses
     * standard JavaBean naming convention (e.g., propertyName would
     * become setPropertyName).
     * @throws NoSuchMethodException if there is no method on the
     * object with the appropriate name
     * @throws SecurityException if the application does not have
     * appropriate permissions to request access to the Method
     */
    private void setupMethodInfo() throws NoSuchMethodException {
        try {
            String firstChar = propertyName.substring(0, 1);
            String remainder = propertyName.substring(1);
            String propertySetterName = "set" + firstChar.toUpperCase() + remainder;

            PropertyDescriptor prop = new PropertyDescriptor(propertyName, object.getClass(),
                    null, propertySetterName);
            propertySetter = prop.getWriteMethod();
            // REMIND: Only need the getter for "to" animations
            try {
                String propertyGetterName = "get" + firstChar.toUpperCase() + 
                        remainder;
                prop = new PropertyDescriptor(propertyName, 
                        object.getClass(), propertyGetterName, null);
                propertyGetter = prop.getReadMethod();
            } catch (Exception e) {
                // "get" failed - try "is"
                String propertyGetterName = "is" + firstChar.toUpperCase() + 
                        remainder;
                prop = new PropertyDescriptor(propertyName, 
                        object.getClass(), propertyGetterName, null);
                propertyGetter = prop.getReadMethod();
            }
        } catch (Exception e) {
            throw new NoSuchMethodException("Cannot find property methods: " + e);
        }
    }
    
    /**
     * Sets the value on the bean property
     */
    @Override
	public void setValue(T value) {
        try {
            propertySetter.invoke(object, value);
        } catch (Exception e) {
            System.out.println("Problem invoking method " +
                    propertySetter + " in object " + object + 
                    " in setValue" + e);
        }
    }

    /**
     * Gets the value from the bean property
     */
    @Override
	public T getValue() {
        try {
            return (T)propertyGetter.invoke(object);
        } catch (Exception e) {
            System.out.println("Problem invoking method " +
                    propertySetter + " in object " + object + 
                    " in setValue" + e);
        }
        return null;
    }
}
