package com.scudata.ide.spl.dialog;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JWindow;

import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.swing.FreeLayout;

/**
 * 集算器显示启动图片窗口
 * 
 */
public class DialogSplash extends JWindow {
	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 * 
	 * @param splashImage splash图片路径
	 */
	public DialogSplash(String splashImage) {
		try {
			// this.setUndecorated(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // 设置光标
			this.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
			this.getRootPane().setBorder(null);
			this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
			// this.setResizable(false);
			initUI(splashImage);
			GM.centerWindow(this);
			// this.setModal(false);
		} catch (Exception e) {
			GM.writeLog(e);
		}
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI(String splashImage) throws Exception {
		ImageIcon ii = getImageIcon(splashImage);
		Image image = ii.getImage();
		panelImage = new ImagePanel(image);
		panelImage.setOpaque(false);
		panelImage.setLayout(new FreeLayout());
		getContentPane().add(panelImage);
	}

	/**
	 * 关闭窗口
	 */
	public void closeWindow() {
		dispose();
	}

	/**
	 * 取图片对象
	 * 
	 * @return
	 */
	private ImageIcon getImageIcon(String splashImage) {
		ImageIcon ii = null;
		if (StringUtils.isValidString(splashImage)) {
			String path = GM.getAbsolutePath(splashImage);
			File f = new File(path);
			if (f.exists()) {
				ii = new ImageIcon(path);
			} else {
				ii = GM.getImageIcon(splashImage);
			}
		}
		if (ii == null) {
			String imgPath = "/com/scudata/ide/common/resources/"
					+ getDefaultImageName() + GM.getLanguageSuffix() + ".png";
			ii = GM.getImageIcon(imgPath);
		}
		return ii;
	}

	/**
	 * 默认的图标文件名
	 * @return
	 */
	protected String getDefaultImageName() {
		return "esproc";
	}

	/**
	 * 显示图片的面板
	 *
	 */
	class ImagePanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private Image image = null;

		public ImagePanel(Image image) {
			this.image = image;
		}

		public void paint(Graphics g) {
			g.drawImage(image, 0, 0, null);
			super.paint(g);
		}
	}

	protected ImagePanel panelImage;

	public static final int WINDOW_WIDTH = 600;
	public static final int WINDOW_HEIGHT = 370;
}
