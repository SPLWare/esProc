package com.scudata.ide.spl.etl;

import java.lang.reflect.*;
import java.util.ArrayList;

import com.scudata.chart.Consts;
import com.scudata.common.*;

import java.awt.*;

/**
 * 参数信息
 * 
 * @author Joancy
 *
 */
public class ParamInfo extends FuncParam {
	private String title;
	private int inputType;
	private Object defValue;// 只给编辑用的缺省值，用于删除表达式后，用该值渲染，该值跟类的缺省值一致

	// 当前正在获取参数的类，需要从该类中根据参数名获取相应参数的定义
	private static transient Class currentClass;
	private static transient Object currentObj;
	private static transient Object defaultObj;

	private MessageManager mm = FuncMessage.get();

	private boolean needCheckEmpty = false;//是否需要检查属性值为空
	
	/**
	 * 设置当前对象实例，取资源时需要根据实例查找相关属性
	 * @param objClass Class
	 * @param obj 对象
	 */
	public static void setCurrent(Class objClass, Object obj) {
		currentClass = objClass;
		currentObj = obj;
		try {
			defaultObj = objClass.newInstance();
		} catch (Exception e) {
		}
	}

	/**
	 * 构造函数
	 * @param name 参数名
	 * @param needCheck 是否需要检查空值
	 * 如果检查空值，则在编辑界面时，遇到空值会报错，并阻止界面退出
	 */
	public ParamInfo(String name,boolean needCheck) {
		this(name, Consts.INPUT_NORMAL,needCheck);
	}
	
	/**
	 * 构造函数
	 * @param name 参数名
	 */
	public ParamInfo(String name) {
		this(name, Consts.INPUT_NORMAL);
	}

	/**
	 * 构造函数
	 * @param name 参数名
	 * @param inputType 输入类型
	 */
	public ParamInfo(String name, int inputType) {
		this(name,inputType,false);
	}
	
	/**
	 * 构造函数
	 * @param name 参数名
	 * @param inputType 输入类型
	 * @param needCheck 是否检查空值
	 */
	public ParamInfo(String name, int inputType, boolean needCheck) {
		this.name = name;
		this.needCheckEmpty = needCheck;
		try {
			Field f = currentClass.getDeclaredField(name);
			Object paraValue = f.get(currentObj);
			if (paraValue instanceof Color) {
				this.value = new Integer(((Color) paraValue).getRGB());
			} else {
				this.value = paraValue;
			}
			String className = currentClass.getName();
			int last = className.lastIndexOf('.');
			String prefix = className.substring(last+1);
			this.title = mm.getMessage(prefix+"."+name);
			this.inputType = inputType;
			this.defValue = f.get(defaultObj);
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	/**
	 * 获取参数的缺省值
	 * @return 缺省值
	 */
	public Object getDefValue() {
		return defValue;
	}

	/**
	 * 设置函数参数对象，将值赋给当前实例
	 * @param fp 函数参数
	 */
	public void setFuncParam(FuncParam fp) {
		Object tmp = fp.getValue();
		value = tmp;
	}

	/**
	 * 参数标题，用于显示在编辑界面
	 * @return 标题
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * 获取输入类型
	 * 参数的编辑界面根据输入类型提供不同的编辑器
	 * @return 输入类型
	 */
	public int getInputType() {
		return inputType;
	}
	
	/**
	 * 检查空参数，空值时抛出相应异常
	 */
	public void check(){
		if(needCheckEmpty){
			String sValue=null;
			if(value instanceof ArrayList){
				ArrayList al = (ArrayList)value;
				if(al.size()>0){
					Object element = al.get(0);
					if(element instanceof FieldDefine){
						ArrayList<FieldDefine> tmp = (ArrayList<FieldDefine>)value;
						sValue = ObjectElement.getFieldDefineExp2(tmp);
					}else if(element instanceof String){
						ArrayList<String> tmp = (ArrayList<String>)value;
						sValue = ObjectElement.getStringListExp(tmp, ",");
					}
				}
			}else if(value instanceof String){
				sValue = (String)value;
			}
			if(!StringUtils.isValidString( sValue )){
				throw new RuntimeException(mm.getMessage("EmptyWarning",title));
			}
		}
	}

}
