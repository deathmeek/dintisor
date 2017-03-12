package upb.com.smarttooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransientStorage {

    private static Map<String, BluetoothDevice> lookupMap = new HashMap<>();
    private static List<BluetoothDevice> deviceList = new ArrayList<>();

    private static Activity topMostActivity;

    private TransientStorage() {
    }

    public static synchronized void addDevice(final BluetoothDevice device) {
        BluetoothDevice syncedDevice = lookupMap.get(device.getAddress());
        if (syncedDevice == null) {
            lookupMap.put(device.getAddress(), device);
            deviceList.add(device);
            topMostActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(topMostActivity, "Got a device " + device.getAddress(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public static List<BluetoothDevice> getDevices() {
        return deviceList;
    }

    public static Activity getTopMostActivity() {
        return topMostActivity;
    }

    public static void setTopMostActivity(Activity topMostActivity) {
        TransientStorage.topMostActivity = topMostActivity;
    }

    public void clearTransientStorage() {
        lookupMap = new HashMap<>();
        deviceList = new ArrayList<>();
    }

}
