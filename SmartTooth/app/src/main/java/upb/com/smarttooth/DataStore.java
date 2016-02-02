package upb.com.smarttooth;

import java.util.HashMap;

public class DataStore {
    String[] measurements = new String[]{"ph", "hum"};
    public static HashMap<String, DataFrame> data = new HashMap<String, DataFrame>();

    public static void pullCachedData() {
        //for(int i = 0; i < measurements.length; i++) {
            //data[i] = new DataFrame(measurements[i] + ".json");
        //}
        //dirty dirty hack
        Tooth.getInstance().update();
     }
}
