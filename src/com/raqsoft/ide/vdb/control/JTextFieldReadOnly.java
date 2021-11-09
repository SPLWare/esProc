package com.raqsoft.ide.vdb.control;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

public class JTextFieldReadOnly extends JTextField {
	private static final long serialVersionUID = 1L;

	public JTextFieldReadOnly(String s, int columns) {
		super(s, columns);
		setEditable(false);
		addFocusListener(new FocusListener() {
			int caretPosition = 0;

			public void focusGained(FocusEvent e) {
				if (!getCaret().isVisible()) {
					getCaret().setVisible(true);
					setCaretPosition(caretPosition);
				}
			}

			public void focusLost(FocusEvent e) {
				if (getCaretPosition() > 0)
					caretPosition = getCaretPosition();
			}
		});

	}

	public JTextFieldReadOnly() {
		this(new String(), 0);
	}

	public JTextFieldReadOnly(int columns) {
		this(new String(), columns);
	}

	public JTextFieldReadOnly(String s) {
		this(s, 0);
	}

}
