/* Released under the GPL2. See license.txt for details. */
package com.vanheusden.nagios;

import java.util.ArrayList;
import java.util.List;

public class DataSource
{
	final protected int maxN = 1000;
	final protected String dataSourceName;
	final protected List<Double> data = new ArrayList<Double>();

	public DataSource(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
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

		for(int index=0; index<data.size(); index++)
		{
			min = Math.min(min, data.get(index));
			max = Math.max(max, data.get(index));
			total += data.get(index);
			sdval += Math.pow(data.get(index), 2.0);
		}

		if (data.size() != 0)
			avg = total / (double)data.size();

		sd = Math.sqrt((sdval / (double)data.size()) - Math.pow(avg, 2.0));

		return new DataInfo(min, max, avg, sd, data.size());
	}

	public List<Double> getValues()
	{
		return data;
	}
}
