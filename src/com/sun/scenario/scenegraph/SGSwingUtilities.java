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

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;

/**
 * A collection of utility methods for Swing.      
 * 
 * @author Igor Kushnirskiy
 */
class SGSwingUtilities {
    /**
     * 
     * @param <T>
     * @param clazz
     * @param parent
     * @param x
     * @param y
     * @param offset offset[0] x offset offset[1] y offset
     * @return
     */
    static <T> T getFirstComponentOfClassAt(Class<T> clazz, Component parent, 
            int x, int y, int[] offset) {
        if (! parent.contains(x, y)) {
            return null;
        }
        if (clazz.isInstance(parent)) {
            return clazz.cast(parent);
        }
        if (parent instanceof Container) {
            Component components[] = ((Container)parent).getComponents();
            int tmpOffset[] = {0, 0};
            for (int i = 0; i < components.length; i++) {
                Component comp = components[i];
                if (comp == null || ! comp.isVisible()) {
                    continue;
                }
                Point loc = comp.getLocation();
                tmpOffset[0] = loc.x;
                tmpOffset[1] = loc.y;
                T t = getFirstComponentOfClassAt(
                        clazz, comp, x - loc.x, y - loc.y, tmpOffset);
                if (t != null) {
                    offset[0] += tmpOffset[0];
                    offset[1] += tmpOffset[1];
                    return t;
                }
            }
        }
        return null;
    }
}
