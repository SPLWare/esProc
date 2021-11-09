package com.raqsoft.ide.common.swing;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import com.raqsoft.cellset.INormalCell;
import com.raqsoft.cellset.datamodel.CellSet;
import com.raqsoft.cellset.datamodel.Command;
import com.raqsoft.cellset.datamodel.PgmNormalCell;
import com.raqsoft.common.CellLocation;
import com.raqsoft.common.Sentence;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.KeyWord;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.dfx.GVDfx;
import com.raqsoft.ide.dfx.control.DfxControl;

/**
 * JTextPane的扩展类。
 * 
 * 可以匹配括号，高亮显示关键字。
 */
public class JTextPaneEx extends JTextPane {

	private static final long serialVersionUID = 1L;

	/**
	 * 样式文档对象
	 */
	private DefaultStyledDocument doc;
	/**
	 * 
	 */
	private boolean matchEnabled = true;
	/**
	 * 是否阻止变化
	 */
	protected boolean preventChanged = false;

	/**
	 * 构造函数
	 */
	public JTextPaneEx() {
		super();
		try {
			this.setEditorKit(new WarpEditorKit());
			init();
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * 设置是否匹配
	 * 
	 * @param matchEnabled
	 */
	public void setMatchEnabled(boolean matchEnabled) {
		this.matchEnabled = matchEnabled;
	}

	/**
	 * 初始化控件
	 */
	private void init() {
		setFont(GC.font);
		StyleContext sc = new StyleContext();
		doc = new DefaultStyledDocument(sc);
		this.setDocument(doc);
		doc.addDocumentListener(new DocumentListener() {

			public void insertUpdate(DocumentEvent e) {
				docUpdate();
			}

			public void removeUpdate(DocumentEvent e) {
				docUpdate();
			}

			public void changedUpdate(DocumentEvent e) {
				docUpdate();
			}

		});
		this.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				int key = e.getKeyCode();
				switch (key) {
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
					if (getText() == null || getText().equals("")) {
						return;
					}
					if (key == KeyEvent.VK_DOWN && e.isShiftDown()) {
						return;
					}
					if ((key == KeyEvent.VK_DOWN || key == KeyEvent.VK_UP)
							&& e.isAltDown()) {
						return;
					}
					caretChanged(getCaretPosition());
					return;
				case KeyEvent.VK_HOME:
				case KeyEvent.VK_END:
				case KeyEvent.VK_PAGE_UP:
				case KeyEvent.VK_PAGE_DOWN:
					caretChanged(getCaretPosition());
					return;
				}
			}
		});
		this.addCaretListener(new CaretListener() {

			public void caretUpdate(CaretEvent e) {
				caretChanged(e.getDot());
			}
		});
		StyleConstants.setBold(AS_BRACKET, true);
		StyleConstants.setForeground(AS_BRACKET, COLOR_BRACKET);
		StyleConstants.setForeground(AS_KEY, COLOR_KEY);
	}

	/**
	 * 设置是否阻止变化
	 * 
	 * @param preventChanged
	 */
	public void setPreventChange(boolean preventChanged) {
		this.preventChanged = preventChanged;
	}

	/**
	 * 文档更新
	 */
	protected void docUpdate() {
		if (preventChanged)
			return;
		initRefCells(true);
	}

	/**
	 * 光标移动
	 * 
	 * @param caret
	 *            光标位置
	 */
	public void caretChanged(int caret) {
		if (!isVisible())
			return;
		if (preventChanged)
			return;
		if (!matchEnabled)
			return;
		matchField();
		List<CA> total = new ArrayList<CA>();
		String text = getText();
		if (text != null && text.length() > 0) {
			// 重置
			total.add(new CA(0, text.length(), AS_DEFAULT, true));
		}
		total.addAll(refCAs);
		List<CA> carets = getCaretCAs(caret);
		total.addAll(carets);
		resetCAs(total);
	}

	/**
	 * 取所有高亮显示的配置
	 * 
	 * @param caret
	 * @return
	 */
	private List<CA> getCaretCAs(int caret) {
		List<CA> cas = new ArrayList<CA>();
		List<CA> tmp;
		tmp = matchKeyWords(caret);
		if (tmp != null)
			cas.addAll(tmp);
		tmp = getBracketCAs(caret);
		if (tmp != null)
			cas.addAll(tmp);
		return cas;
	}

	/**
	 * 匹配字段名
	 */
	private void matchField() {
		if (GVDfx.matchWindow != null) {
			try {
				String text = getText();
				if (!StringUtils.isValidString(text)) {
					GVDfx.matchWindow.dispose();
					GVDfx.matchWindow = null;
					return;
				}
				int start = GVDfx.matchWindow.getDot();
				int p = getCaretPosition();
				if (p < start - 1) {
					GVDfx.matchWindow.dispose();
					GVDfx.matchWindow = null;
					return;
				}
				if (start == p) {
					return;
				}
				int len = text.length();
				int end = len;
				for (int i = start; i < len; i++) {
					char c = text.charAt(i);
					if (KeyWord.isSymbol(c)) {
						end = i;
						break;
					}
				}
				if (p > end) {
					GVDfx.matchWindow.dispose();
					GVDfx.matchWindow = null;
				}
			} catch (Throwable t) {
				// t.printStackTrace();
			}
		}
	}

	/**
	 * 匹配关键字
	 * 
	 * @param caret
	 * @return
	 */
	private List<CA> matchKeyWords(int caret) {
		List<CA> cas = new ArrayList<CA>();
		try {
			String text = getText();
			if (!StringUtils.isValidString(text) || text.startsWith("/"))
				return cas;
			for (int i = 0; i < text.length(); i++) {
				int end = KeyWord.scanId(text, i);
				if (end > i) {
					String word = text.substring(i, end);
					if (keyWords.contains(word)) {
						cas.add(new CA(i, word.length(), AS_KEY, false));
						i = end;
					}
				}
			}
		} catch (Throwable t) {
			// t.printStackTrace();
		}
		return cas;
	}

	/**
	 * 取匹配的括号
	 * 
	 * @param caret
	 * @return
	 */
	private List<CA> getBracketCAs(int caret) {
		List<CA> cas = new ArrayList<CA>();
		int[] brackets = matchBrackets(caret);
		if (brackets == null)
			return cas;
		cas.add(new CA(brackets[0], 1, AS_BRACKET, false));
		cas.add(new CA(brackets[1], 1, AS_BRACKET, false));
		return cas;
	}

	/**
	 * 匹配括号
	 * 
	 * @param caret
	 * @return
	 */
	private int[] matchBrackets(int caret) {
		try {
			String text = getText();
			if (!StringUtils.isValidString(text))
				return null;
			if (caret >= text.length()) {
				caret--;
			}
			int matchDot = -1;
			int md = findBrackets(caret);
			if (md > -1) {
				matchDot = md;
			} else if (caret > 0) {
				caret--;
				md = findBrackets(caret);
				if (md > -1) {
					matchDot = md;
				}
			}
			if (matchDot > -1) {
				return new int[] { caret, matchDot };
			}
		} catch (Throwable t) {
			// t.printStackTrace();
		}
		return null;
	}

	/**
	 * 找括号
	 * 
	 * @param caret
	 * @return
	 */
	private int findBrackets(int caret) {
		String text = getText();
		char c = text.charAt(caret);
		int inBrackets = 0;
		if (c == '(') {
			if (!isValidChar(caret)) {
				return -1;
			}
			inBrackets++;
			for (int i = caret + 1; i < text.length(); i++) {
				char c1 = text.charAt(i);
				if (c1 == '(') {
					if (isValidChar(i)) {
						inBrackets++;
					}
				} else if (c1 == ')') {
					if (isValidChar(i)) {
						inBrackets--;
						if (inBrackets == 0) {
							return i;
						} else if (inBrackets < 0) { // 括号不匹配
							return -1;
						}
					}
				}
			}
		} else if (c == ')') {
			if (!isValidChar(caret)) {
				return -1;
			}
			inBrackets++;
			for (int i = caret - 1; i >= 0; i--) {
				char c1 = text.charAt(i);
				if (c1 == ')') {
					if (isValidChar(i)) {
						inBrackets++;
					}
				} else if (c1 == '(') {
					if (isValidChar(i)) {
						inBrackets--;
						if (inBrackets == 0) {
							return i;
						} else if (inBrackets < 0) { // 括号不匹配
							return -1;
						}
					}
				}
			}
		}
		return -1;
	}

	/**
	 * 字符是否合法
	 * 
	 * @param index
	 * @return
	 */
	private boolean isValidChar(int index) {
		return isValidString(index, 1);
	}

	/**
	 * 字符串是否合法
	 * 
	 * @param index
	 * @param len
	 * @return
	 */
	private boolean isValidString(int index, int len) {
		String text = getText();
		int i = Sentence.indexOf(text, index,
				text.substring(index, index + len), SEARCH_FLAG);
		return index == i;
	}

	/**
	 * 初始化引用格
	 * 
	 * @param isUpdate
	 */
	public synchronized void initRefCells(boolean isUpdate) {
		if (!matchEnabled)
			return;
		List<INormalCell> lastRefCells = new ArrayList<INormalCell>();
		if (refCells != null) {
			lastRefCells.addAll(refCells);
		}
		refCells.clear();
		try {
			DfxControl control = null;
			CellSet cellSet = null;
			if (GVDfx.dfxEditor != null) {
				control = GVDfx.dfxEditor.getComponent();
				if (control != null) {
					cellSet = control.dfx;
				}
			}
			String text = getText();
			if (text != null && text.length() > 0) {
				// 重置
				refCAs.add(new CA(0, text.length(), AS_DEFAULT, true));
			}
			if (cellSet != null && text != null) {
				if (isUpdate) {
					if (StringUtils.isValidString(text)
							&& !text.startsWith("/")) {
						Command cmd = null;
						try {
							cmd = Command.parse(text);
						} catch (Exception ex) {
						}
						if (cmd != null) {
							IParam param = cmd.getParam(control.dfx,
									new Context());
							if (param != null)
								param.getUsedCells(refCells);
						} else {
							Expression exp = new Expression(cellSet,
									new Context(), text);
							exp.getUsedCells(refCells);
						}
					}
				} else {
					CellLocation cl = control.getActiveCell();
					PgmNormalCell activeCell = control.dfx.getPgmNormalCell(
							cl.getRow(), cl.getCol());
					if (activeCell != null) {
						activeCell.getUsedCells(refCells);
					}
				}
				if (!refCells.isEmpty()) {
					for (int i = 0; i < refCells.size(); i++) {
						INormalCell cell = refCells.get(i);
						if (cell != null) {
							int colorIndex = i % REF_COLORS.length;
							if (as[colorIndex] == null) {
								as[colorIndex] = new SimpleAttributeSet();
								StyleConstants.setForeground(as[colorIndex],
										REF_COLORS[colorIndex]);
							}
							final String cellId = cell.getCellId();
							int start = 0;
							while (true) {
								final int index = Sentence.indexOf(text, start,
										cellId, SEARCH_FLAG);
								if (index < 0)
									break;
								refCAs.add(new CA(index, cellId.length(),
										as[colorIndex], false));
								start = index + cellId.length();
							}
						}
					}
				}
			}
			List<CA> total = new ArrayList<CA>();
			total.addAll(refCAs);
			total.addAll(getCaretCAs(getCaretPosition()));
			resetCAs(total);
			if (control != null) {
				// 比较一下，如果格子没变就不刷新了
				boolean isSame = false;
				if (lastRefCells != null && refCells != null)
					if (lastRefCells.size() == refCells.size()) {
						isSame = true;
						for (INormalCell cell1 : refCells) {
							boolean hasCell = false;
							for (INormalCell cell2 : lastRefCells) {
								if (cell1.getRow() == cell2.getRow()
										&& cell1.getCol() == cell2.getCol()) {
									hasCell = true;
								}
							}
							if (!hasCell) {
								isSame = false;
								break;
							}
						}
					}
				if (!isSame)
					control.getContentPanel().repaint();
			}
		} catch (Throwable t) {
			// t.printStackTrace();
		}
	}

	/**
	 * 重置匹配列表
	 * 
	 * @param cas
	 */
	private synchronized void resetCAs(final List<CA> cas) {
		if (cas == null)
			return;
		if (!cas.isEmpty()) {
			SwingUtilities.invokeLater(new Thread() {
				public void run() {
					resetCAList(cas);
				}
			});
		}
	}

	/**
	 * 重置匹配设置列表
	 * 
	 * @param cas
	 */
	private synchronized void resetCAList(List<CA> cas) {
		try {
			preventChanged = true;
			GVDfx.toolBarProperty.preventAction = true;
			for (CA ca : cas) {
				doc.setCharacterAttributes(ca.offset, ca.length, ca.s,
						ca.replace);
			}
			SwingUtilities.invokeLater(new Thread() {
				public void run() {
					repaint();
				}
			});
		} finally {
			GVDfx.toolBarProperty.preventAction = false;
			preventChanged = false;
		}
	}

	/**
	 * 取引用格列表
	 * 
	 * @return
	 */
	public List<INormalCell> getRefCells() {
		return refCells;
	}

	/**
	 * 取引用格颜色
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public Color getRefCellColor(int row, int col) {
		if (refCells == null || refCells.isEmpty())
			return null;
		for (int i = 0; i < refCells.size(); i++) {
			INormalCell cell = refCells.get(i);
			if (cell != null) {
				if (cell.getRow() == row && cell.getCol() == col) {
					return REF_COLORS[i % REF_COLORS.length];
				}
			}
		}
		return null;
	}

	/**
	 * 定义关键字
	 */
	private static final String KEY_IF = "if";
	private static final String KEY_ELSE = "else";
	private static final String KEY_ELSEIF = "elseif";

	private static final String KEY_FOR = "for";
	private static final String KEY_NEXT = "next";
	private static final String KEY_BREAK = "break";

	private static final String KEY_FUNC = "func";
	private static final String KEY_RETURN = "return";
	private static final String KEY_END = "end";
	private static final String KEY_RESULT = "result";
	private static final String KEY_CLEAR = "clear";
	private static final String KEY_FORK = "fork";
	private static final String KEY_REDUCE = "reduce";

	private static final String KEY_GOTO = "goto";
	private static final String KEY_CHANNEL = "cursor";

	/**
	 * 关键字集合
	 */
	private static List<String> keyWords = new ArrayList<String>();
	static {
		keyWords.add(KEY_IF);
		keyWords.add(KEY_ELSE);
		keyWords.add(KEY_ELSEIF);
		keyWords.add(KEY_FOR);
		keyWords.add(KEY_NEXT);
		keyWords.add(KEY_BREAK);
		keyWords.add(KEY_FUNC);
		keyWords.add(KEY_RETURN);
		keyWords.add(KEY_END);
		keyWords.add(KEY_RESULT);
		keyWords.add(KEY_CLEAR);
		keyWords.add(KEY_FORK);
		keyWords.add(KEY_REDUCE);
		keyWords.add(KEY_GOTO);
		keyWords.add(KEY_CHANNEL);
	}

	/**
	 * 多个颜色循环使用，超出了再从0开始
	 */
	private static final Color REF_COLOR1 = Color.BLUE;
	private static final Color REF_COLOR2 = Color.RED;
	private static final Color REF_COLOR3 = Color.PINK;
	private static final Color REF_COLOR4 = Color.GREEN;
	private static final Color REF_COLOR5 = Color.MAGENTA;
	private static final Color REF_COLOR6 = Color.ORANGE;
	private static final Color REF_COLOR7 = Color.CYAN;
	private static final Color[] REF_COLORS = new Color[] { REF_COLOR1,
			REF_COLOR2, REF_COLOR3, REF_COLOR4, REF_COLOR5, REF_COLOR6,
			REF_COLOR7 };

	private static final Color COLOR_BRACKET = Color.RED;
	private static final Color COLOR_KEY = Color.BLUE;

	/** 缺省 */
	private static final MutableAttributeSet AS_DEFAULT = new SimpleAttributeSet();
	/** 匹配括号 */
	private static final MutableAttributeSet AS_BRACKET = new SimpleAttributeSet();
	/** 关键字 */
	private static final MutableAttributeSet AS_KEY = new SimpleAttributeSet();

	/**
	 * 引用单元格颜色的集合
	 */
	private MutableAttributeSet[] as = new MutableAttributeSet[REF_COLORS.length];

	/**
	 * 所有引用格的匹配设置
	 */
	private List<CA> refCAs = new ArrayList<CA>();
	/**
	 * 引用格列表
	 */
	private List<INormalCell> refCells = new ArrayList<INormalCell>();
	/**
	 * 搜索配置
	 */
	private final int SEARCH_FLAG = Sentence.IGNORE_PARS + Sentence.ONLY_PHRASE;

	/**
	 * 编辑工具
	 *
	 */
	private class WarpEditorKit extends StyledEditorKit {
		private static final long serialVersionUID = 1L;
		private ViewFactory defaultFactory = new WarpColumnFactory();

		public ViewFactory getViewFactory() {
			return defaultFactory;
		}
	}

	/**
	 * 列工厂
	 */
	private class WarpColumnFactory implements ViewFactory {

		public View create(Element elem) {
			String kind = elem.getName();
			if (kind != null) {
				if (kind.equals(AbstractDocument.ContentElementName)) {
					return new WarpLabelView(elem);
				} else if (kind.equals(AbstractDocument.ParagraphElementName)) {
					return new ParagraphView(elem);
				} else if (kind.equals(AbstractDocument.SectionElementName)) {
					return new BoxView(elem, View.Y_AXIS);
				} else if (kind.equals(StyleConstants.ComponentElementName)) {
					return new ComponentView(elem);
				} else if (kind.equals(StyleConstants.IconElementName)) {
					return new IconView(elem);
				}
			}
			return new LabelView(elem);
		}
	}

	/**
	 * 标签视图
	 *
	 */
	private class WarpLabelView extends LabelView {

		public WarpLabelView(Element elem) {
			super(elem);
		}

		public float getMinimumSpan(int axis) {
			switch (axis) {
			case View.X_AXIS:
				return 0;
			case View.Y_AXIS:
				return super.getMinimumSpan(axis);
			default:
				throw new IllegalArgumentException("Invalid axis: " + axis);
			}
		}
	}

	/**
	 * 匹配到的高亮显示的配置
	 *
	 */
	class CA {
		int offset;
		int length;
		AttributeSet s;
		boolean replace;

		public CA(int offset, int length, AttributeSet s, boolean replace) {
			this.offset = offset;
			this.length = length;
			this.s = s;
			this.replace = replace;
		}
	}
}
