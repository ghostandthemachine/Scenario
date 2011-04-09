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

import static java.awt.event.MouseEvent.MOUSE_ENTERED;
import static java.awt.event.MouseEvent.MOUSE_EXITED;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.JComponent;

import com.sun.scenario.scenegraph.event.SGMouseAdapter;
import com.sun.scenario.settings.Settings;

/**
 * JSGPanel is a JComponent that renders a scene graph.
 *
 * @author Chet Haase
 * @author Hans Muller
 */
public class JSGPanel extends JComponent {

    // toggles the use of incremental repainting optimizations
    private static final boolean incrementalRepaintOpt;
    static {
        String pkg = JSGPanel.class.getPackage().getName();
        incrementalRepaintOpt = !Settings.getBoolean(pkg + ".fullrepaint");
    }
    
    // debugging utility: fills dirty areas with red XOR rectangles
    private static final boolean hiliteDirty = false;
    private static final boolean enableFPS = true;
    private static final boolean defaultShowFPS = false;
    private FPSData fpsData;

    private SGNode scene = null;
    private boolean sceneIsNew = false;
    private SGGroup sceneGroup = null;
    private Dimension validatedPreferredSize = null;
    private final MouseInputDispatcher dispatchMouseEvents;
    public JSGPanel() {
        setOpaque(true);
        dispatchMouseEvents = new MouseInputDispatcher();
        addMouseListener(dispatchMouseEvents);
        addMouseMotionListener(dispatchMouseEvents);
        addMouseWheelListener(dispatchMouseEvents);
        try {
            setFocusTraversalPolicyProvider(true);
        } catch (NoSuchMethodError e) {
            setFocusCycleRoot(true);
        }
        setFocusTraversalPolicy(FocusHandler.getFocusTraversalPolicy());
    }
    
    SGGroup getSceneGroup() {
        return sceneGroup;
    }
    
    public final SGNode getScene() {
        return fpsData == null ? scene : fpsData.origScene;
    }

    void removeScene() {
        scene = null;
        sceneGroup = null;
        fpsData = null;
        markDirty();
    }

    public void setScene(SGNode scene) {
        JSGPanel oldpanel = scene.getPanel();
        if (oldpanel != null && oldpanel.getScene() == scene) {
            oldpanel.removeScene();
        }
        SGParent oldParent = scene.getParent();
        if (oldParent != null) {
            oldParent.remove(scene);
        }
        if (enableFPS) {
            fpsData = new FPSData(scene);
            scene = fpsData.scene;
        }
        this.scene = scene;
        this.sceneIsNew = true;
        sceneGroup = new SGGroup();
        sceneGroup.add(scene);
        sceneGroup.setParent(this);
        sceneGroup.markDirty(true);
        FocusHandler.addNotify(scene);
        markDirty();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Insets insets = getInsets();
        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;
        SGNode root = getScene(); // so that we ignore FPSData, if present
        if (root == null) {
            return new Dimension(dx + 640, dy + 480);
        } 
        else {
            Rectangle r = root.getBounds().getBounds();
            return new Dimension(dx + r.width, dy + r.height);
        }
    }
    
    
    MouseInputDispatcher getMouseInputDispatcher() {
        return dispatchMouseEvents;
    }
    
    /*
     * Input handling below...
     */

    /* 
     * A mouse event whose location isn't contained by an SGLeaf will be 
     * discarded.
     * 
     * The MouseEvent location/X/Y properties are never updated.  Their
     * value is left as JSGPanel-relative.
     * 
     * Mouse events will be dispatched to all of the nodes returned by 
     * SGNode#pick() - in order - that have a matching listener.  Any node
     * can short circuit this process by {@code SGNode.setMouseBlocker(true)}.
     * 
     * All of the events that comprise a press-drag-release gesture will be
     * delivered to the node that contained the press event.  
     * 
     * A press-drag-release gesture begins when no buttons are down and
     * a button is pressed.  It ends when no buttons are down again.
     * The trailing click (if any), which comes after the last release, 
     * is also considered part of the gesture.
     *
     * A node that receives an enter event, will receive an exit event
     * before a subsequent event is dispatched to another non-overlapping node.
     */
    class MouseInputDispatcher implements 
    MouseListener, MouseMotionListener, MouseWheelListener {
        private int buttonsPressed = 0;        // bit mask, bits 1,2,3 (0 unused)
        private List<SGNode> pdrNodes = null;  // pdr - press-drag-release
        private List<SGNode> enterNodes = Collections.emptyList(); 
    
        /**
         * Returns portion of the {@code nodes} list up to the first 
         * mouse blocker (inclusive). 
         */
        private List<SGNode> uptoMouseBlocker(List<SGNode> nodes) {
            List<SGNode> rv = nodes;
            for (int i = 0; i < nodes.size() - 1; i++) {
                if (nodes.get(i).isMouseBlocker()) {
                    rv = nodes.subList(0, i + 1);
                    break;
                }
            }
            return rv;
        }
        
        private void deliver(MouseEvent e, List<SGNode> nodes) {
            deliver(e, nodes, null);
        }
        
        private Runnable deliver(final MouseEvent e, List<SGNode> nodes, 
                SGComponent sgComponent) {
            Runnable rv = null;
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    SGNode node = nodes.get(i);
                    if (sgComponent != null && node == sgComponent) {
                        // about to postpone the delivery 
                        final List<SGNode> tail = nodes.subList(i, nodes.size());
                        rv = new Runnable() {
                            @Override
							public void run() {
                                deliver(e, tail, null);
                            }
                        };
                        break;
                    } else {
                        node.processMouseEvent(e);
                    }
                }
            }
            return rv;
        }

        private MouseEvent createEvent(MouseEvent e, int id) {
            return new MouseEvent(
                JSGPanel.this, id, e.getWhen(), e.getModifiers(), e.getX(), 
                e.getY(), e.getClickCount(), e.isPopupTrigger(), e.getButton());
        }


        /* Generate an exit event for all nodes that are no longer
         * under the mouse cursor, and an enter event for all nodes
         * that are under the mouse cursor but were not the last time
         * enter/exit notifications were delivered.
         */
        private void deliverEnterExit(MouseEvent e, List<SGNode> nodes) {
            HashSet<SGNode> nodesHash = 
                new HashSet<SGNode>(nodes);
            for(SGNode n : enterNodes) {
                if (! nodesHash.contains(n)) {
                    n.processMouseEvent(createEvent(e, MOUSE_EXITED));
                }
            }
            nodesHash.clear();
            nodesHash.addAll(enterNodes);
            for(SGNode n : nodes) {
                if (! nodesHash.contains(n)) {
                    n.processMouseEvent(createEvent(e, MOUSE_ENTERED));
                }
            }
            enterNodes = nodes;
        }
        
        @Override
		public void mousePressed(MouseEvent e) {
            mousePressed(e, null, null);
        }
        
        private Runnable mousePressed(final MouseEvent e, Point2D _point, 
                SGComponent sgComponent) {
            Runnable rv = null;
            SGGroup sceneGroup = getSceneGroup();
            if (sceneGroup != null) {
                Point2D point = (_point == null ) ? e.getPoint() : _point;
                final List<SGNode> nodes = 
                    uptoMouseBlocker(sceneGroup.pick(point));
                if (buttonsPressed == 0) {
                    pdrNodes = nodes;
                }
                buttonsPressed |= (1 << e.getButton());  // getButton returns 1,2,3 
                rv = deliver(e, pdrNodes, sgComponent);
                if (rv != null) {
                    final Runnable innerRunnable = rv;
                    rv = new Runnable() {
                        @Override
						public void run() {
                            innerRunnable.run();
                            deliverEnterExit(e, nodes);
                        }
                    };
                } else {
                    deliverEnterExit(e, nodes);
                }
            }
            return rv;
        }

        
        @Override
		public void mouseDragged(MouseEvent e) {
            mouseDragged(e, null, null);
        }
        private Runnable mouseDragged(final MouseEvent e, Point2D _point, SGComponent sgComponent) {
            Runnable rv = null;
            SGGroup sceneGroup = getSceneGroup();
            if (sceneGroup != null) {
                Point2D point = (_point == null) ? e.getPoint() : _point;
                final List<SGNode> nodes = 
                    uptoMouseBlocker(sceneGroup.pick(point));
                if (pdrNodes != null) {
                    rv = deliver(e, pdrNodes, sgComponent);
                    if (rv != null) {
                        final Runnable innerRunnable = rv;
                        rv = new Runnable() {
                            @Override
							public void run() {
                                innerRunnable.run();
                                deliverEnterExit(e, nodes);
                            }
                        };
                    }
                } 
                if (rv == null) {
                    deliverEnterExit(e, nodes);
                }
            }
            return rv;
        }

        @Override
		public void mouseReleased(MouseEvent e) {
            mouseReleased(e, null, null);
        }
        private Runnable mouseReleased(final MouseEvent e, Point2D _point, SGComponent sgComponent) {
            Runnable rv = null;
            SGGroup sceneGroup = getSceneGroup();
            if (sceneGroup != null) {
                Point2D point = (_point == null) ? e.getPoint() : _point;
                buttonsPressed &= ~(1 << e.getButton());
                rv = deliver(e, pdrNodes, sgComponent);
                final List<SGNode> nodes = 
                    uptoMouseBlocker(sceneGroup.pick(point));
                if (rv != null) {
                    final Runnable innerRunnable = rv;
                    rv = new Runnable() {
                        @Override
						public void run() {
                            innerRunnable.run();
                            deliverEnterExit(e, nodes);
                        }
                    };
                } else {
                    deliverEnterExit(e, nodes);
                }
            }
            return rv;
        }

        @Override
		public void mouseClicked(MouseEvent e) {
            mouseClicked(e, null, null);
        }
        private Runnable mouseClicked(MouseEvent e, Point2D point, SGComponent sgComponent) {
            return deliver(e, pdrNodes, sgComponent);
        }

        @Override
		public void mouseMoved(MouseEvent e) {
            mouseMoved(e, null, null);
        }
        private Runnable mouseMoved(MouseEvent e, Point2D point, SGComponent sgComponent) {
            Runnable rv = null;
            SGGroup sceneGroup = getSceneGroup();
            mousePoint = (point == null) ? e.getPoint() : point;
            if (sceneGroup != null) {
                List<SGNode> nodes =  
                    uptoMouseBlocker(sceneGroup.pick(mousePoint));
                updateCursor(nodes);
                deliverEnterExit(e, nodes);
                rv = deliver(e, nodes, sgComponent);
            }
            return rv;
        }

        @Override
		public void mouseEntered(MouseEvent e) {
            mouseEntered(e, null, null);
        }
        private Runnable mouseEntered(MouseEvent e, Point2D point, SGComponent sgComponent) {
            return null;
        }
        
        @Override
		public void mouseExited(MouseEvent e) {
            mouseExited(e, null, null);
        }
        private Runnable mouseExited(MouseEvent e, Point2D point, SGComponent sgCompnent) {
            return null;
        }
        
        @Override
		public void mouseWheelMoved(MouseWheelEvent e) {
            mouseWheelMoved(e, null, null);
        }
        private Runnable mouseWheelMoved(MouseWheelEvent e,
                Point2D _point, 
                SGComponent sgComponent) {
            Runnable rv = null;
            SGGroup sceneGroup = getSceneGroup();
            if (sceneGroup != null) {
                Point2D point = (_point == null) ? e.getPoint() : _point;
                List<SGNode> nodes = uptoMouseBlocker(sceneGroup.pick(point));
                deliverEnterExit(e, nodes);
                rv = deliver(e, nodes, sgComponent);
            }
            return rv;
        }
        
        /**
         * Notifies nodes on the mouseEvent. It notifies up to the sgComponent
         * (exclusive) and returns Runnable which notifies the rest of the nodes.
         * 
         * This method is needed for mouse event dispatching for embedded swing 
         * components.
         *
         */
        Runnable processMouseEvent(MouseEvent e, Point2D point, SGComponent sgComponent) {
            /*
             * Point2D point is hires version of e.getPoint()
             */
            Runnable rv = null;
            switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                rv = mousePressed(e, point, sgComponent);
                break;
            case MouseEvent.MOUSE_RELEASED:
                rv = mouseReleased(e, point, sgComponent);
                break;
            case MouseEvent.MOUSE_CLICKED:
                rv = mouseClicked(e, point, sgComponent);
                break;
            case MouseEvent.MOUSE_EXITED:
                rv = mouseExited(e, point, sgComponent);
                break; 
            case MouseEvent.MOUSE_ENTERED:
                rv = mouseEntered(e, point, sgComponent);
                break;
            case MouseEvent.MOUSE_MOVED:
                rv = mouseMoved(e, point, sgComponent);
                break;
            case MouseEvent.MOUSE_DRAGGED:
                rv = mouseDragged(e, point, sgComponent);
                break;
            case MouseEvent.MOUSE_WHEEL:
                rv = mouseWheelMoved((MouseWheelEvent) e, point, sgComponent);
                break;
            }
            return rv;
        }
    }

    
    /*
     * Cursor management below...
     */
    private Cursor defaultCursor = null;
    private Point2D mousePoint = null;
    
    @Override
    public void setCursor(Cursor cursor) {
        setCursor(cursor, true);
    }
    
    private void setCursor(Cursor cursor, boolean isDefault) {
        if (isDefault) {
            defaultCursor = cursor;
        }
        super.setCursor(cursor);
    }
    void updateCursor() {
        if (mousePoint != null && sceneGroup != null) {
            List<SGNode> nodes =  sceneGroup.pick(mousePoint);
            updateCursor(nodes);
        }
    }
    
    void updateCursor(List<SGNode> nodes) {
        if (mousePoint == null) {
            return;
        }
        Cursor cursor = null;
        for (SGNode node : nodes) {
            cursor = node.getCursor();
            if (cursor != null || node.isMouseBlocker()) {
                break;
            }
        }
        cursor = (cursor == null) ? defaultCursor : cursor;
        setCursor(cursor, false);
    }
    
    /*
     * Focus management below...
     */
    
    private SGNode focusOwner = null;
    final void setFocusOwner(SGNode newFocusOwner) {
        SGNode oldFocusOwner = focusOwner;
        focusOwner = newFocusOwner; 
        firePropertyChange("focusOwner", oldFocusOwner, focusOwner);
    }
    public final SGNode getFocusOwner() {
        return focusOwner;
    }


    /*
     * Rendering code below...
     */

    private Rectangle dmgrect;

    @Override
    protected void paintComponent(Graphics g) {
        Rectangle dirtyRegion = g.getClipBounds();
        if (dirtyRegion == null) {
            dirtyRegion = new Rectangle(0, 0, getWidth(), getHeight());
        }

        if (!dirtyRegion.isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            if (isOpaque()) {
                // fill in the background
                g2.setColor(getBackground());
                g2.fill(dirtyRegion);
            }
            SGNode root = getSceneGroup();
            if (root != null) {
                // render all areas of the scene that intersect the dirtyRegion
                root.render(g2, incrementalRepaintOpt ? dirtyRegion : null);
            }
            g2.dispose();
        }
        if (dmgrect != null) {
            Graphics g2 = g.create();
            g2.setXORMode(getBackground());
            g2.setColor(Color.red);
            g2.fillRect(dmgrect.x, dmgrect.y, dmgrect.width, dmgrect.height);
            g2.dispose();
        }
    }
    
    /**
     * Repaints (at minimum) the dirty regions of this panel.
     *
     * @param immediately if true, use paintImmediately() to paint
     *     the dirty regions (useful for "top-level" JSGPanels that are
     *     driven by the master timeline); otherwise, use repaint() (useful
     *     for "embedded" JSGPanels)
     */
    final void repaintDirtyRegions(boolean immediately) {
        if (getSceneGroup() != null) {
            if (getSceneGroup().isDirty() 
                    || SGNodeEventDispatcher.hasPendingEvents()) {
                // process bounds notification events prior to painting
                SGNodeEventDispatcher.dispatchPendingNodeEvents();

                if (!isPreferredSizeSet()) {
                    // if the preferred size hasn't been explicitly set
                    // and there has been a change in the scene bounds,
                    // we may need to revalidate; here we cache the most
                    // recently checked preferred size and compare it to
                    // the current, to avoid repeated calls to revalidate()
                    Dimension d = getPreferredSize();
                    if (!d.equals(validatedPreferredSize)) {
                        validatedPreferredSize = d;
                        revalidate();
                    }
                }

                Rectangle clip = new Rectangle(0, 0, getWidth(), getHeight());
                Rectangle dirtyRegion;
                if (incrementalRepaintOpt && !sceneIsNew) {
                    // walk the entire scene and build the "master"
                    // dirty region
                    dirtyRegion = accumulateDirtyRegions(clip);
                    if (hiliteDirty) {
                        Rectangle olddmg = dmgrect;
                        if (dirtyRegion != null) {
                            if (olddmg == null) {
                                olddmg = dirtyRegion;
                            } else {
                                olddmg.add(dirtyRegion);
                            }
                        }
                        dmgrect = dirtyRegion;
                        dirtyRegion = olddmg;
                    }
                } else {
                    dirtyRegion = clip;
                    sceneIsNew = false;
                }
                if (dirtyRegion != null) {
                    if (immediately) {
                        paintImmediately(dirtyRegion);
                    } else {
                        repaint(dirtyRegion);
                    }
                    if (fpsData != null) {
                        fpsData.nextFrame();
                    }
                }
                clearDirty();
            }
        }
    }
    
    
    /*
     * Dirty region management below...
     */
    
    /**
     * Notifies this JSGPanel that the scene contained within has been
     * made dirty.  This is mainly useful for JSGPanels that are embedded
     * in an SGComponent so that this JSGPanel's dirty region can be properly
     * reported as part of the painting process of that SGComponent.
     */
    final void markDirty() {
        JSGPanelRepainter.getJSGPanelRepainter().addDirtyPanel(this);
    }
    
    /**
     * Clears the dirty state of this JSGPanel as well as that of the scene
     * contained within.
     */
    void clearDirty() {
        if (getSceneGroup() != null) {
            getSceneGroup().clearDirty();
        }
    }
    
    private Rectangle2D accumulateDirty(SGNode node, Rectangle2D r,
                                        Rectangle2D clip)
    {
        if (!node.isDirty()) {
            return r;
        }

        boolean accumulateNodeBounds = false;
        boolean accumulateChildBounds = false;
        
        if (node instanceof SGLeaf) {
            accumulateNodeBounds = true;
        }
        else if (node instanceof SGParent) {
            int dirtyState = node.getDirtyState();
            if (((dirtyState & SGNode.DIRTY_BOUNDS) != 0) ||
                ((dirtyState & SGNode.DIRTY_VISUAL) != 0))
            {
                /*
                 * The group's overall bounds and/or visual state have changed;
                 * delegate to SGNode.accumulateDirty() just like we would
                 * for any leaf node.
                 *
                 * Since SGNode.accumulateDirty() will accurately capture
                 * the overall "former" bounds state, as well as the overall
                 * "updated" bounds state (including any child bounds changes)
                 * it is unnecessary to accumulate for its individual
                 * descendents as we do in the CHILDREN_BOUNDS and
                 * CHILDREN_VISUAL cases below.
                 */
                accumulateNodeBounds = true;
            } else if ((dirtyState & SGNode.DIRTY_CHILDREN_BOUNDS) != 0) {
                /*
                 * There's been a change in the bounds of one or more the
                 * group's descendents; just accumulate dirty children on an
                 * individual basis.  This is less heavyhanded than
                 * accumulating the entire bounds of the group, as we do above,
                 * since it may just be that a single child node has changed
                 * position).
                 *
                 * Note that for this case we can't do the group/clip
                 * intersection optimization as we do for the CHILDREN_VISUAL
                 * case below.  This is because a child node may have changed
                 * its position from a location outside the current
                 * accumulated transformed bounds of the group, and we may
                 * need to repaint the "former" bounds of that child node
                 * (this is ultimately handled by SGNode.accumulateDirty()).
                 * 
                 * Also note that this optimization cannot be applied to
                 * certain SGFilter implementations that paint outside the
                 * child bounds.  In those cases, we simply accumulate the
                 * entire SGFilter bounds, similar to what we do in the
                 * DIRTY_BOUNDS/VISUAL case above.
                 */
                if (node instanceof SGFilter &&
                    ((SGFilter)node).canExpandBounds())
                {
                    accumulateNodeBounds = true;
                } else {
                    accumulateChildBounds = true;
                }
            } else if ((dirtyState & SGNode.DIRTY_CHILDREN_VISUAL) != 0) {
                /*
                 * There's been a visual change in one or more of the
                 * group's descendents; just accumulate dirty children
                 * on an individual basis.
                 *
                 * Optimization: only look at the group's children
                 * if the group's bounding box intersects the clip region.
                 * Looking at only the current accumulated transformed bounds
                 * of the group is safe here because we know at this point that
                 * no children have changed their bounds, so we don't need to
                 * worry about repainting "former" bounds of any children.
                 */
                Rectangle2D bounds = node.getTransformedBoundsRelativeToRoot();
                if (bounds.intersects(clip)) {
                    if (node instanceof SGFilter &&
                        ((SGFilter)node).canExpandBounds())
                    {
                        accumulateNodeBounds = true;
                    } else {
                        accumulateChildBounds = true;
                    }
                }
            }
        }

        if (accumulateNodeBounds) {
            r = node.accumulateDirty(r);
        } else if (accumulateChildBounds) {
            for (SGNode child : ((SGParent) node).getChildren()) {
                r = accumulateDirty(child, r, clip);
            }
        }
        
        return r;
    }
    
    private Rectangle accumulateDirtyRegions(Rectangle clip) {
        assert (getSceneGroup() != null);
        
        // walk down the tree and accumulate dirty regions
        Rectangle2D dirtyRegion = accumulateDirty(getSceneGroup(), null, clip);
        if (dirtyRegion == null || dirtyRegion.isEmpty()) {
            return null;
        } else {
            // expand the bounds just slightly, since a fractional
            // bounding box will still require a complete repaint on
            // pixel boundaries
            return dirtyRegion.getBounds();
        }
    }

    
    /*
     * FPS overlay stuff below...
     */
    
    private static class FPSData extends SGMouseAdapter {
        public SGNode scene;
        public SGNode origScene;
        public SGGroup fpsGroup;
        public SGText fpsText;
        
        long prevMillis;
        
        public FPSData(SGNode scene) {
            SGShape bg = new SGShape();
            bg.setShape(new Rectangle(0, 0, 60, 10));
            bg.setMode(SGShape.Mode.FILL);
            bg.setFillPaint(Color.yellow);

            this.fpsText = new SGText();
            fpsText.setMode(SGText.Mode.FILL);
            fpsText.setFillPaint(Color.black);
            fpsText.setFont(new Font("Serif", Font.PLAIN, 10));
            fpsText.setLocation(new Point(5, 8));
            fpsText.setText("? FPS");

            this.fpsGroup = new SGGroup();
            fpsGroup.add(bg);
            fpsGroup.add(fpsText);
            fpsGroup.setVisible(defaultShowFPS);
            fpsGroup.addMouseListener(this);
            fpsGroup.setMouseBlocker(true);
            SGGroup g = new SGGroup();
            g.add(scene);
            g.add(fpsGroup);
            g.addMouseListener(this);

            this.origScene = scene;
            this.scene = g;
            this.prevMillis = System.currentTimeMillis();
        }

        static final int enableMask =
                (MouseEvent.CTRL_MASK | MouseEvent.ALT_MASK);

        @Override
        public void mouseClicked(MouseEvent e, SGNode node) {
            if (node == fpsGroup) {
                fpsGroup.setVisible(false);
            } else {
                if ((e.getModifiers() & enableMask) == enableMask) {
                    fpsGroup.setVisible(!fpsGroup.isVisible());
                }
            }
        }

        public void nextFrame() {
            if (fpsGroup.isVisible()) {
                long now = System.currentTimeMillis();
                float fps = 1000f/(now-prevMillis);
                fps = Math.round(fps*100)/100f;
                fpsText.setText(fps+" FPS");
                prevMillis = now;
            }
        }
    }

    
    /*
     * TODO: these are just temporary stubs for the FX port...
     */
    
    public javax.swing.Icon toIcon() {
        return null;
    }
    
    public java.awt.image.BufferedImage getIconImage() {
        return null;
    }
}
