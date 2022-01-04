package com.scudata.ide.spl.etl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.scudata.chart.Para;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.dm.Sequence;
import com.scudata.ide.common.GC;

/**
 * 辅助函数的对象元素基类
 * 
 * cellName.funcName(args...)
 * @author Joancy
 *
 */
public abstract class ObjectElement implements IFuncObject{
	public MessageManager mm = FuncMessage.get();
	public static String SNULL = "_NULL_";
	
	String elementName;
	String cellName = null;//格子名字， 对应于函数的实例格子

	public abstract byte getReturnType();//函数返回类型
	public abstract String getFuncName();//函数书写名称
	public abstract String getFuncBody();//函数体表达式
	public abstract String optionString();//函数选项
	public void checkEmpty(){};//属性必填项空值检查
	
	public abstract ParamInfoList getParamInfoList();//参数列表
	public abstract boolean setFuncBody(String funcBody);//函数体
	
	/**
	 * 设置函数的选项
	 * @param options 选项
	 */
	public void setOptions(String options){
		if(options==null){
			return;
		}
		String group = FuncMessage.get().getMessage("options");
		ArrayList<ParamInfo> ofs = getParamInfoList().getParams(group);
		if(ofs==null){
			return;
		}
		for(ParamInfo pi:ofs){
			String fieldName = pi.getName();
			try {
				Field  fieldOption = getClass().getField(fieldName);
//				0和1选项的属性名为对应的英文
				if(fieldName.equals("zero")){
					fieldName = "0";
				}else if(fieldName.equals("one")){
					fieldName = "1";
				}
				Object value = options.indexOf(fieldName)>-1;
				fieldOption.set(this, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * 将函数定义转换为表达式类型串
	 * @return 表达式串
	 */
	public String toExpressionString() {
		StringBuffer sb = new StringBuffer();
		if(getReturnType()==EtlConsts.TYPE_EMPTY){
			sb.append(">");
		}else{
			sb.append("=");
		}
		if(StringUtils.isValidString(cellName)){
			sb.append(cellName);
			sb.append(".");
		}
		sb.append(getFuncName());
		sb.append(getOptions());
		sb.append("(");
		String body = getFuncBody();
		if(StringUtils.isValidString(body)){
			sb.append(body);
		}
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * 设置元素名称
	 * 
	 * 元素名字不同于函数名funcName， funcName可以不唯一； 但是元素名字必须唯一
	 * 用于解决同名函数问题； 比如， db.query(sql, arg)  db.query(A,sql,arg);
	 * 此处db为cellName， query为funcName，不需要定义；而elementName则可能为  SQLQuery， SequenceQuery，用于区分不同的函数体
	 * @param eleName 元素名称
	 */
	public void setElementName(String eleName){
		this.elementName = eleName;
	}
	/**
	 * 获取元素名称
	 * @return 名称
	 */
	public String getElementName(){
		return elementName;
	}
	
	/**
	 * 获取函数的描述信息
	 * 该信息存储在资源文件，显示在编辑窗口
	 * @return 描述信息
	 */
	public String getFuncDesc(){
		return mm.getMessage(elementName+".desc");
	}
	
	/**
	 * 获取该函数的帮助链接
	 * @return 链接地址
	 */
	public String getHelpUrl(){
		String url = mm.getMessage(elementName+".url");
		if(url.startsWith(elementName)){
			String prefix = "http://doc.raqsoft.com.cn/esproc/func/";
			if(GC.LANGUAGE==GC.ENGLISH){
				prefix = "http://doc.raqsoft.com/esproc/func/";
			}
			return prefix+elementName.toLowerCase()+".html";
		}
		return url;
	}
	
	/**
	 * 设置单元格名称
	 * @param cellName 名称
	 */
	public void setCellName(String cellName){
		this.cellName = cellName;
	}
	/**
	 * 获取单元格名称
	 * @return
	 */
	public String getCellName(){
		return cellName;
	}
	
	/**
	 * 设置参数信息列表
	 * @param paramInfos 参数信息列表
	 */
	public void setParamInfoList(ArrayList<ParamInfo> paramInfos){
		Sequence params = new Sequence();
		for(ParamInfo pi:paramInfos){
			params.add(pi);
		}
		setParams(getClass(),this,params);
	}

	private void setParams(Class elementClass, ObjectElement elementObject,
			Sequence funcParams) {
		int size = funcParams.length();
		for (int i = 1; i <= size; i++) {
			FuncParam fp = (FuncParam) funcParams.get(i);
			Para p = new Para(fp.getValue());
			Field f = null;
			try {
				f = elementClass.getField(fp.getName());
				String className = f.getType().getName().toLowerCase();
				if (className.endsWith("boolean")) {
					f.set(elementObject, new Boolean(p.booleanValue()));
				} else if (className.endsWith("byte")) {
					f.set(elementObject, new Byte((byte) p.intValue()));
				} else if (className.endsWith("int")
						|| className.endsWith("integer")) {
					f.set(elementObject, new Integer(p.intValue()));
				} else if (className.endsWith("float")) {
					f.set(elementObject, new Float(p.floatValue()));
				} else if (className.endsWith("double")) {
					f.set(elementObject, new java.lang.Double(p.doubleValue()));
				} else if (className.endsWith(".color")) {// 加上点就不会把chartcolor当成color了
					f.set(elementObject, p.colorValue(0));
				} else if (className.endsWith("string")) {
					f.set(elementObject, p.stringValue());
				} else if (className.endsWith("sequence")) {
					f.set(elementObject, p.sequenceValue());
				} else if (className.endsWith("date")) {
					f.set(elementObject, p.dateValue());
				} else if (className.endsWith("chartcolor")) {
					f.set(elementObject, p.chartColorValue());
				} else {
					f.set(elementObject, p.getValue());
				}
			} catch (java.lang.NoSuchFieldException nField) {
			} catch (RQException rqe) {
				throw rqe;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * 将参数值列表args根据分隔符seperator拼成表达式串
	 * @param args 参数值列表
	 * @param seperator 分隔符
	 * @return 表达式串
	 */
	public static String getStringListExp(ArrayList<String> args,String seperator){
		if(args==null || args.isEmpty()){
			return "";
		}
		StringBuffer options = new StringBuffer();
		for(int i=0;i<args.size();i++){
			if(i>0){
				options.append(seperator);
			}
			options.append(args.get(i));
		}
		return options.toString();
	}
	
	/**
	 * getStringListExp函数的逆操作
	 * @param args 表达式串
	 * @param seperator 分隔符
	 * @return 参数值列表
	 */
	public static ArrayList<String> getStringList(String args,String seperator){
		if(!isValidString(args)){
			return null;
		}
		ArrayList<String> sl = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(args,seperator);
		while(st.hasMoreTokens()){
			sl.add(st.nextToken());
		}
		return sl;
	}
	
	/**
	 * 获取表达式拼串的选项表示
	 * @return 选项表示
	 */
	public String getOptions(){
		String options = optionString();
		if(StringUtils.isValidString(options)){
			return "@"+options.toString();
		}else{
			return "";
		}
	}
	
	/**
	 * 将参数值paramValue按照参数表示规则拼为SPL表达式串
	 * @param paramValue 参数值
	 * @return SPL表达式串
	 */
	public static String getParamExp(String paramValue){
		if(paramValue==null){
			return "";
		}
		if(paramValue.startsWith("=")){
			return paramValue.substring(1);
		}
		return Escape.addEscAndQuote(paramValue);
	}
	/**
	 * getParamExp的逆操作
	 * @param paramValue SPL表达式串
	 * @return 参数值的内存表示 
	 */
	public static String getParam(String paramValue){
		if(!isValidString(paramValue)){
			return "";
		}
		if(paramValue.startsWith("\"")){
			return Escape.removeEscAndQuote(paramValue);
		}
		return "="+paramValue;
	}
	
	/**
	 * 将参数值paramValue按照表达式表示规则拼为SPL表达式串
	 * @param paramValue 参数值
	 * @return SPL表达式串
	 */
	public static String getExpressionExp(String paramValue){
		if(!isValidString(paramValue)){
			return "";
		}
		if(paramValue.startsWith("=")){
			return paramValue.substring(1);
		}
		return paramValue;
	}
	
	/**
	 * getExpressionExp的逆操作
	 * @param paramValue SPL表达式串
	 * @return 参数值的内存表示 
	 */
	public static String getExpression(String paramValue){
		if(!isValidString(paramValue)){
			return "";
		}
		String exp = paramValue;
		int idx = 0;
		int len = exp.length();
		int tmp = Sentence.scanIdentifier( exp, idx );
		if( tmp < len - 1 ) {//标识符比长度短时，则是表达式
			return "="+paramValue;
		}
		return paramValue;
	}
	
	/**
	 * 将参数值paramValue按照数值表示规则拼为SPL表达式串
	 * @param paramValue 参数值
	 * @return SPL表达式串
	 */
	public static String getNumberExp(String paramValue){
		if(!isValidString(paramValue)){
			return "";
		}
		if(paramValue.startsWith("=")){
			return paramValue.substring(1);
		}
		return paramValue;
	}
	
	/**
	 * getNumberExp的逆操作
	 * @param paramValue SPL表达式串
	 * @return 内存参数值
	 */
	public static String getNumber(String paramValue){
		if(!isValidString(paramValue)){
			return "";
		}
		try{
			Double d = Double.valueOf(paramValue);
			//如果是数值，直接返回当前表达式
			return paramValue;
		}catch(Exception x){
			//否则当表达式
			return "="+paramValue;
		}
	}

	/**
	 * 将参数值fields按照表达式表示规则拼为SPL表达式串
	 * @param fields 字段定义列表
	 * @return SPL表达式串
	 */
	public static String getFieldDefineExp(ArrayList<FieldDefine> fields){
		if(fields==null || fields.isEmpty()){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		if(fields!=null){
			for(FieldDefine fd:fields){
				if(sb.length()>0){
					sb.append(",");
				}
				sb.append(fd.getOne());
				if(StringUtils.isValidString(fd.getTwo())){
					sb.append(":");
					sb.append(fd.getTwo());
				}
				if(StringUtils.isValidString(fd.getThree())){
					sb.append(":");
					sb.append(fd.getThree());
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * getFieldDefineExp的逆操作
	 * 反向字段定义，从串生成字段定义列表
	 * @param fields SPL表达式串
	 * @return 字段定义列表
	 */
	public static ArrayList<FieldDefine> getFieldDefine(String fields){
		if(!isValidString(fields)){
			return null;
		}
		ArrayList<FieldDefine> fds = new ArrayList<FieldDefine>();
		StringTokenizer st = new StringTokenizer(fields,",");
		while(st.hasMoreTokens()){
			String section = st.nextToken();
			StringTokenizer token = new StringTokenizer(section,":");
			FieldDefine fd = new FieldDefine();
			fd.setOne(token.nextToken());
			if(token.hasMoreTokens()){
				fd.setTwo(token.nextToken());	
			}
			if(token.hasMoreTokens()){
				fd.setThree(token.nextToken());	
			}
			fds.add(fd);
		}
		return fds;
	}
	
	/**
	 * 将参数值fields按照表达式表示规则2拼为SPL表达式串
	 * 从FieldDefine对象生成规则2：没有字段名时，缺省使用表达式本身  A:A
	 * @param fields 字段定义列表
	 * @return SPL表达式串
	 */
	public static String getFieldDefineExp2(ArrayList<FieldDefine> fields){
		if(fields==null || fields.isEmpty()){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		if(fields!=null){
			for(FieldDefine fd:fields){
				if(sb.length()>0){
					sb.append(",");
				}
				sb.append(fd.getOne());
				sb.append(":");
				if(StringUtils.isValidString(fd.getTwo())){
					sb.append(fd.getTwo());
				}else{
					sb.append(fd.getOne());
				}
			}
		}
		return sb.toString();
	}

	/**
	 * getFieldDefineExp2的逆操作
	 * 反向字段定义，从串生成字段定义列表
	 * @param fields SPL表达式串
	 * @return 字段定义列表
	 */
	public static ArrayList<FieldDefine> getFieldDefine2(String fields){
		if(!StringUtils.isValidString(fields)){
			return null;
		}
		ArrayList<FieldDefine> fds = new ArrayList<FieldDefine>();
		StringTokenizer st = new StringTokenizer(fields,",");
		while( st.hasMoreTokens() ){
			String tmp = st.nextToken();
			StringTokenizer tmpST = new StringTokenizer(tmp,":");
			String one = tmpST.nextToken();
			String two = tmpST.nextToken();
			FieldDefine fd = new FieldDefine();
			fd.setOne(one);
			if(!one.equals(two)){
				fd.setTwo(two);
			}
			fds.add(fd);
		}
		return fds;
	}
	
	/**
	 * 判断参数值是否为表达式写法
	 * @param paramValue 参数值
	 * @return 表达式写法时返回true，否则返回false
	 */
	public static boolean isExpression(String paramValue){
		if(paramValue==null){
			return false;
		}
		return paramValue.startsWith("=");
	}
	
	/**
	 * 函数参数省略中间时，会造成连续的分隔符，比如“A;;B”, 此时第2节，用Tokenizer会跳过，
	 * 为了避免这类情况，此处将这些分隔符中间插上 NULL 常量
	 * @param param 参数值
	 * @return 校正后的参数值
	 */
	private static String verifyParam(String param){
		if(param.startsWith(";")){
			param = SNULL+param;
		}
		param = Sentence.replace(param, ";;", ";"+SNULL+";", 0);
		param = Sentence.replace(param, ";;", ";"+SNULL+";", 0);
		param = Sentence.replace(param, ";,", ";"+SNULL+",", 0);
		param = Sentence.replace(param, ";:", ";"+SNULL+":", 0);
		param = Sentence.replace(param, ",:", ","+SNULL+":", 0);
		return param;
	}
	
	/**
	 * 处理字串时，使用了NULL字符串做
	 * @param str
	 * @return
	 */
	public static boolean isValidString(String str){
		if(!StringUtils.isValidString(str)){
			return false;
		}
		return !str.equals(SNULL);
	}
	
	/**
	 * 表达式串解析为函数对象
	 * @param exp 表达式
	 * @param oes 所有编辑好的单元格跟对象的映射表
	 * @return 对象元素
	 */
	public static ObjectElement parseString(String exp, HashMap<String, ObjectElement> oes){
		if(!isValidString(exp)){
			return null;
		}
		exp = exp.trim();
		if(!(exp.startsWith("=") || exp.startsWith(">"))){
			return null;
		}
//		=file@s("d:/1.txt")  =A1.create@o(arg1)
		int index = exp.indexOf("(");
		if(index<0){
			return null;
		}
		int iTmp = Sentence.scanParenthesis(exp, index);
		if (iTmp < 0) {
			return null;
		} else if(iTmp<(exp.length()-1)){//不支持多级函数连写 A1.import().sort()
			return null;
		}

		String celName = null;//变量，格子名 A1
		String funName = null;//函数名 create
		String options = null;//选项 o
		String funBody = null;//函数体 arg1

		String tmp = exp.substring(1,index);
		int dot1 = tmp.indexOf(".");
		if(dot1>0){
			celName = tmp.substring(0,dot1);
			tmp = tmp.substring(dot1+1);
		}
		int indexOption = tmp.indexOf("@");
		if(indexOption>0){
			funName = tmp.substring(0,indexOption);
			tmp = tmp.substring(indexOption+1);
		}else{
			funName = tmp;
			tmp = null;
		}
		options = tmp;
		funBody = exp.substring(index+1,exp.length()-1);
		ArrayList<ElementInfo> funObjs = ElementLib.getElementInfos(funName);
		if(funObjs.isEmpty()){
			return null;
		}
		ElementInfo ei=null;
		if( funObjs.size()==1 ){
			ei = funObjs.get(0);
		}else{
			ObjectElement parent = oes.get(celName);
			byte parentType;
			if(parent==null){
				parentType = EtlConsts.TYPE_EMPTY;
			}else{
				parentType = parent.getReturnType();
			}
			int c = 0;
			ArrayList<ElementInfo> tmpFunObjs = new ArrayList<ElementInfo>();
			for(ElementInfo tmpEI:funObjs){
				ObjectElement tmpOE = tmpEI.newInstance();
				if(tmpOE.getParentType()==parentType){
					ei = tmpEI;
					c++;
					tmpFunObjs.add(ei);
					if(!StringUtils.isValidString(funBody)){
						break;
					}
				}
			}
			if(c>1){
				for(ElementInfo tmpEI:tmpFunObjs){
					ObjectElement tmpOE = tmpEI.newInstance();
					String tmps = verifyParam(funBody);
					if(tmpOE.setFuncBody(tmps)){
						ei = tmpEI;
						break;
					}
				}
			}
		}
		if(ei==null){
			return null;
		}
		ObjectElement oe = ei.newInstance();
		oe.setCellName(celName);
		oe.setOptions(options);
		if(StringUtils.isValidString(funBody)){
			funBody = verifyParam(funBody);
			oe.setFuncBody(funBody);
		}
		return oe;
	}
	
}
