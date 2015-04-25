import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The class that produces sound of a xylophone.
 */
public class MIDIGUIExample
{
    public static void main(String[] args) throws Exception
    {
        /** gui **/
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        System.out.println("1");
        JPanel pane = new JPanel();
        System.out.println("2");
        JButton button1 = new JButton("Click me!");
        System.out.println("3");
        frame.getContentPane().add(pane);
        System.out.println("4");
        pane.add(button1);
        System.out.println("5");
        frame.pack();
        System.out.println("6");
        frame.setVisible(true);
        System.out.println("GUI done");

        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();
        final MidiChannel[] mc = synth.getChannels();
        Instrument[] instr = synth.getDefaultSoundbank().getInstruments();
        synth.loadInstrument(instr[90]);
        System.out.println("MIDI done");

        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mc[5].noteOn(60, 600);
                System.out.println("ActionListener done");
            }
        });
    }
}
