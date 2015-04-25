
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

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
        frame.setSize(1300, 500);
        Mat currRGB = new Mat();
        Mat currGray = new Mat();
        Mat prevRGB = new Mat();
        Mat prevGray = new Mat();
        Mat diff = new Mat();
        Mat thresholdImg = new Mat();
        BufferedImage temp;
        VideoCapture capture = new VideoCapture(1);
        if (capture.isOpened()) {
            // set resulution
            capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 600);
            capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 400);

            capture.read(prevRGB);
            Imgproc.cvtColor(prevRGB, prevGray, Imgproc.COLOR_RGB2GRAY);
            Thread.sleep(500);

            while (true) {
                capture.read(currRGB);
                temp = matToBufferedImage(currRGB);
                camPanel.setimage(temp);
                camPanel.repaint();

                Imgproc.cvtColor(currRGB, currGray, Imgproc.COLOR_RGB2GRAY);

                Core.absdiff(currGray, prevGray, diff);
                Imgproc.threshold(diff, thresholdImg, 20, 255, Imgproc.THRESH_BINARY);


                temp = matToBufferedImage(thresholdImg);
                subtractPanel.setimage(temp);
                subtractPanel.repaint();

                prevRGB = currRGB;
                Imgproc.cvtColor(prevRGB, prevGray, Imgproc.COLOR_RGB2GRAY);
            }
        }
    }
}