package com.raqsoft.ide.dfx.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.raqsoft.common.MessageManager;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.dfx.resources.IdeDfxMessage;

/**
 * 集算器技术支持界面
 * 
 */
public class DialogProcSurport extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * 集算器语言资源管理器
	 */
	private MessageManager mm = IdeDfxMessage.get();
	/**
	 * 关闭按钮
	 */
	private JButton jBClose = new JButton();

	/**
	 * 构造函数
	 */
	public DialogProcSurport() {
		super(GV.appFrame, "技术支持", true);
		init();
		setSize(GC.LANGUAGE == GC.ASIAN_CHINESE ? 425 : 450, 340);
		GM.setDialogDefaultButton(this, jBClose, jBClose);
		resetLangText();
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		setTitle(mm.getMessage("dialogprocsurport.title"));
		jBClose.setText(mm.getMessage("button.close"));
	}

	/**
	 * 关闭窗口
	 */
	private void closeWindow() {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 初始化控件
	 */
	private void init() {
		final String QQ = "800025723";
		final String TEL = "010-51292230";
		final String LABEL_UNCOPIED = TEL + "  ("
				+ mm.getMessage("dialogprocsurport.clickcopy") + ")";
		final String LABEL_COPIED = TEL + "  ("
				+ mm.getMessage("dialogprocsurport.copied") + ")";
		jBClose.setMnemonic('C');
		jBClose.setText("关闭(C)");
		jBClose.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				closeWindow();
			}
		});
		JPanel panelSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		this.getContentPane().add(panelSouth, BorderLayout.SOUTH);
		panelSouth.add(jBClose);

		JLabel textQQ = new JLabel();
		JLabel textSupportQQ = new JLabel();
		final JLabel textTel = new JLabel();
		JLabel textEmail = new JLabel();
		final JLabel textBBS = new JLabel();
		final JLabel textDemo = new JLabel();

		final String LABEL_QQ = mm.getMessage("dialogprocsurport.clickqq");
		final String LABEL_SUPPORT_QQ = mm
				.getMessage("dialogprocsurport.clicksupqq");
		final String LABEL_MAIL = mm.getMessage("dialogprocsurport.clickmail");

		textQQ.setText(QQ + "  (" + LABEL_QQ + ")");
		textSupportQQ.setText("18693267" + "  (" + LABEL_SUPPORT_QQ + ")");
		textTel.setText(LABEL_UNCOPIED);
		textEmail.setText("contact@raqsoft.com" + "  (" + LABEL_MAIL + ")");
		textBBS.setText("http://bbs.raqsoft.com.cn/forum.php");
		textDemo.setText("http://esproc.raqsoft.com.cn/");
		final Color NOTE_COLOR = Color.BLUE;
		textQQ.setForeground(NOTE_COLOR);
		textSupportQQ.setForeground(NOTE_COLOR);
		textTel.setForeground(NOTE_COLOR);
		textEmail.setForeground(NOTE_COLOR);
		textBBS.setForeground(NOTE_COLOR);
		textDemo.setForeground(NOTE_COLOR);

		JPanel panelCenter = new JPanel(new BorderLayout());
		JPanel panelInfo = new JPanel(new GridBagLayout());
		JLabel labelImage = new JLabel(GM.getImageIcon(GC.IMAGES_PATH
				+ "contact.png"));
		JPanel panelImage = new JPanel(new BorderLayout());
		panelImage.add(labelImage, BorderLayout.CENTER);
		panelCenter.add(panelImage, BorderLayout.CENTER);
		panelCenter.add(panelInfo, BorderLayout.SOUTH);

		final String LABEL_TEL = mm.getMessage("dialogprocsurport.tel");
		final String LABEL_EMAIL = mm.getMessage("dialogprocsurport.email");
		final String LABEL_BBS = mm.getMessage("dialogprocsurport.bbs");
		final String LABEL_DEMO = mm.getMessage("dialogprocsurport.demo");

		JLabel labelQQ = new JLabel(mm.getMessage("dialogprocsurport.qq"));
		JLabel labelSupportQQ = new JLabel(
				mm.getMessage("dialogprocsurport.supportqq"));
		JLabel labelTel = new JLabel(LABEL_TEL);
		JLabel labelEmail = new JLabel(LABEL_EMAIL);
		JLabel labelBBS = new JLabel(LABEL_BBS);
		JLabel labelDemo = new JLabel(LABEL_DEMO);

		textTel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				GM.clipBoard(TEL.replaceAll("-", ""));
				textTel.setText(LABEL_COPIED);

			}
		});
		textEmail.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				try {
					com.raqsoft.ide.common.GM
							.browse("mailto:contact@raqsoft.com");
				} catch (Exception e1) {
					GM.showException(e1);
				}
			}
		});
		textBBS.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				try {
					com.raqsoft.ide.common.GM.browse(textBBS.getText());
				} catch (Exception e1) {
					GM.showException(e1);
				}
			}
		});
		textDemo.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				try {
					com.raqsoft.ide.common.GM.browse(textDemo.getText());
				} catch (Exception e1) {
					GM.showException(e1);
				}
			}
		});

		textQQ.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				try {
					com.raqsoft.ide.common.GM.browse("tencent://message/?uin="
							+ QQ);
				} catch (Exception e1) {
					GM.showException(e1);
				}
			}
		});
		textSupportQQ.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				try {
					com.raqsoft.ide.common.GM
							.browse("https://jq.qq.com/?_wv=1027&k=554M75H");
				} catch (Exception e1) {
					GM.showException(e1);
				}
			}
		});
		panelInfo.add(labelQQ, GM.getGBC(0, 0));
		panelInfo.add(textQQ, GM.getGBC(0, 1));
		panelInfo.add(labelSupportQQ, GM.getGBC(1, 0));
		panelInfo.add(textSupportQQ, GM.getGBC(1, 1));
		panelInfo.add(labelTel, GM.getGBC(2, 0));
		panelInfo.add(textTel, GM.getGBC(2, 1));
		panelInfo.add(labelEmail, GM.getGBC(3, 0));
		panelInfo.add(textEmail, GM.getGBC(3, 1));
		panelInfo.add(labelBBS, GM.getGBC(4, 0));
		panelInfo.add(textBBS, GM.getGBC(4, 1));
		panelInfo.add(labelDemo, GM.getGBC(5, 0));
		panelInfo.add(textDemo, GM.getGBC(5, 1));
		panelInfo.add(new JLabel(), GM.getGBC(5, 2, true));

		labelQQ.setForeground(SystemColor.textHighlight);
		labelSupportQQ.setForeground(SystemColor.textHighlight);
		labelTel.setForeground(SystemColor.textHighlight);
		labelEmail.setForeground(SystemColor.textHighlight);
		labelBBS.setForeground(SystemColor.textHighlight);
		labelDemo.setForeground(SystemColor.textHighlight);

		this.getContentPane().add(panelCenter, BorderLayout.CENTER);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeWindow();
			}
		});

	}

}
