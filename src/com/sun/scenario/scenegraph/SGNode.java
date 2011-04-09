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

import static java.awt.event.MouseEvent.MOUSE_CLICKED;
import static java.awt.event.MouseEvent.MOUSE_DRAGGED;
import static java.awt.event.MouseEvent.MOUSE_ENTERED;
import static java.awt.event.MouseEvent.MOUSE_EXITED;
import static java.awt.event.MouseEvent.MOUSE_MOVED;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;
import static java.awt.event.MouseEvent.MOUSE_RELEASED;
import static java.awt.event.MouseEvent.MOUSE_WHEEL;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.scenario.effect.Effect;
import com.sun.scenario.scenegraph.event.SGFocusListener;
import com.sun.scenario.scenegraph.event.SGKeyListener;
import com.sun.scenario.scenegraph.event.SGMouseListener;
import com.sun.scenario.scenegraph.event.SGNodeEvent;
import com.sun.scenario.scenegraph.event.SGNodeListener;


/**
 * Base class for scene graph nodes.  Nodes define a "local" coordinate
 * system like the one used by AWT/Swing: x increases to the right, y 
 * increases downwards.  
 * 
 * @author Chet Haase
 * @author Hans Muller
 */
public abstract class SGNode {
    // debugging utility: displays red rectangles around node bounds
    private static final boolean debugBounds = false;

    private Object parent;
    private Map<String, Object> attributeMap;
    private List<SGNodeListener> nodeListeners = null;
    private List<SGMouseListener> mouseListeners = null;
    
    @SuppressWarnings("unchecked") 
    private List<SGKeyListener> keyListeners = Collections.EMPTY_LIST;
    @SuppressWarnings("unchecked")
    private List<SGFocusListener> focusListeners = Collections.EMPTY_LIST;
    
    private boolean visible = true;
    private String id;

    private Rectangle2D cachedAccumBounds;
    private AffineTransform cachedAccumXform;
    
    private Cursor cursor = null;
    
    private boolean isMouseBlocker = false;
    
    public final boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            if (visible) {
                this.visible = true;
                markDirty(true);
            } else {
                markDirty(true);
                this.visible = false;
            }
            updateCursor();
        }
    }
    
    /**
     * @return {@code true} if mouse event should not be dispatched to the nodes 
     * underneath this one.
     */
    public final boolean isMouseBlocker() {
        return isMouseBlocker;
    }
    
    public final void setMouseBlocker(boolean value) {
        if (value != isMouseBlocker) {
            isMouseBlocker = value;
            updateCursor();
        }
    }
    
    public final String getID() {
        return id;
    }
    
    public final void setID(String id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return id + " " + super.toString();
    }

    public SGParent getParent() { 
        return (parent instanceof JSGPanel) ? null : (SGParent) parent;
    }

    final void setParent(Object parent) {
        assert (parent == null ||
                parent instanceof SGParent ||
                parent instanceof JSGPanel);
        this.parent = parent;
        updateCursor();
    }

    public JSGPanel getPanel() {
        Object node = parent;
        while (node != null) {
            if (node instanceof JSGPanel) {
                return (JSGPanel)node;
            }
            else {
                node = ((SGNode)node).parent;
            }
        }
        return null;
    }

    /**
     * Returns the bounding box of this node in the coordinate space
     * inherited from the parent.
     * This is a convenience method, equivalent to calling
     * {@code getBounds(null)}.
     */
    public final Rectangle2D getBounds() {
        return getBounds(null);
    }

    /**
     * Returns the bounding box of this node relative to the specified
     * coordinate space.
     *
     * @param transform the transform applied to the geometry
     */
    public abstract Rectangle2D getBounds(AffineTransform transform);

    /**
     * Transforms the bounds of this node by the "cumulative transform",
     * and then returns the bounding box of that transformed shape.
     */
    final Rectangle2D getTransformedBoundsRelativeToRoot() {
        if (cachedAccumBounds == null) {
            cachedAccumBounds = calculateAccumBounds();
        }
        return cachedAccumBounds;
    }

    /**
     * Calculate the accumulated bounds object representing the
     * global bounds relative to the root of the tree.
     * The default implementation calculates new bounds based
     * on the accumulated transform, but SGFilter nodes override
     * this method to return a shared accumulated bounds object
     * from their child.
     */
    Rectangle2D calculateAccumBounds() {
        return getBounds(getCumulativeTransform());
    }
    
    /**
     * Returns the "cumulative transform", which is the concatenation of all
     * ancestor transforms plus the transform of this node (if present).
     */
    final AffineTransform getCumulativeTransform() {
        if (cachedAccumXform == null) {
            cachedAccumXform = calculateCumulativeTransform();
        }
        return cachedAccumXform;
    }

    /**
     * Calculates the accumulated product of all transforms back to
     * the root of the tree.
     * The default implementation simply returns a shared value
     * from the parent, but SGTransform nodes will override this
     * method to return a new modified transform.
     */
    AffineTransform calculateCumulativeTransform() {
        SGNode parent = getParent();
        if (parent == null) {
            return new AffineTransform();
        } else {
            return parent.getCumulativeTransform();
        }
    }

    /**
     * Transforms a point from the global coordinate system of the root
     * node (typically a {@link JSGPanel}) into the local coordinate space
     * of this SGNode.
     * The {@code global} parameter must not be null.
     * If the {@code local} parameter is null then a new {@link Point2D}
     * object will be created and returned after transforming the point.
     * The {@code global} and {@code local} parameters may be the same
     * object and the coordinates will be correctly updated with the
     * transformed coordinates.
     *
     * @param global the coordinates in the global coordinate system to
     *               be transformed
     * @param local a {@code Point2D} object to store the results in
     * @return a {@code Point2D} object containig the transformed coordinates
     */
    public Point2D globalToLocal(Point2D global, Point2D local) {
        try {
            return getCumulativeTransform().inverseTransform(global, local);
        } catch (NoninvertibleTransformException e) {
            // The SGTransform nodes do a "best effort" inverse transform
            // on points so we can get a better answer than just punting
            // by asking them to individually "try" to transform the point...
            SGNode cur = this;
            while (cur != null) {
                if (cur instanceof SGTransform) {
                    global = ((SGTransform) this).inverseTransform(global, local);
                    local = global;
                }
                cur = cur.getParent();
            }
            if (local != global) {
                if (local == null) {
                    local = new Point2D.Float();
                }
                local.setLocation(global);
            }
            return local;
        }
    }


    /**
     * Transforms a point from the local coordinate space of
     * this SGNode into the global coordinate system of the root
     * node (typically a {@link JSGPanel}).
     * The {@code local} parameter must not be null.
     * If the {@code global} parameter is null then a new {@link Point2D}
     * object will be created and returned after transforming the point.
     * The {@code local} and {@code global} parameters may be the same
     * object and the coordinates will be correctly updated with the
     * transformed coordinates.
     *
     * @param local the coordinates in the local coordinate system to
     *               be transformed
     * @param global a {@code Point2D} object to store the results in
     * @return a {@code Point2D} object containig the transformed coordinates
     */
    public Point2D localToGlobal(Point2D local, Point2D global) {
        return getCumulativeTransform().transform(global, local);
    }

    /**
     * Returns true if the given point (specified in the local/untransformed
     * coordinate space of this node) is contained within the visual bounds
     * of this node.  Note that this method does not take visibility
     * into account, the test is based on the node's geometry only.
     *
     * @param point a point in the local coordinate space of this node
     * @return true if the given point is contained within the visual bounds
     *     of this node
     * @throws IllegalArgumentException if {@code point} is null
     */
    public boolean contains(Point2D point) {
        if (point == null) {
            throw new IllegalArgumentException("null point");
        }
        Rectangle2D bounds = getBounds(null);
        return bounds.contains(point);
    }

    
    /*
     * Input handling below...
     */
    
    private boolean maybeAppend(boolean b, SGNode n, List<SGNode> l) {
        if (b) { l.add(n); }
        return b;
    }

    private boolean pickRecursive(SGNode node, Point2D p, List<SGNode> rv) {
        if (node.isVisible()) {
            if (node instanceof SGLeaf) {
                return maybeAppend(node.contains(p), node, rv);
            } else if (node instanceof SGParent) {
                if (node instanceof SGTransform) {
                    p = ((SGTransform) node).inverseTransform(p, null);
                } else if (node instanceof SGClip) {
                    if (!node.contains(p)) {
                        return false;
                    }
                }
                List<SGNode> children = ((SGParent)node).getChildren();
                boolean descendantPicked = false;
                for (int i = children.size() - 1; i >= 0; i--) {
                    SGNode child = children.get(i);
                    if (pickRecursive(child, p, rv)) {
                        descendantPicked = true;
                    }
                }
                return maybeAppend(descendantPicked, node, rv);
            } 
        }
        return false;
    }

    /** 
     * Returns a list of the visible nodes that overlap the specified
     * point in the same order they'd be considered for event
     * dispatching, topmost leaf first.  The point {@code p} is
     * specified in local coordinates.
     */
    public List<SGNode> pick(Point2D p) {
        List<SGNode> rv = new ArrayList<SGNode>();
        if (pickRecursive(this, p, rv)) {
            return rv;
        }
        else {
            return Collections.emptyList();
        }
    }

    final void processMouseEvent(MouseEvent e) {
        if ((mouseListeners != null) && (mouseListeners.size() > 0)) {
            for (SGMouseListener ml : mouseListeners) {
                switch(e.getID()) {
                case MOUSE_PRESSED:  ml.mousePressed(e, this);   break;
                case MOUSE_RELEASED: ml.mouseReleased(e, this);  break;
                case MOUSE_CLICKED:  ml.mouseClicked(e, this);   break;
                case MOUSE_ENTERED:  ml.mouseEntered(e, this);   break;
                case MOUSE_EXITED:   ml.mouseExited(e, this);    break;
                case MOUSE_MOVED:    ml.mouseMoved(e, this);     break;
                case MOUSE_DRAGGED:  ml.mouseDragged(e, this);   break;
                case MOUSE_WHEEL:    
                    ml.mouseWheelMoved((MouseWheelEvent)e, this); 
                    break;
                }
            }
        }
    }

    public void addMouseListener(SGMouseListener listener) { 
	if (listener == null) {
	    throw new IllegalArgumentException("null listener");
	}
        if (mouseListeners == null) {
            mouseListeners = new ArrayList<SGMouseListener>(1);
        }
        mouseListeners.add(listener);
    }

    public void removeMouseListener(SGMouseListener listener) { 
	if (listener == null) {
	    throw new IllegalArgumentException("null listener");
	}
        if (mouseListeners != null) {
            mouseListeners.remove(listener);
        }
    }


    /*
     * Attribute-related methods below...
     */
    
    public final Object getAttribute(String key) {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }
        return (attributeMap == null) ? null : attributeMap.get(key);
    }

    public final void putAttribute(String key, Object value) {
        if (attributeMap == null) {
            attributeMap = new HashMap<String, Object>(1);
        }
        attributeMap.put(key, value);
    }

    
    /*
     * Dirty state/region management below...
     */

    /**
     * This node is completely clean, and so are all of its descendents.
     */
    static final int DIRTY_NONE            = (0 << 0);
    /**
     * This node has changed its overall visual state.
     */
    static final int DIRTY_VISUAL          = (1 << 0);
    /**
     * This node has changed only a subregion of its overall visual state.
     * (Only applicable to SGLLeaf nodes.)
     */
    static final int DIRTY_SUBREGION       = (1 << 1);
    /**
     * This node has changed its bounds, so it is important to account for
     * both the former bounds and its new, updated bounds.
     */
    static final int DIRTY_BOUNDS          = (1 << 2);
    /**
     * One or more of this node's descendents has changed its visual state.
     * (Only applicable to SGLGroup and SGLFilter nodes.)
     */
    static final int DIRTY_CHILDREN_VISUAL = (1 << 3);
    /**
     * One or more of this node's descendents has had a change in bounds,
     * which means that the overall bounds of this group will need recalculation.
     * (Only applicable to SGLGroup and SGLFilter nodes.)
     */
    static final int DIRTY_CHILDREN_BOUNDS = (1 << 4);
    
    /**
     * The dirty state of this node.  This is initialized to DIRTY_VISUAL
     * so that this node is painted for the very first paint cycle.
     */
    private int dirtyState = DIRTY_VISUAL;
    
    /**
     * The most recently painted bounds of this node (transformed relative
     * to the root node, i.e., in device space).  This field is initially
     * set to null and is updated everytime the node is actually rendered
     * to the destination.  It is later used in the case of DIRTY_BOUNDS
     * for the purposes of accumulating the former (dirty) bounds of a
     * particular node.
     */
    private Rectangle2D lastPaintedBounds;
    
    void markDirty(int state) {
        // only mark us if we haven't been marked with this particular
        // dirty state before...
        // and only propagate if we are visible
        if (isVisible() && (dirtyState & state) == 0) {
            // mark this node dirty
            dirtyState |= state;

            // walk up the tree and mark the entire branch dirty
            if (parent instanceof SGNode) {
                if (state == DIRTY_VISUAL || state == DIRTY_SUBREGION) {
                    // tell our ancestors that at least one descendent has
                    // changed its visual state
                    state = DIRTY_CHILDREN_VISUAL;
                } else if (state == DIRTY_BOUNDS) {
                    // tell our ancestors that at least one descendent has
                    // changed its bounds
                    state = DIRTY_CHILDREN_BOUNDS;
                }
                ((SGNode)parent).markDirty(state);
            } else if (parent instanceof JSGPanel) {
                ((JSGPanel)parent).markDirty();
            }
        }
    }
    
    final void markDirty(boolean boundsChanged) {
        if (boundsChanged) {
            markDirty(DIRTY_BOUNDS);
            // we have no choice but to always walk up the entire tree
            // and invalidate all cached local/accum bounds
            invalidateLocalBounds();
        } else {
            markDirty(DIRTY_VISUAL);
        }
    }
    
    final void markSubregionDirty() {
        markDirty(DIRTY_SUBREGION);
    }
    
    void clearDirty() {
        dirtyState = DIRTY_NONE;
    }
    
    void invalidateAccumBounds() {
        // this change affects this node and any/all descendents
        cachedAccumXform = null;
        cachedAccumBounds = null;
    }
    
    void invalidateLocalBounds() {
        // this change affects this node and any/all ancestors
        // (either this group's overall bounds have changed due to
        // a transform change, or there's been a change in the
        // bounds of one or more descendents; either way, we need
        // to invalidate the current cached bounds)
        cachedAccumBounds = null;
        
        // notify any listeners that the local bounds have changed
        if (nodeListeners != null) {
            SGNodeEventDispatcher.addNodeEvent(this);
        }
        
        // walk up the tree and mark the invalidate the cached bounds
        // of every node in this branch
        // TODO: is there some way to minimize the amount of work done here?
        SGNode parent = getParent();
        if (parent != null) {
            parent.invalidateLocalBounds();
        }
    }
    
    final void setLastPaintedBounds(Rectangle2D bounds) {
        // no clone necessary since lastPaintedBounds will not be mutated nor
        // passed outside this object
        this.lastPaintedBounds = bounds;
    }
    
    final boolean isDirty() {
        return (dirtyState != DIRTY_NONE);
    }
    
    final int getDirtyState() {
        return dirtyState;
    }

    /**
     * Safely accumulates the {@code newrect} rectangle into an existing
     * {@code accumulator} rectangle and returns the accumulated result.
     * The result may be {@code null} if the existing {@code accumulator}
     * was {@code null} and the {@code newrect} is either null or empty.
     * If the existing {@code accumulator} was not {@code null} then it
     * is returned, possibly augmented with the union of the bounds of the
     * two rectangles.
     * If a non-{@code null} result is returned then it is guaranteed to
     * be non-empty.
     * The result is never the same object as {@code newrect}.
     * <p>
     * This method provides a convenient mechanism to perform the task
     * of accumulating rectangles used throughout various parts of
     * scene graph management while providing workarounds for unexpected
     * behaviors in the {@link Rectangle2D#add} method which sometimes
     * produces a non-empty result from combining two empty rectangles.
     * 
     * @param accumulator the existing accumulation of rectangle bounds
     * @param newrect a new rectangle to accumulate
     * @return the non-empty result of accumulation, or null if the
     *         accumulation is still empty
     */
    static Rectangle2D accumulate(Rectangle2D accumulator,
                                  Rectangle2D newrect)
    {
        return accumulate(accumulator, newrect, false);
    }

    /**
     * Safely accumulates the {@code newrect} rectangle into an existing
     * {@code accumulator} rectangle and returns the accumulated result.
     * The result may be {@code null} if the existing {@code accumulator}
     * was {@code null} and the {@code newrect} is either null or empty.
     * If the existing {@code accumulator} was not {@code null} then it
     * is returned, possibly augmented with the union of the bounds of the
     * two rectangles.
     * If a non-{@code null} result is returned then it is guaranteed to
     * be non-empty.
     * The result is never the same object as {@code newrect} if
     * {@code newrectshareable} is false.
     * <p>
     * This method provides a convenient mechanism to perform the task
     * of accumulating rectangles used throughout various parts of
     * scene graph management while providing workarounds for unexpected
     * behaviors in the {@link Rectangle2D#add} method which sometimes
     * produces a non-empty result from combining two empty rectangles.
     * 
     * @param accumulator the existing accumulation of rectangle bounds
     * @param newrect a new rectangle to accumulate
     * @param newrectshareable a boolean to indicate if the {@code newrect}
     *        parameter can be shared by using it as the result
     * @return the non-empty result of accumulation, or null if the
     *         accumulation is still empty
     */
    static Rectangle2D accumulate(Rectangle2D accumulator,
                                  Rectangle2D newrect,
                                  boolean newrectshareable)
    {
        if (newrect == null || newrect.isEmpty()) {
            return accumulator;
        }
        if (accumulator == null) {
            // TODO: We really shouldn't be so trusting of the incoming
            // Rectangle type - we should instantiate a (platform sensitive)
            // specific type like R2D.Double (desktop) or R2D.Float (phone)
            return (newrectshareable ? newrect : (Rectangle2D) newrect.clone());
        }
        accumulator.add(newrect);
        return accumulator;
    }

    /*
     * TODO: We may want to consider maintaining an Area/Region object
     * instead to preserve non-contiguous dirty regions; using Rectangle
     * means we're forced to use union(), which will create larger areas
     * than neccessary in many cases; the downside of Area/Region is that
     * we may force Java 2D into complex clipping situations, which may
     * sometimes be slower than the rectangular fast path.
     */
    final Rectangle2D accumulateDirty(Rectangle2D r) {
        if (((dirtyState & DIRTY_BOUNDS) != 0) ||
            ((dirtyState & DIRTY_CHILDREN_BOUNDS) != 0))
        {
            // add in the node's original bounds
            if (lastPaintedBounds != null) {
                r = accumulate(r, lastPaintedBounds, false);
            }
        }
        if (!isVisible()) {
            return r;
        }
        
        if ((dirtyState & DIRTY_SUBREGION) == 0) {
            // add in the node's latest bounds
            r = accumulate(r, getTransformedBoundsRelativeToRoot(), false);
        } else {
            // add in only the affected subregion, transformed
            // relative to the root and intersected with the overall
            // transformed bounds of this node
            Rectangle2D subregionBounds = ((SGLeaf)this).getSubregionBounds();
            Rectangle2D fullBounds = getTransformedBoundsRelativeToRoot();
            if (subregionBounds != null) {
                AffineTransform accumXform = getCumulativeTransform();
                subregionBounds =
                    accumXform.createTransformedShape(subregionBounds).getBounds2D();
                subregionBounds = subregionBounds.createIntersection(fullBounds);
            } else {
                subregionBounds = fullBounds;
            }
            if (!subregionBounds.isEmpty()) {
                boolean srBoundsShareable = (subregionBounds != fullBounds);
                r = accumulate(r, subregionBounds, srBoundsShareable);
            }
        }

        return r;
    }


    /*
     * Rendering code below...
     */

    /**
     * Render the tree of nodes to the specified {@link Graphics2D} object
     * descending from this node as the root.
     * 
     * @param g the {@code Graphics2D} object to render into
     */
    public final void render(Graphics2D g) {
        render(g, null);
    }

    /**
     * Render the tree of nodes to the specified {@link Graphics2D} object
     * descending from this node as the root.
     * The {@code dirtyRegion} parameter can be used to cull the rendering
     * operations on the tree so that only parts of the tree that intersect
     * the indicated rectangle (in device space) will be visited and rendered.
     * If the {@code dirtyRegion} parameter is null then all parts of the
     * tree will be visited and rendered whether they will eventually be
     * visible or not.
     * 
     * @param g the {@code Graphics2D} object to render into
     * @param dirtyRegion a Rectangle to cull which parts of the tree to
     *                    operate on, or null if the full tree should be
     *                    visited and rendered
     */
    final void render(Graphics2D g, Rectangle dirtyRegion) {
        if (!isVisible()) {
            return;
        }

        if (dirtyRegion != null) {
            // check to see whether we need to render this node (including
            // any children) at all
            Rectangle2D bounds = getTransformedBoundsRelativeToRoot();
            if (bounds != null && bounds.intersects(dirtyRegion)) {
                // save the most recently painted bounds in the node, which
                // will be used later when accumulating dirty regions
                setLastPaintedBounds(bounds);
            } else {
                // no need to render this node (or any children)
                return;
            }
        }

        if (this instanceof SGLeaf) {
            SGLeaf leaf = (SGLeaf) this;
            Graphics2D gLeaf = (Graphics2D) g.create();
            leaf.paint(gLeaf);
            if (debugBounds) {
                AffineTransform gtx = g.getTransform();
                Rectangle leafBounds = getBounds(gtx).getBounds();
                g.setTransform(new AffineTransform());
                g.setColor(Color.RED);
                g.drawRect(leafBounds.x, leafBounds.y,
                           leafBounds.width-1,
                           leafBounds.height-1);
                g.setTransform(gtx);
            }
        } else if (this instanceof SGFilter) {
            SGFilter filter = (SGFilter) this;
            SGNode child = filter.getChild();
            if (child != null) {
                Graphics2D gOrig = (Graphics2D) g.create();
                if (filter instanceof SGTransform) {
                    AffineTransform newXform =
                        ((SGTransform) filter).createAffineTransform();
                    gOrig.transform(newXform);
                }
                if (filter.canSkipRendering()) {
                    if (!filter.canSkipChildren()) {
                        child.render(gOrig, dirtyRegion);
                    }
                } else {
                    int sourceType = filter.needsSourceContent();
                    if (sourceType == SGFilter.NONE) {
                        filter.setupRenderGraphics(gOrig);
                        child.render(gOrig, dirtyRegion);
                    } else if (sourceType == SGFilter.CACHED) {
                        filter.renderFromCache(gOrig);
                    } else {
                        Image xformImage = null;
                        Rectangle xformBounds = null;
                        Image unxformImage = null;
                        Rectangle unxformBounds = child.getBounds().getBounds();
                        // We need a rectangle to hand down to the child that
                        // doesn't cause any culling, but still does the
                        // lastpaintedbounds part of the dirty optimization
                        // code above.
                        Rectangle childDirty =
                            (dirtyRegion == null
                             ? null
                             : child.getTransformedBoundsRelativeToRoot().getBounds());
                        if (unxformBounds.isEmpty()) {
                            // nothing to render
                            return;
                        }
                        GraphicsConfiguration gc = gOrig.getDeviceConfiguration();
                        AffineTransform gtx = gOrig.getTransform();
                        if ((sourceType & SGFilter.TRANSFORMED) != 0) {
                            xformBounds = child.getBounds(gtx).getBounds();
                            int nodeX = xformBounds.x;
                            int nodeY = xformBounds.y;
                            int nodeW = xformBounds.width;
                            int nodeH = xformBounds.height;
                            // TODO: image should be constrained to the size of the clip
                            if (filter instanceof SGRenderCache) {
                                // SGRenderCache will hold onto the image
                                // for some time, so create a fresh one
                                // (don't use the pool)
                                xformImage = Effect.createCompatibleImage(gc, nodeW, nodeH);
                            } else {
                                xformImage = Effect.getCompatibleImage(gc, nodeW, nodeH);
                            }

                            Graphics2D gFilter =
                                (Graphics2D) xformImage.getGraphics();
                            AffineTransform filterXform =
                                AffineTransform.getTranslateInstance(-nodeX, -nodeY);
                            filterXform.concatenate(gtx);
                            gFilter.setTransform(filterXform);

                            filter.setupRenderGraphics(gFilter);
                            child.render(gFilter, childDirty);

                            gOrig.setTransform(AffineTransform.getTranslateInstance(nodeX, nodeY));
                        }
                        if ((sourceType & SGFilter.UNTRANSFORMED) != 0) {
                            if (xformImage != null && gtx.isIdentity()) {
                                // in this case there will be no difference
                                // between xformImage and unxformImage; we
                                // can reuse xformImage instead of creating
                                // what would essentially be a duplicate image
                                unxformImage = xformImage;
                            } else {
                                int nodeX = unxformBounds.x;
                                int nodeY = unxformBounds.y;
                                int nodeW = unxformBounds.width;
                                int nodeH = unxformBounds.height;
                                unxformImage = Effect.getCompatibleImage(gc, nodeW, nodeH);

                                Graphics2D gFilter =
                                    (Graphics2D) unxformImage.getGraphics();
                                AffineTransform filterXform =
                                    AffineTransform.getTranslateInstance(-nodeX, -nodeY);
                                gFilter.setTransform(filterXform);

                                filter.setupRenderGraphics(gFilter);
                                child.render(gFilter, childDirty);

                                // TODO: we have very little support for
                                // untransformed source effects at this time;
                                // this needs to be fixed asap...
                                //gOrig.setTransform(AffineTransform.getTranslateInstance(nodeX, nodeY));
                            }
                        }

                        SGSourceContent sourceContent =
                            new SGSourceContent(gtx,
                                                unxformImage, unxformBounds,
                                                xformImage, xformBounds);

                        filter.renderFinalImage(gOrig, sourceContent);

                        if (unxformImage != null) {
                            Effect.releaseCompatibleImage(gc, unxformImage);
                        }
                        if (xformImage != null && xformImage != unxformImage) {
                            Effect.releaseCompatibleImage(gc, xformImage);
                        }
                    }
                }
            }
        } else if (this instanceof SGParent) {
            for (SGNode child : ((SGParent) this).getChildren()) {
                child.render(g, dirtyRegion);
            }
        }
    }

    /*
     * Event handling below...
     */
    
    public void addNodeListener(SGNodeListener listener) {
	if (listener == null) {
	    throw new IllegalArgumentException("null listener");
	}
        if (nodeListeners == null) {
            nodeListeners = new ArrayList<SGNodeListener>(1);
        }
        nodeListeners.add(listener);
    }

    public void removeNodeListener(SGNodeListener listener) {
	if (listener == null) {
	    throw new IllegalArgumentException("null listener");
	}
        if (nodeListeners != null) {
            nodeListeners.remove(listener);
        }
    }
    
    void dispatchNodeEvent() {
        if ((nodeListeners != null) && (nodeListeners.size() > 0)) {
            SGNodeEvent e = SGNodeEvent.createBoundsChangedEvent(this);
            for (SGNodeListener listener : nodeListeners) {
                listener.boundsChanged(e);
            }
        }
    }

    
    /*
     * Input event handling below...
     */

    public void addKeyListener(SGKeyListener listener) { 
        if (listener == null) {
            throw new IllegalArgumentException("null listener");
        }
        if (keyListeners == Collections.EMPTY_LIST) {
            keyListeners = new ArrayList<SGKeyListener>(1);
        }
        keyListeners.add(listener);
    }

    public void removeKeyListener(SGKeyListener listener) { 
        if (listener == null) {
            throw new IllegalArgumentException("null listener");
        }
        keyListeners.remove(listener);
    }
    public void addFocusListener(SGFocusListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("null listener");
        }
        if (focusListeners == Collections.EMPTY_LIST) {
            focusListeners = new ArrayList<SGFocusListener>(1);
        }
        focusListeners.add(listener);
    }
    public void removeFocusListener(SGFocusListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("null listener");
        }
        focusListeners.remove(listener);
    }
    
    void processKeyEvent(KeyEvent e) {
        int id = e.getID();
        for (SGKeyListener listener : keyListeners) {
            switch(id) {
            case KeyEvent.KEY_TYPED:
                listener.keyTyped(e, this);
                break;
            case KeyEvent.KEY_PRESSED:
                listener.keyPressed(e, this);
                break;
            case KeyEvent.KEY_RELEASED:
                listener.keyReleased(e, this);
                break;
            }
        }
    }
    
    void processFocusEvent(FocusEvent e) {
        int id = e.getID();
        for (SGFocusListener listener : focusListeners) {
            switch(id) {
            case FocusEvent.FOCUS_GAINED:
                listener.focusGained(e, this);
                break;
            case FocusEvent.FOCUS_LOST:
                listener.focusLost(e, this);
                break;
          }
        }
    }
    
    boolean isFocusable() {
        return isVisible() && keyListeners.size() > 0;
    }
    
    public final void requestFocus() {
        FocusHandler.requestFocus(this);
    }
    
    public final void setCursor(Cursor cursor) {
        this.cursor = cursor;
        updateCursor();
    }
    
    public final Cursor getCursor() {
        return cursor;
    }
    void updateCursor() {
        JSGPanel panel = getPanel();
        if (panel != null) {
            panel.updateCursor();
        }
    }

    boolean hasOverlappingContents() {
        return true;
    }
}
