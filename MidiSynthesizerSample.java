import javax.sound.midi.*;

public class MidiSynthesizerSample {
    public static void main(String[] args) {
        int[] notes = new int[]{60, 62, 64, 65, 67, 69, 71, 72, 72, 71, 69, 67, 65, 64, 62, 60};
        try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel channel = synthesizer.getChannels()[0];

            for (int note : notes) {
                channel.noteOn(note, 500);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    break;
                } finally {
                    channel.noteOff(note);
                }
            }
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }
}