
import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class PanelColorBased extends JPanel
{
    public Point mouseUL;
    public Point mouseLR;
    public final int selectBoxLen = 10;
    public boolean selecting = true;

    private static final long serialVersionUID = 1L;
    private BufferedImage image;


    // Create a constructor method
    public PanelColorBased()
    {
        super();
        mouseUL = new Point(0, 0);
        mouseLR = new Point(0, 0);
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

    public int getImageWidth()
    {
        return image.getWidth();
    }

    public int getImageHeight()
    {
        return image.getHeight();
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
        /** GUI set up **/
        JFrame GUIframe = new JFrame("BasicPanel");
        GUIframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GUIframe.setSize(400, 400);
        PanelColorBased mainPanelMotionBased = new PanelColorBased();
        GUIframe.setContentPane(mainPanelMotionBased);
        GridLayout layout = new GridLayout(0, 2);
        mainPanelMotionBased.setLayout(layout);
        PanelColorBased camPanelColorBased = new PanelColorBased();
        PanelColorBased binaryPanelColorBased = new PanelColorBased();
        mainPanelMotionBased.add(camPanelColorBased);
        mainPanelMotionBased.add(binaryPanelColorBased);
        GUIframe.setVisible(true);
        GUIframe.setSize(800, 280);

        /** set up MIDI **/
        MIDI midi = new MIDI();
        midi.run(); // start a new thread

        /** img processing **/
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Scalar red = new Scalar(0, 0, 255);
        Scalar green = new Scalar(0, 255, 0);
        Scalar blue = new Scalar(255, 0, 0);

        BufferedImage currBuffImg;

        VideoCapture capture = new VideoCapture(1);

        PanelMouseAdapter mouseAdapter = new PanelMouseAdapter();
        mouseAdapter.attach(camPanelColorBased);
        camPanelColorBased.addMouseListener(mouseAdapter);

        Mat currBGRFrame = new Mat();
        Mat currHSVFrame = new Mat();

        Mat roi;

        if (capture.isOpened()) {
            // set resolution
            capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 360);
            capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 240);

            /** learn what to track **/
            Point sampleBoxUL = new Point(30, 30);
            Point sampleBoxLR = new Point(sampleBoxUL.x + 2 * camPanelColorBased.selectBoxLen,
                    sampleBoxUL.y + 2 * camPanelColorBased.selectBoxLen);
            while (camPanelColorBased.selecting) {
                capture.read(currBGRFrame);
                Core.rectangle(currBGRFrame, camPanelColorBased.mouseUL, camPanelColorBased.mouseLR, green);
                Core.rectangle(currBGRFrame, sampleBoxUL, sampleBoxLR, green);
                currBuffImg = matToBufferedImage(currBGRFrame);
                camPanelColorBased.setimage(currBuffImg);
                camPanelColorBased.repaint();
            }

            Thread.sleep(1000);

            /** compute roi HSV ranges **/
            roi = currBGRFrame.submat((int) camPanelColorBased.mouseUL.y + 2, (int) camPanelColorBased.mouseLR.y - 2,
                    (int) camPanelColorBased.mouseUL.x + 2, (int) camPanelColorBased.mouseLR.x - 2);
            Imgproc.cvtColor(roi, roi, Imgproc.COLOR_BGR2HSV);

            MatOfDouble mean = new MatOfDouble(); // h s v
            MatOfDouble stddev = new MatOfDouble(); // h s v
            Core.meanStdDev(roi, mean, stddev);

            // for readability
            double meanHue = mean.get(0, 0)[0], stddevHue = stddev.get(0, 0)[0];
            double meanSat = mean.get(1, 0)[0], stddevSat = stddev.get(1, 0)[0];
            double meanVal = mean.get(2, 0)[0], stddevVal = stddev.get(2, 0)[0];

            double minHue = meanHue - 3 * stddevHue;
            double maxHue = meanHue + 3 * stddevHue;

            double minSat = meanSat - 3 * stddevSat;
            double maxSat = meanSat + 3 * stddevSat;

            double minVal = meanVal - 3 * stddevVal;
            double maxVal = meanVal + 3 * stddevVal;

            System.out.printf("minHue: %f   maxHue: %f\nminSat: %f   maxSat: %f\nminVal: %f   maxVal: %f\n",
                    minHue, maxHue, minSat, maxSat, minVal, maxVal);

            Scalar low = new Scalar(minHue, minSat, minVal);
            Scalar high = new Scalar(maxHue, maxSat, maxVal);

            /** tracking **/
            Size erodeSize = new Size(2, 2);
            Size dilateSize = new Size(1, 1);

            double dM01, dM10, dM00;
            Point currCentroid = new Point(0, 0);
            Point lastCentroid = new Point(0, 0);

            double direction = 0; // direction the mallet moves
            Point hitIndicator = new Point(20, 20);
            while (true) {
                capture.read(currBGRFrame);
                Imgproc.cvtColor(currBGRFrame, currHSVFrame, Imgproc.COLOR_BGR2HSV);

                /** thresholding HSV img **/
                Core.inRange(currHSVFrame, low, high, currHSVFrame);

                /** remove noise **/
                Imgproc.erode(currHSVFrame, currHSVFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, erodeSize));
                Imgproc.dilate(currHSVFrame, currHSVFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, dilateSize));

                /** compute centroid **/
                Moments moments = Imgproc.moments(currHSVFrame);
                dM01 = moments.get_m01();
                dM10 = moments.get_m10();
                dM00 = moments.get_m00();
                currCentroid.x = dM10 / dM00;
                currCentroid.y = dM01 / dM00;

                /** stabilize centroid **/
                if ((currCentroid.x - lastCentroid.x) * (currCentroid.x - lastCentroid.x) +
                        (currCentroid.y - lastCentroid.y) * (currCentroid.y - lastCentroid.y)
                        < 20) {
                    currCentroid.x = lastCentroid.x; // just jittering, ignore
                    currCentroid.y = lastCentroid.y;
                }

                /** show centroid **/
                Imgproc.cvtColor(currHSVFrame, currHSVFrame, Imgproc.COLOR_GRAY2BGR);
                Core.circle(currHSVFrame, currCentroid, 5, green, -1);

                /** hit detection **/
                if (direction > 0 && (currCentroid.y - lastCentroid.y < 0)) { // hit
                    midi.sound((int) (currCentroid.x / 360.0 * 77 + 50));
                    Core.circle(currHSVFrame, hitIndicator, 10, red, -1);
                }
                direction = currCentroid.y - lastCentroid.y;
                System.out.println(direction);

                /** update left canvas **/
                currBuffImg = matToBufferedImage(currBGRFrame);
                camPanelColorBased.setimage(currBuffImg);
                camPanelColorBased.repaint();

                /** update right canvas **/
                currBuffImg = matToBufferedImage(currHSVFrame);
                binaryPanelColorBased.setimage(currBuffImg);
                binaryPanelColorBased.repaint();

                /** update last centroid **/
                lastCentroid.x = currCentroid.x;
                lastCentroid.y = currCentroid.y;
            }
        }
    }
}