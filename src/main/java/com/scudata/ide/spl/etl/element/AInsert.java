package com.scudata.ide.spl.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.scudata.chart.Consts;
import com.scudata.common.StringUtils;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.FieldDefine;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 A.insert()
 * 函数名前缀A表示序表
 * 
 * @author Joancy
 *
 */
public class AInsert extends ObjectElement {
	public int k=0;
	public String originalTable;//依照源表插入，该属性也可以依照整数长度插入，没法混合编辑，忽略该整数写法，自己写表达式去
	public ArrayList<FieldDefine> expFields;//插入的值以及对应字段
	
	public boolean n;
	public boolean r;
	public boolean f;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(AInsert.class, this);

		paramInfos.add(new ParamInfo("k",Consts.INPUT_INTEGER));
		paramInfos.add(new ParamInfo("originalTable",EtlConsts.INPUT_CELLA));
		paramInfos.add(new ParamInfo("expFields",EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("n", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("r", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("f", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}


	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return 前缀A开头的函数，均返回EtlConsts.TYPE_SEQUENCE
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_SEQUENCE;
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
		if(n){
			options.append("n");
		}
		if(r){
			options.append("r");
		}
		if(f){
			options.append("f");
		}
		return options.toString();
	}
	
	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "insert";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append( k );
		if(StringUtils.isValidString(originalTable)){
			sb.append(":");
			sb.append(originalTable);
		}
		
		String buf = getFieldDefineExp2(expFields);
		if(!buf.isEmpty()){
			sb.append(",");
			sb.append( buf );
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,",");
		String tmp = st.nextToken();
		StringTokenizer header = new StringTokenizer(tmp,":");
		k = Integer.parseInt(header.nextToken());
		if(header.hasMoreTokens()){
			tmp = header.nextToken();
			originalTable = tmp;
		}
		
		if(st.hasMoreTokens()){
			tmp = st.nextToken(";");
			expFields = getFieldDefine2(tmp);
		}
		return true;
	}
}
