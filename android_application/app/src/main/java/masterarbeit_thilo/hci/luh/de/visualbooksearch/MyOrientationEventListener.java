package masterarbeit_thilo.hci.luh.de.visualbooksearch;

import android.content.Context;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;

// Zur Bestimmung der Orientierung des Smartphones
public class MyOrientationEventListener extends OrientationEventListener {

    private static final String TAG = "MyOrientationEventListener";
    private static final int ANGLE_GAP = 45;

    private int orientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private int rotation = 0;

    public MyOrientationEventListener(Context context) {
        super(context);
    }

    public int getOrientation() {
        return orientation;
    }

    public int getRotation() {
        return rotation;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        //Log.d(TAG, "orientation: " + orientation);
        this.orientation = orientation;
        if (orientation == -1) return;
        if (orientation >= 360 - ANGLE_GAP || orientation < ANGLE_GAP) {
            rotation = 0;
        } else if (orientation < 90 + ANGLE_GAP) {
            rotation = 90;
        } else if (orientation < 180 + ANGLE_GAP) {
            rotation = 180;
        } else if (rotation < 270 + ANGLE_GAP){
            rotation = 270;
        }
    }

}
