package com.scudata.ide.dfx.etl;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.scudata.common.MessageManager;

/**
 * 已经实现了辅助编辑的函数
 * 
 * 都需要动态登记到元素库
 * 
 * @author Joancy
 *
 */
public class ElementLib {
	private static ArrayList<ElementInfo> elements = new ArrayList<ElementInfo>(
			20);
	private static MessageManager mm = FuncMessage.get();

	static {
		loadSystemElements();
	}

	private static int indexof(String name) {
		int size = elements.size();
		for (int i = 0; i < size; i++) {
			ElementInfo ei = elements.get(i);
			if (ei.getName().equalsIgnoreCase(name))
				return i;
		}
		return -1;
	}

	/**
	 * 根据名称找到对应的元素信息
	 * 
	 * @param name
	 *            函数名
	 * @return 元素信息
	 */
	public static ElementInfo getElementInfo(String name) {
		int i = indexof(name);
		if (i >= 0)
			return elements.get(i);
		return null;
	}

	/**
	 * 追加一个元素
	 * 
	 * @param name
	 *            元素名
	 */
	public static void addElement(String name) {
		try {
			String packageName = "com.scudata.ide.dfx.etl.element.";
			String className = packageName + name;
			Class elemClass = Class.forName(className);
			String title = mm.getMessage(name);

			ElementInfo ei = new ElementInfo(name, title, elemClass);
			int i = indexof(name);
			if (i >= 0) {
				elements.add(i, ei);
			} else {
				elements.add(ei);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 根据父类型列出所有相关函数信息的列表
	 * 
	 * @param parentType
	 *            父类型
	 * @return 对应的元素信息列表
	 */
	public static ArrayList<ElementInfo> getElementInfos(byte parentType) {
		ArrayList<ElementInfo> eis = new ArrayList<ElementInfo>();
		for (ElementInfo ei : elements) {
			if (ei.getParentType() == parentType) {
				eis.add(ei);
			}
		}
		return eis;
	}

	/**
	 * 根据函数名称列出同名的元素信息，比如group，游标以及序表类型都有该函数
	 * 
	 * @param funcName
	 *            函数名
	 * @return 元素信息
	 */
	public static ArrayList<ElementInfo> getElementInfos(String funcName) {
		ArrayList<ElementInfo> eis = new ArrayList<ElementInfo>();
		for (ElementInfo ei : elements) {
			if (ei.getFuncName().equals(funcName)) {
				eis.add(ei);
			}
		}
		return eis;
	}

	/**
	 * 装载系统目录下全部实现辅助编辑的函数元素 路径：/com/raqsoft/ide/dfx/etl/element
	 */
	public static void loadSystemElements() {
		String names = "ACreate,ADelete,ADerive,AGroup,AGroup2,AGroups,AInsert,"
				+ "AJoin,AKeys,ANew,ANews,ARename,ARun,ASelect,"
				+ "ConnectDB,ConnectDriver,Create,CsDerive,CsFetch,CsGroup,CsGroups,"
				+ "CsGroupx,CsJoin,CsJoinx,CsNew,CsNews,CsRename,CsRun,CsSortx,DCursorSQL,DExecute,"
				+ "DQuerySQL,DUpdate,FCreate,FCursor,FExport,File,FImport,FOpen,FXlsExport,"
				+ "FXlsImport,FXlsOpen,FXlsWrite,Joinx,TAppend,TAttach,TClose,TCursor,TUpdate,"
				+ "XXlsClose,XXlsExport,XXlsImport";
		StringTokenizer st = new StringTokenizer(names,",");
		while (st.hasMoreTokens()) {
			String name = st.nextToken();
			addElement(name);
		}
	}

}
