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

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;


/* A Logger class for mobile platforms that don't provide
 * the java.util.logging API.
 */
abstract class Logger {
    public enum Level {MESSAGE, WARNING, ERROR};
    private static final Map<String, Logger> loggerMap = new HashMap<String, Logger>();
    private static Constructor loggerCtor = null;

    private static Logger createLogger(String name) {

        try {
            if (loggerCtor != null) {
                return (Logger)loggerCtor.newInstance(name);
            } else {
                try {
                    loggerCtor = UtilLogger.class.getConstructor(String.class);
                    return (Logger)loggerCtor.newInstance(name);
                }
                catch (Exception e) {
                    loggerCtor = DefaultLogger.class.getConstructor(String.class);
                    return (Logger)loggerCtor.newInstance(name);
                }
            }
        }
        catch(Exception e) {
            throw new Error(e);
        }
    }

    public static synchronized Logger getLogger(String name) {
        Logger logger = loggerMap.get(name);
        if (logger == null) {
            logger = createLogger(name);
            loggerMap.put(name, logger);
        }
        return logger;
    }

    public abstract boolean isEnabled(Level l); 
    public abstract void setEnabled(Level l); 
    public abstract void message(String format, Object... args);
    public abstract void warning(Exception e, String format, Object... args);
    public abstract void error(Exception e, String format, Object... args);

    public final void warning(String format, Object... args) {
        warning(null, format, args);
    }

    public final void error(String format, Object... args) {
        error(null, format, args);
    }


    private static class DefaultLogger extends Logger {
        private Level level = Level.WARNING;

        @Override
		public final boolean isEnabled(Level l) {
            return l.compareTo(level) >= 0;
        }

        @Override
		public final void setEnabled(Level l) { 
            level = l; 
        }
        
        public DefaultLogger(String name) { }

        private void log(Level l, PrintStream p, Exception e, String format, Object[] args) {
            if (l.compareTo(level) >= 0) {
                if (e != null) {
                    e.printStackTrace(p);
                }
                // p.println(String.format(format, args)); No String#format or
                // PrintStream#format on FX Mobile, CDC
                p.print(format);
                for(Object a : args) { p.print(" " + a); }
                p.println();
            }
        }

        @Override
		public final void message(String format, Object... args) {
            log(Level.MESSAGE, System.out, null, format, args); 
        }

        @Override
		public final void warning(Exception e, String format, Object... args) {
            log(Level.WARNING, System.err, e, format, args); 
        }

        @Override
		public final void error(Exception e, String format, Object... args) {
            log(Level.ERROR, System.err, e, format, args); 
        }
    }


    private static class UtilLogger extends Logger {
        private final java.util.logging.Logger logger;

        public UtilLogger(String name) { 
            logger = java.util.logging.Logger.getLogger(name);
        }
        
        private java.util.logging.Level convertLevel(Level l) {
            switch(l) {
            case ERROR: return java.util.logging.Level.SEVERE;
            case WARNING: return java.util.logging.Level.WARNING;
            default: return java.util.logging.Level.ALL;
            }
        }

        @Override
		public final boolean isEnabled(Level l) {
            return logger.isLoggable(convertLevel(l));
        }

        @Override
		public final void setEnabled(Level l) { 
            logger.setLevel(convertLevel(l));
        }

        private void log(java.util.logging.Level l, Exception e, String format, Object[] args) {
            logger.log(l, String.format(format, args), e);
        }

        @Override
		public void message(String format, Object... args) {
            log(java.util.logging.Level.INFO, null, format, args);
        }

        @Override
		public void warning(Exception e, String format, Object... args) {
            log(java.util.logging.Level.WARNING, null, format, args);
        }

        @Override
		public void error(Exception e, String format, Object... args) {
            log(java.util.logging.Level.SEVERE, null, format, args);
        }
    }
}
