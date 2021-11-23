package com.scudata.chart.edit;

import java.util.*;

import com.scudata.chart.resources.*;
import com.scudata.common.*;

/**
 * 参数信息列表类，用于编辑面板中列出图元的可编辑参数信息
 * @author Joancy
 *
 */
public class ParamInfoList {
	ArrayList<ArrayList<ParamInfo>> paramGroups = new ArrayList<ArrayList<ParamInfo>>();
	ArrayList<String> groupNames = new ArrayList<String>();

	ArrayList<ParamInfo> rootList = new ArrayList<ParamInfo>();
	private final static String ROOTGROUP = "RootGroup";
	private MessageManager mm = ChartMessage.get();

	/**
	 * 缺省参数构造函数
	 */
	public ParamInfoList() {
	}

	private ArrayList<ArrayList<ParamInfo>> getParamGroups() {
		return paramGroups;
	}

	private ArrayList<ParamInfo> getrootList() {
		return rootList;
	}

	/**
	 * 将另一个参数信息列表的内容全部添加到当前列表
	 * @param pil 参数信息列表
	 */
	public void addAll(ParamInfoList pil) {
		//pil中分组名称要跟本参数分组相同名称合并，改成如下代码
		ArrayList<String> groupNames = pil.getGroupNames();
		for( int i=0; i<groupNames.size(); i++){
			String grpName = groupNames.get(i);
			ArrayList<ParamInfo> grpParams = pil.getParams(grpName);
			for( int n=0;n<grpParams.size(); n++){
				add(grpName,grpParams.get(n));
			}
		}
		rootList.addAll(pil.getrootList());
	}

	/**
	 * 将一个参数信息pi添加到分组group下
	 * @param group 组名称
	 * @param pi 参数信息
	 */
	public void add(String group, ParamInfo pi) {
		ArrayList<ParamInfo> pis = null;
		if (group == null || ROOTGROUP.equalsIgnoreCase(group)) {
			pis = rootList;
		} else {
			group = mm.getMessage(group);
			int index = groupNames.indexOf(group);
			if (index < 0) {
				groupNames.add(group);
				pis = new ArrayList<ParamInfo>();
				paramGroups.add(pis);
			} else {
				pis = paramGroups.get(index);
			}
		}
		if (pis == null) {
			pis = rootList;
		}
		pis.add(pi);
	}
	
	/**
	 * 某些子类不需要父类中定义好的参数信息时，可以删除它
	 * 该函数删除整个组下的信息
	 * @param group 分组名称
	 */
	public void deleteGroup(String group){
		group = mm.getMessage(group);
		int index = groupNames.indexOf(group);
		if (index >=0 ) {
			groupNames.remove(index);
			paramGroups.remove(index);
		}
	}

	/**
	 * 删除分组group下的一个参数信息
	 * @param group 分组名
	 * @param name 参数信息的名称
	 */
	public void delete(String group, String name){
		group = mm.getMessage(group);
		delete(getParams(group),name);
	}
	
	/**
	 * 往根路径下，也即不属于任何分组，增加一个参数信息
	 * @param pi 参数信息
	 */
	public void add(ParamInfo pi) {
		rootList.add(pi);
	}
	
	/**
	 * 删除根目录下的参数信息
	 * @param name 参数名称
	 */
	public void delete(String name){
		delete(rootList,name);
	}
	
	/**
	 * 从指定的参数信息列表中删掉一个参数信息
	 * @param list 参数信息列表
	 * @param name 要删除的参数名称
	 */
	public void delete(ArrayList<ParamInfo> list,String name){
		for(int i=0; i<list.size();i++){
			ParamInfo pi = list.get(i);
			if(pi.getName().equalsIgnoreCase(name)){
				list.remove(i);
				return;
			}
		}
	}

	/**
	 * 列出当前信息列表中的全部分组名称
	 * @return 分组名称列表
	 */
	public ArrayList<String> getGroupNames() {
		return groupNames;
	}

	/**
	 * 获取一个分组的所有参数信息列表
	 * @param groupName 分组名称
	 * @return 参数信息列表
	 */
	public ArrayList<ParamInfo> getParams(String groupName) {
		ArrayList<ParamInfo> pis = null;
		int index = groupNames.indexOf(groupName);
		if (index >= 0) {
			pis = paramGroups.get(index);
		}
		if (pis != null)
			return pis;
		return rootList;
	}

	/**
	 * 获取根目录下的全部参数信息列表
	 * @return 参数信息列表
	 */
	public ArrayList<ParamInfo> getRootParams() {
		return rootList;
	}

	/**
	 * 从所有参数信息中找到对应的参数信息
	 * @param name 参数名称
	 * @return 参数信息
	 */
	public ParamInfo getParamInfoByName(String name) {
		ArrayList<ParamInfo> aps = getAllParams();
		int infoSize = aps.size();
		for (int i = 0; i < infoSize; i++) {
			ParamInfo pi = (ParamInfo) aps.get(i);
			if (pi.getName().equalsIgnoreCase(name)) {
				return pi;
			}
		}
		return null;
	}

	/**
	 * 列出所有参数信息的综合列表，包含根目录以及下面所有分组
	 * @return 全部参数信息列表
	 */
	public ArrayList<ParamInfo> getAllParams() {
		ArrayList<ParamInfo> aps = new ArrayList<ParamInfo>();
		int size = rootList == null ? 0 : rootList.size();
		for (int i = 0; i < size; i++) {
			aps.add(rootList.get(i));
		}
		size = paramGroups == null ? 0 : paramGroups.size();
		for (int i = 0; i < size; i++) {
			ArrayList<ParamInfo> pis = paramGroups.get(i);
			int ps = pis == null ? 0 : pis.size();
			for (int j = 0; j < ps; j++) {
				aps.add(pis.get(j));
			}
		}
		return aps;
	}

}
