package com.raqsoft.lib.redis.function;

import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * RedisTool
 */
public class RedisTool {
	final private ApplicationContext appCtx;
	final private StringRedisTemplate strRedisTemplate;

	public RedisTool(ApplicationContext appCtx, StringRedisTemplate redisTemplate) {
		this.appCtx = appCtx;
		this.strRedisTemplate = redisTemplate;
	}

	public StringRedisTemplate getRedisTemplate() {
		return this.strRedisTemplate;
	}

	public ApplicationContext getApplicationContext() {
		return this.appCtx;
	}

	/** -------------------key相关操作--------------------- */

	/**
	 * 删除单个key
	 * 
	 * @param key
	 */
	public void delete(String key) {
		strRedisTemplate.delete(key);
	}

	/**
	 * 删除多个key
	 * 
	 * @param keys 键集县
	 */
	public void delete(Collection<String> keys) {
		strRedisTemplate.delete(keys);
	}

	/**
	 * 序列化key
	 * 
	 * @param key
	 * @return
	 */
	public byte[] dump(String key) {
		return strRedisTemplate.dump(key);
	}

	/**
	 * 是否存在key
	 * 
	 * @param key
	 * @return
	 */
	public Boolean hasKey(String key) {
		return strRedisTemplate.hasKey(key);
	}

	/**
	 * 给指定键设置过期时间
	 * 
	 * @param key
	 * @param timeout
	 * @param unit
	 * @return
	 */
	public Boolean expire(String key, long timeout, TimeUnit unit) {
		return strRedisTemplate.expire(key, timeout, unit);
	}
	
	public Boolean expire(String key, long timeout) {
		return strRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
	}

	/**
	 * 给指定键设置过期时间
	 * 
	 * @param key
	 * @param date
	 * @return
	 */
	public Boolean expireAt(String key, Date date) {
		return strRedisTemplate.expireAt(key, date);
	}

	/**
	 * 查找匹配的key
	 * 
	 * @param pattern
	 * @return 
	 */
	public Set<String> keys(String pattern) {
		return strRedisTemplate.keys(pattern);
	}

	/**
	 * 将指定的键移动到指定的库
	 * 
	 * @param key
	 * @param dbIndex
	 * @return
	 */
	public Boolean move(String key, int dbIndex) {
		return strRedisTemplate.move(key, dbIndex);
	}

	/**
	 * 将key持久化保存， 就是把过期或者设置了过期时间的key变为永不过期
	 * 
	 * @param key
	 * @return
	 */
	public Boolean persist(String key) {
		return strRedisTemplate.persist(key);
	}

	/**
	 * 获取过期时间
	 * 
	 * @param key
	 * @param unit
	 * @return
	 */
	public Long getExpire(String key, TimeUnit unit) {
		return strRedisTemplate.getExpire(key, unit);
	}

	/**
	 * 获取过期时间
	 * 
	 * @param key
	 * @return
	 */
	public Long getExpire(String key) {
		return strRedisTemplate.getExpire(key);
	}

	/**
	 * 随机取一个key
	 * 
	 * @return
	 */
	public String randomKey() {
		return strRedisTemplate.randomKey();
	}

	/**
	 * 修改 key 的名祿
	 * 
	 * @param oldKey
	 * @param newKey
	 */
	public void rename(String oldKey, String newKey) {
		strRedisTemplate.rename(oldKey, newKey);
	}

	/**
	 * 旧?存在时，将旧?改为新值
	 * 
	 * @param oldKey
	 * @param newKey
	 * @return
	 */
	public Boolean renameIfAbsent(String oldKey, String newKey) {
		return strRedisTemplate.renameIfAbsent(oldKey, newKey);
	}

	/**
	 * 返回 key值的类型
	 * 
	 * @param key
	 * @return
	 */
	public DataType type(String key) {
		return strRedisTemplate.type(key);
	}

	/** -------------------String数据结构相关操作--------------------- */

	/**
	 * 存入数据
	 * @param key
	 * @param value
	 */
	public void set(String key, String value) {
		strRedisTemplate.opsForValue().set(key, value);
	}

	/**
	 * 获取数据
	 * @param key
	 * @return
	 */
	public String get(String key) {
		return strRedisTemplate.opsForValue().get(key);
	}

	/**
	 * 返回 key值的子字串
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public String getRange(String key, long start, long end) {
		return strRedisTemplate.opsForValue().get(key, start, end);
	}

	/**
	 * 将给 key的设置value，并返回 key旧值(old value)
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public String getAndSet(String key, String value) {
		return strRedisTemplate.opsForValue().getAndSet(key, value);
	}

	/**
	 * 判断指定的offset位置ASCII码的bit位是否为1
	 * 
	 * @param key
	 * @param offset
	 * @return
	 */
	public Boolean getBit(String key, long offset) {
		return strRedisTemplate.opsForValue().getBit(key, offset);
	}

	/**
	 * 获取多个数据
	 * 
	 * @param keys
	 * @return
	 */
	public List<String> multiGet(Collection<String> keys) {
		return strRedisTemplate.opsForValue().multiGet(keys);
	}

	/**
	 * 设置ASCII瞿, 字符?'a'的ASCII码是97, 转为二进制是'01100001', 此方法是将二进制第offset位?变为value
	 * 
	 * @param key
	 * @param postion 
	 * @param value
	 * @return 
	 */
	public boolean setBit(String key, long offset, boolean value) {
		return strRedisTemplate.opsForValue().setBit(key, offset, value);
	}

	/**
	 * 存入数据并且设置过期时间，并宿 key 的过期时间设? timeout
	 * 
	 * @param key
	 * @param value
	 * @param timeout 过期时间
	 * @param unit
	 *            时间单位, 墿:TimeUnit.DAYS 小时:TimeUnit.HOURS 分钟:TimeUnit.MINUTES
	 *            祿:TimeUnit.SECONDS 毫秒:TimeUnit.MILLISECONDS
	 */
	public void setEx(String key, String value, long timeout, TimeUnit unit) {
		strRedisTemplate.opsForValue().set(key, value, timeout, unit);
	}

	/**
	 * 只有嚿 key不存在时设置 key 的?
	 * 
	 * @param key
	 * @param value
	 * @return 之前已经存在返回false,不存在返回true
	 */
	public boolean setIfAbsent(String key, String value) {
		return strRedisTemplate.opsForValue().setIfAbsent(key, value);
	}

	/**
	 * 给定 key储存value的字符串值，从偏移量 offset庿奿
	 * 
	 * @param key
	 * @param value
	 * @param offset
	 *            
	 */
	public void setRange(String key, String value, long offset) {
		strRedisTemplate.opsForValue().set(key, value, offset);
	}

	/**
	 * 获取字符串的长度
	 * 
	 * @param key
	 * @return
	 */
	public Long size(String key) {
		return strRedisTemplate.opsForValue().size(key);
	}

	/**
	 * 多个键?的插入
	 * 
	 * @param maps
	 */
	public void multiSet(Map<String, String> maps) {
		strRedisTemplate.opsForValue().multiSet(maps);
	}

	/**
	 * 多个键?的插入，当且仅当所有给嬿 key 都不存在
	 * 
	 * @param maps
	 * @return 之前已经存在返回false,不存在返回true
	 */
	public boolean multiSetIfAbsent(Map<String, String> maps) {
		return strRedisTemplate.opsForValue().multiSetIfAbsent(maps);
	}

	/**
	 * 给指定键 加指定整数，如果值不是数字则抛出异常； 不存在指定键创建一个初始为0的加指定整数 增加成功则返回增加后的值
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Long increment(String key) {
		return strRedisTemplate.opsForValue().increment(key);
	}
	
	public Long increment(String key, long increment) {
		return strRedisTemplate.opsForValue().increment(key, increment);
	}

	/**
	 * 给指定键 加指定双精确度数，如果?不是数字则抛出异常 不存在指定键创建?个初始为0的加指定整数 增加成功则返回增加后的?
	 * @param key
	 * @param value
	 * @return
	 */
	public Double increment(String key, double increment) {
		return strRedisTemplate.opsForValue().increment(key, increment);
	}

	/**
	 * 给指定键 的?追加字符串
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Integer append(String key, String value) {
		return strRedisTemplate.opsForValue().append(key, value);
	}

	/** -------------------hash数据结构相关操作------------------------- */

	/**
	 * 获取存储在Hash中指定字段的值
	 * 
	 * @param key
	 * @param field
	 * @return
	 */
	public Object hGet(String key, String field) {
		return strRedisTemplate.opsForHash().get(key, field);
	}

	/**
	 * 获取所有字段的值
	 * 
	 * @param key
	 * @return
	 */
	public Map<Object, Object> hGetAll(String key) {
		return strRedisTemplate.opsForHash().entries(key);
	}

	/**
	 * 获取懿有给定字段的值
	 * 
	 * @param key
	 * @param fields
	 * @return
	 */
	public List<Object> hMultiGet(String key, Collection<Object> fields) {
		return strRedisTemplate.opsForHash().multiGet(key, fields);
	}

	public void hPut(String key, String hashKey, String value) {
		strRedisTemplate.opsForHash().put(key, hashKey, value);
	}

	public void hPutAll(String key, Map<String, String> maps) {
		strRedisTemplate.opsForHash().putAll(key, maps);
	}

	/**
	 * 仅当hashKey不存在时才设绿
	 * 
	 * @param key
	 * @param hashKey
	 * @param value
	 * @return
	 */
	public Boolean hPutIfAbsent(String key, String hashKey, String value) {
		return strRedisTemplate.opsForHash().putIfAbsent(key, hashKey, value);
	}

	/**
	 * 删除?个或多个哈希表字欿
	 * 
	 * @param key
	 * @param fields
	 * @return
	 */
	public void hDelete(String key, Object... fields) {
		strRedisTemplate.opsForHash().delete(key, fields);
	}

	/**
	 * 查看哈希表中指定的字段是否存在
	 * 
	 * @param key
	 * @param field
	 * @return
	 */
	public boolean hExists(String key, String field) {
		return strRedisTemplate.opsForHash().hasKey(key, field);
	}

	/**
	 * 为哈希表 key 中的指定字段的整数?加上增酿 increment
	 * 
	 * @param key
	 * @param field
	 * @param increment
	 * @return
	 */
	public Long hIncrement(String key, Object field, long increment) {
		return strRedisTemplate.opsForHash().increment(key, field, increment);
	}

	/**
	 * 为哈希表 key 中的指定字段的整数?加上增酿 increment
	 * 
	 * @param key
	 * @param field
	 * @param delta
	 * @return
	 */
	public Double hIncrement(String key, Object field, double delta) {
		return strRedisTemplate.opsForHash().increment(key, field, delta);
	}

	/**
	 * 获取哈希表中的所有字段
	 * 
	 * @param key
	 * @return
	 */
	public Set<Object> hKeys(String key) {
		return strRedisTemplate.opsForHash().keys(key);
	}

	/**
	 * 获取哈希表中字段的数量
	 * 
	 * @param key
	 * @return
	 */
	public Long hSize(String key) {
		return strRedisTemplate.opsForHash().size(key);
	}

	/**
	 * 获取哈希表中懿有?
	 * 
	 * @param key
	 * @return
	 */
	public List<Object> hValues(String key) {
		return strRedisTemplate.opsForHash().values(key);
	}

	/**
	 * 迭代哈希表中的键值对
	 * 
	 * @param key
	 * @param options
	 * @return
	 */
	public Cursor<Entry<Object, Object>> hScan(String key, ScanOptions options) {
		return strRedisTemplate.opsForHash().scan(key, options);
	}

	/** ------------------------list数据结构相关操作---------------------------- */

	/**
	 * 通过索引获取列表中的元素
	 * 
	 * @param key
	 * @param index
	 * @return
	 */
	public String lIndex(String key, long index) {
		return strRedisTemplate.opsForList().index(key, index);
	}

	/**
	 * 获取列表指定范围内的元素
	 * 
	 * @param key
	 * @param start开始位置
	 *            
	 * @param end结束位置,　-1返回所有
	 * @return
	 */
	public List<String> lRange(String key, long start, long end) {
		return strRedisTemplate.opsForList().range(key, start, end);
	}

	/**
	 * 存入List数据 做左边推入一? 如果键不存在 则创建一个空的并左推僿
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lLeftPush(String key, String value) {
		return strRedisTemplate.opsForList().leftPush(key, value);
	}

	/**
	 * 存入List数据 做左边推入多? 如果键不存在 则创建一个空的并左推僿
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lLeftPushAll(String key, String... value) {
		return strRedisTemplate.opsForList().leftPushAll(key, value);
	}

	/**
	 * 存入List数据 做左边推入集合， 如果键不存在则创建一个空的并左推僿
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lLeftPushAll(String key, Collection<String> value) {
		return strRedisTemplate.opsForList().leftPushAll(key, value);
	}

	/**
	 * 如果存在该键的List数据 则向左推入一个元粿 不存在的话不操作
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lLeftPushIfPresent(String key, String value) {
		return strRedisTemplate.opsForList().leftPushIfPresent(key, value);
	}

	/**
	 * 如果pivot存在,再pivot前面添加
	 * 
	 * @param key
	 * @param pivot
	 * @param value
	 * @return
	 */
	public Long lLeftPush(String key, String pivot, String value) {
		return strRedisTemplate.opsForList().leftPush(key, pivot, value);
	}

	/**
	 * 存入List数据 做右边推入一? 如果键不存在 则创建一个空的并左推僿
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lRightPush(String key, String value) {
		return strRedisTemplate.opsForList().rightPush(key, value);
	}

	/**
	 * 存入List数据 做右边推入多? 如果键不存在 则创建一个空的并左推僿
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lRightPushAll(String key, String... value) {
		return strRedisTemplate.opsForList().rightPushAll(key, value);
	}

	/**
	 * 存入List数据 做右边推入集合， 如果键不存在则创建一个空的并左推僿
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lRightPushAll(String key, Collection<String> value) {
		return strRedisTemplate.opsForList().rightPushAll(key, value);
	}

	/**
	 * 为已存在的列表添加?
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lRightPushIfPresent(String key, String value) {
		return strRedisTemplate.opsForList().rightPushIfPresent(key, value);
	}

	/**
	 * 在pivot元素的右边添加?
	 * 
	 * @param key
	 * @param pivot
	 * @param value
	 * @return
	 */
	public Long lRightPush(String key, String pivot, String value) {
		return strRedisTemplate.opsForList().rightPush(key, pivot, value);
	}

	/**
	 * 通过索引设置列表元素的?
	 * 
	 * @param key
	 * @param index 索引位置
	 * @param value
	 */
	public void lSet(String key, long index, String value) {
		strRedisTemplate.opsForList().set(key, index, value);
	}

	/**
	 * 移出并获取列表的第一个元粿
	 * 
	 * @param key
	 * @return 删除的元粿
	 */
	public String lLeftPop(String key) {
		return strRedisTemplate.opsForList().leftPop(key);
	}

	/**
	 * 移出并获取列表的第一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为櫿
	 * 
	 * @param key
	 * @param timeout
	 *            等待时间
	 * @param unit
	 *            时间单位
	 * @return
	 */
	public String lBLeftPop(String key, long timeout, TimeUnit unit) {
		return strRedisTemplate.opsForList().leftPop(key, timeout, unit);
	}

	/**
	 * 移除并获取列表最后一个元粿
	 * 
	 * @param key
	 * @return 删除的元粿
	 */
	public String lRightPop(String key) {
		return strRedisTemplate.opsForList().rightPop(key);
	}

	/**
	 * 移出并获取列表的暿后一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为櫿
	 * 
	 * @param key
	 * @param timeout
	 *            等待时间
	 * @param unit
	 *            时间单位
	 * @return
	 */
	public String lBRightPop(String key, long timeout, TimeUnit unit) {
		return strRedisTemplate.opsForList().rightPop(key, timeout, unit);
	}

	/**
	 * 移除列表的最后一个元素，并将该元素添加到另一个列表并返回
	 * 
	 * @param sourceKey
	 * @param destinationKey
	 * @return
	 */
	public String lRightPopAndLeftPush(String sourceKey, String destinationKey) {
		return strRedisTemplate.opsForList().rightPopAndLeftPush(sourceKey,
				destinationKey);
	}

	/**
	 * 从列表中弹出?个?，将弹出的元素插入到另外一个列表中并返回它? 
	 * 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为櫿
	 * 
	 * @param sourceKey
	 * @param destinationKey
	 * @param timeout
	 * @param unit
	 * @return
	 */
	public String lBRightPopAndLeftPush(String sourceKey, String destinationKey,
			long timeout, TimeUnit unit) {
		return strRedisTemplate.opsForList().rightPopAndLeftPush(sourceKey,
				destinationKey, timeout, unit);
	}

	/**
	 * 删除集合中?等于value得元粿
	 * 
	 * @param key
	 * @param index
	 *            index=0, 删除懿有?等于value的元粿; index>0, 从头部开始删除第?个?等于value的元粿;
	 *            index<0, 从尾部开始删除第?个?等于value的元粿;
	 * @param value
	 * @return
	 */
	public Long lRemove(String key, long index, String value) {
		return strRedisTemplate.opsForList().remove(key, index, value);
	}

	/**
	 * 裁剪list
	 * 
	 * @param key
	 * @param start
	 * @param end
	 */
	public void lTrim(String key, long start, long end) {
		strRedisTemplate.opsForList().trim(key, start, end);
	}

	/**
	 * 获取列表长度
	 * 
	 * @param key
	 * @return
	 */
	public Long lSize(String key) {
		return strRedisTemplate.opsForList().size(key);
	}

	/** --------------------set数据结构相关操作-------------------------- */

	/**
	 * 向键的添加元素（若没有该键，创建?个新的，并加入元素）
	 * 
	 * @param key
	 * @param values
	 * @return
	 */
	public Long sAdd(String key, String... values) {
		return strRedisTemplate.opsForSet().add(key, values);
	}

	/**
	 * set移除元素
	 * 
	 * @param key
	 * @param values
	 * @return
	 */
	public Long sRemove(String key, Object... values) {
		return strRedisTemplate.opsForSet().remove(key, values);
	}

	/**
	 * 移除并返回集合的?个随机元粿
	 * 
	 * @param key
	 * @return
	 */
	public String sPop(String key) {
		return strRedisTemplate.opsForSet().pop(key);
	}

	/**
	 * 将指定键的指定元素移动到指定键中
	 * 
	 * @param key
	 * @param value
	 * @param destKey
	 * @return
	 */
	public Boolean sMove(String key, String value, String destKey) {
		return strRedisTemplate.opsForSet().move(key, value, destKey);
	}

	/**
	 * 查询指定键的包含元素个数
	 * 
	 * @param key
	 * @return
	 */
	public Long sSize(String key) {
		return strRedisTemplate.opsForSet().size(key);
	}

	/**
	 * 查询指定键是否有该元素
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Boolean sIsMember(String key, Object value) {
		return strRedisTemplate.opsForSet().isMember(key, value);
	}

	/**
	 * 获取两个集合的交陿
	 * 
	 * @param key
	 * @param otherKey
	 * @return
	 */
	public Set<String> sIntersect(String key, String otherKey) {
		return strRedisTemplate.opsForSet().intersect(key, otherKey);
	}

	/**
	 * 获取key集合与多个集合的交集
	 * 
	 * @param key
	 * @param otherKeys
	 * @return
	 */
	public Set<String> sIntersect(String key, Collection<String> otherKeys) {
		return strRedisTemplate.opsForSet().intersect(key, otherKeys);
	}

	/**
	 * key集合与otherKey集合的交集存储到destKey集合
	 * 
	 * @param key
	 * @param otherKey
	 * @param destKey
	 * @return 返回新的集合元素个数
	 */
	public Long sIntersectAndStore(String key, String otherKey, String destKey) {
		return strRedisTemplate.opsForSet().intersectAndStore(key, otherKey,
				destKey);
	}

	/**
	 * key集合与多个集合的交集存储到destKey集合?
	 * 
	 * @param key
	 * @param otherKeys
	 * @param destKey
	 * @return
	 */
	public Long sIntersectAndStore(String key, Collection<String> otherKeys,
			String destKey) {
		return strRedisTemplate.opsForSet().intersectAndStore(key, otherKeys,
				destKey);
	}

	/**
	 * 获取两个集合的并陿
	 * 
	 * @param key
	 * @param otherKey
	 * @return
	 */
	public Set<String> sUnion(String key, String otherKey) {
		return strRedisTemplate.opsForSet().union(key, otherKey);
	}

	/**
	 * 获取key集合与多个集合的并集
	 * 
	 * @param key
	 * @param otherKeys
	 * @return
	 */
	public Set<String> sUnion(String key, Collection<String> otherKeys) {
		return strRedisTemplate.opsForSet().union(key, otherKeys);
	}

	/**
	 * key集合与otherKey集合的并集存储到destKey?
	 * 
	 * @param key
	 * @param otherKey
	 * @param destKey
	 * @return
	 */
	public Long sUnionAndStore(String key, String otherKey, String destKey) {
		return strRedisTemplate.opsForSet().unionAndStore(key, otherKey, destKey);
	}

	/**
	 * key集合与多个集合的并集存储到destKey?
	 * 
	 * @param key
	 * @param otherKeys
	 * @param destKey
	 * @return
	 */
	public Long sUnionAndStore(String key, Collection<String> otherKeys,
			String destKey) {
		return strRedisTemplate.opsForSet().unionAndStore(key, otherKeys, destKey);
	}

	/**
	 * 获取两个集合的差陿
	 * 
	 * @param key
	 * @param otherKey
	 * @return
	 */
	public Set<String> sDifference(String key, String otherKey) {
		return strRedisTemplate.opsForSet().difference(key, otherKey);
	}

	/**
	 * 获取key集合与多个集合的差集
	 * 
	 * @param key
	 * @param otherKeys
	 * @return
	 */
	public Set<String> sDifference(String key, Collection<String> otherKeys) {
		return strRedisTemplate.opsForSet().difference(key, otherKeys);
	}

	/**
	 * key集合与otherKey集合的差集存储到destKey?
	 * 
	 * @param key
	 * @param otherKey
	 * @param destKey
	 * @return
	 */
	public Long sDifference(String key, String otherKey, String destKey) {
		return strRedisTemplate.opsForSet().differenceAndStore(key, otherKey,
				destKey);
	}

	/**
	 * key集合与多个集合的差集存储到destKey?
	 * 
	 * @param key
	 * @param otherKeys
	 * @param destKey
	 * @return
	 */
	public Long sDifference(String key, Collection<String> otherKeys,
			String destKey) {
		return strRedisTemplate.opsForSet().differenceAndStore(key, otherKeys,
				destKey);
	}

	/**
	 * 获取集合所有元素
	 * 
	 * @param key
	 * @param otherKeys
	 * @param destKey
	 * @return
	 */
	public Set<String> sMembers(String key) {
		return strRedisTemplate.opsForSet().members(key);
	}

	/**
	 * 随机获取集合中的一个元素
	 * 
	 * @param key
	 * @return
	 */
	public String sRandomMember(String key) {
		return strRedisTemplate.opsForSet().randomMember(key);
	}

	/**
	 * 随机获取集合中count个元素，随机的元素可能重复
	 * 
	 * @param key
	 * @param count
	 * @return
	 */
	public List<String> sRandomMembers(String key, long count) {
		return strRedisTemplate.opsForSet().randomMembers(key, count);
	}

	/**
	 * 随机获取集合中count个元素并且去除重复的。随机的元素不会重复
	 * 
	 * @param key
	 * @param count
	 * @return
	 */
	public Set<String> sDistinctRandomMembers(String key, long count) {
		return strRedisTemplate.opsForSet().distinctRandomMembers(key, count);
	}

	/**
	 * 获取集合的游标
	 * @param key
	 * @param options
	 * @return
	 */
	public Cursor<String> sScan(String key, ScanOptions options) {
		return strRedisTemplate.opsForSet().scan(key, options);
	}

	/**------------------ZSet数据结构相关操作--------------------------------*/
	
	/**
	 * 增加元素,有序集合是按照元素的score值由小到大排冿
	 * 
	 * @param key
	 * @param value
	 * @param score
	 * @return
	 */
	public Boolean zAdd(String key, String value, double score) {
		return strRedisTemplate.opsForZSet().add(key, value, score);
	}

	/**
	 * 增加元素
	 * @param key
	 * @param values
	 * @return
	 */
	public Long zAdd(String key, Set<TypedTuple<String>> values) {
		return strRedisTemplate.opsForZSet().add(key, values);
	}

	/**
	 * 指定键的移除指定元素
	 * @param key
	 * @param values
	 * @return
	 */
	public Long zRemove(String key, Object... values) {
		return strRedisTemplate.opsForZSet().remove(key, values);
	}

	/**
	 * 增加元素的score值，并返回增加后的?
	 * 
	 * @param key
	 * @param value
	 * @param delta
	 * @return
	 */
	public Double zIncrementScore(String key, String value, double delta) {
		return strRedisTemplate.opsForZSet().incrementScore(key, value, delta);
	}

	/**
	 * 返回有序集中指定成员的排名，其中有序集成员按分数值習增(从小到大)顺序排列
	 * 
	 * @param key
	 * @param value
	 * @return 0表示第一仿
	 */
	public Long zRank(String key, Object value) {
		return strRedisTemplate.opsForZSet().rank(key, value);
	}

	/**
	 * 返回有序集中指定成员的排名，其中有序集成员按分数值習增(从大到小)顺序排列
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Long zReverseRank(String key, Object value) {
		return strRedisTemplate.opsForZSet().reverseRank(key, value);
	}

	/**
	 * 获取集合的元粿, 从小到大排序
	 * 
	 * @param key
	 * @param start
	 *            庿始位绿
	 * @param end
	 *            结束位置, -1查询懿暿
	 * @return
	 */
	public Set<String> zRange(String key, long start, long end) {
		return strRedisTemplate.opsForZSet().range(key, start, end);
	}

	/**
	 * 获取集合元素, 并且把score值也获取
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public Set<TypedTuple<String>> zRangeWithScores(String key, long start,
			long end) {
		return strRedisTemplate.opsForZSet().rangeWithScores(key, start, end);
	}

	/**
	 * 根据Score值查询集合元粿
	 * 
	 * @param key
	 * @param min
	 *            暿小?
	 * @param max
	 *            暿大?
	 * @return
	 */
	public Set<String> zRangeByScore(String key, double min, double max) {
		return strRedisTemplate.opsForZSet().rangeByScore(key, min, max);
	}

	/**
	 * 根据Score值查询集合元粿, 从小到大排序
	 * 
	 * @param key
	 * @param min
	 *            暿小?
	 * @param max
	 *            暿大?
	 * @return
	 */
	public Set<TypedTuple<String>> zRangeByScoreWithScores(String key,
			double min, double max) {
		return strRedisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max);
	}

	/**
	 * 指定键的分数在min到max之间的元粿(从小到大排序)并且带有分数
	 * @param key
	 * @param min
	 * @param max
	 * @param start
	 * @param end
	 * @return
	 */
	public Set<TypedTuple<String>> zRangeByScoreWithScores(String key,
			double min, double max, long start, long end) {
		return strRedisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max,
				start, end);
	}

	/**
	 * 获取集合的元粿, 从大到小排序
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public Set<String> zReverseRange(String key, long start, long end) {
		return strRedisTemplate.opsForZSet().reverseRange(key, start, end);
	}

	/**
	 * 获取集合的元粿, 从大到小排序, 并返回score值
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public Set<TypedTuple<String>> zReverseRangeWithScores(String key,
			long start, long end) {
		return strRedisTemplate.opsForZSet().reverseRangeWithScores(key, start,
				end);
	}

	/**
	 * 根据Score值查询集合元粿, 从大到小排序
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public Set<String> zReverseRangeByScore(String key, double min,
			double max) {
		return strRedisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
	}

	/**
	 * 指定键的分数在min到max之间的元粿(从大到小排序)
	 * @param key
	 * @param min
	 * @param max
	 * @param start
	 * @param end
	 * @return
	 */
	public Set<String> zReverseRangeByScore(String key, double min,
			double max, long start, long end) {
		return strRedisTemplate.opsForZSet().reverseRangeByScore(key, min, max,
				start, end);
	}
	/**
	 * 根据Score值查询集合元粿, 从大到小排序
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public Set<TypedTuple<String>> zReverseRangeByScoreWithScores(
			String key, double min, double max) {
		return strRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key,
				min, max);
	}

	public Set<TypedTuple<String>> zReverseRangeByScoreWithScores(
			String key, double min, double max, long start, long end) {
		return strRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key,
				min, max, start, end);
	}
	

	/**
	 * 根据score值获取集合元素数酿
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public Long zCount(String key, double min, double max) {
		return strRedisTemplate.opsForZSet().count(key, min, max);
	}

	/**
	 * 获取集合大小
	 * 
	 * @param key
	 * @return
	 */
	public Long zSize(String key) {
		return strRedisTemplate.opsForZSet().size(key);
	}

	/**
	 * 获取集合大小
	 * 
	 * @param key
	 * @return
	 */
	public Long zZCard(String key) {
		return strRedisTemplate.opsForZSet().zCard(key);
	}

	/**
	 * 获取集合中value元素的score值
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Double zScore(String key, Object value) {
		return strRedisTemplate.opsForZSet().score(key, value);
	}

	/**
	 * 移除指定索引位置的成叿
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public Long zRemoveRange(String key, long start, long end) {
		return strRedisTemplate.opsForZSet().removeRange(key, start, end);
	}

	/**
	 * 根据指定的score值的范围来移除成叿
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public Long zRemoveRangeByScore(String key, double min, double max) {
		return strRedisTemplate.opsForZSet().removeRangeByScore(key, min, max);
	}

	/**
	 * 获取key和otherKey的并集并存储在destKey?
	 * 
	 * @param key
	 * @param otherKey
	 * @param destKey
	 * @return
	 */
	public Long zUnionAndStore(String key, String otherKey, String destKey) {
		return strRedisTemplate.opsForZSet().unionAndStore(key, otherKey, destKey);
	}

	/**
	 * 求指定键与另外一个集合的并集，并存入?个集合中
	 * @param key
	 * @param otherKeys
	 * @param destKey
	 * @return
	 */
	public Long zUnionAndStore(String key, Collection<String> otherKeys,
			String destKey) {
		return strRedisTemplate.opsForZSet()
				.unionAndStore(key, otherKeys, destKey);
	}

	/**
	 * 交集
	 * 
	 * @param key
	 * @param otherKey
	 * @param destKey
	 * @return
	 */
	public Long zIntersectAndStore(String key, String otherKey,
			String destKey) {
		return strRedisTemplate.opsForZSet().intersectAndStore(key, otherKey,
				destKey);
	}

	/**
	 * 获取两个集合的交集，并存入一个集合中
	 * 
	 * @param key
	 * @param otherKeys
	 * @param destKey
	 * @return
	 */
	public Long zIntersectAndStore(String key, Collection<String> otherKeys,
			String destKey) {
		return strRedisTemplate.opsForZSet().intersectAndStore(key, otherKeys,
				destKey);
	}

	/**
	 * 获取集合的游枿
	 * @param key
	 * @param options
	 * @return
	 */
	public Cursor<TypedTuple<String>> zScan(String key, ScanOptions options) {
		return strRedisTemplate.opsForZSet().scan(key, options);
	}
	
	 /**
     * 获取Redis List 序列势
     * @param key
     * @param targetClass
     * @param <T>
     * @return
     */
    public <T> List<T> getListCache(final String key, Class<T> targetClass) {
        byte[] result = strRedisTemplate.execute(new RedisCallback<byte[]>() {
            public byte[] doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.get(key.getBytes());
            }
        });
        if (result == null) {
            return null;
        }
        return null; //ProtoStuffSerializerUtil.deserializeList(result, targetClass);
    }

    /***
     * 将List 放进缓存里面
     * @param key
     * @param objList
     * @param expireTime
     * @param <T>
     * @return
     */
//    public <T> boolean putListCacheWithExpireTime(String key, List<T> objList, final long expireTime) {
//        final byte[] bkey = key.getBytes();
//        final byte[] bvalue = ProtoStuffSerializerUtil.serializeList(objList);
//        boolean result = strRedisTemplate.execute(new RedisCallback<Boolean>() {
//            @Override
//            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
//                connection.setEx(bkey, expireTime, bvalue);
//                return true;
//            }
//        });
//        return result;
//    }
}