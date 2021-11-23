package com.scudata.ide.vdb.menu;

public class ToolbarVDB extends ToolbarFactory {
	private static final long serialVersionUID = 1L;

	public ToolbarVDB() {
		super();
		add(getButton(GCMenu.iCONN_NEW, GCMenu.CONN_NEW));
		add(getButton(GCMenu.iCONN_OPEN, GCMenu.CONN_OPEN));
		add(getButton(GCMenu.iCONN_SAVE, GCMenu.CONN_SAVE));
		add(getButton(GCMenu.iCONN_CLOSE, GCMenu.CONN_CLOSE));
		add(getButton(GCMenu.iCONN_CONFIG, GCMenu.CONN_CONFIG));
	}
	
	public void disableAll(){
		short[] cmdId = new short[]{GCMenu.iCONN_OPEN,GCMenu.iCONN_SAVE,GCMenu.iCONN_CLOSE,
				GCMenu.iCONN_CONFIG};
		this.setButtonsEnabled(cmdId, false);
	}
}
