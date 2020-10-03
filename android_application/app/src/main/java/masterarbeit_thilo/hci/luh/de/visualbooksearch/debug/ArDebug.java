package masterarbeit_thilo.hci.luh.de.visualbooksearch.debug;

import android.content.Context;

import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

import java.nio.FloatBuffer;
import java.util.ArrayList;

public class ArDebug {

    public static final boolean AR_DEBUGGING = false;

    private static final String TAG = "ArCoreDebug";
    private static final float SPHERE_RADIUS = 0.005f;
    private static final float SPHERE_RADIUS_CLOUD = 0.0025f;
    private static final float COORDINATE_SYSTEM_DISTANCE = 0.008f;
    private static final float COORDINATE_SYSTEM_LENGTH = 15;

    public static ModelRenderable sphereYellow;
    public static ModelRenderable sphereRed;
    public static ModelRenderable sphereGreen;
    public static ModelRenderable sphereBlue;
    public static ModelRenderable sphereCloud;

    private boolean worldCoordinatesInit = false;
    private ArSceneView arSceneView;
    //private PointCloudNode pointCloudNode;
    private ArrayList<Node> pointCloudNodes = new ArrayList<>();

    public ArDebug(Context context, ArSceneView arSceneView) {
        loadRenderables(context);
        this.arSceneView = arSceneView;
    }

    public void showFeaturePoints(PointCloud pointCloud) {
        Scene scene = arSceneView.getScene();
        for (Node n : pointCloudNodes) {
            //n.getAnchor().detach();
            scene.removeChild(n);
        }
        pointCloudNodes.clear();
        FloatBuffer points = pointCloud.getPoints();
        Session session = arSceneView.getSession();
        if (session == null) return;
        for (int i = 0; i < points.capacity() / 4; i++) {
            float x = points.get();
            float y = points.get();
            float z = points.get();
            float confidence = points.get();
            //float[] translation = {x, y, z};
            //AnchorNode n = new AnchorNode(session.createAnchor(Pose.makeTranslation(translation)));
            Node n = new Node();
            n.setWorldPosition(new Vector3(x, y, z));
            n.setRenderable(sphereYellow);
            pointCloudNodes.add(n);
            scene.addChild(n);
        }
    }

    // Zur Anzeige des Ausgangskoordinatensystems beim Start von ARCore
    public void showInitialWorldCoordinateSystem(Frame frame) {
        if (!worldCoordinatesInit && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            Session session = arSceneView.getSession();
            float[] translation = {0, 0, 0};
            //float[] rotation = arSceneView.getArFrame().getCamera().getPose().getRotationQuaternion();
            float[] rotation = {0, 0, 0, 0};
            // hat keinen Einfluss auf world-Ausrichtung, jedoch auf local-Ausrichtung
            if (session == null) return;
            AnchorNode coordinateSystem = new AnchorNode(session.createAnchor(new Pose(translation, rotation)));
            showLocalWorldCoordinateSystem(coordinateSystem);
            arSceneView.getScene().addChild(coordinateSystem);
            worldCoordinatesInit = true;
        }
    }

    // Zur Anzeige eines Koordinatensystems ausgehend von "node"
    public static void showLocalWorldCoordinateSystem(Node node) {
        Node zeroPosition = new Node();
        zeroPosition.setRenderable(sphereYellow);
        node.addChild(zeroPosition);
        for (int i = 1; i < COORDINATE_SYSTEM_LENGTH; i++) {
            Node x = new Node();
            Node y = new Node();
            Node z = new Node();
            x.setLocalPosition(new Vector3(i*COORDINATE_SYSTEM_DISTANCE, 0, 0));
            y.setLocalPosition(new Vector3(0, i*COORDINATE_SYSTEM_DISTANCE, 0));
            z.setLocalPosition(new Vector3(0, 0, i*COORDINATE_SYSTEM_DISTANCE));
            x.setRenderable(sphereRed);
            y.setRenderable(sphereGreen);
            z.setRenderable(sphereBlue);
            node.addChild(x);
            node.addChild(y);
            node.addChild(z);
        }
    }

    private static void loadRenderables(Context context) {
        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.YELLOW))
                .thenAccept(
                        material -> {
                            sphereYellow = ShapeFactory.makeSphere(SPHERE_RADIUS, Vector3.zero(), material);
                        });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            sphereRed = ShapeFactory.makeSphere(SPHERE_RADIUS, Vector3.zero(), material);
                        });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.GREEN))
                .thenAccept(
                        material -> {
                            sphereGreen = ShapeFactory.makeSphere(SPHERE_RADIUS, Vector3.zero(), material);
                        });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.BLUE))
                .thenAccept(
                        material -> {
                            sphereBlue = ShapeFactory.makeSphere(SPHERE_RADIUS, Vector3.zero(), material);
                        });

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.YELLOW))
                .thenAccept(
                        material -> {
                            sphereCloud = ShapeFactory.makeSphere(SPHERE_RADIUS_CLOUD, Vector3.zero(), material);
                        });
    }

}