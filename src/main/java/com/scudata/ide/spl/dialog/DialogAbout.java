package com.scudata.ide.spl.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.scudata.common.MessageManager;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.dialog.DialogInputText;
import com.scudata.ide.common.dialog.DialogResourceSearch;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.FreeConstraints;
import com.scudata.ide.common.swing.FreeLayout;
import com.scudata.ide.spl.GMSpl;

/**
 * 关于对话框
 *
 */
public class DialogAbout extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	protected MessageManager mm = IdeCommonMessage.get();

	/**
	 * Logo控件
	 */
	protected JLabel jLabelLogo;
	/**
	 * 关闭按钮
	 */
	protected JButton jBClose = new JButton();
	/**
	 * 上部面板
	 */
	protected JPanel panelTop = new JPanel();
	/**
	 * 发布时间
	 */
	protected JLabel jLReleaseDate1 = new JLabel();
	/**
	 * 发布时间
	 */
	protected JLabel jLReleaseDate2 = new JLabel();
	/**
	 * 公司名
	 */
	protected JLabel jLCompanyName = new JLabel();
	/**
	 * 网址
	 */
	protected JLabel jLWebsite = new JLabel();
	/**
	 * 电话
	 */
	protected JLabel jLTel = new JLabel();

	/**
	 * 网址
	 */
	protected JLabel jLbHttp = new JLabel();
	/**
	 * 网址2
	 */
	protected JLabel jLbHttp2 = new JLabel();

	/**
	 * 电话文本框
	 */
	protected JLabel jLTel2 = new JLabel();
	/**
	 * 公司名称
	 */
	protected JLabel jLbName = new JLabel();

	/**
	 * 公司名称2
	 */
	protected JLabel jLbName2 = new JLabel();
	/**
	 * 产品名
	 */
	protected JLabel jLProductName1 = new JLabel();
	/**
	 * 产品名
	 */
	protected JLabel jLProductName2 = new JLabel();
	/**
	 * JDK按钮
	 */
	protected JButton jBJDK = new JButton();

	/**
	 * 产品名称
	 */
	protected String productName = null;

	/**
	 * 按钮面板
	 */
	protected JPanel jPButton = new JPanel();

	public static final int DIALOG_HEIGHT = GM.isChineseLanguage() ? 360 : 325;
	public static final int DIALOG_WIDTH = GM.isChineseLanguage() ? 445 : 500;
	public static final int ROW_HEIGHT = 25;

	/**
	 * 构造函数
	 * 
	 */
	public DialogAbout() {
		this(GV.appFrame, GV.appFrame.getProductName());
	}

	public DialogAbout(JFrame frame, String productName) {
		super(frame, "", true);
		this.productName = productName;
	}

	public void init() {
		try {
			this.setTitle(mm.getMessage("dialogabout.title") + productName);
			jbInit();
			resetLangText();
			setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
			GM.setDialogDefaultButton(this, jBClose, jBClose);
			this.setResizable(false);
		} catch (Exception ex) {
			GM.showException(this, ex);
		}
	}

	/**
	 * 调用时可以重写此方法来替换Logo
	 * 
	 * @return
	 */
	protected ImageIcon getLogoImageIcon() {
		return GM.getLogoImage(this, false);
	}

	/**
	 * 重写此方法可以替换发布日期
	 * 
	 * @return
	 */
	protected String getReleaseDate() {
		return GV.appFrame.getReleaseDate();
	}

	/**
	 * 窗口关闭
	 */
	protected void closeDialog() {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 按语言加载
	 */
	private void resetLangText() {
		jBClose.setText(mm.getMessage("button.close")); // 关闭(C)
		jBJDK.setText(mm.getMessage("dialogabout.jdk")); // JDK环境
	}

	/**
	 * 设置显示文本
	 * 
	 * @param sDefault
	 * @param sText
	 * @param lbTitle
	 * @param tfText
	 */
	private void setText(String sDefault, String sText, JLabel lbTitle,
			Object tfText) {
		if (!StringUtils.isValidString(sText)) {
			return;
		}
		int i = -1;
		i = sText.indexOf("："); // 必须用中文冒号作为分隔符分开，以和http://的英文冒号区分开

		if (i == -1) {
			lbTitle.setText(sDefault);
			if (tfText instanceof JLabel) {
				((JLabel) tfText).setText(sText);
			} else {
				((JTextField) tfText).setText(sText);
			}
		} else {
			lbTitle.setText(sText.substring(0, i + 1));
			if (tfText instanceof JLabel) {
				((JLabel) tfText).setText(sText.substring(i + 1));
			} else {
				((JTextField) tfText).setText(sText.substring(i + 1));
			}
		}
	}

	/**
	 * 加载信息
	 */
	protected void loadMessage() {
		jLProductName1.setText(mm.getMessage("dialogabout.productname"));
		jLReleaseDate1.setText(mm.getMessage("dialogabout.label1",
				getReleaseDate()));

		jLProductName2.setText(productName);
		jLReleaseDate2.setText(getReleaseDate());

		String tmp = mm.getMessage("dialogabout.providername");// 公司名称
		String vendorName = mm.getMessage("dialogabout.defvendor");
		setText(tmp, vendorName, jLCompanyName, jLbName);
		tmp = mm.getMessage("dialogabout.providerhttp");// 公司网址
		String vendorURL = mm.getMessage("dialogabout.defvendorurl1");
		setText(tmp, vendorURL, jLWebsite, jLbHttp);
		tmp = mm.getMessage("dialogabout.providertel");// 公司电话
		String vendorTel = "010-51295366";
		setText(tmp, vendorTel, jLTel, jLTel2);

		jLbName2.setText(mm.getMessage("dialogabout.defvendor1"));
		jLbHttp2.setText(mm.getMessage("dialogabout.defvendorurl"));
	}

	protected int getTopLabel1Width() {
		return GM.isChineseLanguage() ? 80 : 145;
	}

	protected int getBottomLabel1Width() {
		return GM.isChineseLanguage() ? 80 : 115;
	}

	public static final int GAP = 10;
	public static final int IMAGE_SIZE = 128;
	protected int x1, y1;

	/**
	 * 摆放Logo
	 */
	protected void placeLogo() {
		final int LABEL1_WIDTH = getTopLabel1Width();
		final int LABEL2_WIDTH = 200;
		panelTop.add(jLabelLogo, new FreeConstraints(GAP, 2, IMAGE_SIZE,
				IMAGE_SIZE));
		x1 = IMAGE_SIZE + GAP * 3;
		y1 = GAP;
		panelTop.add(jLProductName1, new FreeConstraints(x1, y1, LABEL1_WIDTH,
				-1));
		panelTop.add(jLProductName2, new FreeConstraints(x1 + LABEL1_WIDTH, y1,
				LABEL2_WIDTH, -1));
		y1 += ROW_HEIGHT;
		panelTop.add(jLReleaseDate1, new FreeConstraints(x1, y1, LABEL1_WIDTH,
				-1));
		panelTop.add(jLReleaseDate2, new FreeConstraints(x1 + LABEL1_WIDTH, y1,
				LABEL2_WIDTH, -1));
	}

	protected int placeCenter() {
		return 148;
	}

	/**
	 * 摆放长Logo
	 */
	protected void placeLongLogo() {
		final int LABEL1_WIDTH = getTopLabel1Width();
		final int LABEL2_WIDTH = 200;
		panelTop.add(jLabelLogo, new FreeConstraints(GAP, 2, DIALOG_WIDTH - GAP
				* 2, 50));
		x1 = GAP;
		y1 = 60;
		panelTop.add(jLProductName1, new FreeConstraints(x1, y1, LABEL1_WIDTH,
				-1));
		panelTop.add(jLProductName2, new FreeConstraints(x1 + LABEL1_WIDTH, y1,
				LABEL2_WIDTH, -1));
		y1 += ROW_HEIGHT;
		panelTop.add(jLReleaseDate1, new FreeConstraints(x1, y1, LABEL1_WIDTH,
				-1));
		panelTop.add(jLReleaseDate2, new FreeConstraints(x1 + LABEL1_WIDTH, y1,
				LABEL2_WIDTH, -1));
	}

	protected void placeBottom(int x1, int x2, int bottomY) {
	}

	protected final Font URL_FONT = new Font("Comic Sans MS", 2, 13);

	/**
	 * 初始化控件
	 * 
	 * @param product
	 * @throws Exception
	 */
	private void jbInit() throws Exception {
		panelTop.setLayout(new FreeLayout());
		ImageIcon icon = getLogoImageIcon();
		boolean isLongLogo = false;
		if (icon != null) {
			Image image = icon.getImage();
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			isLongLogo = w * 1.0 / h > 2;
			if (isLongLogo) {
				image = image.getScaledInstance(380, (int) (380.0 * h / w),
						Image.SCALE_SMOOTH);
			} else {
				if (w > h) {
					image = image.getScaledInstance(128,
							(int) (128 * (h * 1.0 / w)), Image.SCALE_SMOOTH);
				} else {
					image = image.getScaledInstance(
							(int) (128 * (w * 1.0 / h)), 128,
							Image.SCALE_SMOOTH);
				}
			}

			jLabelLogo = new JLabel(new ImageIcon(image));
		} else {
			jLabelLogo = new JLabel();
		}

		jBClose.setDoubleBuffered(false);
		jBClose.setMnemonic('C');
		jBClose.setText("关闭(C)");

		jBClose.addActionListener(new DialogAbout_jBClose_actionAdapter(this));
		jLWebsite.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jLWebsite.setForeground(SystemColor.textHighlight);
		jLbHttp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jLbHttp.setFont(URL_FONT);
		jLbHttp.setForeground(Color.blue);
		jLbHttp.setBorder(null);

		jLbHttp2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jLbHttp2.setFont(URL_FONT);
		jLbHttp2.setForeground(Color.blue);
		jLbHttp2.setBorder(null);
		jLbHttp2.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int b = e.getButton();
				if (b != MouseEvent.BUTTON1) {
					return;
				}
				try {
					GM.browse(jLbHttp2.getText());
				} catch (Exception x) {
					GM.showException(DialogAbout.this, x);
				}
			}
		});

		jLbHttp.addMouseListener(new DialogAbout_jLbHttp_mouseAdapter(this));
		jLTel2.setHorizontalAlignment(SwingConstants.LEFT);
		jLTel2.addMouseListener(new DialogAbout_jTFTele_mouseAdapter(this));
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setResizable(false);
		this.getContentPane().setLayout(new BorderLayout());
		this.addWindowListener(new DialogAbout_this_windowAdapter(this));
		jBJDK.setMnemonic('K');
		jBJDK.setText("JDK环境");
		jBJDK.addActionListener(new DialogAbout_jBJDK_actionAdapter(this));

		jLCompanyName.setForeground(SystemColor.textHighlight);
		jLTel.setForeground(SystemColor.textHighlight);
		getContentPane().add(panelTop, BorderLayout.CENTER);
		 boolean isCN = GM.isChineseLanguage();
		panelTop.add(jLabelLogo, new FreeConstraints(0, 0, 145, 123));
		// panelTop.add(jLProductName, new FreeConstraints(163, 15, -1, -1));
		int bottomY = placeCenter();
		final int BOTTOM_X1 = 14;
		final int BOTTOM_X2 = BOTTOM_X1 + getBottomLabel1Width();
		final int BOTTOM_L1 = 69;
		final int BOTTOM_L2 = 288;
		panelTop.add(jLCompanyName, new FreeConstraints(BOTTOM_X1, bottomY,
				BOTTOM_L1, -1));
		panelTop.add(jLbName, new FreeConstraints(BOTTOM_X2, bottomY,
				BOTTOM_L2, -1));
		 if (isCN) {
		bottomY += ROW_HEIGHT;
		panelTop.add(jLbName2, new FreeConstraints(BOTTOM_X2, bottomY,
				BOTTOM_L2, -1));
		 }
		bottomY += ROW_HEIGHT;
		panelTop.add(jLWebsite, new FreeConstraints(BOTTOM_X1, bottomY,
				BOTTOM_L1, -1));
		panelTop.add(jLbHttp, new FreeConstraints(BOTTOM_X2, bottomY,
				BOTTOM_L2, -1));
		// if (isCN) {
		bottomY += ROW_HEIGHT;
		panelTop.add(jLbHttp2, new FreeConstraints(BOTTOM_X2, bottomY,
				BOTTOM_L2, -1));
		// }
		if (isTelVisible()) {
			bottomY += ROW_HEIGHT;
			panelTop.add(jLTel, new FreeConstraints(BOTTOM_X1, bottomY,
					BOTTOM_L1, -1));
			panelTop.add(jLTel2, new FreeConstraints(BOTTOM_X2, bottomY,
					BOTTOM_L2, -1));
		}
		placeBottom(BOTTOM_X1, BOTTOM_X2, bottomY + ROW_HEIGHT);
		jPButton.setLayout(new FlowLayout(FlowLayout.RIGHT));
		jPButton.add(jBJDK);
		jPButton.add(jBClose);
		this.getContentPane().add(jPButton, BorderLayout.SOUTH);

		if (isLongLogo) {
			placeLongLogo();
		} else {
			placeLogo();
		}
		loadMessage();

		KeyListener searchResource = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				boolean isMacOS = GM.isMacOS();
				boolean isCmdKey = isMacOS ? e.isMetaDown() : e.isControlDown();
				if (isCmdKey && e.getKeyCode() == KeyEvent.VK_F) {
					DialogResourceSearch drs = new DialogResourceSearch(
							DialogAbout.this);
					drs.setVisible(true);
				}
			}
		};
		jBClose.addKeyListener(searchResource);
		this.getContentPane().addKeyListener(searchResource);
	}

	protected boolean isTelVisible() {
		return GMSpl.isChineseLanguage();
	}

	/**
	 * 关闭按钮命令
	 * 
	 * @param e
	 */
	void jBClose_actionPerformed(ActionEvent e) {
		closeDialog();
	}

	/**
	 * 鼠标点击HTTP
	 * 
	 * @param e
	 */
	void jLbHttp_mouseClicked(MouseEvent e) {
		int b = e.getButton();
		if (b != MouseEvent.BUTTON1) {
			return;
		}
		try {
			GM.browse(jLbHttp.getText());
		} catch (Exception x) {
			GM.showException(this, x);
		}
	}

	/**
	 * 鼠标点击电话号
	 * 
	 * @param e
	 */
	void jTFTele_mouseClicked(MouseEvent e) {
		int b = e.getButton();
		if (b != MouseEvent.BUTTON3) {
			return;
		}
		GM.clipBoard(jLTel2.getText());
	}

	/**
	 * JDK按钮命令
	 * 
	 * @param e
	 */
	void jBJDK_actionPerformed(ActionEvent e) {
		DialogInputText dit = new DialogInputText(this, false);
		Properties p = System.getProperties();
		String str = p.toString();
		try {
			str = Sentence.replace(str, ",", "\n", 0);
		} catch (Exception ex) {
			StringBuffer buf = new StringBuffer();
			Iterator it = p.keySet().iterator();
			while (it.hasNext()) {
				Object key = it.next();
				buf.append(key + "=" + p.getProperty((String) key));
				buf.append("\n");
			}
			str = buf.toString();
		}
		dit.setText(str);
		dit.setVisible(true);
	}

	/**
	 * 窗口关闭
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		closeDialog();
	}

}

class DialogAbout_jBClose_actionAdapter implements
		java.awt.event.ActionListener {
	DialogAbout adaptee;

	DialogAbout_jBClose_actionAdapter(DialogAbout adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBClose_actionPerformed(e);
	}
}

class DialogAbout_jLbHttp_mouseAdapter extends java.awt.event.MouseAdapter {
	DialogAbout adaptee;

	DialogAbout_jLbHttp_mouseAdapter(DialogAbout adaptee) {
		this.adaptee = adaptee;
	}

	public void mouseClicked(MouseEvent e) {
		adaptee.jLbHttp_mouseClicked(e);
	}
}

class DialogAbout_jTFTele_mouseAdapter extends java.awt.event.MouseAdapter {
	DialogAbout adaptee;

	DialogAbout_jTFTele_mouseAdapter(DialogAbout adaptee) {
		this.adaptee = adaptee;
	}

	public void mouseClicked(MouseEvent e) {
		adaptee.jTFTele_mouseClicked(e);
	}
}

class DialogAbout_jBJDK_actionAdapter implements java.awt.event.ActionListener {
	DialogAbout adaptee;

	DialogAbout_jBJDK_actionAdapter(DialogAbout adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBJDK_actionPerformed(e);
	}
}

class DialogAbout_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogAbout adaptee;

	DialogAbout_this_windowAdapter(DialogAbout adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}
