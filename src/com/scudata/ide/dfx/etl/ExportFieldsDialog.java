package com.scudata.ide.dfx.etl;

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

import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 导出字段时，定义字段名跟表达式的对话框
 * 
 * @author Joancy
 *
 */
public class ExportFieldsDialog extends JDialog implements IFieldDefineDialog{
	private static final long serialVersionUID = 1L;
	private static MessageManager mm = FuncMessage.get();

	JPanel jPanel2 = new JPanel();
	VFlowLayout vFlowLayout1 = new VFlowLayout();
	JButton jBOK = new JButton();
	JButton jBCancel = new JButton();
	JScrollPane jScrollPane1 = new JScrollPane();

	private final byte COL_INDEX = 0;
	private final byte COL_EXP = 1;
	private final byte COL_FIELD = 2;

	JTableEx exportFields;
	JPanel jPanel1 = new JPanel();
	JButton jBAdd = new JButton();
	JButton jBDel = new JButton();

	private int m_option = JOptionPane.CANCEL_OPTION;
	
	/**
	 * 构造函数
	 * @param owner 父窗口
	 * @param defineType 定义常量类型
	 */
	public ExportFieldsDialog(Dialog owner,int defineType) {
		super(owner);
		try {
			String colNames;
			if( defineType == EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD){
				colNames = "ExportFieldsDialog.exportfields";
			}else if(defineType == EtlConsts.INPUT_FIELDDEFINE_FIELD_EXP){
				colNames = "ExportFieldsDialog.fieldExps";
			}else{
				colNames = "ExportFieldsDialog.renameFields";
			}
			exportFields = new JTableEx(mm.getMessage(colNames));	
			
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
		setTitle(mm.getMessage("ExportFieldsDialog.title"));
		MessageManager icm = IdeCommonMessage.get();
		
		jBOK.setText(icm.getMessage("button.ok"));
		jBCancel.setText(icm.getMessage("button.cancel"));
		jBAdd.setText(icm.getMessage("button.add"));
		jBDel.setText(icm.getMessage("button.delete"));
	}

	/**
	 * 获取窗口返回的动作选项
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 设置字段定义列表
	 * @param fields 字段定义列表
	 */
	public void setFieldDefines(ArrayList<FieldDefine> fields) {
		if(fields==null){
			return;
		}
		
		for (int i = 0; i < fields.size(); i++) {
			int row = exportFields.addRow();
			FieldDefine fd = fields.get(i);
			exportFields.data.setValueAt(fd.getOne(), row, COL_EXP);
			exportFields.data.setValueAt(fd.getTwo(), row, COL_FIELD);
		}
	}

	/**
	 * 获取编辑好的字段定义列表
	 * @return 字段定义列表
	 */
	public ArrayList<FieldDefine> getFieldDefines() {
		exportFields.acceptText();
		int rows = exportFields.getRowCount(); 
		if(rows==0){
			return null;
		}
		ArrayList<FieldDefine> fields = new ArrayList<FieldDefine>();
		for (int i = 0; i < rows; i++) {
			String name = (String) exportFields.data.getValueAt(i, COL_EXP);
			if(!StringUtils.isValidString(name)){
				continue;
			}
			FieldDefine fd = new FieldDefine();
			fd.setOne(name);
			fd.setTwo((String) exportFields.data.getValueAt(i, COL_FIELD));
			
			fields.add(fd);
		}
		return fields;
	}


	private void rqInit() {
		exportFields.setIndexCol(COL_INDEX);
		exportFields.setRowHeight(20);
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
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);
		jPanel2.add(jPanel1, null);
		jPanel2.add(jBAdd, null);
		jPanel2.add(jBDel, null);
		
		this.getContentPane().add(jScrollPane1, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
	}

}
