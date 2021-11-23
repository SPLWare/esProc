package com.scudata.ide.dfx.etl;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.dialog.DialogCellFormat;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 字段定义的通用对话框编辑器
 * 
 * @author Joancy
 *
 */
public class FieldDefineDialog extends JDialog implements IFieldDefineDialog{
	private static final long serialVersionUID = 1L;
	private static MessageManager mm = FuncMessage.get();

	JPanel jPanel2 = new JPanel();
	VFlowLayout vFlowLayout1 = new VFlowLayout();
	JButton jBOK = new JButton();
	JButton jBCancel = new JButton();
	JScrollPane jScrollPane1 = new JScrollPane();

	private final byte COL_INDEX = 0;
	private final byte COL_NAME = 1;
	private final byte COL_TYPE = 2;
	private final byte COL_FORMAT = 3;

	JTableEx tableFields = new JTableEx(mm.getMessage("FieldDefineDialog.tablefields")){//"序号,字段名,数据类型,格式"
		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			if (row > -1 && col==COL_FORMAT) {
				 DialogCellFormat dcf = new DialogCellFormat();
				 String fmt = (String) data.getValueAt(row, col);
				 dcf.setFormat(fmt);
				 dcf.setVisible(true);
				 if (dcf.getOption() == JOptionPane.OK_OPTION) {
					 acceptText();
					 setValueAt(dcf.getFormat(), row, col);
					 acceptText();
				 }
			}
		}
	};
	JPanel jPanel1 = new JPanel();
	JButton jBAdd = new JButton();
	JButton jBDel = new JButton();

	private int m_option = JOptionPane.CANCEL_OPTION;
	
	/**
	 * 构造函数
	 * @param owner 父窗口
	 */
	public FieldDefineDialog(Dialog owner) {
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
		setTitle(mm.getMessage("FieldDefineDialog.title"));
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
	 * 设置字段定义值列表
	 * @param fields 字段定义列表
	 */
	public void setFieldDefines(ArrayList<FieldDefine> fields) {
		if(fields==null){
			return;
		}
		
		for (int i = 0; i < fields.size(); i++) {
			int row = tableFields.addRow();
			FieldDefine fd = fields.get(i);
			tableFields.data.setValueAt(fd.getOne(), row, COL_NAME);
			tableFields.data.setValueAt(fd.getTwo(), row, COL_TYPE);
			tableFields.data.setValueAt(fd.getThree(), row, COL_FORMAT);
		}
	}

	/**
	 * 获取字段定义列表
	 */
	public ArrayList<FieldDefine> getFieldDefines() {
		tableFields.acceptText();
		int rows = tableFields.getRowCount(); 
		if(rows==0){
			return null;
		}
		ArrayList<FieldDefine> fields = new ArrayList<FieldDefine>();
		for (int i = 0; i < rows; i++) {
			String name = (String) tableFields.data.getValueAt(i, COL_NAME);
			if(!StringUtils.isValidString(name)){
				continue;
			}
			FieldDefine fd = new FieldDefine();
			fd.setOne(name);
			fd.setTwo((String) tableFields.data.getValueAt(i, COL_TYPE));
			fd.setThree((String) tableFields.data.getValueAt(i, COL_FORMAT));
			
			fields.add(fd);
		}
		return fields;
	}


	private void rqInit() {
		tableFields.setIndexCol(COL_INDEX);
		tableFields.setRowHeight(20);
		
		Vector<String> disp = new Vector<String>();
		disp.add("");
		disp.add("bool");
		disp.add("int");
		disp.add("long");
		disp.add("float");
		disp.add("decimal");
		disp.add("number");
		disp.add("string");
		disp.add("date");
		disp.add("time");
		disp.add("datetime");
		
		JComboBox combo = tableFields.setColumnDropDown(COL_TYPE, disp, disp);
		combo.setMaximumRowCount(10);

		tableFields.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableFields.getTableHeader().setReorderingAllowed(false);
		tableFields.setColumnWidth(COL_NAME, 120);
		tableFields.setColumnWidth(COL_TYPE, 86);
		tableFields.setColumnWidth(COL_FORMAT, 120);
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
