package masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines;

import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;

// Eine Verbindung von mehreren Liniensegmenten
public class LineSegmentCluster {

    private static final String TAG = "LineSegmentCluster";

    // eine Linie kann mit einer anderen Linie verbunden werden, wenn der Winkel der Verbindungslinie zwischen den beiden
    // Mittelpunkten maximal MERGING_ANGLE_THRESHOLD vom eigenen Winkel abweicht
    private static final double MERGING_ANGLE_THRESHOLD = Math.toRadians(1);

    // eine Linie kann mit einer anderen Linie verbunden werden, die maximal (eigene Länge * MERGING_DISTANCE_FACTOR) entfernt ist
    private static final double MERGING_DISTANCE_FACTOR = 2;

    // benachbarte Cluster deren Abstand kleiner ist werden zusammengefügt
    private static final double MERGE_ADJACENT_CLUSTER_THRESHOLD = 20;

    // Sonderfall, falls die Mittelpunkte der beiden Linien sehr nahe beieinander liegen
    private static final double MERGING_CENTER_DISTANCE = 5;
    private static final double MERGING_CENTER_ANGLE = Math.toRadians(2);

    private ArrayList<LineSegment> lineSegments = new ArrayList<>();
    private double xMeanCum, yMeanCum, angleCum;
    private double length;
    private double xMean, yMean;
    private int yMax, yMin;
    private double yTopBorder, yBotBorder;
    private double position;
    private Point bot, top;
    private double intersectBotX, intersectTopX;
    private double angle;
    private double cot;
    private Scalar color;

    public LineSegmentCluster(LineSegment line, double yTopBorder, double yBotBorder) {
        yMin = (int) line.getTop().y;
        yMax = (int) line.getBot().y;
        this.yTopBorder = yTopBorder;
        this.yBotBorder = yBotBorder;
        add(line);
        if (Debug.DEBUGGING) color = Debug.getRandomColor();
    }

    public void add(LineSegment line) {
        lineSegments.add(line);
        if (line.getTop().y < yMin) yMin = (int) line.getTop().y;
        if (line.getBot().y > yMax) yMax = (int) line.getBot().y;
        xMeanCum += line.getLength() * (line.getBot().x + line.getTop().x) / 2;
        yMeanCum += line.getLength() * (line.getBot().y + line.getTop().y) / 2;
        angleCum += line.getLength() * line.getAngle();
        length += line.getLength();
    }

    /*
        Es soll geprüft werden, ob das übergebene LineSegment zu diesem Cluster passt, indem es mit
        allen bisherigen lineSegments des Clusters verglichen wird. Dazu wird eine Verbindungslinie
        zwischen den Mittelpunkten der beiden zu vergleichenden LineSegments gebildet. Wenn der Winkel
        der Verbindungslinie nur gering (MERGING_ANGLE_THRESHOLD) vom Winkel der beiden zu vergleichenden
        Linien abweicht und die Verbindungslinie nicht größer als die Länge * MERGING_DISTANCE_FACTOR
        der beiden zu vergleichenden Linien ist, dann wird die Linie zu diesem Cluster hinzugefügt (4.4.4.1).
    */
    public boolean match(LineSegment line) {
        for (LineSegment clusterLine : lineSegments) {
            double length = Math.sqrt(Math.pow(line.getMean().x - clusterLine.getMean().x, 2) + Math.pow(line.getMean().y - clusterLine.getMean().y, 2));
            double angle;
            // angle wird immer ausgehend vom unteren Punkt berechnet
            if (line.getMean().y > clusterLine.getMean().y) { // wenn line unter l liegt
                angle = Math.acos((clusterLine.getMean().x - line.getMean().x) / length);
            } else { // wenn line über l liegt
                angle = Math.acos((line.getMean().x - clusterLine.getMean().x) / length);
            }
            if (Math.abs(angle - line.getAngle()) < MERGING_ANGLE_THRESHOLD && Math.abs(angle - clusterLine.getAngle()) < MERGING_ANGLE_THRESHOLD
                    && line.getLength() * MERGING_DISTANCE_FACTOR > length && clusterLine.getLength() * MERGING_DISTANCE_FACTOR > length) {
                return true;
            } else if (length < MERGING_CENTER_DISTANCE && Math.abs(line.getAngle() - clusterLine.getAngle()) < MERGING_CENTER_ANGLE) {
                // Sonderfall, wenn Mittelpunkte sehr nahe oder genau aufeinander liegen
                return true;
            }
        }
        return false;
    }

    // berechnet die Eigenschaften neu
    public void calculatePoints() {
        xMean = xMeanCum / length;
        yMean = yMeanCum / length;
        angle = angleCum / length;
        cot = Math.cos(angle) / Math.sin(angle);
        top = new Point(xMean + cot * (yMean - yMin), yMin);
        bot = new Point(xMean + cot * (yMean - yMax), yMax);
        intersectTopX = xMean + cot * (yMean - yTopBorder);
        intersectBotX = xMean + cot * (yMean - yBotBorder);
        position = intersectBotX + intersectTopX;
    }

    // Prüft ob dieses Cluster mit dem Cluster c benachbart ist und fügt sie ggf. zusammen (4.4.4.4)
    public boolean mergeWith(LineSegmentCluster c) {
        if (Math.abs(intersectBotX - c.intersectBotX) + Math.abs(intersectTopX - c.intersectTopX) < MERGE_ADJACENT_CLUSTER_THRESHOLD) {
            if (c.yMin < yMin) yMin = c.yMin;
            if (c.yMax > yMax) yMax = c.yMax;
            xMeanCum += c.xMeanCum;
            yMeanCum += c.yMeanCum;
            angleCum += c.angleCum;
            length += c.length;
            calculatePoints();
            return true;
        } else {
            return false;
        }
    }

    // Berechnet zu einer gegebenen y-Koordinate den dazugehörigen x-Wert der Linie
    public double calculateX(double y) {
        return xMean + cot * (yMean - y);
    }

    // Berechnet den Schnittpunkt mit einer Linie
    public Point getIntersectionPoint(LineSegment line) {
        double cotLine = Math.cos(line.getAngle()) / Math.sin(line.getAngle());
        double alpha = (calculateX(line.getBot().y) - line.getBot().x) / (cotLine - cot);
        double x = line.getBot().x + alpha * cotLine;
        double y = line.getBot().y - alpha;
        return new Point(x, y);
    }

    // Berechnet die Breite zu einem gegebenen zweiten Cluster
    public double getWidth(LineSegmentCluster secondBound) {
        double orthogonalAngle = (angle + Math.toRadians(90)) % Math.toRadians(180);
        LineSegment orthogonalLine = new LineSegment(new Point(xMean, yMean),
                new Point(xMean, yMean), orthogonalAngle);
        Point p = secondBound.getIntersectionPoint(orthogonalLine);
        return Math.sqrt(Math.pow(xMean - p.x, 2) + Math.pow(yMean - p.y, 2));
    }

    public ArrayList<LineSegment> getLineSegments() {
        return lineSegments;
    }

    public double getLength() {
        return length;
    }

    public double getPosition() {
        return position;
    }

    public Point getTop() {
        return top;
    }

    public Point getBot() {
        return bot;
    }

    public double getAngle() {
        return angle;
    }

    public int getMinY() {
        return yMin;
    }

    public int getMaxY() {
        return yMax;
    }

    public Scalar getColor() {
        return color;
    }

}