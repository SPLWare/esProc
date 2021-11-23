package com.scudata.ide.dfx.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.scudata.chart.Consts;
import com.scudata.common.StringUtils;
import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.FieldDefine;
import com.scudata.ide.dfx.etl.ObjectElement;
import com.scudata.ide.dfx.etl.ParamInfo;
import com.scudata.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 db.update()
 * 函数名前缀D表示数据库连接
 * 
 * @author Joancy
 *
 */
public class DUpdate extends ObjectElement {
	public String aOrCs;
	public String originalA;
	public String tableName;
	
	public ArrayList<FieldDefine> updateFields;
	public ArrayList<String> keys;
	
	public boolean u;
	public boolean i;
	public boolean a;
	public boolean k;
	public boolean one;
	public boolean d;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(DUpdate.class, this);

		paramInfos.add(new ParamInfo("aOrCs", EtlConsts.INPUT_CELLAORCS,true));
		paramInfos.add(new ParamInfo("originalA", EtlConsts.INPUT_CELLA));
		paramInfos.add(new ParamInfo("tableName",true));
		paramInfos.add(new ParamInfo("updateFields", EtlConsts.INPUT_FIELDDEFINE_FIELD_EXP));
		paramInfos.add(new ParamInfo("keys", EtlConsts.INPUT_STRINGLIST));
		String group = "options";
		paramInfos.add(group, new ParamInfo("u", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("a", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("k", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("one", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("d", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_DB
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_DB;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_EMPTY
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_EMPTY;
	}

	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer options = new StringBuffer();
		if(u){
			options.append("u");
		}
		if(i){
			options.append("i");
		}
		if(a){
			options.append("a");
		}
		if(k){
			options.append("k");
		}
		if(one){
			options.append("1");
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
		return "update";
	}
	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(aOrCs);
		if(StringUtils.isValidString(originalA)){
			sb.append(":");
			sb.append(originalA);
		}
		
		sb.append(",");
		sb.append(tableName);
		String buf = getFieldDefineExp2(updateFields);
		if(StringUtils.isValidString(buf)){
			sb.append(",");
			sb.append(buf);	
		}
		String keysStr = getStringListExp(keys,",");
		if(StringUtils.isValidString(keysStr)){
			sb.append(";");
			sb.append(keysStr);	
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		int fenhao = funcBody.indexOf(";");
		String header;
		if(fenhao>0){
			header = funcBody.substring(0,fenhao);
			String tmp = funcBody.substring(fenhao+1);
			keys = getStringList(tmp,",");
		}else{
			header = funcBody;
		}
		StringTokenizer st = new StringTokenizer(header,",");
		String buf = st.nextToken();
		int maohao = buf.indexOf(":");
		if(maohao>0){
			aOrCs = buf.substring(0,maohao);
			originalA = buf.substring(maohao+1);
		}else{
			aOrCs = buf;
		}
		tableName = st.nextToken();
		buf = st.nextToken(";");//将后面的所有字段全部截出来
		if(StringUtils.isValidString(buf)){
			buf = buf.substring(1);//去掉全截后，多出的首字符逗号
			updateFields = getFieldDefine2(buf);
		}
		return true;
	}

}
