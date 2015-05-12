import org.jfugue.pattern.Pattern;
import org.jfugue.player.Player;
import org.jfugue.realtime.RealtimePlayer;
import org.jfugue.rhythm.Rhythm;
import org.jfugue.theory.Note;

import javax.sound.midi.MidiUnavailableException;
import java.util.Random;
import java.util.Scanner;

public class RealtimeExample
{
    public static void main(String[] args) throws MidiUnavailableException {
        RealtimePlayer player = new RealtimePlayer();
        Scanner scanner = new Scanner(System.in);
        boolean quit = false;
        player.changeInstrument(62);
        while (quit == false) {
            String entry = scanner.next();
            if (entry.startsWith("+")) {
                player.startNote(new Note(entry.substring(1)));
            }
            else if (entry.startsWith("-")) {
                player.stopNote(new Note(entry.substring(1)));
            }
            else if (entry.equalsIgnoreCase("q")) {
                quit = true;
            }
        }
        scanner.close();
        player.close();
    }
}