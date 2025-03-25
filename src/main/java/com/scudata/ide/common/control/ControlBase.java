package com.scudata.ide.common.control;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.scudata.cellset.ICellSet;
import com.scudata.common.Area;
import com.scudata.ide.common.GV;

/**
 * The base class of the edit control
 *
 */
public abstract class ControlBase extends JScrollPane {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 */
	public ControlBase() {
		super(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	/**
	 * Get the cellset being edited
	 * 
	 * @return
	 */
	public abstract ICellSet getICellSet();

	/**
	 * Get the copy area
	 * 
	 * @return
	 */
	public Area getCopySourceArea() {
		if (GV.cellSelection != null) {
			if (GV.cellSelection.srcCellSet == getICellSet()) {
				return GV.cellSelection.getSelectArea();
			}
		}

		return null;
	}

	/**
	 * After the command is executed, the copy status is cleared. Redraw the
	 * control.
	 * 
	 * @param cs
	 *            CellSelection
	 */
	public void resetCellSelection(CellSelection cs) {
		if (cs == null && GV.cellSelection != null
				&& GV.cellSelection.srcCellSet != getICellSet()) {
			return;
		}
		GV.cellSelection = cs;
		repaint();
	}
}
