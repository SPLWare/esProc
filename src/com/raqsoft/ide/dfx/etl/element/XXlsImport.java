package com.raqsoft.ide.dfx.etl.element;

import com.raqsoft.chart.Consts;
import com.raqsoft.ide.dfx.etl.EtlConsts;
import com.raqsoft.ide.dfx.etl.ParamInfo;
import com.raqsoft.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 xls.xlsimport()
 * 函数名前缀X表示 xls文件对象
 * 
 * @author Joancy
 *
 */
public class XXlsImport extends FXlsImport {
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(FXlsImport.class, this);

		paramInfos.add(new ParamInfo("fields", EtlConsts.INPUT_STRINGLIST));
		paramInfos.add(new ParamInfo("sheetName"));
		paramInfos.add(new ParamInfo("argb"));
		paramInfos.add(new ParamInfo("arge"));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("t", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("c", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_XLS
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_XLS;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_CURSOR
	 */
	public byte getReturnType() {
		if(c){
			return EtlConsts.TYPE_CURSOR;
		}
		return EtlConsts.TYPE_SEQUENCE;
	}
}
