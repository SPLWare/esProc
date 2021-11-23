package com.scudata.ide.dfx.etl;

import java.util.*;

import com.scudata.common.*;

/**
 * 参数信息列表
 * 
 * 函数的对象元素的所有参数的集合
 * @author Joancy
 *
 */
public class ParamInfoList {
	ArrayList<ArrayList<ParamInfo>> paramGroups = new ArrayList<ArrayList<ParamInfo>>();
	ArrayList<String> groupNames = new ArrayList<String>();

	ArrayList<ParamInfo> rootList = new ArrayList<ParamInfo>();
	private final static String ROOTGROUP = "RootGroup";
	private MessageManager mm = FuncMessage.get();

	/**
	 * 构造函数
	 */
	public ParamInfoList() {
	}

	/**
	 * 获取根参数信息列表
	 * 由于元素的参数通常比较多，所以会将参数分组以方便编辑
	 * 其中通用的会分到根组
	 * @return 参数信息列表
	 */
	private ArrayList<ParamInfo> getrootList() {
		return rootList;
	}

	/**
	 * 追加所有参数信息
	 * @param pil 另一个参数信息列表
	 */
	public void addAll(ParamInfoList pil) {
		//pil中分组名称要跟本参数分组相同名称合并
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
	 * 将参数信息pi追加到分组group下面
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
	 * 删除组group下的名为name的参数
	 * @param group 组名称
	 * @param name 参数名称
	 */
	public void delete(String group, String name){
		group = mm.getMessage(group);
		delete(getParams(group),name);
	}
	
	/**
	 * 往根组增加一个参数信息
	 * @param pi 参数信息
	 */
	public void add(ParamInfo pi) {
		rootList.add(pi);
	}
	/**
	 * 删除根组下的名为name的参数
	 * @param name 参数名称
	 */
	public void delete(String name){
		delete(rootList,name);
	}
	
	/**
	 * 删除参数信息列表list中的名为name的参数
	 * @param list 参数信息列表
	 * @param name 参数名称
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
	 * 获取所有组的组名称
	 * @return 组名称列表
	 */
	public ArrayList<String> getGroupNames() {
		return groupNames;
	}

	/**
	 * 获取名为groupName的组的所有参数信息
	 * @param groupName 组名称
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
	 * 获取根组的参数信息
	 * @return 参数信息列表
	 */
	public ArrayList<ParamInfo> getRootParams() {
		return rootList;
	}

	/**
	 * 在所有参数中找到名为name的参数信息
	 * @param name 参数名称
	 * @return 参数信息，找不到时返回null
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
	 * 获取所有组的全部参数信息
	 * @return 参数信息列表
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
	
	/**
	 * 对所有参数执行空值检查
	 */
	public void check(){
		ArrayList<ParamInfo> all = getAllParams();
		for(ParamInfo pi:all){
			pi.check();
		}
	}

}
