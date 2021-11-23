package com.scudata.ide.dfx.chart;

import javax.swing.*;
import javax.swing.table.*;

import com.scudata.ide.common.*;

import java.awt.*;

import javax.swing.border.*;
import java.awt.event.*;

/**
 * 参数值的序列表编辑表头渲染类
 * 
 * @author Joancy
 *
 */
public class HeaderRenderer extends JPanel implements TableCellRenderer, ActionListener, MouseListener {
	private static final long serialVersionUID = -6809294871123761055L;
	private static Border border = UIManager.getBorder("TableHeader.cellBorder");
	private static ImageIcon delIcon = GM.getImageIcon( GC.IMAGES_PATH + "s_del.gif" );
	private static ImageIcon showIcon = GM.getImageIcon( GC.IMAGES_PATH + "m_addrecord.gif" );
	private static ImageIcon hideIcon = GM.getImageIcon( GC.IMAGES_PATH + "m_deleterecord.gif" );
	private TableInputSeries seriesTable;
	private int currCol;
	private JLabel label;
	private JButton delButton;
	private JButton extButton;

	/**
	 * 创建一个缺省参数的表头渲染实例
	 */
	public HeaderRenderer() {
		super( new FlowLayout( FlowLayout.CENTER, 0, 0 ) );
		setBorder( border );
		delButton = new JButton( delIcon );
		delButton.setMargin( new Insets( 0, 0, 0, 0 ) );
		delButton.setPreferredSize( new Dimension( 23, 22 ) );
		delButton.setBorder( null );
		delButton.addActionListener( this );
		add( delButton );
		label = new JLabel();
		add( label );
		extButton = new JButton( showIcon );
		extButton.setMargin( new Insets( 0, 0, 0, 0 ) );
		extButton.setPreferredSize( new Dimension( 22, 22 ) );
		extButton.setBorder( null );
		extButton.addActionListener( this );
		add( extButton );
		delButton.setVisible( false );
		extButton.setVisible( false );
	}

	/**
	 * 实现渲染接口
	 */
	public Component getTableCellRendererComponent( JTable table, Object value,
													boolean isSelected, boolean hasFocus, int row, int column ) {
		seriesTable = (TableInputSeries) table;
		currCol = column;
		JTableHeader header = table.getTableHeader();
		if (header != null) {
			boolean hasThis = false;
			MouseListener[] mls = header.getMouseListeners();
			for( int i = 0; i < mls.length; i++ ) {
				if( mls[i].equals( this ) ) {
					hasThis = true;
					break;
				}
			}
			if( !hasThis ) header.addMouseListener( this );
			label.setFont( header.getFont() );
		}
		TableColumn tc = seriesTable.getColumn( currCol );
		String cfg = tc.getIdentifier().toString();
		if( cfg.indexOf( "," ) < 0 ) {
			delButton.setVisible( false );
			extButton.setVisible( false );
		}
		else {
			delButton.setVisible( true );
			extButton.setVisible( true );
			String isShow = cfg.substring( cfg.indexOf( "," ) + 1 );
			if( "s".equals( isShow ) ) extButton.setIcon( hideIcon );
			else extButton.setIcon( showIcon );
		}
		if( value != null ) label.setText( value.toString() );
		return this;
	}

	/**
	 * 表头点击事件触发
	 *
	 * @param e ActionEvent 事件对象
	 */
	public void actionPerformed( ActionEvent e ) {
		Object o = e.getSource();
		if( o.equals( delButton ) ) {
			seriesTable.deleteParam( currCol );
		}
		else if( o.equals( extButton ) ) {
			TableColumn tc = seriesTable.getColumn( currCol );
			String cfg = (String)tc.getIdentifier();
			if( cfg.endsWith( "s" ) ) {
				cfg = cfg.substring( 0, cfg.indexOf( "," ) ) + ",h";
				tc.setIdentifier( cfg );
				seriesTable.minimizeColumn( currCol + 1 );
			}
			else if( cfg.endsWith( "h" ) ) {
				cfg = cfg.substring( 0, cfg.indexOf( "," ) ) + ",s";
				tc.setIdentifier( cfg );
				seriesTable.recoverColumn( currCol + 1 );
			}
			seriesTable.repaint();
		}
	}

	/**
	 * 鼠标点击事件触发
	 * 
	 * @param e 鼠标事件实例
	 */
	public void mouseClicked( MouseEvent e ) {
		JTableHeader header = (JTableHeader) (e.getSource());
		JTable tableView = header.getTable();
		TableColumnModel columnModel = tableView.getColumnModel();
		int x = e.getX();
		int viewColumn = columnModel.getColumnIndexAtX(x);
		if( viewColumn < 0 || viewColumn != currCol ) return;
		TableColumn tc = columnModel.getColumn( viewColumn );
		JPanel panel = ( JPanel ) tc.getHeaderRenderer();
		if( panel == null || !panel.equals( this ) ) return;
		int w = 0;
		for( int i = 0; i < viewColumn; i++ ) {
			w += columnModel.getColumn(i).getWidth();
		}
		x = x - w;
		Component c = getComponentAt( panel, x, e.getY() );
		if( c == null ) return;
		if( c.equals( delButton ) ) {
			delButton.doClick();
		}
		else if( c.equals( extButton ) ) {
			extButton.doClick();
		}
	}

	public void mouseEntered( MouseEvent e ) {}
	public void mouseExited( MouseEvent e ) {}
	public void mousePressed( MouseEvent e ) {}
	public void mouseReleased( MouseEvent e ) {}

	private Component getComponentAt( JPanel panel, int x, int y ) {
		Component children[] = panel.getComponents();
		for( int i = 0; i < children.length; i++ ) {
			if( children[i].getBounds().contains( x, y ) ) return children[i];
		}
		return null;
	}

}
