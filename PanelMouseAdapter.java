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
        master.mouseUL.x = e.getX() - 10;
        master.mouseUL.y = e.getY() - 10;
        master.mouseLR.x = master.mouseUL.x + master.selectBoxLen;
        master.mouseLR.y = master.mouseUL.y + master.selectBoxLen;
        System.out.println(master.mouseUL.x + " " + master.mouseUL.y);
    }

    public void attach(PanelColorBased panelColorBased)
    {
        master = panelColorBased;
    }
}
