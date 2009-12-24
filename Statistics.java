/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.util.concurrent.Semaphore;

public class Statistics
{
	private final Semaphore statisticsSemaphore = new Semaphore(1);
	private double totalRefreshTime, runningSince, totalImageLoadTime;
	private int nRefreshes;
	private int exceptions;

	public Statistics()
	{
	}

	public void lock()
	{
		statisticsSemaphore.acquireUninterruptibly();
	}

	public void unlock()
	{
		statisticsSemaphore.release();
	}

	public double getTotalRefreshTime()
	{
		double copy;
		lock();
		copy = totalRefreshTime;
		unlock();
		return copy;
	}

	public double getRunningSince()
	{
		double copy;
		lock();
		copy = runningSince;
		unlock();
		return copy;
	}

	public double getTotalImageLoadTime()
	{
		double copy;
		lock();
		copy = totalImageLoadTime;
		unlock();
		return copy;
	}

	public int getNRefreshes()
	{
		int copy;
		lock();
		copy = nRefreshes;
		unlock();
		return copy;
	}

	public void addToTotalRefreshTime(double time)
	{
		lock();
		totalRefreshTime += time;
		unlock();
	}

	public void addToTotalImageLoadTime(double time)
	{
		lock();
		totalImageLoadTime += time;
		unlock();
	}

	public void addToNRefreshes(int n)
	{
		lock();
		nRefreshes += n;
		unlock();
	}

	public void setRunningSince(double time)
	{
		lock();
		runningSince = time;
		unlock();
	}

	public void incExceptions()
	{
		lock();
		exceptions++;
		unlock();
	}

	public int getNExceptions()
	{
		int copy;
		lock();
		copy = exceptions;
		unlock();
		return copy;
	}
}
