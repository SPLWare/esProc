package com.scudata.ide.common.function;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.scudata.app.common.AppConsts;
import com.scudata.app.common.Section;
import com.scudata.common.Logger;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.XMLFile;

/**
 * 函数管理
 *
 */
public class FuncManager {
	/**
	 * 文件名
	 */
	private static String fileName = null;

	/**
	 * 函数管理对象
	 */
	private static FuncManager fm = null;

	/**
	 * 函数对象列表
	 */
	private ArrayList<FuncInfo> funcList;

	/**
	 * 根结点
	 */
	private final String ROOT = "funcs";

	/**
	 * 普通结点
	 */
	private final String NORMAL = "normal";

	/**
	 * 文件路径
	 */
	private static String relativeFile = null;

	/**
	 * 默认类路径加载文件时只读
	 */
	private boolean readonly = false;

	/**
	 * 文件前缀
	 */
	public static String filePrefix = "esProc";

	/**
	 * 取函数管理对象
	 * 
	 * @return
	 */
	public static FuncManager getManager() {
		if (fm == null) {
			fm = new FuncManager();
		}
		return fm;
	}

	/**
	 * 私有构造函数，通过FuncManager.getManager()调用
	 */
	private FuncManager() {
		funcList = new ArrayList<FuncInfo>();
		try {
			load(getFileName());
		} catch (Throwable x) {
			fileName = getRelativeFile();
			readonly = true;
			InputStream is = FuncManager.class
					.getResourceAsStream(getRelativeFile());
			if (is != null) {
				try {
					load(is);
				} catch (Throwable t) {
					Logger.debug(t);
				}
			}

		}
	}

	/**
	 * 取文件路径
	 * 
	 * @return
	 */
	private static String getRelativeFile() {
		if (relativeFile == null) {
			String pre = filePrefix;
			String suf = GM.getLanguageSuffix();
			relativeFile = GC.PATH_CONFIG + "/" + pre + "Functions" + suf + "."
					+ AppConsts.FILE_XML;
		}
		return relativeFile;
	}

	/**
	 * 是否只读
	 * 
	 * @return
	 */
	public boolean readOnly() {
		return readonly;
	}

	/**
	 * 取文件名
	 * 
	 * @return
	 */
	public static String getFileName() {
		if (fileName == null) {
			fileName = GM.getAbsolutePath(getRelativeFile());
		}
		return fileName;
	}

	/**
	 * 加载文件
	 * 
	 * @param fileName 文件名
	 * @throws Throwable
	 */
	public void load(String fileName) throws Throwable {
		XMLFile xml = new XMLFile(fileName);
		load(xml);
	}

	/**
	 * 加载文件
	 * 
	 * @param is 文件输入流
	 * @throws Throwable
	 */
	public void load(InputStream is) throws Throwable {
		XMLFile xml = new XMLFile(is);
		load(xml);
	}

	/**
	 * 加载文件
	 * 
	 * @param xml XML文件对象
	 * @throws Throwable
	 */
	private void load(XMLFile xml) throws Throwable {
		funcList.clear();
		Section funcIDs = xml.listElement(ROOT + "/" + NORMAL);
		for (int i = 0; i < funcIDs.size(); i++) {
			String fID = funcIDs.get(i);
			String path = ROOT + "/" + NORMAL + "/" + fID + "/";
			FuncInfo fi = new FuncInfo();
			fi.setName(xml.getAttribute(path + "name"));
			fi.setDesc(xml.getAttribute(path + "desc"));
			fi.setDisplayStr(xml.getAttribute(path + "displaystr"));
			try {
				fi.setPostfix(xml.getAttribute(path + "postfix"));
			} catch (Throwable e) {
			}
			String tmp = xml.getAttribute(path + "majortype");
			fi.setMajorType(Byte.parseByte(tmp));
			tmp = xml.getAttribute(path + "returntype");
			fi.setReturnType(Byte.parseByte(tmp));
			fi.setOptions(loadOptions(xml, path + "options"));
			fi.setParams(loadParams(xml, path + "params"));

			funcList.add(fi);
		}
	}

	/**
	 * 加载函数选项
	 * 
	 * @param xml      XML文件对象
	 * @param rootPath 节点路径
	 * @return
	 */
	ArrayList<FuncOption> loadOptions(XMLFile xml, String rootPath) {
		try {
			Section options = xml.listElement(rootPath);
			if (options.size() < 1) {
				return null;
			}
			ArrayList<FuncOption> al = new ArrayList<FuncOption>(options.size());
			for (int i = 0; i < options.size(); i++) {
				String opKey = options.get(i);
				FuncOption fo = new FuncOption();
				fo.setOptionChar(xml.getAttribute(rootPath + "/" + opKey
						+ "/optionchar"));
				fo.setDescription(xml.getAttribute(rootPath + "/" + opKey
						+ "/description"));
				String select = xml.getAttribute(rootPath + "/" + opKey
						+ "/defaultselect");
				if (StringUtils.isValidString(select)) {
					fo.setDefaultSelect(Boolean.valueOf(select).booleanValue());
				}
				al.add(fo);
			}
			return al;
		} catch (Exception ex) {
		}
		return null;
	}

	/**
	 * 保存函数选项
	 * 
	 * @param xml      XML文件对象
	 * @param rootPath 节点路径
	 * @param options  函数选项列表
	 */
	void storeOptions(XMLFile xml, String rootPath,
			ArrayList<FuncOption> options) {
		try {
			if (options.size() < 1) {
				return;
			}
			for (int i = 0; i < options.size(); i++) {
				FuncOption fo = options.get(i);
				String opKey = "O" + Integer.toString(i + 1);
				xml.newElement(rootPath, opKey);
				String path = rootPath + "/" + opKey + "/";
				xml.setAttribute(path + "optionchar", fo.getOptionChar());
				xml.setAttribute(path + "description",
						removeTab(fo.getDescription()));
				xml.setAttribute(path + "defaultselect", fo.isDefaultSelect()
						+ "");
			}
		} catch (Exception ex) {
		}
	}

	/**
	 * 加载函数参数
	 * 
	 * @param xml      XML文件对象
	 * @param rootPath 节点路径
	 * @return
	 */
	ArrayList<FuncParam> loadParams(XMLFile xml, String rootPath) {
		try {
			Section params = xml.listElement(rootPath);
			if (params.size() < 1) {
				return null;
			}
			ArrayList<FuncParam> al = new ArrayList<FuncParam>(params.size());
			for (int i = 0; i < params.size(); i++) {
				String paraKey = params.get(i);
				FuncParam fp = new FuncParam();
				String path = rootPath + "/" + paraKey + "/";
				fp.setDesc(xml.getAttribute(path + "desc"));
				String tmp = xml.getAttribute(path + "presign");
				if (!StringUtils.isValidString(tmp)) {
					fp.setPreSign(' ');
				} else {
					fp.setPreSign(tmp.charAt(0));
				}
				tmp = xml.getAttribute(path + "subparam");
				if (StringUtils.isValidString(tmp)) {
					fp.setSubParam(Boolean.valueOf(tmp).booleanValue());
				}
				tmp = xml.getAttribute(path + "repeatable");
				if (StringUtils.isValidString(tmp)) {
					fp.setRepeatable(Boolean.valueOf(tmp).booleanValue());
				}
				tmp = xml.getAttribute(path + "identifieronly");
				if (StringUtils.isValidString(tmp)) {
					fp.setIdentifierOnly(Boolean.valueOf(tmp).booleanValue());
				}
				fp.setOptions(loadOptions(xml, path + "options"));
				tmp = xml.getAttribute(path + "filtertype");
				if (StringUtils.isValidString(tmp)) {
					fp.setFilterType(Byte.parseByte(tmp));
				}
				al.add(fp);
			}
			return al;
		} catch (Exception ex) {
		}
		return null;
	}

	/**
	 * 保存函数参数
	 * 
	 * @param xml      XML文件对象
	 * @param rootPath 结点路径
	 * @param params   函数参数列表
	 */
	void storeParams(XMLFile xml, String rootPath, ArrayList<FuncParam> params) {
		try {
			if (params.size() < 1) {
				return;
			}
			for (int i = 0; i < params.size(); i++) {
				FuncParam fp = params.get(i);
				String pKey = "P" + Integer.toString(i + 1);
				xml.newElement(rootPath, pKey);
				String path = rootPath + "/" + pKey;

				xml.setAttribute(path + "/desc", removeTab(fp.getDesc()));
				xml.setAttribute(path + "/presign", fp.getPreSign() + "");
				xml.setAttribute(path + "/subparam", fp.isSubParam() + "");
				xml.setAttribute(path + "/repeatable", fp.isRepeatable() + "");
				xml.setAttribute(path + "/identifieronly",
						fp.isIdentifierOnly() + "");
				xml.setAttribute(path + "/valuestring", fp.getParamValue());

				xml.newElement(path, "options");
				storeOptions(xml, path + "/options", fp.getOptions());

				xml.setAttribute(path + "/filtertype", fp.getFilterType() + "");
			}
		} catch (Exception ex) {
		}
		return;
	}

	/**
	 * 返回函数列表
	 * @return
	 */
	public List<FuncInfo> getFuncList() {
		return funcList;
	}

	/**
	 * 按序号取函数
	 * 
	 * @param index 序号
	 * @return
	 */
	public FuncInfo getFunc(int index) {
		return funcList.get(index);
	}

	/**
	 * 按函数名取函数列表（可能同名函数）
	 * 
	 * @param funcName
	 * @return
	 */
	public ArrayList<FuncInfo> getFunc(String funcName) {
		ArrayList<FuncInfo> al = null;
		for (int i = 0; i < funcList.size(); i++) {
			FuncInfo fi = (FuncInfo) funcList.get(i);
			if (fi.getName().equalsIgnoreCase(funcName)) {
				if (al == null) {
					al = new ArrayList<FuncInfo>();
				}
				al.add(fi);
			}
		}
		return al;
	}

	/**
	 * 取函数的数量
	 * 
	 * @return
	 */
	public int size() {
		return funcList.size();
	}

	/**
	 * 清理函数列表
	 */
	public void clear() {
		funcList.clear();
	}

	public void addFunc(FuncInfo fi) {
		funcList.add(fi);
	}

	/**
	 * 将\t替换为空格
	 * 
	 * @param str
	 * @return
	 */
	private String removeTab(String str) {
		if (str == null) {
			return str;
		}
		return Sentence.replace(str, "\t", "        ", 0);
	}

	/**
	 * 保存
	 * 
	 * @return
	 */
	public boolean save() {
		try {
			if (ConfigOptions.bAutoBackup.booleanValue()) {
				// 保存前备份原来文件
				String backName = fileName + ".bak";
				File fb = new File(backName);
				fb.deleteOnExit();
				File old = new File(fileName);
				if (old.exists()) {
					old.renameTo(fb);
				}
			}

			XMLFile xml = XMLFile.newXML(fileName, ROOT);
			xml.newElement(ROOT, NORMAL);
			for (int i = 0; i < funcList.size(); i++) {
				FuncInfo fi = getFunc(i);
				String fID = "F" + Integer.toString(i + 1);
				xml.newElement(ROOT + "/" + NORMAL, fID);
				String path = ROOT + "/" + NORMAL + "/" + fID;

				xml.setAttribute(path + "/name", fi.getName());
				xml.setAttribute(path + "/desc", removeTab(fi.getDesc()));
				xml.setAttribute(path + "/displaystr", removeTab(fi.getDisplayStr()));
				xml.setAttribute(path + "/postfix", removeTab(fi.getPostfix()));
				xml.setAttribute(path + "/majortype",
						String.valueOf(fi.getMajorType()));
				xml.setAttribute(path + "/returntype",
						String.valueOf(fi.getReturnType()));

				xml.newElement(path, "options");
				storeOptions(xml, path + "/options", fi.getOptions());

				xml.newElement(path, "params");
				storeParams(xml, path + "/params", fi.getParams());
			}
			xml.save();
		} catch (Throwable t) {
			GM.showException(t);
			return false;
		}
		return true;
	}
}
