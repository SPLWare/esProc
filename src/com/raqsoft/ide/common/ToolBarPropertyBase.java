package com.raqsoft.ide.common;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.raqsoft.common.CellLocation;
import com.raqsoft.common.IByteMap;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Context;
import com.raqsoft.ide.common.control.FuncWindow;
import com.raqsoft.ide.common.resources.IdeCommonMessage;
import com.raqsoft.ide.common.swing.JTextPaneEx;
import com.raqsoft.ide.common.swing.ToolbarGradient;
import com.raqsoft.ide.dfx.GVDfx;

/**
 * The base class of the IDE toolbar property
 *
 */
public abstract class ToolBarPropertyBase extends ToolbarGradient {
	private static final long serialVersionUID = 1L;

	/**
	 * Button size
	 */
	public static final Dimension BT_SIZE = new Dimension(20, 25);
	/**
	 * Size of cell name
	 */
	public static final Dimension CELLPOS_SIZE = new Dimension(60, 25);
	/**
	 * Size of cell expression
	 */
	public static final Dimension CELLEXP_SIZE = new Dimension(400, 25);

	/**
	 * Equal sign icon
	 */
	public static final String IMAGE_EQUAL = "t_equal.gif";

	/**
	 * Cell name text box
	 */
	protected JTextField cellName = new JTextField();

	/**
	 * Edit button
	 */
	protected SpeedButton btEdit;

	/**
	 * Selected state
	 */
	protected byte selectState;

	/**
	 * Prevent action execution
	 */
	public boolean preventAction = false;

	/**
	 * Expression edit box
	 */
	protected JTextPaneEx textEditor = new JTextPaneEx() {
		private static final long serialVersionUID = 1L;

		public void requestFocus() {
			editorSelected();
			super.requestFocus();
		}
	};

	/**
	 * Common MessageManager
	 */
	protected MessageManager mm = IdeCommonMessage.get();

	/**
	 * The font of the expression edit box
	 */
	protected Font textEditorFont = GC.font;

	/**
	 * Function window
	 */
	protected FuncWindow funcWindow = GV.getFuncWindow();

	/**
	 * JScrollPane of the expression edit box
	 */
	protected JScrollPane spEditor = new JScrollPane(textEditor);

	/**
	 * Expand and collapse button
	 */
	protected JButton jBExt = new JButton();

	/**
	 * Constructor
	 */
	public ToolBarPropertyBase() {
		super(IdeCommonMessage.get().getMessage("public.toolbar"));
		this.setFloatable(false);
		funcWindow.setVisible(true);
		GridBagConstraints gbc;
		this.setLayout(new GridBagLayout());
		setToolTipText(mm.getMessage("public.toolbar"));

		cellName.setPreferredSize(CELLPOS_SIZE);
		cellName.setMaximumSize(CELLPOS_SIZE);
		cellName.setMinimumSize(CELLPOS_SIZE);
		cellName.setToolTipText(mm.getMessage("toolbarproperty.cellname"));
		cellName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					String sCell = cellName.getText();
					boolean hasSet = false;
					String error = null;
					if (StringUtils.isValidString(sCell)) {
						sCell = sCell.toUpperCase();
						Pattern p = Pattern.compile("[A-Z]+\\d+$");
						Matcher m = p.matcher(sCell);
						if (m.matches()) {
							p = Pattern.compile("\\d");
							m = p.matcher(sCell);
							if (m.find()) {
								int index = m.start();
								String pre = sCell.substring(0, index);
								String suffix = sCell.substring(index);
								int row = Integer.parseInt(suffix);
								int col = GM.getColByName(pre);
								CellLocation maxCL = getMaxCellLocation();
								if (row > maxCL.getRow()) {
									error = mm
											.getMessage("toolbarpropertybase.invalidrow");
								} else if (col > maxCL.getCol()) {
									error = mm
											.getMessage("toolbarpropertybase.invalidcol");
								} else {
									setActiveCell(row, col);
									hasSet = true;
								}
							}
						} else {
							error = mm
									.getMessage("toolbarpropertybase.invalidcell");
						}
					}
					if (StringUtils.isValidString(error))
						JOptionPane.showMessageDialog(GV.appFrame, error);
					if (!hasSet)
						cellName.setText(getActiveCellId());
				} catch (Exception ex) {
				}
			}
		});
		gbc = GM.getGBC(1, 1);
		resetGBC(gbc);
		add(cellName, gbc);
		gbc = GM.getGBC(2, 1);
		resetGBC(gbc);
		add(getEmptyPanel(), gbc);

		btEdit = new SpeedButton(GM.getImageIcon(GC.IMAGES_PATH + IMAGE_EQUAL),
				mm.getMessage("toolbarproperty.switch"), true);

		btEdit.setMinimumSize(BT_SIZE);
		btEdit.setMaximumSize(BT_SIZE);
		gbc = GM.getGBC(1, 2);
		resetGBC(gbc);
		add(btEdit, gbc);
		gbc = GM.getGBC(2, 2);
		resetGBC(gbc);
		add(getEmptyPanel(), gbc);
		btEdit.addMouseListener(new EqualMouseListener(this));
		gbc = GM.getGBC(1, 3, true, true);
		gbc.gridheight = 2;
		resetGBC(gbc);
		add(spEditor, gbc);
		setEnabled(false);

		initProperties();
		textEditor.setEditable(true);
		textEditor.setFont(GC.font);
		textEditor.setToolTipText(mm.getMessage("toolbarproperty.cellexp"));
		KeyStroke enter = KeyStroke.getKeyStroke("ENTER");
		textEditor.getInputMap().put(enter, "none");

		textEditor.addKeyListener(funcListener);
		textEditor.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				funcWindow.caretPositionChanged(textEditor, getContext());
			}
		});

		textEditor.getDocument().addDocumentListener(new DocTextListener(this));

		textEditor.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
			}

			public void focusLost(FocusEvent e) {
				funcWindow.setFuncEnabled(false);
				funcWindow.hideWindow();
			}
		});

		jBExt.setMaximumSize(BT_SIZE);
		jBExt.setMinimumSize(BT_SIZE);
		jBExt.setPreferredSize(BT_SIZE);
		gbc = GM.getGBC(1, 4);
		resetGBC(gbc);
		add(jBExt, gbc);
		gbc = GM.getGBC(2, 4);
		resetGBC(gbc);
		add(getEmptyPanel(), gbc);
		jBExt.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_shiftdown.gif"));
		ActionListener extListener = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// isExt = !isExt;
				resetTextWindow();
				setToolBarExpand();
			}
		};
		jBExt.addActionListener(extListener);

		jBExt.registerKeyboardAction(
				extListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK
						+ InputEvent.SHIFT_DOWN_MASK),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/**
	 * Get expression edit box
	 * 
	 * @return
	 */
	public JTextPaneEx getWindowEditor() {
		return textEditor;
	}

	/**
	 * Set active cell
	 * 
	 * @param row
	 *            Row number
	 * @param col
	 *            Column number
	 */
	protected abstract void setActiveCell(int row, int col);

	/**
	 * Get the maximum cell position
	 * 
	 * @return
	 */
	protected abstract CellLocation getMaxCellLocation();

	/**
	 * Set the icon of Expand and collapse button
	 * 
	 * @param isExt
	 *            Is expanded
	 */
	public void setExtendButtonIcon(boolean isExt) {
		if (isExt) {
			jBExt.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_shiftup.gif"));
		} else {
			jBExt.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_shiftdown.gif"));
		}
	}

	/**
	 * Get a empty panel
	 * 
	 * @return
	 */
	private JPanel getEmptyPanel() {
		JPanel p = new JPanel();
		Dimension d = new Dimension(0, 0);
		p.setMaximumSize(d);
		p.setMinimumSize(d);
		p.setPreferredSize(d);
		return p;
	}

	/**
	 * Expand toolbar
	 */
	protected abstract void setToolBarExpand();

	/**
	 * Reset GridBagConstraints
	 * 
	 * @param gbc
	 */
	private void resetGBC(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 0, 0, 0);
	}

	/**
	 * Function KeyListener
	 */
	protected KeyListener funcListener = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				if (e.isAltDown() || e.isShiftDown()) {
					return;
				} else if (e.isControlDown()) {
					GM.addText(textEditor, "\n");
					textEdited(e);
				} else {
					if (GVDfx.matchWindow != null) {
						GVDfx.matchWindow.selectName();
						return;
					}
					submitEditor(textEditor.getText(),
							e.isControlDown() ? FORWARD_NONE
									: (e.isShiftDown() ? UPWARD : DOWNWARD));
				}
				e.consume();
			} else if (e.getKeyCode() == KeyEvent.VK_TAB) {
				tabPressed();
			} else if (e.isAltDown()) {
				if (e.getKeyCode() == KeyEvent.VK_UP
						|| e.getKeyCode() == KeyEvent.VK_DOWN) {
					e.consume();
				}
			}
		}

		public void keyReleased(KeyEvent e) {
			if (e.isAltDown()) {
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					if (funcWindow.isDisplay()) {
						funcWindow.showNextFunc(textEditor);
					} else {
						funcWindow.setFuncEnabled(true);
						resetTextWindow();
					}
				} else if (e.getKeyCode() == KeyEvent.VK_UP) {
					funcWindow.setFuncEnabled(false);
					funcWindow.hideWindow();
				}
				e.consume();
			} else if (e.isControlDown()
					&& (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_SHIFT)) {
				return;
			} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				editCancel();
				return;
			} else if (e.getKeyCode() != KeyEvent.VK_ALT) {
				funcWindow.caretPositionChanged(textEditor, getContext());
			}
		}
	};

	/**
	 * The expression has been modified.
	 * 
	 * @return
	 */
	public void changedUpdate(DocumentEvent e) {
		if (preventAction) {
			return;
		}
		GV.cmdSender = this;
		textEdited(null);
	}

	/**
	 * Document listener of the expression editor
	 *
	 */
	class DocTextListener implements DocumentListener {
		private ToolBarPropertyBase adaptee;

		public DocTextListener(ToolBarPropertyBase adaptee) {
			this.adaptee = adaptee;
		}

		public void changedUpdate(DocumentEvent e) {
			adaptee.changedUpdate(e);
		}

		public void insertUpdate(DocumentEvent e) {
			adaptee.changedUpdate(e);
		};

		public void removeUpdate(DocumentEvent e) {
			adaptee.changedUpdate(e);
		};
	}

	/**
	 * TAB pressed
	 */
	public abstract void tabPressed();

	/**
	 * Refresh toolbar
	 * 
	 * @param selectState
	 * @param values
	 */
	public abstract void refresh(byte selectState, IByteMap values);

	/**
	 * Set the text of the expression edit box
	 * 
	 * @param newText
	 */
	public abstract void setTextEditorText(String newText);

	/**
	 * Cell expression edited
	 * 
	 * @param e
	 */
	public abstract void textEdited(KeyEvent e);

	/**
	 * Edit canceled
	 */
	public abstract void editCancel();

	/**
	 * Expression editor selected
	 */
	public abstract void editorSelected();

	/**
	 * Get context
	 * 
	 * @return
	 */
	public abstract Context getContext();

	/**
	 * Get active cell ID
	 * 
	 * @return
	 */
	protected abstract String getActiveCellId();

	/**
	 * Cursor direction after cell submission
	 */
	/** Cursor doesn't move */
	protected static final byte FORWARD_NONE = 0;
	/** Move the cursor to the upper cell */
	protected static final byte UPWARD = 1;
	/** Move the cursor to the cell below */
	protected static final byte DOWNWARD = 2;

	/**
	 * Edit completed and submitted
	 * 
	 * @param newText
	 *            New text
	 * @param forward
	 *            Cursor direction:FORWARD_NONE,UPWARD,DOWNWARD
	 */
	public abstract void submitEditor(String newText, byte forward);

	/**
	 * Get the text being edited
	 * 
	 * @return
	 */
	public String getEditingText() {
		return textEditor.getText();
	}

	/**
	 * Reset text window
	 */
	public void resetTextWindow() {
		resetTextWindow(false);
	}

	/**
	 * Reset text window
	 * 
	 * @param resizeFuncWin
	 *            Resize the function window
	 */
	public void resetTextWindow(boolean resizeFuncWin) {
		resetTextWindow(resizeFuncWin, false);
	}

	/**
	 * Reset text window
	 * 
	 * @param resizeFuncWin
	 *            Resize the function window
	 * @param frameResize
	 *            Frame resized
	 */
	public void resetTextWindow(boolean resizeFuncWin, boolean frameResize) {
		Rectangle srcRect = spEditor.getBounds();
		int x = GV.appFrame.getX() + GM.getAbsolutePos(spEditor, true) + 5;
		int y = GM.getAbsolutePos(spEditor, false) + 2;
		int w = (int) srcRect.getWidth() - 3;
		int h;
		h = srcRect.height;
		textEditor.setBorder(null);
		if (funcWindow.isFuncEnabled()) {
			funcWindow.setPosition(x, y + h + 10, w);
			funcWindow.caretPositionChanged(textEditor, getContext(),
					resizeFuncWin);
		} else {
			funcWindow.hideWindow();
		}
	}

	/**
	 * Edit button mouse click event
	 * 
	 * @param e
	 */
	public void BtEdit_mouseClicked(MouseEvent e) {
		if (!btEdit.isEnabled()) {
			return;
		}
		if (preventAction) {
			return;
		}
		if (btEdit.isChecked()) {
			String text = textEditor.getText();
			if (text.startsWith("=")) {
				textEditor.setText(text.substring(1));
			} else {
				textEditor.setText("=" + text);
			}
			GV.cmdSender = this;
			textEditor.requestFocus();
			textEdited(null);
		}
	}

	/**
	 * Initialize the value of the components
	 */
	protected void initProperties() {
		try {
			cellName.setText("");
			textEditor.setText("");
		} catch (Exception ex) {
		}
	}

	/**
	 * Set whether the toolbar is enabled
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		cellName.setEnabled(enabled);
		spEditor.setEnabled(enabled);
		textEditor.setEnabled(enabled);
		btEdit.setEnabled(enabled);
		jBExt.setEnabled(enabled);

		if (enabled) {
			resetTextWindow();
		} else {
			try {
				preventAction = true;
				cellName.setText(null);
				textEditor.setText("");
				funcWindow.setFuncEnabled(false);
				funcWindow.hideWindow();
			} catch (Exception ex) {
			} finally {
				preventAction = false;
			}
		}
	}

	// 类名:SpeedButton
	// 说明:当鼠标进入时，按钮突起，当鼠标离开时，按钮变成扁平
	// 当toggle=true时，按钮具有JToggle按钮特性

	/**
	 * When the mouse enters, the button protrudes; when the mouse leaves, the
	 * button becomes flat. When toggle=true, the button has the characteristics
	 * of the JToggle button.
	 */
	class SpeedButton extends JLabel {
		private static final long serialVersionUID = 1L;

		class BtMouseAdapter extends MouseAdapter {
			private SpeedButton adaptee;

			public BtMouseAdapter(SpeedButton button) {
				adaptee = button;
			}

			public void mouseClicked(MouseEvent e) {
				adaptee.button_mouseClicked(e);
			}

			public void mouseEntered(MouseEvent e) {
				adaptee.button_mouseEntered(e);
			}

			public void mouseExited(MouseEvent e) {
				adaptee.button_mouseExited(e);
			}

			public void mousePressed(MouseEvent e) {
				adaptee.button_mousePressed(e);
			}

			public void mouseReleased(MouseEvent e) {
				adaptee.button_mouseReleased(e);
			}

		}

		private boolean bChecked = true;

		public SpeedButton(Icon icon, String tip, boolean bToggle) {
			super(icon);
			this.setToolTipText(tip);
			this.addMouseListener(new BtMouseAdapter(this));
		}

		public boolean isChecked() {
			return bChecked;
		}

		public void button_mouseClicked(MouseEvent e) {
			if (!isEnabled()) {
				return;
			}
		}

		public void button_mouseEntered(MouseEvent e) {

			if (!isEnabled()) {
				return;
			}
			this.setBorder(BorderFactory.createRaisedBevelBorder());
		}

		public void button_mouseExited(MouseEvent e) {

			if (!isEnabled()) {
				return;
			}
			this.setBorder(BorderFactory.createEmptyBorder());
		}

		public void button_mousePressed(MouseEvent e) {
			if (!isEnabled()) {
				return;
			}
			this.setBorder(BorderFactory.createLoweredBevelBorder());
		}

		public void button_mouseReleased(MouseEvent e) {
			if (!isEnabled()) {
				return;
			}
			this.setBorder(BorderFactory.createRaisedBevelBorder());
		}

	}

	/**
	 * Mouse click event of equal sign button
	 *
	 */
	class EqualMouseListener extends MouseAdapter {
		ToolBarPropertyBase adaptee;

		public EqualMouseListener(ToolBarPropertyBase adaptee) {
			this.adaptee = adaptee;
		}

		public void mouseClicked(MouseEvent e) {
			adaptee.BtEdit_mouseClicked(e);
		}
	}

}
