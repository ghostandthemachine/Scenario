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
 * Interface that defines the single {@code evaluate()} method,
 * which is used to control the spatial interpolation between {@link KeyFrame}s
 * of an animation.
 * Most applications will typically use either a
 * {@link Evaluators linear Evaluator} or a curve {@code Evaluator} via the
 * {@link MotionPath} class.
 * Advanced developers may choose to implement their own {@code Evaluator} to
 * get custom spatial interpolation behavior.
 *
 * @author Chris Campbell
 * @see Evaluators
 */
public interface Evaluator<T> {
    
    public T evaluate(T v0, T v1, float fraction);
}
