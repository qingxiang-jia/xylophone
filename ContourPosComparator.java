import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.Comparator;

/**
 * Sort contours left to right.
 */
public class ContourPosComparator implements Comparator<MatOfPoint>
{
    @Override
    public int compare(MatOfPoint contour1, MatOfPoint contour2)
    {
        // first get the moments of each contour, then get the center of mass, then compare which one is leftmost
        Moments m1 = Imgproc.moments(contour1);
        Moments m2 = Imgproc.moments(contour2);
        int x1 = (int) (m1.get_m10() / m1.get_m00());
        int x2 = (int) (m2.get_m10() / m2.get_m00());
        return x1 - x2;
    }
}
