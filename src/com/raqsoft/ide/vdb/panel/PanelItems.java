package com.raqsoft.ide.vdb.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import com.raqsoft.ide.common.EditListener;
import com.raqsoft.ide.vdb.control.VDBTreeNode;

public abstract class PanelItems extends PanelEditor {
	private static final long serialVersionUID = 1L;

	private JList listView = new JList();
	DefaultListModel listItems = new DefaultListModel();

	public PanelItems(EditListener listener) {
		super(listener);
		setLayout(new BorderLayout());
		init();
	}
	public abstract void doubleClicked(VDBTreeNode node);

	void init() {
		add(new JScrollPane(listView), BorderLayout.CENTER);
		listView.setModel( listItems );
		listView.setCellRenderer(new ItemRender());
		listView.setFont(new Font("Dialog",0,12));
		listView.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount()==2){
					Object selected = listView.getSelectedValue();
					doubleClicked( (VDBTreeNode)selected );
				}
				
			}
		});
	}

	public void setNode(VDBTreeNode node) {
		this.node = node;
		beforeInit();
		int n = node.getChildCount();
		listItems.removeAllElements();
		for (int i = 0; i < n; i++) {
			VDBTreeNode sub = (VDBTreeNode) node.getChildAt(i);
			listItems.addElement(sub);
		}

		afterInit();
	}

	public VDBTreeNode getNode() {
		return null;
	}

}
class ItemRender implements ListCellRenderer{
	public Component getListCellRendererComponent(JList list,Object value, int index, boolean isSelected,boolean cellHasFocus) {
		String val = "";
		VDBTreeNode item = (VDBTreeNode)value;
		val = item.getTitle();

		JLabel label = new JLabel(val);
		label.setOpaque(true);
		Icon icon = item.getDispIcon();
		label.setIcon(icon);

		if (isSelected) {
			label.setForeground(Color.WHITE);
			label.setBackground(Color.LIGHT_GRAY);
		} else {
			label.setForeground(list.getForeground());
			label.setBackground(list.getBackground());
		}
		
		return label;
	}
	
}
