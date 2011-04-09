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

import java.util.Iterator;

final class Schedule extends AnimationList<Animation, Schedule>
    implements ScheduledAnimation
{
    public Schedule() {
    }

    /** Creates a new instance of ScheduleList */
    private Schedule(Animation anim, long tsched) {
        super(anim, tsched);
    }
    
    @Override
	protected Schedule makeEntry(Animation anim, long tsched) {
        return new Schedule(anim, tsched);
    }

    public final void scheduleRelativeTo(long when) {
        Schedule cur = next;
        while (cur != null) {
            cur.v.startAt(when+cur.t);
            cur = cur.next;
        }
    }

    void transferUpTo(long trel, long tend, RunQueue runq) {
        Schedule cur = next;
        while (cur != null) {
            long tstart = trel + cur.t;
            if (tstart <= tend) {
                cur.v.scheduleTo(tstart, tend, runq);
            }
            cur = cur.next;
        }
    }

    public final Iterable<Schedule> entries(final Animation runCheck) {
        return new Iterable<Schedule>() {
            @Override
			public Iterator<Schedule> iterator() {
                return Schedule.this.iterator(runCheck);
            }
        };
    }
}
