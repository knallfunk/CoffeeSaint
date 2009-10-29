import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class FrameListener extends WindowAdapter
{
	public void windowClosing(WindowEvent event)
	{
		try
		{
			if (CoffeeSaint.predictor != null)
			{
				System.out.println("Storing brain to file " + CoffeeSaint.predictorBrainFileName);
				CoffeeSaint.predictor.dumpBrainToFile(CoffeeSaint.predictorBrainFileName);
			}
		}
		catch(Exception exception)
		{
			CoffeeSaint.showException(exception);
		}

		System.exit(0);
	}
}
