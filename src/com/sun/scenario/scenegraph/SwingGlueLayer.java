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

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JComponent;
import javax.swing.PopupFactory;

/**
 * !!!! WARNING !!!! 
 * This class is implementation specific. It should not be used outside of 
 * scenario project.
 *   
 * Initialize swing glue layer: RepaintManager and what not.    
 * 
 * @author Igor Kushnirskiy
 */
public class SwingGlueLayer {
    private static final SwingGlueLayer instance;
    private static final  Class<?> repaintManagerClass;
    
    private final Object repaintManager;
    private final SGEventQueue eventQueue;
    static {
        Class<?> repaintManagerClassTmp = null;
        try {
            repaintManagerClassTmp = Class.forName("javax.swing.RepaintManager");
        } catch (ClassNotFoundException ignore) {
        } catch (SecurityException ignore) {
        }
        repaintManagerClass = repaintManagerClassTmp;
        instance = new SwingGlueLayer();
    }
    public SwingGlueLayer() {
        repaintManager = (repaintManagerClass != null) 
          ? RepaintManagerRegister.createRepaintManager()
          : null;
        eventQueue = new SGEventQueue();
        
        EventQueue.invokeLater(new Runnable() {
            @Override
			public void run() {
                PopupFactory.setSharedInstance(new SGPopupFactory());
                eventQueue.register();
            }
        });

    }
    public static SwingGlueLayer getSwingGlueLayer() {
        return instance;
    }
    
    void registerRepaintManager(JComponent c) {
        if (repaintManager == null) {
            return;
        } else {
            RepaintManagerRegister.registerRepaintManager(
                    c, repaintManager);
        }
    }

    //this method may be called off EDT
    public void setAnimationRunnable(DelayedRunnable animationRunnable) {
        eventQueue.setAnimationRunnable(animationRunnable);
    }
    
    /**
     * we are using this class so we do not try to load RepaintManager class if
     * it is not available.
     */ 
    private static class RepaintManagerRegister {
        private static final Method setDelegateRepaintManagerMethod;
        private static final Class<?> swingUtilities3Class;
        static {
            Class<?> swingUtilities3ClassTmp = null;
            Method setDelegateRepaintManagerMethodTmp = null;
            try {
                swingUtilities3ClassTmp = 
                    Class.forName("com.sun.java.swing.SwingUtilities3");
                setDelegateRepaintManagerMethodTmp = 
                    swingUtilities3ClassTmp.getMethod("setDelegateRepaintManager",
                            JComponent.class, repaintManagerClass); 
            } catch (ClassNotFoundException ignore) {
            } catch (SecurityException ignore) {
            } catch (NoSuchMethodException ignore) {
            }
            if (swingUtilities3ClassTmp == null
                    || setDelegateRepaintManagerMethodTmp == null) {
                swingUtilities3ClassTmp = null;
                setDelegateRepaintManagerMethodTmp = null;
            }
            swingUtilities3Class = swingUtilities3ClassTmp;
            setDelegateRepaintManagerMethod = setDelegateRepaintManagerMethodTmp;
        }
        static Object createRepaintManager() {
            return new SGComponentRepaintManager(null);
        }
        static void registerRepaintManager(JComponent c, Object repaintManager) {
            if (setDelegateRepaintManagerMethod == null) {
                /* 
                 * do not have SwingUtilities3.setDelegateRepaintManager.
                 * may need to register wrapper RepaintManager
                 */
                javax.swing.RepaintManager manager = 
                    javax.swing.RepaintManager.currentManager(null);
                if (manager != repaintManager) {
                    ((SGComponentRepaintManager) repaintManager).setDelegate(
                            manager);
                    javax.swing.RepaintManager.setCurrentManager(
                            (SGComponentRepaintManager) repaintManager);
                }
                return;
            } else {
                try {
                    setDelegateRepaintManagerMethod.invoke(
                            swingUtilities3Class, c, repaintManager);
                } catch (IllegalArgumentException ignore) {
                } catch (IllegalAccessException ignore) {
                } catch (InvocationTargetException ignore) {
                }
            }
        }
    }
    
    public static interface DelayedRunnable extends Runnable {
        
        /**
         * @return delay in millis
         */
        public long getDelay();
    }
}
