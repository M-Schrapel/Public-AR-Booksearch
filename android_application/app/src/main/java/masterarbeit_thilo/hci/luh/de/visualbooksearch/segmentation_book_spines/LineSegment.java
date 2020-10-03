package masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines;

import org.opencv.core.Point;
import org.opencv.core.Size;

// Ein Liniensegment, welches normalerweise aus dem LSD entsteht
public class LineSegment {

    private static int width, height;

    private Point top;
    private Point bot;
    private Point mean;
    private double length, angle;
    private int intersectionLeft, intersectionRight;
    private int intersectionBot, intersectionTop;

    public LineSegment(Point bot, Point top) {
        this.bot = bot;
        this.top = top;
        mean = new Point((bot.x + top.x) / 2, (bot.y + top.y) / 2);
        length = Math.sqrt(Math.pow(bot.x - top.x, 2) + Math.pow(bot.y - top.y, 2));
        angle = Math.acos((top.x - bot.x) / length);
        intersectionRight = (int) (mean.y + (width - mean.x) * (top.y - bot.y) / (top.x - bot.x));
        intersectionLeft = (int) (mean.y - mean.x * (top.y - bot.y) / (top.x - bot.x));
        intersectionTop = (int) (mean.x - mean.y * (top.x - bot.x) / (top.y - bot.y));
        intersectionBot = (int) (mean.x + (height - mean.y) * (top.x - bot.x) / (top.y - bot.y));
    }

    public LineSegment(Point bot, Point top, double angle) {
        this.top = top;
        this.bot = bot;
        length = Math.sqrt(Math.pow(top.x - bot.x, 2) + Math.pow(top.y - bot.y, 2));
        this.angle = angle;
    }

    // setzen der Breite und Höhe der aktuellen Region
    public static void setSize(Size size) {
        LineSegment.width = (int) size.width;
        LineSegment.height = (int) size.height;
    }

    public Point getTop() {
        return top;
    }

    public Point getBot() {
        return bot;
    }

    public Point getRight() {
        return (bot.x > top.x) ? bot : top;
    }

    public Point getLeft() {
        return (bot.x < top.x) ? bot : top;
    }

    public static double getAngle(Point p1, Point p2) {
        Point bot, top;
        if (p1.y > p2.y) {
            top = p2;
            bot = p1;
        } else {
            top = p1;
            bot = p2;
        }
        double length = Math.sqrt(Math.pow(bot.x - top.x, 2) + Math.pow(bot.y - top.y, 2));
        return Math.acos((top.x - bot.x) / length);
    }

    public double getAngle() {
        return angle;
    }

    public double getLength() {
        return length;
    }

    public Point getMean() {
        return mean;
    }

    // zum Tauschen der x- und y-Position bei horizontalen Bücherrücken
    public void swapXY() {
        if (bot.x > top.x) {
            bot = new Point(bot.y, bot.x);
            top = new Point(top.y, top.x);
        } else {
            Point tmp = new Point(bot.y, bot.x);
            bot = new Point(top.y, top.x);
            top = tmp;
        }
        mean = new Point((bot.x + top.x) / 2, (bot.y + top.y) / 2);
        length = Math.sqrt(Math.pow(bot.x - top.x, 2) + Math.pow(bot.y - top.y, 2));
        angle = Math.acos((top.x - bot.x) / length);
    }

    public int getVerticalPosition() {
        return intersectionLeft + intersectionRight;
    }

    public int getHorizontalPosition() {
        return intersectionBot + intersectionTop;
    }

    public int getIntersectionLeft() {
        return intersectionLeft;
    }

    public int getIntersectionRight() {
        return intersectionRight;
    }
}
