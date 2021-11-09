package com.raqsoft.ide.dfx.etl;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.raqsoft.common.MessageManager;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.common.swing.JTableEx;
import com.raqsoft.ide.common.swing.VFlowLayout;

/**
 * 使用对话框编辑分隔符分隔的字符串
 * 
 * @author Joancy
 *
 */
public class StringListDialog extends JDialog{
	private static final long serialVersionUID = 1L;
	private static MessageManager mm = FuncMessage.get();

	JPanel jPanel2 = new JPanel();
	VFlowLayout vFlowLayout1 = new VFlowLayout();
	JButton jBOK = new JButton();
	JButton jBCancel = new JButton();
	JScrollPane jScrollPane1 = new JScrollPane();

	private final byte COL_INDEX = 0;
	private final byte COL_NAME = 1;

	JTableEx tableFields = new JTableEx(mm.getMessage("StringListDialog.tablefields"));//"序号,值");
	JPanel jPanel1 = new JPanel();
	JButton jBAdd = new JButton();
	JButton jBDel = new JButton();

	private int m_option = JOptionPane.CANCEL_OPTION;
	
	/**
	 * 构造函数
	 * @param owner 父窗口
	 */
	public StringListDialog(Dialog owner) {
		super(owner);
		try {
			init();
			rqInit();
			setSize(450, 300);
			resetText();
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	private void resetText() {
		setTitle(mm.getMessage("StringListDialog.title"));
		MessageManager icm = IdeCommonMessage.get();
	
		jBOK.setText(icm.getMessage("button.ok"));
		jBCancel.setText(icm.getMessage("button.cancel"));
		jBAdd.setText(icm.getMessage("button.add"));
		jBDel.setText(icm.getMessage("button.delete"));
	}

	/**
	 * 获取窗口返回选项 
	 * @return 选项
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 设置字串列表
	 * @param list 字符串列表
	 */
	public void setList(ArrayList<String> list) {
		if(list==null){
			return;
		}
		
		for (int i = 0; i < list.size(); i++) {
			int row = tableFields.addRow();
			tableFields.data.setValueAt(list.get(i), row, COL_NAME);
		}
	}

	/**
	 * 获取字符串列表
	 * @return 字符串列表
	 */
	public ArrayList<String> getList() {
		tableFields.acceptText();
		int rows = tableFields.getRowCount(); 
		if(rows==0){
			return null;
		}
		ArrayList<String> fields = new ArrayList<String>();
		for (int i = 0; i < rows; i++) {
			String name = (String) tableFields.data.getValueAt(i, COL_NAME);
			fields.add(name);
		}
		return fields;
	}


	private void rqInit() {
		tableFields.setIndexCol(COL_INDEX);
		tableFields.setRowHeight(20);
	}

	private void init() throws Exception {
		setModal(true);
		jPanel2.setLayout(vFlowLayout1);
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				tableFields.acceptText();
				m_option = JOptionPane.OK_OPTION;
				dispose();
			}
		});
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		jBAdd.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				tableFields.addRow();
			}
		});
		jBDel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				tableFields.deleteSelectedRows();
			}
		});
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		jScrollPane1.getViewport().add(tableFields);
		jBAdd.setMnemonic('A');
		jBAdd.setText("增加(A)");
		jBDel.setMnemonic('D');
		jBDel.setText("删除(D)");
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);
		jPanel2.add(jPanel1, null);
		jPanel2.add(jBAdd, null);
		jPanel2.add(jBDel, null);
		
		this.getContentPane().add(jScrollPane1, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
	}

}
