package com.scudata.ide.spl.chart.box;

import java.awt.*;
import java.awt.geom.Point2D;

import javax.swing.*;
import javax.swing.table.*;

import com.scudata.chart.*;
import com.scudata.ide.common.swing.*;

/**
 * 点的类型下拉列表
 * 
 * @author Joancy
 *
 */
public class PointStyleComboBox extends JComboBox {
	private static final long serialVersionUID = 1L;

	public PointStyleComboBox() {
		super( new Object[] {
			   new Integer( Consts.PT_CIRCLE ),
			   new Integer( Consts.PT_DOT ),
			   new Integer( Consts.PT_SQUARE ),
			   new Integer( Consts.PT_TRIANGLE ),
			   new Integer( Consts.PT_RECTANGLE ),
			   new Integer( Consts.PT_STAR ),
			   new Integer( Consts.PT_DIAMOND ),
			   new Integer( Consts.PT_CORSS ),
			   new Integer( Consts.PT_PLUS ),
			   new Integer( Consts.PT_D_CIRCEL ),
			   new Integer( Consts.PT_D_SQUARE ),
			   new Integer( Consts.PT_D_TRIANGLE ),
			   new Integer( Consts.PT_D_RECTANGLE ),
			   new Integer( Consts.PT_D_DIAMOND ),
			   new Integer( Consts.PT_CIRCLE_PLUS ),
			   new Integer( Consts.PT_SQUARE_PLUS ),
			   new Integer( Consts.PT_TRIANGLE_PLUS ),
			   new Integer( Consts.PT_RECTANGLE_PLUS ),
			   new Integer( Consts.PT_DIAMOND_PLUS ),
			   new Integer( Consts.PT_NONE )
		} );
		setRenderer( new PointStyleRender() );
		setEditor( new PointStyleComboBoxEditor() );
		this.setPreferredSize( new Dimension( 70, 25 ) );
	}

	/**
	 * 获取点类型
	 * @return 整数类型的点类型
	 */
	public int getValue() {
		return ( ( Integer ) getEditor().getItem() ).intValue();
	}

	class PointStyleComboBoxEditor extends AbstractComboBoxEditor {
		PointStyleIcon editorIcon = new PointStyleIcon();
		JLabel editorLabel = new JLabel( editorIcon );
		Object item = new Object();

		public PointStyleComboBoxEditor() {
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

class PointStyleRender extends JLabel implements ListCellRenderer, TableCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1668766399969661267L;
	private PointStyleIcon icon = new PointStyleIcon();

	public PointStyleRender() {
		setOpaque( true );
		setHorizontalAlignment(CENTER);
		setVerticalAlignment(CENTER);
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
		setIcon( icon );
		return this;
	}
}


class PointStyleIcon implements Icon {
	private int type;

	public PointStyleIcon() {
		this( Consts.PT_CIRCLE );
	}

	public PointStyleIcon( int type ) {
		this.type = type;
	}

	public void paintIcon( Component c, Graphics g, int x, int y ) {
		ChartColor cc = new ChartColor(Color.white);
		cc.setGradient(false);
		Point2D p = new Point2D.Double(x+10,y+5);
		Utils.drawCartesianPoint2((Graphics2D)g, p, type, 4, 4, 4, Consts.LINE_SOLID, 1f,
				cc, Color.black, 1f);
	}

	public int getIconWidth() {
		return 20;
	}

	public int getIconHeight() {
		return 10;
	}

	public void setIconType( int type ) {
		this.type = type;
	}

}

