import javax.swing.*;

/**
 * A JButton that holds a private boolean variable.
 */
public class BooleanButton extends JButton
{
    public boolean val;

    public BooleanButton(String text, boolean val)
    {
        super(text);
        this.val = val;
    }

}
