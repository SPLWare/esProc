package com.scudata.expression.fn.string;

import javax.crypto.*;
import javax.crypto.spec.*;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Base64;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.expression.IParam;
import com.scudata.expression.fn.CharFunction;
import com.scudata.resources.EngineMessage;

/**
 * des(input, key, iv; charset) DES加密/解密
 * parament input	输入，blob或者String
 * parament key		密码，与输入统一
 * parament iv		初始化变量，与输入统一
 * parament charset	字符编码，如果input/key/iv为字符串时使用，默认为utf8
 * 选项@d	解密，默认执行加密
 * 选项@c	CBC(Cipher Block Chaining)模式，默认使用ECB(Electronic Codebook)模式	
 * 选项@n	填充模式NoPadding，缺省PKCS5Padding
 * 返回，input为字符串时返回字符串，为二进制数组时返回二进制数组
 * @author bd
 *
 */
public class DESEncrypt extends CharFunction {	
    // DES密钥算法
    private static final String KEY_ALGORITHM = "DES";
 
    // 加密/解密算法/工作模式/填充方式
    private static final String CIPHER_ALGORITHM_E = "DES/ECB/PKCS5Padding";
    private static final String CIPHER_ALGORITHM_C = "DES/CBC/PKCS5Padding";
    private static final String CIPHER_ALGORITHM_EN = "DES/ECB/NoPadding";
    private static final String CIPHER_ALGORITHM_CN = "DES/CBC/NoPadding";
    
    protected static final byte[] DEF_IV = DESEncrypt.encode("1a3b5c7d", "utf-8");
 
    // 生成密钥
    public static String generateDESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(128, new SecureRandom()); // 192 and 256 bits may not be available
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
    
	protected Object doQuery(Object[] objs) {
		try {
			if (objs==null || objs.length<2){
				MessageManager mm = EngineMessage.get();
				throw new RQException("des" + mm.getMessage("function.invalidParam"));
			}
			Object[] params = objs;
			String charset = "utf-8";
			if (param.getType() == IParam.Semicolon) {
				Object obj = objs[0];
				if (obj instanceof Object[]) {
					params = (Object[])obj;
				}
				else {
					params = new Object[1];
					params[0] = obj;
				}
				//des(...;charset)的情况
				if (objs[1] instanceof String) {
					charset = (String) objs[1];
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("des(...;charset), charset is invalid." + mm.getMessage("function.invalidParam"));
				}
			}
			if (params == null || params.length < 2){
				MessageManager mm = EngineMessage.get();
				throw new RQException("des" + mm.getMessage("function.invalidParam"));
			}
			//boolean string = params[0] instanceof String;
			byte[] inputb = DESEncrypt.encode(params[0], charset);
			byte[] keyb = DESEncrypt.encode(params[1], charset);
			byte[] ivb = null;
			if (params.length > 2) {
				ivb = DESEncrypt.encode(params[2], charset);
			}
			boolean dec = false;
			boolean modeC = false;
			boolean nopad = false;
			boolean autopad = false;
			boolean decStr = false;
			if (option != null) {
				if (option.indexOf('d')>-1) {
					dec = true;
				}
				if (option.indexOf('c')>-1) {
					modeC = true;
				}
				if (option.indexOf('n')>-1) {
					nopad = true;
				}
				if (option.indexOf('a')>-1) {
					autopad = true;
				}
				if (option.indexOf('s')>-1) {
					decStr = true;
				}
			}
			if (nopad) {
				if (inputb.length % 8 != 0 ) {
					if (autopad) {
						MessageManager mm = EngineMessage.get();
						Logger.info("des(input, key, iv; charset): " + mm.getMessage("encrypt.autopadding", "input", 8));
						inputb = AESEncrypt.autoPadding(inputb, 8);
					}
					else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("des(input, key, iv; charset): " + mm.getMessage("encrypt.nopadding", "input", 8));
					}
				}
			}
			if (keyb.length < 8) {
				if (autopad) {
					keyb = AESEncrypt.padding(keyb, 8);
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("des(input, key, iv; charset): " + mm.getMessage("encrypt.lenless", "key", 8));
				}
			}
			Cipher ci = null;
			if (modeC) {
				if (ivb == null) {
					MessageManager mm = EngineMessage.get();
					Logger.info("des(input, key, iv; charset): " + mm.getMessage("encrypt.CBCnoiv"));
					ivb = DEF_IV;
				}
				else if (ivb.length != 8) {
					if (autopad) {
						ivb = AESEncrypt.padding(keyb, 8);
					}
					else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("des(input, key, iv; charset): " + mm.getMessage("encrypt.wronglen", "iv", 8));
					}
				}
				if (nopad) {
					ci = Cipher.getInstance(DESEncrypt.CIPHER_ALGORITHM_CN);
				} else {
					ci = Cipher.getInstance(DESEncrypt.CIPHER_ALGORITHM_C);
				}
			} else {
				if (nopad) {
					ci = Cipher.getInstance(DESEncrypt.CIPHER_ALGORITHM_EN);
				} else {
					ci = Cipher.getInstance(DESEncrypt.CIPHER_ALGORITHM_E);
				}
			}
			IvParameterSpec iv = null;
			if (ivb != null) {
				iv = new IvParameterSpec(ivb);
			}
			DESKeySpec dks = new DESKeySpec(keyb);
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM);  
	        //生成密钥  
	        SecretKey key = keyFactory.generateSecret(dks);
			byte[] result = null;
			if (dec) {
				if (!modeC) {
					ci.init(Cipher.DECRYPT_MODE, key);
					result = ci.doFinal(inputb);
				}
				else {
					ci.init(Cipher.DECRYPT_MODE, key, iv);
					result = ci.doFinal(inputb);
				}
				if (decStr) {
					return DESEncrypt.encode(result, charset);
				}
			} else {
				if (!modeC) {
					ci.init(Cipher.ENCRYPT_MODE, key);
					result = ci.doFinal(inputb);
				}
				else {
					ci.init(Cipher.ENCRYPT_MODE, key, iv);
					result = ci.doFinal(inputb);
				}
			}
			/*
			if (string) {			
				if (charset.equalsIgnoreCase("unicodeescape")  ||
						charset.equalsIgnoreCase("unicode escape") ||
						charset.equalsIgnoreCase("unicode-escape") ){
					return CharEncode.utf_unescape(new String(result));
				}else{
					return new String(result, charset);
				}
			}
			*/
			return result;
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return null;
	}
	
	protected static String encode(byte[] bytes, String charset) throws UnsupportedEncodingException {		
		if (charset.equalsIgnoreCase("unicodeescape")  ||
				charset.equalsIgnoreCase("unicode escape") ||
				charset.equalsIgnoreCase("unicode-escape") ){
			return CharEncode.utf_unescape(new String(bytes));
		}else{
			return new String(bytes, charset);
		}
	}
	
	protected static byte[] encode(Object obj, String encodeName) {
		if (obj == null) return null;
		if(obj instanceof byte[] ){
			return (byte[])obj;
		}else { //encode
			String str = null;
			if(obj instanceof String ){
				str = (String)obj;
			}
			else {
				str = obj.toString();
			}
			if (str != null) {
				if (encodeName.equalsIgnoreCase("unicodeescape")  ||
						encodeName.equalsIgnoreCase("unicode escape") ||
						encodeName.equalsIgnoreCase("unicode-escape") ){
					str = CharEncode.utf_escape(str);
					return str.getBytes(); 
				} else {
					try {
						return str.getBytes(encodeName);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
						Logger.error(e.getMessage());
						return null;
					}
				}
			}
			else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("encode error: " + mm.getMessage("function.paramTypeError"));
			}
		}
	}
}
