package com.raqsoft.ide.dfx.chart;

import com.raqsoft.ide.common.swing.*;

import javax.swing.*;
import javax.swing.table.*;

import java.awt.*;
import java.awt.image.*;

import com.raqsoft.util.*;
import com.raqsoft.dm.*;
import com.raqsoft.common.*;
import com.raqsoft.chart.edit.*;
import com.raqsoft.ide.dfx.chart.box.*;
import com.raqsoft.cellset.datamodel.PgmNormalCell;
import com.raqsoft.chart.*;
import com.raqsoft.ide.dfx.resources.*;

/**
 * 序列值编辑表
 * 
 * @author Joancy
 *
 */
public class TableInputSeries extends JTableEx {
	private static final long serialVersionUID = 1L;
	private int currCols;  //当前正在编辑的属性数
	private TableParamEdit paramTable;
	private String expColName = ChartMessage.get().getMessage( "label.exp" );  //"表达式";
	private String nameColName = ChartMessage.get().getMessage( "label.paramname" ); //"参数名";

	/**
	 * 构造函数
	 * @param table 对应参数编辑表
	 */
	public TableInputSeries( TableParamEdit table ) {
		this.paramTable = table;
		this.currCols = 0;
		String[] colNames = new String[] { nameColName, expColName, nameColName, expColName, nameColName, expColName,
							nameColName, expColName, nameColName, expColName };
		data.setColumnIdentifiers( colNames );
		this.addRow();
		this.setRowHeight( 25 );
		for( int i = 0; i < 10; i++ ) {
			TableColumn tc = getColumn( i );
			//必须每列new一个新的renderer，否则查找不到是否点击了本列头的按钮
			tc.setHeaderRenderer( new HeaderRenderer() );
			if( i % 2 == 0 ) tc.setPreferredWidth( 120 );
			else {
				tc.setPreferredWidth( 80 );
				tc.setCellEditor( new JTextAreaEditor( this ) );
			}
			this.minimizeColumn( i );
		}
		this.recoverColumn( 0 );
		this.recoverColumn( 1 );
	}

	/**
	 * 从参数表添加一个参数来编辑
	 * @param row 参数在参数表中的行号
	 */
	public void addParam2Edit( int row, Dialog owner ) {
		if( currCols == 5 ) {
			JOptionPane.showMessageDialog( this, ChartMessage.get().getMessage( "info.maxparams" ) );  //"允许最多同时编辑5个参数！" );
			return;
		}
		for( int i = 1; i <= currCols; i++ ) {
			TableColumn tc = getColumn( i * 2 - 2 );
			if( tc.getIdentifier().toString().startsWith( row + "" ) ) return;
		}
		String propName = paramTable.getModel().getValueAt( row, 1 ).toString();
		String cfg = "";
		TableColumn nameTc, expTc;
		int nameIndex, expIndex;
		if( currCols > 0 ) {
			nameIndex = currCols * 2;
			expIndex = currCols * 2 + 1;
			this.recoverColumn( nameIndex );
			nameTc = getColumn( nameIndex );
			expTc = getColumn( expIndex );
			cfg = row + ",h";
		}
		else {
			nameIndex = 0;
			expIndex = 1;
			nameTc = getColumn( 0 );
			expTc = getColumn( 1 );
			cfg = row + ",s";
		}
		nameTc.setIdentifier( cfg );
		nameTc.setHeaderValue( propName );
		int w = getStringW( propName ) + 45 + 10;
		nameTc.setPreferredWidth( w );
		currCols++;

		//设置新加列每行的值
		EachRowEditor ere = (EachRowEditor)paramTable.getCellEditor( row, TableParamEdit.iVALUECOL );
		TableCellEditor tce = ere.selectEditor( paramTable, row, TableParamEdit.iEDITSTYLECOL );
		nameTc.setCellEditor( tce );
		
		EachRowRenderer err = (EachRowRenderer)paramTable.getCellRenderer( row, TableParamEdit.iVALUECOL );
		TableCellRenderer tcr = err.selectRenderer( paramTable, row, TableParamEdit.iEDITSTYLECOL );
		nameTc.setCellRenderer( tcr );
		
		Object o = paramTable.getModel().getValueAt( row, 3 );
		if( o != null ) {
			o = PgmNormalCell.parseConstValue( o.toString().trim() );
		}
		boolean isCC = isChartColor( o );
		ParamInfo info = (ParamInfo) paramTable.getModel().getValueAt( row,  TableParamEdit.iOBJCOL );
		if( o == null || o.toString().trim().length() == 0 || isCC ) {  //没有表达式
			o = paramTable.getModel().getValueAt( row, 2 );
			for( int i = 0; i < this.getRowCount(); i++ ) {
				if( i == 0 ) data.setValueAt( o, i, nameIndex );
				else data.setValueAt( info.getDefValue(), i, nameIndex );
				data.setValueAt( "", i, expIndex );
			}
		}
		else {   //是表达式
			String exp = ((String)paramTable.getModel().getValueAt( row, 3 )).trim();
			if( exp.startsWith( "[" ) && !isCC ) {
				if( !exp.endsWith( "]" ) ) {
					JOptionPane.showMessageDialog( this, exp + ChartMessage.get().getMessage( "info.nozkh" ) );  //"缺少]" );
					return;
				}
				exp = exp.substring( 1, exp.length() - 1 ).trim();
				ArgumentTokenizer at = new ArgumentTokenizer( exp, ',' );
				int addRows = at.countTokens() - this.getRowCount();
				for( int r = 1; r <= addRows; r++ ) {
					myAddRow();
				}
				for( int r = 0; r < this.getRowCount(); r++ ) {
					if( at.hasMoreTokens() ) {
						String tmp = at.nextToken().trim();
						Object val = PgmNormalCell.parseConstValue( tmp );
						if ( Variant.isEquals( tmp, val ) || isChartColor( val ) ) { //说明是表达式
							data.setValueAt( tmp, r, expIndex );
							data.setValueAt( TableParamEdit.toValueObject( info, tmp ), r, nameIndex );
						}
						else {
							data.setValueAt( val, r, nameIndex );
						}
					}
					else {
						data.setValueAt( "", r, expIndex );
						data.setValueAt( info.getDefValue(), r, nameIndex );
					}
				}
			}
			else {   //普通表达式或ChartColor
				for( int i = 0; i < this.getRowCount(); i++ ) {
					if( i == 0 ) {
						if( isCC ) {
							data.setValueAt( info.getDefValue(), i, nameIndex );
							data.setValueAt( exp, i, expIndex );
						}
						else {
							data.setValueAt( TableParamEdit.toValueObject( info, exp ), i, nameIndex );
							data.setValueAt( exp, i, expIndex );
						}
					}
					else {
						data.setValueAt( info.getDefValue(), i, nameIndex );
						data.setValueAt( "", i, expIndex );
					}
				}
			}
		}

		reDraw();
	}

	/**
	 * 是否是ChartColor
	 * @param o 参数值
	 * @return 如果是ChartColor类的参数值返回true，否则返回false
	 */
	public static boolean isChartColor( Object o ) {
		if( !( o instanceof Sequence ) ) return false;
		Sequence s = (Sequence) o;
		if( s.length() != 6 ) return false;
		try {
			ChartColor.getInstance( s );
		}catch( Throwable t ) {
			return false;
		}
		return true;
	}

	private int getStringW( String s ) {
		Font font = this.getTableHeader().getFont();
		FontMetrics fm = new BufferedImage( 10, 10, BufferedImage.TYPE_INT_RGB ).getGraphics().getFontMetrics( font );
		return fm.stringWidth( s );
	}

	/**
	 * 删除指定位置的参数值
	 * @param colIndex 序号
	 */
	public void deleteParam( int colIndex ) {
		if( currCols == 0 ) return;
		if( currCols == 1 ) {
			TableColumn tc = getColumn(0);
			tc.setHeaderValue( nameColName );
			tc.setIdentifier( nameColName );
			tc.setPreferredWidth( 120 );
			tc.setCellRenderer( null );
			tc.setCellEditor( null );
			this.recoverColumn( 1 );
			for( int i = 0; i < this.getRowCount(); i++ ) {
				data.setValueAt( null, i, 0 );
				data.setValueAt( "", i, 1 );
			}
		}
		else {
			for( int i = colIndex + 2; i < currCols * 2; i++ ) {
				this.getColumnModel().moveColumn( i, i - 2 );
			}
			this.minimizeColumn( currCols * 2 - 2 );
			this.minimizeColumn( currCols * 2 - 1 );
		}
		reDraw();
		currCols--;
	}

	private void reDraw() {
		this.getParent().repaint();
	}

	/**
	 * 追加一行
	 */
	public void myAddRow() {
		int row = this.addRow();
		for( int col = 0; col < currCols * 2; col += 2 ) {
			ParamInfo info = getParamInfo( col );
			if( info != null ) {
				data.setValueAt( info.getDefValue(), row, col );
			}
		}
	}

	/**
	 * 在当前行前插入一行
	 */
	public void myInsertRow() {
		int row = this.insertRow( this.getSelectedRow(), null );
		for( int col = 0; col < currCols * 2; col += 2 ) {
			ParamInfo info = getParamInfo( col );
			if( info != null ) {
				data.setValueAt( info.getDefValue(), row, col );
			}
		}
	}

	/**
	 * 删除当前行
	 */
	public void myDelRow() {
		if( this.deleteSelectedRows() ) updateParams();
	}

	private ParamInfo getParamInfo( int col ) {
		int row = getParamRow( col );
		if( row < 0 ) return null;
		return (ParamInfo) paramTable.getModel().getValueAt( row, 6 );
	}

	private int getParamRow( int col ) {
		if( col % 2 == 1 ) col--;
		String cfg = this.getColumn( col ).getIdentifier().toString();
		int pos = cfg.indexOf( "," );
		if( pos <= 0 ) return -1;
		int row = -1;
		try {
			row = Integer.parseInt( cfg.substring( 0, pos ) );
		}catch( Exception e ) {}
		return row;
	}

	/**
	 * 某个单元格被修改后触发
	 */
	public void setValueAt( Object aValue, int row, int column ) {
		Object oldValue = getValueAt( row, column );
		int nameIndex, expIndex;
		if( column % 2 == 1 ) {  //表达式列
			aValue = aValue.toString().trim();
			if( oldValue == null ) oldValue = "";
			expIndex = column;
			nameIndex = column - 1;
		}
		else {
			if( oldValue == null ) oldValue = "";
			nameIndex = column;
			expIndex = column + 1;
		}
		if ( Variant.isEquals( aValue, oldValue ) ) {
			return;
		}
		super.setValueAt( aValue, row, column );

		ParamInfo info = getParamInfo( column );
		if( info == null ) return;
		if ( column % 2 == 0 ) { //输入值，要在表达式列显示下拉等输入方式选择的值
			super.setValueAt( TableParamEdit.toExpString( info, aValue ), row, expIndex );
		}
		else { //输入表达式，要将能解析的值显示成值列对应的显示值
			super.setValueAt( TableParamEdit.toValueObject( info, aValue.toString() ), row, nameIndex );
		}
		updateParams();
	}

	private String getSeriesValueString( int nameIndex, int expIndex, ParamInfo info ) {
		if(getRowCount()==0){
			return "";
		}
		StringBuffer sb = new StringBuffer( "[" );
		for( int r = 0; r < this.getRowCount(); r++ ) {
			Object expo = this.getValueAt( r, expIndex );
			Object o = this.getValueAt( r, nameIndex );
			if(o instanceof Color){
				Color c = (Color)o;
				o = c.getRGB();
			}
			String exps = "";
			if( expo != null ) exps = expo.toString().trim();
			if( exps.length() == 0 ) {   //没有表达式
				if( o == null ) o = "";
				
				Object obj = Variant.parse(o.toString(), false);
				if(obj instanceof Number){
					sb.append( o.toString() );
				}else if(obj==null){
					sb.append("\"\"");
				}else if(obj instanceof Sequence){
					sb.append(obj.toString());
				}else{
					sb.append( Escape.addEscAndQuote(obj.toString()));
				}
			}
			else {
				sb.append( exps );
			}
			if( r < this.getRowCount() - 1 ) sb.append( "," );
		}
		sb.append( "]" );
		return sb.toString();
	}

	private void updateParam( int col ) {
		int paramRow = getParamRow( col );
		ParamInfo info = getParamInfo( col );
		if( info == null ) return;
		int nameIndex, expIndex;
		if( col % 2 == 0 ) {
			nameIndex = col;
			expIndex = col + 1;
		}
		else {
			nameIndex = col - 1;
			expIndex = col;
		}
		String exp = getSeriesValueString( nameIndex, expIndex, info );
		paramTable.setValueAt( exp, paramRow, TableParamEdit.iEXPCOL );
	}

	/**
	 * 将当前编辑表的序列值更新到参数编辑表
	 */
	public void updateParams() {
		acceptText();
		for( int i = 0; i < currCols * 2; i += 2 ) {
			updateParam( i );
		}
	}

	/**
	 * 接受当前的编辑值
	 */
	public void acceptText() {
		if ( this.isEditing() ) {
			this.getCellEditor().stopCellEditing();
		}
	}

}
