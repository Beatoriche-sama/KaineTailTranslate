import java.awt.*;
import java.awt.image.BufferedImage;

public class AreaScreenshot {
    private final Robot robot;

    public AreaScreenshot() throws AWTException {
        robot = new Robot();
    }

    public BufferedImage screenshot(Point point, Dimension dimension) {
        Rectangle captureRect = new Rectangle(point.x, point.y,
                dimension.width, dimension.height);
        return robot.createScreenCapture(captureRect);
    }

}
