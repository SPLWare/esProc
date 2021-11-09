package com.raqsoft.ide.common.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;
import java.util.Date;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.raqsoft.app.common.Section;
import com.raqsoft.common.MessageManager;
import com.raqsoft.ide.common.resources.IdeCommonMessage;

/**
 * 日期选择器
 *
 */
public class DateChooser extends javax.swing.JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 日历的实例
	 */
	private Calendar calendar;
	/**
	 * 选择的日历
	 */
	private Calendar retCal;

	/**
	 * 构造函数
	 */
	public DateChooser() {
		super();
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 */
	public DateChooser(Dialog owner) {
		super(owner);
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 * @param modal
	 *            是否模态的
	 */
	public DateChooser(Dialog owner, boolean modal) {
		super(owner, modal);
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 * @param title
	 *            标题
	 */
	public DateChooser(Dialog owner, String title) {
		super(owner, title);
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 * @param title
	 *            标题
	 * @param modal
	 *            是否模态的
	 */
	public DateChooser(Dialog owner, String title, boolean modal) {
		super(owner, title, modal);
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 * @param title
	 *            标题
	 * @param modal
	 *            是否模态的
	 * @param gc
	 *            GraphicsConfiguration
	 */
	public DateChooser(Dialog owner, String title, boolean modal,
			GraphicsConfiguration gc) {
		super(owner, title, modal, gc);
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 */
	public DateChooser(Frame owner) {
		super(owner);
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 * @param modal
	 *            是否模态的
	 */
	public DateChooser(Frame owner, boolean modal) {
		super(owner, modal);
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 * @param title
	 *            标题
	 */
	public DateChooser(Frame owner, String title) {
		super(owner, title);
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 * @param title
	 *            标题
	 * @param modal
	 *            是否模态的
	 */
	public DateChooser(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
		init();
	}

	/**
	 * 构造函数
	 * 
	 * @param owner
	 *            父组件
	 * @param title
	 *            标题
	 * @param modal
	 *            是否模态的
	 * @param gc
	 *            GraphicsConfiguration
	 */
	public DateChooser(Frame owner, String title, boolean modal,
			GraphicsConfiguration gc) {
		super(owner, title, modal, gc);
		init();
	}

	/**
	 * 初始化日期
	 * 
	 * @param calendar
	 */
	public void initDate(Calendar calendar) {
		int m = calendar.get(Calendar.MONTH);
		jComboBox1.setSelectedIndex(m);
		jSpinner1.setValue(calendar.getTime());
		monthCalendar.setYearMonth(calendar);
	}

	/**
	 * 初始化
	 */
	private void init() {
		initComponents();
		// init date
		calendar = Calendar.getInstance();
		initDate(calendar);
	}

	/**
	 * 初始化控件
	 */
	private void initComponents() {
		JPanel basePanel = new JPanel();
		JPanel containerPanel = new JPanel();
		JPanel controlPanel = new JPanel();
		jComboBox1 = new JComboBox();
		jSpinner1 = new JSpinner();
		JPanel viewPanel = new JPanel();
		monthCalendar = new MonthlyCalendar() {
			private static final long serialVersionUID = 1L;

			protected void dateSelected() {
				doSelection();
			}
		};

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle(mm.getMessage("datechooser.selectdate"));
		setResizable(false);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				formWindowClosing(evt);
			}
		});

		basePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

		basePanel.setBackground(new Color(252, 250, 252));
		containerPanel.setLayout(new BorderLayout());

		containerPanel.setBorder(new TitledBorder(mm
				.getMessage("datechooser.date")));
		containerPanel.setOpaque(false);
		controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 0));

		controlPanel.setBorder(new EmptyBorder(new Insets(5, 0, 10, 0)));
		controlPanel.setOpaque(false);
		jComboBox1.setMaximumRowCount(7);
		jComboBox1.setModel(new DefaultComboBoxModel(new Section(mm
				.getMessage("datechooser.month")).toStringArray()));
		jComboBox1.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent evt) {
				jComboBox1ItemStateChanged(evt);
			}
		});

		controlPanel.add(jComboBox1);

		jSpinner1.setModel(new SpinnerDateModel());
		jSpinner1.setEditor(new JSpinner.DateEditor(jSpinner1, "yyyy"));
		jSpinner1.setPreferredSize(jComboBox1.getPreferredSize());
		jSpinner1.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				jSpinner1StateChanged(evt);
			}
		});

		controlPanel.add(jSpinner1);

		containerPanel.add(controlPanel, BorderLayout.NORTH);

		viewPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

		viewPanel.setBorder(new EmptyBorder(new java.awt.Insets(0, 0, 10, 0)));
		viewPanel.setOpaque(false);
		monthCalendar.setBorder(new BevelBorder(BevelBorder.LOWERED));
		viewPanel.add(monthCalendar);

		containerPanel.add(viewPanel, BorderLayout.CENTER);

		basePanel.add(containerPanel);

		getContentPane().add(basePanel, BorderLayout.CENTER);

		pack();
	}

	/**
	 * 日期值发生变化
	 * 
	 * @param evt
	 */
	private void jSpinner1StateChanged(ChangeEvent evt) {
		Date date = (Date) jSpinner1.getValue();
		calendar.setTime(date);
		monthCalendar.setYear(calendar.get(Calendar.YEAR));
		monthCalendar.repaint();
	}

	/**
	 * 下拉月份变化
	 * 
	 * @param evt
	 */
	private void jComboBox1ItemStateChanged(ItemEvent evt) {
		if (evt.getStateChange() == ItemEvent.DESELECTED) {
			return;
		}
		monthCalendar.setMonth(jComboBox1.getSelectedIndex() + 1);
		monthCalendar.repaint();
	}

	/**
	 * 窗口关闭事件
	 * 
	 * @param evt
	 */
	private void formWindowClosing(WindowEvent evt) {
		retCal = null;
	}

	/**
	 * Override parent setVisible method to include resetting of highlighted
	 * date
	 */
	public void setVisible(boolean b) {
		monthCalendar.resetHighlight();
		super.setVisible(b);
	}

	/** Contains handling code for date selection event */
	private void doSelection() {
		retCal = monthCalendar.getSelectedDate();
		super.setVisible(false);
		dispose();
	}

	/**
	 * User code can call this method to get the selected date as a Calendar
	 * object. If the DateChooser dialog is closed by clicking the "Close"
	 * button, this method returns null; Else, the DateChooser dialog is closed
	 * by selecting a date, and the method will return a Calendar object
	 * representing that date.
	 */
	public Calendar getSelectedDate() {
		return retCal;
	}

	/**
	 * Set the calendar year and month to the specified value. Should be called
	 * before set the date chooser visible.
	 */
	public void setYearMonth(int y, int m) {
		if (y < 1970) {
			throw new IllegalArgumentException(
					mm.getMessage("datechooser.err1"));
		}
		if (m < 1 || m > 12) {
			throw new IllegalArgumentException(
					mm.getMessage("datechooser.err2"));
		}
		jComboBox1.setSelectedIndex(m - 1);
		calendar.set(Calendar.YEAR, y);
		jSpinner1.setValue(calendar.getTime());
		monthCalendar.setYearMonth(y, m);
	}

	/**
	 * Bean method to set start day of a week. Will delegate to
	 * MonthlyCalendar.setWeekStartOnSunday(boolean) method.
	 */
	public void setWeekStartOnSunday(boolean b) {
		monthCalendar.setWeekStartOnSunday(b);
	}

	/**
	 * Bean method to get start day of a week. Will delegate to
	 * MonthlyCalendar.getWeekStartOnSunday method.
	 */
	public boolean getWeekStartOnSunday() {
		return monthCalendar.getWeekStartOnSunday();
	}

	/**
	 * 月份下拉框
	 */
	private JComboBox jComboBox1;
	/**
	 * 日期时间值
	 */
	private JSpinner jSpinner1;
	/**
	 * Monthly Calendar organized in weeks
	 */
	private MonthlyCalendar monthCalendar;

}
