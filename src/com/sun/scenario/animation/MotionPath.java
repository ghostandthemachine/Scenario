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

/**
 * Represents a function that controls spatial (motion) interpolation
 * between two {@link KeyFrame} instances.
 * Both simple (two-state) and complex (piecewise linear and non-linear)
 * motion is conveniently available through this class.
 *
 * @author Chris Campbell
 */
public class MotionPath<V> extends Path {
    
    private final Composer<V> composer;
    private final List<Segment> segments;
    private final double[] cachedRet;
    private final double[] v0, v1;
    
    private MotionPath(Composer<V> composer) {
        super(composer.getNumVals());
        this.composer = composer;
        
        // initial value will be filled in by setStartValue(); leave room
        // for it here...
        this.segments = new ArrayList<Segment>();
        this.segments.add(null);
        
        this.cachedRet = new double[composer.getNumVals()];
        this.v0 = new double[composer.getNumVals()*3];
        this.v1 = new double[composer.getNumVals()];
    }
    
    public static <V> MotionPath<V> create(Class<?> type) {
        return new MotionPath(Composer.getInstance(type));
    }

    private static class Segment<V> {
        enum Type { LINEAR, CUBIC }
        final Type type;
        V pt;
        Segment(Type type, V pt) {
            this.type = type;
            this.pt = pt;
        }
    }

    private static class LinearSegment<V> extends Segment<V> {
        LinearSegment(V pt) {
            super(Segment.Type.LINEAR, pt);
        }
    }
    
    private static class CubicSegment<V> extends Segment<V> {
        V ctrlPt1;
        V ctrlPt2;
        CubicSegment(V ctrlPt1, V ctrlPt2, V pt) {
            super(Segment.Type.CUBIC, pt);
            this.ctrlPt1 = ctrlPt1;
            this.ctrlPt2 = ctrlPt2;
        }
    }
    
    private void checkExpanded() {
        if (segments.isEmpty()) {
            return;
        }

        Segment<V> seg = segments.get(0);
        composer.decompose(seg.pt, v0);
        moveTo(v0);
        
        for (int i = 1; i < segments.size(); i++) {
            seg = segments.get(i);
            if (seg.type == Segment.Type.LINEAR) {
                composer.decompose(seg.pt, v0);
                linearTo(v0);
            } else {
                CubicSegment<V> cseg = (CubicSegment<V>)seg;
                int n = composer.getNumVals();
                composer.decompose(cseg.ctrlPt1, v1);
                System.arraycopy(v1, 0, v0, 0, n);
                composer.decompose(cseg.ctrlPt2, v1);
                System.arraycopy(v1, 0, v0, n, n);
                composer.decompose(cseg.pt, v1);
                System.arraycopy(v1, 0, v0, n+n, n);
                cubicTo(v0);
            }
        }

        // the following is not strictly necessary, but helps reduce footprint
        // since the segments are no longer needed after being expanded
        segments.clear();
    }
    
    public void linearTo(V pt) {
        segments.add(new LinearSegment(pt));
    }
    
    public void cubicTo(V ctrlPt1, V ctrlPt2, V pt) {
        segments.add(new CubicSegment(ctrlPt1, ctrlPt2, pt));
    }
    
    public V getValue(float t) {
        checkExpanded();
        getValue(t, cachedRet);
        return composer.compose(cachedRet);
    }
    
    public V getRotationVector(float t) {
        checkExpanded();
        getRotationVector(t, cachedRet);
        return composer.compose(cachedRet);
    }

    public Evaluator<V> createEvaluator() {
        return new PathEvaluator();        
    }
    
    private class PathEvaluator implements Evaluator<V> {
        @Override
		public V evaluate(V v0, V v1, float fraction) {
            if (!segments.isEmpty()) {
                // only do this the first time...
                setExtremeValues(v0, v1);
            }
            return getValue(fraction);
        }
    }

    public Evaluator<V> createRotationEvaluator() {
        return new RotationEvaluator();
    }

    private class RotationEvaluator implements Evaluator<V> {
        @Override
		public V evaluate(V v0, V v1, float fraction) {
            if (!segments.isEmpty()) {
                // only do this the first time...
                setExtremeValues(v0, v1);
            }
            return getRotationVector(fraction);
        }
    }
    
    // TODO: for now we will assume that the start and end values will
    // be constant for all calls to evaluate(), but that may not be the
    // case if this path is reused for different KeyFrames
    private void setExtremeValues(V start, V end) {
        // fill in the gap that we left at construction time
        Segment first = new LinearSegment(start);
        segments.set(0, first);
        
        Segment last = segments.get(segments.size()-1);
        last.pt = end;
    }
}
