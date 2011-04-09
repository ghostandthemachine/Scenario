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

import java.util.HashMap;
import java.util.Map;

/**
 * Provides built-in implementations of the {@link Evaluator} interface.
 *
 * @author Chris Campbell
 */
public class Evaluators {
    
    /**
     * Private constructor to prevent instantiation.
     */
    private Evaluators() {
    }

    private static Map<Class<?>, Evaluator> linearCache =
        new HashMap<Class<?>, Evaluator>();

    public static <T> Evaluator<T> getLinearInstance(Class<?> type) {
        Evaluator evaluator = linearCache.get(type);
        if (evaluator == null) {
            evaluator = new Linear(Composer.getInstance(type));
            linearCache.put(type, evaluator);
        }
        return evaluator;
    }
    
    private static class Linear<T> implements Evaluator<T> {
        private Composer<T> composer;
        private double[] v0arr, v1arr;
        
        private Linear(Composer<T> composer) {
            this.composer = composer;
            this.v0arr = new double[composer.getNumVals()];
            this.v1arr = new double[composer.getNumVals()];
        }

        @Override
		public T evaluate(T v0, T v1, float fraction) {
            composer.decompose(v0, v0arr);
            composer.decompose(v1, v1arr);
            for (int i = 0; i < v0arr.length; i++) {
                v0arr[i] += (v1arr[i] - v0arr[i]) * fraction;
            }
            return composer.compose(v0arr);
        }
    }
}
