import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;

class ImageLoader implements Runnable {
	Image result = null;
	String url = null;
	int to;
	Semaphore lock = new Semaphore(1);
	Thread t;

	ImageLoader(String addr, int timeout) {
		to = timeout;
		url = addr;

		lock.acquireUninterruptibly();
		t = new Thread(this);
		t.start();
	}

	Image getImage() throws Exception {
		if (lock.tryAcquire(to, TimeUnit.MILLISECONDS))
			return result;

		t.interrupt();

		System.out.println("ImageLoader: could not retrieve in time (" + to + ")");
		return null;
	}

	public void run() {
		try {
			Image dummy = Toolkit.getDefaultToolkit().createImage(new URL(url));
			new ImageIcon(dummy); //loads the image
			result = dummy;
			System.out.println("ImageLoader: " + url + " succesfully retrieved");
		}
		catch(Exception e) {
			System.err.println("ImageLoader(" + url + ", " + to + "): " + e);
		}
		finally {
			lock.release();
		}
	}

	public static void main(String [] args) {
		ImageLoader i = new ImageLoader("http://keetweej.vanheusden.com/linux-logo.gif", 100);

		try {
			System.out.println("" + i.getImage());
		}
		catch(Exception e) {
			System.err.println("Exception: " + e);
		}
	}
}
