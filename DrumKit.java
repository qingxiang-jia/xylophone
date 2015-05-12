import org.jfugue.pattern.Pattern;
import org.jfugue.player.Player;
import org.jfugue.realtime.RealtimePlayer;
import org.jfugue.rhythm.Rhythm;
import org.jfugue.theory.Note;

import javax.sound.midi.MidiUnavailableException;
import java.util.Random;
import java.util.Scanner;

public class DrumKit implements Runnable
{
    private static final Pattern BASS = new Pattern("V9 [BASS_DRUM]q");
    private static final Pattern SNARE = new Pattern("V9 [ELECTRIC_SNARE]q");
    private static final Pattern CYMBAL = new Pattern("V9 [CHINESE_CYMBAL]q");

    Player player;

    public DrumKit()
    {
        player = new Player();
    }

    public void run()
    {}

    public void sound(int instrument)
    {
        if (instrument == 0) {
            player.play(BASS);
        } else if (instrument == 1) {
            player.play(SNARE);
        } else {
            player.play(CYMBAL);
        }
    }
}