/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.CommandResolver;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.window.WindowManager;

import org.eclipse.ui.internal.AboutInfo;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.activities.IObjectActivityManager;
import org.eclipse.ui.application.IWorkbenchPreferences;
import org.eclipse.ui.application.WorkbenchAdviser;
import org.eclipse.ui.commands.IActionService;
import org.eclipse.ui.commands.IActionServiceEvent;
import org.eclipse.ui.commands.IActionServiceListener;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.ICommandManagerEvent;
import org.eclipse.ui.commands.ICommandManagerListener;
import org.eclipse.ui.commands.NotDefinedException;
import org.eclipse.ui.contexts.IContextActivationService;
import org.eclipse.ui.contexts.IContextActivationServiceEvent;
import org.eclipse.ui.contexts.IContextActivationServiceListener;
import org.eclipse.ui.contexts.IContextManager;
import org.eclipse.ui.contexts.IContextManagerEvent;
import org.eclipse.ui.contexts.IContextManagerListener;
import org.eclipse.ui.internal.commands.ActionService;
import org.eclipse.ui.internal.commands.CommandManager;
import org.eclipse.ui.internal.commands.Match;
import org.eclipse.ui.internal.contexts.ContextActivationService;
import org.eclipse.ui.internal.contexts.ContextManager;
import org.eclipse.ui.internal.decorators.DecoratorManager;
import org.eclipse.ui.internal.fonts.FontDefinition;
import org.eclipse.ui.internal.keys.KeySupport;
import org.eclipse.ui.internal.misc.Assert;
import org.eclipse.ui.internal.misc.Policy;
import org.eclipse.ui.internal.misc.UIStats;
import org.eclipse.ui.internal.progress.ProgressManager;
import org.eclipse.ui.internal.roles.ObjectActivityManager;
import org.eclipse.ui.internal.roles.RoleManager;
import org.eclipse.ui.internal.testing.WorkbenchTestable;
import org.eclipse.ui.internal.util.Util;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.ParseException;
import org.eclipse.ui.progress.IProgressManager;

/**
 * The workbench class represe
nts the top of the Eclipse user interface. 
 * Its primary responsability is the management of workbench windows, dialogs,
 * wizards, and other workbench-related windows.
 * <p>
 * Note that any code that is run during the creation of a workbench instance
 * should not required access to the display.
 * </p><p>
 * Note that this internal class changed significantly between 2.1 and 3.0.
 * Applications that used to define subclasses of this internal class need to
 * be rewritten to use the new workbench adviser API.
 * </p>
 */
public final class Workbench implements IWorkbench {
	private static final String VERSION_STRING[] = { "0.046", "2.0" }; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String DEFAULT_WORKBENCH_STATE_FILENAME = "workbench.xml"; //$NON-NLS-1$
	private static final int RESTORE_CODE_OK = 0;
	private static final int RESTORE_CODE_RESET = 1;
	private static final int RESTORE_CODE_EXIT = 2;

	/**
	 * Holds onto the only instance of Workbench.
	 */
	private static Workbench instance;

	/**
	 * The testable object facade.
	 * 
	 * @since 3.0
	 */
	private static WorkbenchTestable testableObject;

	private WindowManager windowManager;
	private WorkbenchWindow activatedWindow;
	private EditorHistory editorHistory;
	private boolean runEventLoop = true;
	private boolean isStarting = true;
	private boolean isClosing = false;
	
	/**
	 * PlatformUI return code (as opposed to IPlatformRunnable return code).
	 */
	private int returnCode;
	
	private ListenerList windowListeners = new ListenerList();
	
	/**
	 * Product name, or null if none.
	 * @since 3.0
	 */
	private String productName = null;
	
	/**
	 * Adviser providing application-specific configuration and customization
	 * of the workbench.
	 * @since 3.0
	 */
	private WorkbenchAdviser adviser;

	/**
	 * Object for configuring the workbench. Lazily initialized to
	 * an instance unique to the workbench instance.
	 * @since 3.0
	 */
	private WorkbenchConfigurer workbenchConfigurer;

	/**
	 * Creates a new workbench.
	 * 
	 * @param adviser the application-specific adviser that configures and
	 * specializes this workbench instance
	 * @since 3.0
	 */
	private Workbench(WorkbenchAdviser adviser) {
		super();

		if (instance != null) {
			throw new IllegalStateException(WorkbenchMessages.getString("Workbench.CreatingWorkbenchTwice")); //$NON-NLS-1$
		}		
		if (adviser == null) {
			throw new IllegalArgumentException(WorkbenchMessages.getString("Workbench.InvalidAdviser")); //$NON-NLS-1$
		}
		this.adviser = adviser;
		Workbench.instance = this;
	}
	
	/**
	 * Returns the one and only instance of the workbench
	 * or <code>null</code> if not created yet.
	 * 
	 * @return the workbench or <code>null</code> if not created yet.
	 */
	public static final Workbench getInstance() {
		return instance;
	}
	
	/**
	 * Creates the workbench and associates it with the given workbench adviser,
	 * and runs the workbench UI. This entails processing and dispatching
	 * events until the workbench is closed or restarted.
	 * <p>
	 * This method is intended to be called by <code>PlatformUI</code>.
	 * Fails if the workbench UI has already been created.
	 * </p>
	 * 
	 * @param adviser the application-specific adviser that configures and
	 * specializes the workbench
	 * @return return code {@link PlatformUI#RETURN_OK RETURN_OK} for normal
	 * exit; {@link PlatformUI#RETURN_RESTART RETURN_RESTART} if the workbench
	 * was terminated with a call to
	 * {@link IWorkbench#restart IWorkbench.restart}; other values reserved
	 * for future use
	 */
	public static final int createAndRunWorkbench(WorkbenchAdviser adviser) {
		// create the workbench instance
		Workbench workbench = new Workbench(adviser);
		// run the workbench event loop
		return workbench.runUI();
	}
	
	/**
	 * Returns the testable object facade, for use by the test harness.
	 * 
	 * @return the testable object facade
	 * @since 3.0
	 */
	public static WorkbenchTestable getWorkbenchTestable() {
		if (testableObject == null) {
			testableObject = new WorkbenchTestable();
		}
		return testableObject;
	}
	
	/* begin command and context support */
		
	/** The properties key for the key strokes that should be processed out of
	 * order.
	 */
	private static final String OUT_OF_ORDER_KEYS = "OutOfOrderKeys"; //$NON-NLS-1$
	/** The collection of keys that are to be processed out-of-order. */
	private static KeySequence outOfOrderKeys;
	
	static {
		initializeOutOfOrderKeys();
	}
	
	/**
	 * A listener that makes sure that global key bindings are processed if no
	 * other listeners do any useful work.
	 * 
	 * @since 3.0
	 */
	private class OutOfOrderListener implements Listener {		
		public void handleEvent(Event event) {
			// Always remove myself as a listener.
			event.widget.removeListener(event.type, this);
				
			/* If the event is still up for grabs, then re-route through the
			 * global key filter.
			 */
			if (event.doit) {
				Set keyStrokes = generatePossibleKeyStrokes(event);
				processKeyEvent(keyStrokes, event);
			}
		}
	}
	
	/**
	 * A listener that makes sure that out-of-order processing occurs if no
	 * other verify listeners do any work.
	 * 
	 * @since 3.0
	 */
	private class OutOfOrderVerifyListener implements VerifyKeyListener {		
		/**
		 * Checks whether any other verify listeners have triggered.  If not,
		 * then it sets up the top-level out-of-order listener.
		 * 
		 * @param event The verify event after it has been processed by all
		 * other verify listeners; must not be <code>null</code>.
		 */
		public void verifyKey(VerifyEvent event) {
			// Always remove myself as a listener.
			Widget widget = event.widget;
			if (widget instanceof StyledText) {
				((StyledText) widget).removeVerifyKeyListener(this);
			}

			// If the event is still up for grabs, then re-route through the
			// global key filter.
			if (event.doit) {
				widget.addListener(SWT.KeyDown, outOfOrderListener);
			}
		}
	}

	/** The listener that runs key events past the global key bindings. */
	private final Listener keyBindingFilter = new Listener() {
		public void handleEvent(Event event) {
			filterKeyBindings(event);
		}
	};
	/** 
	 * The listener that allows out-of-order key processing to hook back into
	 * the global key bindings.
	 */
	private final OutOfOrderListener outOfOrderListener = new OutOfOrderListener();
	/**
	 * The listener that allows out-of-order key processing on 
	 * <code>StyledText</code> widgets to detect useful work in a verify key
	 * listener.
	 */
	private final OutOfOrderVerifyListener outOfOrderVerifyListener = new OutOfOrderVerifyListener();
	
	private final Listener modeCleaner = new Listener() {
		public void handleEvent(Event event) {
			CommandManager manager = (CommandManager) getCommandManager();
			manager.setMode(KeySequence.getInstance()); // clear the mode
			// TODO Remove this when mode listener updating becomes available.
			updateModeLines(manager.getMode());
		}
	};

	private final ICommandManagerListener commandManagerListener = new ICommandManagerListener() {
		public final void commandManagerChanged(final ICommandManagerEvent commandManagerEvent) {
			updateActiveContextIds();
		}
	};

	private final IContextManagerListener contextManagerListener = new IContextManagerListener() {
		
		List activeContextIds;
		
		public final void contextManagerChanged(final IContextManagerEvent contextManagerEvent) {
			updateActiveContextIds();
			
			List activeContextIds = contextManagerEvent.getContextManager().getActiveContextIds();

			if (!Util.equals(this.activeContextIds, activeContextIds)) {
				this.activeContextIds = activeContextIds;
				IWorkbenchWindow workbenchWindow = getActiveWorkbenchWindow();

				if (workbenchWindow instanceof WorkbenchWindow) {
					MenuManager menuManager = ((WorkbenchWindow) workbenchWindow).getMenuManager();
					menuManager.updateAll(true);
				}
			}
		}
	};

	private IActionServiceListener actionServiceListener =
		new IActionServiceListener() {
		public void actionServiceChanged(IActionServiceEvent actionServiceEvent) {
			updateActiveCommandIdsAndActiveContextIds();
		}
	};

	private IContextActivationServiceListener contextActivationServiceListener =
		new IContextActivationServiceListener() {
		public void contextActivationServiceChanged(IContextActivationServiceEvent contextActivationServiceEvent) {
			updateActiveCommandIdsAndActiveContextIds();
		}
	};

	private IInternalPerspectiveListener internalPerspectiveListener =
		new IInternalPerspectiveListener() {
		public void perspectiveActivated(
			IWorkbenchPage workbenchPage,
			IPerspectiveDescriptor perspectiveDescriptor) {
			updateActiveCommandIdsAndActiveContextIds();
		}

		public void perspectiveChanged(
			IWorkbenchPage workbenchPage,
			IPerspectiveDescriptor perspectiveDescriptor,
			String changeId) {
			updateActiveCommandIdsAndActiveContextIds();
		}

		public void perspectiveClosed(
			IWorkbenchPage page,
			IPerspectiveDescriptor perspective) {
			updateActiveCommandIdsAndActiveContextIds();
		}

		public void perspectiveOpened(
			IWorkbenchPage page,
			IPerspectiveDescriptor perspective) {
			updateActiveCommandIdsAndActiveContextIds();
		}
	};

	private IPageListener pageListener = new IPageListener() {
		public void pageActivated(IWorkbenchPage workbenchPage) {
			updateActiveCommandIdsAndActiveContextIds();
		}

		public void pageClosed(IWorkbenchPage workbenchPage) {
			updateActiveCommandIdsAndActiveContextIds();
		}

		public void pageOpened(IWorkbenchPage workbenchPage) {
			updateActiveCommandIdsAndActiveContextIds();
		}
	};

	private IPartListener partListener = new IPartListener() {
		public void partActivated(IWorkbenchPart workbenchPart) {
			updateActiveCommandIdsAndActiveContextIds();
			updateActiveWorkbenchWindowMenuManager();
		}

		public void partBroughtToTop(IWorkbenchPart workbenchPart) {
		}

		public void partClosed(IWorkbenchPart workbenchPart) {
			updateActiveCommandIdsAndActiveContextIds();
		}

		public void partDeactivated(IWorkbenchPart workbenchPart) {
			updateActiveCommandIdsAndActiveContextIds();
		}

		public void partOpened(IWorkbenchPart workbenchPart) {
			updateActiveCommandIdsAndActiveContextIds();
		}
	};

	private IWindowListener windowListener = new IWindowListener() {
		public void windowActivated(IWorkbenchWindow workbenchWindow) {
			updateActiveCommandIdsAndActiveContextIds();
			updateActiveWorkbenchWindowMenuManager();
		}

		public void windowClosed(IWorkbenchWindow workbenchWindow) {
			updateActiveCommandIdsAndActiveContextIds();
			updateActiveWorkbenchWindowMenuManager();
		}

		public void windowDeactivated(IWorkbenchWindow workbenchWindow) {
			updateActiveCommandIdsAndActiveContextIds();
			updateActiveWorkbenchWindowMenuManager();
		}

		public void windowOpened(IWorkbenchWindow workbenchWindow) {
			updateActiveCommandIdsAndActiveContextIds();
			updateActiveWorkbenchWindowMenuManager();
		}
	};

	private CommandManager commandManager;
	private ContextManager contextManager;
	private volatile boolean keyFilterDisabled;
	private final Object keyFilterMutex = new Object();
	
	private IWorkbenchWindow activeWorkbenchWindow;
	//private IActionService activeWorkbenchWindowActionService;
	//private IContextActivationService activeWorkbenchWindowContextActivationService;

	private IWorkbenchPage activeWorkbenchPage;
	private IActionService activeWorkbenchPageActionService;
	private IContextActivationService activeWorkbenchPageContextActivationService;

	private IWorkbenchPart activeWorkbenchPart;
	private IActionService activeWorkbenchPartActionService;
	private IContextActivationService activeWorkbenchPartContextActivationService;

	private IActionService actionService;
	private IContextActivationService contextActivationService;

	public IActionService getActionService() {
		if (actionService == null) {
			actionService = new ActionService();
			actionService.addActionServiceListener(actionServiceListener);
		}

		return actionService;
	}

	public ICommandManager getCommandManager() {
		return commandManager;
	}

	public IContextActivationService getContextActivationService() {
		if (contextActivationService == null) {
			contextActivationService = new ContextActivationService();
			contextActivationService.addContextActivationServiceListener(
				contextActivationServiceListener);
		}

		return contextActivationService;
	}

	public IContextManager getContextManager() {
		return contextManager;
	}

	public final void disableKeyFilter() {
		synchronized (keyFilterMutex) {
			final Display display = Display.getCurrent();
			display.removeFilter(SWT.KeyDown, keyBindingFilter);
			display.removeFilter(SWT.Traverse, keyBindingFilter);
			keyFilterDisabled = true;
		}
	}

	public final void enableKeyFilter() {
		synchronized (keyFilterMutex) {
			final Display display = Display.getCurrent();
			display.addFilter(SWT.KeyDown, keyBindingFilter);
			display.addFilter(SWT.Traverse, keyBindingFilter);
			keyFilterDisabled = false;
		}
	}
	
	public final boolean isKeyFilterEnabled() {
		synchronized (keyFilterMutex) {
			return !keyFilterDisabled;
		}
	}
	
	public String getName(String commandId) {
		String name = null;
		
		if (commandId != null) {
			final ICommand command = commandManager.getCommand(commandId);

			if (command != null)
				try {
				    name = command.getName();
				} catch (NotDefinedException eNotDefined) {
				}
		}

		return name;
	}

	/**
	 * Initializes the <code>outOfOrderKeys</code> member variable using the
	 * keys defined in the properties file.
	 * 
	 * @since 3.0
	 */
	private static void initializeOutOfOrderKeys() {
		// Get the key strokes which should be out of order.
		String keysText = WorkbenchMessages.getString(OUT_OF_ORDER_KEYS);
		outOfOrderKeys = KeySequence.getInstance();
		try {
			outOfOrderKeys = KeySequence.getInstance(keysText);
		} catch (ParseException e) {
			String message = "Could not parse out-of-order keys definition: '" + keysText + "'.  Continuing with no out-of-order keys."; //$NON-NLS-1$ //$NON-NLS-2$
			WorkbenchPlugin.log(message, new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH, 0, message, e));
		}
	}
	
	/**
	 * <p>
	 * Launches the command matching a the typed key.  This filter an incoming
	 * <code>SWT.KeyDown</code> or <code>SWT.Traverse</code> event at the level
	 * of the display (i.e., before it reaches the widgets).  It does not allow
	 * processing in a dialog or if the key strokes does not contain a natural
	 * key.
	 * </p>
	 * <p>
	 * Some key strokes (defined as a property) are declared as out-of-order
	 * keys.  This means that they are processed by the widget <em>first</em>.
	 * Only if the other widget listeners do no useful work does it try to
	 * process key bindings.  For example, "ESC" can cancel the current widget
	 * action, if there is one, without triggering key bindings.
	 * </p>
	 *  
	 * @param event The incoming event; must not be <code>null</code>.
	 * 
	 * @since 3.0
	 */
	private void filterKeyBindings(Event event) {
		/* Only process key strokes containing natural keys to trigger key 
		 * bindings
		 */
		if ((event.keyCode & SWT.MODIFIER_MASK) != 0)
			return;

		// Don't allow dialogs to process key bindings.
		if (event.widget instanceof Control) {
			Shell shell = ((Control) event.widget).getShell();
			if (shell.getParent() != null)
				return;
		}
			
		// Allow special key out-of-order processing.
		Set keyStrokes = generatePossibleKeyStrokes(event);
		if (isOutOfOrderKey(keyStrokes)) {
			if (event.type == SWT.KeyDown) {
				Widget widget = event.widget;
				if (widget instanceof StyledText) {
					/* KLUDGE.  Some people try to do useful work in verify
					 * listeners.  The way verify listeners work in SWT, we
					 * need to verify the key as well; otherwise, we can
					 * detect that useful work has been done.
					 */
					((StyledText) widget).addVerifyKeyListener(outOfOrderVerifyListener);
				} else {
					widget.addListener(SWT.KeyDown, outOfOrderListener);
				}
			}
			/* Otherwise, we count on a key down arriving eventually.
			 * Expecting out of order handling on Ctrl+Tab, for example, is
			 * a bad idea (stick to keys that are not window traversal 
			 * keys). 
			 */
		} else {
			processKeyEvent(keyStrokes, event);
		}
	}
	
	/**
	 * <p>
	 * Determines whether the given event represents a key press that should be
	 * handled as an out-of-order event.  An out-of-order key press is one that
	 * is passed to the focus control first.  Only if the focus control fails to
	 * respond will the regular key bindings get applied.
	 * </p>
	 * <p>
	 * Care must be taken in choosing which keys are chosen as out-of-order
	 * keys.  This method has only been designed and test to work with the
	 * unmodified "Escape" key stroke.
	 * </p>
	 * 
	 * @param keyStrokes The key stroke in which to look for out-of-order keys;
	 * must not be <code>null</code>.
	 * 
	 * @since 3.0
	 */
	private static boolean isOutOfOrderKey(Set keyStrokes) {		
		// Compare to see if one of the possible key strokes is out of order.
		Iterator keyStrokeItr = keyStrokes.iterator();
		while (keyStrokeItr.hasNext()) {
			if (outOfOrderKeys.getKeyStrokes().contains(keyStrokeItr.next())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Generates any key strokes that are near matches to the given event.  The
	 * first such key stroke is always the exactly matching key stroke.
	 * 
	 * @param event The event from which the key strokes should be generated;
	 * must not be <code>null</code>.
	 * @return The set of nearly matching key strokes.  It is never 
	 * <code>null</code> and never empty. 
	 * 
	 * @since 3.0
	 */
	public static Set generatePossibleKeyStrokes(Event event) {
		Set keyStrokes = new HashSet();
		keyStrokes.add(KeySupport.convertAcceleratorToKeyStroke(KeySupport.convertEventToUnmodifiedAccelerator(event)));
		keyStrokes.add(KeySupport.convertAcceleratorToKeyStroke(KeySupport.convertEventToUnshiftedModifiedAccelerator(event)));
		keyStrokes.add(KeySupport.convertAcceleratorToKeyStroke(KeySupport.convertEventToModifiedAccelerator(event)));
		return keyStrokes;
	}
	
	/**
	 * Actually performs the processing of the key event by interacting with the
	 * <code>ICommandManager</code>.  If work is carried out, then the event is
	 * stopped here (i.e., <code>event.doit = false</code>).
	 * 
	 * @param keyStrokes The set of all possible matching key strokes; must not
	 * be <code>null</code>.
	 * @param event The event to process; must not be <code>null</code>.
	 * 
	 * @since 3.0
	 */
	private void processKeyEvent(Set keyStrokes, Event event) {
		if (press(keyStrokes, event)) {
			switch (event.type) {
				case SWT.KeyDown :
					event.doit = false;
					break;
				case SWT.Traverse :
					event.detail = SWT.TRAVERSE_NONE;
					event.doit = true;
					break;
				default :
					}

			event.type = SWT.NONE;
		}
	}
	
	/**
	 * Processes a key press with respect to the key binding architecture.  This
	 * updates the mode of the command manager, and runs the current handler for
	 * the command that matches the key sequence, if any.
	 * 
	 * @param potentialKeyStrokes The key strokes that could potentially match,
	 * in the order of priority; must not be <code>null</code>.
	 * @param event The event to pass to the action; may be <code>null</code>.
	 * 
	 * @return <code>true</code> if a command is executed; <code>false</code>
	 * otherwise.
	 * 
	 * @since 3.0
	 */
	public boolean press(Set potentialKeyStrokes, Event event) {
		// TODO move this method to CommandManager once getMode() is added to ICommandManager (and triggers and change event)
		// TODO remove event parameter once key-modified actions are removed
		
		// Check every potential key stroke until one matches.
		Iterator keyStrokeItr = potentialKeyStrokes.iterator();
		while (keyStrokeItr.hasNext()) {
			KeySequence modeBeforeKeyStroke = commandManager.getMode();
			List keyStrokes = new ArrayList(modeBeforeKeyStroke.getKeyStrokes());
			keyStrokes.add(keyStrokeItr.next());
			KeySequence modeAfterKeyStroke = KeySequence.getInstance(keyStrokes);
			Map matchesByKeySequenceForModeBeforeKeyStroke = commandManager.getMatchesByKeySequenceForMode();
			commandManager.setMode(modeAfterKeyStroke);
			Map matchesByKeySequenceForModeAfterKeyStroke = commandManager.getMatchesByKeySequenceForMode();
			boolean consumeKeyStroke = false;
			boolean matchingSequence = false;

			if (!matchesByKeySequenceForModeAfterKeyStroke.isEmpty()) {
				// this key stroke is part of one or more possible completions: consume the keystroke
				updateModeLines(modeAfterKeyStroke);
				consumeKeyStroke = true;
				matchingSequence = true;
			} else {
				// there are no possible longer multi-stroke sequences, allow a completion now if possible
				Match match = (Match) matchesByKeySequenceForModeBeforeKeyStroke.get(modeAfterKeyStroke);

				if (match != null) {
					// a completion was found. 
					String commandId = match.getCommandId();
					Map actionsById = commandManager.getActionsById();
					org.eclipse.ui.commands.IAction action = (org.eclipse.ui.commands.IAction) actionsById.get(commandId);

					if (action != null) {
						// an action was found corresponding to the completion

						if (action.isEnabled()) {
							updateModeLines(modeAfterKeyStroke);
							try {
								action.execute(event);
							} catch (Exception e) {
								String message = "Action for command '" + commandId + "' failed to execute properly."; //$NON-NLS-1$ //$NON-NLS-2$
								WorkbenchPlugin.log(message, new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH, 0, message, e));
							}
						}
	
						// consume the keystroke
						consumeKeyStroke = true;
					}
					
					matchingSequence = true;
				}
	
				// possibly no completion was found, or no action was found corresponding to the completion, but if we were already in a mode consume the keystroke anyway.									
				if (modeBeforeKeyStroke.getKeyStrokes().size() >= 1)
					consumeKeyStroke = true;
	
				// clear mode			
				commandManager.setMode(KeySequence.getInstance());
				updateModeLines(KeySequence.getInstance());
			}
	
			// TODO is this necessary?		
			updateActiveContextIds();
			
			if (consumeKeyStroke) {
				// We found a match, so stop now.
				return consumeKeyStroke;
			} else {
				/* If we haven't consumed the stroke, but we found a command
				 * That matches, then we should break the loop.
				 */
				if (matchingSequence) {
					break;
				} else {
					// Restore the mode, so we can try again.
					commandManager.setMode(modeBeforeKeyStroke);
				}
			}
		}
		
		// No key strokes match.
		return false;
	}	
	
	/**
	 * Updates the text of the mode lines with the current mode.
	 * @param mode The mode which should be used to update the status line;
	 * must not be <code>null</code>.
	 */
	private void updateModeLines(KeySequence mode) {
		// Format the mode into text.
		String text = mode.format();
		
		// Update each open window's status line.
		IWorkbenchWindow[] windows = getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			IWorkbenchWindow window = windows[i];
			if (window instanceof WorkbenchWindow) {
				// @issue mode contribution item is added by IDE, so an upcall would be required here
				// mode contribution item should use a mode listener instead
//				((WorkbenchWindow) window).getActionBuilder().updateModeLine(text);
 			}
 		}
 	}

	public void updateActiveContextIds() {
		commandManager.setActiveContextIds(contextManager.getActiveContextIds());
	}

	public void updateActiveWorkbenchWindowMenuManager() {
		IWorkbenchWindow workbenchWindow = getActiveWorkbenchWindow();

		if (workbenchWindow instanceof WorkbenchWindow) {
			MenuManager menuManager = ((WorkbenchWindow) workbenchWindow).getMenuManager();			
			menuManager.update(IAction.TEXT);
		}
	}

	void updateActiveCommandIdsAndActiveContextIds() {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();

		if (activeWorkbenchWindow != null
			&& !(activeWorkbenchWindow instanceof WorkbenchWindow))
			activeWorkbenchWindow = null;

		//IActionService activeWorkbenchWindowActionService = activeWorkbenchWindow != null ? ((WorkbenchWindow) activeWorkbenchWindow).getActionService() : null;
		//IContextActivationService activeWorkbenchWindowContextActivationService = activeWorkbenchWindow != null ? ((WorkbenchWindow) activeWorkbenchWindow).getContextActivationService() : null;

		IWorkbenchPage activeWorkbenchPage =
			activeWorkbenchWindow != null
				? activeWorkbenchWindow.getActivePage()
				: null;
		IActionService activeWorkbenchPageActionService =
			activeWorkbenchPage != null
				? ((WorkbenchPage) activeWorkbenchPage).getActionService()
				: null;
		IContextActivationService activeWorkbenchPageContextActivationService =
			activeWorkbenchPage != null
				? ((WorkbenchPage) activeWorkbenchPage)
					.getContextActivationService()
				: null;
		IPartService activePartService =
			activeWorkbenchWindow != null
				? activeWorkbenchWindow.getPartService()
				: null;
		IWorkbenchPart activeWorkbenchPart =
			activePartService != null
				? activePartService.getActivePart()
				: null;
		IWorkbenchPartSite activeWorkbenchPartSite =
			activeWorkbenchPart != null ? activeWorkbenchPart.getSite() : null;
		IActionService activeWorkbenchPartActionService =
			activeWorkbenchPartSite != null
				? ((PartSite) activeWorkbenchPartSite).getActionService()
				: null;
		IContextActivationService activeWorkbenchPartContextActivationService =
			activeWorkbenchPartSite != null
				? ((PartSite) activeWorkbenchPartSite)
					.getContextActivationService()
				: null;

		if (activeWorkbenchWindow != this.activeWorkbenchWindow) {
			if (this.activeWorkbenchWindow != null) {
				this.activeWorkbenchWindow.removePageListener(pageListener);
				this.activeWorkbenchWindow.getPartService().removePartListener(
					partListener);
				((WorkbenchWindow) this.activeWorkbenchWindow)
					.getPerspectiveService()
					.removePerspectiveListener(internalPerspectiveListener);
			}

			this.activeWorkbenchWindow = activeWorkbenchWindow;

			if (this.activeWorkbenchWindow != null) {
				this.activeWorkbenchWindow.addPageListener(pageListener);
				this.activeWorkbenchWindow.getPartService().addPartListener(
					partListener);
				((WorkbenchWindow) this.activeWorkbenchWindow)
					.getPerspectiveService()
					.addPerspectiveListener(internalPerspectiveListener);
			}
		}

		/*
		if (activeWorkbenchWindowActionService != this.activeWorkbenchWindowActionService) {
			if (this.activeWorkbenchWindowActionService != null)
				this.activeWorkbenchWindowActionService.removeActionServiceListener(actionServiceListener);
					
			this.activeWorkbenchWindow = activeWorkbenchWindow;
			this.activeWorkbenchWindowActionService = activeWorkbenchWindowActionService;
		
			if (this.activeWorkbenchWindowActionService != null)
				this.activeWorkbenchWindowActionService.addActionServiceListener(actionServiceListener);
		}
		*/

		if (activeWorkbenchPageActionService
			!= this.activeWorkbenchPageActionService) {
			if (this.activeWorkbenchPageActionService != null)
				this
					.activeWorkbenchPageActionService
					.removeActionServiceListener(
					actionServiceListener);

			this.activeWorkbenchPage = activeWorkbenchPage;
			this.activeWorkbenchPageActionService =
				activeWorkbenchPageActionService;

			if (this.activeWorkbenchPageActionService != null)
				this.activeWorkbenchPageActionService.addActionServiceListener(
					actionServiceListener);
		}

		if (activeWorkbenchPartActionService
			!= this.activeWorkbenchPartActionService) {
			if (this.activeWorkbenchPartActionService != null)
				this
					.activeWorkbenchPartActionService
					.removeActionServiceListener(
					actionServiceListener);

			this.activeWorkbenchPart = activeWorkbenchPart;
			this.activeWorkbenchPartActionService =
				activeWorkbenchPartActionService;

			if (this.activeWorkbenchPartActionService != null)
				this.activeWorkbenchPartActionService.addActionServiceListener(
					actionServiceListener);
		}

		SortedMap actionsById = new TreeMap();
		actionsById.putAll(getActionService().getActionsById());

		//if (this.activeWorkbenchWindowActionService != null)
		//	actionsById.putAll(this.activeWorkbenchWindowActionService.getActionsById());

		if (this.activeWorkbenchWindow != null) {
			actionsById.putAll(
				((WorkbenchWindow) this.activeWorkbenchWindow)
					.getActionsForGlobalActions());
			actionsById.putAll(
				((WorkbenchWindow) this.activeWorkbenchWindow)
					.getActionsForActionSets());
		}

		if (this.activeWorkbenchPageActionService != null)
			actionsById.putAll(
				this.activeWorkbenchPageActionService.getActionsById());

		if (this.activeWorkbenchPartActionService != null)
			actionsById.putAll(
				this.activeWorkbenchPartActionService.getActionsById());

		commandManager.setActionsById(actionsById);

		/*
		if (activeWorkbenchWindowContextActivationService != this.activeWorkbenchWindowContextActivationService) {
			if (this.activeWorkbenchWindowContextActivationService != null)
				this.activeWorkbenchWindowContextActivationService.removeContextActivationServiceListener(contextActivationServiceListener);
					
			this.activeWorkbenchWindow = activeWorkbenchWindow;
			this.activeWorkbenchWindowContextActivationService = activeWorkbenchWindowContextActivationService;
		
			if (this.activeWorkbenchWindowContextActivationService != null)
				this.activeWorkbenchWindowContextActivationService.addContextActivationServiceListener(contextActivationServiceListener);
		}
		*/

		if (activeWorkbenchPageContextActivationService
			!= this.activeWorkbenchPageContextActivationService) {
			if (this.activeWorkbenchPageContextActivationService != null)
				this
					.activeWorkbenchPageContextActivationService
					.removeContextActivationServiceListener(contextActivationServiceListener);

			this.activeWorkbenchPage = activeWorkbenchPage;
			this.activeWorkbenchPageContextActivationService =
				activeWorkbenchPageContextActivationService;

			if (this.activeWorkbenchPageContextActivationService != null)
				this
					.activeWorkbenchPageContextActivationService
					.addContextActivationServiceListener(contextActivationServiceListener);
		}

		if (activeWorkbenchPartContextActivationService
			!= this.activeWorkbenchPartContextActivationService) {
			if (this.activeWorkbenchPartContextActivationService != null)
				this
					.activeWorkbenchPartContextActivationService
					.removeContextActivationServiceListener(contextActivationServiceListener);

			this.activeWorkbenchPart = activeWorkbenchPart;
			this.activeWorkbenchPartContextActivationService =
				activeWorkbenchPartContextActivationService;

			if (this.activeWorkbenchPartContextActivationService != null)
				this
					.activeWorkbenchPartContextActivationService
					.addContextActivationServiceListener(contextActivationServiceListener);
		}

		SortedSet activeContextIds = new TreeSet();
		activeContextIds.addAll(
			getContextActivationService().getActiveContextIds());

		//if (this.activeWorkbenchWindowContextActivationService != null)
		//	activeContextIds.addAll(this.activeWorkbenchWindowContextActivationService.getActiveContextIds());

		if (this.activeWorkbenchPageContextActivationService != null)
			activeContextIds.addAll(
				this
					.activeWorkbenchPageContextActivationService
					.getActiveContextIds());

		if (this.activeWorkbenchPartContextActivationService != null)
			activeContextIds.addAll(
				this
					.activeWorkbenchPartContextActivationService
					.getActiveContextIds());

		contextManager.setActiveContextIds(new ArrayList(activeContextIds));
	}

	private void initializeCommandsAndContexts(final Display display) {
		CommandResolver.getInstance().setCommandResolver(new CommandResolver.ICallback() {
			public String guessCommandIdFromActionId(String actionId) {
				// TODO bad cast
				return ((CommandManager) getCommandManager()).guessCommandIdFromActionId(actionId);		
			}

			public Integer getAccelerator(String commandId) {
				// TODO bad cast
				return ((CommandManager) getCommandManager()).getAccelerator(commandId);		
			}
	
			public String getAcceleratorText(String commandId) {
				// TODO bad cast
				return ((CommandManager) getCommandManager()).getAcceleratorText(commandId);		
			}	

			public final boolean inContext(final String commandId) {
				if (commandId != null) {
					final ICommand command = commandManager.getCommand(commandId);
	
					if (command != null)
						return command.isDefined() && command.isActive();
				}

				return true;
			}
			
			public final boolean isKeyFilterEnabled() {
				return Workbench.this.isKeyFilterEnabled();
			}
		});
		
		commandManager = CommandManager.getInstance();
		contextManager = ContextManager.getInstance();
		commandManager.addCommandManagerListener(commandManagerListener);
		contextManager.addContextManagerListener(contextManagerListener);
		updateActiveContextIds();
		display.addFilter(SWT.Traverse, keyBindingFilter);
		display.addFilter(SWT.KeyDown, keyBindingFilter);
		display.addFilter(SWT.FocusOut, modeCleaner);
		addWindowListener(windowListener);
		updateActiveCommandIdsAndActiveContextIds();
	}

	/* end command and context support */

	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public void addWindowListener(IWindowListener l) {
		windowListeners.add(l);
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public void removeWindowListener(IWindowListener l) {
		windowListeners.remove(l);
	}

	/**
	 * Fire window opened event.
	 */
	protected void fireWindowOpened(IWorkbenchWindow window) {
		Object list[] = windowListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((IWindowListener) list[i]).windowOpened(window);
		}
	}
	/**
	 * Fire window closed event.
	 */
	protected void fireWindowClosed(IWorkbenchWindow window) {
		if (activatedWindow == window) {
			// Do not hang onto it so it can be GC'ed
			activatedWindow = null;
		}
					
		Object list[] = windowListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((IWindowListener) list[i]).windowClosed(window);
		}
	}
	/**
	 * Fire window activated event.
	 */
	protected void fireWindowActivated(IWorkbenchWindow window) {
		Object list[] = windowListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((IWindowListener) list[i]).windowActivated(window);
		}
	}
	/**
	 * Fire window deactivated event.
	 */
	protected void fireWindowDeactivated(IWorkbenchWindow window) {
		Object list[] = windowListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((IWindowListener) list[i]).windowDeactivated(window);
		}
	}

	/**
	 * Closes the workbench. Assumes that the busy cursor is active.
	 * 
	 * @param force true if the close is mandatory, and false if the close is
	 * allowed to fail
	 * @return true if the close succeeded, and false otherwise
	 */
	private boolean busyClose(final boolean force) {
		
		// save any open editors if they are dirty
		isClosing = saveAllEditors(!force);
		if (!force && !isClosing) {
			return false;
		}

		IPreferenceStore store = getPreferenceStore();
		boolean closeEditors = store.getBoolean(IWorkbenchPreferences.SHOULD_CLOSE_EDITORS_ON_EXIT);
		if (closeEditors) {
			Platform.run(new SafeRunnable() {
				public void run() {
					IWorkbenchWindow windows[] = getWorkbenchWindows();
					for (int i = 0; i < windows.length; i++) {
						IWorkbenchPage pages[] = windows[i].getPages();
						for (int j = 0; j < pages.length; j++) {
							isClosing = isClosing && pages[j].closeAllEditors(false);
						}
					}
				}
			});
			if (!force && !isClosing) {
				return false;
			}
		}

		if (getWorkbenchConfigurer().getSaveAndRestore()) {
			Platform.run(new SafeRunnable() {
				public void run() {
					XMLMemento mem = recordWorkbenchState();
					//Save the IMemento to a file.
					saveMementoToFile(mem);
				}
				public void handleException(Throwable e) {
					String message;
					if (e.getMessage() == null) {
						message = WorkbenchMessages.getString("ErrorClosingNoArg"); //$NON-NLS-1$
					} else {
						message = WorkbenchMessages.format("ErrorClosingOneArg", new Object[] { e.getMessage()}); //$NON-NLS-1$
					}
	
					if (!MessageDialog.openQuestion(null, WorkbenchMessages.getString("Error"), message)) { //$NON-NLS-1$
						isClosing = false;
					}
				}
			});
		}
		if (!force && !isClosing) {
			return false;
		}

		Platform.run(new SafeRunnable(WorkbenchMessages.getString("ErrorClosing")) { //$NON-NLS-1$
			public void run() {
				if (isClosing || force)
					isClosing = windowManager.close();
			}
		});

		if (!force && !isClosing) {
			return false;
		}

		runEventLoop = false;
		return true;
	}

	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public boolean saveAllEditors(boolean confirm) {
		final boolean finalConfirm = confirm;
		final boolean[] result = new boolean[1];
		result[0] = true;

		Platform.run(new SafeRunnable(WorkbenchMessages.getString("ErrorClosing")) { //$NON-NLS-1$
			public void run() {
				//Collect dirtyEditors
				ArrayList dirtyEditors = new ArrayList();
				ArrayList dirtyEditorsInput = new ArrayList();
				IWorkbenchWindow windows[] = getWorkbenchWindows();
				for (int i = 0; i < windows.length; i++) {
					IWorkbenchPage pages[] = windows[i].getPages();
					for (int j = 0; j < pages.length; j++) {
						WorkbenchPage page = (WorkbenchPage) pages[j];
						IEditorPart editors[] = page.getDirtyEditors();
						for (int k = 0; k < editors.length; k++) {
							IEditorPart editor = editors[k];
							if (editor.isDirty()) {
								if (!dirtyEditorsInput.contains(editor.getEditorInput())) {
									dirtyEditors.add(editor);
									dirtyEditorsInput.add(editor.getEditorInput());
								}
							}
						}
					}
				}
				if (dirtyEditors.size() > 0) {
					IWorkbenchWindow w = getActiveWorkbenchWindow();
					if (w == null)
						w = windows[0];
					result[0] = EditorManager.saveAll(dirtyEditors, finalConfirm, w);
				}
			}
		});
		return result[0];
	}
	
	/**
	 * Opens a new workbench window and page with a specific perspective.
	 *
	 * Assumes that busy cursor is active.
	 */
	private IWorkbenchWindow busyOpenWorkbenchWindow(String perspID, IAdaptable input) throws WorkbenchException {
		// Create a workbench window (becomes active window)
		WorkbenchWindow newWindow = newWorkbenchWindow();
		newWindow.create(); // must be created before adding to window manager
		windowManager.add(newWindow);

		// Create the initial page.
		newWindow.busyOpenPage(perspID, input);

		// Open after opening page, to avoid flicker.
		newWindow.open();

		return newWindow;
	}

	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public boolean close() {
		return close(PlatformUI.RETURN_OK, false);
	}
	/**
	 * Closes the workbench, returning the given return code from the run method.
	 * If forced, the workbench is closed no matter what.
	 * 
	 * @param returnCode {@link PlatformUI#RETURN_OK RETURN_OK} for normal exit; 
	 * {@link PlatformUI#RETURN_RESTART RETURN_RESTART} if the workbench was terminated
	 * with a call to {@link IWorkbench#restart IWorkbench.restart}; 
	 * {@link PlatformUI#RETURN_UNSTARTABLE RETURN_UNSTARTABLE} if the workbench could
	 * not be started; other values reserved for future use
	 * @param force true to force the workbench close, and false for a "soft"
	 * close that can be canceled
	 * @return true if the close was successful, and false if the close was
	 * canceled
	 */
	/* package */ boolean close(int returnCode, final boolean force) {
		this.returnCode = returnCode;
		final boolean[] ret = new boolean[1];
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				ret[0] = busyClose(force);
			}
		});
		return ret[0];
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IWorkbenchWindow getActiveWorkbenchWindow() {
		// Display will be null if SWT has not been initialized or
		// this method was called from wrong thread.
		// @issue if this is called from the wrong thread, this should fail, not return null -- general workbench thread safety issue
		Display display = Display.getCurrent();
		if (display == null)
			return null;

		// Look at the current shell and up its parent
		// hierarchy for a workbench window.
		Control shell = display.getActiveShell();
		while (shell != null) {
			Object data = shell.getData();
			if (data instanceof IWorkbenchWindow)
				return (IWorkbenchWindow) data;
			shell = shell.getParent();
		}
		
		// Look for the window that was last known being
		// the active one
		WorkbenchWindow win = getActivatedWindow();
		if (win != null) {
			return win;
		}
		
		// Look at all the shells and pick the first one
		// that is a workbench window.
		Shell shells[] = display.getShells();
		for (int i = 0; i < shells.length; i++) {
			Object data = shells[i].getData();
			if (data instanceof IWorkbenchWindow)
				return (IWorkbenchWindow) data;
		}
		
		// Can't find anything!
		return null;
	}

	/*
	 * Returns the editor history.
	 */
	protected EditorHistory getEditorHistory() {
		if (editorHistory == null) {
			editorHistory = new EditorHistory();
		}
		return editorHistory;
	}
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IEditorRegistry getEditorRegistry() {
		return WorkbenchPlugin.getDefault().getEditorRegistry();
	}
	
	/*
	 * Returns the number for a new window.  This will be the first
	 * number > 0 which is not used to identify another window in
	 * the workbench.
	 */
	private int getNewWindowNumber() {
		// Get window list.
		Window[] windows = windowManager.getWindows();
		int count = windows.length;

		// Create an array of booleans (size = window count).
		// Cross off every number found in the window list.
		boolean checkArray[] = new boolean[count];
		for (int nX = 0; nX < count; nX++) {
			if (windows[nX] instanceof WorkbenchWindow) {
				WorkbenchWindow ww = (WorkbenchWindow) windows[nX];
				int index = ww.getNumber() - 1;
				if (index >= 0 && index < count)
					checkArray[index] = true;
			}
		}

		// Return first index which is not used.
		// If no empty index was found then every slot is full.
		// Return next index.
		for (int index = 0; index < count; index++) {
			if (!checkArray[index])
				return index + 1;
		}
		return count + 1;
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IPerspectiveRegistry getPerspectiveRegistry() {
		return WorkbenchPlugin.getDefault().getPerspectiveRegistry();
	}

	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public PreferenceManager getPreferenceManager() {
		return WorkbenchPlugin.getDefault().getPreferenceManager();
	}
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IPreferenceStore getPreferenceStore() {
		return WorkbenchPlugin.getDefault().getPreferenceStore();
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public ISharedImages getSharedImages() {
		return WorkbenchPlugin.getDefault().getSharedImages();
	}
	/**
	 * Returns the window manager for this workbench.
	 * 
	 * @return the window manager
	 */
	/* package */ WindowManager getWindowManager() {
		return windowManager;
	}

	/*
	 * Answer the workbench state file.
	 */
	private File getWorkbenchStateFile() {
		IPath path = WorkbenchPlugin.getDefault().getStateLocation();
		path = path.append(DEFAULT_WORKBENCH_STATE_FILENAME);
		return path.toFile();
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public int getWorkbenchWindowCount() {
		return windowManager.getWindowCount();
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IWorkbenchWindow[] getWorkbenchWindows() {
		Window[] windows = windowManager.getWindows();
		IWorkbenchWindow[] dwindows = new IWorkbenchWindow[windows.length];
		System.arraycopy(windows, 0, dwindows, 0, windows.length);
		return dwindows;
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IWorkingSetManager getWorkingSetManager() {
		return WorkbenchPlugin.getDefault().getWorkingSetManager();
	}
		
	/**
	 * Initializes the workbench now that the display is created.
	 *
	 * @param windowImage the descriptor of the image to be used in the corner
	 * of each window, or <code>null</code> if none
	 * @return true if init succeeded.
	 */
	private boolean init(ImageDescriptor windowImage, Display display) {
		initializeCommandsAndContexts(display);
		
		// setup debug mode if required.
		if (WorkbenchPlugin.getDefault().isDebugging()) {
			WorkbenchPlugin.DEBUG = true;
			ModalContext.setDebugMode(true);
		}

		// create workbench window manager
		windowManager = new WindowManager();

		// allow the workbench configurer to initialize
		getWorkbenchConfigurer().init();

		initializeImages(windowImage);
		initializeFonts();
		initializeColors();

		// now that the workbench is sufficiently initialized, let the adviser have a turn.
		adviser.initialize(getWorkbenchConfigurer());
		
		// configure use of color icons in toolbars
		boolean useColorIcons = getPreferenceStore().getBoolean(IPreferenceConstants.COLOR_ICONS);
		ActionContributionItem.setUseColorIconsInToolbars(useColorIcons);
		
		// initialize workbench single-click vs double-click behavior	
		initializeSingleClickOption();

		// deadlock code
		boolean avoidDeadlock = true;

		String[] commandLineArgs = Platform.getCommandLineArgs();
		for (int i = 0; i < commandLineArgs.length; i++) {
			if (commandLineArgs[i].equalsIgnoreCase("-allowDeadlock")) //$NON-NLS-1$
				avoidDeadlock = false;
		}

		if (avoidDeadlock) {
			UILockListener uiLockListener = new UILockListener(display);
			Platform.getJobManager().setLockListener(uiLockListener);
			display.setSynchronizer(
				new UISynchronizer(display, uiLockListener));
		}

		// attempt to restore a previous workbench state
		try {
			UIStats.start(UIStats.RESTORE_WORKBENCH, "Workbench"); //$NON-NLS-1$

			adviser.preStartup();

			int restoreCode = openPreviousWorkbenchState();
			if (restoreCode == RESTORE_CODE_EXIT) {
				return false;
			}
			if (restoreCode == RESTORE_CODE_RESET) {
				openFirstTimeWindow();
			}
		} finally {
			UIStats.end(UIStats.RESTORE_WORKBENCH, "Workbench"); //$NON-NLS-1$
		}

		forceOpenPerspective();
		
		isStarting = false;
		return true;
	}

	private void initializeSingleClickOption() {
		IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
		boolean openOnSingleClick = store.getBoolean(IPreferenceConstants.OPEN_ON_SINGLE_CLICK);
		boolean selectOnHover = store.getBoolean(IPreferenceConstants.SELECT_ON_HOVER);
		boolean openAfterDelay = store.getBoolean(IPreferenceConstants.OPEN_AFTER_DELAY);
		int singleClickMethod = openOnSingleClick ? OpenStrategy.SINGLE_CLICK : OpenStrategy.DOUBLE_CLICK;
		if (openOnSingleClick) {
			if (selectOnHover)
				singleClickMethod |= OpenStrategy.SELECT_ON_HOVER;
			if (openAfterDelay)
				singleClickMethod |= OpenStrategy.ARROW_KEYS_OPEN;
		}
		OpenStrategy.setOpenMethod(singleClickMethod);
	}

	/*
	 * Initializes the workbench fonts with the stored values.
	 */
	private void initializeFonts() {
		IPreferenceStore store = getPreferenceStore();
		FontRegistry registry = JFaceResources.getFontRegistry();

		//Iterate through the definitions and initialize thier
		//defaults in the preference store.
		FontDefinition[] definitions = FontDefinition.getDefinitions();
		ArrayList fontsToSet = new ArrayList();
		for (int i = 0; i < definitions.length; i++) {
			FontDefinition definition = definitions[i];
			String fontKey = definition.getId();
			installFont(fontKey, registry, store);
			String defaultsTo = definitions[i].getDefaultsTo();
			if (defaultsTo != null){
				PreferenceConverter.setDefault(
					store,
					definition.getId(),
					PreferenceConverter.
						getDefaultFontDataArray(store,defaultsTo));
				
				//If there is no value in the registry pass though the mapping
				if(!registry.hasValueFor(fontKey)) {
					fontsToSet.add(definition);
				}
			}
		}
		
		
		/*
		 * Now that all of the font have been initialized anything
		 * that is still at its defaults and has a defaults to
		 * needs to have its value set in the registry.
		 * Post process to be sure that all of the fonts have the correct
		 * setting before there is an update.
		 */		
		Iterator updateIterator = fontsToSet.iterator();
		while(updateIterator.hasNext()){
			FontDefinition update = (FontDefinition) updateIterator.next();
			registry.put(update.getId(),registry.getFontData(update.getDefaultsTo()));
		}
	}
	/*
	 * Installs the given font in the font registry.
	 * 
	 * @param fontKey the font key
	 * @param registry the font registry
	 * @param store the preference store from which to obtain font data
	 */
	private void installFont(String fontKey, FontRegistry registry, IPreferenceStore store) {
		if (store.isDefault(fontKey))
			return;
		FontData[] font = PreferenceConverter.getFontDataArray(store, fontKey);
		registry.put(fontKey, font);
	}
	
	/*
	 * Initialize the workbench images.
	 * 
	 * @since 3.0
	 */
	private void initializeImages(ImageDescriptor windowImage) {
		if (windowImage != null) {
			WorkbenchImages.getImageRegistry().put(IWorkbenchGraphicConstants.IMG_OBJS_DEFAULT_PROD, windowImage);
			Image image = WorkbenchImages.getImage(IWorkbenchGraphicConstants.IMG_OBJS_DEFAULT_PROD);
			if (image != null) {
				Window.setDefaultImage(image);
			}
		} else {
			// Avoid setting a missing image as the window default image
			WorkbenchImages.getImageRegistry().put(IWorkbenchGraphicConstants.IMG_OBJS_DEFAULT_PROD, ImageDescriptor.getMissingImageDescriptor());
		}
	}
	
	/*
	 * Initialize the workbench colors.
	 * 
	 * @since 3.0
	 */
	private void initializeColors() {
		// @issue some colors are generic; some are app-specific
		WorkbenchColors.startup();
	}

	/**
	 * Returns <code>true</code> if the workbench is in the process of closing.
	 */
	public boolean isClosing() {
		return isClosing;
	}
	
	/*
	 * Returns true if the workbench is in the process of starting
	 */
	/* package */ boolean isStarting() {
		return isStarting;
	}
	
 	/*
 	 * Creates a new workbench window.
 	 * 
 	 * @return the new workbench window
    */
 	private WorkbenchWindow newWorkbenchWindow() {
 		return new WorkbenchWindow(getNewWindowNumber());
	}

	/*
	 * If a perspective was specified on the command line (-perspective)
	 * then force that perspective to open in the active window.
	 */	
	private void forceOpenPerspective() {
		if (getWorkbenchWindowCount() == 0) {
			// there should be an open window by now, bail out.
			return;
		}
		
		String perspId = null;
		String[] commandLineArgs = Platform.getCommandLineArgs();
		for (int i = 0; i < commandLineArgs.length - 1; i++) {
			if (commandLineArgs[i].equalsIgnoreCase("-perspective")) { //$NON-NLS-1$
				perspId = commandLineArgs[i + 1];
				break;
			}
		}
		if (perspId == null) {
			return;
		}
		IPerspectiveDescriptor desc = getPerspectiveRegistry().findPerspectiveWithId(perspId);
		if (desc == null) {
			return;
		}

		IWorkbenchWindow win = getActiveWorkbenchWindow();
		if (win == null) {
			win = getWorkbenchWindows()[0];
		}
		try {
			showPerspective(perspId, win);
		} catch (WorkbenchException e) {
			String msg = "Workbench exception showing specified command line perspective on startup."; //$NON-NLS-1$
			WorkbenchPlugin.log(msg, new Status(Status.ERROR, PlatformUI.PLUGIN_ID, 0, msg, e));
		}
	}

	/*
	 * Create the initial workbench window.
	 */
	private void openFirstTimeWindow() {
		
		// create the workbench window
		WorkbenchWindow newWindow = newWorkbenchWindow();
		newWindow.create();
		windowManager.add(newWindow);

		// Create the initial page.
		try {
			newWindow.openPage(getPerspectiveRegistry().getDefaultPerspective(), getAdviser().getDefaultWindowInput());
		} catch (WorkbenchException e) {
			ErrorDialog.openError(newWindow.getShell(), WorkbenchMessages.getString("Problems_Opening_Page"), //$NON-NLS-1$
			e.getMessage(), e.getStatus());
		}
		newWindow.open();
	}
	
	/*
	 * Create the workbench UI from a persistence file.
	 * 
	 * @return RESTORE_CODE_OK if a window was opened,
	 * RESTORE_CODE_RESET if no window was opened but one should be,
	 * and RESTORE_CODE_EXIT if the workbench should close immediately
	 */
	private int openPreviousWorkbenchState() {
		
		if (!getPreferenceStore().getBoolean(IWorkbenchPreferences.SHOULD_SAVE_WORKBENCH_STATE)) {
			return RESTORE_CODE_RESET;
		}
		// Read the workbench state file.
		final File stateFile = getWorkbenchStateFile();
		// If there is no state file cause one to open.
		if (!stateFile.exists())
			return RESTORE_CODE_RESET;

		final int result[] = { RESTORE_CODE_OK };
		Platform.run(new SafeRunnable(WorkbenchMessages.getString("ErrorReadingState")) { //$NON-NLS-1$
			public void run() throws Exception {
				FileInputStream input = new FileInputStream(stateFile);
				BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf-8")); //$NON-NLS-1$
				IMemento memento = XMLMemento.createReadRoot(reader);

				// Validate known version format
				String version = memento.getString(IWorkbenchConstants.TAG_VERSION);
				boolean valid = false;
				for (int i = 0; i < VERSION_STRING.length; i++) {
					if (VERSION_STRING[i].equals(version)) {
						valid = true;
						break;
					}
				}
				if (!valid) {
					reader.close();
					MessageDialog.openError(
						(Shell) null,
						WorkbenchMessages.getString("Restoring_Problems"), //$NON-NLS-1$
					WorkbenchMessages.getString("Invalid_workbench_state_ve")); //$NON-NLS-1$
					stateFile.delete();
					result[0] = RESTORE_CODE_RESET;
					return;
				}

				// Validate compatible version format
				// We no longer support the release 1.0 format
				if (VERSION_STRING[0].equals(version)) {
					reader.close();
					boolean ignoreSavedState = new MessageDialog(
						null, 
						WorkbenchMessages.getString("Workbench.incompatibleUIState"), //$NON-NLS-1$
						null,
						WorkbenchMessages.getString("Workbench.incompatibleSavedStateVersion"), //$NON-NLS-1$
						MessageDialog.WARNING,
						new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 
						0).open() == 0; 	// OK is the default
					if (ignoreSavedState) {
						stateFile.delete();
						result[0] = RESTORE_CODE_RESET;
					} else {
						result[0] = RESTORE_CODE_EXIT;
					}
					return;
				}

				// Restore the saved state
				IStatus restoreResult = restoreState(memento);
				reader.close();
				if (restoreResult.getSeverity() == IStatus.ERROR) {
					ErrorDialog.openError(
						null,
						WorkbenchMessages.getString("Workspace.problemsTitle"), //$NON-NLS-1$
						WorkbenchMessages.getString("Workbench.problemsRestoringMsg"), //$NON-NLS-1$
						restoreResult);
				}
			}
			public void handleException(Throwable e) {
				super.handleException(e);
				result[0] = RESTORE_CODE_RESET;
				stateFile.delete();
			}

		});
		// ensure at least one window was opened
		if (result[0] == RESTORE_CODE_OK && windowManager.getWindows().length == 0)
			result[0] = RESTORE_CODE_RESET;
		return result[0];
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IWorkbenchWindow openWorkbenchWindow(IAdaptable input) throws WorkbenchException {
		return openWorkbenchWindow(getPerspectiveRegistry().getDefaultPerspective(), input);
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IWorkbenchWindow openWorkbenchWindow(final String perspID, final IAdaptable input) throws WorkbenchException {
		// Run op in busy cursor.
		final Object[] result = new Object[1];
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				try {
					result[0] = busyOpenWorkbenchWindow(perspID, input);
				} catch (WorkbenchException e) {
					result[0] = e;
				}
			}
		});
		if (result[0] instanceof IWorkbenchWindow) {
			return (IWorkbenchWindow) result[0];
		} else if (result[0] instanceof WorkbenchException) {
			throw (WorkbenchException) result[0];
		} else {
			throw new WorkbenchException(WorkbenchMessages.getString("Abnormal_Workbench_Conditi")); //$NON-NLS-1$
		}
	}

	/*
	 * Record the workbench UI in a document
	 */
	private XMLMemento recordWorkbenchState() {
		XMLMemento memento = XMLMemento.createWriteRoot(IWorkbenchConstants.TAG_WORKBENCH);
		IStatus status = saveState(memento);
		if (status.getSeverity() != IStatus.OK) {
			ErrorDialog.openError((Shell)null,
				WorkbenchMessages.getString("Workbench.problemsSaving"),  //$NON-NLS-1$
			WorkbenchMessages.getString("Workbench.problemsSavingMsg"), //$NON-NLS-1$
			status);
		}
		return memento;
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public boolean restart() {
		// this is the return code from run() to trigger a restart
		return close(PlatformUI.RETURN_RESTART, false);
	}
	
	/*
	 * Restores the state of the previously saved workbench
	 */
	private IStatus restoreState(IMemento memento) {

		MultiStatus result = new MultiStatus(
			PlatformUI.PLUGIN_ID,IStatus.OK,
			WorkbenchMessages.getString("Workbench.problemsRestoring"),null); //$NON-NLS-1$
		IMemento childMem;
		try {
			UIStats.start(UIStats.RESTORE_WORKBENCH,"MRUList"); //$NON-NLS-1$
			IMemento mruMemento = memento.getChild(IWorkbenchConstants.TAG_MRU_LIST); //$NON-NLS-1$
			if (mruMemento != null) {
				result.add(getEditorHistory().restoreState(mruMemento));
			}
		} finally {
			UIStats.end(UIStats.RESTORE_WORKBENCH,"MRUList"); //$NON-NLS-1$
		}
		// Get the child windows.
		IMemento[] children = memento.getChildren(IWorkbenchConstants.TAG_WINDOW);

		// Read the workbench windows.
		for (int x = 0; x < children.length; x++) {
			childMem = children[x];
			WorkbenchWindow newWindow = newWorkbenchWindow();
			newWindow.create();

			// allow the application to specify an initial perspective to open
			String initialPerspectiveId = getAdviser().getInitialWindowPerspectiveId();
			if (initialPerspectiveId != null) {
				IPerspectiveDescriptor desc = getPerspectiveRegistry().findPerspectiveWithId(initialPerspectiveId);
				if (desc != null) {
					result.merge(newWindow.restoreState(childMem, desc));
				}    
			}
			windowManager.add(newWindow);
			try {
				getAdviser().postWindowRestore(newWindow.getWindowConfigurer());
			} catch (WorkbenchException e) {
				result.add(e.getStatus());
			}
			newWindow.open();
		}
		return result;
	}

	/**
	 * Returns an array of all plugins that extend the 
	 * <code>org.eclipse.ui.startup</code> extension point.
	 */
	public IPluginDescriptor[] getEarlyActivatedPlugins() {
		IPluginRegistry registry = Platform.getPluginRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PlatformUI.PLUGIN_ID, IWorkbenchConstants.PL_STARTUP);
		IExtension[] extensions = point.getExtensions();
		IPluginDescriptor result[] = new IPluginDescriptor[extensions.length];
		for (int i = 0; i < extensions.length; i++) {
			result[i] = extensions[i].getDeclaringPluginDescriptor();
		}
		return result;
	}
	
	/*
	 * Starts all plugins that extend the <code>org.eclipse.ui.startup</code>
	 * extension point, and that the user has not disabled via the preference page.
	 */
	private void startPlugins() {
		Runnable work = new Runnable() {
			IPreferenceStore store = getPreferenceStore();
			final String pref = store.getString(IPreferenceConstants.PLUGINS_NOT_ACTIVATED_ON_STARTUP);
			public void run() {
				IPluginDescriptor descriptors[] = getEarlyActivatedPlugins();
				for (int i = 0; i < descriptors.length; i++) {
					final IPluginDescriptor pluginDescriptor = descriptors[i];
					SafeRunnable code = new SafeRunnable() {
						public void run() throws Exception {
							String id = pluginDescriptor.getUniqueIdentifier() + IPreferenceConstants.SEPARATOR;
							if (pref.indexOf(id) < 0) {
								Plugin plugin = pluginDescriptor.getPlugin();
								IStartup startup = (IStartup) plugin;
								startup.earlyStartup();
							}
						}
						public void handleException(Throwable exception) {
							WorkbenchPlugin.log("Unhandled Exception", new Status(IStatus.ERROR, "org.eclipse.ui", 0, "Unhandled Exception", exception)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					};
					Platform.run(code);
				}
			}
		};

		Thread thread = new Thread(work);
		thread.start();
	}

	/**
	 * Creates the <code>Display</code> to be used by the workbench.
	 * 
	 * @param applicationName the application name, or <code>null</code> if none
	 * @return the display
	 */
	private Display createDisplay(String applicationName) {
		// setup the application name used by SWT to lookup resources on some platforms
		if (applicationName != null) {
			Display.setAppName(applicationName);
		}
		
		// create the display
		Display display = null;
		if (Policy.DEBUG_SWT_GRAPHICS) {
			DeviceData data = new DeviceData();
			data.tracking = true;
			display = new Display(data);
		} else {
			display = new Display();
		}
		
		// workaround for 1GEZ9UR and 1GF07HN
		display.setWarnings(false);

		//Set the priority higher than normal so as to be higher 
		//than the JobManager.
		Thread.currentThread().setPriority(
			Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 1));

		// react to display close event by closing the workhench nicely
		display.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				event.doit = close();
			}
		});
		
		return display;
	}
	
	/**
	 * Internal method for running the workbench UI. This entails processing
	 * and dispatching events until the workbench is closed or restarted.
	 * 
	 * @return return code {@link PlatformUI#RETURN_OK RETURN_OK} for normal exit; 
	 * {@link PlatformUI#RETURN_RESTART RETURN_RESTART} if the workbench was terminated
	 * with a call to {@link IWorkbench#restart IWorkbench.restart}; 
	 * {@link PlatformUI#RETURN_UNSTARTABLE RETURN_UNSTARTABLE} if the workbench could
	 * not be started; other values reserved for future use
	 * @since 3.0
	 */
	private int runUI() {
		UIStats.start(UIStats.START_WORKBENCH,"Workbench"); //$NON-NLS-1$

		String appName = null;
		ImageDescriptor windowImage = null;
		{
			// extract app name and window image from primary feature
			IPlatformConfiguration conf = BootLoader.getCurrentPlatformConfiguration();
			String id = conf.getPrimaryFeatureIdentifier();
			AboutInfo aboutInfo = null;
			if (id != null) {
				aboutInfo = AboutInfo.readFeatureInfo(id);
			}
			if (aboutInfo != null) {
				appName = aboutInfo.getAppName();
				windowImage = aboutInfo.getWindowImage();
				setProductName(aboutInfo.getProductName()); 
			}
		}

		// create and startup the display for the workbench
		Display display = createDisplay(appName);
				
		try {
			// install backstop to catch exceptions thrown out of event loop
			Window.IExceptionHandler handler = new ExceptionHandler();
			Window.setExceptionHandler(handler);
			
			// initialize workbench and restore or open one window
			
			boolean initOK = init(windowImage, display);
			
			// drop the splash screen now that a workbench window is up
			Platform.endSplash();
			
			// let the adviser run its start up code
			if (initOK) {
				adviser.postStartup(); // may trigger a close/restart
			}
			
			if (initOK && runEventLoop) {
				// start eager plug-ins
				startPlugins();
				
				display.asyncExec(new Runnable() {
					public void run() {
						UIStats.end(UIStats.START_WORKBENCH,"Workbench"); //$NON-NLS-1$
					}
				});
				
				getWorkbenchTestable().init(display, this);
				
				// the event loop
				runEventLoop(handler, display);
			}
			
			// shutdown in an orderly way after event loop finishes
			shutdown();
		} finally {
			// mandatory clean up
			if (!display.isDisposed()) {
				display.dispose();
			}
		}
		
		// restart or exit based on returnCode
		Workbench.instance = null;
		return returnCode;
	}

	/*
	 * Runs an event loop for the workbench.
	 */
	private void runEventLoop(Window.IExceptionHandler handler, Display display) {
		runEventLoop = true;
		while (runEventLoop) {
			try {
				if (!display.readAndDispatch()) {
					getAdviser().eventLoopIdle(display);
				}
			} catch (Throwable t) {
				handler.handleException(t);
			}
		}
	}
	
	/*
	 * Saves the current state of the workbench so it can be restored later on
	 */
	private IStatus saveState(IMemento memento) {
		MultiStatus result = new MultiStatus(
			PlatformUI.PLUGIN_ID,IStatus.OK,
			WorkbenchMessages.getString("Workbench.problemsSaving"),null); //$NON-NLS-1$

		// Save the version number.
		memento.putString(IWorkbenchConstants.TAG_VERSION, VERSION_STRING[1]);

		// Save the workbench windows.
		IWorkbenchWindow[] windows = getWorkbenchWindows();
		for (int nX = 0; nX < windows.length; nX++) {
			WorkbenchWindow window = (WorkbenchWindow) windows[nX];
			IMemento childMem = memento.createChild(IWorkbenchConstants.TAG_WINDOW);
			result.merge(window.saveState(childMem));
		}
		result.add(getEditorHistory().saveState(memento.createChild(IWorkbenchConstants.TAG_MRU_LIST))); //$NON-NLS-1$
		return result;
	}
	
	/*
	 * Save the workbench UI in a persistence file.
	 */
	private boolean saveMementoToFile(XMLMemento memento) {
		// Save it to a file.
		File stateFile = getWorkbenchStateFile();
		try {
			FileOutputStream stream = new FileOutputStream(stateFile);
			OutputStreamWriter writer = new OutputStreamWriter(stream, "utf-8"); //$NON-NLS-1$
			memento.save(writer);
			writer.close();
		} catch (IOException e) {
			stateFile.delete();
			MessageDialog.openError((Shell) null, WorkbenchMessages.getString("SavingProblem"), //$NON-NLS-1$
			WorkbenchMessages.getString("ProblemSavingState")); //$NON-NLS-1$
			return false;
		}

		// Success !
		return true;
	}

	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IWorkbenchPage showPerspective(String perspectiveId, IWorkbenchWindow window) throws WorkbenchException {
		Assert.isNotNull(perspectiveId);

		// If the specified window has the requested perspective open, then the window
		// is given focus and the perspective is shown. The page's input is ignored.
		WorkbenchWindow win = (WorkbenchWindow) window;
		if (win != null) {
			WorkbenchPage page = win.getActiveWorkbenchPage();
			if (page != null) {
				IPerspectiveDescriptor perspectives[] = page.getOpenedPerspectives();
				for (int i = 0; i < perspectives.length; i++) {
					IPerspectiveDescriptor persp = perspectives[i];
					if (perspectiveId.equals(persp.getId())) {
						win.getShell().open();
						page.setPerspective(persp);
						return page;
					}
				}
			}
		}

		// If another window that has the workspace root as input and the requested
		// perpective open and active, then the window is given focus.
		IAdaptable input = adviser.getDefaultWindowInput();
		IWorkbenchWindow[] windows = getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			win = (WorkbenchWindow) windows[i];
			if (window != win) {
				WorkbenchPage page = win.getActiveWorkbenchPage();
				if (page != null) {
					boolean inputSame = false;
					if (input == null)
						inputSame = (page.getInput() == null);
					else
						inputSame = input.equals(page.getInput());
					if (inputSame) {
						Perspective persp = page.getActivePerspective();
						if (perspectiveId.equals(persp.getDesc().getId())) {
							Shell shell = win.getShell();
							shell.open();
							if(shell.getMinimized())
								shell.setMinimized(false);
							return page;
						}
					}
				}
			}
		}

		// Otherwise the requested perspective is opened and shown in the specified
		// window or in a new window depending on the current user preference for opening
		// perspectives, and that window is given focus.
		win = (WorkbenchWindow) window;
		if (win != null) {
			IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
			int mode = store.getInt(IPreferenceConstants.OPEN_PERSP_MODE);
			IWorkbenchPage page = win.getActiveWorkbenchPage();
			IPerspectiveDescriptor persp = null;
			if (page != null)
				persp = page.getPerspective();

			// Only open a new window if user preference is set and the window
			// has an active perspective.
			if (IPreferenceConstants.OPM_NEW_WINDOW == mode && persp != null) {
				IWorkbenchWindow newWindow = openWorkbenchWindow(perspectiveId, input);
				return newWindow.getActivePage();
			} else {
				IPerspectiveDescriptor desc = getPerspectiveRegistry().findPerspectiveWithId(perspectiveId);
				if (desc == null)
					throw new WorkbenchException(WorkbenchMessages.getString("WorkbenchPage.ErrorRecreatingPerspective")); //$NON-NLS-1$
				win.getShell().open();
				if (page == null)
					page = win.openPage(perspectiveId, input);
				else
					page.setPerspective(desc);
				return page;
			}
		}

		// Just throw an exception....
		throw new WorkbenchException(WorkbenchMessages.format("Workbench.showPerspectiveError", new Object[] { perspectiveId })); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IWorkbenchPage showPerspective(String perspectiveId, IWorkbenchWindow window, IAdaptable input) throws WorkbenchException {
		Assert.isNotNull(perspectiveId);

		// If the specified window has the requested perspective open and the same requested
		// input, then the window is given focus and the perspective is shown.
		boolean inputSameAsWindow = false;
		WorkbenchWindow win = (WorkbenchWindow) window;
		if (win != null) {
			WorkbenchPage page = win.getActiveWorkbenchPage();
			if (page != null) {
				boolean inputSame = false;
				if (input == null)
					inputSame = (page.getInput() == null);
				else
					inputSame = input.equals(page.getInput());
				if (inputSame) {
					inputSameAsWindow = true;
					IPerspectiveDescriptor perspectives[] = page.getOpenedPerspectives();
					for (int i = 0; i < perspectives.length; i++) {
						IPerspectiveDescriptor persp = perspectives[i];
						if (perspectiveId.equals(persp.getId())) {
							win.getShell().open();
							page.setPerspective(persp);
							return page;
						}
					}
				}
			}
		}

		// If another window has the requested input and the requested
		// perpective open and active, then that window is given focus.
		IWorkbenchWindow[] windows = getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			win = (WorkbenchWindow) windows[i];
			if (window != win) {
				WorkbenchPage page = win.getActiveWorkbenchPage();
				if (page != null) {
					boolean inputSame = false;
					if (input == null)
						inputSame = (page.getInput() == null);
					else
						inputSame = input.equals(page.getInput());
					if (inputSame) {
						Perspective persp = page.getActivePerspective();
						if (perspectiveId.equals(persp.getDesc().getId())) {
							win.getShell().open();
							return page;
						}
					}
				}
			}
		}

		// If the specified window has the same requested input but not the requested
		// perspective, then the window is given focus and the perspective is opened and shown
		// on condition that the user preference is not to open perspectives in a new window.
		win = (WorkbenchWindow) window;
		if (inputSameAsWindow && win != null) {
			IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
			int mode = store.getInt(IPreferenceConstants.OPEN_PERSP_MODE);

			if (IPreferenceConstants.OPM_NEW_WINDOW != mode) {
				IWorkbenchPage page = win.getActiveWorkbenchPage();
				IPerspectiveDescriptor desc = getPerspectiveRegistry().findPerspectiveWithId(perspectiveId);
				if (desc == null)
					throw new WorkbenchException(WorkbenchMessages.getString("WorkbenchPage.ErrorRecreatingPerspective")); //$NON-NLS-1$
				win.getShell().open();
				if (page == null)
					page = win.openPage(perspectiveId, input);
				else
					page.setPerspective(desc);
				return page;
			}
		}

		// If the specified window has no active perspective, then open the
		// requested perspective and show the specified window.
		if (win != null) {
			IWorkbenchPage page = win.getActiveWorkbenchPage();
			IPerspectiveDescriptor persp = null;
			if (page != null)
				persp = page.getPerspective();
			if (persp == null) {
				IPerspectiveDescriptor desc = getPerspectiveRegistry().findPerspectiveWithId(perspectiveId);
				if (desc == null)
					throw new WorkbenchException(WorkbenchMessages.getString("WorkbenchPage.ErrorRecreatingPerspective")); //$NON-NLS-1$
				win.getShell().open();
				if (page == null)
					page = win.openPage(perspectiveId, input);
				else
					page.setPerspective(desc);
				return page;
			}
		}

		// Otherwise the requested perspective is opened and shown in a new window, and the
		// window is given focus.
		IWorkbenchWindow newWindow = openWorkbenchWindow(perspectiveId, input);
		return newWindow.getActivePage();
	}

	/*
	 * Shuts down the application.
	 */
	private void shutdown() {
		// shutdown application-specific portions first
		adviser.postShutdown();
		
		// shutdown the rest of the workbench
		WorkbenchColors.shutdown();
		JFaceColors.disposeColors();
		if(getDecoratorManager() != null) {
			((DecoratorManager) getDecoratorManager()).shutdown();
		}
		RoleManager.shutdown();
	}

	/* (non-Javadoc)
	 * Method declared on IWorkbench.
	 */
	public IDecoratorManager getDecoratorManager() {
		return WorkbenchPlugin.getDefault().getDecoratorManager();
	}

	/*
	 * Returns the workbench window which was last known being
	 * the active one, or <code>null</code>.
	 */
	private WorkbenchWindow getActivatedWindow() {
		if (activatedWindow != null) {
			Shell shell = activatedWindow.getShell();
			if (shell != null && !shell.isDisposed()) {
				return activatedWindow;
			}
		}
		
		return null;
	}
	
	/*
	 * Sets the workbench window which was last known being the
	 * active one, or <code>null</code>.
	 */
	/* package */ void setActivatedWindow(WorkbenchWindow window) {
		activatedWindow = window;
	}
	
	/**
	 * Returns the unique object that applications use to configure the
	 * workbench.
	 * <p>
	 * IMPORTANT This method is declared package-private to prevent regular
	 * plug-ins from downcasting IWorkbench to Workbench and getting
	 * hold of the workbench configurer that would allow them to tamper with the
	 * workbench. The workbench configurer is available only to the application.
	 * </p>
	 */
	/* package */ WorkbenchConfigurer getWorkbenchConfigurer() {
		if (workbenchConfigurer == null) {
			workbenchConfigurer = new WorkbenchConfigurer();
		}
		return workbenchConfigurer;
	}
	
	/**
	 * Returns the workbench adviser that created this workbench.
	 * <p>
	 * IMPORTANT This method is declared package-private to prevent regular
	 * plug-ins from downcasting IWorkbench to Workbench and getting
	 * hold of the workbench adviser that would allow them to tamper with the
	 * workbench. The workbench adviser is internal to the application.
	 * </p>
	 */
	/* package */ WorkbenchAdviser getAdviser() {
		return adviser;
	}
	
	/**
	 * Returns the default perspective id.
	 * 
	 * @return the default perspective id, or <code>null</code> if none
	 */
	public String getDefaultPerspectiveId() {
		return getAdviser().getInitialWindowPerspectiveId();
	}

	/**
	 * Returns the default workbench window input.
	 * 
	 * @return the default window input or <code>null</code> if none
	 */
	public IAdaptable getDefaultWindowInput() {
		return getAdviser().getDefaultWindowInput();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbench
	 * @since 3.0
	 */
	public IElementFactory getElementFactory(String factoryId) {
		Assert.isNotNull(factoryId);
		return WorkbenchPlugin.getDefault().getElementFactory(factoryId);
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbench#getActivityManager(java.lang.String, boolean)
	 */
	public IObjectActivityManager getActivityManager(String id, boolean create) {
		if(RoleManager.getInstance().isFiltering())
			return ObjectActivityManager.getManager(id,create);
		else
			return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbench#getProgressManager()
	 */
	public IProgressManager getProgressManager() {
		return ProgressManager.getInstance();
	}

	/**
	 * Returns the name of the product.
	 * 
	 * @return the product name, or <code>null</code> if none
	 * @since 3.0
	 */
	public String getProductName() {
		return productName;
	}
	
	/**
	 * Sets the name of the product.
	 * 
	 * @param name the product name, or <code>null</code> if none
	 * @since 3.0
	 */
	public void setProductName(String name) {
		productName = name;
	}
}