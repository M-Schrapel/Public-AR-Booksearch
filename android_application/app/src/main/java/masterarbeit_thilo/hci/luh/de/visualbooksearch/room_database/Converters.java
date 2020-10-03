package masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;

public class Converters {

    private static final String TAG = "Converters";
    private static Gson gson = new Gson();

    @TypeConverter
    public static String FloatArrayToString(float[] colorFeatureVector) {
        return gson.toJson(colorFeatureVector);
    }

    @TypeConverter
    public static float[] StringToFloatArray(String colorFeatureVector) {
        return gson.fromJson(colorFeatureVector, float[].class);
    }
}
