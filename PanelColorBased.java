
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class PanelColorBased extends JPanel
{
    private static final long serialVersionUID = 1L;
    private BufferedImage image;

    // Create a constructor method
    public PanelColorBased()
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
        JFrame GUIframe = new JFrame("BasicPanel");
        GUIframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GUIframe.setSize(400, 400);
        PanelColorBased mainPanelMotionBased = new PanelColorBased();
        GUIframe.setContentPane(mainPanelMotionBased);
        GridLayout layout = new GridLayout(0, 2);
        mainPanelMotionBased.setLayout(layout);
        PanelColorBased camPanelColorBased = new PanelColorBased();
        PanelColorBased subtractPanelColorBased = new PanelColorBased();
        mainPanelMotionBased.add(camPanelColorBased);
        mainPanelMotionBased.add(subtractPanelColorBased);
        GUIframe.setVisible(true);
        GUIframe.setSize(800, 300);
        BufferedImage currBuffImg;
        Mat currBGRFrame = new Mat();
        VideoCapture capture = new VideoCapture(1);

        if (capture.isOpened()) {
            // set resolution
            capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 360);
            capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 240);

            while (true) {
                capture.read(currBGRFrame);
                currBuffImg = matToBufferedImage(currBGRFrame);
                camPanelColorBased.setimage(currBuffImg);
                camPanelColorBased.repaint();
            }
        }
    }
}