package com.scudata.ide.common.control;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JViewport;

import com.scudata.app.common.StringUtils2;
import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.IStyle;
import com.scudata.common.Area;
import com.scudata.common.CellLocation;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.StringSplit;

/**
 * The basic tool class of the control
 *
 */
public class ControlUtilsBase {
	/**
	 * Adjust the starting row and column in the area object. So that the
	 * starting row and column are always smaller than the ending row and
	 * column.
	 * 
	 * @param area
	 *            Area
	 * @return Area
	 */
	public static Area adjustArea(Area area) {
		int startRow = area.getBeginRow();
		int endRow = area.getEndRow();
		int startCol = area.getBeginCol();
		int endCol = area.getEndCol();
		if (startRow > endRow) {
			int tmp = startRow;
			startRow = endRow;
			endRow = tmp;
			area.setBeginRow(startRow);
			area.setEndRow(endRow);
		}
		if (startCol > endCol) {
			int tmp = startCol;
			startCol = endCol;
			endCol = tmp;
			area.setBeginCol(startCol);
			area.setEndCol(endCol);
		}
		return area;

	}

	/**
	 * Get all cell locations in the selected area
	 * 
	 * @param area
	 *            Selected area
	 * @return
	 */
	public static Vector<CellLocation> listSelectedCells(Area area) {
		Vector<CellLocation> cells = new Vector<CellLocation>();
		if (area != null) {
			for (int r = area.getBeginRow(); r <= area.getEndRow(); r++) {
				for (int c = area.getBeginCol(); c <= area.getEndCol(); c++) {
					cells.add(new CellLocation(r, c));
				}
			}
		}
		return cells;
	}

	/**
	 * Get all cell locations in the selected areas or cell rectangles
	 * 
	 * @param selectedAreas
	 * @return
	 */
	public static Vector<CellLocation> listSelectedCells(
			Vector<Object> selectedAreas) {
		Vector<CellLocation> cells = new Vector<CellLocation>();
		for (int i = 0; i < selectedAreas.size(); i++) {
			Object areaOrRect = selectedAreas.get(i);
			if (areaOrRect == null) {
				continue;
			}
			Area area;
			if (areaOrRect instanceof CellRect) {
				area = ((CellRect) areaOrRect).getArea();
			} else if (areaOrRect instanceof Area) {
				area = (Area) areaOrRect;
			} else {
				continue;
			}
			cells.addAll(listSelectedCells(area));
		}
		return cells;
	}

	/**
	 * Get column numbers in the selected areas or cell rectangles
	 * 
	 * @param selectedAreas
	 * @return
	 */
	public static HashSet<Integer> listSelectedCols(Vector<Object> selectedAreas) {
		HashSet<Integer> cols = new HashSet<Integer>();

		for (int i = 0; i < selectedAreas.size(); i++) {
			Object areaOrRect = selectedAreas.get(i);
			if (areaOrRect == null) {
				continue;
			}
			Area area;
			if (areaOrRect instanceof CellRect) {
				area = ((CellRect) areaOrRect).getArea();
			} else if (areaOrRect instanceof Area) {
				area = (Area) areaOrRect;
			} else {
				continue;
			}
			for (int c = area.getBeginCol(); c <= area.getEndCol(); c++) {
				Integer col = new Integer(c);
				cols.add(col);
			}
		}
		return cols;
	}

	/**
	 * Get row numbers in the selected areas or cell rectangles
	 * 
	 * @param selectedAreas
	 * @return
	 */
	public static HashSet<Integer> listSelectedRows(Vector<Object> selectedAreas) {
		HashSet<Integer> rows = new HashSet<Integer>();
		for (int i = 0; i < selectedAreas.size(); i++) {
			Object areaOrRect = selectedAreas.get(i);
			if (areaOrRect == null) {
				continue;
			}
			Area area;
			if (areaOrRect instanceof CellRect) {
				area = ((CellRect) areaOrRect).getArea();
			} else if (areaOrRect instanceof Area) {
				area = (Area) areaOrRect;
			} else {
				continue;
			}
			for (int r = area.getBeginRow(); r <= area.getEndRow(); r++) {
				Integer row = new Integer(r);
				rows.add(row);
			}
		}
		return rows;
	}

	/**
	 * Determine whether the specified cell is located in the display window, if
	 * not, scroll to the display window
	 * 
	 * @param viewport
	 *            JViewport
	 * @param fieldArea
	 *            Rectangle
	 * @return When the cell is in the display window, false is returned.
	 *         Otherwise, after scrolling to the display window, return true.
	 */
	public static boolean scrollToVisible(JViewport viewport,
			Rectangle fieldArea) {
		Rectangle viewArea = viewport.getViewRect();
		if (viewArea.contains(fieldArea)) {
			return false;
		}
		Point pos = new Point();
		pos.x = viewArea.x;
		if (fieldArea.x + fieldArea.width > viewArea.x + viewArea.width) {
			pos.x = fieldArea.x + fieldArea.width - viewArea.width + 5;
		}
		pos.x = pos.x > fieldArea.x ? fieldArea.x - 5 : pos.x;
		if (pos.x + viewArea.width > viewport.getView().getWidth()) {
			pos.x = viewport.getView().getWidth() - viewArea.width;
		}
		pos.y = viewArea.y;
		if (fieldArea.y + fieldArea.height > viewArea.y + viewArea.height) {
			pos.y = fieldArea.y + fieldArea.height - viewArea.height + 5;
		}
		pos.y = pos.y > fieldArea.y ? fieldArea.y - 5 : pos.y;
		if (pos.y + viewArea.height > viewport.getView().getHeight()) {
			pos.y = viewport.getView().getHeight() - viewArea.height;
		}
		viewport.setViewPosition(pos);
		return true;
	}

	/**
	 * Draw the header of the row or column
	 * 
	 * @param g
	 *            Graphics
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @param w
	 *            Width
	 * @param h
	 *            Height
	 * @param label
	 *            Text to be drawn
	 * @param scale
	 *            Display ratio
	 * @param backColor
	 *            Background color
	 * @param selectState
	 *            Select state: GC.SELECT_STATE_NONE, GC.SELECT_STATE_CELL,
	 *            GC.SELECT_STATE_ROW, GC.SELECT_STATE_COL
	 * @param editable
	 */
	public static void drawHeader(Graphics g, int x, int y, int w, int h,
			String label, float scale, Color backColor, byte selectState,
			boolean editable) {
		int fontSize = (int) (ConfigOptions.iFontSize.intValue() * scale);
		Color fontColor = Color.black;
		if (backColor.equals(Color.gray)) {
			fontColor = Color.white;
		}
		Font font = new Font(ConfigOptions.sFontName, Font.PLAIN, fontSize);
		switch (selectState) {
		case GC.SELECT_STATE_CELL:
			if (ConfigOptions.getHeaderColor() != null) {
				backColor = ConfigOptions.getHeaderColor();
			} else {
				backColor = ACTIVE_BACK_COLOR;
			}

			break;
		case GC.SELECT_STATE_ROW:
		case GC.SELECT_STATE_COL:
			fontColor = Color.white;
			backColor = SELECTED_BACK_COLOR;
			break;
		}
		if (editable) {
			if (selectState == GC.SELECT_STATE_CELL) {
				gradientPaint(g, x, y, w, h, backColor, false);
			} else {
				gradientPaint(g, x, y, w, h, backColor);
			}
		} else {
			g.clearRect(x, y, w, h);
			fontColor = Color.lightGray.darker();
		}
		g.setColor(new Color(236, 236, 236));
		g.drawLine(x, y, x + w, y);
		g.drawLine(x, y, x, y + h);
		g.setColor(Color.darkGray);
		g.drawLine(x + w, y + h, x, y + h);
		g.drawLine(x + w, y + h, x + w, y);

		g.setColor(fontColor);

		int fontW = g.getFontMetrics(font).stringWidth(label);
		g.setFont(font);
		g.drawString(label, x + (w - fontW) / 2, y + h / 2 + 5);
	}

	/**
	 * Gradient Paint
	 * 
	 * @param g
	 *            Graphics
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @param w
	 *            Width
	 * @param h
	 *            Height
	 * @param c
	 *            Color
	 */
	public static void gradientPaint(Graphics g, int x, int y, int w, int h,
			Color c) {
		gradientPaint(g, x, y, w, h, c, true);
	}

	/**
	 * ??????????????????????????????????????????????????????
	 * 
	 * @param g
	 *            Graphics
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @param w
	 *            Width
	 * @param h
	 *            Height
	 * @param c
	 *            Color
	 * @param isBrighter
	 *            Is the background color brighter
	 */
	public static void gradientPaint(Graphics g, int x, int y, int w, int h,
			Color c, boolean isBrighter) {
		Graphics2D g2 = (Graphics2D) g;
		Paint storedPaint = g2.getPaint();
		if (isBrighter) {
			g2.setPaint(new GradientPaint(x + w / 2, y + h / 2, c, x + w / 2,
					y, c.brighter()));
		} else {
			g2.setPaint(new GradientPaint(x + w / 2, y + h / 2, c, x + w / 2,
					y, c));
		}
		g2.fillRect(x, y, w, h);
		g2.setPaint(storedPaint);
	}

	/**
	 * Get the text to be displayed in the cell
	 * 
	 * @param cellSet
	 *            Cell set
	 * @param row
	 *            Row number
	 * @param col
	 *            Column number
	 * @param isEditing
	 *            Is the cell being edited
	 * @return
	 */
	public static String getCellText(ICellSet cellSet, int row, int col,
			boolean isEditing) {
		String textValue = null;
		INormalCell nc = cellSet.getCell(row, col);
		if (nc == null) {
			return "";
		}
		if (!isEditing) {
			Object o = nc.getValue();
			if (o instanceof String) {
				textValue = (String) o;
			} else if (o != null) {
				textValue = o.toString();
			}

			if (textValue != null) {
				return textValue;
			}
		}
		Object value = nc.getExpString();
		textValue = value == null ? GC.NULL : value.toString();
		if (textValue != null && textValue.length() > 0) {
			return textValue;
		}
		return "";
	}

	/**
	 * Graphics
	 */
	public static Graphics fg = null;

	/**
	 * Get the display height of the string
	 * 
	 * @param text
	 *            Text string
	 * @param fixedWidth
	 *            Maximum width
	 * @param font
	 *            Font
	 * @return
	 */
	public static float getStringHeight(String text, float fixedWidth, Font font) {
		return getStringHeight(text, fixedWidth, font, -1);
	}

	/**
	 * Get the display height of the string
	 * 
	 * @param text
	 *            Text string
	 * @param fixedWidth
	 *            Maximum width
	 * @param font
	 *            Font
	 * @param maxH
	 *            Maximum height
	 * @return
	 */
	public static float getStringHeight(String text, float fixedWidth,
			Font font, float maxH) {
		if (fg == null) {
			BufferedImage bi = new BufferedImage(50, 50,
					BufferedImage.TYPE_INT_RGB);
			fg = (Graphics2D) bi.getGraphics();
		}
		FontMetrics fm = fg.getFontMetrics(font);
		return getStringHeight(fm, text, fixedWidth, maxH);
	}

	/**
	 * Get the display height of the string
	 * 
	 * @param fm
	 *            FontMetrics
	 * @param text
	 *            Text string
	 * @param fixedWidth
	 *            Maximum width
	 * @return
	 */
	public static float getStringHeight(FontMetrics fm, String text,
			float fixedWidth) {
		return getStringHeight(fm, text, fixedWidth, -1);
	}

	/**
	 * Get the display height of the string
	 * 
	 * @param fm
	 *            FontMetrics
	 * @param text
	 *            Text string
	 * @param fixedWidth
	 *            Maximum width
	 * @param maxH
	 *            Maximum height
	 * @return
	 */
	public static float getStringHeight(FontMetrics fm, String text,
			float fixedWidth, float maxH) {
		if (fixedWidth < 1) {
			return Float.MAX_VALUE;
		}
		float linH = fm.getHeight();
		int maxRow = -1;
		if (maxH > 0) {
			maxRow = (int) (maxH / linH) + 10;
		}
		ArrayList<String> wrapString = wrapString(text, fm, (int) fixedWidth,
				maxRow);
		return wrapString.size() * linH;
	}

	/**
	 * Get the maximum width of the text display
	 * 
	 * @param text
	 *            Text string
	 * @param font
	 *            Font
	 * @return
	 */
	public static int getStringMaxWidth(String text, Font font) {
		return getStringMaxWidth(text, font, 0);
	}

	/**
	 * 
	 * Get the maximum width of the text display
	 * 
	 * @param text
	 *            Text string
	 * @param font
	 *            Font
	 * @param indent
	 *            Indent
	 * @return
	 */
	public static int getStringMaxWidth(String text, Font font, int indent) {
		if (fg == null) {
			BufferedImage bi = new BufferedImage(50, 50,
					BufferedImage.TYPE_INT_RGB);
			fg = (Graphics2D) bi.getGraphics();
		}
		FontMetrics fm = fg.getFontMetrics(font);
		return getStringMaxWidth(fm, text, font, indent);
	}

	/**
	 * Get the maximum width of the text display
	 * 
	 * @param fm
	 *            FontMetrics
	 * @param text
	 *            Text string
	 * @return
	 */
	public static int getStringMaxWidth(FontMetrics fm, String text) {
		return getStringMaxWidth(fm, text, fm.getFont());
	}

	/**
	 * Get the maximum width of the text display
	 * 
	 * @param fm
	 *            FontMetrics
	 * @param text
	 *            Text string
	 * @param font
	 *            Font
	 * @return
	 */
	public static int getStringMaxWidth(FontMetrics fm, String text, Font font) {
		return getStringMaxWidth(fm, text, font, 0);
	}

	/**
	 * Get the maximum width of the text display
	 * 
	 * @param fm
	 *            FontMetrics
	 * @param text
	 *            Text string
	 * @param font
	 *            Font
	 * @param indent
	 *            Indent
	 * @return
	 */
	public static int getStringMaxWidth(FontMetrics fm, String text, Font font,
			int indent) {
		Object hashKey = text + font.hashCode();
		ArrayList<String> wrapString = wrapStringBuffer.get(hashKey);
		if (wrapString == null) {
			wrapString = new ArrayList<String>();
			text = StringUtils.replace(text, "\\n", "\n");
			if (text.indexOf('\n') < 0) {
				wrapString.add(text);
			} else {
				StringSplit at = new StringSplit(text, '\n', true, true, true);
				while (at.hasNext()) {
					wrapString.add(at.next());
				}
			}
		}

		int w = 0, len;
		for (int i = 0; i < wrapString.size(); i++) {
			String line = (String) wrapString.get(i);
			len = fm.stringWidth(line);
			if (i == 0) {
				len += indent;
			}
			if (len > w) {
				w = len;
			}
		}

		return w;
	}

	/**
	 * ????????
	 * 
	 * @param g
	 *            Graphics
	 * @param text
	 *            ????????????
	 * @param x
	 *            X????
	 * @param y
	 *            Y????
	 * @param w
	 *            ????
	 * @param h
	 *            ????
	 * @param underLine
	 *            ????????????
	 * @param halign
	 *            ????????????
	 * @param valign
	 *            ????????????
	 * @param font
	 *            ????
	 * @param c
	 *            ????
	 * @param indent
	 *            ????
	 * @return
	 */
	public static int drawText(Graphics g, String text, int x, int y, int w,
			int h, boolean underLine, byte halign, byte valign, Font font,
			Color c, int indent) {
		return ControlUtilsBase.drawText(g, text, x, y, w, h, underLine,
				halign, valign, font, c, indent, true);
	}

	/**
	 * ????????
	 * 
	 * @param g
	 *            Graphics
	 * @param text
	 *            ????????????
	 * @param x
	 *            X????
	 * @param y
	 *            Y????
	 * @param w
	 *            ????
	 * @param h
	 *            ????
	 * @param underLine
	 *            ????????????
	 * @param halign
	 *            ????????????
	 * @param valign
	 *            ????????????
	 * @param font
	 *            ????
	 * @param c
	 *            ??????
	 * @param indent
	 *            ????
	 * @param wordWrap
	 *            ????
	 * @return
	 */
	public static int drawText(Graphics g, String text, int x, int y, int w,
			int h, boolean underLine, byte halign, byte valign, Font font,
			Color c, int indent, boolean wordWrap) {
		if (text == null || text.trim().length() == 0) {
			return 0;
		}
		g.setColor(c);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics(font);
		if (halign == IStyle.HALIGN_LEFT) {
			x = x + indent;
			w = w - indent;
		} else if (halign == IStyle.HALIGN_RIGHT) {
			w = w - indent;
		}
		int fw = fm.stringWidth(text);
		int ascent = fm.getAscent();
		int descent = fm.getDescent();

		if (!wordWrap) {
			float topSpace = (h - fm.getHeight()) / 2.0f;
			if (topSpace > 3) {
				topSpace = 3;
			}
			if (topSpace > 0) {
				y = y + (int) topSpace;
				h = h - (int) (topSpace * 2);
			}
			if (fw > w) {
				text = getCanDisplayText(text, fm, w);
				fw = stringWidth(fm, text);
			}
			int x1 = x;
			if (halign == IStyle.HALIGN_CENTER) {
				x1 = x + (w - fw) / 2;
			} else if (halign == IStyle.HALIGN_RIGHT) {
				x1 = x + w - fw;
			}
			int y1 = y + ascent;
			if (valign == IStyle.VALIGN_MIDDLE) {
				y1 = y + (h - ascent - descent) / 2 + ascent;
			} else if (valign == IStyle.VALIGN_BOTTOM) {
				y1 = y + h - descent;
			}
			outText(g, text, x1, y1, fw, underLine, font, c);
			return fw;
		} else {
			int lineH = fm.getHeight();
			int maxRow = (int) h / lineH + 10;
			ArrayList<String> al = wrapString(text, fm, w, maxRow);
			if (al.size() <= 1) {
				float topSpace = (h - fm.getHeight()) / 2.0f;
				if (topSpace > 3) {
					topSpace = 3;
				}
				if (topSpace > 0) {
					y = y + (int) topSpace;
					h = h - (int) (topSpace * 2);
				}
				if (lineH > h) {
					lineH = h;
				}
			}
			int yy = y;
			if (valign == IStyle.VALIGN_MIDDLE) {
				yy = y + (h - lineH * al.size()) / 2;
			} else if (valign == IStyle.VALIGN_BOTTOM) {
				yy = y + h - lineH * al.size();
			}
			if (yy < y) {
				yy = y;
			}
			int maxW = 0;
			for (int i = 0; i < al.size(); i++) {
				if (yy > y + h) {
					break;
				}
				if (i > 0 && (yy + lineH) > (y + h)) { // ??????????????????????????
					break;
				}
				String wrapedText = (String) al.get(i);
				fw = fm.stringWidth(wrapedText);
				int x1 = x;
				if (halign == IStyle.HALIGN_CENTER) {
					x1 = x + (w - fw) / 2;
				} else if (halign == IStyle.HALIGN_RIGHT) {
					x1 = x + w - fw;
				}
				int y1 = yy + ascent;
				drawText(g, wrapedText, x1, y1, fw, descent, underLine);
				maxW = Math.max(maxW, fw);
				yy += lineH;
			}
			return maxW;
		}
	}

	/**
	 * ????????????????????????????????
	 * 
	 * @param text
	 *            ????????????
	 * @param fm
	 *            FontMetrics
	 * @param w
	 *            ????
	 * @return
	 */
	private static String getCanDisplayText(String text, FontMetrics fm, int w) {
		String newText = text;
		int w1 = stringWidth(fm, newText);
		if (w1 < w) {
			return text;
		}
		if (w1 < w * 2) {
			while (stringWidth(fm, newText) > w) {
				if (newText.trim().length() == 0) {
					return "";
				}
				newText = newText.substring(0, newText.length() - 1);
			}
		} else {
			newText = "";
			int index = 0;
			while (stringWidth(fm, newText) < w) {
				index++;
				newText = text.substring(0, index);
			}
			if (index == 0) {
				return "";
			}
			newText = text.substring(0, index - 1);
		}
		return newText;
	}

	/**
	 * ????????????????????????????
	 */
	private static HashMap<Short, Integer> fmWidthBuf = new HashMap<Short, Integer>();

	/**
	 * ????????????????????
	 * 
	 * @param fm
	 *            FontMetrics
	 * @param text
	 *            ??????????????
	 * @return ????
	 */
	public static int stringWidth(FontMetrics fm, String text) {
		short key = (short) (fm.hashCode() + text.hashCode());
		Integer val = fmWidthBuf.get(key);
		int width;
		if (val == null) {
			width = fm.stringWidth(text);
			val = new Integer(width);
			fmWidthBuf.put(key, val);
		} else {
			width = val.intValue();
		}
		return width;
	}

	/**
	 * ????????????????????,??????PDF??????????pdfCell??????????
	 * ??????????????????????????????g????draw????Pdf??????????????????PDFCell??????draw????????????
	 * ??????????????????????????????????????????????????????????????????????????
	 * 
	 * @param g
	 *            Graphics,??????????????????
	 * @param text
	 *            String????????????
	 * @param x
	 *            int??x????
	 * @param y
	 *            int??y????
	 * @param w
	 *            int??????????
	 * @param underLine
	 *            boolean??????????????
	 * @param font
	 *            Font??????
	 * @param c
	 *            Color????????
	 */
	public static void outText(Graphics g, String text, int x, int y, int w,
			boolean underLine, Font font, Color c) {
		if (text == null || text.length() == 0) {
			return;
		}
		while (text.endsWith("\n")) {
			text = text.substring(0, text.length() - 1);
		}
		g.setColor(c);
		g.setFont(font);
		if (underLine) {
			Graphics2D g2d = (Graphics2D) g;
			Stroke bsOld = g2d.getStroke();
			g2d.setStroke(new BasicStroke(0.75f));
			g2d.setPaintMode();
			g2d.drawLine(x, y + 2, x + w, y + 2);
			g2d.setStroke(bsOld);
		}

		drawString(g, font, text, x, y);
	}

	/**
	 * ??????????
	 * 
	 * @param g
	 *            ????
	 * @param f
	 *            ????
	 * @param text
	 *            ????????????
	 * @param x
	 *            X????
	 * @param y
	 *            Y????
	 */
	private static void drawString(Graphics g, Font f, String text, int x, int y) {
		g.drawString(text, x, y);
	}

	/**
	 * ????????????????????????????????
	 */
	private static BasicStroke bstroke = new BasicStroke(0.75f);

	/**
	 * ????????????????????
	 * 
	 * @param g
	 *            ????
	 * @param text
	 *            ????????????
	 * @param x
	 *            X????
	 * @param y
	 *            Y????
	 * @param w
	 *            ????
	 * @param descent
	 *            Font descent
	 * @param underLine
	 *            ????????????
	 */
	private static void drawText(Graphics g, String text, int x, int y, int w,
			int descent, boolean underLine) {
		if (text == null || text.length() == 0) {
			return;
		}
		while (text.endsWith("\n")) {
			text = text.substring(0, text.length() - 1);
		}
		g.drawString(text, x, y);
		if (underLine) {
			Graphics2D g2d = (Graphics2D) g;
			Stroke bsOld = g2d.getStroke();
			g2d.setStroke(bstroke);
			g2d.setPaintMode();
			g2d.drawLine(x, y + descent, x + w, y + descent);
			g2d.setStroke(bsOld);
		}
	}

	/**
	 * ??????
	 */
	private static ArrayList<String> emptyArrayList = new ArrayList<String>();

	/**
	 * ??????????????????????KEY??????????VALUE??????????????????
	 */
	public static HashMap<String, ArrayList<String>> wrapStringBuffer = new HashMap<String, ArrayList<String>>();

	/**
	 * ????????????????
	 */
	public static void clearWrapBuffer() {
		wrapStringBuffer.clear();
	}

	/**
	 * ????
	 * 
	 * @param text
	 *            ????????????
	 * @param fm
	 *            FontMetrics
	 * @param w
	 *            ????
	 * @return
	 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm,
			int w) {
		return wrapString(text, fm, w, -1);
	}

	/**
	 * ????
	 * 
	 * @param text
	 *            ????????????
	 * @param fm
	 *            FontMetrics
	 * @param w
	 *            ????
	 * @param maxRow
	 *            ????????(????????????????????)
	 * @return
	 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm,
			int w, int maxRow) {
		if (!StringUtils.isValidString(text) || w < 1) {
			return emptyArrayList;
		}
		boolean isExp = text != null && text.startsWith("=");
		String hashKey = text.hashCode() + "" + fm.hashCode() + w + maxRow;
		ArrayList<String> wrapedString = wrapStringBuffer.get(hashKey);
		if (wrapedString == null) {
			if (isExp) {
				wrapedString = StringUtils2.wrapExpString(text, fm, w, false,
						maxRow);
			} else {

				text = StringUtils.replace(text, "\\n", "\n");
				text = StringUtils.replace(text, "\t", "        ");

				if (text.indexOf('\n') < 0 && fm.stringWidth(text) < w) {
					wrapedString = new ArrayList<String>();
					wrapedString.add(text);
					if (maxRow > 0 && wrapedString.size() > maxRow) {
						wrapStringBuffer.put(hashKey, wrapedString);
						return wrapedString;
					}
				} else {
					// ??jdk6??New Times Roman????????LineBreakMeasurer????jvm??????????
					// ????????????????????????????LineBreakMeasurer????????????????????????????wunan
					// 2018-05-29
					wrapedString = StringUtils2.wrapString(text, fm, w, false,
							maxRow);
				}
			}
			wrapStringBuffer.put(hashKey, wrapedString);
		}
		return wrapedString;
	}

	/**
	 * 
	 * Find the cell position index at the head of the row or column according
	 * to the cell number
	 * 
	 * @param curr
	 *            Cell number
	 * @param start
	 *            The ordinal array of the starting point of the first cell of a
	 *            row or column
	 * @param len
	 *            The height array of the first row of rows or the width array
	 *            of the first row of columns
	 * @return Return the index number of the first cell found, if not found,
	 *         return -1
	 */
	public static int lookupHeaderIndex(int curr, int[] start, int[] len) {
		for (int i = 1; i < start.length; i++) {
			if (curr > start[i] && curr <= start[i] + len[i]) {
				return i;
			}
		}
		if (curr > start[start.length - 1]
				&& curr > start[start.length - 1] + len[start.length - 1]) {
			return start.length - 1;
		}
		return -1;
	}

	/**
	 * Get byte array from input stream
	 * 
	 * @param is
	 *            InputStream
	 * @throws Exception
	 * @return
	 */
	public static byte[] getStreamBytes(InputStream is) throws Exception {
		ArrayList<byte[]> al = new ArrayList<byte[]>();
		int totalBytes = 0;
		byte[] b = new byte[102400];
		int readBytes = 0;
		while ((readBytes = is.read(b)) > 0) {
			byte[] bb = new byte[readBytes];
			System.arraycopy(b, 0, bb, 0, readBytes);
			al.add(bb);
			totalBytes += readBytes;
		}
		b = new byte[totalBytes];
		int pos = 0;
		for (int i = 0; i < al.size(); i++) {
			byte[] bb = al.get(i);
			System.arraycopy(bb, 0, b, pos, bb.length);
			pos += bb.length;
		}
		return b;
	}

	/**
	 * ????????????
	 * 
	 * @param cp
	 *            ????????
	 * @param ics
	 *            ????????
	 * @return
	 */
	public static CellLocation checkPosition(CellLocation cp, ICellSet ics) {
		if (cp == null) {
			return null;
		}
		if (cp.getRow() < 1) {
			cp.setRow(1);
		}
		if (cp.getRow() > ics.getRowCount()) {
			cp.setRow(ics.getRowCount());
		}
		if (cp.getCol() < 1) {
			cp.setCol(1);
		}
		if (cp.getCol() > ics.getColCount()) {
			cp.setCol(ics.getColCount());
		}
		return cp;
	}

	/**
	 * ??????????????
	 */
	public static Color ACTIVE_BACK_COLOR = new Color(0x80, 0xA0, 0xC0);

	/**
	 * ????????????????
	 */
	public static Color SELECTED_BACK_COLOR = new Color(0x99, 0x99, 0x66);
}
