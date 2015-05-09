import javax.swing.*;

/**
 * Extension of JButton, allowing the button to access outer class Condition variable.
 */
public class ConditionButton extends JButton
{
    Condition handle;

    public ConditionButton()
    {
        super();
    }

    public void setCondition(Condition handle)
    {
        this.handle = handle;
    }
}
