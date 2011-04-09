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

/**
 * A linked list of animations with timing information.
 * A single instance of a node is used as the head of the list and
 * then the insertion/removal methods are called on that "head" object.
 * List maintenance operations are unsynchronized - it is the responsibility
 * of the caller to synchronize on an object of choice to control
 * access to the list.
 * 
 * @param T the subclass of Animation which is being scheduled (usually
 *          Animation or Clip)
 * @param N the type of nodes in the chain (usually the type of the
 *          AnimationList subclass - Schedule or RunQueue)
 */
abstract class AnimationList<T extends Animation,
                             N extends AnimationList<T, N>>
    implements Iterable<N>
{
    protected long t;
    protected T v;
    N next;

    /** Creates a new instance of AnimationList */
    AnimationList() {
    }

    /** Creates a new instance of AnimationList */
    protected AnimationList(T v, long t) {
        this.v = v;
        this.t = t;
    }

    protected abstract N makeEntry(T v, long t);

    public final long getTime() {
        return t;
    }
    
    public final T getAnimation() {
        return v;
    }

    final boolean isEmpty() {
        return (next == null);
    }

    final void clear() {
        next = null;
    }

    final void insert(T v, long t) {
        insert(makeEntry(v, t));
    }

    final void insert(N entry) {
        AnimationList<?, N> prev = this;
        N cur;
        while ((cur = prev.next) != null) {
            if (entry.t < cur.t) {
                break;
            }
            prev = cur;
        }
        entry.next = cur;
        prev.next = entry;
    }

    final void append(T v, long t) {
        append(makeEntry(v, t));
    }

    final void append(N entry) {
        AnimationList<?, N> prev = this;
        N cur;
        while ((cur = prev.next) != null) {
            prev = cur;
        }
        prev.next = entry;
    }

    final void prepend(N entry) {
        entry.next = this.next;
        this.next = entry;
    }

    final N find(T v) {
        N cur = this.next;
        while (cur != null) {
            if (cur.v == v) {
                return cur;
            }
            cur = cur.next;
        }
        return null;
    }

    final N remove(T v) {
        AnimationList<?, N> prev = this;
        N cur;
        while ((cur = prev.next) != null) {
            if (cur.v == v) {
                prev.next = cur.next;
                cur.next = null;
                return cur;
            }
            prev = cur;
        }
        return null;
    }

    @Override
	public final Iterator<N> iterator() {
        // REMIND: This implementation uses an Iterator that allows
        // remove to be called.  The class is currently package private
        // so we can assume that the caller knows what they are doing,
        // but if we expose the class we might want to provide control
        // over when and if the remove() method is allowed.
        return iterator(null);
    }

    final Iterator<N> iterator(Animation runCheck) {
        return new LinkIter<N>(this, runCheck);
    }

    static final class LinkIter<NN extends AnimationList<?, NN>>
        extends BaseIter<NN>
        implements Iterator<NN>
    {
        public LinkIter(AnimationList<?, NN> head, Animation runCheck) {
            super(head, runCheck);
        }
        
        @Override
		public NN next() {
            return super.nextLink();
        }
    }

    final Iterable<T> animations(final Animation runCheck) {
        return new Iterable<T>() {
            @Override
			public Iterator<T> iterator() {
                return new AnimIter<T, N>(AnimationList.this, runCheck);
            }
        };
    }

    static final class AnimIter<TT extends Animation,
                                NN extends AnimationList<TT, NN>>
        extends BaseIter<NN>
        implements Iterator<TT>
    {
        public AnimIter(AnimationList<?, NN> head, Animation runCheck) {
            super(head, runCheck);
        }
        
        @Override
		public TT next() {
            return super.nextLink().getAnimation();
        }
    }

    static abstract class BaseIter<NN extends AnimationList<?, NN>>
    {
        AnimationList<?, NN> prev;
        AnimationList<?, NN> cur;
        Animation runCheck;

        public BaseIter(AnimationList<?, NN> head, Animation runCheck) {
            this.cur = head;
            this.runCheck = runCheck;
        }
        
        public boolean hasNext() {
            return (cur.next != null);
        }
        
        public NN nextLink() {
            prev = cur;
            NN ret = cur.next;
            cur = ret;
            return ret;
        }

        public void remove() {
            if (prev == null) {
                throw new IllegalStateException("no element to remove");
            }
            if (runCheck != null && runCheck.isRunning()) {
                throw new UnsupportedOperationException
                    ("cannot call remove while animation is running");
            }
            prev.next = cur.next;
            cur.next = null;
            cur = prev;
            prev = null;
        }
    }
}
