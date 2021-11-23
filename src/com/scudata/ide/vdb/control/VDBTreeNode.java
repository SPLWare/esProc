package com.scudata.ide.vdb.control;

import java.io.File;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

import com.scudata.dm.Sequence;
import com.scudata.ide.common.GM;
import com.scudata.vdb.IVS;
import com.scudata.vdb.VDB;

public class VDBTreeNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 1L;

 	public static final byte TYPE_HOME = 0;
 	public static final byte TYPE_CONNECTION = 1;
 	public static final byte TYPE_VDB = 2;
 	
	public static final byte TYPE_FOLDER = 3;
	public static final byte TYPE_SEQUENCE = 4;
	public static final byte TYPE_STRING = 5;
	public static final byte TYPE_IMAGE = 6;
	public static final byte TYPE_OTHER = 100;

	private boolean isLoaded = false;

	private byte type = TYPE_OTHER;
	private String title = null;

	private String filter = null;
	private ArrayList<VDBTreeNode> childBuffer = null;
	private boolean isMatched = false;
	private boolean isExpanded = false;

	public VDBTreeNode(IVS ivs) {
		this.setUserObject( ivs );
		Object data = getData();
		if(ivs instanceof VDB){
			type = TYPE_VDB;
		}else if(data==null){
			type = TYPE_FOLDER;
		}else{
			if(data instanceof Sequence){
				type = TYPE_SEQUENCE;
			}else if(data instanceof String){
				type = TYPE_STRING;
			}else if (data instanceof byte[]){
				type = TYPE_IMAGE;
			}else{
				type = TYPE_OTHER;
			}
		}
		title = getNodeTitle();
	}

	public VDB getVDB(){
		if(type<=TYPE_CONNECTION ){
			return null;
		}
		VDBTreeNode pNode = this;
		while(pNode.getType()!=TYPE_VDB){
			pNode = (VDBTreeNode)pNode.getParent();
		}
		return (VDB)pNode.getUserObject();
	}
	
	public Object getData(){
		IVS ivs = (IVS) getUserObject();
		Object data = ivs.load(null);
		return data;
	}
	
	public boolean saveData(Object data){
		IVS ivs = (IVS) getUserObject();
		ivs.save(data);
		return true;
	}

	private String getNodeTitle(){
		IVS ivs = (IVS) getUserObject();
		String name = (String)ivs.path("f");
		Object value = ivs.path(null);
		StringBuffer sb = new StringBuffer();
		if(value!=null){
			sb.append(value);
		}
		if(name!=null){
			sb.append(" [ ");
			sb.append(name);
			sb.append(" ]");
		}else{
			if(sb.length()==0){
				sb.append("ROOT");
			}else{
				sb.append(" [ ]");
			}
		}
		return sb.toString();
	}
	
	public VDBTreeNode(Object obj, byte type) {
		if(obj instanceof ConnectionConfig){
			ConnectionConfig cc = (ConnectionConfig)obj;
			title = cc.getName();
		}else{
			title = obj.toString();
		}
		this.type = type;
		this.setUserObject(obj);
	}

	public void setFilter(String filter) {
		this.filter = filter.toLowerCase();
		filter();
	}

	private void filter() {
		if (childBuffer == null) {
			childBuffer = new ArrayList<VDBTreeNode>();
			for (int i = 0; i < getChildCount(); i++) {
				childBuffer.add((VDBTreeNode) getChildAt(i));
			}
		}
		removeAllChildren();
		for (int c = 0; c < childBuffer.size(); c++) {
			VDBTreeNode childNode = childBuffer.get(c);
			String lowerTitle = childNode.getTitle().toLowerCase();
			if (lowerTitle.indexOf(filter) >= 0) {
				add(childNode);
			}
		}
	}

	public String getFilter() {
		return filter;
	}

	public VDBTreeNode deepClone() {
		VDBTreeNode newNode = new VDBTreeNode(title, type);
		newNode.setUserObject(getUserObject());
		newNode.setMatched(isMatched);
		return newNode;
	}

	public void setMatched(boolean isMatched) {
		this.isMatched = isMatched;
	}

	public boolean isMatched() {
		return isMatched;
	}

	public ImageIcon getDispIcon() {
		String imgPath = "/com/raqsoft/ide/vdb/img/";
		switch(type){
		case TYPE_HOME:
			imgPath += "home.png";
			break;
		case TYPE_CONNECTION:
			ConnectionConfig cc = (ConnectionConfig)getUserObject();
			if(cc.isConnected()){
				imgPath += "connection_on.png";
			}else{
				imgPath += "connection_off.png";
			}
			break;
		case TYPE_VDB:
			imgPath += "vdb.png";
			break;
		case TYPE_FOLDER:
			imgPath += "folder.png";
			break;
		case TYPE_SEQUENCE:
			imgPath += "sequence.png";
			break;
		case TYPE_STRING:
			imgPath += "string.png";
			break;
		case TYPE_IMAGE:
			imgPath += "image.png";
			break;
		case TYPE_OTHER:
			imgPath += "other.png";
			break;
		}
		
		ImageIcon img = GM.getImageIcon(imgPath);
		// img.setImage(img.getImage().getScaledInstance(22, 22,
		// Image.SCALE_DEFAULT));
		return img;
	}

	public void setLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public String getName() {
		return toString();
	}

	public String getFullPath() {
		String path = getName();
		VDBTreeNode pNode = (VDBTreeNode) getParent();
		while (pNode != null) {
			path = pNode.getName() + File.separator + path;
			pNode = (VDBTreeNode) pNode.getParent();
		}
		return path;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String toString() {
		return title;
	}

	public boolean isExpanded() {
		return isExpanded;
	}

	public void setExpanded(boolean isExpanded) {
		this.isExpanded = isExpanded;
	}
}
