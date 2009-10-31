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
			System.err.println("Predictor brain dump of unsupported version (expected: " + brainDumpVersion + ", got: " + line + ")");
			return;
		}

		line = in.readLine();
		if (line.equals("" + interval) == false)
		{
			System.err.println("Expected interval " + interval + " but the file has " + line + ".");
			return;
		}

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
		nagiosErrorCountMonth[intervalNr] = (nagiosErrorCountMonth[intervalNr] * 3.0 + (double)errorCount) / 4.0;

		intervalNr = dateToIntervalWeek(nowDay, nowSecond);
		nagiosErrorCountWeek[intervalNr] = (nagiosErrorCountWeek[intervalNr] * 3.0 + (double)errorCount) / 4.0;

		intervalNr = dateToIntervalDay(nowSecond);
		nagiosErrorCountDay [intervalNr] = (nagiosErrorCountDay [intervalNr] * 3.0 + (double)errorCount) / 4.0;
	}

	int fixIndex(int nr, int nElements)
	{
		while (nr < 0)
			nr += nElements;

		while (nr >= nElements)
			nr -= nElements;

		return nr;
	}

	public double averageDifference(int intervalNrNow, double [] values, int nElements)
	{
		double prev = values[fixIndex(intervalNrNow + 1, nElements)];
		double diff = 0.0;
		for(int index=2; index<nElements; index++)
		{
			double cur = values[fixIndex(intervalNrNow + index, nElements)];
			diff += cur - prev;
			prev = cur;
		}

		return diff / (double)(nElements - 2);
	}

	public double averageDifferenceNextValue(int intervalNrNow, double [] values, int nElements)
	{
		double prevValue = values[fixIndex(intervalNrNow - 1, nElements)];

		double avgDiff = averageDifference(intervalNrNow, values, nElements);

		return prevValue + avgDiff;
	}

	public int secondsFromMidnight(Calendar now)
	{
		return (now.get(Calendar.HOUR_OF_DAY) * 3600) + (now.get(Calendar.MINUTE) * 60) + now.get(Calendar.SECOND);
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

		double dayPrediction   = averageDifferenceNextValue(intervalNrDayNow,   nagiosErrorCountDay,   getElementCountDay());
		double weekPrediction  = averageDifferenceNextValue(intervalNrWeekNow,  nagiosErrorCountWeek,  getElementCountWeek());
		double monthPrediction = averageDifferenceNextValue(intervalNrMonthNow, nagiosErrorCountMonth, getElementCountMonth());

		value = (dayPrediction + weekPrediction + monthPrediction +
			nagiosErrorCountMonth[intervalNrMonthThen] * 2.0 + nagiosErrorCountWeek[intervalNrWeekThen] * 2.0 + nagiosErrorCountDay[intervalNrDayThen]) / 8.0;

		return value;
	}
}
