package com.scudata.ide.vdb.panel;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.scudata.common.ImageUtils;
import com.scudata.common.Logger;
import com.scudata.ide.common.EditListener;
import com.scudata.ide.vdb.control.VDBTreeNode;

public class PanelImage extends PanelEditor{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JLabel image=null;

	
	public PanelImage(EditListener listener){
		super(listener);
		setLayout(new BorderLayout());
		image = new JLabel();
		add(image,BorderLayout.CENTER);
		init();
	}
	
	void init(){
//		rstaHtml.addKeyListener(keyListener);
	}
	
	void setImageBytes(byte[] bytes){
		beforeInit();
		ImageIcon icon = new ImageIcon(bytes);
		image.setIcon(icon);
		afterInit();
	}
	
	byte[] getImageBytes(){
		ImageIcon ii = (ImageIcon)image.getIcon();
		int w = ii.getIconWidth();
		int h = ii.getIconHeight();
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bi.getGraphics();
		g.drawImage(ii.getImage(), 0, 0, null);
		
		byte[] bytes=null;
		try {
			bytes = ImageUtils.writePNG( bi );
		} catch (IOException e) {
			Logger.severe(e);
		}
		return bytes;
	}

	public void setNode(VDBTreeNode node) {
		this.node = node;
		byte[] bytes = (byte[])node.getData();
		setImageBytes( bytes );
	}

	public VDBTreeNode getNode(){
		byte[] bytes = getImageBytes();
		node.saveData(bytes);
		return node;
	}

}