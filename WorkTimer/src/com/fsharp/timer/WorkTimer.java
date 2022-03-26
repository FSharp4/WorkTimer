/**
 * 
 */
package com.fsharp.timer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Optional;
import java.util.Scanner;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;
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
  private static boolean snooze = true; // Not ideal but users can always run many jvm
  private static int PAGE_SIZE = 30;
  private static final String DISENGAGE = "Disengage";
  private static final Scanner sc = new Scanner(System.in);
  private static String CLEAN_SCREEN = "";

  /**
   * Debug Constructor
   */
  public WorkTimer() {
    // default time = 1hr
    duration = new Date();
    duration.setTime(3600 * 1000000000);

  }

  /**
   * Degenerate constructor (no title)
   * 
   * @param date String object encapsulating a duration (format hh:mm:ss)
   */
  public WorkTimer(String date) {
    // format hh:mm:ss
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
   * 
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
    System.out.println("Disarm timer by typing " + DISENGAGE + " or Ctrl+C");
    System.out.println("Type anything else to snooze by 5 min");
    // System.out.println("Supposed Time: " + duration.getTime());
    while (currentTime.getTime() < duration.getTime()) {
      try {
        clearScreen();
      } catch (IOException | InterruptedException e1) {
        e1.printStackTrace();
      }
      currentTime.setTime(System.currentTimeMillis() - start);
      long formatTime = Math.round((duration.getTime() - currentTime.getTime()) / 1000.0);
      // System.out.println("Format Time: " + formatTime);
      long hours = formatTime / 3600;
      formatTime %= 3600;
      long minutes = formatTime / 60;
      long seconds = formatTime % 60;
      System.out.format("\rHours: %d, \tMinutes: %d, \tSeconds: %d ", hours, minutes, seconds);
      try {
        Thread.sleep((long) (1000));
      } catch (InterruptedException e) {
        e.printStackTrace();
        WorkTimer.snooze = false;
      }
    }
    System.out.println();
    System.out.println(this.title + " done!");
    try {
      WorkTimer.snooze = alarmAndWaitForSnooze();
    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
      e.printStackTrace();
      WorkTimer.snooze = false;
    }
  }

  /**
   * Clears screen via static subclass methods
   * 
   * @throws IOException thrown at subclass
   * @throws InterruptedException thrown at subclass
   */
  public static void clearScreen() throws IOException, InterruptedException {
    CLS.main();
  }

  /**
   * Sets the audio file to trigger on timer finish
   * 
   * @param filename File name
   * @return boolean flag if setting was successful.
   */
  public boolean setFile(String filename) {
    try {
      file = new File("res/" + filename + ".wav");
      return true;
    } catch (Exception e) {
      file = new File("res/Item.wav");
      System.err.println("File not found! Using default instead");
      return false;
    }
  }

  boolean alarmAndWaitForSnooze() throws MalformedURLException, UnsupportedAudioFileException,
      IOException, LineUnavailableException {
    try {
      AudioInputStream stream;
      AudioFormat format;
      DataLine.Info info;
      Clip clip;

      stream = AudioSystem.getAudioInputStream(this.file);
      format = stream.getFormat();
      info = new DataLine.Info(Clip.class, format);
      clip = (Clip) AudioSystem.getLine(info);
      SoundMonitor sm = new SoundMonitor(clip, stream);
      sm.start();
      boolean snooze = willUserSnooze();
      sm.close();
      return snooze;
    } catch (Exception e) {
      System.out.println("Interrupted unexpectedly on init alarm");
      e.printStackTrace();
      return false;
    }
  }

  boolean willUserSnooze() {
    // Scanner sc = new Scanner(System.in);
    return !sc.next().contentEquals(DISENGAGE);
  }

  /**
   * Main method
   * 
   * @param args [ Duration ("hh:mm:ss"), Title, Sound filename ]
   */
  public static void main(String args[]) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < PAGE_SIZE; i++) {
      sb.append("\n");
    }
    CLEAN_SCREEN = sb.toString();
    WorkTimer wT = null;
    String filename = "Item";
    if (args.length >= 1) {
      wT = new WorkTimer(args[0]);
      if (args.length >= 2) {
        wT.title = args[1];
        if (args.length == 3) {
          filename = args[2];
        }

        wT.setFile(filename);
      }
    } else {
      wT = new WorkTimer("00:00:05");
      wT.setFile(filename);

    }

    wT.run();
    while (snooze) {
      // TODO: Update to be longer
      wT = new WorkTimer("00:05:00");
      wT.setFile(filename);
      wT.run();
    }

    sc.close();
  }

  private static class CLS {
    static void main() throws IOException, InterruptedException {
      System.out.print(CLEAN_SCREEN);
    }
  }

  private static class SoundMonitor implements LineListener {
    Clip c;
    AudioInputStream s;
    private boolean restarting = false;
    private boolean starting = false;
    private boolean closing = false;

    SoundMonitor(Clip c, AudioInputStream s) {
      this.c = c;
      this.s = s;
      this.starting = true;
      c.addLineListener(this);
      open();
    }

    boolean start() {
      boolean ready = !closing && !c.isRunning();
      if (ready) {
        this.c.start();
      }

      return ready;
    }

    boolean stop() {
      this.c.stop();
      flush();
      return true;
    }

    void flush() {
      this.c.flush();
      this.c.setFramePosition(0);
    }

    boolean restart() {
      boolean success = true;
      success &= stop();
      success &= start();
      return success;
    }

    boolean close() {
      boolean ready = !starting && !restarting && !closing;
      if (ready) {
        this.closing = true;
        stop();
        this.c.close();
        c.removeLineListener(this);
      }

      return ready;
    }

    private boolean open() {
      try {
        this.c.open(s);
        return true;
      } catch (LineUnavailableException | IOException e) {
        System.err.println("Unable to open file: " + e.getLocalizedMessage());
        e.printStackTrace();
      }
      return false;
    }

    @Override
    public void update(LineEvent event) {
      Optional<LineEventType> type = LineEventType.getLineEventType(event.getType());
      if (type.isPresent() && !this.restarting) {
        switch (type.get()) {
          case CLOSE:
            if (!closing) {
              System.err.println("Audio Line unexpectedly closed");
            }
            break;
          case OPEN:
            if (!starting) {
              System.err.println("Audio Line unexpectedly reopened");
            } else {
              this.starting = false;
            }
            break;
          case START:
            break;
          case STOP:
            if (!this.closing) {
              restarting = true;
              if (!restart()) {
                System.err.println("Interrupted while restarting playback");
              }
              restarting = false;
            }
        }
      }
    }

    private static enum LineEventType {
      OPEN(LineEvent.Type.OPEN, "Open"), CLOSE(LineEvent.Type.CLOSE,
          "Close"), START(LineEvent.Type.START, "Start"), STOP(LineEvent.Type.STOP, "Stop");

      Type type;
      String name;

      LineEventType(Type type, String name) {
        this.type = type;
        this.name = name;
      }

      Type getType() {
        return this.type;
      }

      static Optional<LineEventType> getLineEventType(Type argtype) {
        for (LineEventType type : LineEventType.values()) {
          if (argtype.equals(type.getType())) {
            return Optional.of(type);

          }
        }

        return Optional.empty();
      }

      @Override
      public String toString() {
        return this.name;
      }
    }
  }
}
