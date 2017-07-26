package upb.com.smarttooth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;

public class Config {
    public static final HashSet<String> TOOTH_MACs = new HashSet<String>();
    public static final int MIN_PULSE_TIME = 32;
    public static final int MIN_STIM_TIME = 1000 * 5 / (((32768) + ((((0) + 1) * 1000) / 2)) / (((0) + 1) * 1000));
    public enum Languages {ROMANIAN, ENGLISH};
    public static final Languages LANGUAGE = Languages.ROMANIAN;

    static {
        TOOTH_MACs.add("CA:79:B5:CE:FF:2E");
        TOOTH_MACs.add("E0:AF:DB:0E:9F:64");
        TOOTH_MACs.add("C3:C0:9F:D9:6E:74");
        TOOTH_MACs.add("D1:43:87:EE:3D:06");
        TOOTH_MACs.add("DB:AE:F9:E7:EB:6A");
        TOOTH_MACs.add("F9:EA:50:8A:36:97");
    }
    public static final int READ_INTERVAL = 10 * 1000;
    public static final String TOOTH_UUID_IN_SERVICE    = "131e922d-7f9e-49ab-827d-1b033f2bf585";
    public static final String TOOTH_UUID_IN_CHARAC_PH  = "131e0003-7f9e-49ab-827d-1b033f2bf585";
    //Todo
    public static final String TOOTH_UUID_IN_CHARAC_HUM = "131e0002-7f9e-49ab-827d-1b033f2bf585";
    public static final String TOOTH_UUID_OUT_SERVICE = "016fbc81-2eaf-418c-b579-e6c313374509";

    public static final String TOOTH_UUID_OUT_ST = "016f0000-2eaf-418c-b579-e6c313374509";
    public static final String TOOTH_UUID_OUT_TT = "016f0001-2eaf-418c-b579-e6c313374509";
    public static final String TOOTH_UUID_OUT_TA = "016f0002-2eaf-418c-b579-e6c313374509";
    public static final String TOOTH_UUID_OUT_TP = "016f0003-2eaf-418c-b579-e6c313374509";
    public static final String TOOTH_UUID_OUT_T1 = "016f0004-2eaf-418c-b579-e6c313374509";
    public static final String TOOTH_UUID_OUT_T2 = "016f0005-2eaf-418c-b579-e6c313374509";
    public static final String TOOTH_UUID_OUT_T3 = "016f0006-2eaf-418c-b579-e6c313374509";
    public static final String TOOTH_UUID_OUT_T4 = "016f0007-2eaf-418c-b579-e6c313374509";
    public static final String TOOTH_UUID_OUT_V  = "016f0008-2eaf-418c-b579-e6c313374509";

    public static final int GRAPH_WIDTH = 20;
    public static DateFormat dateformatOut = new SimpleDateFormat("mm:ss");
    public static float XLabelsAngle = 300f;
    public static int[] margins = new int[]{5,75,35,5};
    public static float GRAPHSOFTNESS = (float) 0.5;
    public static boolean USING_TEST_DEVICE = true;
}
