package com.scudata.ide.spl.dialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.scudata.ide.common.GM;
import com.scudata.ide.common.dialog.RQDialog;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.ide.spl.GCSpl;
import com.scudata.ide.spl.resources.IdeSplMessage;

public class DialogZoom extends RQDialog {
	private static final long serialVersionUID = 1L;

	public DialogZoom() {
		super(IdeSplMessage.get().getMessage("dialogscale.title"), 200, 250);
		try {
			initUI();
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	public void setScale(float scale) {
		scale *= 100;
		if (Float.compare(scale, 200) == 0) {
			jRB200.setSelected(true);
		} else if (Float.compare(scale, 150) == 0) {
			jRB150.setSelected(true);
		} else if (Float.compare(scale, 100) == 0) {
			jRB100.setSelected(true);
		} else if (Float.compare(scale, 75) == 0) {
			jRB75.setSelected(true);
		} else if (Float.compare(scale, 50) == 0) {
			jRB50.setSelected(true);
		} else {
			jRBCustom.setSelected(true);
		}
		int percent = (int) scale;
		jSCustom.setValue(percent);
	}

	public float getScale() {
		int percent;
		if (jRB200.isSelected()) {
			percent = 200;
		} else if (jRB150.isSelected()) {
			percent = 150;
		} else if (jRB100.isSelected()) {
			percent = 100;
		} else if (jRB75.isSelected()) {
			percent = 75;
		} else if (jRB50.isSelected()) {
			percent = 50;
		} else {
			percent = ((Number) jSCustom.getValue()).intValue();
		}
		return new Float(percent) / 100f;
	}

	private void initUI() {
		panelCenter.setLayout(new VFlowLayout(VFlowLayout.CENTER));

		JPanel jPCustom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		jPCustom.add(jRBCustom);
		jPCustom.add(jSCustom);
		jPCustom.add(new JLabel("%"));

		JPanel panelScale = new JPanel(new VFlowLayout(VFlowLayout.LEFT));
		panelScale.add(jRB200);
		panelScale.add(jRB150);
		panelScale.add(jRB100);
		panelScale.add(jRB75);
		panelScale.add(jRB50);
		panelScale.add(jPCustom);
		panelCenter.add(panelScale);

		ButtonGroup bg1 = new ButtonGroup();
		bg1.add(jRB200);
		bg1.add(jRB150);
		bg1.add(jRB100);
		bg1.add(jRB75);
		bg1.add(jRB50);
		bg1.add(jRBCustom);

		jRB200.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				jSCustom.setValue(200);
			}

		});
		jRB150.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				jSCustom.setValue(150);
			}

		});
		jRB100.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				jSCustom.setValue(100);
			}

		});
		jRB75.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				jSCustom.setValue(75);
			}

		});
		jRB50.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				jSCustom.setValue(50);
			}

		});
	}

	private JRadioButton jRB200 = new JRadioButton("200%");
	private JRadioButton jRB150 = new JRadioButton("150%");
	private JRadioButton jRB100 = new JRadioButton("100%");
	private JRadioButton jRB75 = new JRadioButton("75%");
	private JRadioButton jRB50 = new JRadioButton("50%");
	private JRadioButton jRBCustom = new JRadioButton(IdeSplMessage.get()
			.getMessage("dialogscale.custom"));
	private JSpinner jSCustom = new JSpinner(new SpinnerNumberModel(100,
			GCSpl.DEFAULT_SCALES[0],
			GCSpl.DEFAULT_SCALES[GCSpl.DEFAULT_SCALES.length - 1], 1));
}
