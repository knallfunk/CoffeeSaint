import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

class Predictor
{
	int elementCount, interval;
	int [] nagiosErrorCount;
	int [] necPredictedOk;
	boolean [] canUse;

	Predictor(int measureInterval)
	{
		interval = measureInterval;
		elementCount = (86400 * 7) / interval;

		nagiosErrorCount = new int[elementCount];
		necPredictedOk = new int[elementCount];
		canUse = new boolean[elementCount];
	}

	public void restoreBrainFromFile(String fileName) throws Exception
	{
                BufferedReader in = new BufferedReader(new FileReader(fileName));

		String line = in.readLine();
		if (line.equals("1.0") == false)
		{
			System.err.println("Predictor brain dump of unsupported version (expected: 1.0, got: " + line + ")");
			return;
		}

		line = in.readLine();
		if (line.equals("" + elementCount) == false)
		{
			System.err.println("Expected element count of " + elementCount + " but the file has got " + line + " of them.");
			return;
		}

		for(int index=0; index<elementCount; index++)
		{
			String valid = in.readLine();
			String value = in.readLine();

			if (valid.equals("1"))
			{
				nagiosErrorCount[index] = Integer.valueOf(value);
				canUse[index] = true;
			}
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

		writeLine(out, "1.0");
		writeLine(out, "" + elementCount);

		for(int index=0; index<elementCount; index++)
		{
			if (canUse[index])
				writeLine(out, "1");
			else
				writeLine(out, "0");

			writeLine(out, "" + nagiosErrorCount[index]);
		}

		out.close();
	}

	public void learn(int day, int second, int errorCount)
	{
		int intervalNr = (day * 86400 + second) / interval;

		if (canUse[intervalNr])
		{
			if (necPredictedOk[intervalNr] == errorCount) // misschien de afwijking erbij optellen ipv count
				necPredictedOk[intervalNr]++;
			else
				necPredictedOk[intervalNr]--;
		}

		if (nagiosErrorCount[intervalNr] != errorCount)
			necPredictedOk[intervalNr] = 0;
		nagiosErrorCount[intervalNr] = errorCount;
		canUse[intervalNr] = true;
	}

	public Double predict(int day, int second)
	{
		int newValue = 0;
		int divider = 0;
		int intervalNr = (day * 86400 + second) / interval;

		for(int index=0; index<elementCount; index++)
		{
			if (index == intervalNr)
				continue;

			if (canUse[index] == false)
				continue;

			divider += necPredictedOk[index];
			newValue += necPredictedOk[index] * nagiosErrorCount[index];
		}

		if (divider != 0)
			return (double)newValue / (double)divider;

		return null;
	}
}
