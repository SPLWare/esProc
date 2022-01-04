package com.scudata.ide.common.function;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.JTextComponent;

import com.scudata.cellset.ICellSet;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;

/**
 * 解析函数参数工具
 *
 */
public final class ParamUtil {
	/**
	 * 分隔符节点类。包括： ';' ',' ':'
	 *
	 */
	private static class SymbolParam implements IParamTreeNode {
		/**
		 * 参数列表
		 */
		private List<IParamTreeNode> paramList = new ArrayList<IParamTreeNode>(
				3);

		/**
		 * 符号
		 */
		private String content;

		/**
		 * 类型
		 */
		private char type;

		/**
		 * 构造函数
		 * 
		 * @param type
		 *            类型
		 */
		public SymbolParam(char type) {
			this.type = type;
			switch (type) {
			case SEMICOLON:
				content = ";";
				break;
			case COMMA:
				content = ",";
				break;
			case COLON:
				content = ":";
				break;
			default:
				throw new RuntimeException();
			}
		}

		/**
		 * 取类型
		 */
		public char getType() {
			return type;
		}

		/**
		 * 取符号
		 */
		public String getContent() {
			return content;
		}

		/**
		 * 是否叶子
		 */
		public boolean isLeaf() {
			return false;
		}

		/**
		 * 取子参数数量
		 */
		public int getSubSize() {
			return paramList.size();
		}

		/**
		 * 按序号取子参数
		 */
		public IParamTreeNode getSub(int index) {
			return (IParamTreeNode) paramList.get(index);
		}

		/**
		 * 取所有子参数列表
		 */
		public void getAllParam(List<IParamTreeNode> list) {
			for (int i = 0, size = getSubSize(); i < size; ++i) {
				if (i > 0) {
					list.add(this);

				}
				IParamTreeNode sub = getSub(i);
				if (sub == null) {
					list.add(null);
				} else {
					sub.getAllParam(list);
				}
			}
		}

		/**
		 * 取所有叶子结点
		 */
		public void getAllLeafParam(List<IParamTreeNode> list) {
			for (int i = 0, size = getSubSize(); i < size; ++i) {
				IParamTreeNode sub = getSub(i);
				if (sub == null) {
					list.add(null);
				} else {
					sub.getAllLeafParam(list);
				}
			}
		}

		/**
		 * 增加子节点
		 * 
		 * @param param
		 */
		void addSub(IParamTreeNode param) {
			paramList.add(param);
		}
	}

	/**
	 * 叶子节点类
	 */
	private static class LeafParam implements IParamTreeNode {

		/**
		 * 符号
		 */
		private String content;

		/**
		 * 构造函数
		 * 
		 * @param content
		 *            符号
		 */
		public LeafParam(String content) {
			this.content = content;
		}

		/**
		 * 取类型
		 */
		public char getType() {
			return NORMAL;
		}

		/**
		 * 取符号
		 */
		public String getContent() {
			return content;
		}

		/**
		 * 是否叶子结点
		 */
		public boolean isLeaf() {
			return true;
		}

		/**
		 * 取子结点的数量
		 */
		public int getSubSize() {
			return 0;
		}

		/**
		 * 按序号取子结点
		 */
		public IParamTreeNode getSub(int index) {
			throw new RuntimeException();
		}

		/**
		 * 取所有的子结点
		 */
		public void getAllParam(List<IParamTreeNode> list) {
			list.add(this);
		}

		/**
		 * 取所有的叶子结点
		 */
		public void getAllLeafParam(List<IParamTreeNode> list) {
			list.add(this);
		}
	}

	/**
	 * 解析层次参数，返回根节点
	 * 
	 * @param paramStr
	 *            String
	 * @return IParam
	 */
	public static IParamTreeNode parse(String paramStr) {
		return parse(paramStr, IParamTreeNode.NORMAL);
	}

	/**
	 * 获得位置p所在的函数名称
	 * 
	 * @param exp
	 *            String
	 * @param p
	 *            int
	 * @return String
	 */
	static String getFuncName(String exp, int p) {
		Point point = pFuncName(exp, p);
		if (point == null) {
			return "";
		}
		String allName = exp.substring(point.x, point.y);
		int at = allName.indexOf('@');
		if (at >= 0) {
			return allName.substring(0, at);
		}
		return allName;
	}

	/**
	 * 找到函数名称位于表达式的坐标，其中x代表起始位置，y代表结束位置 未找到返回null,该函数返回值包含选项
	 * 
	 * @param exp
	 *            String
	 * @param p
	 *            int
	 * @return Point
	 */
	static Point pFuncName(String exp, int p) {
		if (p >= exp.length()) {
			return null;
		}

		// 光标如果是落在函数名上，也算定位函数
		if (p > 1
				&& (exp.charAt(p - 1) == '@' || Sentence.isWordChar(exp
						.charAt(p - 1)))) {
			for (int i = p; i < exp.length(); i++) {
				if (exp.charAt(i) == '(') {
					p = i + 1;
					break;
				}
				if (exp.charAt(i) != '@' && !Sentence.isWordChar(exp.charAt(i))) {
					break;
				}
			}
		}

		for (int i = p - 1; i >= 0; i--) {
			if (exp.charAt(i) == '(') {
				int r = Sentence.scanParenthesis(exp, i);
				if (r > 0 && r >= p) {
					int start = scanPreIdentifier(exp, i);
					return new Point(start, i);
				}
			}
		}
		return null;
	}

	/**
	 * 取函数选项字符串
	 * 
	 * @param exp
	 * @param p
	 * @return
	 */
	static String getFuncOption(String exp, int p) {
		Point point = pFuncName(exp, p);
		if (point == null) {
			return "";
		}
		String allName = exp.substring(point.x, point.y);
		int at = allName.indexOf('@');
		if (at >= 0) {
			return allName.substring(at + 1);
		}
		return "";
	}

	/**
	 * 正在编辑的函数信息
	 */
	private static EditingFuncInfo lastEFI = null;

	/**
	 * 取正在编辑的函数信息
	 * 
	 * @param editor
	 *            编辑框
	 * @param cellSet
	 *            网格
	 * @param context
	 *            上下文
	 * @return
	 */
	public static EditingFuncInfo getEditingFunc(JTextComponent editor,
			ICellSet cellSet, Context context) {
		String exp = editor.getText();
		boolean addEqual = !exp.startsWith("=");
		int caretPosition = editor.getCaretPosition();
		if (addEqual) {
			exp = "=" + exp;
			caretPosition += 1;
		}
		String editingFuncName = getFuncName(exp, caretPosition);
		if (editingFuncName == null) {
			return null;
		}
		String funcParam = getFuncParams(exp, caretPosition);
		if (lastEFI != null && lastEFI.getFuncName().equals(editingFuncName)
				&& lastEFI.getFuncParam().equals(funcParam)) {
			return lastEFI;
		}

		Point p = pFuncName(exp, caretPosition);
		if (p == null) {
			return null;
		}
		int start = p.x;
		int end = p.y + 1;
		String editingFuncOption = getFuncOption(exp, caretPosition);
		EditingFuncParam editingFuncParam = new EditingFuncParam();
		editingFuncParam.setParamString(funcParam);
		editingFuncParam.setBoldPos(editor.getSelectionStart(),
				editor.getSelectionEnd());
		EditingFuncInfo efi = new EditingFuncInfo(editor, editingFuncName,
				editingFuncOption, editingFuncParam, addEqual ? start - 1
						: start, addEqual ? end - 1 : end);
		String majorExp = exp.substring(0, start);
		try {
			Expression major = new Expression(cellSet, context, majorExp);
			byte majorType = major.getExpValueType(context);
			efi.setMajorType(majorType);
		} catch (Exception x) {
		}

		return efi;
	}

	/**
	 * 将指定的参数串 params 匹配到 函数定义 srcFunc
	 * 
	 * @param srcFunc
	 *            FuncInfo
	 * @param params
	 *            String
	 * @return FuncInfo，匹配成功返回克隆后赋值的 FuncInfo，否则返回null
	 */
	public static FuncInfo matchFuncInfoParams(FuncInfo srcFunc, String params) {
		if (!StringUtils.isValidString(params)
				&& (srcFunc.getParams() == null || srcFunc.getParams().size() == 0)) {
			return (FuncInfo) srcFunc.deepClone();
		}
		IParamTreeNode paramTree = parse(params);
		if (paramTree == null) {
			return null;
		}
		ArrayList<FuncParam> paramMatchResult = match1(srcFunc.getParams(),
				paramTree);
		if (paramMatchResult != null) {
			FuncInfo cloneFunc = (FuncInfo) srcFunc.deepClone();
			cloneFunc.setParams(paramMatchResult);
			return cloneFunc;
		}
		return null;
	}

	/**
	 * 一级匹配，也即分号的匹配
	 * 
	 * @return boolean
	 */
	private static ArrayList<FuncParam> match1(
			ArrayList<FuncParam> paramDefine, IParamTreeNode rootExp) {
		ArrayList<ArrayList<FuncParam>> defineLevel1 = splitParamList(
				paramDefine, IParamTreeNode.SEMICOLON);
		ArrayList<IParamTreeNode> inputLevel1 = getSubLevelParams(rootExp,
				IParamTreeNode.SEMICOLON);
		if (inputLevel1.size() > defineLevel1.size()) {
			return null;
		}

		ArrayList<FuncParam> al = new ArrayList<FuncParam>();
		for (int i = 0; i < inputLevel1.size(); i++) {
			IParamTreeNode subParam = (IParamTreeNode) inputLevel1.get(i);
			ArrayList<FuncParam> subList = match2(defineLevel1.get(i), subParam);
			if (subList == null) {
				return null;
			} else {
				al.addAll(subList);
			}
		}
		// 补齐被省略掉的参数
		for (int i = inputLevel1.size(); i < defineLevel1.size(); i++) {
			al.addAll(defineLevel1.get(i));
		}
		return al;
	}

	/**
	 * 取下一层的子参数列表
	 * 
	 * @param root
	 * @param levelChar
	 * @return
	 */
	private static ArrayList<IParamTreeNode> getSubLevelParams(
			IParamTreeNode root, char levelChar) {
		ArrayList<IParamTreeNode> al = new ArrayList<IParamTreeNode>();
		if (root == null) {
			return al;
		}
		if (root.getType() == levelChar) {
			for (int i = 0; i < root.getSubSize(); i++) {
				IParamTreeNode sub = root.getSub(i);
				al.add(sub);
			}
		} else {
			al.add(root);
		}
		return al;
	}

	/**
	 * 二级匹配，也即逗号的匹配 二级参数定义为，最多有一个可重复的参数，如果有，且只能是最后一个参数
	 * 
	 * @param paramDefine
	 * @param rootExp
	 * @return
	 */
	private static ArrayList<FuncParam> match2(
			ArrayList<FuncParam> paramDefine, IParamTreeNode rootExp) {
		ArrayList<ArrayList<FuncParam>> defineLevel2 = splitParamList(
				paramDefine, IParamTreeNode.COMMA);
		ArrayList<IParamTreeNode> inputLevel2 = getSubLevelParams(rootExp,
				IParamTreeNode.COMMA);

		int repIndex = -1;
		FuncParam fp;
		ArrayList<FuncParam> subDef;
		for (int i = 0; i < defineLevel2.size(); i++) {
			subDef = defineLevel2.get(i);
			if (subDef.isEmpty()) {
				repIndex = i;
				break;
			}
			fp = (FuncParam) subDef.get(0);
			if (fp.isRepeatable()) {
				repIndex = i;
				break;
			}
		}
		if (inputLevel2.size() > defineLevel2.size()) {
			if (repIndex < 0) {
				return null;
			}
		}

		ArrayList<FuncParam> al = new ArrayList<FuncParam>();
		ArrayList<FuncParam> subDefine = new ArrayList<FuncParam>();
		ArrayList<FuncParam> subList = new ArrayList<FuncParam>();
		for (int i = 0; i < inputLevel2.size(); i++) {
			IParamTreeNode subParam = (IParamTreeNode) inputLevel2.get(i);
			if (repIndex == 0 && defineLevel2.size() > 1) { // 有特殊的函数，比如A.pselect(x1:y1,x2:y2,......xi:yi{,k})
				if (i != 0 && i == inputLevel2.size() - 1) {
					subDefine = defineLevel2.get(defineLevel2.size() - 1);
					subList = match3(subDefine, subParam);
					if (subList == null) {
						subDefine = defineLevel2.get(repIndex);
						subList = match3(subDefine, subParam);
					}
				} else {
					subDefine = defineLevel2.get(repIndex);
					subList = match3(subDefine, subParam);
					if (subList == null) {
						subDefine = defineLevel2.get(defineLevel2.size() - 1);
						subList = match3(subDefine, subParam);
					}
				}
			} else {
				if (i >= defineLevel2.size()) {
					subDefine = defineLevel2.get(repIndex);
				} else {
					subDefine = defineLevel2.get(i);
				}
				subList = match3(subDefine, subParam);
			}
			if (subList == null) {
				return null;
			} else {
				al.addAll(subList);
			}
		}

		// 补齐被省略掉的参数
		for (int i = inputLevel2.size(); i < defineLevel2.size(); i++) {
			al.addAll(defineLevel2.get(i));
		}
		return al;
	}

	/**
	 * 三级匹配，也即冒号的匹配 三级参数定义为，冒号可以n个，但只能是固定的n个
	 * 
	 * @param paramDefine
	 * @param rootExp
	 * @return
	 */
	private static ArrayList<FuncParam> match3(
			ArrayList<FuncParam> paramDefine, IParamTreeNode rootExp) {
		ArrayList<ArrayList<FuncParam>> defineLevel3 = splitParamList(
				paramDefine, IParamTreeNode.COLON);
		ArrayList<IParamTreeNode> inputLevel3 = getSubLevelParams(rootExp,
				IParamTreeNode.COMMA);
		if (inputLevel3.size() > defineLevel3.size()) {
			return null;
		}

		ArrayList<FuncParam> al = new ArrayList<FuncParam>();
		for (int i = 0; i < inputLevel3.size(); i++) {
			ArrayList<FuncParam> subDefine = defineLevel3.get(i);
			if (subDefine.isEmpty()) {
				continue;
			}
			FuncParam cloneDefine = (FuncParam) ((FuncParam) subDefine.get(0))
					.deepClone();

			IParamTreeNode subParam = (IParamTreeNode) inputLevel3.get(i);
			if (subParam != null) {
				cloneDefine.setParamValue(subParam.getContent());
			}
			al.add(cloneDefine);
		}

		// 补齐被省略掉的参数
		for (int i = inputLevel3.size(); i < defineLevel3.size(); i++) {
			ArrayList<FuncParam> subDefine = (ArrayList<FuncParam>) defineLevel3
					.get(i);
			if (subDefine.isEmpty()) {
				continue;
			}
			FuncParam cloneDefine = (FuncParam) ((FuncParam) subDefine.get(0))
					.deepClone();
			al.add(cloneDefine);
		}
		return al;
	}

	/**
	 * 将指定的参数定义拆分成按sign级别分割的参数列表
	 * 
	 * @param params
	 *            ArrayList
	 * @param sign
	 *            char
	 * @return
	 */
	private static ArrayList<ArrayList<FuncParam>> splitParamList(
			ArrayList<FuncParam> params, char sign) {
		ArrayList<ArrayList<FuncParam>> al = new ArrayList<ArrayList<FuncParam>>();
		ArrayList<FuncParam> sub = new ArrayList<FuncParam>();
		al.add(sub);
		if (params != null) {
			for (int i = 0; i < params.size(); i++) {
				FuncParam fp = params.get(i);
				if (fp.getPreSign() == sign) {
					if (i != 0) {
						sub = new ArrayList<FuncParam>();
						al.add(sub);
					}
				}
				sub.add(fp);
			}
		}
		return al;
	}

	/**
	 * 获得位置p所在的函数参数串
	 * 
	 * @param exp
	 *            String
	 * @param p
	 *            int
	 * @return String
	 */
	static String getFuncParams(String exp, int p) {
		Point point = pFuncParams(exp, p);
		if (point == null) {
			return "";
		}
		return exp.substring(point.x, point.y);
	}

	/**
	 * 定位函数参数
	 * 
	 * @param exp
	 * @param p
	 * @return
	 */
	private static Point pFuncParams(String exp, int p) {
		if (p >= exp.length()) {
			return null;
		}

		// 光标如果是落在函数名上，也算定位函数
		if (p > 1
				&& (exp.charAt(p - 1) == '@' || Sentence.isWordChar(exp
						.charAt(p - 1)))) {
			for (int i = p; i < exp.length(); i++) {
				if (exp.charAt(i) == '(') {
					p = i + 1;
					break;
				}
				if (exp.charAt(i) != '@' && !Sentence.isWordChar(exp.charAt(i))) {
					break;
				}
			}
		}
		for (int i = p - 1; i >= 0; i--) {
			if (exp.charAt(i) == '(') {
				int r = Sentence.scanParenthesis(exp, i);
				if (r > 0 && r >= p) {
					return new Point(i + 1, r);
				}
			}
		}
		return null;
	}

	/**
	 * 扫描标识符
	 * 
	 * @param str
	 * @param end
	 * @return
	 */
	static int scanPreIdentifier(String str, int end) {
		int i = end;
		char ch;
		while (i > 0) {
			i--;
			ch = str.charAt(i);
			if (ch == (char) 0) {
				break;
			}
			if (ch == (char) 1) {
				break;
			}
			if (ch != '@' && !Character.isJavaIdentifierPart(ch)) {
				break;
			}
		}

		while (i < end) {
			ch = str.charAt(i);
			if (Character.isJavaIdentifierStart(ch)) {
				break;
			}
			i++;
		}

		return i;
	}

	/**
	 * 解析参数字符串
	 * 
	 * @param paramStr
	 *            参数字符串
	 * @param prevLevel
	 *            结点的父级别
	 * @return
	 */
	private static IParamTreeNode parse(String paramStr, char prevLevel) {
		if (paramStr == null) {
			return null;
		}
		paramStr = paramStr.trim();
		if (paramStr.length() == 0) {
			return null;
		}

		if (prevLevel == IParamTreeNode.COLON) { // :节点下只能是叶子节点
			return new LeafParam(paramStr);
		}

		char level = getNextLevel(prevLevel);
		while (!hasSeparator(paramStr, level)) {
			if (level == IParamTreeNode.COLON) {
				return new LeafParam(paramStr);
			} else {
				level = getNextLevel(level);
			}
		}

		SymbolParam param = new SymbolParam(level);
		ArgumentTokenizer arg = new ArgumentTokenizer(paramStr, level);
		while (arg.hasMoreElements()) {
			param.addSub(parse(arg.nextToken(), level));
		}

		return param;
	}

	/**
	 * 取下一个级别
	 * 
	 * @param prevLevel
	 *            上一个级别
	 * @return
	 */
	private static char getNextLevel(char prevLevel) {
		switch (prevLevel) {
		case IParamTreeNode.NORMAL:
			return IParamTreeNode.SEMICOLON;
		case IParamTreeNode.SEMICOLON:
			return IParamTreeNode.COMMA;
		case IParamTreeNode.COMMA:
			return IParamTreeNode.COLON;
		default:
			throw new RQException();
		}
	}

	/**
	 * 是否有分隔符
	 * 
	 * @param str
	 * @param separator
	 * @return
	 */
	private static boolean hasSeparator(String str, char separator) {
		int len = str.length();
		int index = 0;
		while (index < len) {
			char ch = str.charAt(index);

			if (ch == separator) {
				return true;
			}
			if (ch == '\\') {
				index += 2;
			} else if (ch == '\"' || ch == '\'') {
				int tmp = Sentence.scanQuotation(str, index);
				if (tmp < 0) {
					return false;
				} else {
					index = tmp + 1;
				}
			} else if (ch == '(') {
				int tmp = Sentence.scanParenthesis(str, index);
				if (tmp < 0) {
					return false;
				} else {
					index = tmp + 1;
				}
			} else if (ch == '[') {
				int tmp = Sentence.scanBracket(str, index);
				if (tmp < 0) {
					return false;
				} else {
					index = tmp + 1;
				}
			} else if (ch == '{') {
				int tmp = Sentence.scanBrace(str, index);
				if (tmp < 0) {
					return false;
				} else {
					index = tmp + 1;
				}
			} else {
				index++;
			}
		}

		return false;
	}
}
