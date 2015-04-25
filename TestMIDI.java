import java.util.Scanner;

/**
 * Test MIDI as a runnable.
 */
public class TestMIDI
{
    public static void main(String[] args) throws Exception
    {
        Scanner scan = new Scanner(System.in);
        MIDI midi = new MIDI();
        midi.run();
        while (true) {
            if (scan.nextInt() == -1)
                System.exit(0);
            else {
                midi.setInstrument(scan.nextInt());
                midi.sound(60);
            }
        }
    }
}
