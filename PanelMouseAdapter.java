import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Captures mouseUL coordinates with respect to the attached panel.
 */
public class PanelMouseAdapter extends MouseAdapter
{
    PanelColorBased master;

    @Override
    public void mouseClicked(MouseEvent e)
    {
        int centerX = e.getX() - 10, centerY = e.getY() - 10; // JFrame-canvas offset

        master.mouseUL.x = centerX - master.selectBoxLen;
        master.mouseUL.y = centerY - master.selectBoxLen;
        master.mouseLR.x = centerX + master.selectBoxLen;
        master.mouseLR.y = centerY + master.selectBoxLen;
        selectBoxInCanvas(); // make sure the region selected is within the canvas
        master.selecting = false;
    }

    private void selectBoxInCanvas()
    {
        int maxWidth = master.getImageWidth();
        int maxHeight = master.getImageHeight();
        if (master.mouseUL.x < 0) master.mouseUL.x = 0;
        if (master.mouseUL.x < 0) master.mouseUL.x = 0;
        if (master.mouseLR.x < 0) master.mouseLR.x = 0;
        if (master.mouseLR.x < 0) master.mouseLR.x = 0;
        if (master.mouseUL.y < 0) master.mouseUL.y = 0;
        if (master.mouseUL.y < 0) master.mouseUL.y = 0;
        if (master.mouseLR.y < 0) master.mouseLR.y = 0;
        if (master.mouseLR.y < 0) master.mouseLR.y = 0;
        if (master.mouseUL.x > maxWidth) master.mouseUL.x = maxWidth - 1;
        if (master.mouseUL.x > maxWidth) master.mouseUL.x = maxWidth - 1;
        if (master.mouseLR.x > maxWidth) master.mouseLR.x = maxWidth - 1;
        if (master.mouseLR.x > maxWidth) master.mouseLR.x = maxWidth - 1;
        if (master.mouseUL.y > maxHeight) master.mouseUL.y = maxHeight - 1;
        if (master.mouseUL.y > maxHeight) master.mouseUL.y = maxHeight - 1;
        if (master.mouseLR.y > maxHeight) master.mouseLR.y = maxHeight - 1;
        if (master.mouseLR.y > maxHeight) master.mouseLR.y = maxHeight - 1;
    }

    public void attach(PanelColorBased panelColorBased)
    {
        master = panelColorBased;
    }
}
