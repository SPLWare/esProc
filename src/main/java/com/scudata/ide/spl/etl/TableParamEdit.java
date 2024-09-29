package com.scudata.ide.spl.etl;

import java.awt.Dialog;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import javax.swing.table.*;

import java.util.*;

import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.chart.*;
import com.scudata.dm.*;
import com.scudata.ide.common.*;
import com.scudata.ide.common.swing.*;
import com.scudata.ide.spl.chart.ImageEditor;
import com.scudata.ide.spl.chart.ImageRenderer;
import com.scudata.ide.spl.chart.box.*;
import com.scudata.ide.spl.dialog.DialogTextEditor;
import com.scudata.ide.spl.resources.*;
import com.scudata.util.*;

/**
 * 参数编辑表
 */
public class TableParamEdit extends JTableEx {
	private static final long serialVersionUID = 924940299890651265L;
	private String NAMECOL = ChartMessage.get().getMessage( "label.propname" );  //"属性名称";
	private String VALUECOL = ChartMessage.get().getMessage( "label.propvalue" );  //"属性值";
	private String EXPCOL = ChartMessage.get().getMessage( "label.propexp" );  //"属性值表达式";
	private String EDITSTYLECOL = "editstyle";
	private String OBJCOL = "objcol"; //ParamInfo对象，如果是null则代表分组行
	
	private int iNAMECOL = 1;
	private int iVALUECOL = 2;
	private int iEXPCOL = 3;
	private int iEDITSTYLECOL = 4;
	private int iOBJCOL = 5; //ParamInfo对象，如果是null则代表分组行

	private HashMap<String,ArrayList<Object[]>> hiddenMap = new HashMap<String,ArrayList<Object[]>>(); //隐藏行的数据 类型ArrayList 每个对象Object[]

	Dialog owner;
	
	/**
	 * 构造函数
	 * @param owner 父窗口
	 */
	public TableParamEdit( Dialog owner) {
		this.owner = owner;
		String[] colNames = new String[] { " ", NAMECOL, VALUECOL, EXPCOL, EDITSTYLECOL, OBJCOL };
		data.setColumnIdentifiers( colNames );

		this.setRowHeight( 25 );
		DefaultParamTableRender render = new DefaultParamTableRender();
		TableColumn tc;
		tc = getColumn( 0 );
		tc.setMaxWidth( 20 );
		tc.setMinWidth( 20 );
		tc.setCellEditor( new ImageEditor() );
		tc.setCellRenderer( new ImageRenderer() );

		tc = getColumn( iNAMECOL );
		tc.setCellRenderer( render );
		tc.setPreferredWidth( 200 );

		tc = getColumn( iVALUECOL );
		tc.setCellEditor( new EtlRowEditor( this, iEDITSTYLECOL, owner ) );
		tc.setCellRenderer( new EtlRowRenderer( iEDITSTYLECOL,owner ) );

		tc = getColumn( iEXPCOL );
		tc.setCellEditor( new JTextAreaEditor( this ) );
		tc.setCellRenderer( render );

		setColumnVisible( EDITSTYLECOL, false );
		setColumnVisible( OBJCOL, false );

		setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		setColumnSelectionAllowed( true ); //列可选
		setRowSelectionAllowed( true ); //行可选
	}

	/**
	 * 设置参数信息列表
	 * @param list 参数信息列表
	 */
	public void setParamEdit(ParamInfoList list ) {
		acceptText();
		data.setRowCount(0);
		hiddenMap.clear();
		TableColumn tc;
		tc = getColumn( iVALUECOL );
		tc.setCellEditor( new EtlRowEditor( this, iEDITSTYLECOL, owner ) );
		tc.setCellRenderer( new EtlRowRenderer( iEDITSTYLECOL,owner ) );
		
		//生成数据
		java.util.List<ParamInfo> root = list.getRootParams();
		for( int i = 0; i < root.size(); i++ ) {
			ParamInfo pi = root.get( i );
			Object row[] = new Object[6];
			row[0] = null;
			row[iNAMECOL] = pi.getTitle();
			Object value = pi.getValue();
			if( value instanceof String ) {
				if( value.toString().startsWith( "=" ) ) {
					row[iEXPCOL] = value.toString().substring( 1 );
				}
				else row[iVALUECOL] = value;
			}
			else {
				row[iVALUECOL] = value;
			}
			row[iEDITSTYLECOL] = new Integer( pi.getInputType() );
			row[iOBJCOL] = pi;
			data.addRow( row );
		}
		java.util.List<String> groups = list.getGroupNames();
		if( groups != null ) {
			for ( int i = 0; i < groups.size(); i++ ) {
				String groupName = groups.get( i );
				Object[] grow = new Object[6];
				if( i == groups.size() - 1 ) grow[0] = new Byte( GC.TYPE_LASTMINUS );
				else grow[0] = new Byte( GC.TYPE_MINUS );
				grow[iNAMECOL] = groupName;
				grow[iEDITSTYLECOL] = new Integer( Consts.INPUT_NORMAL );
				data.addRow( grow );
				ArrayList<Object[]> grows = new ArrayList<Object[]>();
				java.util.List<ParamInfo> glist = list.getParams( groupName );
				for( int j = 0; j < glist.size(); j++ ) {
					ParamInfo pi = glist.get( j );
					Object row[] = new Object[6];
					if( j == glist.size() - 1 ) row[0] = new Byte( GC.TYPE_LASTNODE );
					else row[0] = new Byte( GC.TYPE_NODE );
					row[iNAMECOL] = pi.getTitle();
					Object value = pi.getValue();
					if( value instanceof String ) {
						if( value.toString().startsWith( "=" ) ) {
							row[iEXPCOL] = value.toString().substring( 1 );
							row[iVALUECOL] = pi.getDefValue();
						}
						else row[iVALUECOL] = value;
					}
					else {
						if( value instanceof ArrayList ) {
							row[iVALUECOL] = arrayList2Series( ( ArrayList ) value );
							pi.setValue( row[iVALUECOL] );
						}
						else row[iVALUECOL] = value;
					}
					row[iEDITSTYLECOL] = new Integer( pi.getInputType() );
					row[iOBJCOL] = pi;
					data.addRow( row );
					grows.add( row );
				}
				hiddenMap.put( groupName, grows );
			}
		}
	}

	/**
	 * 实现鼠标双击触发事件
	 * @param xpos 横坐标
	 * @param ypos 纵坐标
	 * @param row  行号
	 * @param col  列号
	 * @param e 鼠标事件
	 */
	public void doubleClicked(int xpos, int ypos, int row, int col, MouseEvent e) {
		if(col==0){
			return;
		}
		if(!isCellEditable(row,col)){
			return;
		}
		if(col==iVALUECOL){
			Object editStyle = data.getValueAt( row, iEDITSTYLECOL );
			byte es = ((Number)editStyle).byteValue(); 
			if(es==Consts.INPUT_FILE){
				String fileExt = "ctx,btx,csv,txt,xls,xlsx";
				File file = GM.dialogSelectFile(owner, fileExt);
				if(file!=null){
					String txt = file.getAbsolutePath();
					Object src = e.getSource();
					if(src instanceof JTextField){
						JTextField tf = (JTextField)src;
						tf.setText(txt);
					}
					setValueAt(txt, row, col);
					acceptText();
				}
				return;
			}else if(es!=Consts.INPUT_NORMAL && es!=EtlConsts.INPUT_ONLYPROPERTY){
//				常规表达式以及仅属性输入时，使用后续文本输入窗口
				return;
			}
		}
		

		Object val = data.getValueAt( row, col );
		if(val!=null && !(val instanceof String)){
			return;
		}
		
		DialogTextEditor dte = new DialogTextEditor();
		String exp = (String)val;
		dte.setText(exp);
		dte.setVisible(true);
		if (dte.getOption() == JOptionPane.OK_OPTION) {
			String txt = dte.getText();
			Object src = e.getSource();
			if(src instanceof JTextField){
				JTextField tf = (JTextField)src;
				tf.setText(txt);
			}
			setValueAt(txt, row, col);
			acceptText();
		}
		
	}
	
	/**
	 * 实现鼠标点击事件
	 */
	public void mouseClicked( MouseEvent e ) {
		super.mouseClicked( e );
		
		if ( e.getButton() == MouseEvent.BUTTON1 ) {
			int row = getSelectedRow();
			int col = getSelectedColumn();
			if ( col != 0 ) {  //鼠标点击行首列时，收缩与展开属性组
				return;
			}
			if ( data.getValueAt( row, col ) == null || ! ( data.getValueAt( row, col ) instanceof Byte ) ) {
				return;
			}
			byte oldType = ( ( Byte ) data.getValueAt( row, col ) ).byteValue();
			byte newType = oldType;
			ArrayList<Object[]> list = new ArrayList<Object[]>();
			Object rowData[];
			String key = ( String ) data.getValueAt( row, 1 );
			acceptText();
			switch ( oldType ) {
				case GC.TYPE_MINUS:
					newType = GC.TYPE_PLUS;
				case GC.TYPE_LASTMINUS:
					newType = GC.TYPE_LASTPLUS;
					while ( row + 1 < data.getRowCount() && data.getValueAt( row + 1, iOBJCOL ) != null ) { //找到表尾或下一个分组行为止
						rowData = new Object[data.getColumnCount()];
						for ( int c = 0; c < data.getColumnCount(); c++ ) {
							rowData[c] = data.getValueAt( row + 1, c );
						}
						list.add( rowData );
						data.removeRow( row + 1 );
					}
					hiddenMap.put( key, list );
					break;
				case GC.TYPE_PLUS:
					newType = GC.TYPE_MINUS;
				case GC.TYPE_LASTPLUS:
					newType = GC.TYPE_LASTMINUS;
					expand( key, row + 1 );
					break;
			}
			data.setValueAt( new Byte( newType ), row, col );
			acceptText();
		}
	}

	/**
	 * 判断指定的行列是否允许编辑
	 * @param row 行号
	 * @param column 列号
	 */
	public boolean isCellEditable( int row, int column ) {
		ParamInfo info = ( ParamInfo ) data.getValueAt( row, iOBJCOL );
		if( info == null ) {   //分组行不能编辑
			return false;
		}
		if(column==iEXPCOL){
			Object val = data.getValueAt(row, iVALUECOL);
			if(val instanceof Boolean){
				return false;
			}
			int type = (Integer)data.getValueAt(row, iEDITSTYLECOL);
			return !EtlConsts.isDisableExpEditType(type);			
		}
		return column != iNAMECOL;
	}

	/**
	 * 实现某个属性值被修改后触发的事件
	 * @param aValue 新的属性值
	 * @param row 行号
	 * @param column 列号
	 */
	public void setValueAt( Object aValue, int row, int column ) {
		Object oldValue = getValueAt( row, column );
		if( oldValue == null ) oldValue = "";
		if( column == iEXPCOL ) {
			aValue = aValue.toString().trim();
		}
		if ( Variant.isEquals( aValue, oldValue ) ) {
			return;
		}
		super.setValueAt( aValue, row, column );
		ParamInfo info = ( ParamInfo ) data.getValueAt( row, iOBJCOL );
		if( info != null ) {
			if( column == iVALUECOL ) {   //输入值，要在表达式列显示下拉等输入方式选择的值
				super.setValueAt( toExpString( info, aValue ), row, iEXPCOL );
			}
			else if( column == iEXPCOL ) {  //输入表达式，要将能解析的值显示成值列对应的显示值
				super.setValueAt( toValueObject( info, aValue.toString() ), row, iVALUECOL );
				if( aValue.toString().trim().length() == 0 ) {
					aValue = info.getDefValue();
				}
				else aValue = "=" + aValue.toString();
			}
			else return;
			info.setValue( aValue );
		}
	}

	private void expand( String groupName, int row ) {
		ArrayList<Object[]> list = hiddenMap.get( groupName );
		for ( int i = 0; i < list.size(); i++ ) {
			data.insertRow( row + i, list.get( i ) );
		}
	}

	/**
	 * 接受当前编辑好的数值
	 */
	public void acceptText() {
		if ( this.isEditing() ) {
			this.getCellEditor().stopCellEditing();
		}
	}

	/**
	 * 将值value根据参数信息info里面的类型定义转换为文本串
	 * @param info 参数信息
	 * @param value 值
	 * @return 文本串
	 */
	public static String toExpString( ParamInfo info, Object value ) {
		int inputType = info.getInputType();
		switch( inputType ) {
			case Consts.INPUT_COLOR:
			case Consts.INPUT_LINESTYLE:
			case Consts.INPUT_TEXTURE:
			case Consts.INPUT_POINTSTYLE:
			case Consts.INPUT_FONTSTYLE:
			case Consts.INPUT_COLUMNSTYLE:
			case Consts.INPUT_DROPDOWN:
			case Consts.INPUT_CHARTCOLOR:
			case Consts.INPUT_ARROW:
			case Consts.INPUT_TICKS:
			case Consts.INPUT_UNIT:
			case Consts.INPUT_COORDINATES:
			case Consts.INPUT_AXISLOCATION:
			case Consts.INPUT_FONTSIZE:
			case Consts.INPUT_HALIGN:
			case Consts.INPUT_VALIGN:
			case Consts.INPUT_LEGENDICON:
				return value == null ? "" : value.toString();
			default:
				return "";
		}
	}

	/**
	 * 将文本串值exp根据参数信息info里的类型定义转换为对应的值
	 * @param info 参数信息
	 * @param exp 文本串值
	 * @return 数据值
	 */
	public static Object toValueObject( ParamInfo info, String exp ) {
		int inputType = info.getInputType();
		switch( inputType ) {
			case Consts.INPUT_NORMAL:
			case Consts.INPUT_EXP:
			case Consts.INPUT_DATE:
				return "";
			case Consts.INPUT_CHARTCOLOR:
				return info.getDefValue();
			default:
				Object value = PgmNormalCell.parseConstValue( exp );
				if( isRightType( inputType, value ) ) return value;
				return info.getDefValue();
		}
	}

	/**
	 * 判断值value是否跟类型type匹配
	 * @param type 编辑的类型
	 * @param value 数据值
	 * @return 一致时返回true，否则返回false
	 */
	public static boolean isRightType( int type, Object value ) {
		switch( type ) {
			case Consts.INPUT_ANGLE:
			case Consts.INPUT_ARROW:
			case Consts.INPUT_AXISLOCATION:
			case Consts.INPUT_COLOR:
			case Consts.INPUT_COLUMNSTYLE:
			case Consts.INPUT_COORDINATES:
			case Consts.INPUT_FONTSIZE:
			case Consts.INPUT_FONTSTYLE:
			case Consts.INPUT_HALIGN:
			case Consts.INPUT_VALIGN:
			case Consts.INPUT_INTEGER:
			case Consts.INPUT_LEGENDICON:
			case Consts.INPUT_LINESTYLE:
			case Consts.INPUT_POINTSTYLE:
			case Consts.INPUT_TEXTURE:
			case Consts.INPUT_TICKS:
			case Consts.INPUT_UNIT:
				return value instanceof Integer;
			case Consts.INPUT_DOUBLE:
				return value instanceof Double;
			case Consts.INPUT_FONT:
				return value instanceof String;
			case Consts.INPUT_CHECKBOX:
				return value instanceof Boolean;
		}
		return false;
	}

	private Sequence arrayList2Series( ArrayList list ) {
		Sequence series = new Sequence();
		for( int i = 0; i < list.size(); i++ ) {
			Object o = list.get( i );
			if( o instanceof ArrayList ) {
				series.add( arrayList2Series( ( ArrayList ) o ) );
			}
			else series.add( o );
		}
		return series;
	}

}


