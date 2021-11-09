package com.raqsoft.ide.vdb.panel;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;

import com.raqsoft.ide.common.EditListener;
import com.raqsoft.ide.vdb.control.VDBTreeNode;

public abstract class PanelEditor extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	VDBTreeNode node;
	EditListener listener;
	KeyListener keyListener;
	
	boolean isIniting = false;
	public void beforeInit(){
		isIniting = true;
	}
	
	public void afterInit(){
		isIniting = false;
	}

	public PanelEditor(EditListener listener){
		this.listener = listener;
		loadKeyListener();
	}
	
	public abstract void setNode(VDBTreeNode node);
	public abstract VDBTreeNode getNode();
	
	void loadKeyListener(){
		keyListener = new KeyListener(){
			public void keyTyped(KeyEvent e) {
				if( isIniting ) return;
				listener.editChanged(null);
			}

			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
			}
		};
	}
	
}