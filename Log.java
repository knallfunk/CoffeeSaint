/* Released under GPL2, (C) 2010 by folkert@vanheusden.com */
import java.text.SimpleDateFormat;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Log
{
	List<String> log = new ArrayList<String>();
	int maxN = 100;
	private Semaphore semaphore = new Semaphore(1);

	private void lock()
	{
		semaphore.acquireUninterruptibly();
	}

	private void unlock()
	{
		semaphore.release();
	}

	public Log(int maxN)
	{
		this.maxN = maxN;
	}

	public String formatDate(Calendar when)
	{
		SimpleDateFormat dateFormatter = new SimpleDateFormat("E yyyy.MM.dd  hh:mm:ss a zzz");

		return dateFormatter.format(when.getTime());
	}

	public void add(String what)
	{
		System.out.println(what);

		lock();
		log.add(formatDate(Calendar.getInstance()) + "] " + what);

		while (log.size() > maxN)
			log.remove(0);
		unlock();
	}

	public List<String> get()
	{
		List<String> copy = new ArrayList<String>();
		lock();
		for(String current : log)
			copy.add(current);
		unlock();
		return copy;
	}
}
