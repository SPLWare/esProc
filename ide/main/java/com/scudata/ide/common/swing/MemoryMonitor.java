package com.scudata.ide.common.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * Tracks Memory allocated & used, displayed in graph form.
 */
public class MemoryMonitor extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();
	/**
	 * Output date stamp
	 */
	private static JCheckBox dateStampCB = new JCheckBox(IdeCommonMessage.get()
			.getMessage("memorymonitor.outputdate"));
	/**
	 * 显示面板
	 */
	public Surface surf;

	/**
	 * 构造函数
	 */
	public MemoryMonitor() {
		setLayout(new BorderLayout());
		setBorder(new TitledBorder(new EtchedBorder(),
				mm.getMessage("memorymonitor.mm")));
		add(surf = new Surface());
		final JPanel controls = new JPanel();
		controls.setPreferredSize(new Dimension(135, 80));
		Font font = new Font("Dialog", Font.PLAIN, 12);
		JLabel label = new JLabel(mm.getMessage("memorymonitor.samplerate"));
		label.setFont(font);
		label.setForeground(Color.black);
		controls.add(label);
		final JTextField tf = new JTextField("1000");
		tf.setPreferredSize(new Dimension(45, 20));
		controls.add(tf);
		controls.add(label = new JLabel(mm.getMessage("memorymonitor.ms"))); // ms
		label.setFont(font);
		label.setForeground(Color.black);
		controls.add(dateStampCB);
		dateStampCB.setFont(font);
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				removeAll();
				try {
					surf.sleepAmount = Long.parseLong(tf.getText().trim());
				} catch (Exception ex) {
				}
				surf.start();
				add(surf);
				validate();
				repaint();
			}
		});
	}

	/**
	 * 显示面板
	 *
	 */
	public class Surface extends JPanel implements Runnable {
		private static final long serialVersionUID = 1L;
		public Thread thread;
		public long sleepAmount = 1000;
		private int w, h;
		private BufferedImage bimg;
		private Graphics2D big;
		private Font font = new Font("Dialog", Font.PLAIN, 10);
		private Runtime r = Runtime.getRuntime();
		private int columnInc;
		private int pts[];
		private int ptNum;
		private int ascent, descent;
		private Rectangle graphOutlineRect = new Rectangle();
		private Rectangle2D mfRect = new Rectangle2D.Float();
		private Rectangle2D muRect = new Rectangle2D.Float();
		private Line2D graphLine = new Line2D.Float();
		private Color graphColor = new Color(46, 139, 87);
		private Color mfColor = new Color(0, 100, 0);
		private String usedStr;

		/**
		 * 构造函数
		 */
		public Surface() {
			setBackground(Color.black);
			addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (thread == null) {
						start();
					} else {
						stop();
					}
				}
			});
		}

		/**
		 * 取最小尺寸
		 */
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		/**
		 * 取最大尺寸
		 */
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		/**
		 * 取尺寸
		 */
		public Dimension getPreferredSize() {
			return new Dimension(135, 80);
		}

		/**
		 * 绘制
		 */
		public void paint(Graphics g) {
			if (big == null) {
				return;
			}

			big.setBackground(getBackground());
			big.clearRect(0, 0, w, h);

			float freeMemory = (float) r.freeMemory();
			float totalMemory = (float) r.totalMemory();

			// .. Draw allocated and used strings ..
			big.setColor(Color.green);
			big.drawString(
					String.valueOf((int) totalMemory / 1024) + "K "
							+ mm.getMessage("memorymonitor.allocated"), 4.0f,
					(float) ascent + 0.5f); // allocated
			usedStr = String.valueOf(((int) (totalMemory - freeMemory)) / 1024)
					+ "K " + mm.getMessage("memorymonitor.used");
			big.drawString(usedStr, 4, h - descent);

			// Calculate remaining size
			float ssH = ascent + descent;
			float remainingHeight = (float) (h - (ssH * 2) - 0.5f);
			float blockHeight = remainingHeight / 10;
			float blockWidth = 20.0f;

			// .. Memory Free ..
			big.setColor(mfColor);
			int MemUsage = (int) ((freeMemory / totalMemory) * 10);
			int i = 0;
			for (; i < MemUsage; i++) {
				mfRect.setRect(5, (float) ssH + i * blockHeight, blockWidth,
						(float) blockHeight - 1);
				big.fill(mfRect);
			}

			// .. Memory Used ..
			big.setColor(Color.green);
			for (; i < 10; i++) {
				muRect.setRect(5, (float) ssH + i * blockHeight, blockWidth,
						(float) blockHeight - 1);
				big.fill(muRect);
			}

			// .. Draw History Graph ..
			big.setColor(graphColor);
			int graphX = 30;
			int graphY = (int) ssH;
			int graphW = w - graphX - 5;
			int graphH = (int) remainingHeight;
			graphOutlineRect.setRect(graphX, graphY, graphW, graphH);
			big.draw(graphOutlineRect);

			int graphRow = graphH / 10;

			// .. Draw row ..
			for (int j = graphY; j <= graphH + graphY; j += graphRow) {
				graphLine.setLine(graphX, j, graphX + graphW, j);
				big.draw(graphLine);
			}

			// .. Draw animated column movement ..
			int graphColumn = graphW / 15;

			if (columnInc == 0) {
				columnInc = graphColumn;
			}

			for (int j = graphX + columnInc; j < graphW + graphX; j += graphColumn) {
				graphLine.setLine(j, graphY, j, graphY + graphH);
				big.draw(graphLine);
			}

			--columnInc;

			if (pts == null) {
				pts = new int[graphW];
				ptNum = 0;
			} else if (pts.length != graphW) {
				int tmp[] = null;
				if (ptNum < graphW) {
					tmp = new int[ptNum];
					System.arraycopy(pts, 0, tmp, 0, tmp.length);
				} else {
					tmp = new int[graphW];
					System.arraycopy(pts, pts.length - tmp.length, tmp, 0,
							tmp.length);
					ptNum = tmp.length - 2;
				}
				pts = new int[graphW];
				System.arraycopy(tmp, 0, pts, 0, tmp.length);
			} else {
				big.setColor(Color.yellow);
				pts[ptNum] = (int) (graphY + graphH
						* (freeMemory / totalMemory));
				for (int j = graphX + graphW - ptNum, k = 0; k < ptNum; k++, j++) {
					if (k != 0) {
						if (pts[k] != pts[k - 1]) {
							big.drawLine(j - 1, pts[k - 1], j, pts[k]);
						} else {
							big.fillRect(j, pts[k], 1, 1);
						}
					}
				}
				if (ptNum + 2 == pts.length) {
					// throw out oldest point
					for (int j = 1; j < ptNum; j++) {
						pts[j - 1] = pts[j];
					}
					--ptNum;
				} else {
					ptNum++;
				}
			}
			g.drawImage(bimg, 0, 0, this);
		}

		/**
		 * 启动线程
		 */
		public void start() {
			thread = new Thread(this);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.setName("MemoryMonitor");
			thread.start();
		}

		/**
		 * 结束线程
		 */
		public synchronized void stop() {
			thread = null;
			notify();
		}

		/**
		 * 执行线程
		 */
		public void run() {

			Thread me = Thread.currentThread();

			while (thread == me && !isShowing() || getSize().width == 0) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					return;
				}
			}

			while (thread == me && isShowing()) {
				Dimension d = getSize();
				if (d.width != w || d.height != h) {
					w = d.width;
					h = d.height;
					bimg = (BufferedImage) createImage(w, h);
					big = bimg.createGraphics();
					big.setFont(font);
					FontMetrics fm = big.getFontMetrics(font);
					ascent = (int) fm.getAscent();
					descent = (int) fm.getDescent();
				}
				repaint();
				try {
					Thread.sleep(sleepAmount);
				} catch (InterruptedException e) {
					break;
				}
				if (MemoryMonitor.dateStampCB.isSelected()) {
					Logger.debug(new Date().toString() + " " + usedStr);
				}
			}
			thread = null;
		}
	}

}
