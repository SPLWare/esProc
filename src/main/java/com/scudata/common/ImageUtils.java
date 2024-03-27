package com.scudata.common;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.awt.geom.*;
import javax.swing.*;

import com.scudata.cellset.BackGraphConfig;

import javax.imageio.*;

public class ImageUtils
{

	public static boolean hasAlpha(Image image) {
		if (image instanceof BufferedImage) {
			BufferedImage bimage = (BufferedImage)image;
			return bimage.getColorModel().hasAlpha();
		}

		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try {
			pg.grabPixels();
		} catch (InterruptedException e) {
		}
		ColorModel cm = pg.getColorModel();
		return cm.hasAlpha();
	}

	public static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage)image;
		}

		//确保图像的所有像素装载
		image = new ImageIcon(image).getImage();

		//检查是否有透明
		boolean hasAlpha = hasAlpha(image);
		int type = BufferedImage.TYPE_INT_RGB;
		if (hasAlpha) {
			type = BufferedImage.TYPE_INT_ARGB;
		}

		BufferedImage bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		Graphics g = bimage.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return bimage;
	}

	public static Image cutImage(Image image, int cutW, int cutH) {
		image = new ImageIcon(image).getImage();

		//检查是否有透明
		boolean hasAlpha = hasAlpha(image);
		int type = BufferedImage.TYPE_INT_RGB;
		if (hasAlpha) {
			type = BufferedImage.TYPE_INT_ARGB;
		}

		BufferedImage bimage = new BufferedImage(cutW, cutH, type);
		Graphics g = bimage.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return bimage;
	}
	
	public static void drawFixedImage(Graphics g,Image srcImg,int x, int y,int fixW, int fixH){
		drawFixedImage(g,srcImg,BackGraphConfig.MODE_NONE,x,y,fixW,fixH);
	}
/**
 * 给定固定位置和宽高，画srcImg，超出部分裁掉
 * @param g
 * @param srcImg
 * @param x
 * @param y
 * @param fixW
 * @param fixH
 * @param scale
 */
	public static void drawFixedImage(Graphics g,Image srcImg,byte mode,int x, int y,int fixW, int fixH){
		Shape oldClip = g.getClip();
		switch(mode){
		case BackGraphConfig.MODE_FILL:
			g.drawImage(srcImg, x, y, fixW, fixH, null);
			break;
		case BackGraphConfig.MODE_NONE:
			drawFixImage(g, srcImg, x, y, x+fixW, y+fixH);
			break;
		case BackGraphConfig.MODE_TILE:
			int x1 = x, y1=y;
			int x2 = x+fixW, y2 = y+fixH;
			int iw = srcImg.getWidth(null);
			int ih = srcImg.getHeight(null);
			while (x1 < x2) {
				y1 = y;
				while (y1 < y2) {
					int clipx = Math.max(x, x1);
					int clipy = Math.max(y, y1);
					g.setClip(clipx, clipy, Math.min(iw, x2 - clipx),
							Math.min(ih, y2 - clipy));
					g.drawImage(srcImg, x1, y1, iw, ih, null);
					y1 += ih;
				}
				x1 += iw;
			}
			break;
		}
		g.setClip(oldClip);

	}
	
	/**
	 * 绘制固定图形，超出边界的图形都会截掉
	 * @param g
	 * @param img
	 * @param x
	 * @param y
	 * @param sideRight，右边界
	 * @param sideBottom，下边界
	 * @return 正常绘制返回true，越界后，返回false
	 */
	public static boolean drawFixImage(Graphics g,Image img, int x, int y, int sideRight, int sideBottom){
		if(x>sideRight){
			return false;
		}
		if(y>sideBottom){
			return false;
		}
		boolean needCut = false;
		int width = img.getWidth(null);
		int height = img.getHeight(null);
		
		int reserveW = width;
		if(x+width>sideRight){
			reserveW = sideRight-x;
			needCut = true;
		}
		int reserveH = height;
		if(y+height>sideBottom){
			reserveH = sideBottom-y;
			needCut = true;
		}
		if(needCut){
			img = cutImage(img, reserveW, reserveH);
		}
		g.drawImage(img, x, y, reserveW, reserveH, null);
		return true;
	}
	
	public static BufferedImage toBufferedImage(RenderedImage image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage)image;
		}

		//检查是否有透明
		boolean hasAlpha = image.getColorModel().hasAlpha();
		int type = BufferedImage.TYPE_INT_RGB;
		if (hasAlpha) {
			type = BufferedImage.TYPE_INT_ARGB;
		}

		BufferedImage bimage = new BufferedImage(image.getWidth(), image.getHeight(), type);
		Graphics2D g = bimage.createGraphics();
		g.drawRenderedImage(image, new AffineTransform());
		g.dispose();

		return bimage;
	}

	public static void writeGIF( RenderedImage img, OutputStream out ) throws IOException {
		ImageIO.write( img, "gif", out );
	}
	
	
	//REPORT-107  added by hhw2013.9.24 斜线导出空白
	public static void writeGIF( Image img, OutputStream out ) throws IOException {
		GifEncoder enc = new GifEncoder( img, out );
		enc.encode();
	}
	public static byte[] writeGIF( Image img ) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream( 2048 );
		writeGIF( img, out );
		return out.toByteArray();
	}
/*
	public static byte[] writeGIF( RenderedImage img ) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream( 2048 );
		writeGIF( img, out );
		return out.toByteArray();
	}
*/
	
	public static void writePNG( RenderedImage img, OutputStream out ) throws IOException {
		ImageIO.write( img, "png", out );
	}

	public static byte[] writePNG( RenderedImage img ) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream( 2048 );
		writePNG( img, out );
		return out.toByteArray();
	}

	public static void writeJPEG( RenderedImage img, OutputStream out ) throws IOException {
		ImageIO.write( img, "jpeg", out );
	}

	public static byte[] writeJPEG( RenderedImage img ) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream( 2048 );
		writeJPEG( img, out );
		return out.toByteArray();
	}

	public static void writeBMP( RenderedImage img, OutputStream out ) throws IOException {
		ImageIO.write( img, "bmp", out );
	}

	public static byte[] writeBMP( RenderedImage img ) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream( 2048 );
		writeBMP( img, out );
		return out.toByteArray();
	}

	/*public static void main(String[] args) throws Exception {
		//Image i1 = new ImageIcon("d:\\a.gif").getImage();

		ParameterBlock pb = new ParameterBlock().add("d:\\a.gif");
		PlanarImage i1 = JAI.create( "fileload", pb );
		//PNGEncodeParam param = PNGEncodeParam.getDefaultEncodeParam( i1 );
		//System.out.println( param.getBitDepth() );
		//int[] dims = param.getPhysicalDimension();
		//for( int i = 0; i <  dims.length; i ++ )
		//	System.out.println( dims[i] );
		OutputStream out = new FileOutputStream( "d:\\a.jpg" );
		writeJPEG( i1, out );
		out.close();

	}*/
}
