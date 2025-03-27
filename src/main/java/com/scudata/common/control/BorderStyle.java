package com.scudata.common.control;

import java.awt.Color;

import com.scudata.cellset.IStyle;

/**
 * Border style
 */
public class BorderStyle {
	/**
	 * Width of the left border
	 */
	float LBWidth = 0.75f;

	/**
	 * Style of the left border
	 */
	byte LBStyle = IStyle.LINE_NONE;

	/**
	 * Color of the left border
	 */
	int LBColor = Color.lightGray.getRGB();
	/**
	 * Width of the right border
	 */
	float RBWidth = 0.75f;
	/**
	 * Style of the right border
	 */
	byte RBStyle = IStyle.LINE_NONE;
	/**
	 * Color of the right border
	 */
	int RBColor = Color.lightGray.getRGB();
	/**
	 * Width of the top border
	 */
	float TBWidth = 0.75f;
	/**
	 * Style of the top border
	 */
	byte TBStyle = IStyle.LINE_NONE;
	/**
	 * Color of the top border
	 */
	int TBColor = Color.lightGray.getRGB();
	/**
	 * Width of the bottom border
	 */
	float BBWidth = 0.75f;
	/**
	 * Style of the bottom border
	 */
	byte BBStyle = IStyle.LINE_NONE;
	/**
	 * Color of the bottom border
	 */
	int BBColor = Color.lightGray.getRGB();

	/**
	 * Constructor
	 */
	public BorderStyle() {
	}

	/**
	 * Get the color of the bottom border
	 * 
	 * @return
	 */
	public int getBBColor() {
		return BBColor;
	}

	/**
	 * Set the color of the bottom border
	 * 
	 * @param color
	 */
	public void setBBColor(int color) {
		BBColor = color;
	}

	/**
	 * Get the Style of the bottom border
	 * 
	 * @return
	 */
	public byte getBBStyle() {
		return BBStyle;
	}

	/**
	 * Set the Style of the bottom border
	 * 
	 * @param style
	 */
	public void setBBStyle(byte style) {
		BBStyle = style;
	}

	/**
	 * Get the Width of the bottom border
	 * 
	 * @return
	 */
	public float getBBWidth() {
		return BBWidth;
	}

	/**
	 * Set the Width of the bottom border
	 * 
	 * @param width
	 */
	public void setBBWidth(float width) {
		BBWidth = width;
	}

	/**
	 * Get the color of the left border
	 * 
	 * @param width
	 */
	public int getLBColor() {
		return LBColor;
	}

	/**
	 * Set the color of the left border
	 * 
	 * @param color
	 */
	public void setLBColor(int color) {
		LBColor = color;
	}

	/**
	 * Get the Style of the left border
	 * 
	 * @return
	 */
	public byte getLBStyle() {
		return LBStyle;
	}

	/**
	 * Set the Style of the left border
	 * 
	 * @param style
	 */
	public void setLBStyle(byte style) {
		LBStyle = style;
	}

	/**
	 * Get the Width of the left border
	 * 
	 * @return
	 */
	public float getLBWidth() {
		return LBWidth;
	}

	/**
	 * Set the Width of the left border
	 * 
	 * @param width
	 */
	public void setLBWidth(float width) {
		LBWidth = width;
	}

	/**
	 * Get the Color of the right border
	 * 
	 * @return
	 */
	public int getRBColor() {
		return RBColor;
	}

	/**
	 * Set the Color of the right border
	 * 
	 * @param color
	 */
	public void setRBColor(int color) {
		RBColor = color;
	}

	/**
	 * Get the Style of the right border
	 * 
	 * @return
	 */
	public byte getRBStyle() {
		return RBStyle;
	}

	/**
	 * Set the Style of the right border
	 * 
	 * @param style
	 */
	public void setRBStyle(byte style) {
		RBStyle = style;
	}

	/**
	 * Get the Width of the right border
	 * 
	 * @return
	 */
	public float getRBWidth() {
		return RBWidth;
	}

	/**
	 * Set the Width of the right border
	 * 
	 * @param width
	 */
	public void setRBWidth(float width) {
		RBWidth = width;
	}

	/**
	 * Get the Color of the top border
	 * 
	 * @return
	 */
	public int getTBColor() {
		return TBColor;
	}

	/**
	 * Set the Color of the top border
	 * 
	 * @param color
	 */
	public void setTBColor(int color) {
		TBColor = color;
	}

	/**
	 * Get the Style of the top border
	 * 
	 * @return
	 */
	public byte getTBStyle() {
		return TBStyle;
	}

	/**
	 * Set the Style of the top border
	 * 
	 * @param style
	 */
	public void setTBStyle(byte style) {
		TBStyle = style;
	}

	/**
	 * Get the Width of the top border
	 * 
	 * @return
	 */
	public float getTBWidth() {
		return TBWidth;
	}

	/**
	 * Set the Width of the top border
	 * 
	 * @param width
	 */
	public void setTBWidth(float width) {
		TBWidth = width;
	}
}
