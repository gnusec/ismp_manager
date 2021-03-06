/*
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2004-2006 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a derivative work, containing both original code, included code and modified
 * code that was published under the GNU General Public License. Copyrights for modified
 * and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Modifications:
 * 
 * Created December 31, 2004
 *
 * Copyright (C) 2004-2006 The OpenNMS Group, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 *      OpenNMS Licensing       <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 */
package org.infosec.ismp.poller.pollable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.infosec.ismp.model.poller.PollStatus;

/**
 * Represents a PollableContainer 
 *
 * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
 */
abstract public class PollableContainer extends PollableElement {

	private final Map<Object, PollableElement> m_members = new HashMap<Object, PollableElement>();

	public PollableContainer(PollableContainer parent, Scope scope) {
		super(parent, scope);
	}

	/**
	 * @param integer
	 * @return
	 */
	protected synchronized PollableElement getMember(Object key) {
		return m_members.get(key);
	}

	protected synchronized int getMemberCount() {
		return m_members.size();
	}

	protected synchronized Collection<PollableElement> getMembers() {
		return new ArrayList<PollableElement>(m_members.values());
	}

	/**
	 * @param member
	 * @return
	 */
	abstract protected Object createMemberKey(PollableElement member);

	/**
	 * @param node
	 */
	protected synchronized void addMember(PollableElement member) {
		Object key = createMemberKey(member);
		m_members.put(key, member);
	}

	public synchronized void removeMember(PollableElement member) {
		Object key = createMemberKey(member);
		m_members.remove(key);
	}

	public void deleteMember(PollableElement member) {
		removeMember(member);
		if (m_members.size() == 0)
			this.delete();
	}

	@Override
	public void delete() {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				Collection<PollableElement> members = getMembers();
				for (Iterator<PollableElement> it = members.iterator(); it
						.hasNext();) {
					PollableElement member = it.next();
					member.delete();
				}
				PollableContainer.super.delete();
			}
		};
		withTreeLock(r);

	}

	@Override
	public void visit(PollableVisitor v) {
		visitThis(v);
		visitMembers(v);
	}

	@Override
	protected void visitThis(PollableVisitor v) {
		super.visitThis(v);
		v.visitContainer(this);
	}

	/**
	 * @param v
	 */
	protected void visitMembers(PollableVisitor v) {
		for (Iterator<PollableElement> it = getMembers().iterator(); it
				.hasNext();) {
			PollableElement element = it.next();
			element.visit(v);
		}

	}

	protected interface Iter {
		public void forEachElement(PollableElement element);
	}

	abstract protected class SimpleIter<T> implements Iter {
		private T result;

		public SimpleIter(T initial) {
			result = initial;
		}

		public SimpleIter() {
			this(null);
		}

		public T getResult() {
			return result;
		}

		public void setResult(T newResult) {
			result = newResult;
		}
	}

	abstract protected class Accumulator<T> extends SimpleIter<T> {
		public Accumulator(T initial) {
			super(initial);
		}

		public Accumulator() {
			super(null);
		}

		@Override
		public void forEachElement(PollableElement element) {
			setResult(processNextMember(element, getResult()));
		}

		abstract T processNextMember(PollableElement member, T currentValue);
	}

	protected void forEachMember(Iter iter) {
		forEachMember(false, iter);
	}

	protected <T> T deriveValueFromMembers(SimpleIter<T> iter) {
		return deriveValueFromMembers(false, iter);
	}

	protected <T> T deriveValueFromMembers(boolean withTreeLock,
			SimpleIter<T> iter) {
		forEachMember(withTreeLock, iter);
		return iter.getResult();
	}

	protected void forEachMember(boolean withTreeLock, final Iter iter) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				for (Iterator<PollableElement> it = getMembers().iterator(); it
						.hasNext();) {
					PollableElement element = it.next();
					iter.forEachElement(element);
				}
			}
		};

		if (withTreeLock) {
			withTreeLock(r);
		} else {
			r.run();
		}
	}

	@Override
	public void recalculateStatus() {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				SimpleIter<PollStatus> iter = new SimpleIter<PollStatus>(
						PollStatus.down()) {
					@Override
					public void forEachElement(PollableElement elem) {
						elem.recalculateStatus();
						if (elem.getStatus().isUp())
							setResult(PollStatus.up());
					}
				};
				forEachMember(iter);
				updateStatus(iter.getResult());
			}
		};
		withTreeLock(r);
	}

	@Override
	public void resetStatusChanged() {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				PollableContainer.super.resetStatusChanged();
				Iter iter = new Iter() {
					@Override
					public void forEachElement(PollableElement elem) {
						elem.resetStatusChanged();
					}
				};
				forEachMember(iter);
			}
		};
		withTreeLock(r);
	}

	PollableElement findMemberWithDescendent(PollableElement elem) {
		PollableElement member = elem;
		while (member != null && member.getParent() != this) {
			member = member.getParent();
		}
		return member;
	}

	@Override
	protected PollStatus poll(final PollableElement elem) {
		final PollStatus retVal[] = new PollStatus[1];
		Runnable r = new Runnable() {
			@Override
			public void run() {
				PollableElement member = findMemberWithDescendent(elem);
				PollStatus memberStatus = member.poll(elem);
				if (memberStatus.isUp() != getStatus().isUp()
						&& member.isStatusChanged()) {
					updateStatus(pollRemainingMembers(member));
				}
				retVal[0] = getStatus();
			}
		};
		elem.withTreeLock(r);
		return retVal[0];
	}

	/**
	 * @param member
	 * @return
	 */
	public PollStatus pollRemainingMembers(final PollableElement member) {
		SimpleIter<PollStatus> iter = new SimpleIter<PollStatus>(
				member.getStatus()) {
			@Override
			public void forEachElement(PollableElement elem) {
				if (elem != member) {
					if (elem.poll().isUp())
						setResult(PollStatus.up());
				}
			}
		};
		forEachMember(iter);
		return iter.getResult();
	}

	public PollStatus getMemberStatus() {
		SimpleIter<PollStatus> iter = new SimpleIter<PollStatus>(
				PollStatus.down()) {
			@Override
			public void forEachElement(PollableElement elem) {
				if (elem.getStatus().isUp())
					setResult(PollStatus.up());
			}

		};
		forEachMember(iter);
		return iter.getResult();
	}

	@Override
	public PollStatus poll() {
		PollableElement leaf = selectPollElement();
		if (leaf == null)
			return PollStatus.up();
		return poll(leaf);
	}

	/**
	 * @return
	 */
	@Override
	public PollableElement selectPollElement() {
		if (getMemberCount() == 0)
			return null;

		PollableElement member = getMembers().iterator().next();
		return member.selectPollElement();

	}

	@Override
	public void processStatusChange(final Date date) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				if (isStatusChanged()) {
					PollableContainer.super.processStatusChange(date);
				} else if (getStatus().isUp()) {
					processMemberStatusChanges(date);
				}
			}
		};
		withTreeLock(r);

	}

	public void processMemberStatusChanges(final Date date) {
		Iter iter = new Iter() {
			@Override
			public void forEachElement(PollableElement elem) {
				elem.processStatusChange(date);
			}

		};
		forEachMember(iter);
	}

	@Override
	protected void processResolution(PollEvent resolvedCause,
			PollEvent resolution) {
		super.processResolution(resolvedCause, resolution);
		processLingeringMemberCauses(resolvedCause, resolution);
	}

	private void processLingeringMemberCauses(final PollEvent resolvedCause,
			final PollEvent resolution) {
		Iter iter = new Iter() {
			@Override
			public void forEachElement(PollableElement elem) {
				elem.processLingeringCauses(resolvedCause, resolution);
			}

		};
		forEachMember(iter);
	}

	@Override
	protected void processCause(final PollEvent cause) {
		super.processCause(cause);
		Iter iter = new Iter() {
			@Override
			public void forEachElement(PollableElement elem) {
				elem.processCause(cause);
			}

		};
		forEachMember(iter);
	}

	@Override
	protected void resolveAllOutages(final PollEvent resolvedCause,
			final PollEvent resolution) {
		super.resolveAllOutages(resolvedCause, resolution);
		Iter iter = new Iter() {
			@Override
			public void forEachElement(PollableElement elem) {
				if (!hasOpenOutage())
					elem.resolveAllOutages(resolvedCause, resolution);
			}

		};
		forEachMember(iter);
	}

	@Override
	protected PollEvent doExtrapolateCause() {

		// find the member cause with the largest scope
		PollEvent cause = extrapolateMemberCauseWithLargestScope();

		// use this largest scope as the cause for the container
		setCause(cause);
		if (cause != null) {
			// we must be down set set status appropriately
			updateStatus(PollStatus.down());
		}

		// return the cause to parent container
		return cause;
	}

	private PollEvent extrapolateMemberCauseWithLargestScope() {
		PollEvent cause = null;
		for (PollableElement member : getMembers()) {
			PollEvent memberCause = member.extrapolateCause();
			if (memberCause != null
					&& !memberCause.hasScopeSmallerThan(getScope())) {
				// a cause has been found that exceeds the scope of the members
				// choose between the current scope and the newly found scope be
				// taking
				// the cause with the largest scope
				cause = PollEvent.withLargestScope(cause, memberCause);

			}
		}
		return cause;
	}

	@Override
	protected void doInheritParentalCause() {
		super.doInheritParentalCause();
		for (PollableElement member : getMembers()) {
			member.inheritParentalCause();
		}

	}

}
