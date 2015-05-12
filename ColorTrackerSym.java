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

public class ColorTrackerSym extends JPanel
{
    public Point mouseUL;
    public Point mouseLR;
    public final int selectBoxLen = 2;
    public boolean selecting = true;

    private static final long serialVersionUID = 1L;
    private BufferedImage image;

    public final int LEARN_LAYOUT = 0;
    public final int LEARN_COLOR = 1;
    public int stage;
    public int verticesLearned;
    public Point[] vertices;

    // Create a constructor method
    public ColorTrackerSym()
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
        ColorTrackerSym mainColorTrackerSym = new ColorTrackerSym();
        GUIframe.setContentPane(mainColorTrackerSym);
        GridLayout layout = new GridLayout(0, 3);
        mainColorTrackerSym.setLayout(layout);
        ColorTrackerSym camColorTrackerSym = new ColorTrackerSym();
        ColorTrackerSym binaryColorTrackerSym = new ColorTrackerSym();
        mainColorTrackerSym.add(camColorTrackerSym);
        mainColorTrackerSym.add(binaryColorTrackerSym);
        JPanel options = new JPanel();
        JSlider slider = new JSlider(0, 255, 50); // min, max, default
        options.add(slider);
        ConditionButton confirmSlider = new ConditionButton();
        confirmSlider.setText("Looks Good");
        options.add(confirmSlider);
        String[] layoutLst = new String[]{"Xylophone Standard", "Xylophone Extended", "Drum Set"};
        JComboBox layoutList = new JComboBox<>(layoutLst);
        layoutList.setSelectedIndex(0);
        options.add(layoutList);
        mainColorTrackerSym.add(options);
        GUIframe.setVisible(true);
        GUIframe.setSize(1200, 280);

        /** set up MIDI **/
        MIDI midi = new MIDI();
        boolean isDrum = false;
        midi.run(); // start a new thread

        /** define note **/
        final int C3 = 50, D3 = 60, E3 = 70, F3 = 80, G3 = 90, A4 = 100, B4 = 110, C4 = 120; // basic layout
        final int D4 = 130, E4 = 140, F4 = 150, G4 = 160, A5 = 170; // extended layout
        int[] colorToNote = new int[171];
        colorToNote[C3] = 60;
        colorToNote[D3] = 62;
        colorToNote[E3] = 64;
        colorToNote[F3] = 65;
        colorToNote[G3] = 67;
        colorToNote[A4] = 69;
        colorToNote[B4] = 71;
        colorToNote[C4] = 72;
        colorToNote[D4] = 74; // beginning of extended layout
        colorToNote[E4] = 76;
        colorToNote[F4] = 77;
        colorToNote[G4] = 79;
        colorToNote[A5] = 81;

        // for drum set
        int colorToNoteDrum[] = new int[101];
        colorToNoteDrum[C3] = Instruments.REVERSE_CYMBAL;
        colorToNoteDrum[D3] = Instruments.SLAP_BASS_1;
        colorToNoteDrum[E3] = Instruments.MELODIC_TOM;
        colorToNoteDrum[F3] = Instruments.MELODIC_TOM;
        colorToNoteDrum[G3] = Instruments.TRUMPET;
        colorToNoteDrum[A4] = Instruments.REVERSE_CYMBAL;

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
        Mat frame1 = new Mat();
        Mat frame2 = new Mat();

        Mat roi;

        if (capture.isOpened()) {
            // set resolution
            capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 360);
            capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 240);
            Size frameSize = new Size(360, 240);

            /** learn the xylophone layout (for now, CDEFGABC) **/
            camColorTrackerSym.stage = camColorTrackerSym.LEARN_LAYOUT; // specify stage

            while (camColorTrackerSym.verticesLearned < 4) { // display the video so the user can select paper vertices
                capture.read(currBGRFrame);
                Core.flip(currBGRFrame, currBGRFrame, 1);

                // update GUI
                currBuffImg = matToBufferedImage(currBGRFrame);
                camColorTrackerSym.setimage(currBuffImg);
                camColorTrackerSym.repaint();
            }

            // four vertices obtained
            Mat paperMask = Mat.zeros(currBGRFrame.size(), CvType.CV_8UC1);

            // connect vertices
            Scalar white8UC1 = new Scalar(255);
            Core.line(paperMask, camColorTrackerSym.vertices[0], camColorTrackerSym.vertices[1], white8UC1);
            Core.line(paperMask, camColorTrackerSym.vertices[1], camColorTrackerSym.vertices[2], white8UC1);
            Core.line(paperMask, camColorTrackerSym.vertices[2], camColorTrackerSym.vertices[3], white8UC1);
            Core.line(paperMask, camColorTrackerSym.vertices[0], camColorTrackerSym.vertices[3], white8UC1);

            // update GUI
            currBuffImg = matToBufferedImage(paperMask);
            binaryColorTrackerSym.setimage(currBuffImg);
            binaryColorTrackerSym.repaint();

            Thread.sleep(500); // optional

            // find the contour
            Mat temp = new Mat();
            paperMask.copyTo(temp);
            ArrayList<MatOfPoint> theContour = new ArrayList<>(2);
            Mat tempHierarchy = new Mat();
            Imgproc.findContours(temp, theContour, tempHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            Core.fillConvexPoly(paperMask, theContour.get(0), white8UC1);

            // update GUI
            currBuffImg = matToBufferedImage(paperMask);
            binaryColorTrackerSym.setimage(currBuffImg);
            binaryColorTrackerSym.repaint();

            /** extract layout **/

            capture.read(currBGRFrame);
            Core.flip(currBGRFrame, currBGRFrame, 1);

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
                    Core.flip(right, right, 1);

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
                binaryColorTrackerSym.setimage(currBuffImg);
                binaryColorTrackerSym.repaint();

                // update GUI, draw filtered contours on it
                for (MatOfPoint contour : contours)
                    if (Imgproc.contourArea(contour) > 400.0) // filter out noise
                        filteredContours.add(contour);
                // sort contours left to right
                Collections.sort(filteredContours, contourPosComparator);
                filteredContoursDisplay = Mat.zeros(contoursToFind.size(), contoursToFind.type());
                for (int i = 0; i < filteredContours.size(); i++) {
                    Imgproc.drawContours(filteredContoursDisplay, filteredContours, i, contourColorGray, -1);
                    contourColorGray.val[0] += 10; // update color
                    if (contourColorGray.val[0] > 255)
                        contourColorGray.val[0] = 50.0;
                } // C:50 D:70 E:90 F:110 G:130 A:150 B:170 C:190
                currBuffImg = matToBufferedImage(filteredContoursDisplay);
                camColorTrackerSym.setimage(currBuffImg);
                camColorTrackerSym.repaint();
            }

            /**************************************************************************************************
             * Now, done learning layout **********************************************************************
             **************************************************************************************************/

            double[][] lowHigh1 = (double[][]) FileIO.deserialize("orange_vic2");
            double[][] lowHigh2 = (double[][]) FileIO.deserialize("red_vic2"); // enables snd mallet
//            double[][] lowHigh2 = null;
            Scalar low1 = new Scalar(lowHigh1[0]);
            Scalar high1 = new Scalar(lowHigh1[1]);
            Scalar low2 = null;
            Scalar high2 = null;
            if (lowHigh2 != null) {
                low2 = new Scalar(lowHigh2[0]);
                high2 = new Scalar(lowHigh2[1]);
            }

            /** tracking **/
            Size erodeSize = new Size(2, 2);
            Size dilateSize = new Size(1, 1);

            Point currCentroid1 = new Point(0, 0);
            Point lastCentroid1 = new Point(0, 0);
            Point currCentroid2 = null;
            Point lastCentroid2 = null;
            if (lowHigh2 != null) {
                currCentroid2 = new Point(0, 0);
                lastCentroid2 = new Point(0, 0);
            }

            double direction1 = 0; // direction the mallet moves
            double direction2 = 0;
            int numOfUps1 = 0;
            int numOfUps2 = 0;
            Point hitIndicator = new Point(20, 20);
            final int ZERO_VAL = 5;
            int zeroCounter1 = ZERO_VAL, zeroCounter2 = ZERO_VAL;
            int keyColorVal1 = 0, keyColorVal2 = 0;
            int row = 0, col = 0;
            int R = (int) filteredContoursDisplay.size().height, C = (int) filteredContoursDisplay.size().width;
            double[] RBGWhite = new double[]{255.0, 255.0, 255.0};

            Mat hierarchy1 = new Mat();
            Mat hierarchy2 = new Mat();
            ArrayList<MatOfPoint> contours1 = new ArrayList<>();
            ArrayList<MatOfPoint> contours2 = new ArrayList<>();
            MatOfPoint candidate1 = null;
            MatOfPoint candidate2 = null;
            ArrayList<MatOfPoint> dummyArr1 = new ArrayList<>(1);
            ArrayList<MatOfPoint> dummyArr2 = new ArrayList<>(1);
            double contourArea = 0;

            boolean mallet1Gone = true;
            boolean mallet2Gone = true;

            while (true) {
                contours1.clear();
                contours2.clear();
                dummyArr1.clear();
                dummyArr2.clear();

                capture.read(currBGRFrame);
                Core.flip(currBGRFrame, currBGRFrame, 1);
                Imgproc.cvtColor(currBGRFrame, currHSVFrame, Imgproc.COLOR_BGR2HSV);
                currHSVFrame.copyTo(frame1);
                if (lowHigh2 != null)
                    currHSVFrame.copyTo(frame2);

                /** thresholding HSV img **/
                Core.inRange(frame1, low1, high1, frame1);
                if (lowHigh2 != null)
                    Core.inRange(frame2, low2, high2, frame2);

                /** remove noise **/
                Imgproc.erode(frame1, frame1, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, erodeSize));
                Imgproc.dilate(frame1, frame1, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, dilateSize));

                // find and keep biggest contour
                Imgproc.findContours(frame1, contours1, hierarchy1, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                for (MatOfPoint contour : contours1) {
                    if (Imgproc.contourArea(contour) > contourArea) {
                        candidate1 = contour;
                        contourArea = Imgproc.contourArea(contour);
                    }
                }
                if (candidate1 != null && contourArea > 10) {
                    dummyArr1.add(candidate1);
                    frame1 = Mat.zeros(frame1.size(), frame1.type());
                    Imgproc.drawContours(frame1, dummyArr1, 0, white8UC1, -1);
                    mallet1Gone = false;
                } else {
                    mallet1Gone = true;
                }
                contourArea = 0;


                if (lowHigh2 != null) {
                    Imgproc.erode(frame2, frame2, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, erodeSize));
                    Imgproc.dilate(frame2, frame2, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, dilateSize));

                    Imgproc.findContours(frame2, contours2, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                    for (MatOfPoint contour : contours2) {
                        if (Imgproc.contourArea(contour) > contourArea) {
                            candidate2 = contour;
                            contourArea = Imgproc.contourArea(contour);
                        }
                    }
                    if (candidate2 != null && contourArea > 10) {
                        dummyArr2.add(candidate2);
                        frame2 = Mat.zeros(frame2.size(), frame2.type());
                        Imgproc.drawContours(frame2, dummyArr2, 0, white8UC1, -1);
                        mallet2Gone = false;
                    } else {
                        mallet2Gone = true;
                    }
                    contourArea = 0;
                }

                /** compute centroid **/
                Moments moments1 = Imgproc.moments(frame1);
                currCentroid1.x = moments1.get_m10() / moments1.get_m00();
                currCentroid1.y = moments1.get_m01() / moments1.get_m00();


                if (lowHigh2 != null) {
                    Moments moments2 = Imgproc.moments(frame2);
                    currCentroid2.x = moments2.get_m10() / moments2.get_m00();
                    currCentroid2.y = moments2.get_m01() / moments2.get_m00();
                }

                /** stabilize centroid **/
                if ((currCentroid1.x - lastCentroid1.x) * (currCentroid1.x - lastCentroid1.x) +
                        (currCentroid1.y - lastCentroid1.y) * (currCentroid1.y - lastCentroid1.y)
                        < 20) {
                    currCentroid1.x = lastCentroid1.x; // just jittering, ignore
                    currCentroid1.y = lastCentroid1.y;
                }
                if (lowHigh2 != null) {
                    if ((currCentroid2.x - lastCentroid2.x) * (currCentroid2.x - lastCentroid2.x) +
                            (currCentroid2.y - lastCentroid2.y) * (currCentroid2.y - lastCentroid2.y)
                            < 20) {
                        currCentroid2.x = lastCentroid2.x; // just jittering, ignore
                        currCentroid2.y = lastCentroid2.y;
                    }
                }

                // temp todo
                currBuffImg = matToBufferedImage(frame2);
                camColorTrackerSym.setimage(currBuffImg);
                camColorTrackerSym.repaint();

                /** show centroid **/
                filteredContoursDisplay.copyTo(currHSVFrame);
                Imgproc.cvtColor(currHSVFrame, currHSVFrame, Imgproc.COLOR_GRAY2BGR);
                if (!mallet1Gone)
                    Core.circle(currHSVFrame, currCentroid1, 5, green, -1);
                if (lowHigh2 != null && !mallet2Gone)
                    Core.circle(currHSVFrame, currCentroid2, 5, blue, -1);

                /** hit detection **/
                if (!mallet1Gone)
                {
                    if (currCentroid1.y - lastCentroid1.y > 0) { // keeps going down
                        zeroCounter1 = ZERO_VAL;
                        direction1 = currCentroid1.y - lastCentroid1.y;
//                    numOfUps1++;
                    } else if (currCentroid1.y - lastCentroid1.y <= 0 && direction1 > 0) { // switching direction to up
                        if (layoutList.getSelectedIndex() != 2) {
                            midi.sound(colorToNote[(int) (filteredContoursDisplay.get((int) currCentroid1.y, (int) currCentroid1.x))[0]]);
                        } else {
                            midi.soundDrumKit(colorToNoteDrum[(int) (filteredContoursDisplay.get((int) currCentroid1.y, (int) currCentroid1.x))[0]]);
                        }
                        if (filteredContoursDisplay.get((int) currCentroid1.y, (int) currCentroid1.x)[0] == 0)
                            Core.circle(currHSVFrame, hitIndicator, 10, red, -1);
                        direction1 = currCentroid1.y - lastCentroid1.y;
                        zeroCounter1 = ZERO_VAL;
//                    numOfUps1 = 0;
                        // highlight on hitting note
                        keyColorVal1 = (int) (filteredContoursDisplay.get((int) currCentroid1.y, (int) currCentroid1.x))[0];
                        if (keyColorVal1 != 0) {
                            for (row = 0; row < R; row++)
                                for (col = 0; col < C; col++) {
                                    if (currHSVFrame.get(row, col)[0] == keyColorVal1)
                                        currHSVFrame.put(row, col, RBGWhite);
                                }
                        }
                    } else if (currCentroid1.y - lastCentroid1.y == 0) {
                        if (zeroCounter1 != 0) { // if next time it goes up, there is still chance to sound
                            zeroCounter1--;
                        } else {
                            direction1 = 0;
//                        numOfUps1 = 0;
                        }
                    }
                }

                if (lowHigh2 != null && !mallet2Gone) {
                    if (currCentroid2.y - lastCentroid2.y > 0) { // keeps going down
                        zeroCounter2 = ZERO_VAL;
                        direction2 = currCentroid2.y - lastCentroid2.y;
//                        numOfUps2++;
                    } else if (currCentroid2.y - lastCentroid2.y <= 0 && direction2 > 0) { // switching direction to up
                        midi.sound(colorToNote[(int) (filteredContoursDisplay.get((int) currCentroid2.y, (int) currCentroid2.x))[0]]);
                        if (filteredContoursDisplay.get((int) currCentroid2.y, (int) currCentroid2.x)[0] == 0)
                            Core.circle(currHSVFrame, hitIndicator, 10, red, -1);
                        direction2 = currCentroid2.y - lastCentroid2.y;
                        zeroCounter2 = ZERO_VAL;
//                        numOfUps2 = 0;
                        // highlight on hitting note
                        keyColorVal2 = (int) (filteredContoursDisplay.get((int) currCentroid2.y, (int) currCentroid2.x))[0];
                        if (keyColorVal2 != 0) {
                            for (row = 0; row < R; row++)
                                for (col = 0; col < C; col++) {
                                    if (currHSVFrame.get(row, col)[0] == keyColorVal2)
                                        currHSVFrame.put(row, col, RBGWhite);
                                }
                        }
                    } else if (currCentroid2.y - lastCentroid2.y == 0) {
                        if (zeroCounter2 != 0) { // if next time it goes up, there is still chance to sound
                            zeroCounter2--;
                        } else {
                            direction2 = 0;
//                            numOfUps2 = 0;
                        }
                    }
                }
//                System.out.println(direction2);

                /** update left canvas **/
//                currBuffImg = matToBufferedImage(currBGRFrame);
//                camColorTrackerSym.setimage(currBuffImg);
//                camColorTrackerSym.repaint();

                /** update right canvas **/
                currBuffImg = matToBufferedImage(currHSVFrame);
                binaryColorTrackerSym.setimage(currBuffImg);
                binaryColorTrackerSym.repaint();

                /** update last centroid **/
                lastCentroid1.x = currCentroid1.x;
                lastCentroid1.y = currCentroid1.y;

                if (lowHigh2 != null) {
                    lastCentroid2.x = currCentroid2.x;
                    lastCentroid2.y = currCentroid2.y;
                }
            }
        }
    }
}