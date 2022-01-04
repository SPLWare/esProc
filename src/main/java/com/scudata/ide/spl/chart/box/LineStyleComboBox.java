package com.scudata.ide.spl.chart.box;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import com.scudata.chart.*;
import com.scudata.ide.common.swing.*;

/**
 * 直线风格下拉列表
 * 
 * @author Joancy
 *
 */
public class LineStyleComboBox extends JComboBox{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4233172328821173300L;

	/**
	 * 构造函数
	 */
	public LineStyleComboBox() {
		super( new Object[] {
			   new Integer( Consts.LINE_SOLID ),
			   new Integer( Consts.LINE_DASHED ),
			   new Integer( Consts.LINE_DOTTED ),
			   new Integer( Consts.LINE_DOTDASH ),
			   new Integer( Consts.LINE_DOUBLE ),
			   new Integer( Consts.LINE_NONE )
		} );
		setRenderer( new LineStyleRender() );
		setEditor( new LineStyleComboBoxEditor() );
		this.setPreferredSize( new Dimension( 70, 25 ) );
	}

	/**
	 * 获取当前值
	 * @return 线的风格值
	 */
	public int getValue() {
		return ( ( Integer ) getEditor().getItem() ).intValue();
	}

	class LineStyleComboBoxEditor extends AbstractComboBoxEditor {
		LineStyleIcon editorIcon = new LineStyleIcon();
		JLabel editorLabel = new JLabel( editorIcon );
		Object item = new Object();

		public LineStyleComboBoxEditor() {
			editorLabel.setBorder( BorderFactory.createEtchedBorder() );
		}

		public Component getEditorComponent() {
			return editorLabel;
		}

		public Object getItem() {
			return item;
		}

		public void setItem( Object itemToSet ) {
			item = itemToSet;
			editorIcon.setIconType( ( ( Integer ) itemToSet ).intValue() );
			editorLabel.setText( " " );
		}

		public void selectAll() {
			// from ComboBoxModel interface:  nothing to select
		}
	}

}

class LineStyleRender extends JLabel implements ListCellRenderer, TableCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7775536535295991604L;
	private LineStyleIcon icon = new LineStyleIcon();

	public LineStyleRender() {
		setOpaque( true );
	}

	public Component getListCellRendererComponent(
		JList list,
		Object value,
		int index,
		boolean isSelected,
		boolean cellHasFocus ) {

		icon.setIconType( ( ( Integer ) value ).intValue() );
		setText( " " );

		if ( isSelected ) {
			setForeground( list.getSelectionForeground() );
			setBackground( list.getSelectionBackground() );
		}
		else {
			setForeground( list.getForeground() );
			setBackground( list.getBackground() );
		}
		this.setPreferredSize( new Dimension( list.getWidth(), 25 ) );
		icon.setWidth( list.getWidth() - 5 );
		setIcon( icon );
		return this;
	}

	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column ) {
		icon.setIconType( ( ( Integer ) value ).intValue() );
		setText( " " );

		if ( isSelected ) {
			setForeground( table.getSelectionForeground() );
			setBackground( table.getSelectionBackground() );
		}
		else {
			setForeground( table.getForeground() );
			setBackground( table.getBackground() );
		}
		int w = ((JTableEx)table).getColumn( column ).getWidth();
		this.setPreferredSize( new Dimension( w, 25 ) );
		icon.setWidth( w - 5 );
		setIcon( icon );
		return this;
	}
}


class LineStyleIcon implements Icon {
	private int width;
	private int type;

	public LineStyleIcon() {
		this( Consts.LINE_SOLID );
	}

	public LineStyleIcon( int type ) {
		this.type = type;
	}

	public void paintIcon( Component c, Graphics g, int x, int y ) {
		//none/dotted/dashed/solid/double
		//无，点线、虚线、实线、双线、点划线
		float lineWidth = 1.2f;
		if ( type == Consts.LINE_NONE ) {
			lineWidth = .0f;
		}
		Utils.setStroke( ( Graphics2D )g, Color.black, type, lineWidth );
		if ( type != Consts.LINE_NONE ) {
			g.drawLine( x + 5, y + 5, x + width, y + 5 );
		}
	}

	public int getIconWidth() {
		return width + 5;
	}

	public int getIconHeight() {
		return 10;
	}

	public void setIconType( int type ) {
		this.type = type;
	}

	public void setWidth( int w ) {
		this.width = w;
	}

}

