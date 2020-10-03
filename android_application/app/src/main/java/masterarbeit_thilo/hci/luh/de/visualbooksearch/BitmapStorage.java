package masterarbeit_thilo.hci.luh.de.visualbooksearch;

import android.graphics.Bitmap;

// Zum Transfer des aufgenommenen Bildes von der CameraActivity zur ArSceneformActivity
public class BitmapStorage {

    private static Bitmap bitmap;

    public static void setBitmap(Bitmap bitmap) {
        BitmapStorage.bitmap = bitmap;
    }

    public static Bitmap getBitmap() {
        return bitmap;
    }
}
