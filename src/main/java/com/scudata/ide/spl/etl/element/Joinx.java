package com.scudata.ide.spl.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.scudata.chart.Consts;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 joinx()
 * 
 * @author Joancy
 *
 */
public class Joinx extends ObjectElement {
	public String srcCursor;//连接的源游标
	public ArrayList<String> srcKeys;//连接的源字段
	public String dstCursor;//连接的目标游标
	public ArrayList<String> dstKeys;//连接的目标字段
	
	public boolean f;
	public boolean one;
	public boolean p;
	public boolean u;
	public boolean i;
	public boolean d;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(Joinx.class, this);

		paramInfos.add(new ParamInfo("srcCursor",EtlConsts.INPUT_CELLCURSOR,true));
		paramInfos.add(new ParamInfo("srcKeys",EtlConsts.INPUT_STRINGLIST));
		paramInfos.add(new ParamInfo("dstCursor",EtlConsts.INPUT_CELLAORCS,true));//选项u时可以为序表
		paramInfos.add(new ParamInfo("dstKeys",EtlConsts.INPUT_STRINGLIST));

		String group = "options";
		paramInfos.add(group, new ParamInfo("f", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("one", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("p", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("u", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("d", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_EMPTY
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_EMPTY;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_CURSOR
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_CURSOR;
	}

	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer options = new StringBuffer();
		if(f){
			options.append("f");
		}
		if(one){
			options.append("1");
		}
		if(p){
			options.append("p");
		}
		if(u){
			options.append("u");
		}
		if(i){
			options.append("i");
		}
		if(d){
			options.append("d");
		}
		return options.toString();
	}
	
	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "joinx";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getExpressionExp(srcCursor));
		sb.append(",");
		sb.append( getStringListExp(srcKeys,",") );
		sb.append(";");
		sb.append(getExpressionExp(dstCursor));
		sb.append(",");
		sb.append( getStringListExp(dstKeys,",") );
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer( funcBody,";");
		String buf = st.nextToken();
		StringTokenizer tmpST = new StringTokenizer( buf, ",");
		srcCursor = getExpression( tmpST.nextToken() );
		srcKeys = getStringList( tmpST.nextToken(";"),",");
		
		buf = st.nextToken();
		tmpST = new StringTokenizer( buf, ",");
		dstCursor = getExpression( tmpST.nextToken() );
		dstKeys = getStringList( tmpST.nextToken(";"),",");
		
		return true;
	}

}
