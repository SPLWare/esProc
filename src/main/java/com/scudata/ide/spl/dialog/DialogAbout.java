package com.scudata.ide.spl.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import com.scudata.common.MessageManager;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.dialog.DialogInputText;
import com.scudata.ide.common.dialog.DialogResourceSearch;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.FreeConstraints;
import com.scudata.ide.common.swing.FreeLayout;

/**
 * 关于对话框
 *
 */
public class DialogAbout extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * Logo控件
	 */
	private JLabel jLabelLogo;
	/**
	 * 关闭按钮
	 */
	private JButton jBClose = new JButton();
	/**
	 * 上部面板
	 */
	private JPanel panelTop = new JPanel();
	/**
	 * 发布时间
	 */
	private JLabel jLReleaseDate = new JLabel();
	/**
	 * 公司名
	 */
	private JLabel jLCompanyName = new JLabel();
	/**
	 * 网址
	 */
	private JLabel jLWebsite = new JLabel();
	/**
	 * 电话
	 */
	private JLabel jLTel = new JLabel();

	/**
	 * 网址
	 */
	private JLabel jLbHttp = new JLabel();
	/**
	 * 网址2
	 */
	private JLabel jLbHttp2 = new JLabel();

	/**
	 * 电话文本框
	 */
	private JTextField jTFTele = new JTextField() {
		private static final long serialVersionUID = 1L;

		public Border getBorder() {
			return null;
		}
	};
	/**
	 * 公司名称
	 */
	private JLabel jLbName = new JLabel();

	/**
	 * 公司名称2
	 */
	private JLabel jLbName2 = new JLabel();
	/**
	 * 产品名
	 */
	private JLabel jLProductName = new JLabel();
	/**
	 * JDK按钮
	 */
	private JButton jBJDK = new JButton();

	/**
	 * 产品名称
	 */
	private String productName = null;

	/**
	 * 主面板
	 */
	private JFrame parent = null;

	/**
	 * 构造函数
	 * 
	 */
	public DialogAbout() {
		this(GV.appFrame, GV.appFrame.getProductName());
	}

	public DialogAbout(JFrame frame, String productName) {
		super(frame, "", true);
		this.parent = frame;
		this.productName = productName;
		this.setTitle(mm.getMessage("dialogabout.title") + productName);
		try {
			jbInit();
			pack();
			resetLangText();
			GM.setDialogDefaultButton(this, jBClose, jBClose);
		} catch (Exception ex) {
			GM.showException(ex);
		}
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
	private void setText(String sDefault, String sText, JLabel lbTitle, Object tfText) {
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
	 * 取发布时间
	 * 
	 * @return
	 */
	private static String getReleaseDate() {
		return "2022-01-06";
	}

	/**
	 * 加载信息
	 */
	private void loadMessage() {
		jLProductName.setText(mm.getMessage("dialogabout.productname") + "      " + productName);
		jLReleaseDate.setText(mm.getMessage("dialogabout.label1", getReleaseDate()));

		String tmp = mm.getMessage("dialogabout.providername");// 公司名称
		String vendorName = mm.getMessage("dialogabout.defvendor");
		setText(tmp, vendorName, jLCompanyName, jLbName);
		tmp = mm.getMessage("dialogabout.providerhttp");// 公司网址
		String vendorURL = mm.getMessage("dialogabout.defvendorurl");
		setText(tmp, vendorURL, jLWebsite, jLbHttp);
		tmp = mm.getMessage("dialogabout.providertel");// 公司电话
		String vendorTel = "010-51295366";
		setText(tmp, vendorTel, jLTel, jTFTele);

		jLbName2.setText(mm.getMessage("dialogabout.defvendor1"));
		jLbHttp2.setText(mm.getMessage("dialogabout.defvendorurl1"));
	}

	/**
	 * 摆放Logo
	 */
	private void placeLogo() {
		panelTop.add(jLabelLogo, new FreeConstraints(10, 0, 128, 128));
		panelTop.add(jLProductName, new FreeConstraints(163, 15, -1, -1));
		panelTop.add(jLReleaseDate, new FreeConstraints(163, 45, -1, -1));
	}

	/**
	 * 摆放长Logo
	 */
	private void placeLongLogo() {
		panelTop.add(jLabelLogo, new FreeConstraints(10, 2, 380, 50));
		panelTop.add(jLProductName, new FreeConstraints(10, 60, -1, -1));
		panelTop.add(jLReleaseDate, new FreeConstraints(10, 90, 209, -1));
	}

	/**
	 * 调用时可以重写此方法来替换Logo
	 * 
	 * @return
	 */
	protected ImageIcon getLogoImageIcon() {
		return GM.getLogoImage(false);
	}

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
				image = image.getScaledInstance(380, (int) (380.0 * h / w), Image.SCALE_SMOOTH);
			} else {
				if (w > h) {
					image = image.getScaledInstance(128, (int) (128 * (h * 1.0 / w)), Image.SCALE_SMOOTH);
				} else {
					image = image.getScaledInstance((int) (128 * (w * 1.0 / h)), 128, Image.SCALE_SMOOTH);
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
		jLbHttp.setFont(new java.awt.Font("Comic Sans MS", 2, 13));
		jLbHttp.setForeground(Color.blue);
		jLbHttp.setBorder(null);

		jLbHttp2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jLbHttp2.setFont(new java.awt.Font("Comic Sans MS", 2, 13));
		jLbHttp2.setForeground(Color.blue);
		jLbHttp2.setBorder(null);
		jLbHttp2.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int b = e.getButton();
				if (b != MouseEvent.BUTTON1) {
					return;
				}
				if (GM.getOperationSytem() == GC.OS_WINDOWS) {
					try {
						Runtime.getRuntime().exec("cmd /C start " + jLbHttp2.getText());
					} catch (Exception x) {
						GM.showException(x);
					}
				}
			}
		});

		jTFTele.setDisabledTextColor(Color.black);
		jTFTele.setEditable(false);
		Color telBg = new Color(jLbHttp.getBackground().getRGB());
		jTFTele.setBackground(telBg);

		jLbHttp.addMouseListener(new DialogAbout_jLbHttp_mouseAdapter(this));
		jTFTele.setHorizontalAlignment(SwingConstants.LEFT);
		jTFTele.addMouseListener(new DialogAbout_jTFTele_mouseAdapter(this));
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
		final int DIFF = 26;
		boolean isCN = GM.isChineseLanguage();
		panelTop.add(jLabelLogo, new FreeConstraints(0, 0, 145, 123));
		panelTop.add(jLProductName, new FreeConstraints(163, 15, -1, -1));
		int bottomY = 148;
		final int BOTTOM_X1 = 14;
		final int BOTTOM_X2 = 95;
		final int BOTTOM_L1 = 69;
		final int BOTTOM_L2 = 288;
		panelTop.add(jLCompanyName, new FreeConstraints(BOTTOM_X1, bottomY, BOTTOM_L1, -1));
		panelTop.add(jLbName, new FreeConstraints(BOTTOM_X2, bottomY, BOTTOM_L2, -1));
		if (isCN) {
			bottomY += DIFF;
			panelTop.add(jLbName2, new FreeConstraints(BOTTOM_X2, bottomY, BOTTOM_L2, -1));
		}
		bottomY += DIFF;
		panelTop.add(jLWebsite, new FreeConstraints(BOTTOM_X1, bottomY, BOTTOM_L1, -1));
		panelTop.add(jLbHttp, new FreeConstraints(BOTTOM_X2, bottomY, BOTTOM_L2, -1));
		if (isCN) {
			bottomY += DIFF;
			panelTop.add(jLbHttp2, new FreeConstraints(BOTTOM_X2, bottomY, BOTTOM_L2, -1));
		}
		bottomY += DIFF;
		panelTop.add(jLTel, new FreeConstraints(BOTTOM_X1, bottomY, -1, -1));
		panelTop.add(jTFTele, new FreeConstraints(BOTTOM_X2, bottomY, BOTTOM_L2, -1));
		JPanel jPanel2 = new JPanel();
		jPanel2.setLayout(new FlowLayout(FlowLayout.RIGHT));
		jPanel2.add(jBJDK);
		jPanel2.add(jBClose);
		this.getContentPane().add(jPanel2, BorderLayout.SOUTH);

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
					DialogResourceSearch drs = new DialogResourceSearch(parent);
					drs.setVisible(true);
				}
			}
		};
		jBClose.addKeyListener(searchResource);
		this.getContentPane().addKeyListener(searchResource);
	}

	/**
	 * 关闭按钮命令
	 * 
	 * @param e
	 */
	void jBClose_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		dispose();
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
		if (GM.getOperationSytem() == GC.OS_WINDOWS) {
			try {
				Runtime.getRuntime().exec("cmd /C start " + jLbHttp.getText());
			} catch (Exception x) {
				GM.showException(x);
			}
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
		String sele = jTFTele.getSelectedText();
		GM.clipBoard(sele);
		jTFTele.setSelectionEnd(0);
	}

	/**
	 * JDK按钮命令
	 * 
	 * @param e
	 */
	void jBJDK_actionPerformed(ActionEvent e) {
		DialogInputText dit = new DialogInputText(parent, false);
		Properties p = System.getProperties();
		String buf = p.toString();
		buf = Sentence.replace(buf, ",", "\r\n", 0);
		dit.setText(buf);
		dit.setVisible(true);
	}

	/**
	 * 窗口关闭
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

}

class DialogAbout_jBClose_actionAdapter implements java.awt.event.ActionListener {
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
