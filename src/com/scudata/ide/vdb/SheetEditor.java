package com.scudata.ide.vdb;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import com.scudata.ide.dfx.base.PanelValue;
import com.scudata.ide.vdb.control.VDBTreeNode;
import com.scudata.ide.vdb.menu.MenuVDB;

public class SheetEditor extends JInternalFrame implements ISheet{
	PanelValue sequence = new PanelValue();
	JTextPane string = new JTextPane();
	JLabel image = new JLabel();

	private VDBTreeNode node;
	
	private boolean isDataChanged = false;
	public SheetEditor(VDBTreeNode node) throws Exception {
		super(node.getTitle(), true, true, true, true);
		this.node = node;
//		this.setFrameIcon(GM.getMenuImageIcon("editor"));

		getContentPane().setLayout(new BorderLayout());
		Component comp=null;
		switch(node.getType()){
		case VDBTreeNode.TYPE_SEQUENCE:
			sequence.tableValue.setValue(node.getData());
			comp = sequence; 
			break;
		case VDBTreeNode.TYPE_STRING:
			JScrollPane scrollPane = new JScrollPane(string);
			String data = (String)node.getData();
			string.setText(data);
			comp = scrollPane;
			break;
		case VDBTreeNode.TYPE_IMAGE:
			byte[] backImage = (byte[])node.getData();
			Image bkImage = new ImageIcon(backImage).getImage();
			image = new JLabel(new ImageIcon(bkImage) );
			comp = image;
			break;
		}
		getContentPane().add( comp, BorderLayout.CENTER);
		
		addInternalFrameListener(new Listener(this));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	}

	public boolean save() {
		return true;
	}

	public boolean close() {
		// 先停止所有编辑器的编辑
		if(node.getType()==VDBTreeNode.TYPE_SEQUENCE){
			sequence.tableValue.acceptText();
		}

		boolean closed = true;
		if (isDataChanged) {
			String t1, t2;
//			t1 = Lang.getText("sheeteditor.asksave", Lang.getText("sheeteditor.report"), fileName);
//			t2 = Lang.getText("sheeteditor.asksavetitle");
			int option = JOptionPane.YES_OPTION;//JOptionPane.showConfirmDialog(GV.appFrame, t1, t2, JOptionPane.YES_NO_CANCEL_OPTION);
			switch (option) {
			case JOptionPane.YES_OPTION:
				closed = save();
				break;
			case JOptionPane.NO_OPTION:
				closed = true;
				break;
			default:
				closed = false;
			}
		}
		if (closed) {
			dispose();
		}
		return closed;
	}

	public boolean isPasteable() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isSaveable() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isImportable() {
		// TODO Auto-generated method stub
		return false;
	}

}

class Listener extends InternalFrameAdapter {
	SheetEditor se;

	public Listener(SheetEditor parent) {
		super();
		se = parent;
	}

	public void internalFrameActivated(InternalFrameEvent e) {
		// 用线程启动以等待别的窗口彻底关闭才激活该窗口
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MenuVDB menu = VDB.getInstance().getMenuVDB();
//				menu.sheetActivated(se);
			}
		});
	}

	public void internalFrameClosing(InternalFrameEvent e) {
//		VDB.getInstance().closeSheet(se);
	}
}
