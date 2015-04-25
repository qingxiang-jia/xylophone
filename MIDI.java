import javax.sound.midi.*;

/**
 * The class that produces sound of a xylophone.
 */
public class MIDI implements Runnable
{
    Synthesizer synth;
    MidiChannel[] channel;
    Instrument[] instruments;

    public MIDI()
    {
        try {
            synth = MidiSystem.getSynthesizer();
        } catch (MidiUnavailableException e) {
            System.out.println("Cannot get MIDI synthesizer.");
            return ;
        }
        try {
            synth.open();
        } catch (MidiUnavailableException e) {
            System.out.println("Cannot open MIDI synthesizer.");
            return ;
        }
        channel = synth.getChannels();
        instruments = synth.getDefaultSoundbank().getInstruments();
        setInstrument(Instruments.XYLOPHONE); // default is xylophone
    }

    public void run()
    {

    }

    public void finalize()
    {
        if (synth.isOpen())
            synth.close();
        try {
            super.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void setInstrument(int instrumentID)
    {
        channel[0].programChange(instrumentID);
    }

    public void sound(int note) throws Exception
    {
        channel[0].noteOn(note, 150);
    }
}
