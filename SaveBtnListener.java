import org.opencv.core.Scalar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Saves the calibration results into a file.
 */
public class SaveBtnListener implements ActionListener
{
    Scalar low, high;

    public SaveBtnListener()
    {
        low = null;
        high = null;
    }

    public void setBounds(Scalar low, Scalar high)
    {
        this.low = low;
        this.high = high;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String path = Long.toString(System.currentTimeMillis());
        double[][] bounds = new double[2][];
        bounds[0] = low.val;
        bounds[1] = high.val;
        FileIO.serialize(path, bounds);
    }
}
