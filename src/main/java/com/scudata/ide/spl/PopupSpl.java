package com.scudata.ide.spl;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuListener;

import com.scudata.ide.common.GV;

/**
 * 右键弹出菜单
 *
 */
public class PopupSpl {
	/**
	 * 弹出菜单监听器
	 */
	PopupMenuListener listener = null;

	/**
	 * 构造函数
	 */
	public PopupSpl() {
	}

	/**
	 * 增加弹出菜单监听器
	 * 
	 * @param listener
	 */
	public void addPopupMenuListener(PopupMenuListener listener) {
		this.listener = listener;
	}

	/**
	 * 取右键弹出菜单
	 * 
	 * @param selectStatus
	 *            网格选择的状态，GCSpl中定义的常量
	 * @return
	 */
	public JPopupMenu getSplPop(byte selectStatus) {
		MenuSpl mSpl = (MenuSpl) GV.appMenu;
		JPopupMenu pm = new JPopupMenu();
		pm.add(mSpl.cloneMenuItem(GCSpl.iCUT));
		pm.add(mSpl.cloneMenuItem(GCSpl.iCOPY));
		pm.add(mSpl.cloneMenuItem(GCSpl.iPASTE));
		pm.addSeparator();

		switch (selectStatus) {
		case GCSpl.SELECT_STATE_CELL:
			pm.add(mSpl.cloneMenuItem(GCSpl.iCTRL_ENTER));
			pm.add(mSpl.cloneMenuItem(GCSpl.iDUP_ROW));
			pm.add(mSpl.cloneMenuItem(GCSpl.iDUP_ROW_ADJUST));
			pm.addSeparator();
			pm.add(mSpl.cloneMenuItem(GCSpl.iCLEAR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iFULL_CLEAR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iCLEAR_VALUE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iTEXT_EDITOR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iNOTE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iTIPS));
			pm.addSeparator();
			pm.add(mSpl.cloneMenuItem(GCSpl.iEDIT_CHART));
			pm.add(mSpl.cloneMenuItem(GCSpl.iFUNC_ASSIST));

			pm.add(mSpl.cloneMenuItem(GCSpl.iDRAW_CHART));
			JMenuItem calcArea = mSpl.cloneMenuItem(GCSpl.iCALC_AREA);
			calcArea.setVisible(true);
			pm.add(calcArea);
			break;
		case GCSpl.SELECT_STATE_COL:
			pm.add(mSpl.cloneMenuItem(GCSpl.iINSERT_COL));
			pm.add(mSpl.cloneMenuItem(GCSpl.iADD_COL));
			pm.add(mSpl.cloneMenuItem(GCSpl.iDELETE_COL));
			pm.addSeparator();
			pm.add(mSpl.cloneMenuItem(GCSpl.iCLEAR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iFULL_CLEAR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iCLEAR_VALUE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iTEXT_EDITOR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iNOTE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iTIPS));
			pm.addSeparator();
			pm.add(mSpl.cloneMenuItem(GCSpl.iCOL_WIDTH));
			pm.add(mSpl.cloneMenuItem(GCSpl.iCOL_HIDE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iCOL_VISIBLE));
			break;
		case GCSpl.SELECT_STATE_ROW:
			pm.add(mSpl.cloneMenuItem(GCSpl.iCTRL_ENTER));
			pm.add(mSpl.cloneMenuItem(GCSpl.iDELETE_ROW));
			pm.addSeparator();
			pm.add(mSpl.cloneMenuItem(GCSpl.iCLEAR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iFULL_CLEAR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iCLEAR_VALUE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iTEXT_EDITOR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iNOTE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iTIPS));
			pm.addSeparator();
			pm.add(mSpl.cloneMenuItem(GCSpl.iROW_HEIGHT));
			pm.add(mSpl.cloneMenuItem(GCSpl.iROW_HIDE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iROW_VISIBLE));
			break;
		default:
			pm.add(mSpl.cloneMenuItem(GCSpl.iCLEAR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iFULL_CLEAR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iCLEAR_VALUE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iTEXT_EDITOR));
			pm.add(mSpl.cloneMenuItem(GCSpl.iNOTE));
			pm.add(mSpl.cloneMenuItem(GCSpl.iTIPS));
			break;
		}
		if (listener != null) {
			pm.addPopupMenuListener(listener);
		}
		return pm;
	}
}
