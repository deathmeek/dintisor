package upb.com.smarttooth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;

/**
 * Created by adrian_nitu on 11/11/15.
 */
public class Config {
    public static final String TOOTH_MAC = "DB:AE:F9:E7:EB:6A";
    public static final int BUFFSIZE = 512;
    public static final int READ_INTERVAL = 3 * 1000;
    public static final String TOOTH_UUID_SERVICE = "180f";
    public static final String TOOTH_UUID_CHARAC_PH = "2a19";
    public static final String TOOTH_UUID_CHARAC_HUM = "2a19";
    public static final int GRAPH_WIDTH = 20;
    public static DateFormat dateformatIn = new SimpleDateFormat("yyyy-MM-dd hh:mm:ssZ");
    public static DateFormat dateformatOut = new SimpleDateFormat("mm:ss");
    public static float XLabelsAngle = 300f;
    public static int[] margins = new int[]{5,75,35,5};
    private static final String TOOTH_UUID = "131e922d-7f9e-49ab-827d-1b033f2bf585";
    public static UUID[] UUIDs = new UUID[]{UUID.fromString(Config.TOOTH_UUID)};
    public static float GRAPHSOFTNESS = (float) 0.5;
}
