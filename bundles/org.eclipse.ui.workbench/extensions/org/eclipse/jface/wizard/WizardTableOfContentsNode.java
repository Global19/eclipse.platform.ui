package org.eclipse.jface.wizard;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * The WizardTableOfContentsNode is the class that represents 
 * each node in the table of contents.
 */
public class WizardTableOfContentsNode implements ITableOfContentsNode {

	/**
	 * Image registry key for decision image (value <code>"toc_decision_image"</code>).
	 */
	private static final String TOC_IMG_DECISION = "toc_decision_image"; //$NON-NLS-1$

	/**
	 * Image registry key for last page image (value <code>"toc_disabled_image"</code>).
	 */
	private static final String TOC_IMG_DISABLED = "toc_disabled_image"; //$NON-NLS-1$

	/**
	 * Image registry key for next image (value <code>"toc_next_image"</code>).
	 */
	private static final String TOC_IMG_NEXT = "toc_next_image"; //$NON-NLS-1$

	/**
	 * The widgets that are created for this node.
	 */
	private Composite innerComposite;
	private Label imageLabel;
	private Label titleLabel;
	private FontMetrics fontMetrics;

	static {
		ImageRegistry reg = JFaceResources.getImageRegistry();
		reg.put(TOC_IMG_DISABLED, ImageDescriptor.createFromFile(WizardTableOfContentsNode.class, "images/disabled.gif")); //$NON-NLS-1$
		reg.put(TOC_IMG_NEXT, ImageDescriptor.createFromFile(WizardTableOfContentsNode.class, "images/next.gif")); //$NON-NLS-1$
		reg.put(TOC_IMG_DECISION, ImageDescriptor.createFromFile(WizardTableOfContentsNode.class, "images/decision.gif")); //$NON-NLS-1$

	}

	IWizardPage page;

	/**
	 * Create a new instance of the receiver with newPage as the page
	 * that is activated on selection.
	 * @param newPage
	 */
	public WizardTableOfContentsNode(IWizardPage newPage) {
		this.page = newPage;
	}

	/**
	* Initializes the computation of horizontal and vertical dialog units
	* based on the size of current font.
	* <p>
	* This method must be called before any of the dialog unit based
	* conversion methods are called.
	* </p>
	*
	* @param control a control from which to obtain the current font
	*/
	protected void initializeDialogUnits(Control control) {
		// Compute and store a font metric
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		fontMetrics = gc.getFontMetrics();
		gc.dispose();
	}

	/*
	 * @see ITableOfContentsNode.createWidgets(Composite)
	 */
	public void createWidgets(
		Composite parentComposite,
		Color foreground,
		Color background) {

		initializeDialogUnits(parentComposite);
		Image image = getImage(true);

		innerComposite = new Composite(parentComposite, SWT.BORDER);
		GridLayout innerLayout = new GridLayout();
		innerLayout.marginHeight = 0;
		innerLayout.marginWidth = 0;
		innerComposite.setLayout(innerLayout);

		innerComposite.setForeground(foreground);
		innerComposite.setBackground(background);
		int width =
			Dialog.convertWidthInCharsToPixels(
				fontMetrics,
				Math.min(this.page.getTitle().length() + 2, 20));

		int height = Dialog.convertHeightInCharsToPixels(fontMetrics, 3);
		height += image.getBounds().height;
		width = Math.max(width, image.getBounds().width);
		RowData labelData = new RowData(width, height);
		innerComposite.setLayoutData(labelData);

		imageLabel = new Label(innerComposite, SWT.NULL);
		imageLabel.setForeground(foreground);
		imageLabel.setBackground(background);
		imageLabel.setImage(image);
		GridData imageData =
			new GridData(
				GridData.FILL_HORIZONTAL
					| GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_CENTER);
		imageLabel.setLayoutData(imageData);
		titleLabel = new Label(innerComposite, SWT.WRAP | SWT.CENTER);
		titleLabel.setText(this.page.getTitle());
		titleLabel.setFont(JFaceResources.getFont(JFaceResources.BANNER_FONT));
		titleLabel.setForeground(foreground);
		titleLabel.setBackground(background);
		GridData titleData =
			new GridData(
				GridData.FILL_BOTH
					| GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL
					| GridData.HORIZONTAL_ALIGN_CENTER);
		titleLabel.setLayoutData(titleData);

		MouseListener clickListener = new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
			}
			public void mouseDown(MouseEvent e) {
				page.getWizard().getContainer().showPage(page);
			}
			public void mouseUp(MouseEvent e) {
			}
		};
		imageLabel.addMouseListener(clickListener);
		titleLabel.addMouseListener(clickListener);

	}

	/*
	 * @see ITableOfContentsNode.getPage()
	 */
	public IWizardPage getPage() {
		return page;
	}

	/**
	 * Sets the page.
	 * @param page The page to set.
	 */
	public void setPage(IWizardPage page) {
		this.page = page;
	}

	/*
	 * @see ITableOfContentsNode.dispose()
	 */
	public void dispose() {
		innerComposite.dispose();
	}

	/*
	 * @see ITableOfContentsNode.setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		imageLabel.setImage(getImage(enabled));
		innerComposite.setEnabled(enabled);
		imageLabel.setEnabled(enabled);
		titleLabel.setEnabled(enabled);

	}

	/**
	 * Get the image for the receiver.
	 * @param enabled The boolean state used to determine the image to use.
	 * @return Image
	 */
	private Image getImage(boolean enabled) {
		if (getPage() instanceof IDecisionPage)
			return JFaceResources.getImage(TOC_IMG_DECISION);
		else {
			if (enabled)
				return JFaceResources.getImage(TOC_IMG_NEXT);
			else
				return JFaceResources.getImage(TOC_IMG_DISABLED);
		}
	}

}
