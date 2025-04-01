package com.scudata.ide.common.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Calendar;

import javax.swing.JComponent;
import javax.swing.border.Border;

import com.scudata.app.common.Section;
import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * Monthly Calendar organized in weeks.
 */

public class MonthlyCalendar extends JComponent implements MouseListener,
		MouseMotionListener {
	private static final long serialVersionUID = 1L;

	/** Default symbols to be displayed for the header */
	public static String[] DEFAULT_HEADER_SYMBOLS;

	/** Default font to be used */
	public static Font DEFAULT_FONT;

	/** Default background color of the header */
	public static final Color DEFAULT_HEADER_BG = new Color(0x7A, 0x96, 0xDF);

	/** Default foreground color of the header */
	public static final Color DEFAULT_HEADER_FG = new Color(0xD8, 0xE4, 0xF8);

	/** Default background color of the date */
	public static final Color DEFAULT_DATE_BG = Color.white;

	/** Default foreground color of the date */
	public static final Color DEFAULT_DATE_FG = Color.black;

	/** Default background color of highlighted date */
	public static final Color DEFAULT_HIGHLIGHT_BG = new Color(0x00, 0x54, 0xE3);

	/** Default foreground color of highlighted date */
	public static final Color DEFAULT_HIGHLIGHT_FG = Color.white;

	/** Default cell width */
	public static final int DEFAULT_CELL_WIDTH = 24;

	/** Default cell height */
	public static final int DEFAULT_CELL_HEIGHT = 18;

	/** Array to hold date distribution information */
	private int[] dateData = new int[42];

	/** Year value for this calendar */
	private int year = -1;

	/** Month value for this calendar */
	private int month = -1;

	/** Index in the date data array for the highlighted date */
	private int hindex = -1;

	/** Selected date */
	private int selectedDate = 0;

	/** if true, week starts with Sunday; else week starts with Monday */
	private boolean startOnSunday = true;

	/** Cell width */
	private int cellWidth = DEFAULT_CELL_WIDTH;

	/** Cell height */
	private int cellHeight = DEFAULT_CELL_HEIGHT;

	/** Header font */
	private Font headerFont = DEFAULT_FONT;

	/** Date font */
	private Font dateFont = DEFAULT_FONT;

	/** Header background */
	private Color headerBg = DEFAULT_HEADER_BG;

	/** Header foreground */
	private Color headerFg = DEFAULT_HEADER_FG;

	/** Date background */
	private Color dateBg = DEFAULT_DATE_BG;

	/** Date foreground */
	private Color dateFg = DEFAULT_DATE_FG;

	/** Highlighted date background */
	private Color highBg = DEFAULT_HIGHLIGHT_BG;

	/** Highlighted date foreground */
	private Color highFg = DEFAULT_HIGHLIGHT_FG;

	/** User installed header component */
	private JComponent headerComponent;

	/** Keep ref to preferredsize to avoid multiple instantiation of objects */
	private Dimension pSize;

	/**
	 * When border are added, origin of graphics will shift; else, origin at(0,
	 * 0)
	 */
	private int basex = 0;

	/**
	 * When border are added, origin of graphics will shift; else, origin at(0,
	 * 0)
	 */
	private int basey = 0;

	/**
	 * Variable keep track of whether "this" MouseListener+MouseMotionListenre
	 * is installed
	 */
	private boolean isListening;

	/** Default constructor */
	public MonthlyCalendar() {
		loadLanguageString();
		pSize = new Dimension(cellWidth * 7, cellHeight * 7);
		super.setPreferredSize(pSize);
		setMouseListeningEnabled(true);
	}

	/**
	 * 加载语言资源
	 */
	private void loadLanguageString() {
		DEFAULT_HEADER_SYMBOLS = new Section(IdeCommonMessage.get().getMessage(
				"monthlycalendar.weekdays")).toStringArray(); // 日, 一, 二, 三, 四,
		// 五, 六
		DEFAULT_FONT = new java.awt.Font("Dialog", 0, 12);
	}

	/**
	 * Bean method. Once disabled, there will be no highlighting effect and no
	 * "date selection event".
	 */
	public void setMouseListeningEnabled(boolean b) {
		if (b && !isListening) {
			addMouseListener(this);
			addMouseMotionListener(this);
		} else if (!b && isListening) {
			removeMouseListener(this);
			removeMouseMotionListener(this);
		}
		isListening = b;
	}

	/** Bean method. */
	public boolean getMouseListeningEnabled() {
		return isListening;
	}

	/** Bean method */
	public void setYear(int year) {
		if (year < 1970) {
			throw new IllegalArgumentException("Year must be later than 1970!");
		}
		if (month != -1) {
			setYearMonth(year, month);
		} else {
			this.year = year;
		}
	}

	/** Bean method */
	public int getYear() {
		return year;
	}

	/** Bean method */
	public void setMonth(int month) {
		if (month < 1 || month > 12) {
			throw new IllegalArgumentException("Invalid month specified!");
		}
		if (year != -1) {
			setYearMonth(year, month);
		} else {
			this.month = month;
		}
	}

	/** Bean method */
	public int getMonth() {
		return month;
	}

	/** Convinent method to set year and month */
	public void setYearMonth(int year, int month) {
		// NOTE: JANUARY = 0 in Calendar object.
		if (this.year == year && this.month == month) {
			return;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month - 1, 1);
		setYearMonth(calendar);
	}

	/** Convinent method to set year and month */
	public void setYearMonth(Calendar calendar) {
		// NOTE: Calendar.SUNDAY = 1;
		// Calendar.MONDAY = 2;
		// Calendar.TUESDAY = 3; ... Calendar.SATURDAY = 6;
		int y = calendar.get(Calendar.YEAR);
		int m = calendar.get(Calendar.MONTH) + 1;
		int d = calendar.get(Calendar.DAY_OF_MONTH);

		calendar.set(Calendar.DAY_OF_MONTH, 1);
		int max = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		int start = calendar.get(Calendar.DAY_OF_WEEK);
		if (startOnSunday) {
			start = start - 1;
		} else {
			start = (start + 5) % 7;
		}
		hindex = d + start - 1;
		if (year == y && month == m) {
			return;
		}
		year = y;
		month = m;

		int backupDate = calendar.get(Calendar.DAY_OF_MONTH);

		for (int i = 0; i < start; i++) {
			dateData[i] = -1;
		}
		for (int i = 0; i < max; i++) {
			dateData[i + start] = i + 1;
		}
		for (int i = start + max; i < 42; i++) {
			dateData[i] = -1;
		}
		// restore old day of month value
		calendar.set(Calendar.DAY_OF_MONTH, backupDate);
	}

	/** Bean method */
	public void setWeekStartOnSunday(boolean b) {
		startOnSunday = b;
	}

	/** Bean method */
	public boolean getWeekStartOnSunday() {
		return startOnSunday;
	}

	/** Bean method */
	public void setHeaderFont(Font font) {
		headerFont = font;
	}

	/** Bean method */
	public Font getHeaderFont() {
		return headerFont;
	}

	/** Bean method */
	public void setDateFont(Font font) {
		dateFont = font;
	}

	/** Bean method */
	public Font getDateFont() {
		return dateFont;
	}

	/** Bean method */
	public void setHeaderBackground(Color c) {
		headerBg = c;
	}

	/** Bean method */
	public Color getHeaderBackground() {
		return headerBg;
	}

	/** Bean method */
	public void setHeaderForeground(Color c) {
		headerFg = c;
	}

	/** Bean method */
	public Color getHeaderForeground() {
		return headerFg;
	}

	/** Bean method */
	public void setDateBackground(Color c) {
		dateBg = c;
	}

	/** Bean method */
	public Color getDateBackground() {
		return dateBg;
	}

	/** Bean method */
	public void setDateForeground(Color c) {
		dateFg = c;
	}

	/** Bean method */
	public Color getDateForeground() {
		return dateFg;
	}

	/** Bean method */
	public void setHighlightBackground(Color c) {
		highBg = c;
	}

	/** Bean method */
	public Color getHighlightBackground() {
		return highBg;
	}

	/** Bean method */
	public void setHighlightForeground(Color c) {
		highFg = c;
	}

	/** Bean method */
	public Color getHighlightForeground() {
		return highFg;
	}

	/**
	 * Bean method. Note: if headerComponent is set, calling this method will
	 * have no effect.
	 */
	public void setCellDimension(Dimension d) {
		if (headerComponent != null) {
			return;
		}
		cellWidth = d.width;
		cellHeight = d.height;
		resize();
	}

	/** Bean method */
	public Dimension getCellDimension() {
		return new Dimension(cellWidth, cellHeight);
	}

	/**
	 * This method will be called in three occasions: (1) when cell dimension is
	 * set; (2) when header component is set; (3) when border is set. It makes
	 * sure the preferredSize of the component is set properly: if header
	 * component is not set, simply use the cell dimension to determine the
	 * preferredSize; if header component is set, use the header component size
	 * to determine cell dimension, and then determine the preferred size. if
	 * border of the component is set, the preferredSize will be incremented to
	 * include the border insets.
	 */
	private void resize() {
		Border border = getBorder();
		int aw, ah;
		if (border != null) {
			Insets insets = border.getBorderInsets(this);
			aw = insets.left + insets.right;
			ah = insets.top + insets.bottom;
			basex = insets.left;
			basey = insets.top;
		} else {
			aw = ah = basex = basey = 0;
		}
		if (headerComponent == null) {
			pSize.width = cellWidth * 7 + aw;
			pSize.height = cellHeight * 7 + ah;
		} else {
			Dimension d = headerComponent.getPreferredSize();
			cellWidth = d.width / 7;
			cellHeight = d.height;
			pSize.width = cellWidth * 7 + d.width % 7 + aw;
			pSize.height = cellHeight * 7 + ah;
			headerComponent.setBounds(basex, basey, d.width, d.height);
		}
		super.setPreferredSize(pSize);
	}

	/**
	 * Override parent class setBorder method to make sure border insets is
	 * included.
	 */
	public void setBorder(Border border) {
		super.setBorder(border);
		resize();
	}

	/**
	 * Bean method. If header component is set, the default header will not
	 * display anymore.
	 */
	public void setHeaderComponent(JComponent comp) {
		removeAll();
		headerComponent = comp;
		if (comp != null) {
			add(comp);
		}
		resize();
	}

	/** Bean method */
	public JComponent getHeaderComponent() {
		return headerComponent;
	}

	/**
	 * This method is blank implemented to disable it. The size of the component
	 * is controlled by the cell dimension. Method is finalized to prevent
	 * subclass overriding.
	 */
	public final void setPreferredSize(Dimension d) {
	}

	/**
	 * Draw the graphics. Method is finalized to prevent subclass overriding.
	 */
	protected final void paintComponent(Graphics g) {
		super.paintComponent(g);

		// default header will be displayed if no header component is installed
		if (headerComponent == null) {
			for (int i = 0; i < 7; i++) {
				int shift;
				if (startOnSunday) {
					shift = i;
				} else {
					shift = (i + 6) % 7;
				}
				int x = shift * cellWidth;
				int y = 0;
				paintSymbol(g, headerFont, headerBg, headerFg,
						DEFAULT_HEADER_SYMBOLS[i], basex + x, basey + y,
						cellWidth, cellHeight);
			}
		}

		if (year != -1 && month != -1) {
			for (int i = 0; i < 42; i++) {
				int x = cellWidth * (i % 7);
				int y = cellHeight * (i / 7 + 1);
				if (dateData[i] == -1) {
					paintSymbol(g, dateFont, dateBg, dateFg, null, basex + x,
							basey + y, cellWidth, cellHeight);
				} else {
					if (hindex == i) {
						paintSymbol(g, dateFont, highBg, highFg,
								String.valueOf(dateData[i]), basex + x, basey
										+ y, cellWidth, cellHeight);
					} else {
						paintSymbol(g, dateFont, dateBg, dateFg,
								String.valueOf(dateData[i]), basex + x, basey
										+ y, cellWidth, cellHeight);
					}
				}
			}
		} // end if
	}

	/**
	 * This will fill a rectangle area specified by x, y, w, h using bg color.
	 * If String s is not null, it will also paint the specified String s at the
	 * center of the rectangle using the specified font and fg color.
	 */
	public static void paintSymbol(Graphics g, Font f, Color bg, Color fg,
			String s, int x, int y, int w, int h) {
		if (s == null) {
			g.setColor(bg);
			g.fillRect(x, y, w, h);
		} else {
			g.setFont(f);
			FontMetrics fm = g.getFontMetrics();
			int sx = (w - fm.stringWidth(s)) / 2;
			int sy = (h - fm.getHeight()) / 2 + fm.getAscent();
			g.setColor(bg);
			g.fillRect(x, y, w, h);
			g.setColor(fg);
			g.drawString(s, x + sx, y + sy);
		}
	}

	/**
	 * Reset highlighted date. Use in combination with repaint().
	 */
	public void resetHighlight() {
		hindex = -1;
	}

	public int getSelectedIndex() {
		return hindex;
	}

	/** Returns only the DAY_OF_MONTH date value */
	public int getSelectedDay() {
		return selectedDate;
	}

	/** Return YEAR+MONTH+DAY_OF_MONTH as a Calendar object */
	public Calendar getSelectedDate() {
		Calendar cal = Calendar.getInstance();
		cal.set(year, month - 1, selectedDate);
		return cal;
	}

	/**
	 * Implementation of MouseListener interface method. User program should
	 * ignore this method. Method is finalized to prevent subclass overriding.
	 */
	public final void mouseEntered(MouseEvent e) {
		if (e.getSource() != this) {
			throw new RuntimeException(
					"You are not supposed to install 'this' "
							+ "mouse handler to any component other than itself.");
		}
	}

	/**
	 * Implementation of MouseListener interface method. User program should
	 * ignore this method. Method is finalized to prevent subclass overriding.
	 */
	public final void mouseExited(MouseEvent e) {
		if (e.getSource() != this) {
			throw new RuntimeException(
					"You are not supposed to install 'this' "
							+ "mouse handler to any component other than itself.");
		}
		// hindex = -1;
		repaint();
	}

	/**
	 * Implementation of MouseListener interface method. User program should
	 * ignore this method. Method is finalized to prevent subclass overriding.
	 */
	public final void mouseClicked(MouseEvent e) {
		if (e.getSource() != this) {
			throw new RuntimeException(
					"You are not supposed to install 'this' "
							+ "mouse handler to any component other than itself.");
		}
		// get selected date
		// int x = e.getX();
		// int y = e.getY();
		// int i = x / cellWidth;
		// int j = y / cellHeight;
		// j--;

		int x = e.getX() - basex;
		int y = e.getY() - basey - cellHeight;
		int width = cellWidth * 7;
		int height = cellHeight * 6;
		if (x < 0 || x >= width || y < 0 || y >= height) {
			if (hindex != -1) {
				// hindex = -1;
				repaint();
			}
			return;
		}
		int i = x / cellWidth;
		int j = y / cellHeight;
		if (dateData[j * 7 + i] != -1) {
			hindex = j * 7 + i;
		} else {
			hindex = -1;
		}

		if (j >= 0 && dateData[j * 7 + i] != -1) {
			selectedDate = dateData[j * 7 + i];
			dateSelected();
		} else {
			selectedDate = 0;
		}
		repaint();
	}

	/**
	 * Implementation of MouseListener interface method. User program should
	 * ignore this method. Method is finalized to prevent subclass overriding.
	 */
	public final void mousePressed(MouseEvent e) {
		if (e.getSource() != this) {
			throw new RuntimeException(
					"You are not supposed to install 'this' "
							+ "mouse handler to any component other than itself.");
		}
	}

	/**
	 * Implementation of MouseListener interface method. User program should
	 * ignore this method. Method is finalized to prevent subclass overriding.
	 */
	public final void mouseReleased(MouseEvent e) {
		if (e.getSource() != this) {
			throw new RuntimeException(
					"You are not supposed to install 'this' "
							+ "mouse handler to any component other than itself.");
		}
	}

	/**
	 * Implementation of MouseMotionListener interface method. User program
	 * should ignore this method. Method is finalized to prevent subclass
	 * overriding.
	 */
	public final void mouseMoved(MouseEvent e) {
		if (e.getSource() != this) {
			throw new RuntimeException(
					"You are not supposed to install 'this' "
							+ "mouse motion handler to any component other than itself.");
		}
		mousePositionChanged(e);
	}

	/**
	 * Implementation of MouseMotionListener interface method. User program
	 * should ignore this method. Method is finalized to prevent subclass
	 * overriding.
	 */
	public final void mouseDragged(MouseEvent e) {
		if (e.getSource() != this) {
			throw new RuntimeException(
					"You are not supposed to install 'this' "
							+ "mouse motion handler to any component other than itself.");
		}
		mousePositionChanged(e);
	}

	/** Called by mouseDragged and mouseMoved method */
	private void mousePositionChanged(MouseEvent e) {
		// int x = e.getX() - basex;
		// int y = e.getY() - basey - cellHeight;
		// int width = cellWidth * 7;
		// int height = cellHeight * 6;
		// if (x < 0 || x >= width || y < 0 || y >= height) {
		// if (hindex != -1) {
		// hindex = -1;
		// repaint();
		// }
		// return;
		// }
		// int i = x / cellWidth;
		// int j = y / cellHeight;
		// if (dateData[j * 7 + i] != -1) {
		// hindex = j * 7 + i;
		// }
		// else {
		// hindex = -1;
		// }
		// repaint();
	}

	/**
	 * This method will be called when the "date selection event" occurs. The
	 * implementation of the method is left empty. Subclasses should override
	 * this method to specify "event handling code".
	 */
	protected void dateSelected() {
	}

}
