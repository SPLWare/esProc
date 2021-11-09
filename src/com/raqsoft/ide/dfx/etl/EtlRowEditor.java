package com.raqsoft.ide.dfx.etl;

import java.awt.*;

import javax.swing.table.*;

import com.raqsoft.ide.common.swing.*;
import com.raqsoft.ide.dfx.chart.box.EachRowEditor;

/**
 * 属性框单元编辑器
 */
public class EtlRowEditor extends EachRowEditor {
	private TableCellEditor fieldDefineEditor;
	private TableCellEditor seperatorEditor, seperatorEditor2;
	private TableCellEditor stringListEditor;
	private TableCellEditor isolationLevelEditor;
	private TableCellEditor aorcsEditor,cellAEditor,cellFileEditor,cellCursorEditor;
	private TableCellEditor cellCtxEditor,dbEditor,cellXlsEditor;

	/**
	 * 构造函数
	 * @param table 本表
	 * @param editTypeCol 编辑类型列
	 * @param owner 父窗口
	 */
	public EtlRowEditor(JTableEx table, int editTypeCol, Dialog owner) {
		super(table, editTypeCol, owner);
	}

	/**
	 * 根据编辑类型选出对应编辑器
	 * @param editType 编辑类型
	 */
	public TableCellEditor selectEditor(int editType) {
		TableCellEditor editor1 = super.selectEditor(editType);
		if (editor1 != defaultEditor) {
			return editor1;
		}
		switch (editType) {
		case EtlConsts.INPUT_FIELDDEFINE_NORMAL:
		case EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD:
		case EtlConsts.INPUT_FIELDDEFINE_FIELD_EXP:
		case EtlConsts.INPUT_FIELDDEFINE_RENAME_FIELD:
		case EtlConsts.INPUT_FIELDDEFINE_FIELD_DIM:
			if (fieldDefineEditor == null) {
				fieldDefineEditor = new FieldDefineEditor(owner,editType);
			}
			editor1 = fieldDefineEditor;
			break;
		case EtlConsts.INPUT_SEPERATOR:
			if (seperatorEditor == null) {
				JComboBoxEx box = EtlConsts.getSeperatorComboBox(false);
				seperatorEditor = new JComboBoxExEditor(box);
			}
			editor1 = seperatorEditor;
			break;
		case EtlConsts.INPUT_SEPERATOR2:
			if (seperatorEditor2 == null) {
				JComboBoxEx box = EtlConsts.getSeperatorComboBox(true);
				seperatorEditor2 = new JComboBoxExEditor(box);
			}
			editor1 = seperatorEditor2;
			break;
		case EtlConsts.INPUT_STRINGLIST:
			if (stringListEditor == null) {
				stringListEditor = new StringListEditor(owner);
			}
			editor1 = stringListEditor;
			break;
		case EtlConsts.INPUT_ISOLATIONLEVEL:
			if (isolationLevelEditor == null) {
				JComboBoxEx box = EtlConsts.getIsolationLevelBox();
				isolationLevelEditor = new JComboBoxExEditor(box);
			}
			editor1 = isolationLevelEditor;
			break;
		case EtlConsts.INPUT_CELLAORCS:
			if (aorcsEditor == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_SEQUENCE,EtlConsts.TYPE_CURSOR});
				aorcsEditor = new JComboBoxExEditor(box);
			}
			editor1 = aorcsEditor;
			break;
		case EtlConsts.INPUT_CELLA:
			if (cellAEditor == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_SEQUENCE});
				cellAEditor = new JComboBoxExEditor(box);
			}
			editor1 = cellAEditor;
			break;
		case EtlConsts.INPUT_CELLXLS:
			if (cellXlsEditor == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_XLS});
				cellXlsEditor = new JComboBoxExEditor(box);
			}
			editor1 = cellXlsEditor;
			break;
		case EtlConsts.INPUT_CELLFILE:
			if (cellFileEditor == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_FILE});
				cellFileEditor = new JComboBoxExEditor(box);
			}
			editor1 = cellFileEditor;
			break;
		case EtlConsts.INPUT_CELLBCTX:
			if (cellCtxEditor == null) {
//				只能笼统用EtlConsts.TYPE_FILE，因为没法分清具体文件类型是啥，文件名也可以是变量，没法从扩展名获取
//				得用户自己清楚要选的为BTX
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_CTX,EtlConsts.TYPE_FILE});
				cellCtxEditor = new JComboBoxExEditor(box);
			}
			editor1 = cellCtxEditor;
			break;
		case EtlConsts.INPUT_CELLCURSOR:
			if (cellCursorEditor == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_CURSOR});
				cellCursorEditor = new JComboBoxExEditor(box);
			}
			editor1 = cellCursorEditor;
			break;
		case EtlConsts.INPUT_DB:
			if (dbEditor == null) {
				JComboBoxEx box = EtlConsts.getDBBox();
				dbEditor = new JComboBoxExEditor(box);
			}
			editor1 = dbEditor;
			break;
		default:
			editor1 = defaultEditor;
			break;
		}
		return editor1;
	}

}
