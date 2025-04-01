package com.scudata.chart.edit;

import java.lang.reflect.*;

import com.scudata.chart.*;
import com.scudata.chart.resources.*;
import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.expression.*;

import java.awt.*;

/**
 * 参数信息类，用于描述某个具体参数的编辑类型
 * @author Joancy
 *
 */
public class ParamInfo extends ChartParam {

	private String title;
	private int inputType;
	private boolean axisEnable = false;
	private Object defValue;// 只给编辑用的缺省值，用于删除表达式后，用该值渲染，该值跟类的缺省值一致

	private static transient Class currentClass;
	private static transient Object currentObj;

	private MessageManager mm = ChartMessage.get();

	/**
	 * 图元的继承是多层的，为了准确定位到具体在哪一层父类
	 * 使用该方法描述当前工作层
	 * @param objClass 具体的某层父类
	 * @param obj 该类的实例对象
	 */
	public static void setCurrent(Class objClass, Object obj) {
		currentClass = objClass;
		currentObj = obj;
	}

	/**
	 * 构造一个常规编辑类型的参数信息
	 * @param name 参数名称
	 */
	public ParamInfo(String name) {
		this(name, Consts.INPUT_NORMAL);
	}

	/**
	 * 构造指定参数编辑类型的参数信息
	 * @param name 名称
	 * @param inputType 编辑类型，值参考：Consts.INPUT_XXX
	 */
	public ParamInfo(String name, int inputType) {
		this.name = name;
		try {
			Field f = currentClass.getDeclaredField(name);
			Object paraValue = f.get(currentObj);
			if (paraValue instanceof Para) {
				this.value = ((Para) paraValue).getValue();
				this.axisEnable = true;
			} else if (paraValue instanceof Color) {
				this.value = new Integer(((Color) paraValue).getRGB());
				// }else if(paraValue instanceof Sequence){
				// this.value = getSequenceEditExp( seq );
			} else {
				this.value = paraValue;
			}
			this.title = mm.getMessage(name);
			this.inputType = inputType;
			this.defValue = value;
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	/**
	 * 获取该参数的缺省值
	 * @return 值
	 */
	public Object getDefValue() {
		return defValue;
	}

	private String getSequenceEditExp(Sequence seq) {
		return "=" + seq.toString();
	}

	/**
	 * 根据基类cp的内容，设置当前内容
	 * @param cp 基类参数
	 */
	public void setChartParam(ChartParam cp) {
		Object tmp = cp.getValue();
		if (tmp instanceof Sequence) {
			Sequence seq = (Sequence) tmp;
			tmp = Utils.sequenceToChartColor(seq);
			if (tmp == null) {
				tmp = getSequenceEditExp(seq);
			}
		}
		value = tmp;

		if (axisEnable) {
			axis = cp.getAxis();
		}
	}

	public boolean isAxisEnable() {
		return axisEnable;
	}

	/**
	 * 获取参数标题
	 * @return 标题内容
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * 设置参数的标题
	 * @param title 标题内容
	 */
	public void setTitle(String title){
		this.title = title;
	}

	/**
	 * 获取参数的编辑类型
	 * @return Consts中定义好的编辑类型
	 */
	public int getInputType() {
		return inputType;
	}

}
