package com.scudata.ide.common.control;

import java.util.ArrayList;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.IColCell;
import com.scudata.cellset.IRowCell;
import com.scudata.common.Area;
import com.scudata.common.Matrix;
import com.scudata.ide.common.GC;

/**
 * Cell selection
 *
 */
public class CellSelection {
	/**
	 * Matrix of selected cells
	 */
	public Matrix matrix;
	/**
	 * Selected area
	 */
	public CellRect rect;
	/**
	 * System clipboard. In order to determine whether the IDE copied.
	 */
	public Object systemClip;
	/**
	 * List of selected RowCell
	 */
	public ArrayList<IRowCell> rowHeaderList;
	/**
	 * List of selected ColCell
	 */
	public ArrayList<IColCell> colHeaderList;

	/**
	 * The original cell set of the selected cells
	 */
	public ICellSet srcCellSet;

	/**
	 * The original data of the selected area. The data is restored to the
	 * original data when used for undo.
	 */
	public Matrix oldData;

	/**
	 * Whether to copy the value in the cell when pasting
	 */
	private boolean isCopyValue = false;

	/**
	 * Is cut
	 */
	private boolean isCutStatus = false;

	/**
	 * Whether to dynamically adjust the cell expression
	 */
	private boolean isAdjustSelf = false;

	/**
	 * Whether to paste the format
	 */
	private boolean isPasteFormat = false;

	/**
	 * Select state. The value is the selection state constants defined in GC
	 */
	public byte selectState = GC.SELECT_STATE_CELL;

	/**
	 * Constructor
	 * 
	 * @param matrix
	 *            Matrix of selected cells
	 * @param rect
	 *            Selected area
	 * @param srcCellSet
	 *            The original cell set of the selected cells
	 */
	public CellSelection(Matrix matrix, CellRect rect, ICellSet srcCellSet) {
		this(matrix, rect, srcCellSet, false);
	}

	/**
	 * Constructor
	 * 
	 * @param matrix
	 *            Matrix of selected cells
	 * @param rect
	 *            Selected area
	 * @param srcCellSet
	 *            The original cell set of the selected cells
	 * @param copyValue
	 *            Whether to copy the value in the cell when pasting
	 */
	public CellSelection(Matrix matrix, CellRect rect, ICellSet srcCellSet,
			boolean copyValue) {
		this.matrix = matrix;
		this.rect = rect;
		this.srcCellSet = srcCellSet;
		this.isCopyValue = copyValue;
	}

	/**
	 * Set to cut mode
	 */
	public void setCutStatus() {
		isCutStatus = true;
	}

	/**
	 * Is cut
	 * 
	 * @return Return true to cut, otherwise paste
	 */
	public boolean isCutStatus() {
		return isCutStatus;
	}

	/**
	 * Set whether to copy the value in the cell when pasting
	 * 
	 * @param copyValue
	 */
	public void setCopyValue(boolean copyValue) {
		this.isCopyValue = copyValue;
	}

	/**
	 * Whether to copy the value in the cell when pasting
	 * 
	 * @return Return true to paste value, otherwise paste expression
	 */
	public boolean isCopyValue() {
		return isCopyValue;
	}

	/**
	 * Set whether to dynamically adjust the cell expression
	 * 
	 * @param adjust
	 */
	public void setAdjustSelf(boolean adjust) {
		this.isAdjustSelf = adjust;
	}

	/**
	 * Whether to dynamically adjust the cell expression
	 * 
	 * @return
	 */
	public boolean isAdjustSelf() {
		return isAdjustSelf;
	}

	/**
	 * Set whether to paste the format
	 * 
	 * @param format
	 */
	public void setPasteFormat(boolean format) {
		this.isPasteFormat = format;
	}

	/**
	 * Whether to paste the format
	 * 
	 * @return
	 */
	public boolean isPasteFormat() {
		return isPasteFormat;
	}

	/**
	 * Get selected state
	 * 
	 * @return
	 */
	public byte getSelectState() {
		return selectState;
	}

	/**
	 * Set selected state
	 * 
	 * @param selectState
	 *            Selection state constants defined in GC
	 */
	public void setSelectState(byte selectState) {
		this.selectState = selectState;
	}

	/**
	 * Get selected area
	 * 
	 * @return
	 */
	public Area getSelectArea() {
		return rect.area;
	}

}
