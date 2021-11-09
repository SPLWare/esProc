package com.raqsoft.common;

/* This class implements the DES arithmetic.
 * Notice: the shift operation to a byte value is hard to control, because Java
 * treat the byte type as a signed integer. In order to avoid the problem, the
 * process in the program is based on character type.
 */

public class DES {
	private static final int DIR_ENCRYPT = 1;
	private static final int DIR_DECRYPT = 2;

	// S-function table
	private static final char sFunctionTable[][] = {
		//  S-1
		{
			14,  4, 13,  1,  2, 15, 11,  8,  3, 10,  6, 12,  5,  9,  0,  7,
			0, 15,  7,  4, 14,  2, 13,  1, 10,  6, 12, 11,  9,  5,  3,  8,
			4,  1, 14,  8, 13,  6,  2, 11, 15, 12,  9,  7,  3, 10,  5,  0,
			15, 12,  8,  2,  4,  9,  1,  7,  5, 11,  3, 14, 10,  0,  6, 13
		},
		//  S-2
		{
			15,  1,  8, 14,  6, 11,  3,  4,  9,  7,  2, 13, 12,  0,  5, 10,
			3, 13,  4,  7, 15,  2,  8, 14, 12,  0,  1, 10,  6,  9, 11,  5,
			0, 14,  7, 11, 10,  4, 13,  1,  5,  8, 12,  6,  9,  3,  2, 15,
			13,  8, 10,  1,  3, 15,  4,  2, 11,  6,  7, 12,  0,  5, 14,  9
		},
		//  S-3
		{
			10,  0,  9, 14,  6,  3, 15,  5,  1, 13, 12,  7, 11,  4,  2,  8,
			13,  7,  0,  9,  3,  4,  6, 10,  2,  8,  5, 14, 12, 11, 15,  1,
			13,  6,  4,  9,  8, 15,  3,  0, 11,  1,  2, 12,  5, 10, 14,  7,
			1, 10, 13,  0,  6,  9,  8,  7,  4, 15, 14,  3, 11,  5,  2, 12
		},
		//  S-4
		{
			7, 13, 14,  3,  0,  6,  9, 10,  1,  2,  8,  5, 11, 12,  4, 15,
			13,  8, 11,  5,  6, 15,  0,  3,  4,  7,  2, 12,  1, 10, 14,  9,
			10,  6,  9,  0, 12, 11,  7, 13, 15,  1,  3, 14,  5,  2,  8,  4,
			3, 15,  0,  6, 10,  1, 13,  8,  9,  4,  5, 11, 12,  7,  2, 14
		},
		//  S-5
		{
			2, 12,  4,  1,  7, 10, 11,  6,  8,  5,  3, 15, 13,  0, 14,  9,
			14, 11,  2, 12,  4,  7, 13,  1,  5,  0, 15, 10,  3,  9,  8,  6,
			4,  2,  1, 11, 10, 13,  7,  8, 15,  9, 12,  5,  6,  3,  0, 14,
			11,  8, 12,  7,  1, 14,  2, 13,  6, 15,  0,  9, 10,  4,  5,  3
		},
		//  S-6
		{
			12,  1, 10, 15,  9,  2,  6,  8,  0, 13,  3,  4, 14,  7,  5, 11,
			10, 15,  4,  2,  7, 12,  9,  5,  6,  1, 13, 14,  0, 11,  3,  8,
			9, 14, 15,  5,  2,  8, 12,  3,  7,  0,  4, 10,  1, 13, 11,  6,
			4,  3,  2, 12,  9,  5, 15, 10, 11, 14,  1,  7,  6,  0,  8, 13
		},
		//  S-7
		{
			4, 11,  2, 14, 15,  0,  8, 13,  3, 12,  9,  7,  5, 10,  6,  1,
			13,  0, 11,  7,  4,  9,  1, 10, 14,  3,  5, 12,  2, 15,  8,  6,
			1,  4, 11, 13, 12,  3,  7, 14, 10, 15,  6,  8,  0,  5,  9,  2,
			6, 11, 13,  8,  1,  4, 10,  7,  9,  5,  0, 15, 14,  2,  3, 12
		},
		//  S-8
		{
			13,  2,  8,  4,  6, 15, 11,  1, 10,  9,  3, 14,  5,  0, 12,  7,
			1, 15, 13,  8, 10,  3,  7,  4, 12,  5,  6, 11,  0, 14,  9,  2,
			7, 11,  4,  1,  9, 12, 14,  2,  0,  6, 10, 13, 15,  3,  5,  8,
			2,  1, 14,  7,  4, 10,  8, 13, 15, 12,  9,  0,  3,  5,  6, 11
		}
	};

	private char[] codedKey = new char[8];

	//Creates a DES object whose key is supplied by a string.

	public byte[] pKey(byte[] key) {
		int len = key.length;
		if( len == 8 ) return key;

		byte[] bb = new byte[8];
		if( len<8 ) {
			System.arraycopy(key, 0, bb, 0, len);
			return bb;
		} else {
			for(int i=0; i<len; i++)
				bb[i&7] ^= (key[i]<<2);
		}
		return bb;
	}


	public DES(String key) throws Exception {
		doKey( pKey(key.getBytes("UTF-8")) );
	}

	//Creates a DES object whose key is supplied by a byte array.
	public DES(byte[] key) throws Exception {
		doKey( pKey(key) );
	}

	//Creates a DES object whose key is supplied by a character array.
	public DES(char[] key) throws Exception {
		doKey(key);
	}

	//Encrypts the source data which are supplied by a byte array.
	public byte[] encrypt(byte[] source) throws Exception
	{
		return desProcess(DIR_ENCRYPT, source, source.length);
	}

	public byte[] encrypt(byte[] source, int len) throws Exception
	{
		return desProcess(DIR_ENCRYPT, source, len);
	}

	//Decrypts the source data which are supplied by a byte array.
	public byte[] decrypt(byte[] source) throws Exception
	{
		return desProcess(DIR_DECRYPT, source, source.length);
	}

	public byte[] decrypt(byte[] source, int len) throws Exception
	{
		return desProcess(DIR_DECRYPT, source, len);
	}

	//Encrypts the source data which are supplied by a character array.
	public char[] encrypt(char[] source) throws Exception
	{
		return desProcess(DIR_ENCRYPT, source, source.length);
	}

	//Decrypts the source data which are supplied by a character array.
	public char[] decrypt(char[] source) throws Exception
	{
		return desProcess(DIR_DECRYPT, source, source.length);
	}

	//Encrypts or decrypts the specified part of a byte array.
	private byte[] desProcess(int dir, byte[] s, int l) throws Exception
	{
		if( dir==DIR_ENCRYPT ) {
			int len = s.length;
			byte[] buf = new byte[4+len];
			buf[0] = (byte)(len>>24);
			buf[1] = (byte)(len>>16);
			buf[2] = (byte)(len>>8);
			buf[3] = (byte)len;
			//System.out.println( (int)buf[0] + "," + (int)buf[1] + "," + (int)buf[2] + "," + (int)buf[3] );
			System.arraycopy(s, 0, buf, 4, len);
			return desProcess1(dir, buf, 4+l);
		} else {
			byte[] buf1 = desProcess1(dir, s, l);
			//System.out.println( (int)buf1[0] + "," + (int)buf1[1] + "," + (int)buf1[2] + "," + (int)buf1[3] );
			int len = ((buf1[0]&0xff)<<24) + ((buf1[1]&0xff)<<16) + ((buf1[2]&0xff)<<8) + (buf1[3]&0xff);
			//System.out.println( "len=" + len );
			byte[] buf2 = new byte[len];
			System.arraycopy(buf1, 4, buf2, 0, len);
			return buf2;
		}
	}

	private byte[] desProcess1(int dir, byte[] source, int l)
		 throws Exception
	{
		//Convert the byte array to a character array.
		char[] srcBuffer = convertByteToChar(source, l);
		//Encrypt or decrypt the character array.
		char[] destBuffer = desProcess(dir, srcBuffer, l);

		//Convert the result character array to a byte array.
		byte[] dest = convertCharToByte(destBuffer, destBuffer.length);
		return dest;
	}

	//Encrypts or decrypts the specified part of a character array.
	private char[] desProcess(int dir, char[] s, int l) throws Exception
	{
		//Compute the length of the result.
		int rl = (s.length / 8) * 8 + (s.length % 8 == 0 ? 0 : 8);

		//Allocate memory for the result.
		char[] dest = new char[rl];

		// Allocate a 8 bytes buffer for internal processing, because
		// DES arithmetic process data 8 bytes by 8 bytes.
		char[] buffer = new char[8];
		int offset = 0;
		while(l > 0) {
			//Copy the source data to the buffer. If the length of
			// source data is less then 8 bytes, fill the buffer with 0.
			System.arraycopy(s, offset, buffer, 0, l > 8 ? 8 : l);
			for (int i = l; i < 8; i++) {
				buffer[i] = 0x0;
			}

			//Encrypt or decrypt this 8 characters.
			desProcess8chars(dir, buffer);
			System.arraycopy(buffer, 0, dest, offset, 8);
			offset += 8;
			l -= 8;
		}
		return dest;
	}

	//Encrypts or decrypts 8 characters supplied by a character array.
	private void desProcess8chars(int dir, char[] s) throws Exception
	{
		char[] keyCopy = new char[8];
		int[] encryptLoop = {1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1};
		int[] decryptLoop = {1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1, 1};
		int[] loop;
		char[] k = new char[6];
		char c;

		if (dir == DIR_ENCRYPT) {
			loop = encryptLoop;
		} else {
			loop = decryptLoop;
		}
		doFirstChange(s);
		System.arraycopy(codedKey, 0, keyCopy, 0, 8);
		for (int i = 0; i < 16; ++i) {
			setKey(dir, keyCopy, loop[i], k);
			doMut(s, k);
		}
		for(int i = 0; i < 4; ++i) {
			c = s[i + 4];
			s[i + 4] = s[i];
			s[i] = c;
		}
		doLastChange(s);
	}

	//Processes the original key supplied by a byte array to its final format.
	private void doKey(byte[] key) throws Exception {
		char[] charBuffer = convertByteToChar(key, key.length);
		doKey(charBuffer);
	}

	//Processes the original key supplied by a character array to its final format.
	private void doKey(char[] key) throws Exception {
		if (key.length != 8) {
			throw new Exception("Invalid key length:" + key.length);
		}

		for (int i = 0; i < 8; i++) {
			if (key[i] > 255) {
				throw new Exception("Invalid character found in the des key");
			}
		}

		char[] t = new char[8];
		for (int i = 0; i < 8; ++i) {
			t[i] = 0;
		}

		for (int i = 0; i < 8; ++i) {
			for (int j = 0; j < 8; ++j) {
				t[7-j] |= (((key[i] & (0x01 << j)) >> j) << i) & 0xff;
			}
		}
		for (int i = 0; i < 4; i++) {
			codedKey[i] = t[i];
			codedKey[i + 4] = t[6-i];
		}
		codedKey[3] &= 0xf0;
		codedKey[7] = (char)(((codedKey[7] & 0x0f) << 4) & 0xff);
	}

	private void doFirstChange(char[] s) {
		char[] t = new char[8];

		for (int i = 0; i < 8; ++i) {
			t[i] = 0;
		}

		for (int i = 0; i < 8; ++i) {
			for (int j = 0; j < 8; ++j) {
				t[7-j] |= (((s[i] >> j) & 0x01 ) << i) & 0xff;
			}
		}
		for (int i = 0; i < 4; ++i) {
			s[i] = t[2 * i + 1];
			s[i + 4] = t[2 * i];
		}
	}

	private void doLastChange(char[] s) {
		char[] t = new char[8];

		for(int i = 0; i < 8; ++i) {
			t[i] = 0;
		}

		for(int i = 0; i < 8; ++i) {
			for(int j = 0; j < 4; ++j) {
				t[i] |= ((s[j] << (7 - i)) & 0x80) >> (2 * j + 1);
				t[i] |= ((s[j + 4] << (7 - i)) & 0x80) >> (2 * j);
			}
		}

		/*for(int i = 0; i < 8; ++i) {
			s[i] = t[i];
		}*/
		System.arraycopy(t, 0, s, 0, 8);
	}

	private void doLeft(char[] s, int n) {
		doLeft(s, 0, n);
	}

	private void doLeft(char[] s, int offset, int n) {
		char l, t;

		l = (char)((0xff << (8 - n)) & 0xff);
		t = (char)((s[offset] & l) >> 4);
		s[offset + 3] |= t;
		for(int i = offset; i < offset + 3; i++) {
			s[i] <<= n;
			s[i] &= 0xff;
			t = (char)((s[i + 1] & l) >> (8 - n));
			s[i] |= t;
		}
		s[offset + 3] <<= n;
		s[offset + 3] &= 0xff;
	}

	private void doRight(char[] s, int n) {
		doRight(s, 0, n);
	}

	private void doRight(char[] s, int offset, int n) {
		for (int i = 0; i < n; ++i) {
			char l0 = (char)(s[offset] & 1);
			char l1 = (char)(s[offset + 1] & 1);
			s[offset] >>= 1;
			s[offset + 1] >>= 1;
			s[offset + 1] |= (l0 << 7) & 0xff;
			l0 = (char)(s[offset + 2] & 1);
			s[offset + 2] >>= 1;
			s[offset + 2] |= (l1 << 7) & 0xff;
			s[offset + 3] >>= 1;
			s[offset + 3] |= (l0 << 7) & 0xff;
			if ((s[offset + 3] & 0xf) != 0) {
				s[offset] |= 0x80;
				s[offset + 3] &= 0xf0;
			}
		}
	}

	private void setKey(int dir, char[] key, int n, char[] k) {
		for (int i = 0; i < 6; ++i) {
			k[i] = 0;
		}

		if (dir == DIR_ENCRYPT) {
			doLeft(key, n);
			doLeft(key, 4, n);
		}
		k[0] = (char)( ((key[1] & 4) << 5)
					 | ((key[2] & 0x80) >> 1)
					  | (key[1] & 0X20)
					 | ((key[2] & 1) << 4)
					 | ((key[0] & 0X80) >> 4)
					 | ((key[0] & 8) >> 1)
					 | ((key[0] & 0x20) >> 4)
					 | ((key[3] & 0X10) >> 4) );
		k[1] = (char)( ((key[1] & 2) << 6)
					 | ((key[0] & 4) << 4)
					 | ((key[2] & 8) << 2)
					 | ((key[1] & 0x40) >> 2)
					 | ((key[2] & 2) << 2)
					 | ((key[2] & 0x20) >> 3)
					 | ((key[1] & 0x10) >> 3)
					 | ((key[0] & 0X10) >> 4) );
		k[2] = (char)( ((key[3] & 0x40) << 1)
					 | ((key[0] & 0x01) << 6)
					 | ((key[1] & 0x01) << 5)
					 | ((key[0] & 2) << 3)
					 | ((key[3] & 0x20) >> 2)
					 | ((key[2] & 0x10) >> 2)
					 | ((key[1] & 0x08) >> 2)
					 | ((key[0] & 0x40) >> 6) );
		k[3] = (char)( ((key[5] & 0x08) << 4)
					 | ((key[6] & 0X01) << 6)
					 | (key[4] & 0X20)
					 | ((key[5] & 0x80) >> 3)
					 | ((key[6] & 0x20) >> 2)
					 | ((key[7] & 0x20) >> 3)
					 | ((key[4] & 0x40) >> 5)
					 | ((key[5] & 0x10) >> 4) );
		k[4] = (char)( ((key[6] & 0x02) << 6)
					 | ((key[6] & 0x80) >> 1)
					 | ((key[4] & 0x08) << 2)
					 | (key[6] & 0x10)
					 | ((key[5] & 0X01) << 3 )
					 | ((key[6] & 0x08) >> 1)
					 | ((key[5] & 0x20) >> 4)
					 | ((key[7] & 0x10) >> 4) );
		k[5] = (char)( ((key[4] & 0x04) << 5)
					 | ((key[7] & 0X80) >> 1)
					 | ((key[6] & 0x40)) >> 1
					 | ((key[5] & 0x04) << 2)
					 | ((key[6] & 0x04) << 1)
					 | ((key[4] & 0x01) << 2)
					 | ((key[4] & 0x80) >> 6)
					 | ((key[4] & 0x10) >> 4) );

		if (dir == DIR_DECRYPT) {
			doRight(key, n);
			doRight(key, 4, n);
		}
	}

	private void eExpand(char[] s, char[] r) {
		r[0] = (char)( ((s[4 + 3] & 0x01) << 7)
					 | ((s[4 + 0] & 0xf8) >> 1)
					 | ((s[4 + 0] & 0x18) >> 3) );
		r[1] = (char)( ((s[4 + 0] & 0x07) << 5)
					 | ((s[4 + 0] & 0x01) << 3)
					 | ((s[4 + 1] & 0x80) >> 3)
					 | ((s[4 + 1] & 0xe0) >> 5) );
		r[2] = (char)( ((s[4 + 1] & 0x18) << 3)
					 | ((s[4 + 1] & 0x1f) << 1)
					 | ((s[4 + 2] & 0x80) >> 7) );
		r[3] = (char)( ((s[4 + 1] & 0x01) << 7)
					 | ((s[4 + 2] & 0xf8) >> 1)
					 | ((s[4 + 2] & 0x18) >> 3) );
		r[4] = (char)( ((s[4 + 2] & 0x07) << 5)
					 | ((s[4 + 2] & 0x01) << 3)
					 | ((s[4 + 3] & 0x80) >> 3)
					 | ((s[4 + 3] & 0xe0) >> 5) );
		r[5] = (char)( ((s[4 + 3] & 0x18) << 3)
					 | ((s[4 + 3] & 0x1f) << 1)
					 | ((s[4 + 0] & 0x80) >> 7) );
	}

	private void pChange(char[] s) throws Exception
	{
		char[] t = null;

		if (s == null || s.length < 4) {
			throw new Exception("Invalid parameter");
		}

		t = new char[4];
		t[0] = (char)( ((s[1] & 0x01) << 7)
					 | ((s[0] & 0x02) << 5)
					 | ((s[2] & 0x10) << 1)
					 | ((s[2] & 0x08) << 1)
					 | (s[3] & 0x08)
					 | ((s[1] & 0x10) >> 2)
					 | ((s[3] & 0x10) >> 3)
					 | ((s[2] & 0x80) >> 7) );
		t[1] = (char)( (s[0] & 0x80)
					 | ((s[1] & 0x02) << 5)
					 | ((s[2] & 0x02) << 4)
					 | ((s[3] & 0x40) >> 2)
					 | (s[0] & 0x08)
					 | ((s[2] & 0x40) >> 4)
					 | (s[3] & 0x02)
					 | ((s[1] & 0x40) >> 6) );
		t[2] = (char)( ((s[0] & 0x40) << 1)
					 | ((s[0] & 0x01) << 6)
					 | ((s[2] & 0x01) << 5)
					 | ((s[1] & 0x04) << 2)
					 | ((s[3] & 0x01) << 3)
					 | ((s[3] & 0x20) >> 3)
					 | ((s[0] & 0x20) >> 4)
					 | ((s[1] & 0x80) >> 7) );
		t[3] = (char)( ((s[2] & 0x20) << 2)
					 | ((s[1] & 0x08) << 3)
					 | ((s[3] & 0x04) << 3)
					 | ((s[0] & 0x04) << 2)
					 | ((s[2] & 0x04) << 1)
					 | ((s[1] & 0x20) >> 3)
					 | ((s[0] & 0x10) >> 3)
					 | ((s[3] & 0x80) >> 7) );

		for(int i = 0 ; i < 4; ++i) {
			s[i] = t[i];
		}
	}

	private char findS(char[] s, int ns) {
		return findS(s, 0, ns);
	}

	private char findS(char[] s, int offset, int ns) {
		int col, num, index = 0;

		if (ns == 1 || ns == 5) {
			col = ((s[offset] & 0x80) >> 6 ) | ((s[offset] & 0x04) >> 2);
			num = (s[offset] & 0x78) >> 3;
			index = col * 16 + num;
		}
		if (ns == 2 || ns == 6) {
			col = (s[offset] & 0x02) | ((s[offset + 1] & 0x10) >> 4);
			num = ((s[offset] & 0x01) << 3) | ((s[offset + 1] & 0xe0) >> 5);
			index = col * 16 + num;
		}
		if (ns == 3 || ns == 7) {
			col = ((s[offset + 1] & 0x08) >> 2)
				 | ((s[offset + 2] & 0x40) >> 6);
			num = ((s[offset + 1] & 0x07) << 1)
				 | ((s[offset + 2] & 0x80) >> 7);
			index = col * 16 + num;
		}
		if (ns == 4 || ns == 8) {
			col = ((s[offset + 2] & 0x20) >> 4) | (s[offset + 2] & 0x01);
			num = ((s[offset + 2] & 0x1e) >> 1);
			index = col * 16 + num;
		}
		return sFunctionTable[ns-1][index];
	}

	private void doSFunction(char[] s, char[] r) {
		r[0] = (char)((findS(s, 1) << 4) | findS(s, 2));
		r[1] = (char)((findS(s, 3) << 4) | findS(s, 4));
		r[2] = (char)((findS(s, 3, 5) << 4) | findS(s, 3, 6));
		r[3] = (char)((findS(s, 3, 7) << 4) | findS(s, 3, 8));
	}

	private void fFunction(char[] s, char[] k, char[] m) throws Exception
	{
		char[] t = new char[6];

		eExpand(s, t);
		for (int i = 0; i < 6; ++i) {
			t[i] ^= k[i];
		}
		doSFunction(t, m);
		pChange(m);
	}

	private void doMut(char[] s, char[] k) throws Exception
	{
		char[] t = new char[4];

		fFunction(s, k, t);
		for (int i = 0; i < 4; ++i) {
			t[i] ^= s[i];
			s[i] = s[i + 4];
			s[i + 4] = t[i];
		}
	}


	//Convert the byte array to a character array.
	static char[] convertByteToChar(byte[] source, int srclen) {
		if (source == null)
			return null;

		int len = source.length;
		if (len > srclen)
			len = srclen;
		char[] destChar = new char[len];
		for (int i = 0; i < len; i++) {
			if (source[i] >= 0)
				destChar[i] = (char)source[i];
			else
				destChar[i] = (char)(256 + source[i]);
		}
		return destChar;
	}

	//Convert the character array to a byte array.
	byte[] convertCharToByte(char[] source, int srclen)	{
		if (source == null)
			return null;

		int len = source.length;
		if (len > srclen)
			len = srclen;
		byte[] dest = new byte[len];
		for (int i = 0; i < len; i++)
			dest[i] = (byte)source[i];
		return dest;
	}

	public static void main(String[] args) throws Exception
	{
		DES des = new DES("1234567890");
		String s = "abcdefgh中国人民解放军";
		//for( int i = 0; i < 10; i++ )
		//	s += s;
		byte[] bs = s.getBytes("utf-8");
		System.out.println( "byte[] len=" + bs.length );
		bs = des.encrypt(bs);
		for( int i = 0; i < bs.length; i ++ ) {
			System.out.print(bs[i]);
			System.out.print(' ');
		}
		System.out.println();

		bs = des.decrypt(bs);
		System.out.println( bs.length );
		s = new String(bs, "utf-8");
		System.out.println( s + "," + s.length() );

		//System.out.println(new Date());
		//for (int i=0; i<100000; i++) {
		//	bs = des.encrypt(bs, 400);//包长度，8的倍数
		//	bs = des.decrypt(bs, 400);
		//}
		//System.out.println(new Date());
	}

}
