package com.scudata.dm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.IRecord;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

/**
 * 序表的数据结构
 * @author WangXiaoJun
 *
 */
public class DataStruct implements Externalizable, IRecord {
	public static final byte Col_AutoIncrement = 0x01; // 列自动增长属性
	
	private static final long serialVersionUID = 0x02010001;
	private static final String DefNamePrefix = "_"; // 默认字段名前缀
	
	private static final int SIGN_TIMEKEY = 0x01;
	private static final int SIGN_SEQKEY = 0x02;

	private String[] fieldNames; // 字段名称
	private String[] primary; // 结构主键
	private int sign = 0; // 标志，取值为上面定义的常量
	transient private int[] pkIndex; // 主键索引

	// 序列化时使用
	public DataStruct() {}

	/**
	 * 构建数据结构
	 * @param fields 字段名数组
	 */
	public DataStruct(String[] fields) {
		if (fields == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("ds.colSize"));
		}

		int count = fields.length;
		this.fieldNames = fields;

		for (int i = 0; i < count; ++i) {
			String name = fields[i];
			if (name== null || name.length() == 0) {
				name = DefNamePrefix + (i + 1);
				fields[i] = name;
			} /*else if (name.charAt(0) == '#') {
				// 字段名可能是#abc，不再当主键指示符了
				name = name.substring(1);
				if (name.length() == 0) {
					name = DefNamePrefix + (i + 1);
				}

				if (pkList == null) {
					pkList = new ArrayList<String>();
				}
				
				fields[i] = name;
				pkList.add(name);
			}*/

			// 不检查名字的合法性，sql语句可能产生空名字段或相同名字的字段
		}
	}

	/**
	 * 把数据结构序列化成字节数组
	 * @return 字节数组
	 */
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeStrings(fieldNames);
		out.writeStrings(primary);
		out.writeInt(sign);
		return out.toByteArray();
	}

	/**
	 * 由字节数组填充数据结构
	 * @param buf 字节数组
	 */
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		fieldNames = in.readStrings();
		setPrimary(in.readStrings());
		
		if (in.available() > 0) {
			sign = in.readInt();
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(3); // 版本号
		out.writeObject(fieldNames);
		out.writeObject(primary);
		out.writeInt(sign); // 版本3添加
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int v = in.readByte(); // 版本号
		fieldNames = (String[])in.readObject();
		setPrimary((String[])in.readObject());
		
		if (v > 2) { // 版本3添加
			sign = in.readInt();
		}
	}

	/**
	 * 复制数据结构
	 */
	public DataStruct dup() {
		String []names = new String[fieldNames.length];
		System.arraycopy(fieldNames, 0, names, 0, names.length);
		DataStruct ds = new DataStruct(names);
		ds.setPrimary(primary);
		ds.sign = sign;
		return ds;
	}

	/**
	 * 创建一个新结构，使用源结构的主键信息
	 * @param newFields 新结构字段名
	 * @return
	 */
	public DataStruct create(String []newFields) {
		DataStruct ds = new DataStruct(newFields);
		String []primary = this.primary;
		
		if (primary != null) {
			int keyCount = primary.length;
			int delCount = 0;
			boolean []signs = new boolean[keyCount];
			for (int i = 0; i < keyCount; ++i) {
				if (ds.getFieldIndex(primary[i]) == -1) {
					delCount++;
					signs[i] = true;
				}
			}

			// 某个主键没了，把他丢掉
			if (delCount > 0) {
				if (delCount < keyCount) {
					String []newPrimary = new String[keyCount - delCount];
					for (int i = 0, seq = 0; i < keyCount; ++i) {
						if (!signs[i]) {
							newPrimary[seq] = primary[i];
							seq++;
						}
					}

					ds.setPrimary(newPrimary);
				}
			} else {
				ds.setPrimary(primary);
				ds.sign = sign;
			}
		}

		return ds;
	}

	/**
	 * 返回字段的索引，如果字段不存在返回-1
	 * @param fieldName String
	 * @return int
	 */
	public int getFieldIndex(String fieldName) {
		if (fieldName == null || fieldName.length() == 0) return -1;

		int fcount = fieldNames.length;
		for (int i = 0; i < fcount; ++i) {
			if (fieldName.equals(fieldNames[i])) {
				return i;
			}
		}

		// 索引id
		if (KeyWord.isFieldId(fieldName)) {
			int i = KeyWord.getFiledId(fieldName);
			if (i > 0 && i <= fcount) {
				return i - 1;
			}
		}
		
		return -1;
	}

	/**
	 * 返回字段的名名称
	 * @param int index 字段索引，从0开始计数
	 * @return String
	 */
	public String getFieldName(int index) {
		if (index < 0 || index >= fieldNames.length) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(index + mm.getMessage("ds.fieldNotExist"));
		}

		return fieldNames[index];
	}

	/**
	 * 设置字段名称
	 * @param fieldNames
	 */
	public void setFieldName(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}
	
	/**
	 * 返回字段数目
	 * @return int
	 */
	public int getFieldCount() {
		return fieldNames.length;
	}

	/**
	 * 返回字段名称数组
	 * @return String[]
	 */
	public String[] getFieldNames() {
		return fieldNames;
	}

	/**
	 * 返回两数据结构是否兼容
	 * @param other DataStruct
	 * @return boolean
	 */
	public boolean isCompatible(DataStruct other) {
		if (other == this) return true;
		if (other == null) return false;

		String []names = other.fieldNames;
		if (fieldNames.length != names.length) return false;

		// 字段顺序需要一致
		for (int i = 0, count = names.length; i < count; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				if (fieldNames[i] != null && fieldNames[i].length() != 0) {
					return false;
				}
			} else {
				if (!names[i].equals(fieldNames[i])) return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 判断结构是否与给定的字段名数组兼容，需要字段名相同并且字段顺序相同
	 * @param names 字段名数组
	 * @return
	 */
	public boolean isCompatible(String []names) {
		if (fieldNames.length != names.length) return false;

		// 字段顺序需要一致
		for (int i = 0, count = names.length; i < count; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				if (fieldNames[i] != null && fieldNames[i].length() != 0) {
					return false;
				}
			} else {
				if (!names[i].equals(fieldNames[i])) return false;
			}
		}
		
		return true;
	}

	/**
	 * 设置结构的主键
	 * @param names String[]
	 */
	public void setPrimary(String []names) {
		setPrimary(names, null);
	}
	
	/**
	 * 设置结构的主键
	 * @param names String[]
	 * @param opt String t：最后一个为
	 */
	public void setPrimary(String []names, String opt) {
		sign = 0;
		if (names == null || names.length == 0) {
			this.primary = null;
			pkIndex = null;
			
			if (opt != null && opt.indexOf('n') != -1) {
				sign |= SIGN_SEQKEY;
			}
		} else {
			int count = names.length;
			int []tmpIndex = new int[count];
			for (int i = 0; i < count; ++i) {
				tmpIndex[i] = getFieldIndex(names[i]);
				if (tmpIndex[i] == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(names[i] + mm.getMessage("ds.fieldNotExist"));
				}
			}

			this.primary = new String[count];
			System.arraycopy(names, 0, this.primary, 0, count);
			pkIndex = tmpIndex;
			if (opt != null) {
				if (opt.indexOf('t') != -1) {
					sign |= SIGN_TIMEKEY;
				}
				
				if (opt.indexOf('n') != -1) {
					sign |= SIGN_SEQKEY;
				}
			}
		}
	}

	/**
	 * 返回结构的主键
	 * @return String[]
	 */
	public String[] getPrimary() {
		return primary;
	}

	/**
	 * 返回主键是否是序号键
	 * @return
	 */
	public boolean isSeqKey() {
		return (sign & SIGN_SEQKEY) == SIGN_SEQKEY;
	}
	
	/**
	 * 取时间键数量，没有时间键则返回0
	 * @return
	 */
	public int getTimeKeyCount() {
		return (sign & SIGN_TIMEKEY) == SIGN_TIMEKEY ? 1 : 0;
	}

	/**
	 * 返回主键在结构中的索引，没有定义主键则返回空
	 * @return int[]
	 */
	public int[] getPKIndex() {
		return pkIndex;
	}

	/**
	 * 取主键数，如果没有设置主键则返回0
	 * @return
	 */
	public int getPKCount() {
		if (primary != null) {
			return primary.length;
		} else {
			return 0;
		}
	}
	
	/**
	 * 取基本建的索引，不包含更新键
	 * @return int[]
	 */
	public int[] getBaseKeyIndex() {
		if (getTimeKeyCount() == 0) {
			return pkIndex;
		} else {
			int count = pkIndex.length - 1;
			int []index = new int[count];
			System.arraycopy(pkIndex, 0, index, 0, count);
			return index;
		}
	}
	
	/**
	 * 取更新时间键的索引
	 * @return
	 */
	public int getTimeKeyIndex() {
		return pkIndex[pkIndex.length - 1];
	}
	
	/**
	 * 重命名指定字段
	 * @param srcFields
	 * @param newFields
	 */
	public void rename(String []srcFields, String []newFields) {
		if (srcFields == null) {
			return;
		}
		
		String[] fieldNames = this.fieldNames; // 普通字段
		for (int i = 0, count = srcFields.length; i < count; ++i) {
			int f = getFieldIndex(srcFields[i]);
			if (f < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(srcFields[i] + mm.getMessage("ds.fieldNotExist"));
			}
			
			if (newFields[i] != null) {
				fieldNames[f] = newFields[i];
			} else {
				fieldNames[f] = DefNamePrefix + (f + 1);
			}
			
			if (pkIndex != null) {
				for (int k = 0; k < pkIndex.length; ++k) {
					if (pkIndex[k] == f) {
						primary[k] = fieldNames[f];
						break;
					}
				}
			}
		}
	}
	
	/**
	 * 判断表达式是否是指定的字段
	 * @param exps 表达式数组
	 * @param fields 字段索引数组
	 * @return true：是相同字段
	 */
	public boolean isSameFields(Expression []exps, int []fields) {
		int len = exps.length;
		if (len != fields.length) {
			return false;
		}
		
		for (int i = 0; i < len; ++i) {
			String field = exps[i].getIdentifierName();
			if (fields[i] != getFieldIndex(field)) {
				return false;
			}
		}
		
		return true;
	}
}
