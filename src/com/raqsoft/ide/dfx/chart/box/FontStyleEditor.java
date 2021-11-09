package com.raqsoft.ide.dfx.chart.box;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import com.raqsoft.chart.*;
import com.raqsoft.ide.dfx.resources.*;

/**
 * 字体样式编辑器
 * 
 * @author Joancy
 *
 */
public class FontStyleEditor extends DefaultCellEditor {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7339854682489514547L;
	protected Object editingVal = null;
	private FontStyleDialog dialog;

	private JButton button = new JButton();
	private FontStyleIcon icon = new FontStyleIcon();

	/**
	 * 构造函数
	 * @param owner 父窗口
	 */
	public FontStyleEditor( Dialog owner ) {
		super( new JCheckBox() );
		button.setIcon( icon );
		button.setHorizontalAlignment( JButton.CENTER );
		button.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				clicked();
			}
		} );
		dialog = new FontStyleDialog( owner );
	}

	protected void clicked() {
		int style = ( (Integer)editingVal ).intValue();
		dialog.setFontStyle( style );
		Point p = button.getLocationOnScreen();
		dialog.setLocation( p.x, p.y + button.getHeight() );
		dialog.setVisible(true);
		if( dialog.getOption() == JOptionPane.OK_OPTION ) {
			editingVal = new Integer( dialog.getFontStyle() );
			icon.setFontStyle( dialog.getFontStyle() );
			this.stopCellEditing();
		}
	}

	/**
	 * 实现父类抽象方法
	 */
	public Component getTableCellEditorComponent( JTable table, Object value,
												  boolean isSelected, int row, int column ) {
		editingVal = value;
		if ( isSelected ) {
			button.setBackground( table.getSelectionBackground() );
		}
		else {
			button.setBackground( table.getBackground() );
		}
		icon.setFontStyle( ( ( Integer ) value ).intValue() );
		return button;
	}

	/**
	 * 获取编辑值
	 */
	public Object getCellEditorValue() {
		return editingVal;
	}

	/**
	 * 停止编辑
	 */
	public boolean stopCellEditing() {
		return super.stopCellEditing();
	}

	protected void fireEditingStopped() {
		super.fireEditingStopped();
	}
}

class FontStyleRender extends JLabel implements TableCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3134413119715348845L;
	private FontStyleIcon icon = new FontStyleIcon();

	public FontStyleRender() {
		setOpaque( true );
		setHorizontalAlignment(CENTER);
		setVerticalAlignment(CENTER);
	}

	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column ) {
		icon.setFontStyle( ( ( Integer ) value ).intValue() );
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


class FontStyleIcon implements Icon {
	private int style;

	public FontStyleIcon() {
		this( 0 );
	}

	public FontStyleIcon( int style ) {
		this.style = style;
	}

	public void paintIcon( Component c, Graphics g, int x, int y ) {
		g.setColor( Color.black );
		String exam = ChartMessage.get().getMessage( "label.example" );    //示例
		if( Utils.isVertical( style ) ) {
			Utils.drawText( (Graphics2D)g, exam, x + 15, y, Utils.getFont( "Dialog", style, 10 ), Color.black, style, 0, Consts.LOCATION_CT );
		}
		else {
			Utils.drawText( (Graphics2D)g, exam, x, y + 12, Utils.getFont( "Dialog", style, 12 ), Color.black, style, 0, Consts.LOCATION_LM );
		}
	}

	public int getIconWidth() {
		return 30;
	}

	public int getIconHeight() {
		return 25;
	}

	public void setFontStyle( int style ) {
		this.style = style;
	}

}


