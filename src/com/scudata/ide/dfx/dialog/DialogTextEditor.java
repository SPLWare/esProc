package com.scudata.ide.dfx.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.dfx.resources.IdeDfxMessage;

/**
 * 文本编辑对话框
 *
 */
public class DialogTextEditor extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	/**
	 * 确认按钮
	 */
	private JButton okButton = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton cancelButton = new JButton();

	/**
	 * 文本编辑控件
	 */
	protected RSyntaxTextArea textEditor = new RSyntaxTextArea() {
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
	 * 滚动面板
	 */
	protected RTextScrollPane spEditor = new RTextScrollPane(textEditor);
	/**
	 * 集算器资源管理器
	 */
	private MessageManager mm = IdeDfxMessage.get();
	/**
	 * 自动换行复选框
	 */
	private JCheckBox jCBLineWrap = new JCheckBox(
			mm.getMessage("dialogtexteditor.linewrap"));

	/**
	 * 退出选项
	 */
	private int option = JOptionPane.CLOSED_OPTION;

	/**
	 * 构造函数
	 */
	public DialogTextEditor() {
		this(true);
	}

	/**
	 * 构造函数
	 * 
	 * @param isEditable
	 *            是否可以编辑
	 */
	public DialogTextEditor(boolean isEditable) {
		super(GV.appFrame, "", true);
		init();

		GM.setWindowToolSize(this);
		GM.setDialogDefaultButton(this, okButton, cancelButton);
		this.setResizable(true);
		jCBLineWrap.setSelected(ConfigOptions.bTextEditorLineWrap
				.booleanValue());
		textEditor.setLineWrap(jCBLineWrap.isSelected());
		if (!isEditable) {
			textEditor.setEditable(false);
			okButton.setVisible(false);
			setTitle(mm.getMessage("dialogtexteditor.title1"));
		} else {
			setTitle(mm.getMessage("dialogtexteditor.title"));
		}
	}

	/**
	 * 取退出选项
	 * 
	 * @return
	 */
	public int getOption() {
		return option;
	}

	/**
	 * 设置文本
	 * 
	 * @param text
	 */
	public void setText(String text) {
		textEditor.setText(text);

	}

	/**
	 * 取文本
	 * 
	 * @return
	 */
	public String getText() {
		return textEditor.getText();
	}

	/**
	 * 初始化
	 */
	private void init() {
		this.getContentPane().setLayout(new BorderLayout());
		okButton.addActionListener(this);
		okButton.setMnemonic('O');
		cancelButton.addActionListener(this);
		cancelButton.setMnemonic('C');
		okButton.setText(mm.getMessage("button.ok"));
		cancelButton.setText(mm.getMessage("button.cancel"));
		JPanel panelSouth = new JPanel(new GridBagLayout());
		panelSouth.add(jCBLineWrap, GM.getGBC(0, 0));
		panelSouth.add(new JLabel(""), GM.getGBC(0, 1, true));
		panelSouth.add(okButton, GM.getGBC(0, 2, false, false, 0));
		panelSouth.add(cancelButton, GM.getGBC(0, 3));
		this.getContentPane().add(panelSouth, BorderLayout.SOUTH);
		JPanel panelCenter = new JPanel(new BorderLayout());
		panelCenter.add(spEditor, BorderLayout.CENTER);
		this.getContentPane().add(panelCenter, BorderLayout.CENTER);
		textEditor.setEditable(true);
		textEditor.setCodeFoldingEnabled(true);
		textEditor.setFont(GC.font);
		textEditor.setToolTipText(mm.getMessage("toolbarproperty.cellexp")); // 单元格表达式
		textEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeWindow();
			}
		});
		jCBLineWrap.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				textEditor.setLineWrap(jCBLineWrap.isSelected());
			}

		});
	}

	/**
	 * 关闭窗口
	 */
	private void closeWindow() {
		ConfigOptions.bTextEditorLineWrap = jCBLineWrap.isSelected();
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 控件事件
	 */
	public void actionPerformed(ActionEvent e) {
		Object c = e.getSource();
		if (c == okButton) {
			option = JOptionPane.OK_OPTION;
			closeWindow();
		} else if (c == cancelButton) {
			closeWindow();
		}
	}
}
