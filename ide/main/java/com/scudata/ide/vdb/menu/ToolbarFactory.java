package com.scudata.ide.vdb.menu;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import com.scudata.ide.vdb.VDB;
import com.scudata.ide.vdb.commonvdb.GM;
import com.scudata.ide.vdb.resources.IdeMessage;

public class ToolbarFactory extends JToolBar {
	private static final long serialVersionUID = 1L;
	public static Map<Short, JButton> buttonHolder = new HashMap<Short, JButton>();

	public ToolbarFactory() {
		this(IdeMessage.get().getMessage("public.toolbar"));
	}

	public ToolbarFactory(String name) {
		super(name);
		this.setFloatable(false);
		setToolTipText(IdeMessage.get().getMessage("public.toolbar"));
	}

	private ActionListener actionNormal = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String menuId = "";
			try {
				menuId = e.getActionCommand();
				short cmdId = Short.parseShort(menuId);
				VDB.getInstance().executeCmd(cmdId);
			} catch (Exception ex) {
				GM.showException(ex);
			}
		}
	};

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (!isOpaque()) {
			return;
		}
		Color control = UIManager.getColor("control");
		int width = getWidth();
		int height = getHeight();

		Graphics2D g2 = (Graphics2D) g;
		Paint storedPaint = g2.getPaint();
		g2.setPaint(new GradientPaint(width / 2, (int) (height * 1.5), control
				.darker(), width / 2, 0, control));
		g2.fillRect(0, 0, width, height);
		g2.setPaint(storedPaint);
	}

	protected JButton getButton(short cmdId, String menuId) {
		ImageIcon img = GM.getMenuImageIcon(menuId);
		JButton b = new JButton(img);
		b.setOpaque(false);
		b.setMargin(new Insets(0, 0, 0, 0));
		String toolTip = IdeMessage.get().getMessage(GCMenu.PRE_MENU + menuId);
		int index = toolTip.indexOf("(");
		if (index > -1) {
			toolTip = toolTip.substring(0, index);
		}
		b.setToolTipText(toolTip);
		b.setActionCommand(Short.toString(cmdId));
		b.addActionListener(actionNormal);
		buttonHolder.put(cmdId, b);
		return b;
	}

	public void setButtonEnabled(short cmdId, boolean enabled) {
		setButtonsEnabled(new short[] { cmdId }, enabled);
	}

	public void setButtonsEnabled(short cmdId[], boolean enabled) {
		for (int i = 0; i < cmdId.length; i++) {
			((AbstractButton) buttonHolder.get(cmdId[i])).setEnabled(enabled);
		}
	}

	public void setButtonVisible(short cmdId, boolean visible) {
		setButtonsVisible(new short[] { cmdId }, visible);
	}

	public void setButtonsVisible(short cmdId[], boolean visible) {
		for (int i = 0; i < cmdId.length; i++) {
			((AbstractButton) buttonHolder.get(cmdId[i])).setVisible(visible);
		}
	}
}
