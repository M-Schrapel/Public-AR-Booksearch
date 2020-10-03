package masterarbeit_thilo.hci.luh.de.visualbooksearch.ar_core;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;

import org.opencv.core.Point;

// Verbindung zwischen Bild- und Weltkoodinaten
public class ScreenToWorldConnection {

    private static final String TAG = "ScreenToWorldConnection";

    private Vector3 worldSpace, screenSpace;
    private Vector3 distanceVector;
    private float distance;
    private Anchor anchor;
    private AnchorNode anchorNode;

    public ScreenToWorldConnection(AnchorNode anchorNode, Vector3 worldSpace, Vector3 screenSpace) {
        this.anchorNode = anchorNode;
        this.worldSpace = worldSpace;
        this.screenSpace = screenSpace;
    }

    public Vector3 getScreenPoint() {
        return screenSpace;
    }

    public Vector3 getWorldPoint() {
        return worldSpace;
    }

    public AnchorNode getAnchorNode() {
        return anchorNode;
    }

    public void calculateDistance(Point center) {
        distanceVector = new Vector3((float) (center.x - screenSpace.x), (float) (center.y - screenSpace.y), 0);
        distance = distanceVector.length();
    }

    public float getDistance() {
        return distance;
    }

    public Vector3 getDistanceVector() {
        return distanceVector;
    }

    public Vector3 getDistanceVector(Point corner) {
        return new Vector3((float) (corner.x - screenSpace.x), (float) (corner.y - screenSpace.y), 0);
    }

    // Unterschied zwischen dem Winkel in Bild- und Weltkoordinaten berechnen
    public double getAngleOffset(ScreenToWorldConnection closestPoint, Node nodeRotation) {
        Point bot;
        Point top;
        if (screenSpace.y > closestPoint.getScreenPoint().y) {
            top = new Point(closestPoint.getScreenPoint().x, closestPoint.getScreenPoint().y);
            bot = new Point(screenSpace.x, screenSpace.y);
        } else {
            top = new Point(screenSpace.x, screenSpace.y);
            bot = new Point(closestPoint.getScreenPoint().x, closestPoint.getScreenPoint().y);
        }
        double deltaX = top.x - bot.x;
        double deltaY = - (top.y - bot.y);
        double length = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
        double angleScreen = Math.acos(deltaX / length);

        if (worldSpace.y > closestPoint.getWorldPoint().y) {
            top = new Point(nodeRotation.worldToLocalPoint(worldSpace).x, worldSpace.y);
            bot = new Point(nodeRotation.worldToLocalPoint(closestPoint.getWorldPoint()).x, closestPoint.getWorldPoint().y);
        } else {
            top = new Point(nodeRotation.worldToLocalPoint(closestPoint.getWorldPoint()).x, closestPoint.getWorldPoint().y);
            bot = new Point(nodeRotation.worldToLocalPoint(worldSpace).x, worldSpace.y);
        }
        deltaX = top.x - bot.x;
        deltaY = - (top.y - bot.y);
        length = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
        double angleWorld = Math.acos(deltaX / length);
        return angleScreen - angleWorld;
    }

    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    public Anchor getAnchor() {
        return anchor;
    }
}
