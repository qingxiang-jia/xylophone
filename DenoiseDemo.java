import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DenoiseDemo
{
    public static void main(String arg[]) throws Exception
    {
        /** GUI set up **/
        JFrame GUIframe = new JFrame("BasicPanel");
        GUIframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GUIframe.setSize(400, 400);
        ColorTrackerSym mainPanelMotionBased = new ColorTrackerSym();
        GUIframe.setContentPane(mainPanelMotionBased);
        GridLayout layout = new GridLayout(0, 2);
        mainPanelMotionBased.setLayout(layout);
        ColorTrackerSym camColorTrackerSym = new ColorTrackerSym();
        ColorTrackerSym binaryColorTrackerSym = new ColorTrackerSym();
        mainPanelMotionBased.add(camColorTrackerSym);
        mainPanelMotionBased.add(binaryColorTrackerSym);
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

        SymMouseAdapter mouseAdapter = new SymMouseAdapter();
        mouseAdapter.attach(camColorTrackerSym);
        camColorTrackerSym.addMouseListener(mouseAdapter);

        Mat currBGRFrame = new Mat();
        Mat currHSVFrame = new Mat();

        Mat roi;

        if (capture.isOpened()) {
            // set resolution
            capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 360);
            capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 240);
            Size frameSize = new Size(360, 240);

            /** De-noise BEGIN **/

            Mat left = Mat.zeros(frameSize, CvType.CV_8UC3);
            Mat right = Mat.zeros(frameSize, CvType.CV_8UC3);
            Mat sum = Mat.zeros(frameSize, CvType.CV_32FC3); // trials & errors, not clearly documented :(

            int cnt = 0;

            while (true) {
                capture.read(right);

                // update right
                currBuffImg = ColorTrackerSym.matToBufferedImage(right);
                binaryColorTrackerSym.setimage(currBuffImg);
                binaryColorTrackerSym.repaint();

                if (cnt != 20) {
                    Imgproc.accumulate(right, sum);
                    cnt++;
                } else {
                    Core.convertScaleAbs(sum, left, 1.0 / 20, 0.0);
                    currBuffImg = ColorTrackerSym.matToBufferedImage(left);
                    camColorTrackerSym.setimage(currBuffImg);
                    camColorTrackerSym.repaint();

                    cnt = 0;
                    sum = Mat.zeros(frameSize, CvType.CV_32FC3);
                }
            }
            /** De-noise END **/
        }
    }
}
