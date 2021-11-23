package com.scudata.ide.vdb.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import com.scudata.ide.common.GM;
import com.scudata.ide.vdb.resources.IdeMessage;

public class DialogInputText extends RQDialog {
	private static final long serialVersionUID = 1L;

	public JButton jBCopy = new JButton();
	public JButton jBPaste = new JButton();

	public JEditorPane editorPane = new JEditorPane();

	public DialogInputText(boolean editable) {
		super();
		try {
			editorPane.setEditable(editable);
			init();
			resetText(editable);
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	public void setText(String text) {
		editorPane.setText(text);
		editorPane.selectAll();
	}

	public String getText() {
		return editorPane.getText();
	}

	public void setRichText(String url) {
		try {
			File file = new File(url);
			editorPane.setContentType("text/html");
			editorPane.setPage("file:" + file.getAbsolutePath());
			editorPane.setEditable(false);
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	private void resetText(boolean editable) {
		setTitle(IdeMessage.get().getMessage("dialoginputtext.texteditbox"));
		jBCancel.setText(IdeMessage.get().getMessage("button.cancel") + "(X)");
		jBCopy.setText(IdeMessage.get().getMessage("button.copy") + "(C)");
		jBPaste.setText(IdeMessage.get().getMessage("button.paste") + "V");
	}

	private void init() throws Exception {
		jBCopy.setMnemonic('C');
		jBCopy.setText("¸´ÖÆ(C)");
		jBCopy.addActionListener(new DialogInputText_jBCopy_actionAdapter(this));
		jBPaste.setMnemonic('V');
		jBPaste.setText("Õ³Ìù(V)");
		jBPaste.addActionListener(new DialogInputText_jBPaste_actionAdapter(this));
		panelCenter.add(new JScrollPane(editorPane), BorderLayout.CENTER);
		getContentPane().add(panelEast, BorderLayout.EAST);
		panelEast.add(jBCopy);
		panelEast.add(jBPaste);
	}

	void jBCopy_actionPerformed(ActionEvent e) {
		editorPane.copy();
	}

	void jBPaste_actionPerformed(ActionEvent e) {
		editorPane.paste();
	}
}

class DialogInputText_jBCopy_actionAdapter implements java.awt.event.ActionListener {
	DialogInputText adaptee;

	DialogInputText_jBCopy_actionAdapter(DialogInputText adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCopy_actionPerformed(e);
	}
}

class DialogInputText_jBPaste_actionAdapter implements java.awt.event.ActionListener {
	DialogInputText adaptee;

	DialogInputText_jBPaste_actionAdapter(DialogInputText adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBPaste_actionPerformed(e);
	}
}
