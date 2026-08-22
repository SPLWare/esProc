package com.scudata.ide.vdb.menu;

import javax.swing.JMenu;

import com.scudata.ide.vdb.VDB;
import com.scudata.ide.vdb.VDBTree;
import com.scudata.ide.vdb.control.ConnectionConfig;
import com.scudata.ide.vdb.control.VDBTreeNode;

/**
 * 初始菜单
 * 
 * @author wunan
 *
 */
public class MenuVDB extends MenuFactory {
	private static final long serialVersionUID = 1L;

	public MenuVDB() {
		JMenu menu;
		// 连接
		menu = newMenu(GCMenu.iCONNECTION, GCMenu.CONNECTION, 'C', true);
		menu.add(newMenuItem(GCMenu.iCONN_NEW, GCMenu.CONN_NEW, 'N', Boolean.TRUE, true));
		menu.add(newMenuItem(GCMenu.iCONN_OPEN, GCMenu.CONN_OPEN, 'O', Boolean.TRUE, true));
		menu.add(newMenuItem(GCMenu.iCONN_SAVE, GCMenu.CONN_SAVE, 'S', Boolean.TRUE, true));
		menu.add(newMenuItem(GCMenu.iCONN_CLOSE, GCMenu.CONN_CLOSE, 'C', Boolean.FALSE, true));
		menu.add(newMenuItem(GCMenu.iCONN_CONFIG, GCMenu.CONN_CONFIG, 'G', Boolean.FALSE, true));
		menu.add(newMenuItem(GCMenu.iCONN_DELETE, GCMenu.CONN_DELETE, 'D', Boolean.FALSE, false));
		menu.addSeparator();
		menu.add(newMenuItem(GCMenu.iCONN_ACHEIVE, GCMenu.CONN_ACHEIVE, 'A', Boolean.FALSE, false));
		menu.add(newMenuItem(GCMenu.iCONN_PURGE, GCMenu.CONN_PURGE, 'P', Boolean.FALSE, false));
		menu.addSeparator();
		menu.add(newMenuItem(GCMenu.iCONN_EXIT, GCMenu.CONN_EXIT, 'X', Boolean.FALSE, true));
		add(menu);
		
		// 节点
		menu = newMenu(GCMenu.iNODE, GCMenu.NODE, 'N', true);
		menu.add(newMenuItem(GCMenu.iNODE_COPY, GCMenu.NODE_COPY, 'C', Boolean.FALSE, true));
		menu.add(newMenuItem(GCMenu.iNODE_PASTE, GCMenu.NODE_PASTE, 'P', Boolean.FALSE, true));
		menu.add(newMenuItem(GCMenu.iNODE_DELETE, GCMenu.NODE_DELETE, 'D', Boolean.TRUE, true));
		menu.add(newMenuItem(GCMenu.iNODE_CREATE, GCMenu.NODE_CREATE, 'T', Boolean.FALSE, true));
		add(menu);

		//数据
		menu = newMenu(GCMenu.iDATA, GCMenu.DATA, 'D', true);
		menu.add(newMenuItem(GCMenu.iDATA_COPY, GCMenu.DATA_COPY, 'C', Boolean.FALSE, true));
		menu.add(newMenuItem(GCMenu.iDATA_PASTE, GCMenu.DATA_PASTE, 'P', Boolean.FALSE, true));
		menu.add(newMenuItem(GCMenu.iDATA_SAVE, GCMenu.DATA_SAVE, 'S', Boolean.FALSE, true));
		menu.add(newMenuItem(GCMenu.iDATA_IMPORT, GCMenu.DATA_IMPORT, 'I', Boolean.FALSE, true));
		add(menu);

		// 工具菜单
		menu = newMenu(GCMenu.iTOOLS, GCMenu.TOOLS, 'T', true);
//		menu.add(newMenuItem(GCMenu.iTOOLS_BINBROWSER, GCMenu.TOOLS_BINBROWSER, 'B', Boolean.FALSE, false));
		menu.add(newMenuItem(GCMenu.iTOOLS_TOGGLE_THEME, GCMenu.TOOLS_TOGGLE_THEME, 'D', Boolean.TRUE, true));
		menu.add(newMenuItem(GCMenu.iTOOLS_OPTION, GCMenu.TOOLS_OPTION, 'O', Boolean.FALSE, true));
		add(menu);
		
		// 窗口菜单
		menu = newMenu(GCMenu.iWINDOW, GCMenu.WINDOW, 'W', true);
		menu.add(newMenuItem(GCMenu.iCASCADE, GCMenu.CASCADE, 'C', Boolean.FALSE, true));
		menu.add(newMenuItem(GCMenu.iTILE_HORIZONTAL, GCMenu.TILE_HORIZONTAL, 'H', Boolean.FALSE, false));
		menu.add(newMenuItem(GCMenu.iTILE_VERTICAL, GCMenu.TILE_VERTICAL, 'V', Boolean.FALSE, false));
		menu.add(newMenuItem(GCMenu.iLAYER, GCMenu.LAYER, 'L', Boolean.FALSE, false));
		add(menu);
	}
	
	public void nodeSelected(VDBTreeNode node){
		disableConnectMenu();
		disableNodeMenu();
		
		ToolbarVDB toolbar = VDB.getInstance().getToolbarVDB();
		toolbar.disableAll();
		
		if(node.getType()==VDBTreeNode.TYPE_HOME){
			return;
		}
		if(node.getType()==VDBTreeNode.TYPE_CONNECTION){
			ConnectionConfig cc = (ConnectionConfig)node.getUserObject();
			setMenuEnabled(GCMenu.iCONN_OPEN, !cc.isConnected());
			setMenuEnabled(GCMenu.iCONN_SAVE, cc.isEditChanged());
			setMenuEnabled(GCMenu.iCONN_CLOSE, cc.isConnected());
			setMenuEnabled(GCMenu.iCONN_CONFIG, !cc.isConnected());
			setMenuEnabled(GCMenu.iCONN_DELETE, true);
			setMenuEnabled(GCMenu.iCONN_ACHEIVE, true);
			setMenuEnabled(GCMenu.iCONN_PURGE, true);
			
			toolbar.setButtonEnabled(GCMenu.iCONN_OPEN, !cc.isConnected());
			toolbar.setButtonEnabled(GCMenu.iCONN_SAVE, cc.isEditChanged());
			toolbar.setButtonEnabled(GCMenu.iCONN_CLOSE, cc.isConnected());
			toolbar.setButtonEnabled(GCMenu.iCONN_CONFIG, !cc.isConnected());
			return;
		}
		ConnectionConfig cc = VDBTree.getNodeConnection(node);
		if(cc!=null){
			setMenuEnabled(GCMenu.iCONN_SAVE, cc.isEditChanged());
			toolbar.setButtonEnabled(GCMenu.iCONN_SAVE, cc.isEditChanged());
		}
		
		setMenuEnabled(GCMenu.iNODE_COPY, true);
		setMenuEnabled(GCMenu.iNODE_PASTE, true);
		setMenuEnabled(GCMenu.iNODE_DELETE, true);
		setMenuEnabled(GCMenu.iNODE_CREATE, true);
	}
	
	public void disableConnectMenu(){
		short[] menuIds = new short[]{GCMenu.iCONN_OPEN,GCMenu.iCONN_CLOSE,
				GCMenu.iCONN_CONFIG,GCMenu.iCONN_DELETE,GCMenu.iCONN_ACHEIVE,GCMenu.iCONN_PURGE
				};
		setMenuEnabled(menuIds, false);
	}
	
	public void disableNodeMenu(){
		short[] menuIds = new short[]{GCMenu.iNODE_COPY, GCMenu.iNODE_PASTE,GCMenu.iNODE_DELETE,
				GCMenu.iNODE_CREATE
				};
		setMenuEnabled(menuIds, false);
	}
	
	public void disableDataMenu(){
		short[] menuIds = new short[]{GCMenu.iDATA_COPY,GCMenu.iDATA_PASTE,GCMenu.iDATA_SAVE,
				GCMenu.iDATA_IMPORT
				};
		setMenuEnabled(menuIds, false);
	}
}
