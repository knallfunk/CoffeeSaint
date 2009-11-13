
/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.util.concurrent.Semaphore;

class Statistics
{
	Semaphore statisticsSemaphore = new Semaphore(1);
	double totalRefreshTime, runningSince, totalImageLoadTime;
	int nRefreshes;

	Statistics()
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

	double getTotalRefreshTime()
	{
		double copy;
		lock();
		copy = totalRefreshTime;
		unlock();
		return copy;
	}

	double getRunningSince()
	{
		double copy;
		lock();
		copy = runningSince;
		unlock();
		return copy;
	}

	double getTotalImageLoadTime()
	{
		double copy;
		lock();
		copy = totalImageLoadTime;
		unlock();
		return copy;
	}

	int getNRefreshes()
	{
		int copy;
		lock();
		copy = nRefreshes;
		unlock();
		return copy;
	}

	void addToTotalRefreshTime(double time)
	{
		lock();
		totalRefreshTime += time;
		unlock();
	}

	void addToTotalImageLoadTime(double time)
	{
		lock();
		totalImageLoadTime += time;
		unlock();
	}

	void addToNRefreshes(int n)
	{
		lock();
		nRefreshes += n;
		unlock();
	}

	void setRunningSince(double time)
	{
		lock();
		runningSince = time;
		unlock();
	}
}
