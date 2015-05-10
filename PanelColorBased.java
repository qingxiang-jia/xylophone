import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.*;

public class PanelColorBased extends JPanel
{
    public Point mouseUL;
    public Point mouseLR;
    public final int selectBoxLen = 10;
    public boolean selecting = true;

    private static final long serialVersionUID = 1L;
    private BufferedImage image;

    public final int LEARN_LAYOUT = 0;
    public final int LEARN_COLOR = 1;
    public int stage;
    public int verticesLearned;
    public Point[] vertices;

    // Create a constructor method
    public PanelColorBased()
    {
        super();
        mouseUL = new Point(0, 0);
        mouseLR = new Point(0, 0);

        verticesLearned = 0;
        vertices = new Point[4];
    }

    public BufferedImage getimage()
    {
        return image;
    }

    public void setimage(BufferedImage newimage)
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
        if (temp == null) return;
        g.drawImage(temp, 10, 10, temp.getWidth(), temp.getHeight(), this);
    }


    public static void main(String arg[]) throws Exception
    {
        /** GUI set up **/
        JFrame GUIframe = new JFrame("BasicPanel");
        GUIframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GUIframe.setSize(400, 400);
        PanelColorBased mainPanelColorBased = new PanelColorBased();
        GUIframe.setContentPane(mainPanelColorBased);
        GridLayout layout = new GridLayout(0, 3);
        mainPanelColorBased.setLayout(layout);
        PanelColorBased camPanelColorBased = new PanelColorBased();
        PanelColorBased binaryPanelColorBased = new PanelColorBased();
        mainPanelColorBased.add(camPanelColorBased);
        mainPanelColorBased.add(binaryPanelColorBased);
        JPanel options = new JPanel();
        JSlider slider = new JSlider(0, 255, 50); // min, max, default
        options.add(slider);
        ConditionButton confirmSlider = new ConditionButton();
        confirmSlider.setText("Looks Good");
        options.add(confirmSlider);
        mainPanelColorBased.add(options);
        GUIframe.setVisible(true);
        GUIframe.setSize(1200, 280);

        /** set up MIDI **/
        MIDI midi = new MIDI();
        midi.run(); // start a new thread

        /** define note **/
        final int C3 = 50, D3 = 70, E3 = 90, F3 = 110, G3 = 130, A4 = 150, B4 = 170, C4 = 190;
        int[] colorToNote = new int[191];
        colorToNote[C3] = 60;
        colorToNote[D3] = 62;
        colorToNote[E3] = 64;
        colorToNote[F3] = 65;
        colorToNote[G3] = 67;
        colorToNote[A4] = 69;
        colorToNote[B4] = 71;
        colorToNote[C4] = 72;

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
            Size frameSize = new Size(360, 240);

            /** learn the xylophone layout (for now, CDEFGABC) **/
            camPanelColorBased.stage = camPanelColorBased.LEARN_LAYOUT; // specify stage

            while (camPanelColorBased.verticesLearned < 4) { // display the video so the user can select paper vertices
                capture.read(currBGRFrame);

                // update GUI
                currBuffImg = matToBufferedImage(currBGRFrame);
                camPanelColorBased.setimage(currBuffImg);
                camPanelColorBased.repaint();
            }

            // four vertices obtained
            Mat paperMask = Mat.zeros(currBGRFrame.size(), CvType.CV_8UC1);

            // connect vertices
            Scalar white8UC1 = new Scalar(255);
            Core.line(paperMask, camPanelColorBased.vertices[0], camPanelColorBased.vertices[1], white8UC1);
            Core.line(paperMask, camPanelColorBased.vertices[1], camPanelColorBased.vertices[2], white8UC1);
            Core.line(paperMask, camPanelColorBased.vertices[2], camPanelColorBased.vertices[3], white8UC1);
            Core.line(paperMask, camPanelColorBased.vertices[0], camPanelColorBased.vertices[3], white8UC1);

            // update GUI
            currBuffImg = matToBufferedImage(paperMask);
            binaryPanelColorBased.setimage(currBuffImg);
            binaryPanelColorBased.repaint();

            Thread.sleep(500); // optional

            // find the contour
            Mat temp = new Mat();
            paperMask.copyTo(temp);
            ArrayList<MatOfPoint> theContour = new ArrayList<>(2);
            Mat tempHierachy = new Mat();
            Imgproc.findContours(temp, theContour, tempHierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            Core.fillConvexPoly(paperMask, theContour.get(0), white8UC1);

            // update GUI
            currBuffImg = matToBufferedImage(paperMask);
            binaryPanelColorBased.setimage(currBuffImg);
            binaryPanelColorBased.repaint();

            /** extract layout **/

            capture.read(currBGRFrame);

            /** capture de-noised image of layout - provide video and slider bar let user decide what's best **/

            Mat binary = new Mat(); // store binary img
            Mat masked = new Mat(); // store masked binary img
            Mat contoursToFind = new Mat(); // store img to be found contours
            Mat hierarchy = new Mat();

            Scalar contourColorGray = new Scalar(new double[]{50.0});
            ArrayList<MatOfPoint> filteredContours = new ArrayList<>(10);
            ContourPosComparator contourPosComparator = new ContourPosComparator();
            Mat filteredContoursDisplay = null;

            Condition learning_layout = new Condition(true);
            // add action listener to confirmSlider
            confirmSlider.setCondition(learning_layout);
            confirmSlider.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    confirmSlider.handle.setVal(false);
                }
            });
            while (learning_layout.val) {
                contourColorGray.val[0] = 50.0;
                filteredContours.clear();

                /** De-noise BEGIN **/
                // visualization: left = sum of right / # of right
                Mat left = Mat.zeros(frameSize, CvType.CV_8UC3);
                Mat right = Mat.zeros(frameSize, CvType.CV_8UC3);
                Mat sum = Mat.zeros(frameSize, CvType.CV_32FC3); // trials & errors, not clearly documented :(

                int cnt = 0;

                while (true) {
                    capture.read(right);
                    if (cnt != 20) {
                        Imgproc.accumulate(right, sum);
                        cnt++;
                    } else {
                        Core.convertScaleAbs(sum, left, 1.0/20, 0.0);
                        break;
                    }
                }

                /** De-noise END **/

                // convert to binary
                Imgproc.cvtColor(left, binary, Imgproc.COLOR_RGB2GRAY);
                Imgproc.threshold(binary, binary, slider.getValue(), 255, Imgproc.THRESH_BINARY_INV); // pixels under thresh becomes maxval

                // apply mask
                binary.copyTo(masked, paperMask);

                // find contours
                masked.copyTo(contoursToFind);
                ArrayList<MatOfPoint> contours = new ArrayList<>();
                Imgproc.findContours(contoursToFind, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                //currBuffImg = matToBufferedImage(masked);
                currBuffImg = matToBufferedImage(masked);
                binaryPanelColorBased.setimage(currBuffImg);
                binaryPanelColorBased.repaint();

                // update GUI, draw filtered contours on it
                for (MatOfPoint contour : contours)
                    if (Imgproc.contourArea(contour) > 400.0) // filter out noise
                        filteredContours.add(contour);
                // sort contours left to right
                Collections.sort(filteredContours, contourPosComparator);
                filteredContoursDisplay = Mat.zeros(contoursToFind.size(), contoursToFind.type());
                for (int i = 0; i < filteredContours.size(); i++) {
                    Imgproc.drawContours(filteredContoursDisplay, filteredContours, i, contourColorGray, -1);
                    contourColorGray.val[0] += 20; // update color
                    if (contourColorGray.val[0] > 255)
                        contourColorGray.val[0] = 50.0;
                } // C:50 D:70 E:90 F:110 G:130 A:150 B:170 C:190
                currBuffImg = matToBufferedImage(filteredContoursDisplay);
                camPanelColorBased.setimage(currBuffImg);
                camPanelColorBased.repaint();
            }

            /**************************************************************************************************
             * Now, done learning layout **********************************************************************
             **************************************************************************************************/

            double[][] lowHigh = (double[][]) FileIO.deserialize("lof_orange");
            Scalar low = new Scalar(lowHigh[0]);
            Scalar high = new Scalar(lowHigh[1]);

            /** tracking **/
            Size erodeSize = new Size(2, 2);
            Size dilateSize = new Size(1, 1);

            double dM01, dM10, dM00;
            Point currCentroid = new Point(0, 0);
            Point lastCentroid = new Point(0, 0);

            double direction = 0; // direction the mallet moves
            Point hitIndicator = new Point(20, 20);
            final int ZERO_VAL = 5;
            int zeroCounter = ZERO_VAL;

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
                        < 30) {
                    currCentroid.x = lastCentroid.x; // just jittering, ignore
                    currCentroid.y = lastCentroid.y;
                }

                /** show centroid **/
                Imgproc.cvtColor(currHSVFrame, currHSVFrame, Imgproc.COLOR_GRAY2BGR);
                Core.circle(currHSVFrame, currCentroid, 5, green, -1);

                /** hit detection **/
                if (currCentroid.y - lastCentroid.y > 0) { // keeps going down
                    zeroCounter = ZERO_VAL;
                    direction = currCentroid.y - lastCentroid.y;
                } else if (currCentroid.y - lastCentroid.y <= 0 && direction > 0) { // switching direction to up
                    midi.sound(colorToNote[(int) (filteredContoursDisplay.get((int) currCentroid.y, (int) currCentroid.x))[0]]);
                    if (filteredContoursDisplay.get((int) currCentroid.y, (int) currCentroid.x)[0] == 0)
                        Core.circle(currHSVFrame, hitIndicator, 10, red, -1);
                    direction = currCentroid.y - lastCentroid.y;
                    zeroCounter = ZERO_VAL;
                } else if (currCentroid.y - lastCentroid.y == 0) {
                    if (zeroCounter != 0) { // if next time it goes up, there is still chance to sound
                        zeroCounter--;
                    } else {
                        direction = 0;
                    }
                }

//                System.out.println(direction);

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