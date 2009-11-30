/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class FrameListener extends WindowAdapter
{
	Config config;

	FrameListener(Config config)
	{
		this.config = config;
	}

	public void windowClosing(WindowEvent event)
	{
		try
		{
			if (CoffeeSaint.getPredictor() != null)
			{
				CoffeeSaint.log.add("Storing brain to file " + config.getBrainFileName());
				CoffeeSaint.predictor.dumpBrainToFile(config.getBrainFileName());
			}
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
