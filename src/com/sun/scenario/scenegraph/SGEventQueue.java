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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InvocationEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.sun.scenario.scenegraph.SGComponent.SGShell;
import com.sun.scenario.scenegraph.SwingGlueLayer.DelayedRunnable;

/**
 * This is required for embedded components to get mouse events as if they are
 * part of regular swing hierarchy.
 * 
 * In addition to that this event queue is used to perform animation at a
 * regular time intervals.
 * 
 * @author Igor Kushnirskiy
 */
class SGEventQueue  extends EventQueue {
   
    private static final Logger logger = 
        Logger.getLogger(SGEventQueue.class.getName());
    private static final AffineTransform IDENTITY_TRANSFORM = 
        new AffineTransform();
    private AffineTransform lastTransform;
    List<SGComponent> lastsgComponents;
    List<JSGPanel> lastjsgPanels;
    List<AffineTransform> lastTransforms;
    private final ActionListener actionListener =
        new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
                doPulse();
            }
    };
   
    private static AffineTransform getDeepestSGComponentTransformInverse(
            Point2D point, Component parent,
            List<SGComponent> sgComponents,
            List<JSGPanel> jsgPanels,
            List<AffineTransform> jsgPanelInverseTransforms) {
        int[] offset = {0, 0};
        JSGPanel jsgPanel = SGSwingUtilities.getFirstComponentOfClassAt(
                JSGPanel.class, parent, 
                (int) point.getX(), (int) point.getY(), offset);
        int offsetX = offset[0];
        int offsetY = offset[1];
      
        if (jsgPanel != null) {
            SGNode node = jsgPanel.getSceneGroup();
            if (node != null) {
                Point2D jsgPanelPoint = new Point2D.Float(
                        (float) (point.getX() - offsetX), 
                        (float) (point.getY() - offsetY));
                List<SGNode> list = node.pick(jsgPanelPoint);
                for (SGNode leaf : list) {
                    Component embeddedComponent = null;
                    if (leaf instanceof SGComponent 
                            && (embeddedComponent = 
                                ((SGComponent) leaf).getComponent()) != null) {
                        AffineTransform leafTransformInverse = null;
                        try {
                            leafTransformInverse = 
                                leaf.getCumulativeTransform().createInverse();
                        } catch (NoninvertibleTransformException exc) {
                            /* this should not happen
                             * using identity just in case  
                             */
                            leafTransformInverse = IDENTITY_TRANSFORM;
                            String fmt = 
                                "couldn't dispatch %s for %s, bad transform %s";
                            logger.warning(exc, fmt, jsgPanelPoint, leaf, 
                                    leaf.getCumulativeTransform());
                        }
                        Point2D leafPoint = new Point2D.Float();
                        leafTransformInverse.transform(jsgPanelPoint, leafPoint);
                        sgComponents.add((SGComponent) leaf);
                        jsgPanels.add(jsgPanel);
                        int jcgPanelIndex = jsgPanelInverseTransforms.size() - 1;
                        AffineTransform jsgPanelInverseTransform;
                        if (jcgPanelIndex < 0) {
                            jsgPanelInverseTransform = new AffineTransform();
                            jsgPanelInverseTransforms.add(jsgPanelInverseTransform);
                            jsgPanelInverseTransform.translate(-offsetX, -offsetY);
                            
                        } else {
                            jsgPanelInverseTransform = 
                                jsgPanelInverseTransforms.get(jcgPanelIndex);
                            AffineTransform translate = new AffineTransform();
                            translate.translate(-offsetX, -offsetY);
                            jsgPanelInverseTransform.preConcatenate(translate);
                        }
                        AffineTransform childJsgPanelInverseTransform = 
                            new AffineTransform();
                        childJsgPanelInverseTransform
                          .concatenate(leafTransformInverse);
                        childJsgPanelInverseTransform 
                          .concatenate(jsgPanelInverseTransform);
                        jsgPanelInverseTransforms.add(
                                childJsgPanelInverseTransform);
                       
                        getDeepestSGComponentTransformInverse(leafPoint, 
                                embeddedComponent, sgComponents, jsgPanels,
                                jsgPanelInverseTransforms);
                       
                        return jsgPanelInverseTransforms.get(
                                jsgPanelInverseTransforms.size() - 1);
                    }
                }
            }
        }
        return IDENTITY_TRANSFORM;
    }
    
    //copied from LightWeightDispatcher.isMouseGrab
    /* This method effectively returns whether or not a mouse button was down
     * just BEFORE the event happened.  A better method name might be
     * wasAMouseButtonDownBeforeThisEvent().
     */
    private static boolean isMouseGrab(MouseEvent e) {
        int modifiers = e.getModifiersEx();
        
        if(e.getID() == MouseEvent.MOUSE_PRESSED 
                || e.getID() == MouseEvent.MOUSE_RELEASED) {
            switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                modifiers ^= InputEvent.BUTTON1_DOWN_MASK;
                break;
            case MouseEvent.BUTTON2:
                modifiers ^= InputEvent.BUTTON2_DOWN_MASK;
                break;
            case MouseEvent.BUTTON3:
                modifiers ^= InputEvent.BUTTON3_DOWN_MASK;
                break;
            }
        }
        /* modifiers now as just before event */ 
        return ((modifiers & (InputEvent.BUTTON1_DOWN_MASK
                              | InputEvent.BUTTON2_DOWN_MASK
                              | InputEvent.BUTTON3_DOWN_MASK)) != 0);
    }
    
    private Object beforeDispatch(AWTEvent event) {
        Object rv = null;
        if (event instanceof MouseEvent) {
            final int STATE_GRAB = 1;
            final int STATE_RETARGET = 2;
            MouseEvent mouseEvent = (MouseEvent) event;
            Component source = (Component) event.getSource();
            AffineTransform transform = null;
            List<SGComponent> sgComponents = null;
            List<JSGPanel> jsgPanels = null;
            List<AffineTransform> jsgPanelInverseTransforms = null;
            Point2D mousePoint = 
                new Point2D.Float(mouseEvent.getX(), mouseEvent.getY());
            int state = 0;
            if (!isMouseGrab(mouseEvent) 
                    && mouseEvent.getID() != MouseEvent.MOUSE_CLICKED) {
                state = STATE_GRAB;
            } else if (lastTransform != null){
                //events need to be forwarded to the same component
                switch (mouseEvent.getID()) {
                case MouseEvent.MOUSE_PRESSED:
                case MouseEvent.MOUSE_RELEASED:
                case MouseEvent.MOUSE_MOVED:
                case MouseEvent.MOUSE_DRAGGED:
                    state = STATE_RETARGET;
                    break;
                }
            }
            if (state == STATE_RETARGET) {
                transform = lastTransform;
                sgComponents = lastsgComponents;
                jsgPanels = lastjsgPanels;
                jsgPanelInverseTransforms = lastTransforms;
            } else {
                sgComponents = new LinkedList<SGComponent>();
                jsgPanels = new LinkedList<JSGPanel>();
                jsgPanelInverseTransforms = new LinkedList<AffineTransform>();
                transform = getDeepestSGComponentTransformInverse(
                        mousePoint, source, sgComponents,
                        jsgPanels,
                        jsgPanelInverseTransforms);
                if (state == STATE_GRAB) {
                    lastTransform = transform;
                    lastsgComponents = sgComponents;
                    lastjsgPanels = jsgPanels;
                    lastTransforms = jsgPanelInverseTransforms;
                }
            }
            if (! sgComponents.isEmpty()) {
                Point2D tmpPoint2D = transform.transform(mousePoint, null);
                Point translatedPoint = 
                    new Point((int) tmpPoint2D.getX(), (int) tmpPoint2D.getY());
                JComponent bottomContainer = 
                    sgComponents.get(sgComponents.size() - 1).getContainer();
                JComponent topContainer = 
                    sgComponents.get(0).getContainer();
                //bottom container's top left corner in the source coordinates
                Point bottomContainerTopLeftPoint = 
                    SwingUtilities.convertPoint(
                            bottomContainer, new Point(0,0), source);
                List<Runnable> runnables = new LinkedList<Runnable>();
                //deliver event to all nodes on top of the target embedded component
                boolean haveMouseBlocker = false;
                for (int i = 0; i < jsgPanels.size(); i++) {
                    JSGPanel jsgPanel = jsgPanels.get(i);
                    SGComponent sgComponent = null;
                    Point2D jsgPoint = null;
                    if (haveMouseBlocker) {
                        /* 
                         * we need to deliver mouse events to all jsgPanels
                         * in a location off them so they can process mouseExit
                         * event.
                         */
                        jsgPoint = 
                            new Point2D.Double(Double.MAX_VALUE, Double.MAX_VALUE);
                    } else {
                        sgComponent = sgComponents.get(i);
                        AffineTransform jsgPanelTransform = 
                            jsgPanelInverseTransforms.get(i);
                        jsgPoint = jsgPanelTransform.transform(mousePoint, null);
                    }
                    mouseEvent.translatePoint(
                            (int) (jsgPoint.getX() - mouseEvent.getX()),
                            (int) (jsgPoint.getY() - mouseEvent.getY()));
                    Runnable runnable = jsgPanel.getMouseInputDispatcher()
                      .processMouseEvent(mouseEvent, jsgPoint, sgComponent);
                    if (haveMouseBlocker || runnable == null) { //we hit mouseBlocker
                        haveMouseBlocker = true;
                    } else {
                        runnables.add(runnable);
                    }
                }
                if (! haveMouseBlocker) {
                    JSGPanel topPanel = jsgPanels.get(0);
                    translatedPoint.translate(
                            bottomContainerTopLeftPoint.x,
                            bottomContainerTopLeftPoint.y);
                    Point translatedPointInTopPanel = SwingUtilities.convertPoint(
                            source, translatedPoint, topPanel);
                    Rectangle visibleRectangle = topPanel.getVisibleRect();
                    if (! visibleRectangle.contains(translatedPointInTopPanel)) {
                        /*
                         * We need to ensure translated mouse coordinates 
                         * are in visible rectangle of the topmost JSGPanel
                         */
                        int deltaX = 
                            visibleRectangle.x - translatedPointInTopPanel.x; 
                        int deltaY =  
                            visibleRectangle.y - translatedPointInTopPanel.y;
                        topContainer.setLocation(
                                topContainer.getX() + deltaX,
                                topContainer.getY() + deltaY);
                        translatedPoint.translate(deltaX, deltaY);
                    }
                } else {
                    /* 
                     * we should not deliver mouse event to the embedded 
                     * component because one of the nodes on top of it is a 
                     * mouseBlocker.
                     */
                    translatedPoint.x = Integer.MAX_VALUE;
                    translatedPoint.y = Integer.MAX_VALUE;
                }
                mouseEvent.translatePoint(
                        (int) (translatedPoint.getX() - mouseEvent.getX()),
                        (int) (translatedPoint.getY() - mouseEvent.getY()));
                
                for (SGComponent sgComponent : sgComponents) {
                    Component component = sgComponent.getComponent();
                    if (component != null) {
                        //component may be removed sometimes. Take drag gesture for example.
                        SGShell shell = (SGShell) component.getParent();
                        shell.setContains(true);
                    }
                }
                rv = new Object[] {sgComponents, runnables, 
                        jsgPanelInverseTransforms, mousePoint};
            }
        }
        return rv;
    }
    
    @SuppressWarnings("unchecked")
    private void afterDispatch(AWTEvent event, Object handle) {
        if (handle != null) {
            Object[] array = (Object[]) handle;
            List<SGComponent> sgComponents = (List<SGComponent>) array[0];
            List<Runnable> runnables = (List<Runnable>) array[1];
            List<AffineTransform> jsgPanelInverseTransforms = 
                (List<AffineTransform>) array[2];
            Point2D mousePoint = (Point2D) array[3]; 
            MouseEvent mouseEvent = (MouseEvent) event;
            for (SGComponent sgComponent : sgComponents) {
                Component component = sgComponent.getComponent();
                if (component != null) {
                    //component might be removed as a result of the event
                    SGShell shell = (SGShell) component.getParent();
                    shell.setContains(false);
                }
            }
            //deliver event to all nodes under the target embedded component
            for (int i = runnables.size() - 1;
                  i >= 0; i--) {
                Runnable runnable = runnables.get(i);
                if (runnable == null) {
                    continue;
                }
                AffineTransform jsgPanelTransform = 
                    jsgPanelInverseTransforms.get(i);
                Point2D jsgPoint = 
                    jsgPanelTransform.transform(mousePoint, null);
                /* 
                 * we need to translate mouseEvent because the same object 
                 * instance is used.
                 */ 
                mouseEvent.translatePoint(
                        (int) (jsgPoint.getX() - mouseEvent.getX()),
                        (int) (jsgPoint.getY() - mouseEvent.getY()));
                runnable.run();
            }
        }
    }
    
    @Override
    protected void dispatchEvent(AWTEvent event) {
        Object handle = beforeDispatch(event);
        super.dispatchEvent(event);
        afterDispatch(event, handle);
    }
    
    //time based animation
    private final InvocationEvent pulseEvent = new InvocationEvent(this, 
            new Runnable() {
                @Override
				public void run() {
                    doPulse();
                } 
    });
    
    private AWTEvent postponed = null;
    private boolean waitForNonPulseEvent = true;
    private static final long RESPONSIVE_THRESHOLD = 1000 / 30;
    
    private final Object lock = new Object();
    //flag to disable alarmTimer start during the pulse
    //can be accessed with the lock held
    private boolean disableTimer = false;
    
    //can be accessed with the lock held
    private Timer alarmTimer = null;
    
    //can be accessed with the lock held
    private DelayedRunnable animationRunnable = null;
    
    private static long getWhen(AWTEvent e) {
        long when = Long.MIN_VALUE;
        if (e instanceof InputEvent) {
            InputEvent ie = (InputEvent)e;
            when = ie.getWhen(); 
        } else if (e instanceof InputMethodEvent) {
            InputMethodEvent ime = (InputMethodEvent)e;
            when = ime.getWhen(); 
        } else if (e instanceof ActionEvent) {
            ActionEvent ae = (ActionEvent)e;
            when = ae.getWhen(); 
        } else if (e instanceof InvocationEvent) {
            InvocationEvent ie = (InvocationEvent)e;
            when = ie.getWhen(); 
        }
        return when;
    }
    
    private AWTEvent getNextEvent(EventQueue eventQueue) throws InterruptedException {
        AWTEvent nextEvent = null;
        if (postponed != null) {
            nextEvent = postponed;
            postponed = null;
        } else {
            AWTEvent superNextEvent = null;
            do {
                DelayedRunnable delayedRunnable = null;
                synchronized (lock) {
                    delayedRunnable = animationRunnable;
                }
                if (delayedRunnable != null
                        && ! waitForNonPulseEvent 
                        && delayedRunnable.getDelay() <= 0) {
                    nextEvent = pulseEvent;
                    postponed = superNextEvent;
                    waitForNonPulseEvent = true;
                } else if (superNextEvent != null){
                    nextEvent = superNextEvent;
                } else {
                    /*
                     * super.getNextEvent is called at least once between
                     * pulses. We need this to get toolkit's events pushed to
                     * the EventQueue.
                     */
                    if (eventQueue == this) {
                        //used as eventQueue
                        superNextEvent = super.getNextEvent();
                    } else {
                        //used as delegate
                        superNextEvent = eventQueue.getNextEvent();
                    }
                    long now = System.currentTimeMillis();
                    /*
                     * Do not return pulseEvent until we catch up with posted 
                     * events. 
                     */
                    waitForNonPulseEvent = 
                        (now - getWhen(superNextEvent) > RESPONSIVE_THRESHOLD);
                }
            } while (nextEvent == null);
        }
        return nextEvent;
    }

    @Override
    public AWTEvent getNextEvent() throws InterruptedException {
       return getNextEvent(this);
    }
    
    private void doPulse() {
        synchronized (lock) {
            disableTimer = true;
            stopPulseAlarm();
            if (animationRunnable != null) {
                animationRunnable.run();
            }
            disableTimer = false;
            updatePulseAlarm();
        }
    }
    
    //this method may be called off EDT
    void setAnimationRunnable(DelayedRunnable animationRunnable) {
        synchronized (lock) {
            this.animationRunnable = animationRunnable;
            updatePulseAlarm();
        }
    }
    
    //this method may be called off EDT
    private void updatePulseAlarm() {
        synchronized (lock) {
            if (disableTimer) {
                return;
            }
            if (animationRunnable != null) {
                if (alarmTimer == null
                        || ! alarmTimer.isRunning()) {
                    int timerDelay = (int) animationRunnable.getDelay();
                    alarmTimer = new Timer(timerDelay, actionListener);
                    alarmTimer.setRepeats(false);
                    alarmTimer.start();
                }
            } else {
                stopPulseAlarm();
            }
        }
    }
    
    //this method may be called off EDT
    private void stopPulseAlarm() {
        synchronized (lock) {
            if (alarmTimer != null) {
                alarmTimer.stop();
                alarmTimer = null;
            }
        }
    }
    
    // EventQueue registration stuff
    
    void register()  {
        boolean registered = false;
        try {
            /* 
             * we need Callable for EventQueueDelegete
             * and it may be not available (on mobile for example)
             */
            Class.forName("java.util.concurrent.Callable");
            Class<?> swingUtilities3Class = 
                Class.forName("com.sun.java.swing.SwingUtilities3");
            Method setEventQueueDelegate = 
                swingUtilities3Class.getMethod("setEventQueueDelegate",
                        Map.class);
            setEventQueueDelegate.invoke(swingUtilities3Class, 
                    EventQueueDelegateFactory.getObjectMap(this));
            registered = true;
        } catch (ClassNotFoundException ignore) {
        } catch (SecurityException ignore) {
        } catch (NoSuchMethodException ignore) {
        } catch (IllegalArgumentException ignore) {
        } catch (IllegalAccessException ignore) {
        } catch (InvocationTargetException ignore) {
        }
        if (! registered) {
            EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
            eq.push(this); 
        }
    }
    
    /**
     * we are using this class so we do not try to load Callable class if
     * it is not available.
     */ 
    private static class EventQueueDelegateFactory {
        static Map<String, Map<String, Object>> getObjectMap(
                final SGEventQueue delegate) {
              Map<String, Map<String, Object>> objectMap =
                  new HashMap<String, Map<String, Object>>();
              Map<String, Object> methodMap;

              final AWTEvent[] afterDispatchEventArgument = new AWTEvent[1];
              final Object[] afterDispatchHandleArgument = new Object[1];
              Callable<Void> afterDispatchCallable =
                  new Callable<Void>() {
                      @Override
					public Void call() {
                          delegate.afterDispatch(afterDispatchEventArgument[0],
                                                 afterDispatchHandleArgument[0]);
                          return null;
                      }
                  };
              methodMap = new HashMap<String, Object>();
              methodMap.put("event", afterDispatchEventArgument);
              methodMap.put("handle", afterDispatchHandleArgument);
              methodMap.put("method", afterDispatchCallable);
              objectMap.put("afterDispatch", methodMap);

              final AWTEvent[] beforeDispatchEventArgument = new AWTEvent[1];
              Callable<Object> beforeDispatchCallable =
                  new Callable<Object>() {
                      @Override
					public Object call() {
                          return delegate.beforeDispatch(
                              beforeDispatchEventArgument[0]);
                      }
                  };
              methodMap = new HashMap<String, Object>();
              methodMap.put("event", beforeDispatchEventArgument);
              methodMap.put("method", beforeDispatchCallable);
              objectMap.put("beforeDispatch", methodMap);

              final EventQueue[] getNextEventEventQueueArgument = new EventQueue[1];
              Callable<AWTEvent> getNextEventCallable =
                  new Callable<AWTEvent>() {
                      @Override
					public AWTEvent call() throws Exception {
                          return delegate.getNextEvent(
                              getNextEventEventQueueArgument[0]);
                      }
                  };
              methodMap = new HashMap<String, Object>();
              methodMap.put("eventQueue", getNextEventEventQueueArgument);
              methodMap.put("method", getNextEventCallable);
              objectMap.put("getNextEvent", methodMap);

              return objectMap;
          }
    }
}
