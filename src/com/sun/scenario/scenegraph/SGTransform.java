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

package com.sun.scenario.scenegraph;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

/**
 *
 * @author Flar
 */
public abstract class SGTransform extends SGFilter {
    /**
     * This class provides factory methods to create specific subclasses
     * of the {@link SGTransform} class for various common affine transformations.
     *
     * @see Translate
     * @see Scale
     * @see Rotate
     * @see Shear
     * @see Affine
     *
     * @author Flar
     */
    static abstract class Factory {
        /** Creates a new instance of TransformFactory */
        Factory() {
        }

        abstract Translate makeTranslate(double tx, double ty);
        abstract Scale makeScale(double sx, double sy);
        abstract Rotate makeRotate(double theta);
        abstract Shear makeShear(double shx, double shy);
        abstract Affine makeAffine(AffineTransform at);
    }

    static Factory theFactory;

    static {
        // REMIND: This should be switch selectable
        theFactory = new DesktopSGTransformFactory();
    }

    /**
     * Returns an instance of {@link Translate} which translates
     * points by the specified amounts.
     *
     * @param tx the initial {@code X} translation for the filter
     * @param ty the initial {@code Y} translation for the filter
     * @param child an optional child for the filter
     * @return a {@code Translate} object
     */
    public static Translate createTranslation(double tx, double ty, SGNode child) {
        Translate t = theFactory.makeTranslate(tx, ty);
        if (child != null) {
            t.setChild(child);
        }
        return t;
    }
    
    /**
     * Returns an instance of {@link Scale} which scales
     * points by the specified amounts.
     *
     * @param sx the initial {@code X} scale for the filter
     * @param sy the initial {@code Y} scale for the filter
     * @param child an optional child for the filter
     * @return a {@code Scale} object
     */
    public static Scale createScale(double sx, double sy, SGNode child) {
        Scale s = theFactory.makeScale(sx, sy);
        if (child != null) {
            s.setChild(child);
        }
        return s;
    }
    
    /**
     * Returns an instance of {@link Rotate} which rotates
     * points by the specified angle, measured in radians.
     *
     * @param theta the initial rotation for the filter
     * @param child an optional child for the filter
     * @return a {@code Rotate} object
     */
    public static Rotate createRotation(double theta, SGNode child) {
        Rotate r = theFactory.makeRotate(theta);
        if (child != null) {
            r.setChild(child);
        }
        return r;
    }
    
    /**
     * Returns an instance of {@link Shear} which shears
     * points by the specified amounts.
     *
     * @param shx the initial {@code X} shear for the filter
     * @param shy the initial {@code Y} shear for the filter
     * @param child an optional child for the filter
     * @return a {@code Shear} object
     */
    public static Shear createShear(double shx, double shy, SGNode child) {
        Shear s = theFactory.makeShear(shx, shy);
        if (child != null) {
            s.setChild(child);
        }
        return s;
    }

    /**
     * Returns an instance of {@link Affine} which transforms
     * points by the specified generalized affine transformation.
     * If the {@link AffineTransform} parameter is null then an
     * identity transform is used initially.
     *
     * @param at an optional initial transform for the filter
     * @param child an optional child for the filter
     * @return an {@code Affine} object
     */
    public static Affine createAffine(AffineTransform at, SGNode child) {
        Affine a = theFactory.makeAffine(at);
        if (child != null) {
            a.setChild(child);
        }
        return a;
    }

    /** Creates a new instance of SGTransform */
    SGTransform() {
    }
    
    /**
     * Transforms a single input point specified by a {@link Point2D} object
     * into an optionally specified output point and returns the object used
     * to store the results.
     * The {@code dst} argument may be null in which case a new {@code Point2D}
     * object will be created for the return value, otherwise the specified
     * object will be used to store the point and returned from the method.
     *
     * @param src the input point to be transformed
     * @param dst an optional output point for storing the result
     * @return the {@code Point2D} object used to store the result
     */
    public abstract Point2D transform(Point2D src, Point2D dst);
    
    /**
     * Untransforms (transforms by the inverse of this transform) a single
     * input point specified by a {@link Point2D} object into an optionally
     * specified output point and returns the object used to store the results.
     * The {@code dst} argument may be null in which case a new {@code Point2D}
     * object will be created for the return value, otherwise the specified
     * object will be used to store the point and returned from the method.
     * <p>
     * Note that this method does not throw a <code>NoninvertibleTransformException</code>
     * as similar methods in the {@link AffineTransform} class do.
     * The subclasses will make a "best effort" to inverse transform the
     * coordinates of the point depending on what pieces of the inverse
     * calculations can be performed and may return the original {@code src}
     * location if necessary.
     * The forgiving nature of this method should reduce or eliminate
     * unnecessary {@code catch} clauses without reducing its utility.
     * If desired, the associated transform can be retrieved and queried for
     * invertibility.
     *
     * @param src the input point to be inverse transformed
     * @param dst an optional output point for storing the result
     * @return the {@code Point2D} object used to store the result
     */
    public abstract Point2D inverseTransform(Point2D src, Point2D dst);
    
    /**
     * Applies the simple transform operation of this {@code Transform}
     * to a more general {@link AffineTransform} object.
     * The transform operation of this object is appended to the existing
     * transform operations already represented in the {@code AffineTransform}
     * object as if by appending a new matrix.
     *
     * @param at the {@code AffineTransform} object to append this transform to
     */
    public abstract void concatenateInto(AffineTransform at);
    
    /**
     * Sets the {@link AffineTransform} object to represent the same
     * transform as the simple transform of this {@code Transform}.
     * The transform operation of this object replaces the existing
     * transform operations that may have been representd in the
     * {@code AffineTransform} object.
     *
     * @param at the {@code AffineTransform} object to store this transform into
     */
    public abstract void getTransform(AffineTransform at);
    
    /**
     * Creates a new AffineTransform representing the same transform operation
     * as this object.
     *
     * @return a new {@code AffineTransform} object representing this transform.
     */
    public AffineTransform createAffineTransform() {
        AffineTransform at = new AffineTransform();
        getTransform(at);
        return at;
    }

    /**
     * Resets this transform node to an identity operation which has no
     * effect on the input points.
     */
    public abstract void reset();

    protected void invalidateTransform() {
        // mark the current bounds dirty (and force repaint of former
        // bounds as well)
        markDirty(true);
        // changing the transform will invalidate the cached transform/bounds
        // of this node (and all descendents)
        invalidateAccumBounds();
    }
    
    @Override
    public boolean canSkipRendering() {
        return true;
    }

    /**
     * Calculates the accumulated product of all transforms back to
     * the root of the tree.
     * The inherited implementation simply returns a shared value
     * from the parent, but SGTransform nodes must append their
     * individual transform to a copy of that inherited object.
     */
    @Override
    final AffineTransform calculateCumulativeTransform() {
        AffineTransform xform = super.calculateCumulativeTransform();
        xform = new AffineTransform(xform);
        concatenateInto(xform);
        return xform;
    }

    @Override
    public Rectangle2D getBounds(AffineTransform transform) {
        SGNode child = getChild();
        if (child == null) {
            // just an empty rectangle
            return new Rectangle2D.Float();
        } else {
            AffineTransform childTx = createAffineTransform();
            if (childTx != null && !childTx.isIdentity()) {
                if (transform != null && !transform.isIdentity()) {
                    childTx.preConcatenate(transform);
                }
                transform = childTx;
            }
            return child.getBounds(transform);
        }
    }

    /**
     * A subclass of {@link SGTransform} that applies a simple translation
     * transform.
     * A translation transform simply adds a constant value to the {@code X}
     * and {@code Y} coordinates of the source coordinates.
     * The transform {@code x',y'} of a source point {@code x,y} is
     * represented by the equations:
     * <pre>
     *     x' = x + transx;
     *     y' = y + transy;
     * </pre>
     *
     * Instances of this class can only be created by calling the
     * {@link SGTransform#createTranslation} factory method.
     *
     * @author Flar
     */
    public static abstract class Translate extends SGTransform {

        /** Creates a new instance of Translate */
        Translate() {
        }

        /**
         * Returns the translation offset applied to the {@code X} coordinates.
         * @return the {@code} X translation
         */
        public abstract double getTranslateX();

        /**
         * Returns the translation offset applied to the {@code Y} coordinates.
         * @return the {@code} Y translation
         */
        public abstract double getTranslateY();

        /**
         * Sets the translation offset applied to the {@code X} coordinates.
         * @param tx the new {@code} X translation
         */
        public abstract void setTranslateX(double tx);

        /**
         * Sets the translation offset applied to the {@code Y} coordinates.
         * @param ty the new {@code} Y translation
         */
        public abstract void setTranslateY(double ty);

        /**
         * Sets the {@code X} and {@code Y} translation offets to the specified
         * new values.
         *
         * @param tx the new X translation offset
         * @param ty the new Y translation offset
         */
        public abstract void setTranslation(double tx, double ty);

        /**
         * Offsets the {@code X} and {@code Y} translation offets by the specified
         * additional offset values.
         * This method is equivalent to:
         * <pre>
         *     tt.setTranslation(tt.getTranslateX() + tx, tt.getTranslateY() + ty);
         * </pre>
         *
         * @param tx the additional X translation offset
         * @param ty the additional Y translation offset
         */
        public abstract void translateBy(double tx, double ty);

        @Override
        public void reset() {
            setTranslation(0, 0);
        }
    }

    /**
     * This class implements a basic 2 Dimensional scale transform.
     * All input points are scaled independently in the {@code X} and
     * {@code Y} directions by the {@code ScaleX} and {@code ScaleY}
     * values.
     * The transform {@code x',y'} of a source point {@code x,y} is
     * represented by the equations:
     * <pre>
     *     x' = x * scalex;
     *     y' = y * scaley;
     * </pre>
     *
     * Instances of this class can only be created by calling the
     * {@link SGTransform#createScale} factory method.
     *
     * @author Flar
     */
    public static abstract class Scale extends SGTransform {

        /** Creates a new instance of Scale */
        Scale() {
        }

        /**
         * Returns the scale factor for the {@code X} coordinates.
         *
         * @return the {@code X} scale factor
         */
        public abstract double getScaleX();

        /**
         * Returns the scale factor for the {@code Y} coordinates.
         *
         * @return the {@code Y} scale factor
         */
        public abstract double getScaleY();

        /**
         * Sets the scale factor for the {@code X} coordinates.
         *
         * @param sx the new {@code X} scale factor
         */
        public abstract void setScaleX(double sx);

        /**
         * Sets the scale factor for the {@code Y} coordinates.
         *
         * @param sy the new {@code Y} scale factor
         */
        public abstract void setScaleY(double sy);

        /**
         * Sets the scale factors for the {@code X} and {@code Y} coordinates.
         *
         * @param sx the new {@code X} scale factor
         * @param sy the new {@code Y} scale factor
         */
        public abstract void setScale(double sx, double sy);

        /**
         * Scales (multiplies) the scale factors for the {@code X} and {@code Y}
         * coordinates by additional scale factors.
         * This method is equivalent to:
         * <pre>
         *     st.setScale(st.getScaleX() * sx, st.getScaleY() * sy);
         * </pre>
         *
         * @param sx the new {@code X} scale factor
         * @param sy the new {@code Y} scale factor
         */
        public abstract void scaleBy(double sx, double sy);

        @Override
        public void reset() {
            setScale(1, 1);
        }
    }

    /**
     * This class implements a rotation transform about the origin.
     * All points are rotated according to the sine and cosine of the
     * specified angle.
     * The transform {@code x',y'} of a source point {@code x,y} is
     * represented by the equations:
     * <pre>
     *     x' = x * cos(theta) - y * sin(theta);
     *     y' = x * sin(theta) + y * cos(theta);
     * </pre>
     *
     * Instances of this class can only be created by calling the
     * {@link SGTransform#createRotation} factory method.
     *
     * @author Flar
     */
    public static abstract class Rotate extends SGTransform {

        /** Creates a new instance of Rotate */
        Rotate() {
        }

        /**
         * Returns the angle, measured in radians, by which the source
         * points are rotated around the origin.
         * The direction of rotation for positive angles will rotate the
         * positive {@code X} axis towards the positive {@code Y} axis.
         *
         * @return the angle, measured in radians
         */
        public abstract double getRotation();

        /**
         * Sets the angle, measured in radians, by which the source
         * points are rotated around the origin.
         * The direction of rotation for positive angles will rotate the
         * positive {@code X} axis towards the positive {@code Y} axis.
         *
         * @param theta the angle, measured in radians
         */
        public abstract void setRotation(double theta);

        /**
         * Adjusts the angle, measured in radians, by which the source
         * points are rotated around the origin.
         * The direction of rotation for positive angles will rotate the
         * positive {@code X} axis towards the positive {@code Y} axis.
         * This method is equivalent to:
         * <pre>
         *     rt.setRotation(rt.getRotation() + theta);
         * </pre>
         *
         * @param theta the angle to be added, measured in radians
         */
        public abstract void rotateBy(double theta);

        @Override
        public void reset() {
            setRotation(0);
        }
    }

    /**
     * This class implements a simple 2 Dimensional shearing transform.
     * Input points are slanted by applying a scaling factor perpendicular
     * to each axis and that increases linearly along the axes such that
     * points near the origin are not moved at all, but positive coordinates
     * far from the origin are moved by ever increasing distances
     * perpendicular to the axes.
     * Negative coordinates are moved in the opposite direction as positive
     * coordinates.
     * The transform {@code x',y'} of a source point {@code x,y} is
     * represented by the equations:
     * <pre>
     *     x' = x + y * shearx;
     *     y' = y + x * sheary;
     * </pre>
     *
     * Instances of this class can only be created by calling the
     * {@link SGTransform#createShear} factory method.
     *
     * @author Flar
     */
    public static abstract class Shear extends SGTransform {

        /** Creates a new instance of Shear */
        Shear() {
        }

        /**
         * Returns the factor by which positive {@code X} coordinates are moved
         * in the direction of the positive {@code Y} axis.
         * Negative coordinates are moved similarly in the opposite direction.
         *
         * @return the {@code X} shearing factor
         */
        public abstract double getShearX();

        /**
         * Returns the factor by which positive {@code Y} coordinates are moved
         * in the direction of the positive {@code X} axis.
         * Negative coordinates are moved similarly in the opposite direction.
         *
         * @return the {@code Y} shearing factor
         */
        public abstract double getShearY();

        /**
         * Sets the factor by which positive {@code X} coordinates are moved
         * in the direction of the positive {@code Y} axis.
         * Negative coordinates are moved similarly in the opposite direction.
         *
         * @param shx the new {@code X} shearing factor
         */
        public abstract void setShearX(double shx);

        /**
         * Sets the factor by which positive {@code Y} coordinates are moved
         * in the direction of the positive {@code X} axis.
         * Negative coordinates are moved similarly in the opposite direction.
         *
         * @param shy the new {@code Y} shearing factor
         */
        public abstract void setShearY(double shy);

        /**
         * Sets the {@code X} and {@code Y} shearing factors to new values.
         *
         * @param shx the new {@code X} shearing factor
         * @param shy the new {@code Y} shearing factor
         */
        public abstract void setShear(double shx, double shy);

        /**
         * Scales the {@code X} and {@code Y} shearing factors by the
         * specified factors.
         * This method is equivalent to:
         * <pre>
         *     st.setShear(st.getShearX() * shx, st.getShearY() * shy);
         * </pre>
         *
         * @param shx the scale applied to the {@code X} shearing factor
         * @param shy the scale applied to the {@code Y} shearing factor
         */
        public abstract void shearBy(double shx, double shy);

        @Override
        public void reset() {
            setShear(0, 0);
        }
    }

    /**
     * This class implements a general 2 Dimensional affine transform.
     * Input points are transformed by applying the generalized affine
     * transform specified by an {@link AffineTransform} object.
     *
     * Instances of this class can only be created by calling the
     * {@link SGTransform#createAffine} factory method.
     *
     * @author Flar
     */
    public static abstract class Affine extends SGTransform {

        /** Creates a new instance of Affine */
        Affine() {
        }

        /**
         * Returns the {@link AffineTransform} object which controls how
         * coordinates are transformed by this node.
         *
         * @return the {@code AffineTransform} object
         */
        public abstract AffineTransform getAffine();

        /**
         * Sets the {@link AffineTransform} object which controls how
         * coordinates are transformed by this node.
         *
         * @param at the {@code AffineTransform} object
         */
        public abstract void setAffine(AffineTransform at);

        /**
         * Concatenates the existing transform with an additional
         * {@link AffineTransform} object as per the implementation of
         * {@link AffineTransform#concatenate}.
         *
         * @param at the {@code AffineTransform} to be concatenated
         */
        public abstract void transformBy(AffineTransform at);
    }
}
