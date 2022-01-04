package com.scudata.ide.spl.chart.box;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import com.scudata.chart.*;
import com.scudata.ide.common.swing.*;

/**
 *属性框单元编辑器
 */
public class EachRowEditor implements TableCellEditor {
	int currentRow, editTypeCol;
	private JTableEx table;
	public Dialog owner;
	private TableCellEditor editor;

	public TableCellEditor defaultEditor;
//公共编辑器
	private DefaultCellEditor checkEditor;

	private TableCellEditor lineStyleEditor;
	private TableCellEditor arrowEditor,simpleArrowEditor;
	private TableCellEditor textureEditor;
	private TableCellEditor colorEditor;
	private TableCellEditor fontEditor;
	private TableCellEditor pointEditor;
	private TableCellEditor fontStyleEditor;
	private TableCellEditor dateEditor;
	private TableCellEditor ticksEditor;
	private TableCellEditor unitEditor;
	private TableCellEditor coordEditor;
	private TableCellEditor axisEditor;
	private TableCellEditor fontsizeEditor;
	private TableCellEditor columnStyleEditor;
	private TableCellEditor chartColorEditor;
	private TableCellEditor halignEditor;
	private TableCellEditor valignEditor,imageModeEditor;
	private JTextAreaEditor angleEditor;
	private JTextAreaEditor intEditor;
	private JTextAreaEditor doubleEditor;
	private TableCellEditor legendIconEditor;
	private TableCellEditor dateUnitEditor;
	private TableCellEditor urlTargetEditor;
	private TableCellEditor transformEditor;
	private TableCellEditor stackTypeEditor,displayDataEditor,legendLocationEditor;
	private TableCellEditor inputColumnTypeEditor,inputLineTypeEditor;
	private TableCellEditor inputPieTypeEditor,input2AxisTypeEditor;
	private TableCellEditor inputBarTypeEditor,inputCharSetEditor,inputRecErrorEditor;

	ActionListener al;
	/**
	 * 构造函数
	 * @param table 本体表
	 * @param editTypeCol 编辑类型
	 * @param owner 父窗口
	 */
	public EachRowEditor( final JTableEx table, int editTypeCol, Dialog owner ) {
		this.table = table;
		this.editTypeCol = editTypeCol;
		this.owner = owner;
		al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				table.stateChanged(new ChangeEvent(e.getSource()));
			}
		};
		
		defaultEditor = table.getDefaultCellEditor(new JTextField(), table);//new JTextAreaEditor( table );
		JCheckBox checkBox = new JCheckBox();
		checkBox.addActionListener(al);
		checkBox.setHorizontalAlignment(JLabel.CENTER);
		checkEditor = new DefaultCellEditor( checkBox );
	}

	/**
	 * 实现父类抽象方法
	 */
	public Component getTableCellEditorComponent( JTable table,
												  Object value,
												  boolean isSelected,
												  int row, int column ) {

		return editor.getTableCellEditorComponent( table,
			value, isSelected, row,
			column );
	}

	/**
	 * 获取编辑值
	 */
	public Object getCellEditorValue() {
		return editor.getCellEditorValue();
	}

	/**
	 * 停止编辑
	 */
	public boolean stopCellEditing() {
		return editor.stopCellEditing();
	}

	/**
	 * 取消编辑
	 */
	public void cancelCellEditing() {
		editor.cancelCellEditing();
	}

	/**
	 * 返回是否运行编辑
	 * @param anEvent 事件对象
	 */
	public boolean isCellEditable( EventObject anEvent ) {
		if(anEvent instanceof MouseEvent){
			selectEditor( ( MouseEvent ) anEvent );
			return editor.isCellEditable( anEvent );
		}else{
			return false;
		}
	}

	/**
	 * 增加格子编辑监听器
	 */
	public void addCellEditorListener( CellEditorListener l ) {
		editor.addCellEditorListener( l );
	}

	/**
	 * 移除格子编辑监听器
	 */
	public void removeCellEditorListener( CellEditorListener l ) {
		editor.removeCellEditorListener( l );
	}

	/**
	 * 选中格子
	 * @param anEvent 事件对象
	 */
	public boolean shouldSelectCell( EventObject anEvent ) {
		return editor.shouldSelectCell( anEvent );
	}

	protected void selectEditor( MouseEvent e ) {
		if ( e == null ) {
			currentRow = table.getSelectionModel().getAnchorSelectionIndex();
		}
		else {
			currentRow = table.rowAtPoint( e.getPoint() );
		}
		editor = selectEditor( table, currentRow, editTypeCol );
	}

	/**
	 * 根据行row动态选择编辑器
	 * @param tbl 本体表
	 * @param row 行号
	 * @param editTypeColumn 编辑类型所在列
	 * @return 对应编辑器
	 */
	public TableCellEditor selectEditor( JTable tbl, int row, int editTypeColumn ) {
		int editType = ( ( Integer ) tbl.getModel().getValueAt( row, editTypeColumn ) ).intValue();
		return selectEditor( editType );
	}

	/**
	 * 根据编辑类型editType返回对应编辑器
	 * @param editType 编辑类型
	 * @return 编辑器
	 */
	public TableCellEditor selectEditor( int editType ) {
		TableCellEditor editor1 = null;
		switch ( editType ) {
			case Consts.INPUT_LINESTYLE:
				if ( lineStyleEditor == null ) {
					LineStyleComboBox lscb = new LineStyleComboBox();
					lscb.addActionListener(al);
					lineStyleEditor = new DefaultCellEditor( lscb );
				}
				editor1 = lineStyleEditor;
				break;
			case Consts.INPUT_ARROW:
				if ( arrowEditor == null ) {
					ArrowComboBox acb = new ArrowComboBox(false);
					acb.addActionListener(al);
					arrowEditor = new DefaultCellEditor( acb );
				}
				editor1 = arrowEditor;
				break;
			case Consts.INPUT_SIMPLE_ARROW:
				if ( simpleArrowEditor == null ) {
					ArrowComboBox acb = new ArrowComboBox(true);
					acb.addActionListener(al);
					simpleArrowEditor = new DefaultCellEditor( acb );
				}
				editor1 = simpleArrowEditor;
				break;
			case Consts.INPUT_TEXTURE:
				if ( textureEditor == null ) {
					TextureComboBox tcb = new TextureComboBox();
					tcb.addActionListener(al);
					textureEditor = new DefaultCellEditor( tcb );
				}
				editor1 = textureEditor;
				break;
			case Consts.INPUT_COLOR:
				if ( colorEditor == null ) {
					ColorComboBox ccb = new ColorComboBox( true );
					ccb.addActionListener(al);
					colorEditor = new DefaultCellEditor( ccb );
				}
				editor1 = colorEditor;
				break;
			case Consts.INPUT_FONT:
				if ( fontEditor == null ) {
					JComboBoxEx jcbe =  EditStyles.getFontBox();
					jcbe.addActionListener(al);
					fontEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = fontEditor;
				break;
			case Consts.INPUT_POINTSTYLE:
				if ( pointEditor == null ) {
					PointStyleComboBox pscb = new PointStyleComboBox();
					pscb.addActionListener(al);
					pointEditor = new DefaultCellEditor( pscb );
				}
				editor1 = pointEditor;
				break;
			case Consts.INPUT_FONTSTYLE:
				if ( fontStyleEditor == null ) {
					fontStyleEditor = new FontStyleEditor( owner );
				}
				editor1 = fontStyleEditor;
				break;
			case Consts.INPUT_DATE:
				if ( dateEditor == null ) {
					dateEditor = new DateEditor( owner );
				}
				editor1 = dateEditor;
				break;
			case Consts.INPUT_CHARTCOLOR:
				if ( chartColorEditor == null ) {
					chartColorEditor = new ChartColorEditor( owner );
				}
				editor1 = chartColorEditor;
				break;
			case Consts.INPUT_TICKS:
				if ( ticksEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getTicksBox();
					jcbe.addActionListener(al);
					ticksEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = ticksEditor;
				break;
			case Consts.INPUT_UNIT:
				if ( unitEditor == null ) {
					JComboBoxEx jcbe =  EditStyles.getUnitBox();
					jcbe.addActionListener(al);
					unitEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = unitEditor;
				break;
			case Consts.INPUT_COORDINATES:
				if ( coordEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getCoordinateBox();
					jcbe.addActionListener(al);
					coordEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = coordEditor;
				break;
			case Consts.INPUT_AXISLOCATION:
				if ( axisEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getAxisBox();
					jcbe.addActionListener(al);
					axisEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = axisEditor;
				break;
			case Consts.INPUT_HALIGN:
				if ( halignEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getHAlignBox();
					jcbe.addActionListener(al);
					halignEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = halignEditor;
				break;
			case Consts.INPUT_VALIGN:
				if ( valignEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getVAlignBox();
					jcbe.addActionListener(al);
					valignEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = valignEditor;
				break;
			case Consts.INPUT_IMAGEMODE:
				if ( imageModeEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getImageMode();
					jcbe.addActionListener(al);
					imageModeEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = imageModeEditor;
				break;
			case Consts.INPUT_FONTSIZE:
				if ( fontsizeEditor == null ) {
					JComboBoxEx fsBox = new FontSizeBox();
					fsBox.setEditable( true );
					fsBox.addActionListener(al);
					fontsizeEditor = new JComboBoxExEditor( fsBox );
				}
				editor1 = fontsizeEditor;
				break;
			case Consts.INPUT_COLUMNSTYLE:
				if ( columnStyleEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getColumnStyleBox();
					jcbe.addActionListener(al);
					columnStyleEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = columnStyleEditor;
				break;
			case Consts.INPUT_ANGLE:
				if ( angleEditor == null ) {
					angleEditor = new JTextAreaEditor( table, JTextAreaEditor.TYPE_SIGNED_INTEGER );
					angleEditor.setArrange( -360, 360, 1 );
				}
				editor1 = angleEditor;
				break;
			case Consts.INPUT_INTEGER:
				if ( intEditor == null ) {
					intEditor = new JTextAreaEditor( table, JTextAreaEditor.TYPE_SIGNED_INTEGER );
				}
				editor1 = intEditor;
				break;
			case Consts.INPUT_DOUBLE:
				if ( doubleEditor == null ) {
					doubleEditor = new JTextAreaEditor( table, JTextAreaEditor.TYPE_SIGNED_DOUBLE );
				}
				editor1 = doubleEditor;
				break;
			case Consts.INPUT_CHECKBOX:
				editor1 = checkEditor;
				break;
			case Consts.INPUT_LEGENDICON:
				if ( legendIconEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getLegendIconBox();
					jcbe.addActionListener(al);
					
					legendIconEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = legendIconEditor;
				break;
			case Consts.INPUT_DATEUNIT:
				if ( dateUnitEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getDateUnitBox();
					jcbe.addActionListener(al);
					
					dateUnitEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = dateUnitEditor;
				break;
			case Consts.INPUT_URLTARGET:
				if ( urlTargetEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getUrlTargetBox();
					jcbe.addActionListener(al);
					urlTargetEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = urlTargetEditor;
				break;
			case Consts.INPUT_TRANSFORM:
				if ( transformEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getTransformBox();
					jcbe.addActionListener(al);
					transformEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = transformEditor;
				break;
			case Consts.INPUT_STACKTYPE:
				if ( stackTypeEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getStackTypeBox();
					jcbe.addActionListener(al);
					stackTypeEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = stackTypeEditor;
				break;
			case Consts.INPUT_DISPLAYDATA:
				if ( displayDataEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getDisplayDataBox();
					jcbe.addActionListener(al);
					displayDataEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = displayDataEditor;
				break;
			case Consts.INPUT_LEGENDLOCATION:
				if ( legendLocationEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getLegendLocationBox();
					jcbe.addActionListener(al);
					legendLocationEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = legendLocationEditor;
				break;
			case Consts.INPUT_COLUMNTYPE:
				if ( inputColumnTypeEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getInputColumnTypeBox();
					jcbe.addActionListener(al);
					inputColumnTypeEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = inputColumnTypeEditor;
				break;
			case Consts.INPUT_LINETYPE:
				if ( inputLineTypeEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getInputLineTypeBox();
					jcbe.addActionListener(al);
					inputLineTypeEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = inputLineTypeEditor;
				break;
			case Consts.INPUT_PIETYPE:
				if ( inputPieTypeEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getInputPieTypeBox();
					jcbe.addActionListener(al);
					inputPieTypeEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = inputPieTypeEditor;
				break;
			case Consts.INPUT_2AXISTYPE:
				if ( input2AxisTypeEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getInput2AxisTypeBox();
					jcbe.addActionListener(al);
					input2AxisTypeEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = input2AxisTypeEditor;
				break;
			case Consts.INPUT_BARTYPE:
				if ( inputBarTypeEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getBarcodeType();
					jcbe.addActionListener(al);
					inputBarTypeEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = inputBarTypeEditor;
				break;
			case Consts.INPUT_CHARSET:
				if ( inputCharSetEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getCharSet();
					jcbe.addActionListener(al);
					inputCharSetEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = inputCharSetEditor;
				break;
			case Consts.INPUT_RECERROR:
				if ( inputRecErrorEditor == null ) {
					JComboBoxEx jcbe = EditStyles.getRecError();
					jcbe.addActionListener(al);
					inputRecErrorEditor = new JComboBoxExEditor( jcbe );
				}
				editor1 = inputRecErrorEditor;
				break;
			default:
				editor1 = defaultEditor;
				break;
		}
		return editor1;
	}

	/**
	 * 设置父窗口
	 * @param dialog 父窗口
	 */
	public void setOwner( Dialog dialog ) {
		owner = dialog;
	}

}
