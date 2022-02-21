package nl.melp.redis.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Implements the encoding (writing) side.
 */
public class Encoder {
	/**
	 * CRLF is used a lot.
	 */
	private static final byte[] CRLF = new byte[]{'\r', '\n'};

	/**
	 * This stream we will write to.
	 */
	private final OutputStream out;

	/**
	 * Construct the encoder with the passed output stream the encoder will write to.
	 *
	 * @param out Will be used to write all encoded data to.
	 */
	public Encoder(OutputStream out) {
		this.out = out;
	}

	/**
	 * Write a byte array in the "RESP Bulk String" format.
	 *
	 * @param value The byte array to write.
	 * @throws IOException Propagated from the output stream.
	 * @link https://redis.io/topics/protocol#resp-bulk-strings
	 */
	void write(byte[] value) throws IOException {
		out.write('$');
		out.write(Long.toString(value.length).getBytes());
		out.write(CRLF);
		out.write(value);
		out.write(CRLF);
	}

	/**
	 * Write a long value in the "RESP Integers" format.
	 *
	 * @param val The value to write.
	 * @throws IOException Propagated from the output stream.
	 * @link https://redis.io/topics/protocol#resp-integers
	 */
	void write(long val) throws IOException {
		out.write(':');
		out.write(Long.toString(val).getBytes());
		out.write(CRLF);
	}

	/**
	 * Write a list of objects in the "RESP Arrays" format.
	 *
	 * @param list A list of objects that contains Strings, Longs, Integers and (recursively) Lists.
	 * @throws IOException              Propagated from the output stream.
	 * @throws IllegalArgumentException If the list contains unencodable objects.
	 * @link https://redis.io/topics/protocol#resp-arrays
	 */
	public void write(List<?> list) throws IOException, IllegalArgumentException {
		out.write('*');
		out.write(Long.toString(list.size()).getBytes());
		out.write(CRLF);

		for (Object o : list) {
			if (o instanceof byte[]) {
				write((byte[]) o);
			} else if (o instanceof String) {
				write(((String) o).getBytes());
			} else if (o instanceof Long) {
				write((Long) o);
			} else if (o instanceof Integer) {
				write(((Integer) o).longValue());
			} else if (o instanceof List) {
				write((List<?>) o);
			} else {
				throw new IllegalArgumentException("Unexpected type " + o.getClass().getCanonicalName());
			}
		}
	}

	public void flush() throws IOException {
		out.flush();
	}
}
