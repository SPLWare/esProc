package com.scudata.ide.dfx.etl;

/**
 * 元素信息类
 * 
 * @author Joancy
 *
 */
public class ElementInfo {
	private String name;
	private String title;
	private Class elementClass;

	transient ObjectElement instance = null;
	
	/**
	 * 构造函数
	 * @param name 名称
	 */
	public ElementInfo(String name) {
		this.name = name;
		ElementInfo ei = ElementLib.getElementInfo(name);
		this.title = ei.getTitle();
		this.elementClass = ei.getElementClass();
	}

	/**
	 * 构造函数
	 * @param name 名称
	 * @param title 标题
	 * @param elementClass 对应Class
	 */
	public ElementInfo(String name, String title, Class elementClass) {
		this.name = name;
		this.title = title;
		this.elementClass = elementClass;
	}

	/**
	 * 函数的父类型
	 * @return 类型
	 */
	public byte getParentType(){
		return getInstance().getParentType();
	}
	
	/**
	 * 函数的实体名称
	 * @return 函数名称
	 */
	public String getFuncName(){
		return getInstance().getFuncName();
	}

	/**
	 * 得到一个当前信息的唯一对象元素实例
	 * @return 实例
	 */
	public ObjectElement getInstance(){
		if(instance==null){
			instance = newInstance();
		}
		return instance;
	}
	
	/**
	 * 新建一个元素实例
	 * @return 实例
	 */
	public ObjectElement newInstance() {
		try {
			ObjectElement oe = (ObjectElement) elementClass.newInstance();
			oe.setElementName( name );
			return oe;
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取名称
	 * @return 名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取标题
	 * @return 标题
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * 获取元素的类Class
	 * @return Class
	 */
	public Class getElementClass() {
		return elementClass;
	}

}
