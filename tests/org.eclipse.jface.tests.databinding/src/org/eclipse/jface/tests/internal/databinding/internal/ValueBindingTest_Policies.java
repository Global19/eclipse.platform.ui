/*******************************************************************************
 * Copyright (c) 2007 Brad Reynolds and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Brad Reynolds - initial API and implementation
 ******************************************************************************/

package org.eclipse.jface.tests.internal.databinding.internal;

import org.eclipse.core.databinding.BindSpec;
import org.eclipse.core.databinding.BindingEvent;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.internal.databinding.ValueBinding;
import org.eclipse.jface.tests.databinding.AbstractDefaultRealmTestCase;
import org.eclipse.jface.tests.internal.databinding.internal.Pipeline.TrackLastListener;

/**
 * Asserts the policies of ValueBinding.
 * 
 * @since 1.1
 */
public class ValueBindingTest_Policies extends AbstractDefaultRealmTestCase {
	private WritableValue target;
	private WritableValue model;
	private DataBindingContext dbc;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();

		target = WritableValue.withValueType(String.class);
		model = WritableValue.withValueType(String.class);
		dbc = new DataBindingContext();
	}

	public void testModelUpdatePolicyNull() throws Exception {
		new ValueBinding(target, model, new BindSpec()
				.setModelUpdatePolicy(null)).init(dbc);
		target.setValue("1");
		assertEquals("should be automatic", target.getValue(), model.getValue());
	}

	public void testModelUpdatePolicyAutomatic() throws Exception {
		ValueBinding binding = new ValueBinding(target, model, new BindSpec()
				.setModelUpdatePolicy(BindSpec.POLICY_AUTOMATIC));
		binding.init(dbc);
		assertEquals(BindingEvent.PIPELINE_AFTER_CHANGE, binding.getTargetChangeModelPipelinePosition().intValue());
	}

	public void testModelUpdatePolicyExplicit() throws Exception {
		int position = BindingEvent.PIPELINE_AFTER_GET;
		ValueBinding binding = new ValueBinding(target, model, new BindSpec()
				.setModelUpdatePolicy(BindSpec.POLICY_EXPLICIT).setTargetValidatePolicy(new Integer(position)));
		binding.init(dbc);

		assertEquals(position, binding.getTargetChangeModelPipelinePosition().intValue());
	}
	
	public void testModelUpdatePolicyExplicitDefault() throws Exception {
		ValueBinding binding = new ValueBinding(target, model, new BindSpec()
				.setModelUpdatePolicy(BindSpec.POLICY_EXPLICIT));
		binding.init(dbc);

		assertEquals(BindingEvent.PIPELINE_AFTER_CONVERT, binding.getTargetChangeModelPipelinePosition().intValue());
	}

	public void testTargetUpdatePolicyNull() throws Exception {
		new ValueBinding(target, model, new BindSpec()
				.setTargetUpdatePolicy(null)).init(dbc);
		model.setValue("1");
		assertEquals("should be automatic", model.getValue(), target.getValue());
	}

	public void testTargetUpdatePolicyAutomatic() throws Exception {
		new ValueBinding(target, model, new BindSpec()
				.setTargetUpdatePolicy(BindSpec.POLICY_AUTOMATIC)).init(dbc);
		model.setValue("1");
		assertEquals("should be automatic", model.getValue(), target.getValue());
	}

	public void testTargetUpdatePolicyDefault() throws Exception {
		TrackLastListener listener = new TrackLastListener();
		listener.lastPosition = -1;
		
		ValueBinding binding = new ValueBinding(target, model,
				new BindSpec().setTargetUpdatePolicy(BindSpec.POLICY_EXPLICIT));
		binding.init(dbc);
		binding.addBindingEventListener(listener);

		model.setValue("1");
		assertFalse(model.getValue().equals(target.getValue()));
		assertEquals("last position should be PIPELINE_AFTER_CONVERT",
				BindingEvent.PIPELINE_AFTER_CONVERT, listener.lastPosition);

		binding.updateTargetToModel();
		assertEquals(target.getValue(), model.getValue());
	}

	public void testTargetValidatePolicy() throws Exception {
		int position = BindingEvent.PIPELINE_AFTER_GET;
		TrackLastListener listener = new TrackLastListener();

		ValueBinding binding = new ValueBinding(target, model, new BindSpec()
				.setModelUpdatePolicy(BindSpec.POLICY_EXPLICIT)
				.setTargetValidatePolicy(new Integer(position)));
		binding.init(dbc);
		binding.addBindingEventListener(listener);

		String value = "1";
		target.setValue(value);

		assertFalse(target.getValue().equals(model.getValue()));
		assertEquals("last position", position, listener.lastPosition);
	}
}
