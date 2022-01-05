package com.scudata.ide.spl.chart.box;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.event.*;

import com.scudata.chart.*;
import com.scudata.ide.common.*;
import com.scudata.ide.common.swing.*;
import com.scudata.ide.spl.resources.*;

/**
 * 填充颜色编辑对话框
 * 
 * @author Joancy
 *
 */
public class ChartColorDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private int m_option = JOptionPane.CLOSED_OPTION;
	private ChartColor cc = new ChartColor();
	JLabel labelFillType = new JLabel("Fill type");
	TextureComboBox fillType = new TextureComboBox();
	
	JCheckBox jianjin = new JCheckBox();
	JButton okbtn = new JButton();
	JButton cancelbtn = new JButton();
	ColorComboBox color1 = new ColorComboBox( true );
	ColorComboBox color2 = new ColorComboBox( true );
	JLabel labelC1 = new JLabel();
	JLabel labelC2 = new JLabel();
	JLabel labelAngle = new JLabel();
	JSpinner angle = new JSpinner();
	JLabel sample = new JLabel();
	ChartColorIcon icon = null;

	boolean isInit = false;
	/**
	 * 构建一个填充颜色编辑对话框
	 * 
	 * @param owner 父窗口
	 */
	public ChartColorDialog( Dialog owner ) {
		super( owner );
		this.setTitle( ChartMessage.get().getMessage( "ccd.setcolor" ) );
		this.setModal( true );
		this.setSize( 400, 280 );
		GM.setDialogDefaultButton( this, okbtn, cancelbtn );
		try {
			jbInit();
			this.addComponentListener(new ComponentListener(){
				public void componentResized(ComponentEvent e) {
					drawSample();
				}
				public void componentMoved(ComponentEvent e) {
				}
				public void componentShown(ComponentEvent e) {
				}
				public void componentHidden(ComponentEvent e) {
				}
			});
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	private boolean isEquals(Color c1, Color c2){
		if(c1==null || c2==null){
			return c1==c2;
		}
		return c1.equals(c2);
	}
	
	private void jbInit() throws Exception {
		this.getContentPane().setLayout( new BorderLayout());//xYLayout1 );
		fillType.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int type = (Integer)fillType.getSelectedItem();
				boolean en = type==Consts.PATTERN_DEFAULT;
				jianjin.setEnabled( en );
				labelAngle.setEnabled(en);
				angle.setEnabled(en);
				
				if(isInit) return;

				cc.setType(type);
				if(type!=Consts.PATTERN_DEFAULT){
					if(isEquals(cc.getColor1(),cc.getColor2())){
						color1.setSelectedItem(Color.WHITE.getRGB());
						color2.setSelectedItem(new Color(128,128,128).getRGB());
					}
					jianjin.setSelected(false);
				}
				
				drawSample();
			}
		});
		jianjin.setFont( new java.awt.Font( "Dialog", 0, 12 ) );
		jianjin.setText( ChartMessage.get().getMessage( "ccd.usegradient" ) );  //"使用渐近色" );
		jianjin.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				jianjin_actionPerformed( e );
			}
		} );
		okbtn.setFont( new java.awt.Font( "Dialog", 0, 12 ) );
		okbtn.setMargin( new Insets( 2, 5, 2, 5 ) );
		okbtn.setText( ChartMessage.get().getMessage( "button.ok" ) );  //"确定(O)" );
		okbtn.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				okbtn_actionPerformed( e );
			}
		} );
		cancelbtn.setText( ChartMessage.get().getMessage( "button.cancel" ) );  //"取消(C)" );
		cancelbtn.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				cancelbtn_actionPerformed( e );
			}
		} );
		cancelbtn.setFont( new java.awt.Font( "Dialog", 0, 12 ) );
		cancelbtn.setMargin( new Insets( 2, 5, 2, 5 ) );
		labelC1.setFont( new java.awt.Font( "Dialog", 0, 12 ) );
		labelC1.setText( ChartMessage.get().getMessage( "ccd.color1" ) );  //"颜色一" );
		labelC2.setText( ChartMessage.get().getMessage( "ccd.color2" ) );  //"颜色二" );
		labelC2.setFont( new java.awt.Font( "Dialog", 0, 12 ) );
		labelAngle.setFont( new java.awt.Font( "Dialog", 0, 12 ) );
		labelAngle.setToolTipText( "" );
		labelAngle.setText( ChartMessage.get().getMessage( "ccd.angle" ) ); //"渐近角度" );
		angle.addChangeListener( new javax.swing.event.ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				angle_stateChanged( e );
			}
		} );
		color1.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				color1_actionPerformed( e );
			}
		} );
		color2.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				color2_actionPerformed( e );
			}
		} );
		JPanel panelCenter = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = GM.getGBC(1, 1);
		labelFillType.setText(ChartMessage.get().getMessage( "ccd.fillType" ));
		panelCenter.add(labelFillType, gbc);
		gbc = GM.getGBC(1, 2,true);
		gbc.gridwidth = 3;
		panelCenter.add(fillType, gbc);
		
		gbc = GM.getGBC(2, 1,true);
		gbc.gridwidth = 2;
		panelCenter.add(jianjin, gbc);
		
		panelCenter.add(labelAngle,GM.getGBC(2, 3));
		panelCenter.add(angle,GM.getGBC(2,4,true));

		panelCenter.add( labelC1, GM.getGBC(3, 1) );
		panelCenter.add( color1, GM.getGBC(3, 2,true) );
		panelCenter.add( labelC2, GM.getGBC(3, 3) );
		panelCenter.add( color2, GM.getGBC(3, 4,true) );

		icon = new ChartColorIcon( cc );
		sample.setIcon( icon );
		gbc = GM.getGBC(4, 1,true,true);
		gbc.gridwidth = 4;
		panelCenter.add( sample, gbc );
		
		this.getContentPane().add( panelCenter,BorderLayout.CENTER );
		
		JPanel panelButton = new JPanel(new VFlowLayout());
		panelButton.add(okbtn);
		panelButton.add(cancelbtn);
		
		this.getContentPane().add( panelButton,BorderLayout.EAST );
		
		okbtn.setMnemonic( 'o' );
		cancelbtn.setMnemonic( 'c' );
		SpinnerNumberModel smodel = new SpinnerNumberModel( 1, -359, 359, 1 );
		angle.setModel( smodel );
	}

	private Integer getIntColor(Color c){
		if(c==null){
			return ChartColor.transparentColor;
		}else{
			return new Integer(c.getRGB());
		}
	}
	
	/**
	 * 设置填充颜色对象
	 * @param c 填充颜色
	 */
	public void setChartColor( ChartColor c ) {
		if ( c == null ) {
			this.cc = new ChartColor();
		}
		else this.cc = c.deepClone();
		isInit = true;
		fillType.setSelectedItem(cc.getType());
		
		jianjin.setSelected(cc.isGradient());
		angle.setValue( new Integer( cc.getAngle() ) );
		Color c1 = cc.getColor1();
		color1.setSelectedItem( getIntColor( c1 ) );
		Color c2 = cc.getColor2();
		color2.setSelectedItem( getIntColor(c2 ) );
		drawSample();
		isInit = false;
		fillType.repaint(100);
	}

	/**
	 * 返回窗口用户选项
	 * @return 窗口选项
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 返回编辑好的填充颜色
	 * @return 填充颜色
	 */
	public ChartColor getChartColor() {
		return cc;
	}

	void okbtn_actionPerformed( ActionEvent e ) {
		cc.setGradient( jianjin.isSelected() );
		cc.setColor1( color1.getColor().intValue() );
		cc.setColor2( color2.getColor().intValue() );
		cc.setAngle( ( ( Integer ) angle.getValue() ).intValue() );
		m_option = JOptionPane.OK_OPTION;
		dispose();
	}

	void cancelbtn_actionPerformed( ActionEvent e ) {
		m_option = JOptionPane.CANCEL_OPTION;
		dispose();
	}

	private void drawSample() {
		boolean  en = (jianjin.isSelected() || cc.getType()!=Consts.PATTERN_DEFAULT);
		labelC2.setEnabled( en );
		color2.setEnabled( en );

		icon.setChartColor( cc );
		icon.setSize(sample.getWidth(), sample.getHeight());
		sample.repaint();
	}

	void jianjin_actionPerformed( ActionEvent e ) {
		cc.setGradient( jianjin.isSelected() );
		
		Integer c1 = color1.getColor();
		if(jianjin.isSelected() && c1.equals(ColorComboBox.transparentColor)){
			color1.setSelectedIndex(0);
		}
		Integer c2 = color2.getColor();
		if(jianjin.isSelected() && c2.equals(ColorComboBox.transparentColor)){
			color2.setSelectedIndex(0);
		}

		cc.setColor1( color1.getColor().intValue() );
		cc.setColor2( color2.getColor().intValue() );
		cc.setAngle( ( ( Integer ) angle.getValue() ).intValue() );
		drawSample();
	}

	void angle_stateChanged( ChangeEvent e ) {
		cc.setAngle( ( ( Integer ) angle.getValue() ).intValue() );
		drawSample();
	}

	void color1_actionPerformed( ActionEvent e ) {
		Integer c1 = color1.getColor();
		if(c1==ColorComboBox.transparentColor){
			jianjin.setSelected(false);
		}
		cc.setColor1( color1.getColor().intValue() );
		drawSample();
	}

	void color2_actionPerformed( ActionEvent e ) {
		Integer c2 = color2.getColor();
		if(c2==ColorComboBox.transparentColor){
			jianjin.setSelected(false);
		}
		cc.setColor2( color2.getColor().intValue() );
		drawSample();
	}

}
