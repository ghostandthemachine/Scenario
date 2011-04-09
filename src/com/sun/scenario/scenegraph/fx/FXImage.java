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

package com.sun.scenario.scenegraph.fx;

import java.awt.Image;
import java.awt.geom.Point2D;

import com.sun.scenario.scenegraph.SGImage;

/**
 * @author Chris Campbell
 */
public class FXImage extends FXNode {

    private SGImage imageNode;
    
    public FXImage() {
        super(new SGImage());
        this.imageNode = (SGImage)getLeaf();
    }
    
    public final Image getImage() { 
        return imageNode.getImage();
    }

    public void setImage(Image image) {
        imageNode.setImage(image);
    }

    public final Point2D getLocation(Point2D rv) {
        return imageNode.getLocation(rv);
    }

    public final Point2D getLocation() {
        return imageNode.getLocation(null);
    }

    public void setLocation(Point2D location) {
        imageNode.setLocation(location);
    }

    public final Object getInterpolationHint() {
        return imageNode.getInterpolationHint();
    }

    public void setInterpolationHint(Object hint) {
        imageNode.setInterpolationHint(hint);
    }
}
