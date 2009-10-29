import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

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

	public void learn(int dayOfMonth, int day, int second, int errorCount)
	{
		int intervalNr;

		intervalNr = dateToIntervalMonth(dayOfMonth, second);
		nagiosErrorCountMonth[intervalNr] = (nagiosErrorCountMonth[intervalNr] * 3.0 + (double)errorCount) / 4.0;

		intervalNr = dateToIntervalWeek(day, second);
		nagiosErrorCountWeek[intervalNr] = (nagiosErrorCountWeek[intervalNr] * 3.0 + (double)errorCount) / 4.0;

		intervalNr = dateToIntervalDay(second);
		nagiosErrorCountDay [intervalNr] = (nagiosErrorCountDay [intervalNr] * 3.0 + (double)errorCount) / 4.0;
	}

	public Double predict(int dayOfMonth, int day, int second)
	{
		double value;
		int intervalNrMonth = dateToIntervalMonth(dayOfMonth, second);
		int intervalNrWeek  = dateToIntervalWeek (day,        second);
		int intervalNrDay   = dateToIntervalDay  (            second);

		value = (nagiosErrorCountMonth[intervalNrMonth] * 2.0 + nagiosErrorCountWeek[intervalNrWeek] * 2.0 + nagiosErrorCountDay[intervalNrDay]) / 5.0;

		return value;
	}
}
