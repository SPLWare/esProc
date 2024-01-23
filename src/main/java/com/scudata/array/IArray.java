package com.scudata.array;

import java.io.Externalizable;
import java.util.Comparator;

import com.scudata.common.IRecord;

/**
 * 数组接口，从1开始计数
 * @author WangXiaoJun
 *
 */
public interface IArray extends Externalizable, IRecord, Comparable<IArray> {
	static final int DEFAULT_LEN = 8; // 默认构造函数创建的数组的默认长度
	
	/**
	 * 取数组的类型串，用于错误信息提示
	 * @return 类型串
	 */
	String getDataType();
	
	/**
	 * 追加元素，如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	void add(Object o);
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	void addAll(Object[] array);
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	void addAll(IArray array);
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param count 元素个数
	 */
	void addAll(IArray array, int count);
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param index 要加入的数据的起始位置
	 * @param count 数量
	 */
	void addAll(IArray array, int index, int count);
	
	/**
	 * 插入元素，如果类型不兼容则抛出异常
	 * @param index 插入位置，从1开始计数
	 * @param o 元素值
	 */
	void insert(int index, Object o);
	
	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	void insertAll(int pos, IArray array);
	
	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	void insertAll(int pos, Object []array);
	
	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素），如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	void push(Object o);
	
	/**
	 * 追加一个空成员（不检查容量，认为有足够空间存放元素）
	 */
	void pushNull();
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	void push(IArray array, int index);
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	void add(IArray array, int index);
	
	/**
	 * 把array中的第index个元素设给到当前数组的指定元素，如果类型不兼容则抛出异常
	 * @param curIndex 当前数组的元素索引，从1开始计数
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	void set(int curIndex, IArray array, int index);
	
	/**
	 * 取指定位置元素
	 * @param index 索引，从1开始计数
	 * @return
	 */
	Object get(int index);
	
	/**
	 * 取指定位置元素组成新数组
	 * @param indexArray 位置数组
	 * @return IArray
	 */
	IArray get(int []indexArray);
	
	/**
	 * 取指定位置元素组成新数组
	 * @param indexArray 位置数组
	 * @param start 起始位置，包含
	 * @param end 结束位置，包含
	 * @param doCheck true：位置可能包含0，0的位置用null填充，false：不会包含0
	 * @return IArray
	 */
	IArray get(int []indexArray, int start, int end, boolean doCheck);
	
	/**
	 * 取某一区段组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @return IArray
	 */
	IArray get(int start, int end);
	
	/**
	 * 取指定位置元素组成新数组
	 * @param IArray 位置数组
	 * @return IArray
	 */
	IArray get(IArray indexArray);
	
	/**
	 * 取指定位置元素的整数值
	 * @param index 索引，从1开始计数
	 * @return
	 */
	int getInt(int index);
	
	/**
	 * 取指定位置元素的长整数值
	 * @param index 索引，从1开始计数
	 * @return
	 */
	long getLong(int index);

	/**
	 * 使列表的容量不小于minCapacity
	 * @param minCapacity 最小容量
	 */
	void ensureCapacity(int minCapacity);
	
	/**
	 * 判断指定位置的元素是否是空
	 * @param index 索引，从1开始计数
	 * @return
	 */
	boolean isNull(int index);
	
	/**
	 * 判断元素是否是真
	 * @return BoolArray
	 */
	BoolArray isTrue();
	
	/**
	 * 判断元素是否是假
	 * @return BoolArray
	 */
	BoolArray isFalse();
	
	/**
	 * 判断指定位置的元素是否是True
	 * @param index 索引，从1开始计数
	 * @return
	 */
	boolean isTrue(int index);
	
	/**
	 * 判断指定位置的元素是否是False
	 * @param index 索引，从1开始计数
	 * @return
	 */
	boolean isFalse(int index);
	
	/**
	 * 是否是计算过程中临时产生的数组，临时产生的可以被修改，比如 f1+f2+f3，只需产生一个数组存放结果
	 * @return true：是临时产生的数组，false：不是临时产生的数组
	 */
	boolean isTemporary();
	
	/**
	 * 设置是否是计算过程中临时产生的数组
	 * @param ifTemporary true：是临时产生的数组，false：不是临时产生的数组
	 */
	void setTemporary(boolean ifTemporary);
	
	/**
	 * 删除最后一个元素
	 */
	void removeLast();
	
	/**
	 * 删除指定位置的元素
	 * @param index 索引，从1开始计数
	 * @return Object 被删除的元素的值
	 */
	void remove(int index);
	
	/**
	 * 删除指定位置的元素
	 * @param seqs 索引数组
	 */
	void remove(int []seqs);
	
	/**
	 * 删除指定区间内的元素
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 */
	void removeRange(int fromIndex, int toIndex);
	
	/**
	 * 返回数组的元素数目
	 * @return int
	 */
	int size();
	
	/**
	 * 设置数组的元素数目
	 * @param int
	 */
	void setSize(int size);
	
	/**
	 * 返回数组布尔判断取值为真的元素数目
	 * @return 非空元素数目
	 */
	int count();
	
	/**
	 * 判断数组是否有取值为true的元素
	 * @return true：有，false：没有
	 */
	boolean containTrue();
	
	/**
	 * 返回第一个不为空的元素
	 * @return Object
	 */
	Object ifn();
	
	/**
	 * 修改数组指定元素的值，如果类型不兼容则抛出异常
	 * @param index 索引，从1开始计数
	 * @param obj 值
	 */
	void set(int index, Object obj);
	
	/**
	 * 删除所有的元素
	 */
	void clear();
	
	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @return 元素的索引,如果不存在返回负的插入位置.
	 */
	int binarySearch(Object elem);
	
	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @param start 起始查找位置（包含）
	 * @param end 结束查找位置（包含）
	 * @return 元素的索引,如果不存在返回负的插入位置.
	 */
	int binarySearch(Object elem, int start, int end);
	
	/**
	 * 返回列表中是否包含指定元素
	 * @param elem Object 待查找的元素
	 * @return boolean true：包含，false：不包含
	 */
	boolean contains(Object elem);
	
	/**
	 * 判断数组的元素是否在当前数组中
	 * @param isSorted 当前数组是否有序
	 * @param array 数组
	 * @param result 用于存放结果，只找取值为true的
	 */
	void contains(boolean isSorted, IArray array, BoolArray result);
	
	/**
	 * 返回列表中是否包含指定元素，使用等号比较
	 * @param elem
	 * @return boolean true：包含，false：不包含
	 */
	boolean objectContains(Object elem);

	/**
	 * 返回元素在数组中首次出现的位置
	 * @param elem 待查找的元素
	 * @param start 起始查找位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	int firstIndexOf(Object elem, int start);
	
	/**
	 * 返回元素在数组中最后出现的位置
	 * @param elem 待查找的元素
	 * @param start 从后面开始查找的位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	int lastIndexOf(Object elem, int start);

	/**
	 * 返回元素在数组中所有出现的位置
	 * @param elem 待查找的元素
	 * @param start 起始查找位置（包含）
	 * @param isSorted 当前数组是否有序
	 * @param isFromHead true：从头开始遍历，false：从尾向前开始遍历
	 * @return IntArray
	 */
	IntArray indexOfAll(Object elem, int start, boolean isSorted, boolean isFromHead);
	
	/**
	 * 复制数组
	 * @return
	 */
	IArray dup();
	
	/**
	 * 返回一个同类型的数组
	 * @param count
	 * @return
	 */
	IArray newInstance(int count);
	
	/**
	 * 对数组成员求绝对值
	 * @return IArray 绝对值数组
	 */
	IArray abs();
	
	/**
	 * 对数组成员求负
	 * @return IArray 负值数组
	 */
	IArray negate();
	
	/**
	 * 对数组成员求非
	 * @return IArray 非值数组
	 */
	IArray not();
	
	/**
	 * 判断数组的成员是否都是数（可以包含null）
	 * @return true：都是数，false：含有非数的值
	 */
	boolean isNumberArray();
	
	/**
	 * 计算两个数组的相对应的成员的和
	 * @param array 右侧数组
	 * @return 和数组
	 */
	IArray memberAdd(IArray array);
	
	/**
	 * 计算数组的成员与指定常数的和
	 * @param value 常数
	 * @return 和数组
	 */
	IArray memberAdd(Object value);
	
	/**
	 * 计算两个数组的相对应的成员的差
	 * @param array 右侧数组
	 * @return 差数组
	 */
	IArray memberSubtract(IArray array);
	
	/**
	 * 计算两个数组的相对应的成员的积
	 * @param array 右侧数组
	 * @return 积数组
	 */
	IArray memberMultiply(IArray array);

	/**
	 * 计算数组的成员与指定常数的积
	 * @param value 常数
	 * @return 积数组
	 */
	IArray memberMultiply(Object value);

	/**
	 * 计算两个数组的相对应的成员的除
	 * @param array 右侧数组
	 * @return 商数组
	 */
	IArray memberDivide(IArray array);
	
	/**
	 * 计算两个数组的相对应的数成员取余或序列成员异或列
	 * @param array 右侧数组
	 * @return 余数数组或序列异或列数组
	 */
	IArray memberMod(IArray array);
	
	/**
	 * 计算两个数组的数成员整除或序列成员差集
	 * @param array 右侧数组
	 * @return 整除值数组或序列差集数组
	 */
	IArray memberIntDivide(IArray array);

	/**
	 * 计算两个数组的相对应的成员的关系运算
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @return 关系运算结果数组
	 */
	BoolArray calcRelation(IArray array, int relation);
	
	/**
	 * 计算数组的成员和指定值的关系运算
	 * @param value 右侧值
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @return 关系运算结果数组
	 */
	BoolArray calcRelation(Object value, int relation);
	
	/**
	 * 计算两个数组的相对应的成员的关系运算，只计算result为真的行
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	void calcRelations(IArray array, int relation, BoolArray result, boolean isAnd);
	
	
	/**
	 * 计算两个数组的相对应的成员的关系运算，只计算result为真的行
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	void calcRelations(Object value, int relation, BoolArray result, boolean isAnd);
	
	/**
	 * 计算两个数组的相对应的成员的按位与
	 * @param array 右侧数组
	 * @return 按位与结果数组
	 */
	IArray bitwiseAnd(IArray array);
	
	/**
	 * 计算两个数组的相对应的成员的按位或
	 * @param array 右侧数组
	 * @return 按位或结果数组
	 */
	IArray bitwiseOr(IArray array);
	
	/**
	 * 计算两个数组的相对应的成员的按位异或
	 * @param array 右侧数组
	 * @return 按位异或结果数组
	 */
	IArray bitwiseXOr(IArray array);
	
	/**
	 * 计算数组成员的按位取反
	 * @return 成员按位取反结果数组
	 */
	IArray bitwiseNot();
	
	/**
	 * 计算数组的2个成员的比较值
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	int memberCompare(int index1, int index2);
	
	/**
	 * 判断数组的两个成员是否相等
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	boolean isMemberEquals(int index1, int index2);

	/**
	 * 比较两个数组的大小
	 * @param array 右侧数组
	 * @return 1：当前数组大，0：两个数组相等，-1：当前数组小
	 */
	int compareTo(IArray array);

	/**
	 * 比较两个数组的大小
	 * @param array 右侧数组
	 * @param comparator 比较器，中文排序等可用到
	 * @return 1：当前数组大，0：两个数组相等，-1：当前数组小
	 */
	//int compareTo(IArray array, Comparator<Object> comparator);
	
	/**
	 * 取指定成员的哈希值
	 * @param index 成员索引，从1开始计数
	 * @return 指定成员的哈希值
	 */
	int hashCode(int index);
	
	/**
	 * 求成员和
	 * @return
	 */
	Object sum();

	/**
	 * 求平均值
	 * @return
	 */
	Object average();
	
	/**
	 * 得到最大的成员
	 * @return
	 */
	Object max();
	
	/**
	 * 得到最小的成员
	 * @return
	 */
	Object min();
	
	/**
	 * 保留指定区间内的数据
	 * @param start 起始位置（包含）
	 * @param end 结束位置（包含）
	 */
	void reserve(int start, int end);
	
	/**
	 * 把成员转成对象数组返回
	 * @return 对象数组
	 */
	Object[] toArray();
	
	/**
	 * 把成员填到指定的数组
	 * @param result 用于存放成员的数组
	 */
	void toArray(Object []result);
	
	/**
	 * 把数组从指定位置拆成两个数组
	 * @param pos 位置，包含
	 * @return 返回后半部分元素构成的数组
	 */
	IArray split(int pos);
	
	/**
	 * 把指定区间元素分离出来组成新数组
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 * @return
	 */
	IArray split(int from, int to);
	
	/**
	 * 调整容量，使其与元素数相等
	 */
	void trimToSize();
	
	/**
	 * 取出标识数组取值为真的行对应的数据，组成新数组
	 * @param signArray 标识数组
	 * @return IArray
	 */
	IArray select(IArray signArray);
	
	/**
	 * 取某一区段标识数组取值为真的行组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @param signArray 标识数组
	 * @return IArray
	 */
	IArray select(int start, int end, IArray signArray);
	
	/**
	 * 判断两个数组的指定元素是否相同
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要比较的数组
	 * @param index 要比较的数组的元素的索引
	 * @return true：相同，false：不相同
	 */
	boolean isEquals(int curIndex, IArray array, int index);
	
	/**
	 * 判断数组的指定元素是否与给定值相等
	 * @param curIndex 数组元素索引，从1开始计数
	 * @param value 值
	 * @return true：相等，false：不相等
	 */
	boolean isEquals(int curIndex, Object value);
	
	/**
	 * 比较两个数组的指定元素的大小
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要比较的数组
	 * @param index 要比较的数组的元素的索引
	 * @return 小于：小于0，等于：0，大于：大于0
	 */
	int compareTo(int curIndex, IArray array, int index);

	/**
	 * 比较数组的指定元素与给定值的大小
	 * @param curIndex 当前数组的元素的索引
	 * @param value 要比较的值
	 * @return
	 */
	int compareTo(int curIndex, Object value);
	
	/**
	 * 把array的指定元素加到当前数组的指定元素上
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要相加的数组
	 * @param index 要相加的数组的元素的索引
	 * @return IArray
	 */
	IArray memberAdd(int curIndex, IArray array, int index);
	
	/**
	 * 对数组的元素进行排序
	 */
	void sort();
	
	/**
	 * 对数组的元素进行排序
	 * @param comparator 比较器
	 */
	void sort(Comparator<Object> comparator);
	
	/**
	 * 返回数组中是否含有记录
	 * @return boolean
	 */
	boolean hasRecord();
	
	/**
	 * 返回是否是（纯）排列
	 * @param isPure true：检查是否是纯排列
	 * @return boolean true：是，false：不是
	 */
	boolean isPmt(boolean isPure);
	
	/**
	 * 返回数组的反转数组
	 * @return IArray
	 */
	IArray rvs();
	
	/**
	 * 对数组元素从小到大做排序，取前count个的位置
	 * @param count 如果count小于0则取后|count|名的位置
	 * @param isAll count为正负1时，如果isAll取值为true则取所有排名第一的元素的位置，否则只取一个
	 * @param isLast 是否从后开始找
	 * @param ignoreNull 是否忽略空元素
	 * @return IntArray
	 */
	IntArray ptop(int count, boolean isAll, boolean isLast, boolean ignoreNull);
	
	/**
	 * 对数组元素从小到大做排名，取前count名的位置
	 * @param count 如果count小于0则从大到小做排名
	 * @param ignoreNull 是否忽略空元素
	 * @param iopt 是否按去重方式做排名
	 * @return IntArray
	 */
	IntArray ptopRank(int count, boolean ignoreNull, boolean iopt);
	
	/**
	 * 把当前数组转成对象数组，如果当前数组是对象数组则返回数组本身
	 * @return ObjectArray
	 */
	ObjectArray toObjectArray();
	
	/**
	 * 把对象数组转成纯类型数组，不能转则抛出异常
	 * @return IArray
	 */
	IArray toPureArray();
	
	/**
	 * 保留数组数据用于生成序列或序表
	 * @param refOrigin 引用源列，不复制数据
	 * @return
	 */
	IArray reserve(boolean refOrigin);
	
	/**
	 * 根据条件从两个数组选出成员组成新数组，从当前数组选出标志为true的，从other数组选出标志为false的
	 * @param signArray 标志数组
	 * @param other 另一个数组
	 * @return IArray
	 */
	IArray combine(IArray signArray, IArray other);

	/**
	 * 根据条件从当前数组选出标志为true的，标志为false的置成value
	 * @param signArray 标志数组
	 * @param other 值
	 * @return IArray
	 */
	IArray combine(IArray signArray, Object value);
	
	/**
	 * 返回指定数组的成员在当前数组中的位置
	 * @param array 待查找的数组
	 * @param opt 选项，b：同序归并法查找，i：返回单递增数列，c：连续出现
	 * @return 位置或者位置序列
	 */
	Object pos(IArray array, String opt);
	
	/**
	 * 返回数组成员的二进制表示时1的个数和
	 * @return
	 */
	int bit1();
	
	/**
	 * 返回数组成员按位异或值的二进制表示时1的个数和
	 * @param array 异或数组
	 * @return 1的个数和
	 */
	int bit1(IArray array);
}
