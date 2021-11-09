package com.raqsoft.chart;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class Ball {
	public int bsize;
	public float x, y;
	public float Vx = 0.1f;
	public float Vy = 0.05f;
	public int nImgs = 5;
	public BufferedImage imgs[];
	public int index = (int) (Math.random() * (nImgs - 1));

	private final float inelasticity = .96f;
	private final float Ax = 0.0f;
	private final float Ay = 0.0002f;
	private final float Ar = 0.9f;
	private final int UP = 0;
	private final int DOWN = 1;
	private int indexDirection = UP;
	private boolean collision_x, collision_y;
	private float jitter;
	private Color color;
	private boolean isSelected;

	public Ball(Color color, int bsize) {
		this.color = color;
		makeImages(bsize);
	}

	public void makeImages(int bsize) {
		//半径不能太大，太大时，会造成maxr越出数组255位
//		if( bsize>230 ){//固定半径的球光照效果更好，填充都会用该图片去压缩或者拉伸
			bsize = 230;
//		}
		this.bsize = bsize * 2;
		int R = bsize;
		byte[] data = new byte[R * 2 * R * 2];
		int maxr = 0;
		for (int Y = 2 * R; --Y >= 0;) {
			int x0 = (int) (Math.sqrt(R * R - (Y - R) * (Y - R)) + 0.5);
			int p = Y * (R * 2) + R - x0;
			for (int X = -x0; X < x0; X++) {
				int x = X + 15;
				int y = Y - R + 15;
				int r = (int) (Math.sqrt(x * x + y * y) + 0.5);
				if (r > maxr) {
					maxr = r;
				}
				data[p++] = r <= 0 ? 1 : (byte) r;
			}
		}

		imgs = new BufferedImage[nImgs];

		int bg = 255;
		int count = 256;
		byte red[] = new byte[count];
		red[0] = (byte) bg;
		byte green[] = new byte[count];
		green[0] = (byte) bg;
		byte blue[] = new byte[count];
		blue[0] = (byte) bg;

		for (int r = 0; r < imgs.length; r++) {
			float b = 0.5f + (float) ((r + 1f) / imgs.length / 2f);
			for (int i = maxr; i >= 1; --i) {
				float d = (float) i / maxr;
				int index = i;//(int) (d * 255);
				red[index] = (byte) blend(blend(color.getRed(), 255, d), bg, b);
				green[index] = (byte) blend(blend(color.getGreen(), 255, d),
						bg, b);
				blue[index] = (byte) blend(blend(color.getBlue(), 255, d), bg,
						b);
			}
			if (maxr > 255)
				maxr = 255;
			IndexColorModel icm = new IndexColorModel(8, maxr + 1, red, green,
					blue, 0);
			DataBufferByte dbb = new DataBufferByte(data, data.length);
			int bandOffsets[] = { 0 };
			WritableRaster wr = Raster.createInterleavedRaster(dbb, R * 2,
					R * 2, R * 2, 1, bandOffsets, null);
			imgs[r] = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(),
					null);
		}
	}

	private final int blend(int fg, int bg, float fgfactor) {
		return (int) (bg + (fg - bg) * fgfactor);
	}

	public void step(long deltaT, int w, int h) {
		collision_x = false;
		collision_y = false;

		jitter = (float) Math.random() * .01f - .005f;

		x += Vx * deltaT + (Ax / 2.0) * deltaT * deltaT;
		y += Vy * deltaT + (Ay / 2.0) * deltaT * deltaT;
		if (x <= 0.0f) {
			x = 0.0f;
			Vx = -Vx * inelasticity + jitter;
			collision_x = true;
		}
		if (x + bsize >= w) {
			x = w - bsize;
			Vx = -Vx * inelasticity + jitter;
			collision_x = true;
		}
		if (y <= 0) {
			y = 0;
			Vy = -Vy * inelasticity + jitter;
			collision_y = true;
		}
		if (y + bsize >= h) {
			y = h - bsize;
			Vx *= inelasticity;
			Vy = -Vy * inelasticity + jitter;
			collision_y = true;
		}
		Vy = Vy + Ay * deltaT;
		Vx = Vx + Ax * deltaT;

		if (indexDirection == UP) {
			index++;
		}
		if (indexDirection == DOWN) {
			--index;
		}
		if (index + 1 == nImgs) {
			indexDirection = DOWN;
		}
		if (index == 0) {
			indexDirection = UP;
		}
	}
} // End class Ball
