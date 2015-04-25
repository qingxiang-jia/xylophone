
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

//    public static void main(String arg[]) throws Exception
//    {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//        JFrame frame = new JFrame("BasicPanel");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(400, 400);
//        Panel mainPanel = new Panel();
//        frame.setContentPane(mainPanel);
//        GridLayout layout = new GridLayout(0, 2);
//        mainPanel.setLayout(layout);
//        Panel camPanel = new Panel();
//        Panel subtractPanel = new Panel();
//        mainPanel.add(camPanel);
//        mainPanel.add(subtractPanel);
//        frame.setVisible(true);
//        frame.setSize(1300, 500);
//        Mat currRGB = new Mat();
//        Mat nextRGB = new Mat();
//        Mat currGray = new Mat();
//        Mat nextGray = new Mat();
//        Mat diff = new Mat();
//        Mat thresholdImg = new Mat();
//        BufferedImage temp;
//        VideoCapture capture = new VideoCapture(1);
//        if (capture.isOpened()) {
//            // set resulution
//            capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 600);
//            capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 400);
//
//            while (true) {
//
//                capture.read(currRGB);
//                temp = matToBufferedImage(currRGB);
//                camPanel.setimage(temp);
//                camPanel.repaint();
//
//                Imgproc.cvtColor(currRGB, currGray, Imgproc.COLOR_RGB2GRAY);
//
//                capture.read(nextRGB);
//
//
//                Imgproc.cvtColor(nextRGB, nextGray, Imgproc.COLOR_RGB2GRAY); // don't need to keep prevRGB
//                Core.absdiff(currGray, nextGray, diff);
//                Imgproc.threshold(diff, thresholdImg, 40, 255, Imgproc.THRESH_BINARY);
//
//                temp = matToBufferedImage(thresholdImg);
//
//                subtractPanel.setimage(temp);
//                subtractPanel.repaint();
//            }
//        }
//    }

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
        Size blueSize = new Size(2.0, 2.0);

        Mat contourImg = new Mat();
        Mat hierachy = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();

        Scalar red = new Scalar(0, 0, 255);
        Scalar green = new Scalar(0, 255, 0);
        Mat bkg = new Mat();
        Rect bRect = new Rect();
        MatOfPoint2f matOfPointForRectWithAngle;

        if (capture.isOpened()) {
            // set resulution
            capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 360);
            capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 240);

            capture.read(prevRGB);
            Imgproc.cvtColor(prevRGB, prevGray, Imgproc.COLOR_RGB2GRAY);
            Thread.sleep(500);

            while (true) {
                contours.clear();


                capture.read(currRGB);
                temp = matToBufferedImage(currRGB);
                camPanel.setimage(temp);
                camPanel.repaint();

                Imgproc.cvtColor(currRGB, currGray, Imgproc.COLOR_RGB2GRAY);

                Core.absdiff(currGray, prevGray, diff);
                Imgproc.threshold(diff, thresholdImg, 20, 255, Imgproc.THRESH_BINARY);
                Imgproc.blur(thresholdImg, thresholdImg, blueSize);

                /** find contours **/
                Imgproc.findContours(thresholdImg, contours, hierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                thresholdImg.copyTo(bkg);
                Imgproc.cvtColor(bkg, bkg, Imgproc.COLOR_GRAY2RGB);
//                Imgproc.drawContours(bkg, contours, -1, red);

                /** bounding rectangle **/
//                for (MatOfPoint contour : contours) {
//                    bRect = Imgproc.boundingRect(contour);
//                    Core.rectangle(bkg, new Point(bRect.x, bRect.y), new Point(bRect.x + bRect.width, bRect.y + bRect.height), red);
//                }

                /** bounding rectangle with angle - draw largest **/
//                RotatedRect largestRect = null;
//                for (MatOfPoint contour : contours) {
//                    matOfPointForRectWithAngle = new MatOfPoint2f(contour.toArray());
//                    RotatedRect rRect = Imgproc.minAreaRect(matOfPointForRectWithAngle);
//                    if (largestRect == null)
//                        largestRect = rRect;
//                    else if (rRect.size.area() > largestRect.size.area())
//                        largestRect = rRect;
//                }
//                // draw the largest rectangle
//                if (largestRect != null) {
//                    Point[] pts = new Point[4];
//                    largestRect.points(pts);
//                    for(int i = 0; i < 4; i++)
//                        Core.line(bkg, pts[i], pts[(i + 1) % 4], green);
//                    Core.circle(bkg, largestRect.center, 5, red, -1);
//                }

                /** bounding rectangle with angle - draw large ones **/
                for (MatOfPoint contour : contours) {
                    matOfPointForRectWithAngle = new MatOfPoint2f(contour.toArray());
                    RotatedRect rRect = Imgproc.minAreaRect(matOfPointForRectWithAngle);
                    if (rRect.size.area() > 200) {
                        Point[] pts = new Point[4];
                        rRect.points(pts);
                        for(int i = 0; i < 4; i++)
                            Core.line(bkg, pts[i], pts[(i + 1) % 4], green);
                        Core.circle(bkg, rRect.center, 5, red, -1);
                    }
                }

                temp = matToBufferedImage(bkg);
                subtractPanel.setimage(temp);
                subtractPanel.repaint();

                prevRGB = currRGB;
                Imgproc.cvtColor(prevRGB, prevGray, Imgproc.COLOR_RGB2GRAY);
            }
        }
    }
}