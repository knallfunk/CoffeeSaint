/* Released under the GPL2. See license.txt for details. */
package com.vanheusden.nagios;

import java.util.ArrayList;
import java.util.List;

public class DataSource
{
	final protected int maxN = 1000;
	final protected String dataSourceName;
	final protected List<Double> data = new ArrayList<Double>();
	protected String unit = "";

	public DataSource(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
	}

	public void setUnit(String unit)
	{
		this.unit = unit;
	}

	public String getUnit()
	{
		return unit;
	}

	public String getDataSourceName()
	{
		return dataSourceName;
	}

	public void add(double value)
	{
		data.add(value);

		if (data.size() > maxN)
			data.remove(0);
	}

	public List<Double> getData()
	{
		return data;
	}

	public DataInfo getStats()
	{
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		double total = 0.0, sdval = 0.0, avg = 0.0, sd = 0.0;
		int n = data.size();

		for(int index=0; index<n; index++)
		{
			double value = data.get(index);
			min = Math.min(min, value);
			max = Math.max(max, value);
			total += value;
			sdval += Math.pow(value, 2.0);
		}

		if (n != 0)
			avg = total / (double)n;

		sd = Math.sqrt((sdval / (double)n) - Math.pow(avg, 2.0));

		return new DataInfo(min, max, avg, sd, n);
	}

	public List<Double> getValues()
	{
		return data;
	}
}
