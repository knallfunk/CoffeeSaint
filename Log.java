/* Released under GPL2, (C) 2011 by folkert@vanheusden.com */
import java.text.SimpleDateFormat;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class Log {
	List<String> log = new ArrayList<String>();
	int maxN = 100;
	private Semaphore semaphore = new Semaphore(1);
	String logFile = null;

	private void lock() {
		semaphore.acquireUninterruptibly();
	}

	private void unlock() {
		semaphore.release();
	}

	public Log(int maxN) {
		this.maxN = maxN;
	}

	public void setLogFile(String f) {
		logFile = f;
	}

	public String formatDate(Calendar when) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("E yyyy.MM.dd  hh:mm:ss a zzz");

		return dateFormatter.format(when.getTime());
	}

	public void add(String what) {
		what = formatDate(Calendar.getInstance()) + "] " + what;
		System.out.println(what);

		if (logFile != null) {
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(logFile, true));
				String ts = new SimpleDateFormat("E yyyy.MM.dd  hh:mm:ss a zzz").format(Calendar.getInstance().getTime());
				out.write(what, 0, what.length());
				out.newLine();
				out.close();
			}
			catch(Exception e) {
				CoffeeSaint.showException(e);
			}
		}

		lock();
		log.add(formatDate(Calendar.getInstance()) + "] " + what);

		while (log.size() > maxN)
			log.remove(0);
		unlock();
	}

	public List<String> get() {
		List<String> copy = new ArrayList<String>();
		lock();
		for(String current : log)
			copy.add(current);
		unlock();
		return copy;
	}
}
