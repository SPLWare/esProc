package com.raqsoft.ide.dfx.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.raqsoft.chart.Consts;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.dfx.etl.EtlConsts;
import com.raqsoft.ide.dfx.etl.FieldDefine;
import com.raqsoft.ide.dfx.etl.ObjectElement;
import com.raqsoft.ide.dfx.etl.ParamInfo;
import com.raqsoft.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 f.import()
 * 函数名前缀F表示文件对象
 * 
 * @author Joancy
 *
 */
public class FImport extends ObjectElement {
	public ArrayList<FieldDefine> fields;
	public String argk;
	public String argn;
	public String seperator="\t";

	//选项：tcsiqokmbednv
	public boolean t;
	public boolean c;
	public boolean s;
	public boolean i;
	public boolean q;
	public boolean o;
	public boolean k;
	public boolean m;
	public boolean b;
	public boolean e;
	public boolean d;
	public boolean n;
	public boolean v;
	public boolean a;
	public boolean f;
	public boolean one;
	public boolean p;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(FImport.class, this);

		paramInfos.add(new ParamInfo("fields", EtlConsts.INPUT_FIELDDEFINE_NORMAL));
		paramInfos.add(new ParamInfo("argk"));
		paramInfos.add(new ParamInfo("argn"));
		paramInfos.add(new ParamInfo("seperator", EtlConsts.INPUT_SEPERATOR));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("t", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("c", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("s", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("q", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("o", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("k", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("m", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("b", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("e", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("d", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("n", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("v", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("a", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("f", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("one", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("p", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_FILE
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_FILE;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_SEQUENCE
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_SEQUENCE;
	}

	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer options = new StringBuffer();
//		tcsiqokmbednv
		if(t){
			options.append("t");
		}
		if(c){
			options.append("c");
		}
		if(s){
			options.append("s");
		}
		if(i){
			options.append("i");
		}
		if(q){
			options.append("q");
		}
		if(o){
			options.append("o");
		}
		if(k){
			options.append("k");
		}
		if(m){
			options.append("m");
		}
		if(b){
			options.append("b");
		}
		if(e){
			options.append("e");
		}
		if(d){
			options.append("d");
		}
		if(n){
			options.append("n");
		}
		if(v){
			options.append("v");
		}
		if(a){
			options.append("a");
		}
		if(f){
			options.append("f");
		}
		if(one){
			options.append("1");
		}
		if(p){
			options.append("p");
		}
		return options.toString();
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "import";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getFieldDefineExp(fields));
		
		StringBuffer suffix = new StringBuffer();
		if(StringUtils.isValidString(argk)){
			suffix.append(getNumberExp(argk));
			suffix.append(":");
			suffix.append(getNumberExp(argn));
		}
		suffix.append(",");
		suffix.append(getParamExp(seperator));
		if(suffix.length()>0){
			sb.append(";");
			sb.append(suffix.toString());
		}
		if(sb.length()==2){
			return null;
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,";");
		fields = getFieldDefine( st.nextToken() );
		if(st.hasMoreTokens()){
			String buf = st.nextToken();
			st = new StringTokenizer( buf, ",");
			buf = st.nextToken();
			if(isValidString(buf)){
				int maohao=buf.indexOf(":");
				if(maohao>0){
					argk = getNumber(buf.substring(0,maohao));
					argn = getNumber(buf.substring(maohao+1));
				}else{
					argk = getNumber( buf );
				}
			}
			if(st.hasMoreTokens()){
				seperator = getParam( st.nextToken() );
			}
		}
		return true;
	}

}
