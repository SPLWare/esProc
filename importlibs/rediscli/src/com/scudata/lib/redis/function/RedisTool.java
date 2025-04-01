package com.scudata.lib.redis.function;

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

	/** -------------------key鐩稿叧鎿嶄綔--------------------- */

	/**
	 * 鍒犻櫎鍗曚釜key
	 * 
	 * @param key
	 */
	public void delete(String key) {
		strRedisTemplate.delete(key);
	}

	/**
	 * 鍒犻櫎澶氫釜key
	 * 
	 * @param keys 閿泦鍘?
	 */
	public void delete(Collection<String> keys) {
		strRedisTemplate.delete(keys);
	}

	/**
	 * 搴忓垪鍖杒ey
	 * 
	 * @param key
	 * @return
	 */
	public byte[] dump(String key) {
		return strRedisTemplate.dump(key);
	}

	/**
	 * 鏄惁瀛樺湪key
	 * 
	 * @param key
	 * @return
	 */
	public Boolean hasKey(String key) {
		return strRedisTemplate.hasKey(key);
	}

	/**
	 * 缁欐寚瀹氶敭璁剧疆杩囨湡鏃堕棿
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
	 * 缁欐寚瀹氶敭璁剧疆杩囨湡鏃堕棿
	 * 
	 * @param key
	 * @param date
	 * @return
	 */
	public Boolean expireAt(String key, Date date) {
		return strRedisTemplate.expireAt(key, date);
	}

	/**
	 * 鏌ユ壘鍖归厤鐨刱ey
	 * 
	 * @param pattern
	 * @return 
	 */
	public Set<String> keys(String pattern) {
		return strRedisTemplate.keys(pattern);
	}

	/**
	 * 灏嗘寚瀹氱殑閿Щ鍔ㄥ埌鎸囧畾鐨勫簱
	 * 
	 * @param key
	 * @param dbIndex
	 * @return
	 */
	public Boolean move(String key, int dbIndex) {
		return strRedisTemplate.move(key, dbIndex);
	}

	/**
	 * 灏唊ey鎸佷箙鍖栦繚瀛橈紝 灏辨槸鎶婅繃鏈熸垨鑰呰缃簡杩囨湡鏃堕棿鐨刱ey鍙樹负姘镐笉杩囨湡
	 * 
	 * @param key
	 * @return
	 */
	public Boolean persist(String key) {
		return strRedisTemplate.persist(key);
	}

	/**
	 * 鑾峰彇杩囨湡鏃堕棿
	 * 
	 * @param key
	 * @param unit
	 * @return
	 */
	public Long getExpire(String key, TimeUnit unit) {
		return strRedisTemplate.getExpire(key, unit);
	}

	/**
	 * 鑾峰彇杩囨湡鏃堕棿
	 * 
	 * @param key
	 * @return
	 */
	public Long getExpire(String key) {
		return strRedisTemplate.getExpire(key);
	}

	/**
	 * 闅忔満鍙栦竴涓猭ey
	 * 
	 * @return
	 */
	public String randomKey() {
		return strRedisTemplate.randomKey();
	}

	/**
	 * 淇敼 key 鐨勫悕绁?
	 * 
	 * @param oldKey
	 * @param newKey
	 */
	public void rename(String oldKey, String newKey) {
		strRedisTemplate.rename(oldKey, newKey);
	}

	/**
	 * 鏃??瀛樺湪鏃讹紝灏嗘棫?鏀逛负鏂板??
	 * 
	 * @param oldKey
	 * @param newKey
	 * @return
	 */
	public Boolean renameIfAbsent(String oldKey, String newKey) {
		return strRedisTemplate.renameIfAbsent(oldKey, newKey);
	}

	/**
	 * 杩斿洖 key鍊肩殑绫诲瀷
	 * 
	 * @param key
	 * @return
	 */
	public DataType type(String key) {
		return strRedisTemplate.type(key);
	}

	/** -------------------String鏁版嵁缁撴瀯鐩稿叧鎿嶄綔--------------------- */

	/**
	 * 瀛樺叆鏁版嵁
	 * @param key
	 * @param value
	 */
	public void set(String key, String value) {
		strRedisTemplate.opsForValue().set(key, value);
	}

	/**
	 * 鑾峰彇鏁版嵁
	 * @param key
	 * @return
	 */
	public String get(String key) {
		return strRedisTemplate.opsForValue().get(key);
	}

	/**
	 * 杩斿洖 key鍊肩殑瀛愬瓧涓?
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public String getRange(String key, long start, long end) {
		return strRedisTemplate.opsForValue().get(key, start, end);
	}

	/**
	 * 灏嗙粰 key鐨勮缃畍alue锛屽苟杩斿洖 key鏃у??(old value)
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public String getAndSet(String key, String value) {
		return strRedisTemplate.opsForValue().getAndSet(key, value);
	}

	/**
	 * 鍒ゆ柇鎸囧畾鐨刼ffset浣嶇疆ASCII鐮佺殑bit浣嶆槸鍚︿负1
	 * 
	 * @param key
	 * @param offset
	 * @return
	 */
	public Boolean getBit(String key, long offset) {
		return strRedisTemplate.opsForValue().getBit(key, offset);
	}

	/**
	 * 鑾峰彇澶氫釜鏁版嵁
	 * 
	 * @param keys
	 * @return
	 */
	public List<String> multiGet(Collection<String> keys) {
		return strRedisTemplate.opsForValue().multiGet(keys);
	}

	/**
	 * 璁剧疆ASCII鐬?, 瀛楃?'a'鐨凙SCII鐮佹槸97, 杞负浜岃繘鍒舵槸'01100001', 姝ゆ柟娉曟槸灏嗕簩杩涘埗绗琽ffset浣??鍙樹负value
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
	 * 瀛樺叆鏁版嵁骞朵笖璁剧疆杩囨湡鏃堕棿锛屽苟瀹? key 鐨勮繃鏈熸椂闂磋? timeout
	 * 
	 * @param key
	 * @param value
	 * @param timeout 杩囨湡鏃堕棿
	 * @param unit
	 *            鏃堕棿鍗曚綅, 澧?:TimeUnit.DAYS 灏忔椂:TimeUnit.HOURS 鍒嗛挓:TimeUnit.MINUTES
	 *            绁?:TimeUnit.SECONDS 姣:TimeUnit.MILLISECONDS
	 */
	public void setEx(String key, String value, long timeout, TimeUnit unit) {
		strRedisTemplate.opsForValue().set(key, value, timeout, unit);
	}

	/**
	 * 鍙湁鍤? key涓嶅瓨鍦ㄦ椂璁剧疆 key 鐨??
	 * 
	 * @param key
	 * @param value
	 * @return 涔嬪墠宸茬粡瀛樺湪杩斿洖false,涓嶅瓨鍦ㄨ繑鍥瀟rue
	 */
	public boolean setIfAbsent(String key, String value) {
		return strRedisTemplate.opsForValue().setIfAbsent(key, value);
	}

	/**
	 * 缁欏畾 key鍌ㄥ瓨value鐨勫瓧绗︿覆鍊硷紝浠庡亸绉婚噺 offset搴垮タ
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
	 * 鑾峰彇瀛楃涓茬殑闀垮害
	 * 
	 * @param key
	 * @return
	 */
	public Long size(String key) {
		return strRedisTemplate.opsForValue().size(key);
	}

	/**
	 * 澶氫釜閿??鐨勬彃鍏?
	 * 
	 * @param maps
	 */
	public void multiSet(Map<String, String> maps) {
		strRedisTemplate.opsForValue().multiSet(maps);
	}

	/**
	 * 澶氫釜閿??鐨勬彃鍏ワ紝褰撲笖浠呭綋鎵?鏈夌粰瀣? key 閮戒笉瀛樺湪
	 * 
	 * @param maps
	 * @return 涔嬪墠宸茬粡瀛樺湪杩斿洖false,涓嶅瓨鍦ㄨ繑鍥瀟rue
	 */
	public boolean multiSetIfAbsent(Map<String, String> maps) {
		return strRedisTemplate.opsForValue().multiSetIfAbsent(maps);
	}

	/**
	 * 缁欐寚瀹氶敭 鍔犳寚瀹氭暣鏁帮紝濡傛灉鍊间笉鏄暟瀛楀垯鎶涘嚭寮傚父锛? 涓嶅瓨鍦ㄦ寚瀹氶敭鍒涘缓涓?涓垵濮嬩负0鐨勫姞鎸囧畾鏁存暟 澧炲姞鎴愬姛鍒欒繑鍥炲鍔犲悗鐨勫??
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
	 * 缁欐寚瀹氶敭 鍔犳寚瀹氬弻绮剧‘搴︽暟锛屽鏋??涓嶆槸鏁板瓧鍒欐姏鍑哄紓甯? 涓嶅瓨鍦ㄦ寚瀹氶敭鍒涘缓?涓垵濮嬩负0鐨勫姞鎸囧畾鏁存暟 澧炲姞鎴愬姛鍒欒繑鍥炲鍔犲悗鐨??
	 * @param key
	 * @param value
	 * @return
	 */
	public Double increment(String key, double increment) {
		return strRedisTemplate.opsForValue().increment(key, increment);
	}

	/**
	 * 缁欐寚瀹氶敭 鐨??杩藉姞瀛楃涓?
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Integer append(String key, String value) {
		return strRedisTemplate.opsForValue().append(key, value);
	}

	/** -------------------hash鏁版嵁缁撴瀯鐩稿叧鎿嶄綔------------------------- */

	/**
	 * 鑾峰彇瀛樺偍鍦℉ash涓寚瀹氬瓧娈电殑鍊?
	 * 
	 * @param key
	 * @param field
	 * @return
	 */
	public Object hGet(String key, String field) {
		return strRedisTemplate.opsForHash().get(key, field);
	}

	/**
	 * 鑾峰彇鎵?鏈夊瓧娈电殑鍊?
	 * 
	 * @param key
	 * @return
	 */
	public Map<Object, Object> hGetAll(String key) {
		return strRedisTemplate.opsForHash().entries(key);
	}

	/**
	 * 鑾峰彇鎳挎湁缁欏畾瀛楁鐨勫??
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
	 * 浠呭綋hashKey涓嶅瓨鍦ㄦ椂鎵嶈缁?
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
	 * 鍒犻櫎?涓垨澶氫釜鍝堝笇琛ㄥ瓧娆?
	 * 
	 * @param key
	 * @param fields
	 * @return
	 */
	public void hDelete(String key, Object... fields) {
		strRedisTemplate.opsForHash().delete(key, fields);
	}

	/**
	 * 鏌ョ湅鍝堝笇琛ㄤ腑鎸囧畾鐨勫瓧娈垫槸鍚﹀瓨鍦?
	 * 
	 * @param key
	 * @param field
	 * @return
	 */
	public boolean hExists(String key, String field) {
		return strRedisTemplate.opsForHash().hasKey(key, field);
	}

	/**
	 * 涓哄搱甯岃〃 key 涓殑鎸囧畾瀛楁鐨勬暣鏁??鍔犱笂澧為吙 increment
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
	 * 涓哄搱甯岃〃 key 涓殑鎸囧畾瀛楁鐨勬暣鏁??鍔犱笂澧為吙 increment
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
	 * 鑾峰彇鍝堝笇琛ㄤ腑鐨勬墍鏈夊瓧娈?
	 * 
	 * @param key
	 * @return
	 */
	public Set<Object> hKeys(String key) {
		return strRedisTemplate.opsForHash().keys(key);
	}

	/**
	 * 鑾峰彇鍝堝笇琛ㄤ腑瀛楁鐨勬暟閲?
	 * 
	 * @param key
	 * @return
	 */
	public Long hSize(String key) {
		return strRedisTemplate.opsForHash().size(key);
	}

	/**
	 * 鑾峰彇鍝堝笇琛ㄤ腑鎳挎湁?
	 * 
	 * @param key
	 * @return
	 */
	public List<Object> hValues(String key) {
		return strRedisTemplate.opsForHash().values(key);
	}

	/**
	 * 杩唬鍝堝笇琛ㄤ腑鐨勯敭鍊煎
	 * 
	 * @param key
	 * @param options
	 * @return
	 */
	public Cursor<Entry<Object, Object>> hScan(String key, ScanOptions options) {
		return strRedisTemplate.opsForHash().scan(key, options);
	}

	/** ------------------------list鏁版嵁缁撴瀯鐩稿叧鎿嶄綔---------------------------- */

	/**
	 * 閫氳繃绱㈠紩鑾峰彇鍒楄〃涓殑鍏冪礌
	 * 
	 * @param key
	 * @param index
	 * @return
	 */
	public String lIndex(String key, long index) {
		return strRedisTemplate.opsForList().index(key, index);
	}

	/**
	 * 鑾峰彇鍒楄〃鎸囧畾鑼冨洿鍐呯殑鍏冪礌
	 * 
	 * @param key
	 * @param start寮?濮嬩綅缃?
	 *            
	 * @param end缁撴潫浣嶇疆,銆?-1杩斿洖鎵?鏈?
	 * @return
	 */
	public List<String> lRange(String key, long start, long end) {
		return strRedisTemplate.opsForList().range(key, start, end);
	}

	/**
	 * 瀛樺叆List鏁版嵁 鍋氬乏杈规帹鍏ヤ竴? 濡傛灉閿笉瀛樺湪 鍒欏垱寤轰竴涓┖鐨勫苟宸︽帹鍍?
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lLeftPush(String key, String value) {
		return strRedisTemplate.opsForList().leftPush(key, value);
	}

	/**
	 * 瀛樺叆List鏁版嵁 鍋氬乏杈规帹鍏ュ? 濡傛灉閿笉瀛樺湪 鍒欏垱寤轰竴涓┖鐨勫苟宸︽帹鍍?
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lLeftPushAll(String key, String... value) {
		return strRedisTemplate.opsForList().leftPushAll(key, value);
	}

	/**
	 * 瀛樺叆List鏁版嵁 鍋氬乏杈规帹鍏ラ泦鍚堬紝 濡傛灉閿笉瀛樺湪鍒欏垱寤轰竴涓┖鐨勫苟宸︽帹鍍?
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lLeftPushAll(String key, Collection<String> value) {
		return strRedisTemplate.opsForList().leftPushAll(key, value);
	}

	/**
	 * 濡傛灉瀛樺湪璇ラ敭鐨凩ist鏁版嵁 鍒欏悜宸︽帹鍏ヤ竴涓厓绮? 涓嶅瓨鍦ㄧ殑璇濅笉鎿嶄綔
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lLeftPushIfPresent(String key, String value) {
		return strRedisTemplate.opsForList().leftPushIfPresent(key, value);
	}

	/**
	 * 濡傛灉pivot瀛樺湪,鍐峱ivot鍓嶉潰娣诲姞
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
	 * 瀛樺叆List鏁版嵁 鍋氬彸杈规帹鍏ヤ竴? 濡傛灉閿笉瀛樺湪 鍒欏垱寤轰竴涓┖鐨勫苟宸︽帹鍍?
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lRightPush(String key, String value) {
		return strRedisTemplate.opsForList().rightPush(key, value);
	}

	/**
	 * 瀛樺叆List鏁版嵁 鍋氬彸杈规帹鍏ュ? 濡傛灉閿笉瀛樺湪 鍒欏垱寤轰竴涓┖鐨勫苟宸︽帹鍍?
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lRightPushAll(String key, String... value) {
		return strRedisTemplate.opsForList().rightPushAll(key, value);
	}

	/**
	 * 瀛樺叆List鏁版嵁 鍋氬彸杈规帹鍏ラ泦鍚堬紝 濡傛灉閿笉瀛樺湪鍒欏垱寤轰竴涓┖鐨勫苟宸︽帹鍍?
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lRightPushAll(String key, Collection<String> value) {
		return strRedisTemplate.opsForList().rightPushAll(key, value);
	}

	/**
	 * 涓哄凡瀛樺湪鐨勫垪琛ㄦ坊鍔??
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Long lRightPushIfPresent(String key, String value) {
		return strRedisTemplate.opsForList().rightPushIfPresent(key, value);
	}

	/**
	 * 鍦╬ivot鍏冪礌鐨勫彸杈规坊鍔??
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
	 * 閫氳繃绱㈠紩璁剧疆鍒楄〃鍏冪礌鐨??
	 * 
	 * @param key
	 * @param index 绱㈠紩浣嶇疆
	 * @param value
	 */
	public void lSet(String key, long index, String value) {
		strRedisTemplate.opsForList().set(key, index, value);
	}

	/**
	 * 绉诲嚭骞惰幏鍙栧垪琛ㄧ殑绗竴涓厓绮?
	 * 
	 * @param key
	 * @return 鍒犻櫎鐨勫厓绮?
	 */
	public String lLeftPop(String key) {
		return strRedisTemplate.opsForList().leftPop(key);
	}

	/**
	 * 绉诲嚭骞惰幏鍙栧垪琛ㄧ殑绗竴涓厓绱狅紝 濡傛灉鍒楄〃娌℃湁鍏冪礌浼氶樆濉炲垪琛ㄧ洿鍒扮瓑寰呰秴鏃舵垨鍙戠幇鍙脊鍑哄厓绱犱负娅?
	 * 
	 * @param key
	 * @param timeout
	 *            绛夊緟鏃堕棿
	 * @param unit
	 *            鏃堕棿鍗曚綅
	 * @return
	 */
	public String lBLeftPop(String key, long timeout, TimeUnit unit) {
		return strRedisTemplate.opsForList().leftPop(key, timeout, unit);
	}

	/**
	 * 绉婚櫎骞惰幏鍙栧垪琛ㄦ渶鍚庝竴涓厓绮?
	 * 
	 * @param key
	 * @return 鍒犻櫎鐨勫厓绮?
	 */
	public String lRightPop(String key) {
		return strRedisTemplate.opsForList().rightPop(key);
	}

	/**
	 * 绉诲嚭骞惰幏鍙栧垪琛ㄧ殑鏆垮悗涓?涓厓绱狅紝 濡傛灉鍒楄〃娌℃湁鍏冪礌浼氶樆濉炲垪琛ㄧ洿鍒扮瓑寰呰秴鏃舵垨鍙戠幇鍙脊鍑哄厓绱犱负娅?
	 * 
	 * @param key
	 * @param timeout
	 *            绛夊緟鏃堕棿
	 * @param unit
	 *            鏃堕棿鍗曚綅
	 * @return
	 */
	public String lBRightPop(String key, long timeout, TimeUnit unit) {
		return strRedisTemplate.opsForList().rightPop(key, timeout, unit);
	}

	/**
	 * 绉婚櫎鍒楄〃鐨勬渶鍚庝竴涓厓绱狅紝骞跺皢璇ュ厓绱犳坊鍔犲埌鍙︿竴涓垪琛ㄥ苟杩斿洖
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
	 * 浠庡垪琛ㄤ腑寮瑰嚭?涓??锛屽皢寮瑰嚭鐨勫厓绱犳彃鍏ュ埌鍙﹀涓?涓垪琛ㄤ腑骞惰繑鍥炲畠? 
	 * 濡傛灉鍒楄〃娌℃湁鍏冪礌浼氶樆濉炲垪琛ㄧ洿鍒扮瓑寰呰秴鏃舵垨鍙戠幇鍙脊鍑哄厓绱犱负娅?
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
	 * 鍒犻櫎闆嗗悎涓??绛変簬value寰楀厓绮?
	 * 
	 * @param key
	 * @param index
	 *            index=0, 鍒犻櫎鎳挎湁?绛変簬value鐨勫厓绮?; index>0, 浠庡ご閮ㄥ紑濮嬪垹闄ょ?涓??绛変簬value鐨勫厓绮?;
	 *            index<0, 浠庡熬閮ㄥ紑濮嬪垹闄ょ?涓??绛変簬value鐨勫厓绮?;
	 * @param value
	 * @return
	 */
	public Long lRemove(String key, long index, String value) {
		return strRedisTemplate.opsForList().remove(key, index, value);
	}

	/**
	 * 瑁佸壀list
	 * 
	 * @param key
	 * @param start
	 * @param end
	 */
	public void lTrim(String key, long start, long end) {
		strRedisTemplate.opsForList().trim(key, start, end);
	}

	/**
	 * 鑾峰彇鍒楄〃闀垮害
	 * 
	 * @param key
	 * @return
	 */
	public Long lSize(String key) {
		return strRedisTemplate.opsForList().size(key);
	}

	/** --------------------set鏁版嵁缁撴瀯鐩稿叧鎿嶄綔-------------------------- */

	/**
	 * 鍚戦敭鐨勬坊鍔犲厓绱狅紙鑻ユ病鏈夎閿紝鍒涘缓?涓柊鐨勶紝骞跺姞鍏ュ厓绱狅級
	 * 
	 * @param key
	 * @param values
	 * @return
	 */
	public Long sAdd(String key, String... values) {
		return strRedisTemplate.opsForSet().add(key, values);
	}

	/**
	 * set绉婚櫎鍏冪礌
	 * 
	 * @param key
	 * @param values
	 * @return
	 */
	public Long sRemove(String key, Object... values) {
		return strRedisTemplate.opsForSet().remove(key, values);
	}

	/**
	 * 绉婚櫎骞惰繑鍥為泦鍚堢殑?涓殢鏈哄厓绮?
	 * 
	 * @param key
	 * @return
	 */
	public String sPop(String key) {
		return strRedisTemplate.opsForSet().pop(key);
	}

	/**
	 * 灏嗘寚瀹氶敭鐨勬寚瀹氬厓绱犵Щ鍔ㄥ埌鎸囧畾閿腑
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
	 * 鏌ヨ鎸囧畾閿殑鍖呭惈鍏冪礌涓暟
	 * 
	 * @param key
	 * @return
	 */
	public Long sSize(String key) {
		return strRedisTemplate.opsForSet().size(key);
	}

	/**
	 * 鏌ヨ鎸囧畾閿槸鍚︽湁璇ュ厓绱?
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Boolean sIsMember(String key, Object value) {
		return strRedisTemplate.opsForSet().isMember(key, value);
	}

	/**
	 * 鑾峰彇涓や釜闆嗗悎鐨勪氦闄?
	 * 
	 * @param key
	 * @param otherKey
	 * @return
	 */
	public Set<String> sIntersect(String key, String otherKey) {
		return strRedisTemplate.opsForSet().intersect(key, otherKey);
	}

	/**
	 * 鑾峰彇key闆嗗悎涓庡涓泦鍚堢殑浜ら泦
	 * 
	 * @param key
	 * @param otherKeys
	 * @return
	 */
	public Set<String> sIntersect(String key, Collection<String> otherKeys) {
		return strRedisTemplate.opsForSet().intersect(key, otherKeys);
	}

	/**
	 * key闆嗗悎涓巓therKey闆嗗悎鐨勪氦闆嗗瓨鍌ㄥ埌destKey闆嗗悎
	 * 
	 * @param key
	 * @param otherKey
	 * @param destKey
	 * @return 杩斿洖鏂扮殑闆嗗悎鍏冪礌涓暟
	 */
	public Long sIntersectAndStore(String key, String otherKey, String destKey) {
		return strRedisTemplate.opsForSet().intersectAndStore(key, otherKey,
				destKey);
	}

	/**
	 * key闆嗗悎涓庡涓泦鍚堢殑浜ら泦瀛樺偍鍒癲estKey闆嗗悎?
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
	 * 鑾峰彇涓や釜闆嗗悎鐨勫苟闄?
	 * 
	 * @param key
	 * @param otherKey
	 * @return
	 */
	public Set<String> sUnion(String key, String otherKey) {
		return strRedisTemplate.opsForSet().union(key, otherKey);
	}

	/**
	 * 鑾峰彇key闆嗗悎涓庡涓泦鍚堢殑骞堕泦
	 * 
	 * @param key
	 * @param otherKeys
	 * @return
	 */
	public Set<String> sUnion(String key, Collection<String> otherKeys) {
		return strRedisTemplate.opsForSet().union(key, otherKeys);
	}

	/**
	 * key闆嗗悎涓巓therKey闆嗗悎鐨勫苟闆嗗瓨鍌ㄥ埌destKey?
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
	 * key闆嗗悎涓庡涓泦鍚堢殑骞堕泦瀛樺偍鍒癲estKey?
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
	 * 鑾峰彇涓や釜闆嗗悎鐨勫樊闄?
	 * 
	 * @param key
	 * @param otherKey
	 * @return
	 */
	public Set<String> sDifference(String key, String otherKey) {
		return strRedisTemplate.opsForSet().difference(key, otherKey);
	}

	/**
	 * 鑾峰彇key闆嗗悎涓庡涓泦鍚堢殑宸泦
	 * 
	 * @param key
	 * @param otherKeys
	 * @return
	 */
	public Set<String> sDifference(String key, Collection<String> otherKeys) {
		return strRedisTemplate.opsForSet().difference(key, otherKeys);
	}

	/**
	 * key闆嗗悎涓巓therKey闆嗗悎鐨勫樊闆嗗瓨鍌ㄥ埌destKey?
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
	 * key闆嗗悎涓庡涓泦鍚堢殑宸泦瀛樺偍鍒癲estKey?
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
	 * 鑾峰彇闆嗗悎鎵?鏈夊厓绱?
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
	 * 闅忔満鑾峰彇闆嗗悎涓殑涓?涓厓绱?
	 * 
	 * @param key
	 * @return
	 */
	public String sRandomMember(String key) {
		return strRedisTemplate.opsForSet().randomMember(key);
	}

	/**
	 * 闅忔満鑾峰彇闆嗗悎涓璫ount涓厓绱狅紝闅忔満鐨勫厓绱犲彲鑳介噸澶?
	 * 
	 * @param key
	 * @param count
	 * @return
	 */
	public List<String> sRandomMembers(String key, long count) {
		return strRedisTemplate.opsForSet().randomMembers(key, count);
	}

	/**
	 * 闅忔満鑾峰彇闆嗗悎涓璫ount涓厓绱犲苟涓斿幓闄ら噸澶嶇殑銆傞殢鏈虹殑鍏冪礌涓嶄細閲嶅
	 * 
	 * @param key
	 * @param count
	 * @return
	 */
	public Set<String> sDistinctRandomMembers(String key, long count) {
		return strRedisTemplate.opsForSet().distinctRandomMembers(key, count);
	}

	/**
	 * 鑾峰彇闆嗗悎鐨勬父鏍?
	 * @param key
	 * @param options
	 * @return
	 */
	public Cursor<String> sScan(String key, ScanOptions options) {
		return strRedisTemplate.opsForSet().scan(key, options);
	}

	/**------------------ZSet鏁版嵁缁撴瀯鐩稿叧鎿嶄綔--------------------------------*/
	
	/**
	 * 澧炲姞鍏冪礌,鏈夊簭闆嗗悎鏄寜鐓у厓绱犵殑score鍊肩敱灏忓埌澶ф帓鍐?
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
	 * 澧炲姞鍏冪礌
	 * @param key
	 * @param values
	 * @return
	 */
	public Long zAdd(String key, Set<TypedTuple<String>> values) {
		return strRedisTemplate.opsForZSet().add(key, values);
	}

	/**
	 * 鎸囧畾閿殑绉婚櫎鎸囧畾鍏冪礌
	 * @param key
	 * @param values
	 * @return
	 */
	public Long zRemove(String key, Object... values) {
		return strRedisTemplate.opsForZSet().remove(key, values);
	}

	/**
	 * 澧炲姞鍏冪礌鐨剆core鍊硷紝骞惰繑鍥炲鍔犲悗鐨??
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
	 * 杩斿洖鏈夊簭闆嗕腑鎸囧畾鎴愬憳鐨勬帓鍚嶏紝鍏朵腑鏈夊簭闆嗘垚鍛樻寜鍒嗘暟鍊肩繏澧?(浠庡皬鍒板ぇ)椤哄簭鎺掑垪
	 * 
	 * @param key
	 * @param value
	 * @return 0琛ㄧず绗竴浠?
	 */
	public Long zRank(String key, Object value) {
		return strRedisTemplate.opsForZSet().rank(key, value);
	}

	/**
	 * 杩斿洖鏈夊簭闆嗕腑鎸囧畾鎴愬憳鐨勬帓鍚嶏紝鍏朵腑鏈夊簭闆嗘垚鍛樻寜鍒嗘暟鍊肩繏澧?(浠庡ぇ鍒板皬)椤哄簭鎺掑垪
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Long zReverseRank(String key, Object value) {
		return strRedisTemplate.opsForZSet().reverseRank(key, value);
	}

	/**
	 * 鑾峰彇闆嗗悎鐨勫厓绮?, 浠庡皬鍒板ぇ鎺掑簭
	 * 
	 * @param key
	 * @param start
	 *            搴垮浣嶇豢
	 * @param end
	 *            缁撴潫浣嶇疆, -1鏌ヨ鎳挎毧
	 * @return
	 */
	public Set<String> zRange(String key, long start, long end) {
		return strRedisTemplate.opsForZSet().range(key, start, end);
	}

	/**
	 * 鑾峰彇闆嗗悎鍏冪礌, 骞朵笖鎶妔core鍊间篃鑾峰彇
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
	 * 鏍规嵁Score鍊兼煡璇㈤泦鍚堝厓绮?
	 * 
	 * @param key
	 * @param min
	 *            鏆垮皬?
	 * @param max
	 *            鏆垮ぇ?
	 * @return
	 */
	public Set<String> zRangeByScore(String key, double min, double max) {
		return strRedisTemplate.opsForZSet().rangeByScore(key, min, max);
	}

	/**
	 * 鏍规嵁Score鍊兼煡璇㈤泦鍚堝厓绮?, 浠庡皬鍒板ぇ鎺掑簭
	 * 
	 * @param key
	 * @param min
	 *            鏆垮皬?
	 * @param max
	 *            鏆垮ぇ?
	 * @return
	 */
	public Set<TypedTuple<String>> zRangeByScoreWithScores(String key,
			double min, double max) {
		return strRedisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max);
	}

	/**
	 * 鎸囧畾閿殑鍒嗘暟鍦╩in鍒癿ax涔嬮棿鐨勫厓绮?(浠庡皬鍒板ぇ鎺掑簭)骞朵笖甯︽湁鍒嗘暟
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
	 * 鑾峰彇闆嗗悎鐨勫厓绮?, 浠庡ぇ鍒板皬鎺掑簭
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
	 * 鑾峰彇闆嗗悎鐨勫厓绮?, 浠庡ぇ鍒板皬鎺掑簭, 骞惰繑鍥瀞core鍊?
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
	 * 鏍规嵁Score鍊兼煡璇㈤泦鍚堝厓绮?, 浠庡ぇ鍒板皬鎺掑簭
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
	 * 鎸囧畾閿殑鍒嗘暟鍦╩in鍒癿ax涔嬮棿鐨勫厓绮?(浠庡ぇ鍒板皬鎺掑簭)
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
	 * 鏍规嵁Score鍊兼煡璇㈤泦鍚堝厓绮?, 浠庡ぇ鍒板皬鎺掑簭
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
	 * 鏍规嵁score鍊艰幏鍙栭泦鍚堝厓绱犳暟閰?
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
	 * 鑾峰彇闆嗗悎澶у皬
	 * 
	 * @param key
	 * @return
	 */
	public Long zSize(String key) {
		return strRedisTemplate.opsForZSet().size(key);
	}

	/**
	 * 鑾峰彇闆嗗悎澶у皬
	 * 
	 * @param key
	 * @return
	 */
	public Long zZCard(String key) {
		return strRedisTemplate.opsForZSet().zCard(key);
	}

	/**
	 * 鑾峰彇闆嗗悎涓璿alue鍏冪礌鐨剆core鍊?
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Double zScore(String key, Object value) {
		return strRedisTemplate.opsForZSet().score(key, value);
	}

	/**
	 * 绉婚櫎鎸囧畾绱㈠紩浣嶇疆鐨勬垚鍙?
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
	 * 鏍规嵁鎸囧畾鐨剆core鍊肩殑鑼冨洿鏉ョЩ闄ゆ垚鍙?
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
	 * 鑾峰彇key鍜宱therKey鐨勫苟闆嗗苟瀛樺偍鍦╠estKey?
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
	 * 姹傛寚瀹氶敭涓庡彟澶栦竴涓泦鍚堢殑骞堕泦锛屽苟瀛樺叆?涓泦鍚堜腑
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
	 * 浜ら泦
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
	 * 鑾峰彇涓や釜闆嗗悎鐨勪氦闆嗭紝骞跺瓨鍏ヤ竴涓泦鍚堜腑
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
	 * 鑾峰彇闆嗗悎鐨勬父鏋?
	 * @param key
	 * @param options
	 * @return
	 */
	public Cursor<TypedTuple<String>> zScan(String key, ScanOptions options) {
		return strRedisTemplate.opsForZSet().scan(key, options);
	}
	
	 /**
     * 鑾峰彇Redis List 搴忓垪鍔?
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
     * 灏哃ist 鏀捐繘缂撳瓨閲岄潰
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