package com.scudata.ide.spl.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.dialog.RQDialog;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 复制可呈现代码对话框
 *
 */
public class DialogCopyPresent extends RQDialog {

	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 */
	public DialogCopyPresent() {
		super("Copy code for presentation", 600, 500);
		try {
			init();
			GM.centerWindow(this);
		} catch (Exception e) {
			GM.showException(this, e);
		}
	}

	/**
	 * 保存选项
	 * 
	 * @param showException
	 *            是否抛异常
	 * @return
	 */
	private boolean saveOption(boolean showException) {
		byte type = ((Number) jCBType.x_getSelectedItem()).byteValue();
		if (type == ConfigOptions.COPY_TEXT) {
			String sep = getSep();
			if (sep == null || "".equals(sep)) {
				if (showException)
					// 请定义列分隔符。
					GM.messageDialog(this,
							mm.getMessage("dialogcopypresent.emptycolsep"));
				return false;
			}
			ConfigOptions.sCopyPresentSep = sep;
		}
		ConfigOptions.iCopyPresentType = type;
		ConfigOptions.bCopyPresentHeader = jCBHeader.isSelected();
		return true;
	}

	/**
	 * 确认按钮事件
	 */
	protected boolean okAction(ActionEvent e) {
		if (!saveOption(true)) {
			return false;
		}

		return true;
	}

	/**
	 * 关闭对话框
	 */
	protected void closeDialog(int option) {
		super.closeDialog(option);
		GM.setWindowDimension(this);
		ConfigOptions.bTextEditorLineWrap = jCBLineWrap.isSelected();
	}

	/**
	 * 取分隔符
	 * 
	 * @return
	 */
	private String getSep() {
		Object disp = jCBSep.getEditor().getItem();
		if (disp != null) {
			int index = disps.indexOf(disp);
			if (index > -1) {
				return (String) codes.get(index);
			} else {
				return disp.toString();
			}
		}
		return "";
	}

	/**
	 * 初始化
	 */
	private void init() {
		panelCenter.setLayout(new GridBagLayout());
		panelCenter.add(jLType, GM.getGBC(0, 0));
		panelCenter.add(jCBType, GM.getGBC(0, 1));

		GridBagConstraints gbc = GM.getGBC(1, 0);
		gbc.gridwidth = 2;
		panelCenter.add(jCBHeader, gbc);

		panelCenter.add(jLSep, GM.getGBC(2, 0));
		panelCenter.add(jCBSep, GM.getGBC(2, 1));

		panelCenter.add(jLPreview, GM.getGBC(3, 0));
		gbc = GM.getGBC(4, 0, true, true);
		gbc.gridwidth = 2;
		panelCenter.add(jSPPreview, gbc);

		gbc = GM.getGBC(5, 0);
		gbc.gridwidth = 2;
		panelCenter.add(jCBLineWrap, gbc);

		Vector<Object> codeData = new Vector<Object>();
		codeData.add(ConfigOptions.COPY_HTML);
		codeData.add(ConfigOptions.COPY_TEXT);
		Vector<String> dispData = new Vector<String>();
		dispData.add(LABEL_HTML);
		dispData.add(LABEL_TEXT);
		jCBType.x_setData(codeData, dispData);

		codes.add(TAB);
		codes.add(SPACE);
		codes.add(CSV);
		disps.add(LABEL_TAB);
		disps.add(LABEL_SPACE);
		disps.add(LABEL_CSV);
		jCBSep.x_setData(codes, disps);
		jCBSep.setEditable(true);

		jTAPreview.setEditable(false);
		jTAPreview.setCodeFoldingEnabled(true);
		jTAPreview.setFont(GC.font);
		// 单元格表达式
		jTAPreview.setToolTipText(mm.getMessage("toolbarproperty.cellexp"));
		jTAPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		jCBLineWrap.setSelected(ConfigOptions.bTextEditorLineWrap
				.booleanValue());
		jTAPreview.setLineWrap(jCBLineWrap.isSelected());

		jCBType.x_setSelectedCodeItem(ConfigOptions.iCopyPresentType);
		jCBHeader.setSelected(ConfigOptions.bCopyPresentHeader);
		jCBSep.setSelectedItem(ConfigOptions.sCopyPresentSep);
		// 复制可呈现代码
		setTitle(mm.getMessage("dialogcopypresent.title"));
		this.setResizable(true);
		jCBType.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				typeChanged();
				preview();
			}

		});
		jCBHeader.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				preview();
			}

		});
		Object editor = jCBSep.getEditor().getEditorComponent();
		if (editor instanceof JTextComponent) {
			final JTextComponent textSplitChar = (JTextComponent) editor;
			textSplitChar.setFocusTraversalKeys(
					KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
					Collections.EMPTY_SET);
			textSplitChar.setFocusTraversalKeys(
					KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
					Collections.EMPTY_SET);
			textSplitChar.addKeyListener(new KeyListener() {

				public void keyTyped(KeyEvent e) {
				}

				public void keyReleased(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_TAB) {
						textSplitChar.setText(LABEL_TAB);
					}
				}

				public void keyPressed(KeyEvent e) {
				}
			});
		}
		jCBSep.getEditor().getEditorComponent()
				.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent e) {
						if (e.getKeyCode() == KeyEvent.VK_ENTER) {
							e.consume();
							preview();
						}
					}
				});
		jCBSep.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				preview();
			}

		});
		jCBLineWrap.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				jTAPreview.setLineWrap(jCBLineWrap.isSelected());
			}

		});
		typeChanged();
		preview();
	}

	/**
	 * 显示
	 */
	private void preview() {
		if (!saveOption(false)) {
			return;
		}
		String str = GVSpl.splEditor.getCopyPresentString();
		jTAPreview.setText(str);
	}

	/**
	 * 类型发生变化
	 */
	private void typeChanged() {
		boolean sepEnabled = ((Number) jCBType.x_getSelectedItem()).byteValue() == ConfigOptions.COPY_TEXT;
		jLSep.setEnabled(sepEnabled);
		jCBSep.setEnabled(sepEnabled);
	}

	/** 代码值 */
	private Vector<Object> codes = new Vector<Object>();
	/** 显示值 */
	private Vector<String> disps = new Vector<String>();
	/** 集算器资源管理器 */
	private MessageManager mm = IdeSplMessage.get();
	/**
	 * 复制类型
	 */
	private JLabel jLType = new JLabel(mm.getMessage("dialogcopypresent.type"));
	/** HTML */
	private final String LABEL_HTML = "HTML";
	/** 文本 */
	private final String LABEL_TEXT = mm.getMessage("dialogcopypresent.text");
	/** 类型控件 */
	private JComboBoxEx jCBType = new JComboBoxEx();
	/** 是否复制行列号控件 */
	private JCheckBox jCBHeader = new JCheckBox(
			mm.getMessage("dialogcopypresent.copyheader"));
	/** 列分隔符 */
	private JLabel jLSep = new JLabel(mm.getMessage("dialogcopypresent.colsep"));

	/** 代码值 */
	private final String TAB = "\t";
	private final String SPACE = "    ";
	private final String CSV = ",";
	/** 显示值 */
	private final String LABEL_TAB = "TAB";
	private final String LABEL_SPACE = mm.getMessage("dialogcopypresent.space");
	private final String LABEL_CSV = ",";

	/**
	 * 分隔符控件
	 */
	private JComboBoxEx jCBSep = new JComboBoxEx();

	/**
	 * 预览
	 */
	private JLabel jLPreview = new JLabel(
			mm.getMessage("dialogcopypresent.preview"));
	/**
	 * 预览的文本控件
	 */
	protected RSyntaxTextArea jTAPreview = new RSyntaxTextArea() {
		private static final long serialVersionUID = 1L;

		public Rectangle modelToView(int pos) throws BadLocationException {
			try {
				return super.modelToView(pos);
			} catch (Exception ex) {
			}
			return null;
		}
	};
	/**
	 * 预览滚动面板
	 */
	protected RTextScrollPane jSPPreview = new RTextScrollPane(jTAPreview);
	/**
	 * 是否自动换行
	 */
	private JCheckBox jCBLineWrap = new JCheckBox(
			mm.getMessage("dialogtexteditor.linewrap"));
}
