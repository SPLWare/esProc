package com.scudata.ide.dfx.base;

import java.io.File;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

import com.scudata.ide.common.GM;

/**
 * 资源树结点
 * 
 * @author wunan
 *
 */
public class FileTreeNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 1L;

	/** 根节点类型 */
	public static final byte TYPE_ROOT = 0;
	/** 本地资源类型 */
	public static final byte TYPE_LOCAL = 1;
	/** 服务器资源类型 */
	public static final byte TYPE_SERVER = 2;

	/** 未选择状态 */
	public static final byte NOT_SELECTED = 0;
	/** 选择状态 */
	public static final byte SELECTED = 1;
	/** 第三态 */
	public static final byte DONT_CARE = 2;

	/**
	 * 选择状态
	 */
	private transient byte selectedState = SELECTED;

	/**
	 * 是否复选框结点
	 */
	private boolean isCheckNode = false;
	/**
	 * 是否已经加载了子结点
	 */
	private boolean isLoaded = false;

	/**
	 * 结点类型
	 */
	private byte type = TYPE_LOCAL;
	/**
	 * 结点标题
	 */
	private String title = null;
	/**
	 * 是否目录。true是目录，false是文件。
	 */
	private boolean isDir = false;
	/**
	 * 过滤条件
	 */
	private String filter = null;
	/**
	 * 子结点列表
	 */
	private ArrayList<FileTreeNode> childBuffer = null;
	/**
	 * 是否匹配了
	 */
	private boolean isMatched = false;
	/**
	 * 是否展开了
	 */
	private boolean isExpanded = false;
	/**
	 * 服务器名称
	 */
	private String serverName;

	/**
	 * 构造函数
	 * 
	 * @param data
	 * @param type
	 */
	public FileTreeNode(Object data, byte type) {
		this.setUserObject(data);
		this.type = type;
	}

	/**
	 * 设置目录
	 * 
	 * @param isDir
	 */
	public void setDir(boolean isDir) {
		this.isDir = isDir;
	}

	/**
	 * 取目录
	 * 
	 * @return
	 */
	public boolean isDir() {
		return isDir;
	}

	/**
	 * 设置是否匹配
	 * 
	 * @param isMatched
	 */
	public void setMatched(boolean isMatched) {
		this.isMatched = isMatched;
	}

	/**
	 * 取是否匹配
	 * 
	 * @return
	 */
	public boolean isMatched() {
		return isMatched;
	}

	/**
	 * 取显示的图标
	 * 
	 * @return
	 */
	public ImageIcon getDispIcon() {
		String imgPath = "/com/scudata/ide/common/resources/tree";
		if (this.getLevel() == 0) {
			imgPath += "0.gif";
		} else if (this.getLevel() == 1) { // 本地资源和服务器资源根目录的图标
			imgPath += "view.gif";
		} else if (type == TYPE_LOCAL || type == TYPE_SERVER) {
			if (isDir)
				imgPath += "folder.gif";
			else
				imgPath += "new.gif";
		}
		ImageIcon img = GM.getImageIcon(imgPath);
		return img;
	}

	/**
	 * 设置是否加载了子结点
	 * 
	 * @param isLoaded
	 */
	public void setLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}

	/**
	 * 取是否加载了子结点
	 * 
	 * @return
	 */
	public boolean isLoaded() {
		return isLoaded;
	}

	/**
	 * 取结点类型
	 * 
	 * @return
	 */
	public byte getType() {
		return type;
	}

	/**
	 * 设置结点类型
	 * 
	 * @param type
	 */
	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * 取结点名字
	 * 
	 * @return
	 */
	public String getName() {
		return toString();
	}

	/**
	 * 取全路径
	 * 
	 * @return
	 */
	public String getFullPath() {
		String path = getName();
		FileTreeNode pNode = (FileTreeNode) getParent();
		while (pNode != null) {
			if (pNode.getName().equals(FileTree.ROOT_TITLE))
				break;
			path = pNode.getName() + File.separator + path;
			pNode = (FileTreeNode) pNode.getParent();
		}
		return path;
	}

	/**
	 * 取标题
	 * 
	 * @return
	 */
	public String getTitle() {
		return title == null ? getName() : title;
	}

	/**
	 * 设置标题
	 * 
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * 选中状态
	 * 
	 * @return NOT_SELECTED,SELECTED,DONT_CARE
	 */
	public byte getSelectedState() {
		return selectedState;
	}

	/**
	 * 设置选中状态
	 * 
	 * @param selectedState
	 *            NOT_SELECTED,SELECTED,DONT_CARE
	 */
	public void setSelectedState(byte selectedState) {
		this.selectedState = selectedState;
	}

	/**
	 * 取服务器名称
	 * 
	 * @return
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * 设置服务器名称
	 * 
	 * @param serverName
	 *            服务器名称
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	/**
	 * 设置过滤条件
	 * 
	 * @param filter
	 *            过滤条件
	 * 
	 */
	public void setFilter(String filter) {
		this.filter = filter.toLowerCase();
		filter();
	}

	/**
	 * 过滤
	 */
	private void filter() {
		if (childBuffer == null) {
			childBuffer = new ArrayList<FileTreeNode>();
			for (int i = 0; i < getChildCount(); i++) {
				childBuffer.add((FileTreeNode) getChildAt(i));
			}
		}
		removeAllChildren();
		for (int c = 0; c < childBuffer.size(); c++) {
			FileTreeNode childNode = childBuffer.get(c);
			String lowerTitle = childNode.getTitle().toLowerCase();
			if (lowerTitle.indexOf(filter) >= 0) {
				add(childNode);
			}
		}
	}

	/**
	 * 取过滤条件
	 * 
	 * @return 过滤条件
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * 是否复选框结点
	 * 
	 * @return
	 */
	public boolean isCheckNode() {
		return isCheckNode;
	}

	/**
	 * 设置复选框结点
	 * 
	 * @param isCheckNode
	 */
	public void setCheckNode(boolean isCheckNode) {
		this.isCheckNode = isCheckNode;
	}

	/**
	 * 结点是否展开
	 * 
	 * @return
	 */
	public boolean isExpanded() {
		return isExpanded;
	}

	/**
	 * 设置结点是否展开
	 * 
	 * @param isExpanded
	 */
	public void setExpanded(boolean isExpanded) {
		this.isExpanded = isExpanded;
	}

	/**
	 * 克隆
	 * 
	 * @return
	 */
	public FileTreeNode deepClone() {
		FileTreeNode newNode = new FileTreeNode(getUserObject(), type);
		newNode.setTitle(title);
		newNode.setMatched(isMatched);
		newNode.setSelectedState(selectedState);
		newNode.setCheckNode(isCheckNode);
		return newNode;
	}

	/**
	 * 转换为字符串
	 */
	public String toString() {
		if (title != null)
			return title;
		Object data = this.getUserObject();
		if (type == TYPE_LOCAL) {
			return data == null ? null : data.toString();
		}
		return null;
	}

}
