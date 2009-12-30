/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;

class Predictor
{
	final String brainDumpVersion = "0.004";
	int interval;
	double [] nagiosErrorCountWeek;
	double [] nagiosErrorCountDay;
	double [] nagiosErrorCountMonth;

	Predictor(int measureInterval)
	{
		interval = measureInterval;

		nagiosErrorCountMonth = new double[getElementCountMonth()];
		nagiosErrorCountWeek  = new double[getElementCountWeek()];
		nagiosErrorCountDay   = new double[getElementCountDay ()];
	}

	public int getElementCountMonth()
	{
		return (86400 * 31) / interval;
	}

	public int getElementCountWeek()
	{
		return (86400 * 7) / interval;
	}

	public int getElementCountDay()
	{
		return 86400 / interval;
	}

	public void restoreBrainFromFile(String fileName) throws Exception
	{
                BufferedReader in = new BufferedReader(new FileReader(fileName));

		String line = in.readLine();
		if (line.equals(brainDumpVersion) == false)
		{
			CoffeeSaint.log.add("Predictor brain dump of unsupported version (expected: " + brainDumpVersion + ", got: " + line + ")");
			in.close();
			return;
		}

		CoffeeSaint.log.add("Brain dump version: " + line);

		line = in.readLine();
		if (line.equals("" + interval) == false)
		{
			CoffeeSaint.log.add("Expected interval " + interval + " but the file has " + line + ".");
			in.close();
			return;
		}

		CoffeeSaint.log.add("Brain interval: " + line);

		for(int index=0; index<getElementCountMonth(); index++)
		{
			String necm = in.readLine();
			nagiosErrorCountMonth[index] = Double.valueOf(necm);
		}

		for(int index=0; index<getElementCountWeek(); index++)
		{
			String necw = in.readLine();
			nagiosErrorCountWeek[index] = Double.valueOf(necw);
		}

		for(int index=0; index<getElementCountDay(); index++)
		{
			String necd = in.readLine();
			nagiosErrorCountDay [index] = Double.valueOf(necd);
		}

		String trailer = in.readLine();
		if (trailer != null)
		{
			if (!trailer.equals("END"))
			{
				CoffeeSaint.log.add("Expected 'END', got " + trailer);
				nagiosErrorCountMonth = new double[getElementCountMonth()];
				nagiosErrorCountWeek  = new double[getElementCountWeek()];
				nagiosErrorCountDay   = new double[getElementCountDay ()];
			}
			else
				CoffeeSaint.log.add("END found");
		}

                in.close();
	}

	void writeLine(BufferedWriter out, String line) throws Exception
	{
		out.write(line, 0, line.length());
		out.newLine();
	}

	public void dumpBrainToFile(String fileName) throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));

		writeLine(out, brainDumpVersion);
		writeLine(out, "" + interval);

		for(int index=0; index<getElementCountMonth(); index++)
			writeLine(out, "" + nagiosErrorCountMonth[index]);

		for(int index=0; index<getElementCountWeek(); index++)
			writeLine(out, "" + nagiosErrorCountWeek[index]);

		for(int index=0; index<getElementCountDay(); index++)
			writeLine(out, "" + nagiosErrorCountDay [index]);

		writeLine(out, "END");

		out.close();
	}

	public int dateToIntervalMonth(int dayOfMonth, int second)
	{
		return ((dayOfMonth - 1) * 86400 + second) / interval;
	}

	public int dateToIntervalWeek(int day, int second)
	{
		return (day * 86400 + second) / interval;
	}

	public int dateToIntervalDay(int second)
	{
		return second / interval;
	}

	public void learn(Calendar now, int errorCount)
	{
		int intervalNr;

		int nowDOM = now.get(Calendar.DAY_OF_MONTH);
		int nowDay = now.get(Calendar.DAY_OF_WEEK) - 1; // SUNDAY=1, SATURDAY=7
		int nowSecond = (now.get(Calendar.HOUR_OF_DAY) * 3600) + (now.get(Calendar.MINUTE) * 60) + now.get(Calendar.SECOND);

		intervalNr = dateToIntervalMonth(nowDOM, nowSecond);
		// nagiosErrorCountMonth[intervalNr] = (nagiosErrorCountMonth[intervalNr] * 3.0 + (double)errorCount) / 4.0;
		nagiosErrorCountMonth[intervalNr % getElementCountMonth()] = (double)errorCount;

		intervalNr = dateToIntervalWeek(nowDay, nowSecond);
		// nagiosErrorCountWeek[intervalNr] = (nagiosErrorCountWeek[intervalNr] * 3.0 + (double)errorCount) / 4.0;
		nagiosErrorCountWeek[intervalNr % getElementCountWeek()] = (double)errorCount;

		intervalNr = dateToIntervalDay(nowSecond);
		// nagiosErrorCountDay [intervalNr] = (nagiosErrorCountDay [intervalNr] * 3.0 + (double)errorCount) / 4.0;
		nagiosErrorCountDay [intervalNr % getElementCountDay()] = (double)errorCount;
	}

	int fixIndex(int nr, int nElements)
	{
		while (nr < 0)
			nr += nElements;

		while (nr >= nElements)
			nr -= nElements;

		return nr;
	}
	public int secondsFromMidnight(Calendar now)
	{
		return (now.get(Calendar.HOUR_OF_DAY) * 3600) + (now.get(Calendar.MINUTE) * 60) + now.get(Calendar.SECOND);
	}

	// http://nl.wikipedia.org/wiki/Regressie-analyse#Meervoudige_lineaire_regressie
	// http://nl.wikipedia.org/wiki/Kleinste-kwadratenmethode
	public double predictWithLeastSquaresEstimate(int intervalNrNow, double [] values, int nElements, long nIndexsIntoTheFuture)
	{
		double sumX = 0.0, sumY = 0.0, sumXY = 0.0, sumX2 = 0.0;

		for(int index=1; index<nElements; index++)
		{
			int timeStamp = -(nElements - index);

			sumX += timeStamp;
			int realIndex = fixIndex(intervalNrNow + index, nElements);
			sumY += values[realIndex];
			sumXY += timeStamp * values[realIndex];
			sumX2 += Math.pow(timeStamp, 2.0);
		}
		int usedNElements = nElements;

		double b = (usedNElements * sumXY - sumX * sumY) /
			   (usedNElements * sumX2 - sumX * sumX);
		double a = (sumY / usedNElements) - b * (sumX / usedNElements);

		return a + nIndexsIntoTheFuture * b;
	}

	public double getHistorical(int intervalNrDay, int intervalNrWeek, int intervalNrMonth)
	{
		return (nagiosErrorCountMonth[intervalNrMonth % getElementCountMonth()] + nagiosErrorCountWeek[intervalNrWeek % getElementCountWeek()] + nagiosErrorCountDay[intervalNrDay % getElementCountDay()]) / 3.0;
	}

	public double getHistorical(Calendar when)
	{
		int DOM    = when.get(Calendar.DAY_OF_MONTH);
		int day    = when.get(Calendar.DAY_OF_WEEK) - 1;
		int second = secondsFromMidnight(when);
                int intervalNrMonth = dateToIntervalMonth(DOM, second);
                int intervalNrWeek  = dateToIntervalWeek (day, second);
                int intervalNrDay   = dateToIntervalDay  (     second);

		return getHistorical(intervalNrDay, intervalNrWeek, intervalNrMonth);
	}

	public Double predict(Calendar now, Calendar then)
	{
		double value;

		int nowDOM = now.get(Calendar.DAY_OF_MONTH);
		int nowDay = now.get(Calendar.DAY_OF_WEEK) - 1;
		int nowSecond = secondsFromMidnight(now);
                int intervalNrMonthNow = dateToIntervalMonth(nowDOM, nowSecond);
                int intervalNrWeekNow  = dateToIntervalWeek (nowDay, nowSecond);
                int intervalNrDayNow   = dateToIntervalDay  (        nowSecond);

		int thenDOM = then.get(Calendar.DAY_OF_MONTH);
		int thenDay = then.get(Calendar.DAY_OF_WEEK) - 1;
		int thenSecond = secondsFromMidnight(then);
                int intervalNrMonthThen = dateToIntervalMonth(thenDOM, thenSecond);
                int intervalNrWeekThen  = dateToIntervalWeek (thenDay, thenSecond);
                int intervalNrDayThen   = dateToIntervalDay  (         thenSecond);

		long nIndexesIntoTheFutre = (then.getTimeInMillis() - now.getTimeInMillis()) / (1000 * interval);
		double dayPredictionLSE   = predictWithLeastSquaresEstimate(intervalNrDayNow,   nagiosErrorCountDay,   getElementCountDay(),   nIndexesIntoTheFutre);
		double weekPredictionLSE  = predictWithLeastSquaresEstimate(intervalNrWeekNow,  nagiosErrorCountWeek,  getElementCountWeek(),  nIndexesIntoTheFutre);
		double monthPredictionLSE = predictWithLeastSquaresEstimate(intervalNrMonthNow, nagiosErrorCountMonth, getElementCountMonth(), nIndexesIntoTheFutre);
		CoffeeSaint.log.add("LSE, day: " + dayPredictionLSE + ", week: " + weekPredictionLSE + ", month: " + monthPredictionLSE);

		value = (dayPredictionLSE * 1.0 + weekPredictionLSE * 2.0 + monthPredictionLSE * 3.0 +
			getHistorical(intervalNrDayThen, intervalNrWeekThen, intervalNrMonthThen)) / 7.0;
		CoffeeSaint.log.add("predict return: " + value);

		return value;
	}
}
