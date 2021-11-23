package com.scudata.ide.common;

import java.awt.Dimension;
import java.beans.PropertyVetoException;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import com.scudata.cellset.ICellSet;

/**
 * The sheet object in the IDE
 *
 */
public abstract class IPrjxSheet extends JInternalFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * Title pane
	 */
	private JComponent titlePane = null;
	/**
	 * The height of the title pane
	 */
	private int titleHeight = 25;
	/**
	 * Window is forced to maximize
	 */
	private boolean isForceMax = false;
	/**
	 * The last state of the window was an icon
	 */
	private boolean lastIsIcon = false;
	/**
	 * Creation time
	 */
	private long createTime = System.currentTimeMillis();

	/**
	 * Constructor
	 * 
	 * @param filePath
	 *            File path
	 */
	public IPrjxSheet(String filePath) {
		super(filePath, true, true, true, true);
		if (getUI() instanceof BasicInternalFrameUI) {
			titlePane = ((BasicInternalFrameUI) getUI()).getNorthPane();
			if (titlePane != null)
				titleHeight = titlePane.getHeight();
			if (titleHeight < 10)
				titleHeight = 25;
		}
	}

	/**
	 * Get the height of the title pane
	 * 
	 * @return
	 */
	public int getTitleHeight() {
		return titleHeight;
	}

	/**
	 * Reset sheet style
	 */
	public void resetSheetStyle() {
		if (titlePane != null)
			if (ConfigOptions.bViewWinList.booleanValue() && isMaximum()
					&& !isIcon()) {
				titlePane.setMaximumSize(new Dimension(GV.appFrame.getWidth(),
						0));
				titlePane.setPreferredSize(new Dimension(
						GV.appFrame.getWidth(), 0));
				titlePane.setVisible(false);
			} else {
				titlePane.setMaximumSize(new Dimension(GV.appFrame.getWidth(),
						titleHeight));
				titlePane.setPreferredSize(new Dimension(
						GV.appFrame.getWidth(), titleHeight));
				titlePane.setVisible(true);
			}
	}

	/**
	 * Set window maximized
	 */
	public void setMaximum(boolean b) throws PropertyVetoException {
		super.setMaximum(b);
		resetSheetStyle();
		GV.toolWin.refreshSheet(this);
	}

	/**
	 * Set to force the window to be maximized
	 */
	public void setForceMax() {
		isForceMax = true;
		lastIsIcon = isIcon();
	}

	/**
	 * Restore sheet
	 */
	public void resumeSheet() {
		if (isForceMax) {
			isForceMax = false;
			try {
				setMaximum(false);
			} catch (PropertyVetoException e) {
				e.printStackTrace();
			}
			if (lastIsIcon)
				try {
					setIcon(true);
				} catch (PropertyVetoException e) {
					e.printStackTrace();
				}
		}
	}

	/**
	 * Get creation time
	 * 
	 * @return
	 */
	public long getCreateTime() {
		return createTime;
	}
	/**
	 * Get sheet image icon, default return null,then use appframe icon
	 * @return
	 */
	public ImageIcon getSheetImageIcon(){
		return null;
	}

	/**
	 * Execute command
	 * 
	 * @param cmd
	 * @throws Exception
	 */
	public abstract void executeCmd(short cmd) throws Exception;

	/**
	 * Close sheet
	 * 
	 * @return
	 */
	public abstract boolean close();

	/**
	 * Get file name
	 * 
	 * @return
	 */
	public abstract String getFileName();

	/**
	 * Rename file
	 * 
	 * @param newName
	 */
	public abstract void changeFileName(String newName);

	/**
	 * Get sheet title
	 * 
	 * @return
	 */
	public abstract String getSheetTitle();

	/**
	 * Set sheet title
	 * 
	 * @param gridName
	 */
	public abstract void setSheetTitle(String gridName);

	/**
	 * Save
	 * 
	 * @return
	 */
	public abstract boolean save();

	/**
	 * Save as
	 * 
	 * @return
	 */
	public abstract boolean saveAs();

	/**
	 * Get cell set
	 * 
	 * @return
	 */
	public abstract ICellSet getCellSet();

	/**
	 * Set cell set
	 * 
	 * @param cellSet
	 */
	public abstract void setCellSet(Object cellSet);

	/**
	 * Set file changed
	 * 
	 * @param isChanged
	 */
	public abstract void setChanged(boolean isChanged);

	/**
	 * Refresh sheet
	 */
	public abstract void refresh();

	/**
	 * Submit cell editor
	 * 
	 * @return
	 */
	public abstract boolean submitEditor();
}
