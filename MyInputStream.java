import java.io.InputStream;

class MyInputStream extends InputStream {
	byte [] data;
	int pointer = 0;

	public MyInputStream(byte [] input) {
		data = input;
	}

	public int available() {
		return data.length - pointer;
	}

	public void close() {
	}

	public void mark() {
	}

	public boolean markSupported() {
		return false;
	}

	public String getEncoding() {
		return "UTF-8";
	}

	public int read() {
		if (pointer < data.length)
			return data[pointer++];

		return -1;
	}

	public int read(byte [] buf) {
		return read(buf, 0, buf.length);
	}

	public int read(byte[] buf, int offset, int length) {
		int n = Math.min(length, data.length - pointer);

		for(int index=0; index<n; index++) {
			buf[offset + index] = data[pointer++];
		}

		return n == 0 ? -1 : n;
	}

	public void reset() {
		pointer = 0;
	}

	public long skip(long n) {
		pointer += (int)n;

		return (int)n;
	}
}
