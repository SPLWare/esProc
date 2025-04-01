package com.scudata.dm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.ICloneable;
import com.scudata.common.IRecord;

/**
 * 参数列表，用于保存变量、参数、常量
 * @author WangXiaoJun
 *
 */
public class ParamList implements Cloneable, ICloneable, Externalizable, IRecord {
	private static final long serialVersionUID = 0x05000004;

	private List<Param> vList;
	private boolean isUserChangeable; //是否每次重新运行时设置参数

	public ParamList() {
	}

	/**
	 * 把另一个参数列表里的参数添加到当前参数列表中
	 * @param pl 参数列表
	 */
	public void addAll(ParamList pl) {
		if (pl.vList != null && pl.vList.size() > 0) {
			if (vList == null) {
				vList = new ArrayList<Param>(pl.vList);
			} else {
				vList.addAll(pl.vList);
			}
		}
	}

	/**
	 * 添加参数
	 * @param v Param 参数
	 */
	public void add(Param v) {
		if (vList == null) {
			vList = new ArrayList<Param>(4);
		}
		
		vList.add(v);
	}

	/**
	 * 添加参数到指定位置
	 * @param index int 指定位置
	 * @param v Param 参数
	 */
	public void add(int index, Param v) {
		if (isValid(v)) {
			if (vList == null) {
				vList = new ArrayList<Param>(index + 1);
			}
			
			vList.add(index, v);
		}
	}

	/**
	 * 按给定名称、参数类型、参数值来添加无标题参数
	 * @param name String 参数名称
	 * @param kind byte 参数类型
	 * @param value Object 参数值
	 */
	public void add(String name, byte kind, Object value) {
		Param v = new Param(name, kind, value);
		if (isValid(v)) {
			if (vList == null) {
				vList = new ArrayList<Param>(4);
			}
			
			vList.add(v);
		}
	}

	/**
	 * 按给定名称、参数值来添加无标题变量
	 * @param name String 变量名称
	 * @param value Object 变量参数值
	 */
	public void addVariable(String name, Object value) {
		add(name, Param.VAR, value);
	}

	/**
	 * 按给定名称、标题、参数值来添加参数
	 * @param name String 参数名称
	 * @param value Object 参数值
	 */
	public void addArgument(String name, Object value) {
		add(name, Param.ARG, value);
	}

	/**
	 * 按给定名称、标题、参数值来添加常参数
	 * @param name String 常参数名称
	 * @param value Object 常参数值
	 */
	public void addConstant( String name, Object value ) {
		add(name, Param.CONST, value);
	}

	/**
	 * 移除指定位置参数
	 * @param index int 指定位置
	 * @return Param 移除参数
	 */
	public Param remove(int index) {
		if (vList == null || vList.size() <= index ) {
			return null;
		}
		
		return vList.remove(index);
	}

	/**
	 * 移除指定名称参数
	 * @param name String 指定名称
	 * @return Param 移除参数
	 */
	public Param remove(String name) {
		if (vList == null) {
			return null;
		}
		
		for (int i = 0, iCount = vList.size(); i<iCount; i++) {
			Param p = vList.get(i);
			if (p != null && p.getName().equals(name)) {
				return vList.remove(i);
			}
		}
		
		return null;
	}

	/**
	 * 获取指定位置参数
	 * @param index int 指定位置
	 * @return Param 参数
	 */
	public Param get(int index) {
		if (vList == null || vList.size() <= index) {
			return null;
		}
		
		return vList.get(index);
	}

	/**
	 * 获取指定名称参数
	 * @param name String 指定名称
	 * @return Param 参数
	 */
	public Param get(String name) {
		if (vList == null) {
			return null;
		}
		
		for (int i = 0, iCount = vList.size(); i < iCount; i++) {
			Param p = vList.get(i);
			if (p != null && p.getName().equals(name)) {
				return p;
			}
		}
		
		return null;
	}

	/**
	 * 查找值等于value的参数
	 * @param value Object 参数值
	 * @return Param
	 */
	public Param getByValue(Object value) {
		if (vList == null) {
			return null;
		}
		
		for (int i = 0, iCount = vList.size(); i < iCount; i++) {
			Param p = vList.get(i);
			if (p != null && p.getValue() == value) {
				return p;
			}
		}
		
		return null;
	}

	/**
	 * 获取所有变量
	 * @param varList ParamList 变量参数列表
	 */
	public void getAllVarParams(ParamList varList) {
		if (vList == null) {
			return;
		}
		
		for (int i = 0, count = vList.size(); i < count; ++i) {
			Param p = vList.get(i);
			if (p != null && p.getKind() == Param.VAR) {
				varList.add(p);
			}
		}
	}

	/**
	 * 获取所有参数
	 * @param expParamList ParamList 参数列表
	 */
	public void getAllArguments(ParamList expParamList) {
		if (vList == null) {
			return;
		}
		
		for (int i = 0, count = vList.size(); i < count; ++i) {
			Param p = vList.get(i);
			if (p != null && p.getKind() == Param.ARG) {
				expParamList.add(p);
			}
		}
	}
	
	/**
	 * 获取所有常参数
	 * @param varList ParamList 常参数列表
	 */
	public void getAllConsts(ParamList varList) {
		if (vList == null) {
			return;
		}
		
		for (int i = 0, count = vList.size(); i < count; ++i) {
			Param p = (Param)vList.get(i);
			if (p != null && p.getKind() == Param.CONST) {
				varList.add(p);
			}
		}
	}

	/**
	 * 是否包含指定参数
	 * @param p Param 指定参数
	 * @return boolean 是否包含
	 */
	public boolean contains(Param p) {
		return (vList == null ? false : vList.contains(p));
	}

	/**
	 * 参数总数
	 * @return int 总数
	 */
	public int count() {
		if (vList == null) {
			return 0;
		}
		
		return vList.size();
	}

	/**
	 * 清空参数列表
	 */
	public void clear() {
		vList = null;
	}

	/**
	 * 深度复制
	 * @return Object
	 */
	public Object deepClone() {
		ParamList pl = new ParamList();
		pl.isUserChangeable = isUserChangeable;

		List<Param> vList = this.vList;
		if (vList != null) {
			int size = vList.size();
			pl.vList = new ArrayList<Param>(size);
			for (int i = 0; i < size; i++) {
				Param v = vList.get(i);
				pl.vList.add((Param)v.deepClone());
			}
		}

		return pl;
	}

	public void setUserChangeable(boolean changeable){
	  isUserChangeable = changeable;
	}

	public boolean isUserChangeable(){
	  return isUserChangeable;
	}

	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		List<Param> vList = this.vList;
		if(vList == null){
			out.writeShort((short)0);
		} else{
			int size = vList.size();
			out.writeShort((short)size);
			for (int i = 0; i < size; i++) {
				Param v = vList.get(i);
				out.writeRecord(v);
			}
		}
		
		out.writeBoolean(isUserChangeable);
		return out.toByteArray();
	}

	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		int count = in.readShort();
		if (count > 0) {
			vList = new ArrayList<Param>(count);
			for (int i = 0; i < count; i++) {
				Param v = new Param();
				in.readRecord(v);
				vList.add(v);
			}
		}
		
		isUserChangeable = in.readBoolean();
	}

	private boolean isValid(Param o) {
		if (o == null) {
			return false;
		} else {
			String name = o.getName();
			if (name == null) return false;
			if (vList == null) return true;
			
			for (int i = 0, iCount = vList.size(); i < iCount; i++) {
				Param v = (Param)vList.get(i);
				if (v != null && v.getName().equals(name)) return false;
			}
			
			return true;
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(1);
		out.writeObject(vList);
		out.writeBoolean(isUserChangeable);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readByte(); // version
		this.vList = (List<Param>)in.readObject();
		isUserChangeable = in.readBoolean();
	}
}
