package upb.com.smarttooth;

import android.provider.ContactsContract;
import android.util.Log;

import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;

import java.sql.Array;
import java.util.Arrays;
import java.util.Date;

public class Tooth {
    private static Tooth instance;
    private final ToothBluetoothManager bm;
    private boolean online = false;

    public Tooth(){
        instance = this;
        this.bm = new ToothBluetoothManager();
    }

    public static Tooth getInstance() {
        return instance;
    }

    public void update() {
        GraphicalView gp = null;
        GraphicalView gh = null;
        try {
            gp = DataStore.data.get("PH").graph;
            gh = DataStore.data.get("HUM").graph;
            return;
        } catch (Exception e){
            ;
        }
        DataStore.data.put("PH", new DataFrame(new String[]{"PH"}, gp));
        DataStore.data.put("HUM", new DataFrame(new String[]{"HUM"}, gh));
    }

    public void setOnline(boolean online) {
        this.online = online;
        if(online) {
            update();
        }
    }

    public boolean isOnline() {
        return online;
    }

    public static void updatePH(int value) {
        DataStore.data.get("PH").update(value);
    }

    public static void updateHUM(int value) {
        DataStore.data.get("HUM").update(value);
    }
}
