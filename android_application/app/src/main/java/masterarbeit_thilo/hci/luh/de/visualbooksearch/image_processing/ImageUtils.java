package masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

// Hilfsklasse zur Konvertierung, Skalierung und Rotation von Bilddaten
public class ImageUtils {

    public static Mat YUV_420_888toMat(Image image) {
        int w = image.getWidth(), h = image.getHeight();

        // sanity checks - 3 planes
        Image.Plane[] planes = image.getPlanes();
        //Log.d(TAG, "planes.length == 3: " + (planes.length == 3));
        //Log.d(TAG, "image.getFormat() == ImageFormat.YUV_420_888: " + (image.getFormat() == ImageFormat.YUV_420_888));

        // see also https://developer.android.com/reference/android/graphics/ImageFormat.html#YUV_420_888
        // Y plane (0) non-interleaved => stride == 1; U/V plane interleaved => stride == 2
        /*
        Log.d(TAG, "planes[0].getPixelStride() == 1: " + (planes[0].getPixelStride() == 1));
        Log.d(TAG, "planes[1].getPixelStride() == 2: " + (planes[1].getPixelStride() == 2));
        Log.d(TAG, "planes[2].getPixelStride() == 2: " + (planes[2].getPixelStride() == 2));
        */

        ByteBuffer y_plane = planes[0].getBuffer();
        ByteBuffer uv_plane = planes[1].getBuffer();
        Mat y_mat = new Mat(h, w, CvType.CV_8UC1, y_plane);
        Mat uv_mat = new Mat(h / 2, w / 2, CvType.CV_8UC2, uv_plane);
        Mat rgb = new Mat();
        Imgproc.cvtColorTwoPlane(y_mat, uv_mat, rgb, Imgproc.COLOR_YUV2RGBA_NV21);
        Mat mat = new Mat();
        Imgproc.cvtColor(rgb, mat, Imgproc.COLOR_RGBA2BGR);
        //Imgproc.cvtColorTwoPlane(y_mat, uv_mat, rgb, Imgproc.COLOR_YUV2RGB_NV21);
        image.close();
        return mat;
    }

    private static byte[] YUV_420_888toNV21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    public static Bitmap YUV_420_888toBitmap(Image image) {
        YuvImage yuvImage = new YuvImage(YUV_420_888toNV21(image), ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        return BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
    }

    public static Bitmap getRotatedBitmap(Bitmap bmp, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        if (matrix.isIdentity()) return bmp;
        Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);
        bmp.recycle();
        return resizedBitmap;
    }

    public static Bitmap getResizedRotatedBitmap(Bitmap bmp, int newWidth, int newHeight, int rotation) {
        float scaleWidth = newWidth / (float) bmp.getWidth();
        float scaleHeight = newHeight / (float) bmp.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        matrix.postRotate(rotation);
        Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);
        bmp.recycle();
        return resizedBitmap;
    }
}
