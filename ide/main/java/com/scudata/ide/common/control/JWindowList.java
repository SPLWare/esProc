package com.scudata.ide.common.control;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.ListCellRenderer;

import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.IPrjxSheet;
import com.scudata.ide.common.swing.JListEx;

/**
 * 窗口列表。当窗口按钮显示不下时，点击更多窗口会显示此窗口
 *
 */
public abstract class JWindowList extends JWindow {
	private static final long serialVersionUID = 1L;
	/**
	 * 过滤文本框
	 */
	private JTextField textFilter = new JTextField();
	/**
	 * 窗口列表控件
	 */
	private JListEx listWindow = new JListEx();
	/**
	 * 文件路径列表
	 */
	private Vector<String> paths = new Vector<String>();
	/**
	 * 文件名称列表
	 */
	private Vector<String> names = new Vector<String>();
	/**
	 * 过滤后的文件路径列表
	 */
	private Vector<String> filterPaths = new Vector<String>();
	/**
	 * 过滤后的文件名称列表
	 */
	private Vector<String> filterNames = new Vector<String>();

	/**
	 * 已经显示在窗口工具栏上的路径名称
	 */
	private HashSet<String> existPaths = new HashSet<String>();

	private Map<String, String> typeMap = new HashMap<String, String>();
	private final Color BACK_COLOR = new Color(255, 255, 214);
	/**
	 * 缺省的背景色
	 */
	private final Color DEFAULT_BACK_COLOR = new JList<Object>()
			.getBackground();
	/**
	 * 缺省的选中状态的背景色
	 */
	private final Color SELECTED_BACK_COLOR = new JList<Object>()
			.getSelectionBackground();

	/**
	 * 构造函数
	 * 
	 * @param buttonSize
	 *            已经显示在窗口工具栏上的按钮数量
	 */
	public JWindowList(int buttonSize) {
		super(GV.appFrame);
		getContentPane().add(textFilter, BorderLayout.NORTH);
		JScrollPane jSPWin = new JScrollPane(listWindow);
		getContentPane().add(jSPWin, BorderLayout.CENTER);
		addWindowFocusListener(new WindowAdapter() {
			public void windowLostFocus(WindowEvent e) {
				if (isClickButton())
					return;
				dispose();
			}
		});
		setFocusable(true);
		GV.appFrame.getAllSheets();
		JInternalFrame[] sheets = GV.appFrame.getAllSheets();
		if (sheets != null) {
			for (int i = 0; i < sheets.length; i++) {
				String filePath = ((IPrjxSheet) sheets[i]).getSheetTitle();
				String fileName = filePath;
				try {
					File f = new File(filePath);
					fileName = f.getName();
				} catch (Exception ex) {
				}
				names.add(fileName);
				paths.add(filePath);
				if (i < buttonSize) {
					existPaths.add(filePath);
				}
				filterNames.add(fileName);
				filterPaths.add(filePath);
				typeMap.put(filePath, getSheetIcon());
			}
		}
		listWindow.x_setData(filterPaths, filterNames);

		ListCellRenderer cellRenderer = new ListCellRenderer() {
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				final JPanel panel = new JPanel(new BorderLayout());
				String filePath = (String) filterPaths.get(index);
				String iconName = typeMap.get(filePath);
				JLabel label = new JLabel(GM.getImageIcon(GC.IMAGES_PATH
						+ iconName));
				panel.add(label, BorderLayout.WEST);
				final JTextField text = new JTextField(
						(String) filterNames.get(index));
				text.setBackground(BACK_COLOR);
				if (!existPaths.contains(filePath)) {
					Font font = text.getFont();
					text.setFont(new Font(font.getFontName(), Font.BOLD, font
							.getSize() + 2));
				}
				text.setBorder(null);
				panel.add(text, BorderLayout.CENTER);
				panel.setToolTipText(filePath);
				text.setToolTipText(filePath);
				label.setToolTipText(filePath);
				panel.setMinimumSize(new Dimension(0, 20));
				panel.setPreferredSize(new Dimension(0, 20));

				MouseAdapter listener = new MouseAdapter() {
					public void mouseEntered(MouseEvent e) {
						panel.setBackground(SELECTED_BACK_COLOR);
						text.setBackground(SELECTED_BACK_COLOR);
					}

					public void mouseExited(MouseEvent e) {
						panel.setBackground(DEFAULT_BACK_COLOR);
						text.setBackground(DEFAULT_BACK_COLOR);
					}
				};
				panel.addMouseListener(listener);
				return panel;
			}
		};
		listWindow.setCellRenderer(cellRenderer);
		listWindow.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() != MouseEvent.BUTTON1)
					return;
				int index = listWindow.getSelectedIndex();
				try {
					if (showSheet((String) filterPaths.get(index)))
						dispose();
				} catch (Exception e1) {
					GM.showException(e1);
				}
			}
		});
		textFilter.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				filter();
			}
		});
		textFilter.requestFocusInWindow();
		setBackground(BACK_COLOR);
		textFilter.setBackground(BACK_COLOR);
		listWindow.setBackground(BACK_COLOR);
		jSPWin.setBackground(BACK_COLOR);
	}

	/**
	 * 取页面的图标
	 * 
	 * @return
	 */
	public abstract String getSheetIcon();

	/**
	 * 显示页面
	 * 
	 * @param filePath
	 *            文件路径
	 * @return
	 */
	public abstract boolean showSheet(String filePath);

	/**
	 * 是否正在点击显示更多窗口按钮
	 * 
	 * @return
	 */
	public abstract boolean isClickButton();

	/**
	 * 设置位置
	 * 
	 * @param x
	 *            X坐标
	 * @param y
	 *            Y坐标
	 */
	public void setPos(int x, int y) {
		int height = (paths.size() + 1) * 20 + 10;
		height = Math.min((int) (GV.appFrame.getHeight() * 0.5), height);
		setBounds(x, y, 200, height);
	}

	/**
	 * 过滤
	 */
	private void filter() {
		String filter = textFilter.getText();
		if (filter != null)
			filter = filter.toLowerCase();
		filterPaths = new Vector<String>();
		filterNames = new Vector<String>();
		for (int i = 0; i < names.size(); i++) {
			String fileName = (String) names.get(i);
			if (filter != null) {
				if (fileName.toLowerCase().startsWith(filter)) {
					filterPaths.add(paths.get(i));
					filterNames.add(names.get(i));
				}
			} else {
				filterPaths.add(paths.get(i));
				filterNames.add(names.get(i));
			}
		}
		listWindow.x_setData(filterPaths, filterNames);
	}
}
