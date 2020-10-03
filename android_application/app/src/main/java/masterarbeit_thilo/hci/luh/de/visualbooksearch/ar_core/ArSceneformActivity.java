package masterarbeit_thilo.hci.luh.de.visualbooksearch.ar_core;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Vibrator;

import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.ArFragment;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.BitmapStorage;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.MyOrientationEventListener;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.R;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.ArDebug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.FileReaderWriter;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing.BookSearch;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.BookSpine;

import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.ArDebug.AR_DEBUGGING;

public class ArSceneformActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "ArDebug ArSceneformActivity";

    // minimale Anzahl an "trackables", die von ARCore gefunden werden müssen
    public static final int TRACKABLE_POINTS_MINIMUM = 20;

    // Grenzwert, um anhand des Gyroscopes Bewegung von Nicht-Bewegung zu unterscheiden
    private static final double GYROSCOPE_NO_MOTION_THRESHOLD = 0.001;

    // Zum initialisieren von ARCore und Sceneform
    private ArFragment arFragment;
    private ArSceneView arSceneView;

    // Zum Debuggen von AR, Anzeigen von "trackables", Koordinatensystem
    private ArDebug arDebug;

    // Eigener "OrientationListener", um die Rotation des Display zu erhalten (0, 90, 180, 270)
    private MyOrientationEventListener mOrientationEventListener;

    // Looper handler thread.
    private HandlerThread backgroundThread;

    // Looper handler.
    private Handler backgroundHandler;

    private BookSearch bookSearch;

    // Smartphone gerade in Bewegung oder nicht
    private boolean isMoving = true;

    // Zur Anzeige, ob die Suche nach einem Buch läuft
    private ProgressBar progressBar;

    // Für den Zugriff auf das Gyroscope
    private SensorManager sensorManager;
    private Sensor sensorGyroscope;

    // Wurden genug "trackables" gefunden
    private boolean foundTrackables = false;

    // Wird das Buch gerade visuell hervorgehoben
    private boolean isHighlighting = false;

    // AsyncTask für die visuelle Hervorhebung
    private AsyncTask highlightBookTask;

    // Zur Speicherung der bisherigen gefundenen "trackables"
    private ArrayList<AnchorNode> trackableAnchorNodes = new ArrayList<>();

    /*
    Wird vor jedem Frame aufgerufen und sucht nach "trackables".
    Wurden genug "trackables" gefunden, dann verschwindet die sich bewegende Hand

     */
    private void onUpdate() {
        if (isHighlighting) return;
        Frame frame;
        if ((frame = arSceneView.getArFrame()) == null) return;
        if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            try (PointCloud pointCloud = frame.acquirePointCloud()) {
                if (AR_DEBUGGING) {
                    arDebug.showInitialWorldCoordinateSystem(frame);
                    arDebug.showFeaturePoints(pointCloud);
                }
                if (findTrackables(pointCloud)) {
                    foundTrackables = true;
                    arFragment.getPlaneDiscoveryController().hide();
                    arFragment.getPlaneDiscoveryController().setInstructionView(null);
                }
            }
        }
    }

    // Funktion zur visuellen Hervorhebung des gefundenen Buches
    private void highlightBook(Size imageSize, BookSpine foundBookSpine, int displayRotation) {
        //Log.d(TAG, "highlightBook");
        while (true) {
            //Log.d(TAG, "foundTrackables: " + foundTrackables + ", isMoving: " + isMoving);
            if (foundTrackables && !isMoving) break;
        }
        isHighlighting = true;
        ArrayList<ScreenToWorldConnection> trackablePoints = new ArrayList<>();
        Scene scene = arSceneView.getScene();

        // Verbindungen zwischen Bild- und Weltkoodinaten herstellen (4.6.2.1)
        for (AnchorNode n : trackableAnchorNodes) {
            Vector3 worldSpace = n.getWorldPosition();
            Vector3 screenSpace = scene.getCamera().worldToScreenPoint(worldSpace);
            trackablePoints.add(new ScreenToWorldConnection(n, worldSpace, screenSpace));
            if (AR_DEBUGGING) runOnUiThread(() -> n.setRenderable(ArDebug.sphereCloud));
        }

        // Bildkoordianten aufgrund der neuen Kameravorschau anpassen (4.6.2.2)
        foundBookSpine.fitCornerPointsToScreenSize(imageSize, arSceneView.getWidth(), arSceneView.getHeight(), displayRotation);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(300);
        runOnUiThread(() -> highlightBookTask = new HighlightBookTask(getBaseContext(), trackablePoints).execute(foundBookSpine));
    }

    // Suche nach einem Buch starten
    private void searchBook() {
        backgroundHandler.post(() -> {
            progressBar.setVisibility(View.VISIBLE);
            Mat mat = new Mat();
            Utils.bitmapToMat(BitmapStorage.getBitmap(), mat);
            Toast.makeText(getBaseContext(), R.string.search_started, Toast.LENGTH_LONG).show();
            BookSpine foundBookSpine = bookSearch.processImage(mat);
            //Log.d(TAG, "foundBookSpine == null: " + (foundBookSpine == null));
            if (foundBookSpine != null) {
                highlightBook(mat.size(), foundBookSpine, mOrientationEventListener.getRotation());
            } else {
                Toast.makeText(this, R.string.search_finished_no_book, Toast.LENGTH_LONG).show();
            }
            progressBar.setVisibility(View.INVISIBLE);
        });
    }

    // Empfängt dauerhaft Werte vom Gyroscope und bestimmt, ob sich das Smartphone bewegt
    @Override
    public void onSensorChanged(SensorEvent event) {
        double rotation = 0;
        for (float f : event.values) {
            rotation += Math.pow(f, 2);
        }
        isMoving = rotation > GYROSCOPE_NO_MOTION_THRESHOLD;
        //Log.d(TAG, "isMoving: " + isMoving);
        //Log.d(TAG, "rotation: " + rotation);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("sharedCameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quit();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while trying to join background handler thread", e);
            }
        }
    }

    /*
    Nimmt alle "trackables" (pointCloud) von ARCore entgegen und prüft, ob es genug sind
    Falls ja, und auch mehr als vorher, dann werden Ankerpunkte an die "trackables" gesetzt
    und die alten Ankerpunkte werden entfernt und verworfen
     */
    private boolean findTrackables(PointCloud pointCloud) {
        FloatBuffer points = pointCloud.getPoints();
        int capacity = points.capacity() / 4;
        if (capacity < TRACKABLE_POINTS_MINIMUM || capacity < trackableAnchorNodes.size()) {
            //Log.d(TAG, "trackableAnchorNodes: " + capacity + " (not enough)");
            return false;
        }
        Log.d(TAG, "trackableAnchorNodes: " + capacity);
        ArrayList<AnchorNode> trackablePoints = new ArrayList<>();
        Session session = arSceneView.getSession();
        Scene scene = arSceneView.getScene();
        if (session == null) return false;
        for (int i = 0; i < capacity; i++) {
            float x = points.get();
            float y = points.get();
            float z = points.get();
            float confidence = points.get();
            float[] translation = {x, y, z};
            AnchorNode n = new AnchorNode(session.createAnchor(Pose.makeTranslation(translation)));
            if (AR_DEBUGGING) n.setRenderable(ArDebug.sphereCloud);
            scene.addChild(n);
            trackablePoints.add(n);
        }
        ArrayList<AnchorNode> tmp = trackableAnchorNodes;
        trackableAnchorNodes = trackablePoints;
        for (AnchorNode n : tmp) {
            scene.removeChild(n);
            n.getAnchor().detach();
        }
        //Log.d(TAG, "foundTrackables");
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_sceneform);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);

        Intent intent = getIntent();
        bookSearch = new BookSearch(BookEntity.getBookEntity(intent), getBaseContext());

        progressBar = findViewById(R.id.progressBar);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mOrientationEventListener = new MyOrientationEventListener(getBaseContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        arSceneView = arFragment.getArSceneView();
        arSceneView.getScene().addOnUpdateListener(frameTime -> onUpdate());
        arSceneView.getPlaneRenderer().setEnabled(false);
        if (AR_DEBUGGING) arDebug = new ArDebug(getBaseContext(), arSceneView);
        searchBook();
        sensorManager.registerListener(this, sensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mOrientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        mOrientationEventListener.disable();
        stopBackgroundThread();
        if (highlightBookTask != null) {
            highlightBookTask.cancel(true);
            highlightBookTask = null;
        }
        super.onPause();
    }

}
