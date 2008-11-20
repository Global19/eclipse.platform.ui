/*******************************************************************************
 * Copyright (c) 2008 Angelo Zerr and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.css.swt.properties.css2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.e4.ui.css.core.dom.properties.Gradient;
import org.eclipse.e4.ui.css.core.dom.properties.css2.AbstractCSSPropertyBackgroundHandler;
import org.eclipse.e4.ui.css.core.dom.properties.css2.ICSSPropertyBackgroundHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.SWTElementHelpers;
import org.eclipse.e4.ui.css.swt.properties.GradientBackgroundListener;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.w3c.dom.css.CSSValue;

public class CSSPropertyBackgroundSWTHandler extends
		AbstractCSSPropertyBackgroundHandler {

	private static Log logger = LogFactory
			.getLog(CSSPropertyBackgroundSWTHandler.class);

	public final static ICSSPropertyBackgroundHandler INSTANCE = new CSSPropertyBackgroundSWTHandler();

	public boolean applyCSSProperty(Object element, String property,
			CSSValue value, String pseudo, CSSEngine engine) throws Exception {
		Control control = SWTElementHelpers.getControl(element);
		if (control != null) {
			super.applyCSSProperty(control, property, value, pseudo, engine);
			return true;
		}
		return false;

	}

	public String retrieveCSSProperty(Object element, String property,
			CSSEngine engine) throws Exception {
		Control control = SWTElementHelpers.getControl(element);
		if (control != null) {
			return super.retrieveCSSProperty(control, property, engine);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.e4.ui.core.css.dom.properties.css2.AbstractCSSPropertyBackgroundHandler#applyCSSPropertyBackgroundColor(java.lang.Object,
	 *      org.w3c.dom.css.CSSValue, java.lang.String,
	 *      org.eclipse.e4.ui.core.css.engine.CSSEngine)
	 */
	public void applyCSSPropertyBackgroundColor(Object element, CSSValue value,
			String pseudo, CSSEngine engine) throws Exception {
		Control control = (Control) element;
		if (value.getCssValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
			Color newColor = (Color) engine.convert(value, Color.class, control
					.getDisplay());
			if (control instanceof CTabFolder && "selected".equals(pseudo)) {
				((CTabFolder) control).setSelectionBackground(newColor);
			} else {
				control.setBackground(newColor);
			}
		} else if (value.getCssValueType() == CSSValue.CSS_VALUE_LIST) {
			Gradient grad = (Gradient) engine.convert(value, Gradient.class,
					control.getDisplay());
			GradientBackgroundListener.handle(control, grad);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.e4.ui.core.css.dom.properties.css2.AbstractCSSPropertyBackgroundHandler#applyCSSPropertyBackgroundImage(java.lang.Object,
	 *      org.w3c.dom.css.CSSValue, java.lang.String,
	 *      org.eclipse.e4.ui.core.css.engine.CSSEngine)
	 */
	public void applyCSSPropertyBackgroundImage(Object element, CSSValue value,
			String pseudo, CSSEngine engine) throws Exception {
		Control control = (Control) element;
		Image image = (Image) engine.convert(value, Image.class, control
				.getDisplay());
		if (control instanceof CTabFolder && "selected".equals(pseudo)) {
			((CTabFolder) control).setSelectionBackground(image);
		} else if (control instanceof Button) {
			Button button = ((Button) control);
			// Image oldImage = button.getImage();
			// if (oldImage != null)
			// oldImage.dispose();
			button.setImage(image);
		} else {
			try {
				control.setBackgroundImage(image);
			} catch (Throwable e) {
				if (logger.isWarnEnabled())
					logger
							.warn("Impossible to manage backround-image, This SWT version doesn't support control.setBackgroundImage(Image image) Method");
			}
		}
	}

	public String retrieveCSSPropertyBackgroundAttachment(Object widget,
			CSSEngine engine) throws Exception {
		return null;
	}

	public String retrieveCSSPropertyBackgroundColor(Object element,
			CSSEngine engine) throws Exception {
		Control control = (Control) element;
		Color color = control.getBackground();
		return engine.convert(color, Color.class, null);
	}

	public String retrieveCSSPropertyBackgroundImage(Object widget,
			CSSEngine engine) throws Exception {
		// TODO : manage path of Image.
		return "none";
	}

	public String retrieveCSSPropertyBackgroundPosition(Object widget,
			CSSEngine engine) throws Exception {
		return null;
	}

	public String retrieveCSSPropertyBackgroundRepeat(Object widget,
			CSSEngine engine) throws Exception {
		return null;
	}
}
