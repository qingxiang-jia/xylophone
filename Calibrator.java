import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Stick learning is separated out from the normal procedure.
 * Helps user to find the best thresholds to capture the stick, and right the data to file.
 */
public class Calibrator
{
    public static void main(String arg[]) throws Exception
    {
        /** GUI set up **/
        JFrame GUIframe = new JFrame("BasicPanel");
        GUIframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ColorTrackerSym mainColorTrackerSym = new ColorTrackerSym();
        GUIframe.setContentPane(mainColorTrackerSym);
        GridLayout layout = new GridLayout(2, 2);
        mainColorTrackerSym.setLayout(layout);
        ColorTrackerSym leftPanel = new ColorTrackerSym();
        ColorTrackerSym rightPanel = new ColorTrackerSym();
        mainColorTrackerSym.add(leftPanel);
        mainColorTrackerSym.add(rightPanel);
        JPanel options = new JPanel();

        // set up option panel
        JSlider lowHue, lowSat, lowVal, highHue, highSat, highVal;
        lowHue = new JSlider(0, 180, 0);
        lowSat = new JSlider(0, 255, 0);
        lowVal = new JSlider(0, 255, 0);
        highHue = new JSlider(0, 180, 0);
        highSat = new JSlider(0, 255, 0);
        highVal = new JSlider(0, 255, 0);

        JLabel lowHVal = new JLabel("0");
        JLabel lowSVal = new JLabel("0");
        JLabel lowVVal = new JLabel("0");
        JLabel highHVal = new JLabel("0");
        JLabel highSVal = new JLabel("0");
        JLabel highVVal = new JLabel("0");

        lowHue.setMinorTickSpacing(30);
        lowHue.setMajorTickSpacing(30);
        lowHue.setPaintTicks(true);
        lowHue.setPaintLabels(true);
        SlideListener lowHListener = new SlideListener(lowHVal);
        lowHue.addChangeListener(lowHListener);

        lowSat.setMinorTickSpacing(50);
        lowSat.setMajorTickSpacing(50);
        lowSat.setPaintTicks(true);
        lowSat.setPaintLabels(true);
        SlideListener lowSListener = new SlideListener(lowSVal);
        lowSat.addChangeListener(lowSListener);

        lowVal.setMinorTickSpacing(50);
        lowVal.setMajorTickSpacing(50);
        lowVal.setPaintTicks(true);
        lowVal.setPaintLabels(true);
        SlideListener lowVListener = new SlideListener(lowVVal);
        lowVal.addChangeListener(lowVListener);

        highHue.setMinorTickSpacing(30);
        highHue.setMajorTickSpacing(30);
        highHue.setPaintTicks(true);
        highHue.setPaintLabels(true);
        SlideListener highHListener = new SlideListener(highHVal);
        highHue.addChangeListener(highHListener);

        highSat.setMinorTickSpacing(50);
        highSat.setMajorTickSpacing(50);
        highSat.setPaintTicks(true);
        highSat.setPaintLabels(true);
        SlideListener highSatSListener = new SlideListener(highSVal);
        highSat.addChangeListener(highSatSListener);

        highVal.setMinorTickSpacing(50);
        highVal.setMajorTickSpacing(50);
        highVal.setPaintTicks(true);
        highVal.setPaintLabels(true);
        SlideListener highVListener = new SlideListener(highVVal);
        highVal.addChangeListener(highVListener);

        JLabel lowH = new JLabel("Low H");
        JLabel lowS = new JLabel("Low S");
        JLabel lowV = new JLabel("Low V");
        JLabel highH = new JLabel("High H");
        JLabel highS = new JLabel("High S");
        JLabel highV = new JLabel("High V");

        options.setLayout(new GridLayout(18, 0));

        options.add(lowH);
        options.add(lowHue);
        options.add(lowHVal);

        options.add(lowS);
        options.add(lowSat);
        options.add(lowSVal);

        options.add(lowV);
        options.add(lowVal);
        options.add(lowVVal);

        options.add(highH);
        options.add(highHue);
        options.add(highHVal);

        options.add(highS);
        options.add(highSat);
        options.add(highSVal);

        options.add(highV);
        options.add(highVal);
        options.add(highVVal);

        // button to save calibration results
        JButton saveBtn = new JButton("Save");
        SaveBtnListener saveBtnListener = new SaveBtnListener();
        saveBtn.addActionListener(saveBtnListener);

        mainColorTrackerSym.add(options);
        mainColorTrackerSym.add(saveBtn);
        GUIframe.setVisible(true);
        GUIframe.setSize(800, 1300);

        // mouse listener
        SymMouseAdapter mouseAdapter = new SymMouseAdapter();
        mouseAdapter.attach(leftPanel);
        leftPanel.addMouseListener(mouseAdapter);

        /** img processing **/
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Scalar red = new Scalar(0, 0, 255);
        Scalar green = new Scalar(0, 255, 0);
        Scalar blue = new Scalar(255, 0, 0);

        BufferedImage currBuffImg;

        VideoCapture capture = new VideoCapture(1); // I have two cam, use the snd one

        Mat currBGRFrame = new Mat();
        Mat currHSVFrame = new Mat();

        Mat roi;

        if (capture.isOpened()) {
            // set resolution
            capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 360);
            capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 240);

            /** learn what to track **/
            leftPanel.stage = leftPanel.LEARN_COLOR;
            org.opencv.core.Point sampleBoxUL = new org.opencv.core.Point(30, 30);
            org.opencv.core.Point sampleBoxLR = new org.opencv.core.Point(sampleBoxUL.x + 2 * leftPanel.selectBoxLen,
                    sampleBoxUL.y + 2 * leftPanel.selectBoxLen);

            while (true) {
                while (leftPanel.selecting) {
                    capture.read(currBGRFrame);
                    Core.rectangle(currBGRFrame, leftPanel.mouseUL, leftPanel.mouseLR, green);
                    Core.rectangle(currBGRFrame, sampleBoxUL, sampleBoxLR, green);
                    currBuffImg = ColorTrackerSym.matToBufferedImage(currBGRFrame);
                    leftPanel.setimage(currBuffImg);
                    leftPanel.repaint();
                }

                Thread.sleep(1000);

                /** compute roi HSV ranges **/
//                roi = currBGRFrame.submat((int) leftPanel.mouseUL.y + 2, (int) leftPanel.mouseLR.y - 2,
//                        (int) leftPanel.mouseUL.x + 2, (int) leftPanel.mouseLR.x - 2);
                roi = currBGRFrame.submat((int) leftPanel.mouseUL.y, (int) leftPanel.mouseLR.y,
                        (int) leftPanel.mouseUL.x, (int) leftPanel.mouseLR.x);
                Imgproc.cvtColor(roi, roi, Imgproc.COLOR_BGR2HSV);

                MatOfDouble mean = new MatOfDouble(); // h s v
                MatOfDouble stddev = new MatOfDouble(); // h s v
                Core.meanStdDev(roi, mean, stddev);

                // for readability
                double meanHue = mean.get(0, 0)[0], stddevHue = stddev.get(0, 0)[0];
                double meanSat = mean.get(1, 0)[0], stddevSat = stddev.get(1, 0)[0];
                double meanVal = mean.get(2, 0)[0], stddevVal = stddev.get(2, 0)[0];

                int minHue = (int) (meanHue - 3 * stddevHue);
                int maxHue = (int) (meanHue + 3 * stddevHue);

                int minSat = (int) (meanSat - 3 * stddevSat);
                int maxSat = (int) (meanSat + 3 * stddevSat);

                int minVal = (int) (meanVal - 3 * stddevVal);
                int maxVal = (int) (meanVal + 3 * stddevVal);

                // disallow negative values
                if (minHue < 0)
                    minHue = 0;
                if (maxHue < 0)
                    maxHue = 0;
                if (minSat < 0)
                    minSat = 0;
                if (maxSat < 0)
                    maxSat = 0;
                if (minVal < 0)
                    minVal = 0;
                if (maxVal < 0)
                    maxVal = 0;

                System.out.printf("minHue: %d   maxHue: %d\nminSat: %d   maxSat: %d\nminVal: %d   maxVal: %d\n",
                        minHue, maxHue, minSat, maxSat, minVal, maxVal);

                // update slider bars
                lowHue.setValue(minHue);
                lowSat.setValue(minSat);
                lowVal.setValue(minVal);
                highHue.setValue(maxHue);
                highSat.setValue(maxSat);
                highVal.setValue(maxVal);
                options.validate();
                options.repaint();

                Scalar low = new Scalar(minHue, minSat, minVal);
                Scalar high = new Scalar(maxHue, maxSat, maxVal);

                // update slider bar listeners
                lowHListener.setBound(low, 0);
                lowSListener.setBound(low, 1);
                lowVListener.setBound(low, 2);

                highHListener.setBound(high, 0);
                highHListener.setBound(high, 1);
                highHListener.setBound(high, 2);

                // update saveBtnListener
                saveBtnListener.setBounds(low, high);

                /** tracking **/
                Size erodeSize = new Size(2, 2);
                Size dilateSize = new Size(1, 1);

                double dM01, dM10, dM00;
                org.opencv.core.Point currCentroid = new org.opencv.core.Point(0, 0);
                org.opencv.core.Point lastCentroid = new org.opencv.core.Point(0, 0);

                double direction = 0; // direction the mallet moves
                org.opencv.core.Point hitIndicator = new org.opencv.core.Point(20, 20);
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

                    /** update left canvas **/
                    currBuffImg = ColorTrackerSym.matToBufferedImage(currBGRFrame);
                    leftPanel.setimage(currBuffImg);
                    leftPanel.repaint();

                    /** update right canvas **/
                    currBuffImg = ColorTrackerSym.matToBufferedImage(currHSVFrame);
                    rightPanel.setimage(currBuffImg);
                    rightPanel.repaint();

                    /** update last centroid **/
                    lastCentroid.x = currCentroid.x;
                    lastCentroid.y = currCentroid.y;
                }
            }
        }

    }
}
