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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

/**
 *
 * @author Flar
 */
class DesktopSGTransformFactory extends SGTransform.Factory {
    
    /** Creates a new instance of DesktopSGTransformFactory */
    public DesktopSGTransformFactory() {
    }
    
    @Override
    SGTransform.Translate makeTranslate(double tx, double ty) {
        return new Translate(tx, ty);
    }
    
    @Override
    SGTransform.Scale makeScale(double sx, double sy) {
        return new Scale(sx, sy);
    }
    
    @Override
    SGTransform.Rotate makeRotate(double theta) {
        return new Rotate(theta);
    }
    
    @Override
    SGTransform.Shear makeShear(double shx, double shy) {
        return new Shear(shx, shy);
    }

    @Override
    SGTransform.Affine makeAffine(AffineTransform at) {
        return new Affine(at);
    }

    static Point2D setPoint(Point2D dst, double x, double y) {
        if (dst == null) {
            // REMIND: This should be changed back to Double when we
            // create a separate Phone/Embedded factory...
            dst = new Point2D.Float();
        }
        dst.setLocation(x, y);
        return dst;
    }

    // REMIND: These classes must be public for beans property setters to work
    public static final class Translate extends SGTransform.Translate {
        private double tx;
        private double ty;
        
        public Translate(double tx, double ty) {
            this.tx = tx;
            this.ty = ty;
        }
    
        @Override 
        public Point2D transform(Point2D src, Point2D dst) {
            return setPoint(dst, src.getX() + tx, src.getY() + ty);
        }

        @Override 
        public Point2D inverseTransform(Point2D src, Point2D dst) {
            return setPoint(dst, src.getX() - tx, src.getY() - ty);
        }

        @Override
        public void concatenateInto(AffineTransform at) {
            at.translate(tx, ty);
        }

        @Override
        public void getTransform(AffineTransform at) {
            at.setToTranslation(tx, ty);
        }

        @Override 
        public double getTranslateX() {
            return tx;
        }

        @Override 
        public double getTranslateY() {
            return ty;
        }

        @Override 
        public void setTranslateX(double tx) {
            this.tx = tx;
            invalidateTransform();
        }

        @Override 
        public void setTranslateY(double ty) {
            this.ty = ty;
            invalidateTransform();
        }

        @Override 
        public void setTranslation(double tx, double ty) {
            this.tx = tx;
            this.ty = ty;
            invalidateTransform();
        }

        @Override 
        public void translateBy(double tx, double ty) {
            this.tx += tx;
            this.ty += ty;
            invalidateTransform();
        }
    }

    // REMIND: These classes must be public for beans property setters to work
    public static final class Scale extends SGTransform.Scale {
        private double sx;
        private double sy;
        
        public Scale(double sx, double sy) {
            this.sx = sx;
            this.sy = sy;
        }

        @Override 
        public Point2D transform(Point2D src, Point2D dst) {
            double retx = src.getX() * sx;
            double rety = src.getY() * sy;
            return setPoint(dst, retx, rety);
        }

        @Override 
        public Point2D inverseTransform(Point2D src, Point2D dst) {
            double retx = src.getX() / (sx == 0 ? 1 : sx);
            double rety = src.getY() / (sy == 0 ? 1 : sy);
            return setPoint(dst, retx, rety);
        }

        @Override
        public void concatenateInto(AffineTransform at) {
            at.scale(sx, sy);
        }

        @Override
        public void getTransform(AffineTransform at) {
            at.setToScale(sx, sy);
        }

        @Override 
        public double getScaleX() {
            return sx;
        }

        @Override 
        public double getScaleY() {
            return sy;
        }

        @Override 
        public void setScaleX(double sx) {
            this.sx = sx;
            invalidateTransform();
        }

        @Override 
        public void setScaleY(double sy) {
            this.sy = sy;
            invalidateTransform();
        }

        @Override 
        public void setScale(double sx, double sy) {
            this.sx = sx;
            this.sy = sy;
            invalidateTransform();
        }

        @Override 
        public void scaleBy(double sx, double sy) {
            this.sx *= sx;
            this.sy *= sy;
            invalidateTransform();
        }
    }
    
    // REMIND: These classes must be public for beans property setters to work
    public static final class Rotate extends SGTransform.Rotate {
        private double theta;
        
        public Rotate(double theta) {
            this.theta = theta;
        }
        
        static Point2D transform(Point2D src, Point2D dst, double theta) {
            double sin = Math.sin(theta);
            double cos = Math.cos(theta);
            double x = src.getX();
            double y = src.getY();
            double retx = x * cos - y * sin;
            double rety = x * sin + y * cos;
            return setPoint(dst, retx, rety);
        }
    
        @Override 
        public Point2D transform(Point2D src, Point2D dst) {
            return transform(src, dst, theta);
        }

        @Override 
        public Point2D inverseTransform(Point2D src, Point2D dst) {
            return transform(src, dst, -theta);
        }
    
        @Override
        public void concatenateInto(AffineTransform at) {
            at.rotate(theta);
        }

        @Override
        public void getTransform(AffineTransform at) {
            at.setToRotation(theta);
        }

        @Override 
        public double getRotation() {
            return theta;
        }

        @Override 
        public void setRotation(double theta) {
            this.theta = theta;
            invalidateTransform();
        }

        @Override 
        public void rotateBy(double theta) {
            this.theta += theta;
            invalidateTransform();
        }
    }

    // REMIND: These classes must be public for beans property setters to work
    public static final class Shear extends SGTransform.Shear {
        private double shx;
        private double shy;

        public Shear(double shx, double shy) {
            this.shx = shx;
            this.shy = shy;
        }

        @Override 
        public Point2D transform(Point2D src, Point2D dst) {
            double x = src.getX();
            double y = src.getY();
            double retx = x + shx * y;
            double rety = y + shy * x;
            return setPoint(dst, retx, rety);
        }

        @Override 
        public Point2D inverseTransform(Point2D src, Point2D dst) {
            double x = src.getX();
            double y = src.getY();
            double det = 1 - shx * shy;
            double retx = x;
            double rety = y;
            // REMIND: are x,y really the best answer if non-invertible?
            if (det != 0) {
                retx -= shx * y;
                rety -= shy * x;
                retx /= det;
                rety /= det;
            }
            return setPoint(dst, retx, rety);
        }

        @Override
        public void concatenateInto(AffineTransform at) {
            at.shear(shx, shy);
        }

        @Override
        public void getTransform(AffineTransform at) {
            at.setToShear(shx, shy);
        }

        @Override 
        public double getShearX() {
            return shx;
        }

        @Override 
        public double getShearY() {
            return shy;
        }

        @Override 
        public void setShearX(double shx) {
            this.shx = shx;
            invalidateTransform();
        }

        @Override 
        public void setShearY(double shy) {
            this.shy = shy;
            invalidateTransform();
        }

        @Override 
        public void setShear(double shx, double shy) {
            this.shx = shx;
            this.shy = shy;
            invalidateTransform();
        }

        @Override 
        public void shearBy(double shx, double shy) {
            // REMIND: Is this correct?
            this.shx *= shx;
            this.shy *= shy;
            invalidateTransform();
        }
    }

    // REMIND: These classes must be public for beans property setters to work
    public static final class Affine extends SGTransform.Affine {
        private AffineTransform at;

        public Affine(AffineTransform at) {
            if (at == null) {
                this.at = new AffineTransform();
            } else {
                this.at = new AffineTransform(at);
            }
        }

        @Override 
        public Point2D transform(Point2D src, Point2D dst) {
            return at.transform(src, dst);
        }

        @Override 
        public Point2D inverseTransform(Point2D src, Point2D dst) {
            try {
                return at.inverseTransform(src, dst);
            } catch (NoninvertibleTransformException e) {
                return setPoint(dst, src.getX(), src.getY());
            }
        }

        @Override
        public void concatenateInto(AffineTransform at) {
            at.concatenate(this.at);
        }

        @Override
        public void getTransform(AffineTransform at) {
            at.setTransform(this.at);
        }

        @Override 
        public AffineTransform getAffine() {
            return new AffineTransform(at);
        }

        @Override 
        public void setAffine(AffineTransform at) {
            this.at.setTransform(at);
            invalidateTransform();
        }

        @Override 
        public void transformBy(AffineTransform at) {
            this.at.concatenate(at);
            invalidateTransform();
        }

        @Override
        public void reset() {
            this.at.setToIdentity();
            invalidateTransform();
        }
    }
}
