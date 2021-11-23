package com.scudata.ide.vdb.commonvdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Console {
	private int MAX_LINES = 1000;

	class ConsoleOutputStream extends OutputStream {
		final byte LF = (byte) '\n';
		ByteArrayOutputStream baos = new ByteArrayOutputStream(17);
		// 此输出流不存在半个汉字问题
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
			// if (jta.getLineCount() > MAX_LINES) {
			// try {
			// int rowEnd = jta.getLineEndOffset(0);
			// jta.getDocument().remove(0, rowEnd);
			// }
			// catch (Exception x) {
			// }
			// }
			jta.append(baos.toString());
			baos.reset();
		}
	}

	public Console(final JTextArea jta) {
		jta.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (jta.getLineCount() >= MAX_LINES) {
							int end = 0;
							try {
								end = jta.getLineEndOffset(100);
							} catch (Exception e) {
							}
							jta.replaceRange("", 0, end);
						}
						jta.setCaretPosition(jta.getText().length());
					}
				});
			}

			public void removeUpdate(DocumentEvent evt) {
			}

			public void changedUpdate(DocumentEvent evt) {
			}
		});
		ConsoleOutputStream cos = new ConsoleOutputStream(jta);
		PrintStream ps = new PrintStream(cos);
		System.setOut(ps);
		System.setErr(ps);
		// Logger.resetConsole();
	}
}
