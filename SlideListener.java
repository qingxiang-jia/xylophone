import org.opencv.core.Scalar;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Listens change in JSlider, and reflects that change to JLabel.
 */
public class SlideListener implements ChangeListener
{
    JLabel handle;
    int index;
    Scalar bound;

    public void setBound(Scalar bound, int index)
    {
        this.bound = bound;
        this.index = index;
    }

    public SlideListener(JLabel label)
    {
        handle = label;
        bound = null;
        index = 0;
    }

    public void stateChanged(ChangeEvent e)
    {
        JSlider source = (JSlider)e.getSource();
        if (!source.getValueIsAdjusting()) {
            handle.setText(Integer.toString(source.getValue()));
            // update bound for display
            if (bound != null) {
                double[] tmp = bound.val;
                tmp[index] = source.getValue();
                bound.set(tmp);
            }
        }
    }
}
