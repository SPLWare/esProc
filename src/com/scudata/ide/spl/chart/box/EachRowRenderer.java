package com.scudata.ide.spl.chart.box;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import com.scudata.chart.*;
import com.scudata.ide.common.swing.*;

/**
 *属性框单元渲染器
 */
public class EachRowRenderer implements TableCellRenderer {
	int editTypeCol;
	public TableCellRenderer defaultRender;
	
	private TableCellRenderer render;
	private CheckBoxRenderer checkBoxRenderer;
	private TableCellRenderer lineStyleRender;
	private TableCellRenderer arrowRender;
	private TableCellRenderer textureRender;
	private TableCellRenderer colorRender;
	private TableCellRenderer pointRender;
	private TableCellRenderer fontStyleRender;
	private TableCellRenderer ticksRender;
	private TableCellRenderer unitRender;
	private TableCellRenderer coordRender;
	private TableCellRenderer axisRender;
	private TableCellRenderer fontsizeRender;
	private TableCellRenderer columnStyleRender;
	private TableCellRenderer chartColorRender;
	private TableCellRenderer halignRender;
	private TableCellRenderer valignRender,imageModeRender;
	private TableCellRenderer legendIconRender;
	private TableCellRenderer dateUnitRender;
	private TableCellRenderer transformRender;
	private TableCellRenderer stackTypeRender,displayDataRender,legendLocationRender;
	private TableCellRenderer inputColumnTypeRender,inputLineTypeRender;
	private TableCellRenderer inputPieTypeRender,input2AxisTypeRender;
	private TableCellRenderer inputBarTypeRender,inputCharSetRender,inputRecErrorRender;

	/**
	 * 构造函数
	 * @param editTypeCol 编辑类型所在列
	 */
	public EachRowRenderer( int editTypeCol ) {
		this.editTypeCol = editTypeCol;
		defaultRender = new DefaultParamTableRender( JLabel.CENTER );
		checkBoxRenderer = new CheckBoxRenderer();
	}

	/**
	 * 实现父类抽象方法
	 */
	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column ) {
		render = selectRenderer( table, row, editTypeCol );
		return render.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
	}

	/**
	 * 选择格子渲染器
	 * @param tbl 本体表
	 * @param row 行号
	 * @param editTypeColumn 编辑类型所在列号
	 * @return 格子渲染器
	 */
	public TableCellRenderer selectRenderer( JTable tbl, int row, int editTypeColumn ) {
		int editType = ( ( Integer ) tbl.getModel().getValueAt( row, editTypeColumn ) ).intValue();
		return selectRenderer( editType );
	}

	/**
	 * 根据编辑类型editType返回对应格子渲染器
	 * @param editType 编辑类型
	 * @return 对应格子渲染器
	 */
	public TableCellRenderer selectRenderer( int editType ) {
		TableCellRenderer render1 = null;
		switch ( editType ) {
			case Consts.INPUT_LINESTYLE:
				if ( lineStyleRender == null ) {
					lineStyleRender = new LineStyleRender();
				}
				render1 = lineStyleRender;
				break;
			case Consts.INPUT_ARROW:
			case Consts.INPUT_SIMPLE_ARROW:
				if ( arrowRender == null ) {
					arrowRender = new ArrowRender();
				}
				render1 = arrowRender;
				break;
			case Consts.INPUT_TEXTURE:
				if ( textureRender == null ) {
					textureRender = new TextureRender();
				}
				render1 = textureRender;
				break;
			case Consts.INPUT_COLOR:
				if ( colorRender == null ) {
					colorRender = new ColorCellRenderer();
				}
				render1 = colorRender;
				break;
			case Consts.INPUT_POINTSTYLE:
				if ( pointRender == null ) {
					pointRender = new PointStyleRender();
				}
				render1 = pointRender;
				break;
			case Consts.INPUT_FONTSTYLE:
				if ( fontStyleRender == null ) {
					fontStyleRender = new FontStyleRender();
				}
				render1 = fontStyleRender;
				break;
			case Consts.INPUT_CHARTCOLOR:
				if ( chartColorRender == null ) {
					chartColorRender = new ChartColorRender();
				}
				render1 = chartColorRender;
				break;
			case Consts.INPUT_TICKS:
				if ( ticksRender == null ) {
					ticksRender = new JComboBoxExRenderer( EditStyles.getTicksBox() );
				}
				render1 = ticksRender;
				break;
			case Consts.INPUT_UNIT:
				if ( unitRender == null ) {
					unitRender = new JComboBoxExRenderer( EditStyles.getUnitBox() );
				}
				render1 = unitRender;
				break;
			case Consts.INPUT_COORDINATES:
				if ( coordRender == null ) {
					coordRender = new JComboBoxExRenderer( EditStyles.getCoordinateBox() );
				}
				render1 = coordRender;
				break;
			case Consts.INPUT_AXISLOCATION:
				if ( axisRender == null ) {
					axisRender = new JComboBoxExRenderer( EditStyles.getAxisBox() );
				}
				render1 = axisRender;
				break;
			case Consts.INPUT_FONTSIZE:
				if ( fontsizeRender == null ) {
					fontsizeRender = new JComboBoxExRenderer( new FontSizeBox() );
				}
				render1 = fontsizeRender;
				break;
			case Consts.INPUT_COLUMNSTYLE:
				if ( columnStyleRender == null ) {
					columnStyleRender = new JComboBoxExRenderer( EditStyles.getColumnStyleBox() );
				}
				render1 = columnStyleRender;
				break;
			case Consts.INPUT_HALIGN:
				if ( halignRender == null ) {
					halignRender = new JComboBoxExRenderer( EditStyles.getHAlignBox() );
				}
				render1 = halignRender;
				break;
			case Consts.INPUT_VALIGN:
				if ( valignRender == null ) {
					valignRender = new JComboBoxExRenderer( EditStyles.getVAlignBox() );
				}
				render1 = valignRender;
				break;
			case Consts.INPUT_IMAGEMODE:
				if ( imageModeRender == null ) {
					imageModeRender = new JComboBoxExRenderer( EditStyles.getImageMode() );
				}
				render1 = imageModeRender;
				break;
			case Consts.INPUT_CHECKBOX:
				render1 = checkBoxRenderer;
				break;
			case Consts.INPUT_LEGENDICON:
				if ( legendIconRender == null ) {
					legendIconRender = new JComboBoxExRenderer( EditStyles.getLegendIconBox() );
				}
				render1 = legendIconRender;
				break;
			case Consts.INPUT_DATEUNIT:
				if ( dateUnitRender == null ) {
					dateUnitRender = new JComboBoxExRenderer( EditStyles.getDateUnitBox() );
				}
				render1 = dateUnitRender;
				break;
			case Consts.INPUT_TRANSFORM:
				if ( transformRender == null ) {
					transformRender = new JComboBoxExRenderer( EditStyles.getTransformBox() );
				}
				render1 = transformRender;
				break;
			case Consts.INPUT_STACKTYPE:
				if ( stackTypeRender == null ) {
					stackTypeRender = new JComboBoxExRenderer( EditStyles.getStackTypeBox() );
				}
				render1 = stackTypeRender;
				break;
			case Consts.INPUT_DISPLAYDATA:
				if ( displayDataRender == null ) {
					displayDataRender = new JComboBoxExRenderer( EditStyles.getDisplayDataBox() );
				}
				render1 = displayDataRender;
				break;
			case Consts.INPUT_LEGENDLOCATION:
				if ( legendLocationRender == null ) {
					legendLocationRender = new JComboBoxExRenderer( EditStyles.getLegendLocationBox() );
				}
				render1 = legendLocationRender;
				break;
			case Consts.INPUT_COLUMNTYPE:
				if ( inputColumnTypeRender == null ) {
					inputColumnTypeRender = new JComboBoxExRenderer( EditStyles.getInputColumnTypeBox() );
				}
				render1 = inputColumnTypeRender;
				break;
			case Consts.INPUT_LINETYPE:
				if ( inputLineTypeRender == null ) {
					inputLineTypeRender = new JComboBoxExRenderer( EditStyles.getInputLineTypeBox() );
				}
				render1 = inputLineTypeRender;
				break;
			case Consts.INPUT_PIETYPE:
				if ( inputPieTypeRender == null ) {
					inputPieTypeRender = new JComboBoxExRenderer( EditStyles.getInputPieTypeBox() );
				}
				render1 = inputPieTypeRender;
				break;
			case Consts.INPUT_2AXISTYPE:
				if ( input2AxisTypeRender == null ) {
					input2AxisTypeRender = new JComboBoxExRenderer( EditStyles.getInput2AxisTypeBox() );
				}
				render1 = input2AxisTypeRender;
				break;
			case Consts.INPUT_BARTYPE:
				if ( inputBarTypeRender == null ) {
					inputBarTypeRender = new JComboBoxExRenderer( EditStyles.getBarcodeType() );
				}
				render1 = inputBarTypeRender;
				break;
			case Consts.INPUT_CHARSET:
				if ( inputCharSetRender == null ) {
					inputCharSetRender = new JComboBoxExRenderer( EditStyles.getCharSet() );
				}
				render1 = inputCharSetRender;
				break;
			case Consts.INPUT_RECERROR:
				if ( inputRecErrorRender == null ) {
					inputRecErrorRender = new JComboBoxExRenderer( EditStyles.getRecError() );
				}
				render1 = inputRecErrorRender;
				break;
			default:
				render1 = defaultRender;
				break;
		}
		return render1;
	}

}
