package com.scudata.ide.common;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.JButton;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.ToolbarGradient;

/**
 * The base class of the IDE application toolbar
 *
 */
public abstract class AppToolBar extends ToolbarGradient {
	private static final long serialVersionUID = 1L;

	/**
	 * Container for buttons
	 */
	public static HashMap<Short, AbstractButton> buttonHolder = new HashMap<Short, AbstractButton>();
	/**
	 * Common MessageManager
	 */
	protected MessageManager mm = IdeCommonMessage.get();

	/**
	 * Dimension of the seperator
	 */
	public Dimension seperator = new Dimension(6, HEIGHT);

	/**
	 * Constructor
	 */
	public AppToolBar() {
		this(IdeCommonMessage.get().getMessage("public.toolbar"));
	}

	/**
	 * Constructor
	 * 
	 * @param name
	 *            Tooltip of the toolbar
	 */
	public AppToolBar(String name) {
		super(name);
		this.setFloatable(false);
		setToolTipText(mm.getMessage("public.toolbar"));
	}

	/**
	 * Set whether the toolbar is enabled
	 * 
	 * @param enabled
	 */
	public abstract void setBarEnabled(boolean enabled);

	public abstract void executeCmd(short cmdId);

	/**
	 * ActionListener for toolbar buttons
	 */
	protected ActionListener actionNormal = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String menuId = "";
			try {
				menuId = e.getActionCommand();
				short cmdId = Short.parseShort(menuId);
				executeCmd(cmdId);
			} catch (Exception ex) {
				GM.showException(GV.appFrame, ex);
			}
		}
	};

	/**
	 * Get button
	 * 
	 * @param cmdId
	 *            Command ID
	 * @param menuId
	 *            Menu name
	 * @return
	 */
	public JButton getCommonButton(short cmdId, String menuId) {
		JButton b = getButton(cmdId, menuId, mm.getMessage(GC.MENU + menuId),
				actionNormal);
		buttonHolder.put(cmdId, b);
		b.setFocusable(false);
		return b;
	}

	/**
	 * Set whether the button is enabled
	 * 
	 * @param cmdId
	 *            Command ID
	 * @param enabled
	 *            Whether the button is enabled
	 */
	public void setButtonEnabled(short cmdId, boolean enabled) {
		setButtonsEnabled(new short[] { cmdId }, enabled);
	}

	/**
	 * Set whether the buttons are enabled
	 * 
	 * @param cmdIds
	 *            Command IDs
	 * @param enabled
	 *            Whether the buttons are enabled
	 */
	public void setButtonsEnabled(short cmdIds[], boolean enabled) {
		for (int i = 0; i < cmdIds.length; i++) {
			AbstractButton b = buttonHolder.get(cmdIds[i]);
			if (b != null)
				b.setEnabled(enabled);
		}
	}

	/**
	 * Set whether the button is visible
	 * 
	 * @param cmdId
	 *            Command ID
	 * @param enabled
	 *            Whether the button is visible
	 */
	public void setButtonVisible(short cmdId, boolean visible) {
		setButtonsVisible(new short[] { cmdId }, visible);
	}

	/**
	 * Set whether the buttons are visible
	 * 
	 * @param cmdIds
	 *            Command IDs
	 * @param enabled
	 *            Whether the buttons are visible
	 */
	public void setButtonsVisible(short cmdIds[], boolean visible) {
		for (int i = 0; i < cmdIds.length; i++) {
			(buttonHolder.get(cmdIds[i])).setVisible(visible);
		}
	}

	/**
	 * Set whether the save button is enabled
	 * 
	 * @param enable
	 *            Whether the save button is enabled
	 */
	public void enableSave(boolean enable) {
		AbstractButton saveButton = buttonHolder.get(GC.iSAVE);
		if (saveButton != null) {
			saveButton.setEnabled(enable);
		}
	}

}
