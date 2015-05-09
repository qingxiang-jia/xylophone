import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Listens change in JSlider, and reflects that change to JLabel.
 */
public class SlideListener implements ChangeListener
{
    JLabel handle;
    public SlideListener(JLabel label)
    {
        handle = label;
    }

    public void stateChanged(ChangeEvent e)
    {
        JSlider source = (JSlider)e.getSource();
        if (!source.getValueIsAdjusting()) {
            handle.setText(Integer.toString(source.getValue()));
        }
    }
}
