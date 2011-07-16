/* Released under GPL2, (C) 2009-2011 by folkert@vanheusden.com */
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
 
public class PlayWav implements Runnable {
	SourceDataLine auline = null;
	AudioInputStream audioInputStream = null;

	public PlayWav(String fileName) throws Exception {
		File soundFile = new File(fileName);
		try {
			audioInputStream = AudioSystem.getAudioInputStream(soundFile);
		}
		catch(UnsupportedAudioFileException uafe) {
			String msg = "Java doesn't support the format of the audio-file " + fileName;
			CoffeeSaint.log.add(msg);
			AudioFileFormat.Type[] formatTypes = AudioSystem.getAudioFileTypes();
			for(AudioFileFormat.Type at : formatTypes) {
				CoffeeSaint.log.add("supported: " + at.toString() + " (*." + at.getExtension() + ")");
			}
			throw new Exception(msg);
		}

		AudioFormat format = audioInputStream.getFormat();
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		if (AudioSystem.isLineSupported(info) == false) {
			CoffeeSaint.log.add("Soundcard doesn't support the format of the audio-file " + fileName + " (which has the format: " + format + ")");
			throw new Exception("Soundcard doesn't support the format of the audio-file");
		}
		auline = (SourceDataLine) AudioSystem.getLine(info);

		auline.open(format);
		auline.start();

		new Thread(this).start();
	}

	public void run() {
		int nBytesRead = 0;
		byte[] abData = new byte[128 * 1024];

		try {
			do {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				if (nBytesRead >= 0)
					auline.write(abData, 0, nBytesRead);
			}
			while (nBytesRead != -1);
		}
		catch(IOException ie) {
			CoffeeSaint.showException(ie);
		}

		auline.drain();
		auline.close();
	}
}
