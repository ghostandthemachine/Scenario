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
 * Common ancestor of MotionPath and Interpolators.Spline.  This is kept
 * package-private for now, since there does not seem to be a compelling
 * reason to expose it as a public superclass.
 *
 * @author Chris Campbell
 */
class Path {
    
    private final int valsPerPoint;
    private final float flatness;
    private double[] coords;
    private int numPoints;
    private boolean normalized;
    
    private static final int INIT_SIZE = 4;
    private static final int EXPAND_MAX = 100;
    
    protected Path(int valsPerPoint) {
        this(valsPerPoint, 0.5f);
    }
    
    protected Path(int valsPerPoint, float flatness) {
        // n vals per point + 1 normalized "t" value
        this.valsPerPoint = valsPerPoint + 1;
        this.coords = new double[valsPerPoint*INIT_SIZE];
        this.flatness = flatness;
    }
    
    void moveTo(double... pt) {
        if (numPoints > 0) {
            // REMIND: need to remove this restriction in order to support
            //         discrete (stepwise) functions
            throw new IllegalStateException(
                "Only one moveTo() allowed per path");
        }
        if (pt.length < valsPerPoint-1) {
            throw new IllegalArgumentException(
                "Not enough elements in parameter list");
        }
        
        maybeExpand();
        int n = numPoints * valsPerPoint;
        for (int i = 0; i < valsPerPoint-1; i++) {
            coords[n+i] = pt[i];
        }
        numPoints++;
    }
    
    void linearTo(double... pt) {
        if (numPoints == 0) {
            throw new IllegalStateException("Missing initial moveTo()");
        }
        if (pt.length < valsPerPoint-1) {
            throw new IllegalArgumentException(
                "Not enough elements in parameter list");
        }
        
        maybeExpand();
        int n = numPoints * valsPerPoint;
        for (int i = 0; i < valsPerPoint-1; i++) {
            coords[n+i] = pt[i];
        }
        numPoints++;
    }
    
    void cubicTo(double... pts) {
        if (numPoints == 0) {
            throw new IllegalStateException("Missing initial moveTo()");
        }
        if (pts.length < (valsPerPoint-1)*3) {
            throw new IllegalArgumentException(
                "Not enough elements in parameter list");
        }

        if (src == null) {
            // the array has room for 4 points: pt1, ctrlPt1, ctrlPt2, pt2
            int len = (valsPerPoint - 1) * 4;
            src = new double[len];
        }

        // create new array containing start pt plus pts
        int prevIndex = (numPoints - 1) * valsPerPoint;
        int n = 0;
        for (int i = 0; i < valsPerPoint-1; i++, n++) {
            src[n] = coords[prevIndex+i]; // startPt
        }
        for (int i = 0; i < (valsPerPoint-1)*3; i++, n++) {
            src[n] = pts[i]; // ctrlPt1, ctrlPt2, endPt
        }
        
        flattenCurve(src);
    }
    
    /* Cached array used by flattenCurve() and related methods. */
    private double[] src;
    
    /**
     * Derived from:
     *   http://people.inf.ethz.ch/fischerk/pubs/bez.pdf
     */
    private void flattenCurve(double[] c) {
        if (isFlat(c, flatness)) {
            // all we care about here is the endPt
            int offset = (valsPerPoint-1)*3;
            for (int i = 0; i < valsPerPoint-1; i++) {
                c[i] = c[offset+i];
            }
            linearTo(c);
        } else {
            // REMIND: find a way to avoid allocating arrays here...
            int len = (valsPerPoint - 1) * 4;
            double[] l = new double[len];
            double[] r = new double[len];
            subdivide(c, l, r); // split c into curves l and r
            flattenCurve(l);    // enumerate left curve
            flattenCurve(r);    // enumerate right curve
        }
    }
    
    /**
     * Derived from java.awt.geom.CubicCurve2D (generalized to handle
     * arbitrary n-dimensional data types).
     */
    private void subdivide(double[] c, double[] left, double[] right) {
        int n = valsPerPoint - 1; // offset between pts

        for (int i = 0; i < n; i++) {
            double x1     = c[(n*0)+i];
            double ctrlx1 = c[(n*1)+i];
            double ctrlx2 = c[(n*2)+i];
            double x2     = c[(n*3)+i];

            left [(n*0)+i] = x1;
            right[(n*3)+i] = x2;

            x1 = (x1 + ctrlx1) / 2.0;
            x2 = (x2 + ctrlx2) / 2.0;

            double centerx = (ctrlx1 + ctrlx2) / 2.0;
            ctrlx1 = (x1 + centerx) / 2.0;
            ctrlx2 = (x2 + centerx) / 2.0;
            centerx = (ctrlx1 + ctrlx2) / 2.0;

            left[(n*1)+i] = x1;
            left[(n*2)+i] = ctrlx1;
            left[(n*3)+i] = centerx;

            right[(n*0)+i] = centerx;
            right[(n*1)+i] = ctrlx2;
            right[(n*2)+i] = x2;
        }
    }
    
    /**
     * Derived from:
     *   http://people.inf.ethz.ch/fischerk/pubs/bez.pdf
     */
    private boolean isFlat(double[] c, float flatness) {
        int n = valsPerPoint - 1;
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            /*
             * The format of "c" looks like this (e.g. for Point2D data):
             *   c[0] = x1;
             *   c[1] = y1;
             *   c[2] = ctrlX1;
             *   c[3] = ctrlY1;
             *   c[4] = ctrlX2;
             *   c[5] = ctrlY2;
             *   c[6] = x2;
             *   c[7] = y2;
             */
            double u = 3.0*c[(n*1)+i] - 2.0*c[(n*0)+i] - c[(n*3)+i];
            double v = 3.0*c[(n*2)+i] - 2.0*c[(n*3)+i] - c[(n*0)+i];
            u *= u;
            v *= v;
            if (u < v) u = v;
            sum += u;
        }
        return (sum <= flatness); // flatness is 16*tol^2
    }
    
    double[] getValue(float t, double[] ret) {
        if (!normalized) {
            normalize();
            normalized = true;
        }
        
        // first, handle trivial cases
        if (t <= 0f) {
            for (int i = 0; i < valsPerPoint-1; i++) {
                ret[i] = coords[i];
            }
            return ret;
        } else if (t >= 1f) {
            int lastIndex = (numPoints-1) * valsPerPoint;
            for (int i = 0; i < valsPerPoint-1; i++) {
                ret[i] = coords[lastIndex+i];
            }
            return ret;
        }
        
        /*
         * In abstract terms (e.g. 3 Point2Ds):
         *   coords[0] = pt1.x;
         *   coords[1] = pt1.y;
         *   coords[2] = segT1; // from pt1 to pt2
         *   coords[3] = pt2.x;
         *   coords[4] = pt2.y;
         *   coords[5] = segT2; // from pt1 to pt3
         *   coords[6] = pt3.x;
         *   coords[7] = pt3.y;
         *   coords[8] = 0f;    // not used
         *
         * A specific example:
         *   coords[0] = 0;
         *   coords[1] = 0;
         *   coords[2] = 0.25; // from pt1 to pt2
         *   coords[3] = 1;
         *   coords[4] = 0;
         *   coords[5] = 1.0;  // from pt1 to pt3
         *   coords[6] = 4;
         *   coords[7] = 0;
         *   coords[8] = 0f;   // not used
         */

        // REMIND: use binary search here...
        int max = (numPoints-1) * valsPerPoint;
        for (int i = 0; i < max; i += valsPerPoint) {
            double segT = coords[i+valsPerPoint-1];
            if (t <= segT) {
                // answer lies somewhere in this segment
                double prevT = (i > 0) ? coords[i-1] : 0.0;
                double relT = (t - prevT) / (segT - prevT);
                for (int j = 0; j < valsPerPoint-1; j++) {
                    double v0 = coords[i+j];
                    double v1 = coords[i+j+valsPerPoint];
                    ret[j] = v0 + ((v1 - v0) * relT);
                }
                return ret;
            }
        }
        
        // should not reach here...
        throw new InternalError();
    }
    
    // TODO: could share much of this implementation with getValue()
    double[] getRotationVector(float t, double[] ret) {
        if (!normalized) {
            normalize();
            normalized = true;
        }
        
        // first, handle trivial cases
        if (t <= 0f) {
            for (int i = 0; i < valsPerPoint-1; i++) {
                ret[i] = coords[i+valsPerPoint] - coords[i];
            }
            return ret;
        } else if (t >= 1f) {
            int lastIndex = (numPoints-1) * valsPerPoint;
            for (int i = 0; i < valsPerPoint-1; i++) {
                ret[i] = coords[lastIndex+i] - coords[lastIndex+i-valsPerPoint];
            }
            return ret;
        }

        // REMIND: use binary search here...
        int max = (numPoints-1) * valsPerPoint;
        for (int i = 0; i < max; i += valsPerPoint) {
            double segT = coords[i+valsPerPoint-1];
            if (t <= segT) {
                // answer lies somewhere in this segment
                double prevT = (i > 0) ? coords[i-1] : 0.0;
                double relT = (t - prevT) / (segT - prevT);
                for (int j = 0; j < valsPerPoint-1; j++) {
                    double v0 = coords[i+j];
                    double v1 = coords[i+j+valsPerPoint];
                    ret[j] = v1 - v0;
                }
                return ret;
            }
        }
        
        // should not reach here...
        throw new InternalError();
    }
    
    private void normalize() {
        // calculate actual distances between each point and accumulate
        // total length
        int max = (numPoints-1) * valsPerPoint;
        double totalLength = 0.0;
        for (int i = 0; i < max; i += valsPerPoint) {
            double sum = 0.0;
            for (int j = 0; j < valsPerPoint-1; j++) {
                double v0 = coords[i+j];
                double v1 = coords[i+j+valsPerPoint];
                double d = v1-v0;
                sum += d*d;
            }
            double segLength = sum == 0.0 ? 0.0 : Math.sqrt(sum);
            coords[i+valsPerPoint-1] = segLength;
            totalLength += segLength;
        }

        if (totalLength == 0.0) {
            // this is possible in weird cases where no distance is traveled
            // (all values are the same); deal with this by assigning a
            // normalized "t" value of 1.0 to the first value and return early,
            // which will prevent divide by zero below
            coords[valsPerPoint-1] = 1.0;
            return;
        }
        
        // now normalize segment lengths
        double accumNormLength = 0.0;
        for (int i = valsPerPoint-1; i < max; i += valsPerPoint) {
            double normLength = coords[i] / totalLength;
            accumNormLength += normLength;
            coords[i] = accumNormLength;
        }
    }
    
    /*
     * The following two methods are used to grow the storage arrays
     * as necessary when new segments are added to the path definition.
     */
    
    private static double[] copyOf(double[] original, int newLength) {
        double[] copy = new double[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
    
    private void maybeExpand() {
        int size = coords.length;
        if ((numPoints+1) * valsPerPoint > size) {
            int grow = size;
            if (grow > EXPAND_MAX * valsPerPoint) {
                grow = EXPAND_MAX * valsPerPoint;
            }
            if (grow < valsPerPoint) {
                grow = valsPerPoint;
            }
            coords = copyOf(coords, size+grow);
        }
    }
}
