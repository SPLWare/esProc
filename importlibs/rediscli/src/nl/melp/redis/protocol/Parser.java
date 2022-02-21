package nl.melp.redis.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements the parser (reader) side of protocol.
 */
public class Parser {
	/**
	 * Thrown whenever data could not be parsed.
	 */
	static class ProtocolException extends IOException {
		ProtocolException(String msg) {
			super(msg);
		}
	}

	/**
	 * Thrown whenever an error string is decoded.
	 */
	static class ServerError extends IOException {
		ServerError(String msg) {
			super(msg);
		}
	}

	/**
	 * The input stream used to read the data from.
	 */
	private final InputStream input;

	/**
	 * Constructor.
	 *
	 * @param input The stream to read the data from.
	 */
	public Parser(InputStream input) {
		this.input = input;
	}

	/**
	 * Parse incoming data from the stream.
	 * <p>
	 * Based on each of the markers which will identify the type of data being sent, the parsing
	 * is delegated to the type-specific methods.
	 *
	 * @return The parsed object
	 * @throws IOException       Propagated from the stream
	 * @throws ProtocolException In case unexpected bytes are encountered.
	 */
	public Object parse() throws IOException, ProtocolException {
		Object ret;
		int read = this.input.read();
		switch (read) {
			case '+':
				ret = this.parseSimpleString();
				break;
			case '-':
				throw new ServerError(new String(this.parseSimpleString()));
			case ':':
				ret = this.parseNumber();
				break;
			case '$':
				ret = this.parseBulkString();
				break;
			case '*':
				long len = this.parseNumber();
				if (len == -1) {
					ret = null;
				} else {
					List<Object> arr = new LinkedList<>();
					for (long i = 0; i < len; i++) {
						arr.add(this.parse());
					}
					ret = arr;
				}
				break;
			case -1:
				return null;
			default:
				throw new ProtocolException("Unexpected input: " + (byte) read);
		}

		return ret;
	}

	/**
	 * Parse "RESP Bulk string" as a String object.
	 *
	 * @return The parsed response
	 * @throws IOException Propagated from underlying stream.
	 */
	private byte[] parseBulkString() throws IOException {
		final long expectedLength = parseNumber();
		if (expectedLength == -1) {
			return null;
		}
		if (expectedLength > Integer.MAX_VALUE) {
			throw new ProtocolException("Unsupported value length for bulk string");
		}
		final int numBytes = (int) expectedLength;
		final byte[] buffer = new byte[numBytes];
		int read = 0;
		while (read < expectedLength) {
			read += input.read(buffer, read, numBytes - read);
		}
		if (input.read() != '\r') {
			throw new ProtocolException("Expected CR");
		}
		if (input.read() != '\n') {
			throw new ProtocolException("Expected LF");
		}

		return buffer;
	}

	/**
	 * Parse "RESP Simple String"
	 *
	 * @return Resultant string
	 * @throws IOException Propagated from underlying stream.
	 */
	private byte[] parseSimpleString() throws IOException {
		return scanCr();
	}

	/**
	 * Parse a number (as long)
	 * @return The number
	 * @throws IOException Propagated from underlying stream
	 */
	private long parseNumber() throws IOException {
		return Long.parseLong(new String(scanCr()));
	}

	/**
	 * Scan the input stream for the next CR character
	 *
	 * @return Byte array.
	 * @throws IOException Propagated from underlying stream
	 */
	private byte[] scanCr() throws IOException {
		int size = 1024;
		int idx = 0;
		int ch;
		byte[] buffer = new byte[size];
		while ((ch = input.read()) != '\r') {
			buffer[idx++] = (byte) ch;
			if (idx == size) {
				// increase buffer size.
				size *= 2;
				buffer = java.util.Arrays.copyOf(buffer, size);
			}
		}
		if (input.read() != '\n') {
			throw new ProtocolException("Expected LF");
		}

		return Arrays.copyOfRange(buffer, 0, idx);
	}
}
