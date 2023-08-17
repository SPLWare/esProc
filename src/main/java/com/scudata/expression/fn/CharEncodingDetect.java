package com.scudata.expression.fn;

/**
 * <Detect encoding .>
 *  Copyright (C) <2009>  <Fluck,ACC http://androidos.cc/dev>
 *
 *   This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CharEncodingDetect extends Encoding {
	public static enum LANG {
		ALL, CHINESE, SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE, JAPANESE, KOREAN
	};

	int scores[];
	public boolean debug;

	public CharEncodingDetect() {
		super();
		debug = false;
		scores = new int[TOTALTYPES];
	}

	public static void main(String argc[]) {
		CharEncodingDetect sinodetector;
		int result = OTHER;
		int i;
		sinodetector = new CharEncodingDetect();
		for (i = 0; i < argc.length; i++) {
			if (argc[i].startsWith("http://") == true) {
				try {
					result = sinodetector.detectEncoding(new URL(argc[i]));
				} catch (Exception e) {
					System.err.println("Bad URL " + e.toString());
				}
			} else if (argc[i].equals("-d")) {
				sinodetector.debug = true;
				continue;
			} else {
				result = sinodetector.detectEncoding(new File(argc[i]));
			}
			System.out.println(nicename[result]);
		}
	}

	/**
	 * Function : detectEncoding Aruguments: URL Returns : One of the encodings
	 * from the Encoding enumeration (GB2312, HZ, BIG5, EUC_TW, ASCII, or OTHER)
	 * Description: This function looks at the URL contents and assigns it a
	 * probability score for each encoding type. The encoding type with the
	 * highest probability is returned.
	 */
	public int detectEncoding(URL testurl) {
		byte[] rawtext = new byte[10000];
		int bytesread = 0, byteoffset = 0;
		int guess = OTHER;
		InputStream chinesestream;
		try {
			chinesestream = testurl.openStream();
			while ((bytesread = chinesestream.read(rawtext, byteoffset, rawtext.length - byteoffset)) > 0) {
				byteoffset += bytesread;
			}

			chinesestream.close();
			guess = detectEncodingLang(rawtext, 0);
		} catch (Exception e) {
			System.err.println("Error loading or using URL " + e.toString());
			guess = -1;
		}
		return guess;
	}

	/**
	 * Function : detectEncoding Aruguments: File Returns : One of the encodings
	 * from the Encoding enumeration (GB2312, HZ, BIG5, EUC_TW, ASCII, or OTHER)
	 * Description: This function looks at the file and assigns it a probability
	 * score for each encoding type. The encoding type with the highest
	 * probability is returned.
	 */
	public int detectEncoding(File testfile) {
		FileInputStream chinesefile;
		byte[] rawtext;
		rawtext = new byte[(int) testfile.length()];
		try {
			chinesefile = new FileInputStream(testfile);
			chinesefile.read(rawtext);
			chinesefile.close();
		} catch (Exception e) {
			System.err.println("Error: " + e);
		}
		return detectEncodingLang(rawtext, 0);
	}

	/**
	 * Function : detectEncoding Aruguments: byte array Returns : One of the
	 * encodings from the Encoding enumeration (GB2312, HZ, BIG5, EUC_TW, ASCII,
	 * or OTHER) Description: This function looks at the byte array and assigns
	 * it a probability score for each encoding type. The encoding type with the
	 * highest probability is returned.
	 */
	public int detectEncoding(byte[] rawtext) {
		return detectEncodingLang(rawtext, 0);
	}

	public int detectEncoding(byte[] rawtext, int lang) {
		return detectEncodingLang(rawtext, lang);
	}

	private int detectEncodingLang(byte[] rawtext, int lang) {
		if (lang == LANG.CHINESE.ordinal()) {
			scores[GB2312] = gb2312_probability(rawtext);
			scores[GBK] = gbk_probability(rawtext);
			scores[GB18030] = gb18030_probability(rawtext);
			scores[HZ] = hz_probability(rawtext);
			scores[BIG5] = big5_probability(rawtext);
			scores[CNS11643] = euc_tw_probability(rawtext);
			scores[ISO2022CN] = iso_2022_cn_probability(rawtext);
			scores[UTF8] = utf8_probability(rawtext);
			scores[UNICODE] = utf16_probability(rawtext);
			scores[UNICODE_ESCAPE] = utf16_escape_probability(rawtext);
			scores[ASCII] = ascii_probability(rawtext);
		} else if (lang == LANG.SIMPLIFIED_CHINESE.ordinal()) {
			scores[GB2312] = gb2312_probability(rawtext);
			scores[GB18030] = gb18030_probability(rawtext);
			scores[ASCII] = ascii_probability(rawtext);
			scores[UTF8] = utf8_probability(rawtext);
			scores[UNICODE] = utf16_probability(rawtext);
			scores[UNICODE_ESCAPE] = utf16_escape_probability(rawtext);
		} else if (lang == LANG.TRADITIONAL_CHINESE.ordinal()) {
			scores[BIG5] = big5_probability(rawtext);
			scores[CNS11643] = euc_tw_probability(rawtext);
			scores[ASCII] = ascii_probability(rawtext);
			scores[UTF8] = utf8_probability(rawtext);
			scores[UNICODE] = utf16_probability(rawtext);
			scores[UNICODE_ESCAPE] = utf16_escape_probability(rawtext);
		} else if (lang == LANG.JAPANESE.ordinal()) {
			scores[SJIS] = sjis_probability(rawtext);
			scores[EUC_JP] = euc_jp_probability(rawtext);
			scores[ISO2022JP] = iso_2022_jp_probability(rawtext);
			scores[ASCII] = ascii_probability(rawtext);
			scores[UTF8] = utf8_probability(rawtext);
			scores[UNICODE] = utf16_probability(rawtext);
		} else if (lang == LANG.KOREAN.ordinal()) {
			scores[ISO2022KR] = iso_2022_kr_probability(rawtext);
			scores[EUC_KR] = euc_kr_probability(rawtext);
			scores[CP949] = cp949_probability(rawtext);
			scores[ASCII] = ascii_probability(rawtext);
			scores[UTF8] = utf8_probability(rawtext);
			scores[UNICODE] = utf16_probability(rawtext);
		} else { // LANG.ALL
			scores[GB2312] = gb2312_probability(rawtext);
			scores[GBK] = gbk_probability(rawtext);
			scores[GB18030] = gb18030_probability(rawtext);
			scores[HZ] = hz_probability(rawtext);
			scores[BIG5] = big5_probability(rawtext);
			scores[CNS11643] = euc_tw_probability(rawtext);
			scores[ISO2022CN] = iso_2022_cn_probability(rawtext);
			scores[UTF8] = utf8_probability(rawtext);
			scores[UNICODE] = utf16_probability(rawtext);
			scores[EUC_KR] = euc_kr_probability(rawtext);
			scores[CP949] = cp949_probability(rawtext);
			scores[JOHAB] = 0;
			scores[ISO2022KR] = iso_2022_kr_probability(rawtext);
			scores[ASCII] = ascii_probability(rawtext);
			scores[SJIS] = sjis_probability(rawtext);
			scores[EUC_JP] = euc_jp_probability(rawtext);
			scores[ISO2022JP] = iso_2022_jp_probability(rawtext);
			scores[UNICODET] = 0;
			scores[UNICODE_ESCAPE] = utf16_escape_probability(rawtext);
			scores[ISO2022CN_GB] = 0;
			scores[ISO2022CN_CNS] = 0;
			scores[OTHER] = 0;
		}
		// Tabulate Scores
		int index, maxscore = 0;
		int encoding_guess = OTHER;

		for (index = 0; index < TOTALTYPES; index++) {
			if (debug && scores[index] > 0)
				System.err.println("Encoding " + nicename[index] + " score " + scores[index]);
			if (scores[index] > maxscore) {
				encoding_guess = index;
				maxscore = scores[index];
			}
		}
		// Return OTHER if nothing scored above 50
		if (maxscore <= 50) {
			encoding_guess = OTHER;
		}
		return encoding_guess;
	}

	public List<String> autoDetectEncoding(byte[] rawtext) {
		scores[GB2312] = gb2312_probability(rawtext);
		scores[GBK] = gbk_probability(rawtext);
		scores[GB18030] = gb18030_probability(rawtext);
		scores[HZ] = hz_probability(rawtext);
		scores[BIG5] = big5_probability(rawtext);
		scores[CNS11643] = euc_tw_probability(rawtext);
		scores[ISO2022CN] = iso_2022_cn_probability(rawtext);
		scores[UTF8] = utf8_probability(rawtext);
		scores[UNICODE] = utf16_probability(rawtext);
		scores[EUC_KR] = euc_kr_probability(rawtext);
		scores[CP949] = cp949_probability(rawtext);
		scores[JOHAB] = 0;
		scores[ISO2022KR] = iso_2022_kr_probability(rawtext);
		scores[ASCII] = ascii_probability(rawtext);
		scores[SJIS] = sjis_probability(rawtext);
		scores[EUC_JP] = euc_jp_probability(rawtext);
		scores[ISO2022JP] = iso_2022_jp_probability(rawtext);
		scores[UNICODET] = 0;
		scores[UNICODE_ESCAPE] = utf16_escape_probability(rawtext);
		scores[ISO2022CN_GB] = 0;
		scores[ISO2022CN_CNS] = 0;
		scores[OTHER] = 0;

		// Tabulate Scores
		int index, maxscore = 0;
		int encoding_guess = OTHER;
		List<String> lls = new ArrayList<String>();
		for (index = 0; index < TOTALTYPES; index++) {
			if (debug && scores[index] > 0)
				System.err.println("Encoding " + nicename[index] + " score " + scores[index]);
			if (scores[index] >= maxscore) {
				encoding_guess = index;
				if (scores[index] > maxscore){
					lls.clear();
				}
				maxscore = scores[index];
				lls.add(nicename[index]);
			}
		}
		// Return OTHER if nothing scored above 50
		if (maxscore <= 50) {
			encoding_guess = OTHER;
			lls.clear();
			lls.add("OTHER");
		}
		
		return lls;
	}
	/*
	 * Function: gb2312_probability Argument: pointer to byte array Returns :
	 * number from 0 to 100 representing probability text in array uses GB-2312
	 * encoding
	 */
	int gb2312_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, gbchars = 1;
		long gbfreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column;
		// Stage 1: Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			// System.err.println(rawtext[i]);
			if (rawtext[i] >= 0) {
				// asciichars++;
			} else {
				dbchars++;
				if ((byte) 0xA1 <= rawtext[i]     && rawtext[i] <= (byte) 0xF7 && 
					(byte) 0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) {
					gbchars++;
					totalfreq += 500;
					row = rawtext[i] + 256 - 0xA1;
					column = rawtext[i + 1] + 256 - 0xA1;
					if (15 <= row && row < 55) {
						gbfreq += 435;
					} else if (55 <= row && row < 87) {
						gbfreq += 234;
					}else{
						gbfreq += 50;
					}
				}
				i++;
			}
		}
		rangeval = 50 * ((float) gbchars / (float) dbchars);
		freqval = 50 * ((float) gbfreq / (float) totalfreq);
//		System.out.println("gb2312_probability::gbchars=" + gbchars + ";dbchars=" + dbchars + ";gbfreq=" + gbfreq
//				+ ";totalfreq=" + totalfreq + ";total=" + (rangeval + freqval));
		return (int) (rangeval + freqval);
	}

	/*
	 * Function: gbk_probability Argument: pointer to byte array Returns :
	 * number from 0 to 100 representing probability text in array uses GBK
	 * encoding
	 */
	int gbk_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, gbchars = 1;
		long gbfreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column;
		// Stage 1: Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			// System.err.println(rawtext[i]);
			if (rawtext[i] >= 0) {
				// asciichars++;
			} else {
				dbchars++;
				if ((byte) 0xA1 <= rawtext[i]     && rawtext[i] <= (byte) 0xF7 && // Original GB range
					(byte) 0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) {
					gbchars++;
					totalfreq += 500;
					row = rawtext[i] + 256 - 0xA1;
					column = rawtext[i + 1] + 256 - 0xA1;
					
					if (15 <= row && row < 55) {
						gbfreq += 435;
					} else if (55 <= row && row < 87) {
						gbfreq += 234;
					}else{
						gbfreq += 50;
					}
				} else if ((byte) 0x81 <= rawtext[i] && rawtext[i] <= (byte) 0xFE && // Extended GB range
						 (((byte) 0x80 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) ||
						  ((byte) 0x40 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7E))) {
					gbchars++;
					totalfreq += 500;
					row = rawtext[i] + 256 - 0x81;
					if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
						column = rawtext[i + 1] - 0x40;
					} else {
						column = rawtext[i + 1] + 256 - 0x40;
					}
					
					gbfreq += 185;
				}
				i++;
			}
		}
		rangeval = 50 * ((float) gbchars / (float) dbchars);
		freqval = 50 * ((float) gbfreq / (float) totalfreq);
		// For regular GB files, this would give the same score, so I handicap it slightly
//		System.out.println("gbk_probability::gbchars=" + gbchars + ";dbchars=" + dbchars + ";gbfreq=" + gbfreq
//				+ ";totalfreq=" + totalfreq + ";total=" + (rangeval + freqval));
		return (int) (rangeval + freqval) - 1;
	}

	/*
	 * Function: gb18030_probability Argument: pointer to byte array Returns :
	 * number from 0 to 100 representing probability text in array uses GBK
	 * encoding
	 */
	int gb18030_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, gbchars = 1;
		long gbfreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column;
		// Stage 1: Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			if (rawtext[i] >= 0) {
				// asciichars++;
			} else {
				dbchars++;
				if (i + 1 < rawtextlen && 
						(byte) 0xA1 <= rawtext[i]     && rawtext[i] <= (byte) 0xF7 && 
						(byte) 0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) {
					gbchars++;
					totalfreq += 500;
					row = rawtext[i] + 256 - 0xA1;
					column = rawtext[i + 1] + 256 - 0xA1;
					
					if (15 <= row && row < 55) {
						gbfreq += 435;
					} else if (55 <= row && row < 87) {
						gbfreq += 234;
					}else{
						gbfreq += 50;
					}
				} else if (i + 1 < rawtextlen && 
						(byte) 0x81 <= rawtext[i] && rawtext[i] <= (byte) 0xFE && 
						(((byte) 0x80 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) || 
						 ((byte) 0x40 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7E)) ) {
					gbchars++;
					totalfreq += 500;
					row = rawtext[i] + 256 - 0x81;
					if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
						column = rawtext[i + 1] - 0x40;
					} else {
						column = rawtext[i + 1] + 256 - 0x40;
					}

					gbfreq += 185;
				} else if (i + 3 < rawtextlen &&
						(byte) 0x81 <= rawtext[i] && rawtext[i] <= (byte) 0xFE && 
						(byte) 0x30 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x39 && 
						(byte) 0x81 <= rawtext[i + 2] && rawtext[i + 2] <= (byte) 0xFE && 
						(byte) 0x30 <= rawtext[i + 3] && rawtext[i + 3] <= (byte) 0x39) {
					gbchars++;
				}
				i++;
			}
		}
		rangeval = 50 * ((float) gbchars / (float) dbchars);
		freqval = 50 * ((float) gbfreq / (float) totalfreq);
//		System.out.println("gb18030_probability::gbchars=" + gbchars + ";dbchars=" + dbchars + ";gbfreq=" + gbfreq
//				+ ";totalfreq=" + totalfreq + ";total=" + (rangeval + freqval));
		return (int) (rangeval + freqval) - 1;
	}

	/*
	 * Function: hz_probability Argument: byte array Returns : number from 0 to
	 * 100 representing probability text in array uses HZ encoding
	 */
	int hz_probability(byte[] rawtext) {
		int i, rawtextlen;
		int hzchars = 0, dbchars = 1;
		long hzfreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int hzstart = 0, hzend = 0;
		int row, column;
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen; i++) {
			if (rawtext[i] == '~') {
				if (rawtext[i + 1] == '{') {
					hzstart++;
					i += 2;
					while (i < rawtextlen - 1) {
						if (rawtext[i] == 0x0A || rawtext[i] == 0x0D) {
							break;
						} else if (rawtext[i] == '~' && rawtext[i + 1] == '}') {
							hzend++;
							i++;
							break;
						} else if ((0x21 <= rawtext[i] 	   && rawtext[i] <= 0x77) &&
								   (0x21 <= rawtext[i + 1] && rawtext[i + 1] <= 0x77)) {
							hzchars += 2;
							row = rawtext[i] - 0x21;
							column = rawtext[i + 1] - 0x21;
							totalfreq += 500;

							if (15 <= row && row < 55) {
								hzfreq += 435;
							} else if (55 <= row && row < 87) {
								hzfreq += 234;
							}else{
								hzfreq += 50;
							}
						} else if ((0xA1 <= rawtext[i] && rawtext[i] <= 0xF7)
								&& (0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= 0xF7)) {
							hzchars += 2;
							row = rawtext[i] + 256 - 0xA1;
							column = rawtext[i + 1] + 256 - 0xA1;
							totalfreq += 500;

							if (15 <= row && row < 55) {
								hzfreq += 435;
							} else if (55 <= row && row < 87) {
								hzfreq += 234;
							}else{
								hzfreq += 50;
							}
						}
						dbchars += 2;
						i += 2;
					}
				} else if (rawtext[i + 1] == '}') {
					hzend++;
					i++;
				} else if (rawtext[i + 1] == '~') {
					i++;
				}
			}
		}
		if (hzstart > 4) {
			rangeval = 50;
		} else if (hzstart > 1) {
			rangeval = 41;
		} else if (hzstart > 0) { // Only 39 in case the sequence happened to occur
			rangeval = 39; 		  // in otherwise non-Hz text
		} else {
			rangeval = 0;
		}
		freqval = 50 * ((float) hzfreq / (float) totalfreq);
		return (int) (rangeval + freqval);
	}

	/**
	 * Function: big5_probability Argument: byte array Returns : number from 0
	 * to 100 representing probability text in array uses Big5 encoding
	 */
	int big5_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, bfchars = 1;
		float rangeval = 0, freqval = 0;
		long bffreq = 0, totalfreq = 1;
		int row, column;
		// Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			if (rawtext[i] >= 0) {
				// asciichars++;
			} else {
				dbchars++;
				if (  (byte) 0xA1 <= rawtext[i] && rawtext[i] <= (byte) 0xF9 &&
					(((byte) 0x40 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7E) ||
					 ((byte) 0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE)) ) {
					bfchars++;
					totalfreq += 500;
					row = rawtext[i] + 256 - 0xA1;
					if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
						column = rawtext[i + 1] - 0x40;
					} else {
						column = rawtext[i + 1] + 256 - 0x61;
					}

					if (3 <= row && row <= 37) {
						bffreq += 436;
					} else {
						bffreq += 50;
					}
				}
				i++;
			}
		}
		rangeval = 50 * ((float) bfchars / (float) dbchars);
		freqval = 50 * ((float) bffreq / (float) totalfreq);
		return (int) (rangeval + freqval);
	}

	/*
	 * Function: big5plus_probability Argument: pointer to unsigned char array
	 * Returns : number from 0 to 100 representing probability text in array
	 * uses Big5+ encoding
	 */
	int big5plus_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, bfchars = 1;
		long bffreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column;
		// Stage 1: Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			// System.err.println(rawtext[i]);
			if (rawtext[i] >= 128) {
				// asciichars++;
			} else {
				dbchars++;
				if (  0xA1 <= rawtext[i] && rawtext[i] <= 0xF9 && // Original Big5 range
					((0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) || 
					 (0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= 0xFE))) {
					bfchars++;
					totalfreq += 500;
					row = rawtext[i] - 0xA1;
					if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
						column = rawtext[i + 1] - 0x40;
					} else {
						column = rawtext[i + 1] - 0x61;
					}
					
					if (3 <= row && row <= 37) {
						bffreq += 436;
					} else {
						bffreq += 50;
					}
				} else if (0x81 <= rawtext[i] && rawtext[i] <= 0xFE && // Extended Big5 range
						 ((0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) || 
						  (0x80 <= rawtext[i + 1] && rawtext[i + 1] <= 0xFE)) ) {
					bfchars++;
					totalfreq += 500;
					row = rawtext[i] - 0x81;
					if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
						column = rawtext[i + 1] - 0x40;
					} else {
						column = rawtext[i + 1] - 0x40;
					}
					bffreq += 185;
				}
				i++;
			}
		}
		rangeval = 50 * ((float) bfchars / (float) dbchars);
		freqval = 50 * ((float) bffreq / (float) totalfreq);
		// For regular Big5 files, this would give the same score, so I handicap it slightly
		return (int) (rangeval + freqval) - 1;
	}

	/*
	 * Function: euc_tw_probability Argument: byte array Returns : number from 0
	 * to 100 representing probability text in array uses EUC-TW (CNS 11643) encoding
	 */
	int euc_tw_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, cnschars = 1;
		long cnsfreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column;
		// Check to see if characters fit into acceptable ranges
		// and have expected frequency of use
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			if (rawtext[i] >= 0) { // in ASCII range
				// asciichars++;
			} else { // high bit set
				dbchars++;
				if (i + 3 < rawtextlen
						&& (byte) 0x8E == rawtext[i] && (byte) 0xA1 <= rawtext[i + 1]
						&& rawtext[i + 1] <= (byte) 0xB0 && (byte) 0xA1 <= rawtext[i + 2]
						&& rawtext[i + 2] <= (byte) 0xFE && (byte) 0xA1 <= rawtext[i + 3]
						&& rawtext[i + 3] <= (byte) 0xFE) { // Planes 1 - 16
					cnschars++;
					// System.out.println("plane 2 or above CNS char");
					// These are all less frequent chars so just ignore freq
					i += 3;
				} else if ((byte) 0xA1 <= rawtext[i] 	 && rawtext[i] <= (byte) 0xFE && // Plane 1
						   (byte) 0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) {
					cnschars++;
					totalfreq += 500;
					row = rawtext[i] + 256 - 0xA1;
					column = rawtext[i + 1] + 256 - 0xA1;

					if (35 <= row && row <= 92) {
						cnsfreq += 435;
					} else {
						cnsfreq += 50;
					}
					i++;
				}
			}
		}
		rangeval = 50 * ((float) cnschars / (float) dbchars);
		freqval = 50 * ((float) cnsfreq / (float) totalfreq);
		return (int) (rangeval + freqval);
	}

	/*
	 * Function: iso_2022_cn_probability Argument: byte array Returns : number
	 * from 0 to 100 representing probability text in array uses ISO 2022-CN
	 * encoding WORKS FOR BASIC CASES, BUT STILL NEEDS MORE WORK
	 */
	int iso_2022_cn_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, isochars = 1;
		long isofreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column;
		// Check to see if characters fit into acceptable ranges
		// and have expected frequency of use
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			if (i + 3 < rawtextlen && rawtext[i] == (byte) 0x1B) { // Escape char ESC
				if (rawtext[i + 1] == (byte) 0x24 && 
					rawtext[i + 2] == 0x29 && 
					rawtext[i + 3] == (byte) 0x41) { // GB Escape $ ) A
					i += 4;
					while (rawtext[i] != (byte) 0x1B && i < rawtextlen - 1) {
						dbchars++;
						if ((0x21 <= rawtext[i]     && rawtext[i] <= 0x77) && 
							(0x21 <= rawtext[i + 1] && rawtext[i + 1] <= 0x77)) {
							isochars++;
							row = rawtext[i] - 0x21;
							column = rawtext[i + 1] - 0x21;
							totalfreq += 500;

							if (15 <= row && row < 55) {
								isofreq += 435;
							} else if (55 <= row && row < 87) {
								isofreq += 234;
							}
							i++;
						}
						i++;
					}
				} else if (rawtext[i + 1] == (byte) 0x24 && 
						   rawtext[i + 2] == (byte) 0x29 &&
						   rawtext[i + 3] == (byte) 0x47) { // CNS Escape $ ) G
					i += 4;
					while (rawtext[i] != (byte) 0x1B) {
						dbchars++;
						if ((byte) 0x21 <= rawtext[i] 	  && rawtext[i] <= (byte) 0x7E && 
							(byte) 0x21 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7E) {
							isochars++;
							totalfreq += 500;
							row = rawtext[i] - 0x21;
							column = rawtext[i + 1] - 0x21;
							if (35 <= row && row <= 92) {
								isofreq += 435;
							} else {
								isofreq += 150;
							}
							i++;
						}
						i++;
					}
				}
				if (rawtext[i] == (byte) 0x1B && i + 2 < rawtextlen && rawtext[i + 1] == (byte) 0x28
						&& rawtext[i + 2] == (byte) 0x42) { // ASCII:ESC ( B
					i += 2;
				}
			}
		}
		rangeval = 50 * ((float) isochars / (float) dbchars);
		freqval = 50 * ((float) isofreq / (float) totalfreq);

		return (int) (rangeval + freqval);
	}

	/*
	 * Function: utf8_probability Argument: byte array Returns : number from 0
	 * to 100 representing probability text in array uses UTF-8 encoding of Unicode
	 */
	int utf8_probability(byte[] rawtext) {
		int score = 0;
		int i, rawtextlen = 0;
		int goodbytes = 0, asciibytes = 0;
		// Maybe also use UTF8 Byte Order Mark: EF BB BF
		// Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen; i++) {
			if ((rawtext[i] & (byte) 0x7F) == rawtext[i]) { // One byte
				asciibytes++;
				// Ignore ASCII, can throw off count
			} else if (i + 1 < rawtextlen && 
					-64 <= rawtext[i] 	   && rawtext[i] <= -33 && // Two bytes
					-128 <= rawtext[i + 1] && rawtext[i + 1] <= -65) {
				goodbytes += 2;
				i++;
			} else if (i + 2 < rawtextlen && 
					-32  <= rawtext[i] && rawtext[i] <= -17 && // Three bytes
					-128 <= rawtext[i + 1] && rawtext[i + 1] <= -65 && 
					-128 <= rawtext[i + 2] && rawtext[i + 2] <= -65) {
				goodbytes += 3;
				i += 2;
			}
		}
		
		if (asciibytes == rawtextlen) {
			return 0;
		}
		score = (int) (100 * ((float) goodbytes / (float) (rawtextlen - asciibytes)));
		// System.out.println("rawtextlen " + rawtextlen + " goodbytes " +
		// goodbytes + " asciibytes " + asciibytes + " score " + score);
		// If not above 98, reduce to zero to prevent coincidental matches
		// Allows for some (few) bad formed sequences
		if (score > 98) {
			return score;
		} else if (score > 95 && goodbytes > 30) {
			return score;
		} else {
			return 0;
		}
	}

	/*
	 * Function: utf16_probability Argument: byte array Returns : number from 0
	 * to 100 representing probability text in array uses UTF-16 encoding of
	 * Unicode, guess based on BOM // NOT VERY GENERAL, NEEDS MUCH MORE WORK
	 */
	int utf16_probability(byte[] rawtext) {
		// int score = 0;
		// int i, rawtextlen = 0;
		// int goodbytes = 0, asciibytes = 0;
		if (rawtext.length > 1 && 
				((byte) 0xFE == rawtext[0] && (byte) 0xFF == rawtext[1]) || // Big-endian
				((byte) 0xFF == rawtext[0] && (byte) 0xFE == rawtext[1])) { // Little-endian
			return 100;
		}
		return 0;
		/*
		 * // Check to see if characters fit into acceptable ranges rawtextlen =
		 * rawtext.length; for (i = 0; i < rawtextlen; i++) { if ((rawtext[i] &
		 * (byte)0x7F) == rawtext[i]) { // One byte goodbytes += 1;
		 * asciibytes++; } else if ((rawtext[i] & (byte)0xDF) == rawtext[i]) {
		 * // Two bytes if (i+1 < rawtextlen && (rawtext[i+1] & (byte)0xBF) ==
		 * rawtext[i+1]) { goodbytes += 2; i++; } } else if ((rawtext[i] &
		 * (byte)0xEF) == rawtext[i]) { // Three bytes if (i+2 < rawtextlen &&
		 * (rawtext[i+1] & (byte)0xBF) == rawtext[i+1] && (rawtext[i+2] &
		 * (byte)0xBF) == rawtext[i+2]) { goodbytes += 3; i+=2; } } }
		 *
		 * score = (int)(100 * ((float)goodbytes/(float)rawtext.length)); // An
		 * all ASCII file is also a good UTF8 file, but I'd rather it // get
		 * identified as ASCII. Can delete following 3 lines otherwise if
		 * (goodbytes == asciibytes) { score = 0; } // If not above 90, reduce
		 * to zero to prevent coincidental matches if (score > 90) { return
		 * score; } else { return 0; }
		 */
	}

	/*
	 * Function: ascii_probability Argument: byte array Returns : number from 0
	 * to 100 representing probability text in array uses all ASCII Description:
	 * Sees if array has any characters not in ASCII range, if so, score is
	 * reduced
	 */
	int ascii_probability(byte[] rawtext) {
		int score = 75;
		int i, rawtextlen;
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen; i++) {
			if (rawtext[i] < 0) {
				score = score - 5;
			} else if (rawtext[i] == (byte) 0x1B) { // ESC (used by ISO 2022)
				score = score - 5;
			}
			if (score <= 0) {
				return 0;
			}
		}
		return score;
	}

	/*
	 * Function: euc_kr__probability Argument: pointer to byte array Returns :
	 * number from 0 to 100 representing probability text in array uses EUC-KR
	 * encoding
	 */
	int euc_kr_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, krchars = 1;
		long krfreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column;
		// Stage 1: Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			// System.err.println(rawtext[i]);
			if (rawtext[i] >= 0) {
				// asciichars++;
			} else {
				dbchars++;
				if ((byte) 0xA1 <= rawtext[i] 	  && rawtext[i] <= (byte) 0xFE && 
					(byte) 0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) {
					krchars++;
					totalfreq += 500;
					row = rawtext[i] + 256 - 0xA1;
					column = rawtext[i + 1] + 256 - 0xA1;

					if (row >= 15 && row < 40) {
						krfreq += 436;
					} else {
						krfreq += 50;
					}
				}
				i++;
			}
		}
		rangeval = 50 * ((float) krchars / (float) dbchars);
		freqval = 50 * ((float) krfreq / (float) totalfreq);
//		System.out.println("euc_kr_probability::gbchars=" + krchars + ";dbchars=" + dbchars + ";gbfreq=" + krfreq
//				+ ";totalfreq=" + totalfreq + ";total=" + (rangeval + freqval));
		return (int) (rangeval + freqval);
	}

	/*
	 * Function: cp949__probability Argument: pointer to byte array Returns :
	 * number from 0 to 100 representing probability text in array uses Cp949
	 * encoding
	 */
	int cp949_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, krchars = 1;
		long krfreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column;
		// Stage 1: Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			// System.err.println(rawtext[i]);
			if (rawtext[i] >= 0) {
				// asciichars++;
			} else {
				dbchars++;
				if ( (byte) 0x81 <= rawtext[i] && rawtext[i] <= (byte) 0xFE &&
					((byte) 0x41 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x5A || 
					 (byte) 0x61 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7A || 
					 (byte) 0x81 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE)) {
					krchars++;
					totalfreq += 500;
					if ((byte) 0xA1 <= rawtext[i] 	  && rawtext[i] <= (byte) 0xFE && 
						(byte) 0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) {
						row = rawtext[i] + 256 - 0xA1;
						column = rawtext[i + 1] + 256 - 0xA1;

						if (row >= 15 && row < 40) {
							krfreq += 436;
						} else {
							krfreq += 50;
						}
					}else{
						krfreq += 235;
					}
				}
				i++;
			}
		}
		rangeval = 50 * ((float) krchars / (float) dbchars);
		freqval = 50 * ((float) krfreq / (float) totalfreq);
		return (int) (rangeval + freqval);
	}

	int iso_2022_kr_probability(byte[] rawtext) {
		int i;
		for (i = 0; i < rawtext.length; i++) {
			if (i + 3 < rawtext.length && rawtext[i] == 0x1b && 
					(char) rawtext[i + 1] == '$' && 
					(char) rawtext[i + 2] == ')' && 
					(char) rawtext[i + 3] == 'C') {
				return 100;
			}
		}
		return 0;
	}

	/*
	 * Function: euc_jp_probability Argument: pointer to byte array Returns :
	 * number from 0 to 100 representing probability text in array uses EUC-JP
	 * encoding
	 */
	int euc_jp_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, jpchars = 1;
		long jpfreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column;
		// Stage 1: Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			// System.err.println(rawtext[i]);
			if (rawtext[i] >= 0) {
				// asciichars++;
			} else {
				dbchars++;
				if ((byte) 0xA1 <= rawtext[i] 	  && rawtext[i] <= (byte) 0xFE && 
					(byte) 0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) {
					jpchars++;
					totalfreq += 500;
					row = rawtext[i] + 256 - 0xA1;
					column = rawtext[i + 1] + 256 - 0xA1;

					if (3 <= row && row < 15) {
						jpfreq += 500;
					}else if (15 <= row && row < 47) {
						jpfreq += 435;
					}else{
						jpfreq += 50;
					}
				}
				i++;
			}
		}
		rangeval = 50 * ((float) jpchars / (float) dbchars);
		freqval = 50 * ((float) jpfreq / (float) totalfreq);
		return (int) (rangeval + freqval);
	}

	int iso_2022_jp_probability(byte[] rawtext) {
		int i;
		for (i = 0; i < rawtext.length; i++) {
			if (i + 2 < rawtext.length && rawtext[i] == 0x1b && 
					(char) rawtext[i + 1] == '$' && 
					(char) rawtext[i + 2] == 'B') {
				return 100;
			}
		}
		return 0;
	}

	/*
	 * Function: sjis_probability Argument: pointer to byte array Returns :
	 * number from 0 to 100 representing probability text in array uses
	 * Shift-JIS encoding
	 */
	int sjis_probability(byte[] rawtext) {
		int i, rawtextlen = 0;
		int dbchars = 1, jpchars = 1;
		long jpfreq = 0, totalfreq = 1;
		float rangeval = 0, freqval = 0;
		int row, column, adjust;
		// Stage 1: Check to see if characters fit into acceptable ranges
		rawtextlen = rawtext.length;
		for (i = 0; i < rawtextlen - 1; i++) {
			// System.err.println(rawtext[i]);
			if (rawtext[i] >= 0) {
				// asciichars++;
			} else {
				dbchars++;
				if (i + 1 < rawtext.length
						&& (((byte) 0x81 <= rawtext[i] && rawtext[i] <= (byte) 0x9F)
						 || ((byte) 0xE0 <= rawtext[i] && rawtext[i] <= (byte) 0xEF))
						&& (((byte) 0x40 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7E)
						 || ((byte) 0x80 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFC))) {
					jpchars++;
					totalfreq += 500;
					row = rawtext[i] + 256;
					column = rawtext[i + 1] + 256;
					if (column < 0x9f) {
						adjust = 1;
						if (column > 0x7f) {
							column -= 0x20;
						} else {
							column -= 0x19;
						}
					} else {
						adjust = 0;
						column -= 0x7e;
					}
					if (row < 0xa0) {
						row = ((row - 0x70) << 1) - adjust;
					} else {
						row = ((row - 0xb0) << 1) - adjust;
					}
					row -= 0x20;
					column = 0x20;
					
					if (3 <= row && row < 15) {
						jpfreq += 500;
					}else if (15 <= row && row < 47) {
						jpfreq += 435;
					}else{
						jpfreq += 50;
					}
					
					i++;
				} else if ((byte) 0xA1 <= rawtext[i] && rawtext[i] <= (byte) 0xDF) {
					// half-width katakana, convert to full-width
				}
			}
		}
		rangeval = 50 * ((float) jpchars / (float) dbchars);
		freqval = 50 * ((float) jpfreq / (float) totalfreq);

		return (int) (rangeval + freqval);
	}
	
	int utf16_escape_probability(byte[] rawtext) {
		int score = 0;
		int i = 0, rawtextlen = 0;
		int goodbytes = 0, asciibytes = 0;

		rawtextlen = rawtext.length;
		while (i < rawtextlen) {
			int ch = rawtext[i];
			if (ch == '+') { // + : map to ' '
				asciibytes++;
			} else if ('A' <= ch && ch <= 'Z') { // 'A'..'Z' : as it was
				asciibytes++;
			} else if ('a' <= ch && ch <= 'z') { // 'a'..'z' : as it was
				asciibytes++;
			} else if ('0' <= ch && ch <= '9') { // '0'..'9' : as it was
				asciibytes++;
			} else if (ch == '-' || ch == '_' // unreserved : as it was
					|| ch == '.' || ch == '!' || ch == '~' || ch == '*' || ch == '/' || ch == '(' || ch == ')') {
				asciibytes++;
			} else if (ch == '%' || ch == '\\') {
				if ('u' != rawtext[i + 1]) { // %XX : map to ascii(XX)
					i += 2;
					goodbytes += 3;
				} else {					 // %uXXXX : map to unicode(XXXX)
					i += 5;
					goodbytes += 6;
				}
			}
			i++;
		}

		score = (int) (100 * ((float) goodbytes / (float) rawtext.length));
		if (goodbytes == asciibytes) {
			score = 0;
		}
		if (goodbytes + asciibytes == rawtextlen) {
			return 100;
		}
		return score;
	}
}

class Encoding {
	// Supported Encoding Types
	public static int GB2312 = 0;
	public static int GBK = 1;
	public static int GB18030 = 2;
	public static int HZ = 3;
	public static int BIG5 = 4;
	public static int CNS11643 = 5;
	public static int UTF8 = 6;
	public static int UTF8T = 7;
	public static int UTF8S = 8;
	public static int UNICODE = 9;
	public static int UNICODET = 10;
	public static int UNICODE_ESCAPE = 11;
	public static int ISO2022CN = 12;
	public static int ISO2022CN_CNS = 13;
	public static int ISO2022CN_GB = 14;
	public static int EUC_KR = 15;
	public static int CP949 = 16;
	public static int ISO2022KR = 17;
	public static int JOHAB = 18;
	public static int SJIS = 19;
	public static int EUC_JP = 20;
	public static int ISO2022JP = 21;
	public static int ASCII = 22;
	public static int OTHER = 23;
	public static int TOTALTYPES = 24;
	public final static int SIMP = 0;
	public final static int TRAD = 1;

	// Names of the encodings for human viewing
	public static String[] nicename;

	// Constructor
	public Encoding() {
		nicename = new String[TOTALTYPES];
		// Assign Human readable names
		nicename[GB2312] = "GB2312";
		nicename[GBK] = "GBK";
		nicename[GB18030] = "GB18030";
		nicename[HZ] = "HZ";
		nicename[ISO2022CN_GB] = "ISO2022CN-GB";
		nicename[BIG5] = "Big5";
		nicename[CNS11643] = "CNS11643";
		nicename[ISO2022CN_CNS] = "ISO2022CN-CNS";
		nicename[ISO2022CN] = "ISO2022CN";
		nicename[UTF8] = "UTF-8";
		nicename[UTF8T] = "UTF-8";
		nicename[UTF8S] = "UTF-8";
		nicename[UNICODE] = "Unicode";
		nicename[UNICODET] = "Unicode";
		nicename[UNICODE_ESCAPE] = "UnicodeEscape";
		nicename[EUC_KR] = "EUC-KR";
		nicename[CP949] = "CP949";
		nicename[ISO2022KR] = "ISO2022KR";
		nicename[JOHAB] = "Johab";
		nicename[SJIS] = "SJIS";
		nicename[EUC_JP] = "EUC-JP";
		nicename[ISO2022JP] = "ISO2022JP";
		nicename[ASCII] = "ASCII";
		nicename[OTHER] = "OTHER";
	}

}