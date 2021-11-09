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
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.common.swing.JTableEx;
import com.raqsoft.ide.common.swing.VFlowLayout;

/**
 * 字段定义的维格式编辑对话框
 * 
 * @author Joancy
 *
 */
public class FieldDimDialog extends JDialog implements IFieldDefineDialog{
	private static final long serialVersionUID = 1L;
	private static MessageManager mm = FuncMessage.get();

	JPanel jPanel2 = new JPanel();
	VFlowLayout vFlowLayout1 = new VFlowLayout();
	JButton jBOK = new JButton();
	JButton jBCancel = new JButton();
	JScrollPane jScrollPane1 = new JScrollPane();

	private final byte COL_INDEX = 0;
	private final byte COL_FIELD = 1;
	private final byte COL_DIM = 2;

	JTableEx exportFields = new JTableEx(mm.getMessage("FieldDimDialog.exportfields"));//"序号,字段名,维");
	JPanel jPanel1 = new JPanel();
	JButton jBAdd = new JButton();
	JButton jBDel = new JButton();
	JButton jBShiftUp = new JButton();
	JButton jBShiftDown = new JButton();

	private int m_option = JOptionPane.CANCEL_OPTION;
	
	/**
	 * 构造函数
	 * @param owner 父窗口
	 */
	public FieldDimDialog(Dialog owner) {
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
		setTitle(mm.getMessage("FieldDimDialog.title"));
		MessageManager icm = IdeCommonMessage.get();
		
		jBOK.setText(icm.getMessage("button.ok"));
		jBCancel.setText(icm.getMessage("button.cancel"));
		jBAdd.setText(icm.getMessage("button.add"));
		jBDel.setText(icm.getMessage("button.delete"));
		jBShiftUp.setText(icm.getMessage("button.shiftup"));
		jBShiftDown.setText(icm.getMessage("button.shiftdown"));
	}

	/**
	 * 获取窗口返回的选项
	 * @return 选项
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 设置字段定义列表值
	 */
	public void setFieldDefines(ArrayList<FieldDefine> fields) {
		if(fields==null){
			return;
		}
		
		for (int i = 0; i < fields.size(); i++) {	
			int row = exportFields.addRow();
			FieldDefine fd = fields.get(i);
			exportFields.data.setValueAt(fd.getOne(), row, COL_FIELD);
			exportFields.data.setValueAt(Boolean.valueOf( fd.getTwo() ), row, COL_DIM);
		}
	}

	/**
	 * 获取维字段格式的字段定义列表
	 */
	public ArrayList<FieldDefine> getFieldDefines() {
		exportFields.acceptText();
		int rows = exportFields.getRowCount(); 
		if(rows==0){
			return null;
		}
		ArrayList<FieldDefine> fields = new ArrayList<FieldDefine>();
		for (int i = 0; i < rows; i++) {
			String name = (String) exportFields.data.getValueAt(i, COL_FIELD);
			if(!StringUtils.isValidString(name)){
				continue;
			}
			FieldDefine fd = new FieldDefine();
			fd.setOne(name);
			Object val = exportFields.data.getValueAt(i, COL_DIM);
			if(val!=null){
				fd.setTwo(((Boolean)val ).toString());
			}
			
			fields.add(fd);
		}
		return fields;
	}


	private void rqInit() {
		exportFields.setIndexCol(COL_INDEX);
		exportFields.setRowHeight(20);
		exportFields.setColumnCheckBox(COL_DIM);
		
	}

	private void init() throws Exception {
		setModal(true);
		jPanel2.setLayout(vFlowLayout1);
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				exportFields.acceptText();
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
				exportFields.addRow();
			}
		});
		jBDel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				exportFields.deleteSelectedRows();
			}
		});
		jBShiftUp.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				exportFields.shiftUp();
			}
		});
		jBShiftDown.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				exportFields.shiftDown();
			}
		});
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		jScrollPane1.getViewport().add(exportFields);
		jBAdd.setMnemonic('A');
		jBAdd.setText("增加(A)");
		jBDel.setMnemonic('D');
		jBDel.setText("删除(D)");
		jBShiftUp.setMnemonic('U');
		jBShiftUp.setText("上移(U)");
		jBShiftDown.setMnemonic('N');
		jBShiftDown.setText("下移(N)");
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);
		jPanel2.add(jPanel1, null);
		jPanel2.add(jBAdd, null);
		jPanel2.add(jBDel, null);
		jPanel2.add(jBShiftUp, null);
		jPanel2.add(jBShiftDown, null);
		
		this.getContentPane().add(jScrollPane1, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
	}

}
