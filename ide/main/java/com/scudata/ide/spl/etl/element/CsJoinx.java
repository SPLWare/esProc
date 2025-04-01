package com.scudata.ide.spl.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.scudata.chart.Consts;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.FieldDefine;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 CS.joinx()
 * 函数名前缀Cs表示游标
 * 
 * @author Joancy
 *
 */
public class CsJoinx extends ObjectElement {
	public ArrayList<String> foreignKeys;
	
	public String joinFile;//连接的集文件或者组表文件
	
	public ArrayList<String> joinKeys;//joinFile的主键，必须跟foreignKeys一一对应
	public ArrayList<FieldDefine> attachFields;//连接后附加在当前A后的表达式以及输出字段名，不使用FieldDefine的第三列
	public String bufferN="1024";//缓存区行数
	
	public boolean i;
	public boolean d;
	public boolean q;
	public boolean c;
	public boolean u;
//	public boolean o;//先不实现o选项
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(CsJoinx.class, this);

		paramInfos.add(new ParamInfo("foreignKeys",EtlConsts.INPUT_STRINGLIST));
		paramInfos.add(new ParamInfo("joinFile",EtlConsts.INPUT_CELLBCTX,true));
		paramInfos.add(new ParamInfo("joinKeys",EtlConsts.INPUT_STRINGLIST));
		paramInfos.add(new ParamInfo("attachFields",EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD));
		paramInfos.add(new ParamInfo("bufferN"));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("d", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("q", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("c", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("u", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_CURSOR
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_CURSOR;
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
		if(i){
			options.append("i");
		}
		if(d){
			options.append("d");
		}
		if(q){
			options.append("q");
		}
		if(c){
			options.append("c");
		}
		if(u){
			options.append("u");
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
		sb.append( getStringListExp(foreignKeys,":") );
		sb.append(",");
		sb.append(getExpressionExp(joinFile));
		sb.append(":");
		sb.append( getStringListExp(joinKeys,":") );
		
		String buf = getFieldDefineExp2(attachFields);
		if(!buf.isEmpty()){
			sb.append(",");
			sb.append( buf );
		}
		
		sb.append(";");
		sb.append(getNumberExp(bufferN));
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer fenhao = new StringTokenizer(funcBody,";");
		String tmp =  fenhao.nextToken();
		StringTokenizer st = new StringTokenizer(tmp,",");
		foreignKeys = getStringList(st.nextToken(),":");
		String joins = st.nextToken();
		int first = joins.indexOf(":");
		String header = joins.substring(0,first);
		joinFile = getExpression(header);
		String tailer = joins.substring(first+1);
		joinKeys = getStringList(tailer,":");
		if(st.hasMoreTokens()){
			attachFields = getFieldDefine2(st.nextToken());
		}
		if( fenhao.hasMoreTokens() ){
			bufferN = getNumber(fenhao.nextToken());
		}
		return true;
	}
}
