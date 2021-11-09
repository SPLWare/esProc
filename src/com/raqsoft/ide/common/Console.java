package com.raqsoft.ide.common;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JTextArea;

import com.raqsoft.common.LimitedQueue;

/**
 * The console of IDE
 *
 */
public class Console {
	/**
	 * Maximum number of console lines
	 */
	private int MAX_LINES = 1000;
	private JTextArea jta = null;
	private PrintStream ps;
	private Object rowLock = new Object();
	private LimitedQueue rowBuffers = new LimitedQueue(MAX_LINES);
	private ArrayList<InputStreamFlusher> flushers = new ArrayList<InputStreamFlusher>();

	/**
	 * Constructor
	 * 
	 * @param jta
	 *            JTextArea component
	 */
	public Console(final JTextArea jta) {
		this(jta, null);
	}

	/**
	 * Constructor
	 * 
	 * @param jta
	 *            JTextArea component
	 * @param is
	 *            Input streams
	 */
	public Console(final JTextArea jta, InputStream[] is) {
		this.jta = jta;
		ConsoleOutputStream cos = new ConsoleOutputStream(jta);
		ps = new PrintStream(cos);
		if (is == null) {
			System.setOut(ps);
			System.setErr(ps);
		} else {
			setInputStreams(is);
		}
		/*
		 * The previous version generated too many trigger threads, and the
		 * speed was too slow when the output continued. Now the messages are
		 * all queued in the queue, and the messages in the queue are taken
		 * every second and displayed to the control.
		 */
		Timer timer = new Timer();
		TimerTask tt = new TimerTask() {
			public void run() {
				synchronized (rowLock) {
					if (!rowBuffers.isChanged())
						return;
					jta.setText(null);
					int rows = rowBuffers.size();
					for (int r = 0; r < rows; r++) {
						jta.append((String) rowBuffers.get(r));
					}
					int len = jta.getText().length();
					jta.setCaretPosition(len);
					rowBuffers.setUnChanged();
				}
			}
		};

		timer.scheduleAtFixedRate(tt, 1000, 1000);
	}

	/**
	 * Clear
	 */
	public void clear() {
		jta.setText(null);
		rowBuffers.clear();
	}

	/**
	 * Set input streams
	 * 
	 * @param is
	 */
	public void setInputStreams(InputStream[] is) {
		clearFlusher();

		int len = is.length;
		for (int i = 0; i < len; i++) {
			InputStreamFlusher isFlush = new InputStreamFlusher(is[i]);
			flushers.add(isFlush);
			isFlush.start();
		}

	}

	/**
	 * Get the JTextArea component
	 * 
	 * @return
	 */
	public JTextArea getTextArea() {
		return jta;
	}

	/**
	 * Clear flushers
	 */
	private void clearFlusher() {
		for (InputStreamFlusher flusher : flushers) {
			flusher.shutDown();
		}
		flushers.clear();
	}

	/**
	 * Console output stream
	 *
	 */
	class ConsoleOutputStream extends OutputStream {
		byte LF = (byte) '\n';
		ByteArrayOutputStream baos = new ByteArrayOutputStream(17);
		JTextArea jta = null;

		public ConsoleOutputStream(JTextArea jta) {
			this.jta = jta;
		}

		public void write(int b) throws IOException {
			baos.write(b);
			if (b == LF) {
				flush();
			}
		}

		public void flush() throws IOException {
			String output = baos.toString();
			baos.reset();
			String sysReturn = System.getProperty("line.separator", "\n");
			if (("Yo" + sysReturn).equals(output))
				return;

			synchronized (rowLock) {
				rowBuffers.add(output);
			}
		}
	}

	/**
	 * Console input stream
	 *
	 */
	class InputStreamFlusher extends Thread {
		InputStream is;
		boolean stop = false;

		public InputStreamFlusher(InputStream is) {
			this.is = is;
		}

		public void shutDown() {
			stop = true;
		}

		public void run() {
			BufferedReader br1 = new BufferedReader(new InputStreamReader(is));
			try {
				String line1 = null;
				while ((line1 = br1.readLine()) != null && !stop) {
					if (line1 != null) {
						ps.println(line1);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
