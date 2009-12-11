/* Released under the GPL2. See license.txt for details. */
package com.vanheusden.nagios;

public class DataInfo
{
	double min, max, avg, sd;

	public DataInfo(double min, double max, double avg, double sd)
	{
		this.min = min;
		this.max = max;
		this.avg = avg;
		this.sd  = sd ;
	}

	public double getMin()
	{
		return min;
	}

	public double getMax()
	{
		return max;
	}

	public double getAvg()
	{
		return avg;
	}

	public double getSd()
	{
		return sd;
	}
}
