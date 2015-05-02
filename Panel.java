
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Panel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private BufferedImage image;

    // Create a constructor method
    public Panel()
    {
        super();
    }

    private BufferedImage getimage()
    {
        return image;
    }

    private void setimage(BufferedImage newimage)
    {
        image = newimage;
        return;
    }

    /**
     * Converts/writes a Mat into a BufferedImage.
     *
     * @param matrix Mat of type CV_8UC3 or CV_8UC1
     * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
     */
    public static BufferedImage matToBufferedImage(Mat matrix)
    {
        int cols = matrix.cols();
        int rows = matrix.rows();
        int elemSize = (int) matrix.elemSize();
        byte[] data = new byte[cols * rows * elemSize];
        int type;
        matrix.get(0, 0, data);
        switch (matrix.channels()) {
            case 1:
                type = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case 3:
                type = BufferedImage.TYPE_3BYTE_BGR;
                // bgr to rgb
                byte b;
                for (int i = 0; i < data.length; i = i + 3) {
                    b = data[i];
                    data[i] = data[i + 2];
                    data[i + 2] = b;
                }
                break;
            default:
                return null;
        }
        BufferedImage image2 = new BufferedImage(cols, rows, type);
        image2.getRaster().setDataElements(0, 0, cols, rows, data);
        return image2;
    }

    public void paintComponent(Graphics g)
    {
        BufferedImage temp = getimage();
        if (temp == null) return ;
        g.drawImage(temp, 10, 10, temp.getWidth(), temp.getHeight(), this);
    }


    public static void main(String arg[]) throws Exception
    {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        JFrame frame = new JFrame("BasicPanel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        Panel mainPanel = new Panel();
        frame.setContentPane(mainPanel);
        GridLayout layout = new GridLayout(0, 2);
        mainPanel.setLayout(layout);
        Panel camPanel = new Panel();
        Panel subtractPanel = new Panel();
        mainPanel.add(camPanel);
        mainPanel.add(subtractPanel);
        frame.setVisible(true);
        frame.setSize(800, 300);
        Mat currRGB = new Mat();
        Mat currGray = new Mat();
        Mat prevRGB = new Mat();
        Mat prevGray = new Mat();
        Mat diff = new Mat();
        Mat thresholdImg = new Mat();
        BufferedImage temp;
        VideoCapture capture = new VideoCapture(1);
        Size blurSize = new Size(2.0, 2.0);

        Mat contourImg = new Mat();
        Mat hierachy = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        ArrayList<MatOfPoint> filteredContours = new ArrayList<>();

        Scalar red = new Scalar(0, 0, 255);
        Scalar green = new Scalar(0, 255, 0);
        Scalar blue = new Scalar(255, 0, 0);
        Mat bkg = new Mat();
        Rect bRect = new Rect();
        MatOfPoint2f matOfPointForRectWithAngle;

        Point hitIndicator = new Point();
        hitIndicator.x = 20;
        hitIndicator.y = 20;

        ArrayList<RotatedRect> lastFrameRRects = new ArrayList<>();
        ArrayList<RotatedRect> rRects = new ArrayList<>();
        double dY1 = 0, dY2 = 0;

        if (capture.isOpened()) {
            // set resolution
            capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 360);
            capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 240);

            capture.read(prevRGB);
            Imgproc.cvtColor(prevRGB, prevGray, Imgproc.COLOR_RGB2GRAY);
            Thread.sleep(500);

            boolean empty = true;

            while (true) {
                contours.clear();
                filteredContours.clear();
                rRects.clear();

                capture.read(currRGB);
                temp = matToBufferedImage(currRGB);
                camPanel.setimage(temp);
                camPanel.repaint();

                Imgproc.cvtColor(currRGB, currGray, Imgproc.COLOR_RGB2GRAY);

                Core.absdiff(currGray, prevGray, diff);
                Imgproc.threshold(diff, thresholdImg, 20, 255, Imgproc.THRESH_BINARY);
//                Imgproc.blur(thresholdImg, thresholdImg, blurSize);

                /** find contours **/
                Imgproc.findContours(thresholdImg, contours, hierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                /** only keep big enough contours **/
                // iterating contours, drop the small ones
                for (MatOfPoint contour : contours) {
                    if (Imgproc.contourArea(contour) >= 200.0)
                        filteredContours.add(contour);
                }

                thresholdImg.copyTo(bkg);
                Imgproc.cvtColor(bkg, bkg, Imgproc.COLOR_GRAY2RGB);
//                Imgproc.drawContours(bkg, contours, -1, red);


//                /** bounding rectangle with angle **/
                for (MatOfPoint contour : filteredContours) {
                    matOfPointForRectWithAngle = new MatOfPoint2f(contour.toArray());
                    RotatedRect rRect = Imgproc.minAreaRect(matOfPointForRectWithAngle);

                    rRects.add(rRect);

                    /** draw bounding rectangle **/
                    Point[] pts = new Point[4];
                    rRect.points(pts);
                    for(int i = 0; i < 4; i++)
                        Core.line(bkg, pts[i], pts[(i + 1) % 4], green);
                    Core.circle(bkg, rRect.center, 5, red, -1);
//                    Core.putText(bkg, "1", rRect.center, 1, 1.0, green);
                }
//
//
//                // crude hit indicator
                if (rRects.size() != 0) {
                    // for now deals with only one stick
                    RotatedRect currRRect = rRects.get(0);
                    empty = false;
                    RotatedRect closestRRect = null;
                    for (RotatedRect lastFrameRRect : lastFrameRRects) { // find the closest rectangle in last frame
                        if (closestRRect == null) {
                            closestRRect = lastFrameRRect;
                        } else {
                            if (((currRRect.center.x - lastFrameRRect.center.x) * (currRRect.center.x - lastFrameRRect.center.x))
                                    + ((currRRect.center.y - lastFrameRRect.center.y) * (currRRect.center.y - lastFrameRRect.center.y))
                                    < ((currRRect.center.x - closestRRect.center.x) * (currRRect.center.x - closestRRect.center.x))
                                    + ((currRRect.center.y - closestRRect.center.y) * (currRRect.center.y - closestRRect.center.y)))
                                closestRRect = lastFrameRRect;
                        }
                    }
                    if (closestRRect != null) {
                        if (dY1 > 0 && currRRect.center.y - closestRRect.center.y < 0) {
                            Core.circle(bkg, hitIndicator, 10, green, -1);
                            System.out.println("hit - direction change");
                        }
                        dY1 = currRRect.center.y - closestRRect.center.y;
                        System.out.println(currRRect.center.y);
                    }
                }
                if (filteredContours.size() == 0 && !empty) { // is a hit at current frame
                    // if change in y is positive, then don't count this as a hit -- just move up the stick
                    // for now deals with only one stick
                    empty = true;
                    if (dY1 > 0) {
                        Core.circle(bkg, hitIndicator, 10, green, -1);
                        System.out.println("hit - static");
                        dY1 = 0;
                    }
                }
//

                temp = matToBufferedImage(bkg);
                subtractPanel.setimage(temp);
                subtractPanel.repaint();

                prevRGB = currRGB;
                Imgproc.cvtColor(prevRGB, prevGray, Imgproc.COLOR_RGB2GRAY);

                lastFrameRRects = new ArrayList<>(rRects);
            }
        }
    }
}