import javax.swing.*;
import java.awt.*;

public class FrameTransparent extends JFrame{
    public FrameTransparent(){
        new JFrame();
        setType(Type.UTILITY);
        setAlwaysOnTop(true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setLayout(null);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
