package com.scudata.ide.vdb.control;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class PanelConsole extends JPanel {
	private static final long serialVersionUID = 1L;

	public PanelConsole(JTextArea textArea) {
		super(new BorderLayout());
		this.add(new JScrollPane(textArea), BorderLayout.CENTER);

	}
}
