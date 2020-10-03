package masterarbeit_thilo.hci.luh.de.visualbooksearch.ar_core;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing.BookSearch;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.BookSpine;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.ArDebug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;

import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.ArDebug.AR_DEBUGGING;

public class HighlightBookTask extends AsyncTask<BookSpine, Void, HighlightBookTask.TaskResult> {

    private static final String TAG = "HighlightBookTask";
    private static final float MATERIAL_TRANSPARENCY = 0.8f;
    private static final float BOOK_WIDTH_STRETCHING_FACTOR = 1.5f;
    private static final float BOOK_HEIGHT_STRETCHING_FACTOR = 1;
    private WeakReference<Context> weakContext;
    private ArrayList<ScreenToWorldConnection> trackablePoints;

    /*
    Eingabe:
    - Screenkoordinaten des gefundenen Buches (center, width, height, rotation)
    - Screen- und Weltkoordinatenpaare ("ScreenToWorldConnection") aus den "trackablePoints"
    Ziel: Finden der Weltkoordinaten des gefundenen Buches mit dazugehöriger Größe und Rotation
    Lösung:
    1. Einen Anchor als Referenzpunkt auswählen ("closestPoint": geringster Abstand zur Buchmitte)
    2. Lokales Koordinatensystem ausrichten ("xAxisRotation": x-Achse rotieren, horizontal
       entlang der Bücher, links->rechts), (y-Achse bleibt unverändert, vertikal, unten -> oben), (4.6.2.3)
    3. Verhältnis der Abstände zwischen Screen- und Weltkoordinaten berechnen ("screenWorldDistanceRatio")
    4. Lokales Koordinatensystem zur Buchmitte hin verschieben ("centerVecWorld"), (4.6.2.4)
    5. ModelRenderable mit ermittelter Größe "bookWidthWorld", "bookHeightWorld" erstellen
    6. Hervorhebung des Buches anhand der Buchausrichtung ("bookRotation") abzgl. der Rotation des
       Smartphones ("angleOffset") rotieren (4.6.3)
    */

    public HighlightBookTask(Context context, ArrayList<ScreenToWorldConnection> trackablePoints) {
        weakContext = new WeakReference<>(context);
        this.trackablePoints = trackablePoints;
    }

    @Override
    protected TaskResult doInBackground(BookSpine... bookSpines) {
        if (isCancelled()) return null;
        BookSpine foundBook = bookSpines[0];

        // Buchmittelpunkt
        Point centerFoundBook = foundBook.getCenter();

        // Alle "trackablePoints" durchlaufen und prüfen, welcher dem Buchmittelpunkt
        // am nächsten liegt (closestPoint)
        ScreenToWorldConnection bookHighlight = trackablePoints.get(0);
        bookHighlight.calculateDistance(centerFoundBook);
        for (int i = 1; i < trackablePoints.size(); i++) {
            trackablePoints.get(i).calculateDistance(centerFoundBook);
            if (trackablePoints.get(i).getDistance() < bookHighlight.getDistance()) bookHighlight = trackablePoints.get(i);
        }
        final ScreenToWorldConnection closestPoint = bookHighlight;
        trackablePoints.remove(closestPoint);
        Vector3 closestPointScreen = closestPoint.getScreenPoint();
        Vector3 closestPointWorld = closestPoint.getWorldPoint();

        // Die Winkel und Abstände zwischen dem "closestPoint" und allen anderen "trackablePoints" berechnen
        ArrayList<Double> xAxisRotations = new ArrayList<>(trackablePoints.size());
        ArrayList<Double> screenWorldDistanceRatios = new ArrayList<>(trackablePoints.size());
        for (ScreenToWorldConnection swc : trackablePoints) {
            Vector3 direction = Vector3.subtract(swc.getWorldPoint(), closestPointWorld);
            double length = Math.sqrt(Math.pow(direction.x, 2) + Math.pow(direction.z, 2));
            if (swc.getScreenPoint().x < closestPointScreen.x) { // wenn swc... links von closest...
                direction.set(-direction.x, 0, -direction.z); // Spiegelung am Punkt closestPointWorld, sodass alle Punkte "vor" closest liegen
            } else {
                direction.set(direction.x, 0, direction.z);
            }
            double angle = Math.acos(direction.x / length);
            if (direction.z > 0) angle = (float) (2 * Math.PI - angle);
            //if (direction.z > 0) angle = -angle; // müsste auch funktionieren (noch testen)
            xAxisRotations.add(angle);

            double screenWorldDistanceRatio = Vector3.subtract(swc.getWorldPoint(), closestPointWorld).length()
                    / Vector3.subtract(swc.getScreenPoint(), closestPointScreen).length();
            screenWorldDistanceRatios.add(screenWorldDistanceRatio);
        }

        // Median vom Winkel und Abstand nehmen
        float xAxisRotation = (float) Math.toDegrees(calculateAngleMedian(xAxisRotations));
        Collections.sort(screenWorldDistanceRatios);
        float screenWorldDistanceRatio = screenWorldDistanceRatios.get(screenWorldDistanceRatios.size() / 2).floatValue();

        // Vektor in Bild- und anschließend Weltkoordinaten berechnen, um den Ankerpunkt (closestPoint)
        // zur Buchmitte hin zu verschieben (centerVecWorld)
        Vector3 centerVecScreen = closestPoint.getDistanceVector();
        Vector3 centerVecWorld = new Vector3(centerVecScreen.x * screenWorldDistanceRatio, -centerVecScreen.y * screenWorldDistanceRatio, 0);

        // nur für AR Debug, um alle vier Eckpunkte des Buches anzuzeigen
        Point[] cornerPoints = foundBook.getCornerPoints();
        Vector3[] cornerVecWorld = new Vector3[cornerPoints.length];
        for (int i = 0; i < cornerPoints.length; i++) {
            Vector3 cornerVecScreen = closestPoint.getDistanceVector(cornerPoints[i]);
            cornerVecWorld[i] = new Vector3(cornerVecScreen.x * screenWorldDistanceRatio, -cornerVecScreen.y * screenWorldDistanceRatio, 0);
        }

        // Breite und Höhe des Buches bestimmen
        float bookWidthWorld = foundBook.getWidth() * screenWorldDistanceRatio;
        float bookHeightWorld = foundBook.getHeight() * screenWorldDistanceRatio;
        float bookRotation = (float) Math.toDegrees(foundBook.getRotation());
        return new TaskResult(closestPoint, xAxisRotation, centerVecWorld, cornerVecWorld, bookWidthWorld, bookHeightWorld, bookRotation);
    }

    // Einige Funktionen müssen auf dem UI-Thread ausgeführt werden
    @Override
    protected void onPostExecute(TaskResult result) {
        if (isCancelled()) return;
        ScreenToWorldConnection closestPoint = result.closestPoint;
        float xAxisRotation = result.xAxisRotation;
        Vector3 centerVecWorld = result.centerVecWorld;
        Vector3[] cornerVecWorld = result.cornerVecWorld;
        float bookWidthWorld = result.bookWidthWorld;
        float bookHeightWorld = result.bookHeightWorld;
        float bookRotation = result.bookRotation;

        Node rotationNode = new Node();
        Node highlightNode = new Node();
        MaterialFactory.makeTransparentWithColor(weakContext.get(), new Color(0.0f, 1.0f, 0.0f, MATERIAL_TRANSPARENCY))
                .thenAccept(
                        material -> {
                            ModelRenderable bookHighlightCube = ShapeFactory.makeCube(
                                    new Vector3(bookWidthWorld * BOOK_WIDTH_STRETCHING_FACTOR,
                                            bookHeightWorld * BOOK_HEIGHT_STRETCHING_FACTOR, 0),
                                    Vector3.zero(),
                                    material);
                            highlightNode.setRenderable(bookHighlightCube);
                        });
        // lokales Koordinatensystem ausrichten
        rotationNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1, 0), xAxisRotation));
        ArrayList<Double> screenWorldAngleOffsets = new ArrayList<>(trackablePoints.size());
        for (ScreenToWorldConnection swc : trackablePoints) {
            double angleOffset = Math.toDegrees(swc.getAngleOffset(closestPoint, rotationNode));
            screenWorldAngleOffsets.add(angleOffset);
        }
        Collections.sort(screenWorldAngleOffsets);
        float angleOffset = screenWorldAngleOffsets.get(screenWorldAngleOffsets.size() / 2).floatValue();

        // lokales Koordinatensystem zur Buchmitte hin verschieben und rotieren
        highlightNode.setLocalPosition(centerVecWorld);
        highlightNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 0, 1), bookRotation - 90 - angleOffset));

        // lokale Koordinatensysteme, Buchmittelpunkt und die 4 Eckpunkte anzeigen
        if (AR_DEBUGGING) {
            closestPoint.getAnchorNode().setRenderable(ArDebug.sphereRed);
            Node centerNode = new Node();
            centerNode.setRenderable(ArDebug.sphereGreen);
            centerNode.setLocalPosition(centerVecWorld);
            centerNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 0, 1), bookRotation - 90 - angleOffset));
            rotationNode.addChild(centerNode);
            for (Vector3 v : cornerVecWorld) {
                Node cornerNode = new Node();
                cornerNode.setRenderable(ArDebug.sphereGreen);
                cornerNode.setLocalPosition(v);
                cornerNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 0, 1), bookRotation - 90 - angleOffset));
                rotationNode.addChild(cornerNode);
            }
        }
        closestPoint.getAnchorNode().addChild(rotationNode);
        rotationNode.addChild(highlightNode);
        if (AR_DEBUGGING) {
            //ArDebug.showLocalWorldCoordinateSystem(closestPoint.getAnchorNode());
            ArDebug.showLocalWorldCoordinateSystem(rotationNode);
            ArDebug.showLocalWorldCoordinateSystem(highlightNode);
        }
    }

    class TaskResult {

        ScreenToWorldConnection closestPoint;
        float xAxisRotation;
        Vector3 centerVecWorld;
        Vector3[] cornerVecWorld;
        float bookWidthWorld;
        float bookHeightWorld;
        float bookRotation;

        TaskResult(ScreenToWorldConnection closestPoint, float xAxisRotation, Vector3 centerVecWorld,
                   Vector3[] cornerVecWorld, float bookWidthWorld, float bookHeightWorld, float bookRotation) {
            this.closestPoint = closestPoint;
            this.xAxisRotation = xAxisRotation;
            this.centerVecWorld = centerVecWorld;
            this.cornerVecWorld = cornerVecWorld;
            this.bookWidthWorld = bookWidthWorld;
            this.bookHeightWorld = bookHeightWorld;
            this.bookRotation = bookRotation;
        }
    }

    // Zur Berechnung des Medians von Winkeln
    private static double calculateAngleMedian(ArrayList<Double> angles) {
        Collections.sort(angles);
        double x = 0;
        double y = 0;

        // Umrechnung in Polarkoordinaten
        for (double a : angles) {
            x += Math.cos(a);
            y += Math.sin(a);
        }

        // Berechnung des Mittelwertes
        double angleMean = Math.atan2(y / angles.size(), x / angles.size());

        // Nullpunkt für die Medianberechnung wird gegenüber des Mittelwertes gesetzt
        double firstAngleMedian = (angleMean + Math.PI) % (2 * Math.PI);

        // Liste wird anhand des neu gesetzten Nullpunktes umgeordnet
        ArrayList<Double> croppedList = null;
        for (int i = 0; i < angles.size(); i++) {
            if (angles.get(i) > firstAngleMedian) {
                croppedList = new ArrayList<>(angles.subList(0, i));
                break;
            }
        }
        if (croppedList != null) {
            angles.removeAll(croppedList);
            angles.addAll(croppedList);
        }

        // Median befindet sich genau in der Mitte der Liste
        return angles.get(angles.size() / 2);
    }

}
