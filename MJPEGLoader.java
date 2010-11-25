import java.awt.Image;
import java.awt.Toolkit;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

class MJPEGLoader extends ImageLoader implements Runnable {
	MJPEGLoader(String addr, int timeout) {
		super(addr, timeout);
	}

	public void run() {
		try {
			URL urlObj = new URL(url);
			HttpURLConnection connection = (HttpURLConnection)urlObj.openConnection();
			connection.setDefaultUseCaches(false);
			connection.setUseCaches(false);
			connection.setRequestProperty("User-Agent", CoffeeSaint.version);
			String cType = connection.getContentType();
			int bOffset = cType.indexOf("boundary=");
			if (bOffset == -1)
				throw new Exception("Don't know how to handle " + url + ", stream not understood");
			String boundary = cType.substring(bOffset + 9);
			int bEnd = boundary.indexOf(" ");
			if (bEnd == -1)
				bEnd = boundary.indexOf(";");
			if (bEnd != -1)
				boundary = boundary.substring(0, bEnd);
			int bLen = boundary.length();

			int maxSize = 1024 * 1024;
			byte [] imageData = new byte[maxSize]; // really frames should not be that large
			int idLen = 0;

			InputStream input = connection.getInputStream();

			byte [] crlf = new byte[4];
			do
			{
				crlf[0] = crlf[1];
				crlf[1] = crlf[2];
				crlf[2] = crlf[3];
				int c = input.read();
				if (c == -1)
					break;
				crlf[3] = (byte)c;
			}
			while(crlf[0] != '\r' || crlf[1] != '\n' || crlf[2] != '\r' || crlf[3] != '\n');

			boolean found = false;
			while(idLen < maxSize) {
				int c = input.read();
				if (c == -1)
					break;
				imageData[idLen++] = (byte)c;
				if (idLen >= bLen) {
					int index = -1;
					found = false;
					for(index=bLen; index<(idLen - bLen); index++) {
						found = true;
						for(int checkLoop=0; checkLoop<bLen; checkLoop++) {
							if (imageData[index + checkLoop] != (byte)boundary.charAt(checkLoop)) {
								found = false;
								break;
							}
						}
						if (found)
							break;
					}

					if (found) {
						idLen = index;
						break;
					}
				}
			}

			input.close();
			connection.disconnect();

			if (found) {
				byte [] resultStream = new byte[idLen];
				System.arraycopy(imageData, 0, resultStream, 0, idLen);

				MyInputStream misr = new MyInputStream(resultStream);

				result = ImageIO.read((InputStream)misr);
			}

			System.out.println("MJPEGLoader: " + url + " succesfully retrieved");
		}
		catch(Exception e) {
			exception = "MJPEGLoader failed to load " + url + ": " + e;
			CoffeeSaint.log.add(exception);
		}
		finally {
			lock.release();
		}
	}
}
