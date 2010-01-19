/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class FrameListener extends WindowAdapter
{
	Config config;
	CoffeeSaint coffeeSaint;

	FrameListener(Config config, CoffeeSaint coffeeSaint)
	{
		this.config = config;
		this.coffeeSaint = coffeeSaint;
	}

	public void windowClosing(WindowEvent event)
	{
		try
		{
			coffeeSaint.dumpPredictorBrainToFile();
			coffeeSaint.dumpPerformanceData();
			coffeeSaint.dumpLatencyData();
		}
		catch(Exception exception)
		{
			CoffeeSaint.showException(exception);
		}
		finally
		{
			System.exit(0);
		}
	}
}
