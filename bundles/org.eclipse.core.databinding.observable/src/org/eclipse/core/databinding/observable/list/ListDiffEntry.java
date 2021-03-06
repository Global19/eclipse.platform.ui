/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stefan Xenos <sxenos@gmail.com> - Bug 335792
 ******************************************************************************/

package org.eclipse.core.databinding.observable.list;

/**
 * A single addition of an element to a list or removal of an element from a
 * list.
 *
 * @param <E>
 *            the type of the elements in this diff entry
 *
 * @since 1.0
 */
public abstract class ListDiffEntry<E> {

	/**
	 * @return the 0-based position of the addition or removal
	 */
	public abstract int getPosition();

	/**
	 * @return true if this represents an addition, false if this represents a
	 *         removal
	 */
	public abstract boolean isAddition();

	/**
	 * @return the element that was added or removed
	 */
	public abstract E getElement();

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer
			.append(this.getClass().getName())
			.append("{position [") //$NON-NLS-1$
			.append(getPosition())
			.append("], isAddition [") //$NON-NLS-1$
			.append(isAddition())
			.append("], element [") //$NON-NLS-1$
			.append(getElement() != null ? getElement().toString() : "null") //$NON-NLS-1$
			.append("]}"); //$NON-NLS-1$

		return buffer.toString();
	}
}
