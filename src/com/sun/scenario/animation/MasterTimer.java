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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import com.sun.scenario.animation.Animation.Status;
import com.sun.scenario.scenegraph.SwingGlueLayer;
import com.sun.scenario.scenegraph.SwingGlueLayer.DelayedRunnable;
import com.sun.scenario.settings.Settings;

/**
 * This class encapsulates the global static methods that manage scheduling
 * and actual running of animations against real wall clock time.
 * It only deals in absolute time values - all relative times that are
 * specified in the {@link Clip} and {@link Timeline} classes will need
 * to be turned into absolute times when the {@code Clip} and {@code Timeline}
 * objects are started.
 *
 * For now it is hidden until we have some use to expose it.
 */
final class MasterTimer {
    private static MainLoop theMaster;
    private static boolean useNanoTime;
    
    private final static String NOGAPS_PROP = "com.sun.scenario.animation.nogaps";
    private static boolean nogaps = false;
    
    private final static String FULLSPEED_PROP = "com.sun.scenario.animation.fullspeed";    
    private static boolean fullspeed = false;

    // This PropertyChangeListener is added to Settings to listen for changes
    // to the nogap and fullspeed properties.
    private static PropertyChangeListener pcl = new PropertyChangeListener() {
        @Override
		public void propertyChange(PropertyChangeEvent event) {
            if (event.getPropertyName().equals(NOGAPS_PROP)) {
                nogaps = Settings.getBoolean(NOGAPS_PROP);
            }
            else if (event.getPropertyName().equals(FULLSPEED_PROP)) {
                fullspeed = Settings.getBoolean(FULLSPEED_PROP);
            }
        }
    };
    
    static {
        nogaps = Settings.getBoolean(NOGAPS_PROP);
        fullspeed = Settings.getBoolean(FULLSPEED_PROP);

        Settings.addPropertyChangeListener(NOGAPS_PROP, pcl);
        Settings.addPropertyChangeListener(FULLSPEED_PROP, pcl);

        try {
            System.nanoTime();
            useNanoTime = true;
        } catch (NoSuchMethodError e) {
        }
        
        theMaster = new MainLoop();
    }

    /** Prevent instantiation of MasterTimer */
    private MasterTimer() {
    }

    /**
     * Returns the current time in milliseconds using the most accurate
     * timing method available.
     */
    static long milliTime() {
        if (useNanoTime) {
            return System.nanoTime() / 1000 / 1000;
        } else {
            return System.currentTimeMillis();
        }
    }

    // MULTI-THREADED VARIABLES:

    // synchronize on waitList for access
    private static RunQueue waitList = new RunQueue();
    private static RunQueue pauseWaitList = new RunQueue();

    // static synchronize to update frameJobList and frameJobs
    private static ArrayList<FrameJob> frameJobList = new ArrayList<FrameJob>();
    private static final FrameJob emptyJobs[] = new FrameJob[0];
    private static FrameJob frameJobs[] = emptyJobs;  // frameJobList snapshot

    // THREAD-SAFE VARIABLES:

    // Only used by theMaster thread.
    private static RunQueue runList = new RunQueue();

    /**
     * Returns true if there are no Clips in any of the queues, whether
     * they are running, scheduled to run at a later time, or paused.
     *
     * @return true if there are no Clips to be processed
     */
    static boolean isIdle() {
        synchronized (waitList) {
            return (runList.isEmpty() &&
                    waitList.isEmpty() &&
                    pauseWaitList.isEmpty());
        }
    }

    /**
     * Adds a Clip target to the list of targets being tracked against
     * the global schedule.  The target should already have an absolute
     * start time recorded in it and that time will be used to start the
     * clip at the appropriate wall clock time as defined by milliTime().
     *
     * @param target the Clip to be added to the scheduling queue
     */
    static void addToRunQueue(Clip target, long tbegin) {
        synchronized (waitList) {
            // Remove it from the waiting run queues just in case it was
            // already added to prevent duplicate entries.
            waitList.remove(target);
            pauseWaitList.remove(target);
            waitList.insert(target, tbegin);
            theMaster.setClipsReady(true);
        }
    }

    /**
     * Removes a Clip target from the list of targets being tracked
     * against the global schedule.  If the target has already reached its
     * recorded start time and moved onto the list of active targets then
     * this method will have no effect.  In other words, it can prevent it
     * from reaching the begin() state, but can do nothing if it has already
     * reached that state.  The timePulse() method should return STOPPED or
     * CANCELED in that case to get the target off of the active list.
     *
     * @param target the Clip to be removed from the scheduling queue
     * @return true if the target never had its begin() method called.
     */
    static void removeFromRunQueue(Clip target) {
        synchronized (waitList) {
            waitList.remove(target);
            pauseWaitList.remove(target);
        }
    }

    /**
     * Adds a frame job to the list of jobs that is called at the end
     * of every frame.
     * REMIND: This isn't visible outside of the animation package.  Hmmm...
     */
    static synchronized void addFrameJob(FrameJob job) {
        frameJobList.add(job);
        // Snapshot new list to a brand new unsynchronized accessor array
        frameJobs = frameJobList.toArray(emptyJobs);
    }

    /**
     * Removes a frame job from the list of jobs that is called at the end
     * of every frame.
     * REMIND: This isn't visible outside of the animation package.  Hmmm...
     */
    static synchronized void removeFrameJob(FrameJob job) {
        if (frameJobList.remove(job)) {
            // Snapshot new list to a brand new unsynchronized accessor array
            frameJobs = frameJobList.toArray(emptyJobs);
        }
    }

    /**
     * Pauses a Clip on whatever run queue it currently lives on.
     */
    static void pause(Clip target) {
        synchronized (waitList) {
            // We process the waitList and runList while waitList is locked
            // so that items cannot move between the two while we scan them
            RunQueue rq = waitList.remove(target);
            if (rq != null) {
                pauseWaitList.prepend(rq);
            } else {
                rq = runList.find(target);
            }
            if (rq != null) {
                rq.pause(milliTime());
            }
        }
    }

    /**
     * Resumes a Clip and puts it back on the appropriate run queue
     */
    static void resume(Clip target) {
        synchronized (waitList) {
            // We process the waitList and runList while waitList is locked
            // so that items cannot move between the two while we scan them
            RunQueue rq = pauseWaitList.remove(target);
            if (rq != null) {
                // Adjust to a new start time and then return the item
                // to the regular wait list at the appropriate new time
                rq.resume(milliTime());
                waitList.insert(rq);
                theMaster.setClipsReady(true);
            } else {
                rq = runList.find(target);
                if (rq != null) {
                    rq.resume(milliTime());
                }
            }
        }
    }
    
    /**
     * Called by FrameJob.wakeUp() to indicate to the MasterTimer thread
     * that there is work to be done.
     */
    static void notifyJobsReady() {
        theMaster.setJobsReady(true);
    }

    /**
     * Hidden inner class to run the main timing loop.
     */
    private final static class MainLoop implements DelayedRunnable {
        /**
         * If true, indicates that there are animations waiting to be
         * processed; otherwise, this thread can sleep if there is no
         * other work pending.
         * Only use the setClipsReady() method to modify this variable.
         */
        private boolean clipsReady;

        /**
         * If true, indicates that there are one or more FrameJobs that
         * need to be processed; otherwise, this thread can sleep if there
         * is no other work pending.
         * Only use the setJobsReady() method to modify this variable.
         */
        private boolean jobsReady;
        
        private final int PULSE_DURATION = 1000 / 60;
        private long nextPulseTime = Integer.MIN_VALUE;
        public void run() {
            timePulse(milliTime());
            updateNextPulseTime();
        }
        
        public long getDelay() {
            if (nextPulseTime == Integer.MIN_VALUE) {
                updateNextPulseTime();
            }
            long timeUntilPulse = nextPulseTime - milliTime();
            return Math.max(0, timeUntilPulse);
        }
        
        private void updateNextPulseTime() {
            nextPulseTime = milliTime();
            if (! fullspeed) {
                nextPulseTime = 
                    ((nextPulseTime + PULSE_DURATION - 1) / PULSE_DURATION) 
                    * PULSE_DURATION;
            }            
        }
        
        synchronized void setClipsReady(boolean clipsReady) {
            if (this.clipsReady != clipsReady) {
                this.clipsReady = clipsReady;
                updateAnimationRunnable();
            }
        }
        
        synchronized void setJobsReady(boolean jobsReady) {
            if (this.jobsReady != jobsReady) {
                this.jobsReady = jobsReady;
                updateAnimationRunnable();
            }
        }
        
        synchronized private void updateAnimationRunnable() {
            DelayedRunnable animationRunnable = null;
            if (jobsReady || clipsReady) {
                animationRunnable = this;
            }
            SwingGlueLayer.getSwingGlueLayer()
              .setAnimationRunnable(animationRunnable);
        }
    }

    /**
     * Process all runList and waitList entries that should be
     * active for the specified clock tick.
     *
     * @param now the absolute time of the current clock tick
     */
    static void timePulse(long now) {
        // First see if there is any work to be done
        if (runList.isEmpty()) {
            synchronized (waitList) {
                if (waitList.isEmpty()) {
                    // No more clips to process
                    theMaster.setClipsReady(false);
                } else if (nogaps) {
                    long nextTrigger = waitList.next.getTime();
                    if (now < nextTrigger) {
                        long delta = now-nextTrigger;
                        for (RunQueue target : waitList) {
                            target.adjustStartTime(delta);
                        }
                    }
                }
            }
        }

        // Next look for inactive targets becoming active
        RunQueue prev = runList;
        RunQueue cur = prev.next;
        do {
            while (cur != null) {
                if (process(cur, now)) {
                    prev.next = cur.next;
                } else {
                    prev = cur;
                }
                cur = prev.next;
            }
            // End of the list - grab any newly activated clips
            if (waitList.next != null) {
                appendStartingClips(prev, now);
                cur = prev.next;
            }
        } while (cur != null);

        // After every frame, call any frame jobs
        theMaster.setJobsReady(false);
        FrameJob jobSnapshot[] = frameJobs;
        for (FrameJob job : jobSnapshot) {
            try {
                job.run();
            } catch (Throwable t) {
                System.err.println("Unexpected exception caught in MasterTimer.timePulse():");
                t.printStackTrace();
                removeFrameJob(job);
            }
        }
    }

    /**
     * Scan the waitList for clips that had reached their start
     * time on or before this clock tick started and put them on
     * the tail of the runList.
     *
     * @param runTail the current last entry on the runList queue
     *                (never null)
     * @param now the time that this clock tick started
     */
    private static void appendStartingClips(RunQueue runTail, long now) {
        // assert(runTail.next == null);
        synchronized (waitList) {
            RunQueue prev = null;
            RunQueue cur = waitList.next;
            while (cur != null && cur.getTime() <= now) {
                prev = cur;
                cur = cur.next;
            }
            if (prev != null) {
                // Move [waitList ... cur] to tail of runList
                // and move [cur ... end] up to top of waitList
                prev.next = null;
                runTail.next = waitList.next;
                waitList.next = cur;
            }
        }
    }

    /**
     * Process one entry in the runList queue at the specified clock tick.
     *
     * @param cur the runList entry to be processed
     * @param now the absolute time of the clock tick to process
     * @return true if the entry should be removed from the runList
     */
    private static boolean process(RunQueue cur, long now) {
        Clip c = cur.getAnimation();
        Status status = cur.getStatus();
        try {
            while (true) {
                switch (status) {
                    case RUNNING:
                        status = c.timePulse(now - cur.getTime());
                        if (status == Status.RUNNING) {
                            return false;
                        }
                        break;
                    case SCHEDULED:
                        // Not yet begun, deal with "begin" dependents
                        // Note that dependents start relative to when
                        // this Clip should have started in an ideal world
                        c.begin();
                        c.scheduleBeginAnimations(cur.getTime());
                        status = cur.began();  // Sets appropriate new status
                        break;
                    case PAUSED:
                    case SCHEDULEPAUSED:
                        return false;
                    case STOPPED:
                        // deal with this clip's "end" dependents, if any
                        // Calling method will remove it from the list
                        // REMIND: Would be more correct to calculate the
                        // start time based on the actual time that c
                        // should have ended in an ideal world
                        c.end();
                        c.scheduleEndAnimations(now);
                        // NO BREAK
                    case CANCELED:
                        return true;
                }
            }
        } catch (Throwable t) {
            System.err.println("Unexpected exception caught in MasterTimer.process():");
            t.printStackTrace();
            return true;
        }
    }
}
