import java.io.File;
import javax.sound.sampled.*;
 
public class PlayWav
{
	public PlayWav(String fileName) throws Exception
	{
		File soundFile = new File(fileName);
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
		AudioFormat format = audioInputStream.getFormat();
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		SourceDataLine auline = (SourceDataLine) AudioSystem.getLine(info);

		auline.open(format);
		auline.start();

		int nBytesRead = 0;
		byte[] abData = new byte[128 * 1024];

		do 
		{
			nBytesRead = audioInputStream.read(abData, 0, abData.length);
			if (nBytesRead >= 0)
				auline.write(abData, 0, nBytesRead);
		}
		while (nBytesRead != -1);

		auline.drain();
		auline.close();
	}
}
