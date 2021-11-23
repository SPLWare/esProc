package com.scudata.ide.dfx.etl;

import java.awt.Dialog;

import javax.swing.table.*;

import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.common.swing.JComboBoxExRenderer;
import com.scudata.ide.dfx.chart.box.EachRowRenderer;

/**
 * 属性框单元渲染器
 */
public class EtlRowRenderer extends EachRowRenderer {
	private TableCellRenderer fieldDefineRender;
	private TableCellRenderer seperatorRender,seperatorRender2;
	private TableCellRenderer stringListRender;
	private TableCellRenderer isolationLevelRender;
	private TableCellRenderer aorcsRender,cellARender,cellFileRender,cellCursorRender;
	private TableCellRenderer cellCtxRender,dbRender,cellXlsRender;

	Dialog owner;
	/**
	 * 构造函数
	 * @param editTypeCol 编辑类型列
	 * @param owner 父窗口
	 */
	public EtlRowRenderer(int editTypeCol,Dialog owner) {
		super(editTypeCol);
		this.owner = owner;
	}

	/**
	 * 根据编辑类型editType选择对应的渲染器
	 * @param editType 编辑类型
	 */
	public TableCellRenderer selectRenderer(int editType) {
		TableCellRenderer render1 = super.selectRenderer(editType);
		if (render1 != defaultRender) {
			return render1;
		}
		switch (editType) {
		case EtlConsts.INPUT_FIELDDEFINE_NORMAL:
		case EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD:
		case EtlConsts.INPUT_FIELDDEFINE_FIELD_EXP:
		case EtlConsts.INPUT_FIELDDEFINE_RENAME_FIELD:
		case EtlConsts.INPUT_FIELDDEFINE_FIELD_DIM:
			if (fieldDefineRender == null) {
				fieldDefineRender = new FieldDefineRender();
			}
			render1 = fieldDefineRender;
			break;
		case EtlConsts.INPUT_SEPERATOR:
			if (seperatorRender == null) {
				seperatorRender = new JComboBoxExRenderer(
						EtlConsts.getSeperatorComboBox(false));
			}
			render1 = seperatorRender;
			break;
		case EtlConsts.INPUT_SEPERATOR2:
			if (seperatorRender2 == null) {
				seperatorRender2 = new JComboBoxExRenderer(
						EtlConsts.getSeperatorComboBox(true));
			}
			render1 = seperatorRender2;
			break;
		case EtlConsts.INPUT_STRINGLIST:
			if (stringListRender == null) {
				stringListRender = new StringListRender();
			}
			render1 = stringListRender;
			break;
		case EtlConsts.INPUT_ISOLATIONLEVEL:
			if (isolationLevelRender == null) {
				JComboBoxEx box = EtlConsts.getIsolationLevelBox();
				isolationLevelRender = new JComboBoxExRenderer(box);
			}
			render1 = isolationLevelRender;
			break;
		case EtlConsts.INPUT_CELLAORCS:
			if (aorcsRender == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_SEQUENCE,EtlConsts.TYPE_CURSOR});
				aorcsRender = new JComboBoxExRenderer(box);
			}
			render1 = aorcsRender;
			break;
		case EtlConsts.INPUT_CELLA:
			if (cellARender == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_SEQUENCE});
				cellARender = new JComboBoxExRenderer(box);
			}
			render1 = cellARender;
			break;
		case EtlConsts.INPUT_CELLXLS:
			if (cellXlsRender == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_XLS});
				cellXlsRender = new JComboBoxExRenderer(box);
			}
			render1 = cellXlsRender;
			break;
		case EtlConsts.INPUT_CELLFILE:
			if (cellFileRender == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_FILE});
				cellFileRender = new JComboBoxExRenderer(box);
			}
			render1 = cellFileRender;
			break;
		case EtlConsts.INPUT_CELLBCTX:
			if (cellCtxRender == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_CTX,EtlConsts.TYPE_FILE});
				cellCtxRender = new JComboBoxExRenderer(box);
			}
			render1 = cellCtxRender;
			break;
		case EtlConsts.INPUT_CELLCURSOR:
			if (cellCursorRender == null) {
				JComboBoxEx box = ((DialogFuncEdit)owner).getCellNameDropdownBox(
						new byte[]{EtlConsts.TYPE_CURSOR});
				cellCursorRender = new JComboBoxExRenderer(box);
			}
			render1 = cellCursorRender;
			break;
		case EtlConsts.INPUT_DB:
			if (dbRender == null) {
				JComboBoxEx box = EtlConsts.getDBBox();
				dbRender = new JComboBoxExRenderer(box);
			}
			render1 = dbRender;
			break;
		default:
			render1 = defaultRender;
			break;
		}
		return render1;
	}

}
