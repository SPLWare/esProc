package com.scudata.ide.spl.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.svg.SVGDocument;

import com.scudata.cellset.graph.config.GraphTypes;
import com.scudata.chart.Consts;
import com.scudata.chart.Engine;
import com.scudata.common.AnimatedGifEncoder;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.ChartParam;
import com.scudata.expression.Expression;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.ide.spl.chart.auto.DialogGraphEdit;
import com.scudata.ide.spl.chart.auto.JComboBoxGraph;
import com.scudata.ide.spl.resources.ChartMessage;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 将画布对象或者已经画好的图像信息 显示为统计图
 * 
 * @author Joancy
 *
 */
public class DialogDisplayChart extends JDialog {
	private static final long serialVersionUID = 1L;
	public static String SUM = "sum";
	public static String COUNT = "count";
	public static String AVG = "avg";
	public static String MAX = "max";
	public static String MIN = "min";

	private Engine engine = null, animateEngine = null;
	private ImageIcon ii = null;
	private byte[] imageBytes = null;

	private Component imageDisplay;
	private JButton close, saveAs, copy;

	private JLabel labelGraphType = new JLabel("图类型");
	private JComboBoxGraph cbGraphType = new JComboBoxGraph();
	private JLabel labelCategory = new JLabel("分类字段");
	private JComboBox<String> cbCategory = new JComboBox<String>();
	private JLabel labelSeries = new JLabel("系列字段");
	private JComboBox<String> cbSeries = new JComboBox<String>();
	private JLabel labelValue = new JLabel("数值字段");
	private JComboBox<String> cbValue = new JComboBox<String>();
	private JLabel labelAccumulate = new JLabel("聚合函数");
	private JComboBoxEx cbAccumulate = new JComboBoxEx();

	private JButton settings = new JButton("设置");
	private Table data = null;
	int sliderScale = 1;
	private HashMap<String, Object> properties = new HashMap<String, Object>();

	MessageManager splMM = IdeSplMessage.get();
	MessageManager mm = ChartMessage.get();

	/************ 动画 ****************/
	private JLabel labelFrameCount = new JLabel("动画帧数");
	int defCount = 24;
	private JSpinner spFrameCount = new JSpinner(new SpinnerNumberModel(
			defCount, 2, 10240, 1));
	private JLabel labelFrameDelay = new JLabel("帧间延时(毫秒)");
	private JSpinner spFrameDelay = new JSpinner(new SpinnerNumberModel(100,
			10, 10000, 10));
	private JCheckBox cbLoop = new JCheckBox("循环");
	JSlider slider = new JSlider(JSlider.HORIZONTAL, 1, defCount, 1);
	private JButton play = new JButton("播放");
	boolean stop = false;

	/**
	 * 构造函数
	 * 
	 * @param imageBytes
	 *            图像数据
	 */
	public DialogDisplayChart(byte[] imageBytes) {
		super(GV.appFrame, "Chart Display", true);
		try {
			if (imageBytes.length < 10)
				throw new Exception("Invalid image bytes. length="
						+ imageBytes.length);
			this.imageBytes = imageBytes;
			ii = new ImageIcon(imageBytes);
			init();
			this.setResizable(true);
		} catch (Exception x) {
			GM.showException(this, x);
			new Thread() {
				public void run() {
					closeDialog();
				}
			}.start();
		}

	}

	public DialogDisplayChart(com.scudata.dm.Canvas canvas) {
		super(GV.appFrame, "Chart Display", true);
		try {
			this.engine = new Engine(canvas.getChartElements());
			if (engine.isAnimate()) {
				animateEngine = engine;
				initAnimate();
			}
			init();
		} catch (Exception x) {
			GM.showException(this, x);
			new Thread() {
				public void run() {
					closeDialog();
				}
			}.start();
		}
	}

	/**
	 * 构造函数
	 * 
	 * @param data
	 *            数据表
	 */
	public DialogDisplayChart(Table data) {
		super(GV.appFrame, "Chart Display", true);
		try {
			this.data = data;
			initTable();
			init();
			setCategory();
			setValue();
			refresh();
		} catch (Exception x) {
			GM.showException(this, x);
			new Thread() {
				public void run() {
					closeDialog();
				}
			}.start();
		}
	}

	private void closeDialog() {
		GM.setWindowDimension(this);
		dispose();
	}

	private void resetLangText() {
		String svgTip = "";
		if (!copy.isEnabled()) {
			svgTip = splMM.getMessage("dialogdisplaychart.svgtip");
		}
		setTitle(splMM.getMessage("dialogdisplaychart.title", svgTip)); // 图形预览
		close.setText(splMM.getMessage("button.close")); // 关闭
		saveAs.setText(splMM.getMessage("button.saveas")); // 另存为
		copy.setText(splMM.getMessage("button.copy")); // 复制
		labelGraphType
				.setText(splMM.getMessage("dialogdisplaychart.graphType"));
		labelCategory.setText(splMM.getMessage("dialogdisplaychart.category"));
		labelSeries.setText(splMM.getMessage("dialogdisplaychart.series"));
		labelValue.setText(splMM.getMessage("dialogdisplaychart.value"));
		labelAccumulate.setText(splMM
				.getMessage("dialogdisplaychart.accumulate"));
		settings.setText(splMM.getMessage("dialogdisplaychart.settings"));

		labelFrameCount.setText(splMM
				.getMessage("dialogdisplaychart.framecount"));
		labelFrameDelay.setText(splMM
				.getMessage("dialogdisplaychart.frameDelay"));
		cbLoop.setText(splMM.getMessage("dialogdisplaychart.loop"));
		play.setText(splMM.getMessage("dialogdisplaychart.play"));
	}

	/**
	 * 获取图形名称
	 * 
	 * @return 名称
	 */
	public String getGraphName() {
		byte graphType = cbGraphType.getValue();
		switch (graphType) {
		case GraphTypes.GT_COL:
			return "GraphColumn";
		case GraphTypes.GT_LINE:
			return "GraphLine";
		case GraphTypes.GT_PIE:
			return "GraphPie";
		}
		return null;
	}

	private void refresh() {
		refreshEngine();
		imageDisplay.repaint();
	}

	private void refreshAnimate() {
		int frameCount = (Integer) spFrameCount.getValue();
		int frameIndex = slider.getValue() - 1;
		engine = animateEngine.getFrameEngine(frameCount, frameIndex);
		imageDisplay.repaint();
	}

	/**
	 * 获取属性映射表
	 * 
	 * @return 属性值映射表
	 */
	public HashMap<String, Object> getProperties() {
		return properties;
	}

	/**
	 * 设置属性值映射表
	 * 
	 * @param properties
	 *            属性值
	 */
	public void setProperties(HashMap<String, Object> properties) {
		this.properties = properties;
		refresh();
	}

	private void refreshEngine() {
		engine = null;
		Sequence elements = new Sequence();
		Sequence graph = new Sequence();
		graph.add(new ChartParam(getGraphName(), null));
		String categoryCol = (String) cbCategory.getSelectedItem();
		int categoryIndex = -1;
		String seriesCol = (String) cbSeries.getSelectedItem();
		int seriesIndex = -1;
		String valueCol = (String) cbValue.getSelectedItem();
		int valueIndex = -1;
		if (!StringUtils.isValidString(valueCol)) {
			return;
		}
		String sexp;
		Sequence tmpSeq;
		Table tmpData = data;

		if (isNeedAccumulate()) {
			String groupExp = getAccumulateExp();
			String accumuExp = (String) cbAccumulate.x_getSelectedItem() + "("
					+ valueCol + "):" + valueCol;
			sexp = "A.groups(" + groupExp + ";" + accumuExp + ")";
			tmpSeq = (Sequence) calculate(sexp);
			tmpData = tmpSeq.derive("o");
		}
		int rows = tmpData.length();

		Sequence categoryData = new Sequence();
		Sequence valueData = new Sequence();
		BaseRecord rec = tmpData.getRecord(1);
		categoryIndex = rec.getFieldIndex(categoryCol);
		seriesIndex = rec.getFieldIndex(seriesCol);
		valueIndex = rec.getFieldIndex(valueCol);
		for (int r = 1; r <= rows; r++) {
			rec = tmpData.getRecord(r);
			Object catVal = rec.getFieldValue(categoryIndex);
			if (seriesIndex > -1) {
				Object seriesVal = rec.getFieldValue(seriesIndex);
				catVal = catVal + "," + seriesVal;
			}
			Object valVal = rec.getFieldValue(valueIndex);

			categoryData.add(catVal);
			valueData.add(valVal);
		}

		ChartParam cp = new ChartParam("categories", categoryData);
		graph.add(cp);
		cp = new ChartParam("values", valueData);
		graph.add(cp);
		Iterator<String> it = properties.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			Object val = properties.get(key);
			cp = new ChartParam(key, val);
			graph.add(cp);
		}

		elements.add(graph);
		engine = new Engine(elements);
	}

	private Component getCenter() throws Exception {
		if (engine != null || data != null) {
			imageDisplay = new JPanel() {
				private static final long serialVersionUID = 1598380962917135629L;

				public void paint(Graphics g) {
					int w = imageDisplay.getWidth();
					int h = imageDisplay.getHeight();
					g.clearRect(0, 0, w, h);
					if (engine != null) {
						engine.draw((Graphics2D) g, 0, 0, w, h, null);
					}
				}
			};
			if (engine != null && engine.isAnimate()) {
				JPanel top = new JPanel(new BorderLayout());

				JPanel tmp = new JPanel(new GridBagLayout());
				tmp.add(labelFrameCount, GM.getGBC(1, 1));
				tmp.add(spFrameCount, GM.getGBC(1, 2, true));
				tmp.add(labelFrameDelay, GM.getGBC(1, 3));
				tmp.add(spFrameDelay, GM.getGBC(1, 4, true));
				tmp.add(cbLoop, GM.getGBC(1, 5));

				GridBagConstraints gbc = GM.getGBC(2, 1, true);
				gbc.gridwidth = 5;
				tmp.add(slider, gbc);

				top.add(tmp, BorderLayout.NORTH);
				top.add(imageDisplay, BorderLayout.CENTER);
				return top;
			}

			if (data != null) {
				JPanel top = new JPanel(new BorderLayout());
				JPanel tmp = new JPanel(new GridBagLayout());
				tmp.add(labelGraphType, GM.getGBC(1, 1));
				tmp.add(cbGraphType, GM.getGBC(1, 2, true));
				tmp.add(labelCategory, GM.getGBC(1, 3));
				tmp.add(cbCategory, GM.getGBC(1, 4, true));
				tmp.add(labelSeries, GM.getGBC(1, 5));
				tmp.add(cbSeries, GM.getGBC(1, 6, true));

				tmp.add(labelValue, GM.getGBC(2, 3));
				tmp.add(cbValue, GM.getGBC(2, 4, true));
				tmp.add(labelAccumulate, GM.getGBC(2, 5));
				tmp.add(cbAccumulate, GM.getGBC(2, 6, true));
				top.add(tmp, BorderLayout.NORTH);
				top.add(imageDisplay, BorderLayout.CENTER);
				return top;
			}

			return imageDisplay;
		}

		if (ii.getIconWidth() != -1) {
			JLabel centerLabel = new JLabel(ii);
			if (ii.getIconWidth() > 700 || ii.getIconHeight() > 570) {
				JScrollPane jsp = new JScrollPane(centerLabel);
				return jsp;
			}
			return centerLabel;
		}
		// SVG画板暂时没有合适的获取宽高的属性，故只能绘制固定800*600区域。
		JSVGCanvas svgCanvas = new JSVGCanvas();

		SVGDocumentFactory factory = new SAXSVGDocumentFactory(null);
		ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
		SVGDocument svgDocument = factory.createSVGDocument(null, bais);
		svgCanvas.setSVGDocument(svgDocument);
		svgCanvas.setEnableImageZoomInteractor(true);
		svgCanvas.setEnableZoomInteractor(true);
		svgCanvas.setEnableResetTransformInteractor(true);
		svgCanvas.setSize(800, 600);
		svgCanvas.repaint(0, 0, 800, 600);
		return svgCanvas;
	}

	private Component getEast() {
		JPanel panelEast = new JPanel(new VFlowLayout());

		close = new JButton("Close");
		close.setMnemonic('c');
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeDialog();
			}
		});
		panelEast.add(close);

		saveAs = new JButton("Save as");
		saveAs.setMnemonic('a');
		saveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String fileExt;
				if (engine != null) {
					fileExt = "svg,jpg,png,gif";
				} else if (ii.getIconWidth() != -1) {
					try {
						fileExt = GM.getImageType(imageBytes);
					} catch (IOException e1) {
						GM.showException(DialogDisplayChart.this, e1);
						fileExt = "jpg,png,gif";
					}
				} else {
					fileExt = "svg";
				}
				String txt = saveAs.getText();
				int p = txt.indexOf("(");
				if (p > 0) {
					txt = txt.substring(0, p);
				}
				File saveFile = GM.dialogSelectFile(DialogDisplayChart.this,
						fileExt, GV.lastDirectory, txt, "");
				if (saveFile == null) {
					return;
				}

				String sfile = saveFile.getAbsolutePath();
				GV.lastDirectory = saveFile.getParent();

				if (!GM.canSaveAsFile(DialogDisplayChart.this, sfile)) {
					return;
				}
				// save
				byte[] streamBytes = null;
				if (engine == null) {
					streamBytes = imageBytes;
				} else if (animateEngine != null) {
					AnimatedGifEncoder age = new AnimatedGifEncoder();
					int delay = (Integer) spFrameDelay.getValue();
					if (cbLoop.isSelected()) {
						age.setRepeat(delay);
					}
					try {
						FileOutputStream fos = new FileOutputStream(saveFile);
						age.start(fos);
						age.setDelay(delay);
						int count = slider.getMaximum();
						for (int i = 0; i <= count; i++) {
							Engine tmpe = animateEngine
									.getFrameEngine(count, i);
							BufferedImage bi = tmpe.calcBufferedImage(
									imageDisplay.getWidth(),
									imageDisplay.getHeight(), Consts.IMAGE_JPG);
							age.addFrame(bi);
						}
						age.finish();
						GM.showException(DialogDisplayChart.this, splMM
								.getMessage("dialogdisplaychart.saveinfo",
										saveFile));
					} catch (Exception x) {
						GM.showException(DialogDisplayChart.this, x);
					}
					return;
				} else {
					fileExt = sfile.substring(sfile.length() - 3);
					byte imageFmt = Consts.IMAGE_PNG;
					if (fileExt.equalsIgnoreCase("JPG")) {
						imageFmt = Consts.IMAGE_JPG;
					} else if (fileExt.equalsIgnoreCase("GIF")) {
						imageFmt = Consts.IMAGE_GIF;
					} else if (fileExt.equalsIgnoreCase("SVG")) {
						imageFmt = Consts.IMAGE_SVG;
					}
					streamBytes = engine.calcImageBytes(
							imageDisplay.getWidth(), imageDisplay.getHeight(),
							imageFmt);
				}
				try {
					FileOutputStream fos = new FileOutputStream(saveFile);
					fos.write(streamBytes);
					fos.close();
				} catch (Exception x) {
					GM.showException(DialogDisplayChart.this, x);
				}
			}
		});
		panelEast.add(saveAs);

		copy = new JButton("Copy");
		copy.setMnemonic('p');
		copy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (engine != null) {
					byte[] bytes = engine.calcImageBytes(
							imageDisplay.getWidth(), imageDisplay.getHeight(),
							Consts.IMAGE_PNG);
					ImageIcon icon = new ImageIcon(bytes);
					setClipboardImage(icon.getImage());
				} else {
					setClipboardImage(ii.getImage());
				}
			}
		});
		panelEast.add(copy);

		if (data != null) {
			panelEast.add(new JLabel(" "));
			panelEast.add(settings);
		} else if (engine != null && engine.isAnimate()) {
			panelEast.add(new JLabel(" "));
			panelEast.add(play);
		}

		return panelEast;
	}

	private Object getFirstValid(String field) {
		int c = data.length();
		for (int r = 1; r <= c; r++) {
			BaseRecord rec = data.getRecord(r);
			Object tmp = rec.getFieldValue(field);
			if (tmp != null) {
				return tmp;
			}
		}
		return null;
	}

	private void initTable() {
		Vector<String> code = new Vector<String>();
		Vector<String> disp = new Vector<String>();
		code.add(SUM);
		code.add(COUNT);
		code.add(AVG);
		code.add(MAX);
		code.add(MIN);

		disp.add(mm.getMessage(SUM));
		disp.add(mm.getMessage(COUNT));
		disp.add(mm.getMessage(AVG));
		disp.add(mm.getMessage(MAX));
		disp.add(mm.getMessage(MIN));

		cbAccumulate.x_setData(code, disp);

		String[] fieldNames = data.dataStruct().getFieldNames();
		cbSeries.addItem("");
		for (int i = 0; i < fieldNames.length; i++) {
			String field = fieldNames[i];
			Object tmp = getFirstValid(field);

			cbCategory.addItem(field);
			cbSeries.addItem(field);

			if (tmp instanceof Number) {
				cbValue.addItem(field);
			}
		}

		String defCat = (String) cbCategory.getSelectedItem();
		int c = cbValue.getItemCount();
		for (int i = 0; i < c; i++) {
			String defVal = (String) cbValue.getItemAt(i);
			if (!defCat.equals(defVal)) {
				cbValue.setSelectedIndex(i);
				break;
			}
		}

		cbGraphType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		cbCategory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setCategory();
				refresh();
			}
		});
		cbSeries.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSeries();
				refresh();
			}
		});
		cbValue.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setValue();
				refresh();
			}
		});
		cbAccumulate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});

		final DialogDisplayChart ddc = this;
		settings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DialogGraphEdit dge = new DialogGraphEdit(ddc);
				dge.setVisible(true);
			}
		});
	}

	private void initAnimate() {
		spFrameCount.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int maximum = (Integer) spFrameCount.getValue();
				slider.setMaximum(maximum + 1);
				refreshAnimate();
			}
		});
		final String PLAY = splMM.getMessage("dialogdisplaychart.play");
		final String STOP = splMM.getMessage("dialogdisplaychart.stop");
		play.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (play.getText().equals(PLAY)) {
					Thread animateThread = new Thread() {
						public void run() {
							play.setText(STOP);
							int count = slider.getMaximum();
							int delay = (Integer) spFrameDelay.getValue();
							for (int i = 0; i <= count; i++) {
								slider.setValue(i);
								try {
									Thread.sleep(delay);
								} catch (InterruptedException e) {
								}
								if (cbLoop.isSelected() && i == count) {
									i = 1;
								}
								if (stop) {
									break;
								}
							}
							stop = false;
							play.setText(PLAY);
						}
					};
					animateThread.start();
				} else {
					stop = true;
				}

			}
		});

		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				refreshAnimate();
			}
		});
	}

	private String getAccumulateExp() {
		// 检查当前的分类，系列字段的值是否唯一，不唯一的话，需要聚合统计
		String sCategory = (String) cbCategory.getSelectedItem();
		if (sCategory == null) {
			return null;
		}
		String exp = Escape.addEscAndQuote(sCategory, false);
		String sSeries = (String) cbSeries.getSelectedItem();
		if (StringUtils.isValidString(sSeries)) {
			exp += "," + Escape.addEscAndQuote(sSeries, false);
		}

		return exp;
	}

	private boolean isNeedAccumulate() {
		String exp = getAccumulateExp();
		if (exp == null) {
			return false;
		}
		int originalLen = data.length();

		String sexp = "A.groups(" + exp + ")";

		Sequence seq = (Sequence) calculate(sexp);
		return seq.length() != originalLen;
	}

	private void enableAccumulate() {
		boolean needAccumulate = isNeedAccumulate();
		labelAccumulate.setEnabled(needAccumulate);
		cbAccumulate.setEnabled(needAccumulate);
	}

	private void setCategory() {
		String XTITLE = "xTitle";
		Object value = cbCategory.getSelectedItem();
		properties.put(XTITLE, value);
		enableAccumulate();
	}

	private void setSeries() {
		enableAccumulate();
	}

	private void setValue() {
		String YTITLE = "yTitle";
		String valueCol = (String) cbValue.getSelectedItem();
		properties.put(YTITLE, valueCol);
	}

	private Object calculate(String sexp) {
		Context ctx = new Context();
		ctx.setParamValue("A", data);
		Expression exp = new Expression(ctx, sexp);
		return exp.calculate(ctx);
	}

	private void init() throws Exception {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(getCenter(), BorderLayout.CENTER);
		panel.add(getEast(), BorderLayout.EAST);

		this.getContentPane().add(panel);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeDialog();
			}
		});

		if (engine != null || data != null) {
			this.setSize(800, 600);
		} else if (ii.getIconWidth() != -1) {
			int w = ii.getIconWidth() + 100;
			int h = ii.getIconHeight() + 30;
			if (w > 700 || h > 570) {
				w = 800;
				h = 600;
			}
			this.setSize(w, h);
		} else {
			this.setSize(800, 600);
			copy.setEnabled(false);// svg图片不支持复制
		}

		resetLangText();
		GM.setDialogDefaultButton(this, close, close);
		if (data != null) {// 直接用数据表绘图时，往左边空出属性设置窗口
			setLocation(getX() - 200, getY());
		}
	}

	/**
	 * 将图像数据复制到系统剪贴板
	 * 
	 * @param image
	 *            图像
	 */
	public static void setClipboardImage(final Image image) {
		Transferable trans = new Transferable() {
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] { DataFlavor.imageFlavor };
			}

			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return DataFlavor.imageFlavor.equals(flavor);
			}

			public Object getTransferData(DataFlavor flavor)
					throws UnsupportedFlavorException, IOException {
				if (isDataFlavorSupported(flavor)) {
					return image;
				}
				throw new UnsupportedFlavorException(flavor);
			}

		};
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(trans, null);
		} catch (HeadlessException e) {
		}
	}

}
