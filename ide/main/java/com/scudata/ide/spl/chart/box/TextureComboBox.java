package com.scudata.ide.spl.chart.box;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import com.scudata.chart.*;
import com.scudata.ide.common.swing.*;

/**
 * 填充纹理下拉列表
 * 
 * @author Joancy
 *
 */
public class TextureComboBox extends JComboBox {
	private static final long serialVersionUID = 1L;

	public TextureComboBox() {
		super( new Object[] {
			   Consts.PATTERN_DEFAULT,
			   Consts.PATTERN_H_THIN_LINE,
			   Consts.PATTERN_H_THICK_LINE,
			   Consts.PATTERN_V_THIN_LINE,
			   Consts.PATTERN_V_THICK_LINE,
			   Consts.PATTERN_THIN_SLASH,
			   Consts.PATTERN_THICK_SLASH,
			   Consts.PATTERN_THIN_BACKSLASH,
			   Consts.PATTERN_THICK_BACKSLASH,
			   Consts.PATTERN_THIN_GRID,
			   Consts.PATTERN_THICK_GRID,
			   Consts.PATTERN_THIN_BEVEL_GRID,
			   Consts.PATTERN_THICK_BEVEL_GRID,
			   Consts.PATTERN_DOT_1,
			   Consts.PATTERN_DOT_2,
			   Consts.PATTERN_DOT_3,
			   Consts.PATTERN_DOT_4,
			   Consts.PATTERN_SQUARE_FLOOR,
			   Consts.PATTERN_DIAMOND_FLOOR,
			   Consts.PATTERN_BRICK_WALL
		} );
		setRenderer( new TextureRender() );
		setEditor( new TextureComboBoxEditor() );
		this.setPreferredSize( new Dimension( 80, 25 ) );
	}

	/**
	 * 获取纹理类型
	 * @return 类型
	 */
	public int getValue() {
		return ( ( Integer ) getEditor().getItem() ).intValue();
	}

	class TextureComboBoxEditor extends AbstractComboBoxEditor {
		TextureIcon editorIcon = new TextureIcon();
		JLabel editorLabel = new JLabel( editorIcon );
		Object item = new Object();

		public TextureComboBoxEditor() {
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

class TextureRender extends JLabel implements ListCellRenderer, TableCellRenderer {
	private static final long serialVersionUID = -411690789129469231L;
	private TextureIcon icon = new TextureIcon();

	public TextureRender() {
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
		this.setPreferredSize( new Dimension( list.getWidth() - 10, 25 ) );
		icon.setWidth( list.getWidth() - 10 );
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
		icon.setWidth( w );
		setIcon( icon );
		return this;
	}
}

class TextureIcon implements Icon {

	private int type;
	private int width;

	public TextureIcon() {
		this( Consts.PATTERN_DEFAULT );
	}

	public TextureIcon( int type ) {
		this.type = type;
	}

	public void paintIcon( Component c, Graphics g, int x, int y ) {
		g.setColor( Color.black );
		ChartColor cc= new ChartColor(Color.WHITE);
		
		cc.setColor2(Color.DARK_GRAY);
		cc.setType(type);
		Utils.setPaint( (Graphics2D)g, 0, 0, width, 20, cc );
		g.fillRect( 5, 2, width, 20 );
	}

	public int getIconWidth() {
		return width + 10;
	}

	public int getIconHeight() {
		return 20;
	}

	public void setIconType( int type ) {
		this.type = type;
	}

	public void setWidth( int w ) {
		this.width = w;
	}
}

