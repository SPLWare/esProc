package com.scudata.ide.common.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxEditor;
import javax.swing.event.EventListenerList;

import com.scudata.common.Logger;

/**
 * ÏÂÀ­¿ò±à¼­Æ÷
 */

public abstract class AbstractComboBoxEditor implements ComboBoxEditor {

	/**
	 * ¼àÌýÆ÷ÁÐ±í
	 */
	EventListenerList listenerList = new EventListenerList();

	/**
	 * Ôö¼Ó¼àÌýÆ÷
	 */
	public void addActionListener(ActionListener listener) {
		listenerList.add(ActionListener.class, listener);
	}

	/**
	 * É¾³ý¼àÌýÆ÷
	 */
	public void removeActionListener(ActionListener listener) {
		listenerList.remove(ActionListener.class, listener);
	}

	/**
	 * Ö´ÐÐÊÂ¼þ
	 * 
	 * @param e
	 */
	protected void fireActionPerformed(ActionEvent e) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ActionListener.class) {
				Logger.debug(listeners[i].getClass().getName());
				((ActionListener) listeners[i + 1]).actionPerformed(e);
			}
		}
	}

}
