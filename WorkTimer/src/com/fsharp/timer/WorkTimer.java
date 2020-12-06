/**
 * 
 */
package com.fsharp.timer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * @author FSharp4
 *
 */
public class WorkTimer extends Thread {
	Date duration;
	String title = "Timer";
	File file;
	
	/**
	 * Debug Constructor
	 */
	public WorkTimer() {
		//default time = 1hr
		duration = new Date();
		duration.setTime(3600 * 1000000000);
		
	}
	
	/**
	 * Degenerate constructor (no title)
	 * @param date String object encapsulating a duration (format hh:mm:ss)
	 */
	public WorkTimer(String date) {
		//format hh:mm:ss
		assert (date.length() == 8);
		assert (date.charAt(2) == ':' && date.charAt(5) == ':');
		duration = new Date();
		int hours = Integer.parseInt(date.substring(0, 2));
		int minutes = Integer.parseInt(date.substring(3, 5));
		int seconds = Integer.parseInt(date.substring(6, 8));
		duration.setTime((3600 * hours + 60 * minutes + seconds) * 1000);
	}
	
	/**
	 * Constructor method
	 * @param date String object encapsulating a duration (format hh:mm:ss)
	 * @param title Title of WorkTimer (i.e. "Chicken in Oven")
	 */
	public WorkTimer(String date, String title) {
		this(date);
		this.title = title;
	}
	
	public void run() {
		Date currentTime = new Date();
		long start = currentTime.getTime();
		currentTime.setTime(0);
		//System.out.println("Supposed Time: " + duration.getTime());
		while(currentTime.getTime() < duration.getTime()) {
			try {
				clearScreen();
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
			currentTime.setTime(System.currentTimeMillis() - start);
			long formatTime = Math.round((duration.getTime() - currentTime.getTime()) / 1000.0);
			//System.out.println("Format Time: " + formatTime);
			long hours = formatTime / 3600;
			formatTime %= 3600;
			long minutes = formatTime / 60;
			long seconds = formatTime % 60;
			System.out.format("\rHours: %d, \tMinutes: %d, \tSeconds: %d ", hours, minutes, seconds);
			try {
				Thread.sleep((long) (1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println();
		System.out.println(this.title + " done!");
		try {
			playSound();
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Clears screen via static subclass methods
	 * @throws IOException thrown at subclass
	 * @throws InterruptedException thrown at subclass
	 */
	public static void clearScreen() throws IOException, InterruptedException {  
		CLS.main();
	}
	
	/**
	 * Sets the audio file to trigger on timer finish
	 * @param filename File name
	 * @return boolean flag if setting was successful.
	 */
	public boolean setFile(String filename) {
		try {
			file = new File("res/" + filename + ".wav");
			return true;
		} catch (Exception e) {
			file = new File("res/Panic.wav");
			System.err.println("File not found! Using default instead");
			return false;
		}
	}
	
	void playSound() throws MalformedURLException, UnsupportedAudioFileException, IOException, LineUnavailableException {
		try {
		    AudioInputStream stream;
		    AudioFormat format;
		    DataLine.Info info;
		    Clip clip;

		    stream = AudioSystem.getAudioInputStream(this.file);
		    format = stream.getFormat();
		    info = new DataLine.Info(Clip.class, format);
		    clip = (Clip) AudioSystem.getLine(info);
		    clip.open(stream);
		    clip.start();
		    Thread.sleep(100000);
		}
		catch (Exception e) {
		    //whatevers
		}
	}
	
	/**
	 * Main method
	 * @param args [ Duration ("hh:mm:ss"), Title, Sound filename ]
	 */
	public static void main(String args[]) {
		WorkTimer wT = null;
		if (args.length >= 1) {
			wT = new WorkTimer(args[0]);
			if (args.length >= 2) {
				wT.title = args[1];
				if (args.length == 3) {
					wT.setFile(args[2]);
				} else {
					wT.setFile("Panic");
				}
			}
		} else {
			wT = new WorkTimer("00:00:05");
			wT.setFile("Panic");

		}
		
		wT.run();
	}
	
	private static class CLS {
	    static void main() throws IOException, InterruptedException {
	        Runtime.getRuntime().exec("clear");
	    }
	}
}
