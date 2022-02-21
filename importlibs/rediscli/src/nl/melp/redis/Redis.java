package nl.melp.redis;

import nl.melp.redis.protocol.Encoder;
import nl.melp.redis.protocol.Parser;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A lightweight implementation of the Redis server protocol at https://redis.io/topics/protocol
 * <p>
 * Effectively a complete Redis client implementation.
 */
public class Redis {

	/**
	 * Used for writing the data to the server.
	 */
	private final Encoder writer;

	/**
	 * Used for reading responses from the server.
	 */
	private final Parser reader;

	/**
	 * Construct the connection with the specified Socket as the server connection with default buffer sizes.
	 *
	 * @param socket Connected socket to the server.
	 * @throws IOException If a socket error occurs.
	 */
	public Redis(Socket socket) throws IOException {
		this(socket, 1 << 16, 1 << 16);
	}

	/**
	 * Construct the connection with the specified Socket as the server connection with specified buffer sizes.
	 *
	 * @param socket           Socket to connect to
	 * @param inputBufferSize  buffer size in bytes for the input stream
	 * @param outputBufferSize buffer size in bytes for the output stream
	 * @throws IOException If a socket error occurs.
	 */
	public Redis(Socket socket, int inputBufferSize, int outputBufferSize) throws IOException {
		this(
			new BufferedInputStream(socket.getInputStream(), inputBufferSize),
			new BufferedOutputStream(socket.getOutputStream(), outputBufferSize)
		);
	}

	/**
	 * Construct with the specified streams to respectively read from and write to.
	 *
	 * @param inputStream  Read from this stream
	 * @param outputStream Write to this stream
	 */
	public Redis(InputStream inputStream, OutputStream outputStream) {
		this.reader = new Parser(inputStream);
		this.writer = new Encoder(outputStream);
	}

	/**
	 * Execute a Redis command and return it's result.
	 *
	 * @param args Command and arguments to pass into redis.
	 * @param <T>  The expected result type
	 * @return Result of redis.
	 * @throws IOException All protocol and io errors are IO exceptions.
	 */
	public <T> T call(Object... args) throws IOException {
		writer.write(Arrays.asList((Object[]) args));
		writer.flush();
		return read();
	}


	/**
	 * Execute a Redis command and return it's result.
	 *
	 * @param args Command and arguments to pass into redis.
	 * @param <T>  The expected result type
	 * @return Result of redis.
	 * @throws IOException All protocol and io errors are IO exceptions.
	 */
	public <T> T command(String command) throws IOException {		
		writer.write(Arrays.asList((Object[]) command.split(" ")));
		writer.flush();
		return read();
	}

	
	/**
	 * Does a blocking read to wait for redis to send data.
	 *
	 * @param <T> The expected result type.
	 * @return Result of redis
	 * @throws IOException Propagated
	 */
	public <T> T read() throws IOException {
		return (T) reader.parse();
	}

	/**
	 * Helper class for pipelining.
	 */
	public static abstract class Pipeline {
		/**
		 * Write a new command to the server.
		 *
		 * @param args Command and arguments.
		 * @return self for chaining
		 * @throws IOException Propagated from underlying server.
		 */
		public abstract Pipeline call(String... args) throws IOException;

		/**
		 * Returns an aligned list of responses for each of the calls.
		 *
		 * @return The responses
		 * @throws IOException Propagated from underlying server.
		 */
		public abstract List<Object> read() throws IOException;
	}

	/**
	 * Create a pipeline which writes all commands to the server and only starts
	 * reading the response when read() is called.
	 *
	 * @return A pipeline object.
	 */
	public Pipeline pipeline() {
		return new Pipeline() {
			private int n = 0;

			public Pipeline call(String... args) throws IOException {
				writer.write(Arrays.asList((Object[]) args));
				writer.flush();
				n++;
				return this;
			}

			public List<Object> read() throws IOException {
				List<Object> ret = new LinkedList<>();
				while (n-- > 0) {
					ret.add(reader.parse());
				}
				return ret;
			}
		};
	}

	@FunctionalInterface
	public interface FailableConsumer<T, E extends Throwable> {
		void accept(T t) throws E;
	}

	/**
	 * Utility method to execute some command with redis and close the connection directly after.
	 *
	 * @param callback The callback to perform with redis.
	 * @param addr     Connection IP address
	 * @param port     Connection port
	 * @throws IOException Propagated
	 */
	public static void run(FailableConsumer<Redis, IOException> callback, String addr, int port) throws IOException {
		try (Managed redis = connect(addr, port)) {
			callback.accept(redis);
		}
	}

	/**
	 * Utility method to run a single command on an existing socket.
	 *
	 * Note that this does not close the connection!
	 *
	 * @param callback 	The callback to perform with redis.
	 * @param s     	Connection socket
	 * @throws IOException Propagated
	 */
	public static void run(FailableConsumer<Redis, IOException> callback, Socket s) throws IOException {
		callback.accept(new Redis(s));
	}

	/**
	 * Autocloseable implementation of Redis.
	 */
	public abstract static class Managed extends Redis implements AutoCloseable {
		Managed(Socket s) throws IOException {
			super(s);
		}

		abstract public void close() throws IOException;
	}

	/**
	 * Create a "managed" connection, i.e. one that is cleanly closed (with a QUIT call), implemented as
	 * an Autoclosable.
	 *
	 * @param host	Redis host
	 * @param port	Redis port
	 * @return The Autoclosable implementation
	 * @throws IOException Propagated
	 */
	public static Managed connect(String host, int port) throws IOException {
		Socket s = new Socket(host, port);
		return new Managed(s) {
			@Override
			public void close() throws IOException {
				call("QUIT");
				s.close();
			}
		};
	}
}
