package com.raqsoft.ide.vdb.panel;

import java.awt.BorderLayout;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.raqsoft.ide.common.EditListener;
import com.raqsoft.ide.vdb.control.VDBTreeNode;


public class PanelScript extends PanelEditor{
	private static final long serialVersionUID = 1L;
	
	RSyntaxTextArea rstaScript;
	
	public PanelScript(EditListener listener){
		super(listener);
		setLayout(new BorderLayout());
		init();
	}
	
	void init(){
		rstaScript = new RSyntaxTextArea(20, 60);
		rstaScript
				.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
		rstaScript.setCodeFoldingEnabled(true);
//		rstaScript.setFont(new Font(config.getEditorFont(), 0, config
//				.getEditorSize()));
		RTextScrollPane sp = new RTextScrollPane(rstaScript);
		rstaScript.addKeyListener(keyListener);
		add(rstaScript,BorderLayout.CENTER);
	}
	
	void setScript(String scr){
		beforeInit();
		rstaScript.setText(scr);
		afterInit();
	}
	
	String getScript(){
		String scr = rstaScript.getText();
		return scr;
	}

	public void setNode(VDBTreeNode node) {
		this.node = node;
		String scr = (String)node.getData();
		setScript( scr );
	}

	public VDBTreeNode getNode(){
		String scr = getScript();
		node.saveData(scr);
		return node;
	}

}