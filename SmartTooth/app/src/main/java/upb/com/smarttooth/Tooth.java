package upb.com.smarttooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import adrian.upb.smarttooth.R;
import upb.com.smarttooth.UI.fragments.ToothSettings;
import upb.com.smarttooth.chart.DataFrame;
import upb.com.smarttooth.storage.TransientStorage;

public class Tooth {

    public void startScan() {
        try {
            bluetoothAdapter.stopLeScan(cbLocate);
        } catch (Exception e) {

        }
        bluetoothAdapter.startLeScan(cbLocate);
    }

    public void stopScan() {
        bluetoothAdapter.stopLeScan(cbLocate);
    }

    public class CharacWrapper {
        boolean write;
        BluetoothGattCharacteristic c;
        public int value;

        public String toString()
        {
            return c != null ? getName(c.getUuid().toString()) : "null";
        }
    }

    static volatile public boolean online;
    private BluetoothAdapter bluetoothAdapter;
    BluetoothGatt bluetoothGatt;
    public BluetoothGattCharacteristic phCharac;
    public BluetoothGattCharacteristic humCharac;
    public BluetoothGattCharacteristic TTCharac;
    public BluetoothGattCharacteristic TPCharac;
    public BluetoothGattCharacteristic TACharac;
    public BluetoothGattCharacteristic T1Charac;
    public BluetoothGattCharacteristic T2Charac;
    public BluetoothGattCharacteristic T3Charac;
    public BluetoothGattCharacteristic T4Charac;
    public BluetoothGattCharacteristic VCharac;
    public BluetoothGattCharacteristic STCharac;

    List<CharacWrapper> map = new ArrayList<CharacWrapper>();
    BluetoothGattCharacteristic target = null;
    boolean real_time_enabled = true;

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private FileWriter logPh = null;
    private FileWriter logHum = null;
    private FileWriter logV = null;

    private static Tooth instance;
    public final DataFrame dataFrame;
    BluetoothAdapter.LeScanCallback cbLocate = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d("BluetoothLocate", "device found - " + device.getAddress() + ", "
                    + device.getName() + ", " + Arrays.toString(device.getUuids()));

            TransientStorage.addDevice(device);
        }
    };

    public void resetBluetooth() {
        disconnectBluetooth();

        stopScan();
        startScan();
    }

    public void connectBluetooth(BluetoothDevice device) {
        bluetoothGatt = device.connectGatt(null, false, btleGattCallback);
        Log.d("tooth", bluetoothGatt.toString());
    }

    public void disconnectBluetooth() {
        if (bluetoothGatt == null)
            return;

        bluetoothGatt.disconnect();
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    public void createLogs(Context context)
    {
        String sdState = Environment.getExternalStorageState();
        if(!Environment.MEDIA_MOUNTED.equals(sdState))
            return;

        File sdRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File sdLogDir = new File(sdRoot, "Microsal");
        if(!sdLogDir.mkdirs())
            Log.w("log", "directory not created");
        Log.i("log", "log path " + sdLogDir.getAbsolutePath());
        try {
            Date now = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

            File filePh = new File(sdLogDir, formatter.format(now) + "_pH.csv");
            File fileHum = new File(sdLogDir, formatter.format(now) + "_humidity.csv");
            File fileV = new File(sdLogDir, formatter.format(now) + "_voltage.csv");

            if(logPh == null) {
                logPh = new FileWriter(filePh);
                logPh.write("Timestamp, pH\n");
            }

            if(logHum == null) {
                logHum = new FileWriter(fileHum);
                logHum.write("Timestamp, Humidity\n");
            }

            if(logV == null) {
                logV = new FileWriter(fileV);
                logV.write("Timestamp, Voltage\n");
            }

            MediaScannerConnection.scanFile(context, new String[]{filePh.getAbsolutePath(), fileHum.getAbsolutePath(), fileV.getAbsolutePath()}, null, null);
        } catch(IOException ex) {
            Log.w("log", ex.toString());
        }
    }

    private Tooth() {
        instance = this;
        dataFrame = new DataFrame(new String[]{"PH", "Humidity"}, null, true);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // start a thread to schedule reads
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(Config.READ_INTERVAL);
                        if (bluetoothGatt != null) {
                            if (online) {
                                if (phCharac != null) {
                                    enqueueRead(phCharac);
                                }
                                if (humCharac != null) {
                                    enqueueRead(humCharac);
                                }
                                if (VCharac != null) {
                                    enqueueRead(VCharac);
                                }
                                if (STCharac != null) {
                                    enqueueRead(STCharac);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    public static Tooth getInstance() {
        if (instance == null) {
            instance = new Tooth();
        }
        return instance;
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            //watchdog.reset();
            Log.i("println", "Characteristic " + getName(characteristic.getUuid().toString()) + "was written ");
            CharacWrapper dr = null;
            synchronized (map) {
                for (CharacWrapper cw : map) {
                    if (cw.c == characteristic && cw.write) {
                        dr = cw;
                        break;
                    }
                }
                map.remove(dr);
            }
            scheduleNext();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            // this will get called anytime you perform a read or write characteristic operation
            //watchdog.reset();
            synchronized (map) {
                CharacWrapper dr = null;
                for (CharacWrapper cw : map) {
                    if (cw.c == characteristic && !cw.write) {
                        dr = cw;
                        break;
                    }
                }
                map.remove(dr);
            }
            int value = -1;
            if (characteristic == phCharac) {
                float v = ByteBuffer.wrap(characteristic.getValue()).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                value = (int)(v * 1000);
                Log.i("pH", "" + v);
                dataFrame.update(value, DataFrame.DataType.PH);
                try {
                    if (logPh != null) {
                        logPh.write(formatter.format(new Date()) + ", " + Integer.toString(value) + "\n");
                        logPh.flush();
                    }
                } catch(IOException ex) {
                    Log.w("log", ex.toString());
                }
            } else if (characteristic == humCharac) {
                float v = ByteBuffer.wrap(characteristic.getValue()).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                value = (int) (v * 1000);
                Log.i("hum", "" + v);
                dataFrame.update(value, DataFrame.DataType.Humidity);
                try {
                    if (logHum != null) {
                        logHum.write(formatter.format(new Date()) + ", " + Integer.toString(value) + "\n");
                        logHum.flush();
                    }
                } catch(IOException ex) {
                    Log.w("log", ex.toString());
                }
            } else if (characteristic == T1Charac || characteristic == T2Charac ||
                    characteristic == T3Charac || characteristic == T4Charac ||
                    characteristic == TACharac || characteristic == TPCharac ||
                    characteristic == TTCharac || characteristic == VCharac) {
                value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                updateUI(characteristic, value);

                if(characteristic == VCharac) {
                    try {
                        if (logV != null) {
                            logV.write(formatter.format(new Date()) + ", " + Integer.toString(value) + "\n");
                            logV.flush();
                        }
                    } catch (IOException ex) {
                        Log.w("log", ex.toString());
                    }
                }
            } else if (characteristic == STCharac) {
                value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                Log.i("stim", "" + value);
                updateUI(characteristic, value);
            }
            scheduleNext();
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i("Tooth", "state changed: " + newState);
            Tooth.online = newState == BluetoothProfile.STATE_CONNECTED;
            if (Tooth.online) {
                gatt.discoverServices();
                TransientStorage.getTopMostActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ToothSettings.status.setText("Connected");
                        } catch (Exception e) {

                        }
                    }
                });
            } else {
                TransientStorage.getTopMostActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ToothSettings.status.setText("Dropped");
                        } catch (Exception e) {

                        }
                    }
                });
                map.clear();
                connectBluetooth(gatt.getDevice());
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a            BluetoothGatt.discoverServices() call
            Log.i("println", "onServicesDiscovered status was " + status);
            for (BluetoothGattService service : bluetoothGatt.getServices()) {
                if (service.getUuid().toString().contains(Config.TOOTH_UUID_IN_SERVICE)) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_IN_CHARAC_PH)) {
                            Log.i("println", "Found Ph " + characteristic.getUuid().toString());
                            phCharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_IN_CHARAC_HUM)) {
                            Log.i("println", "Found Hum " + characteristic.getUuid().toString());
                            humCharac = characteristic;
                        }
                    }
                }
                if (service.getUuid().toString().contains(Config.TOOTH_UUID_OUT_SERVICE)) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_TT)) {
                            Log.i("println", "Found TT");
                            TTCharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_TA)) {
                            Log.i("println", "Found TA");
                            TACharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_TP)) {
                            Log.i("println", "Found TP");
                            TPCharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_T1)) {
                            Log.i("println", "Found T1");
                            T1Charac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_T2)) {
                            Log.i("println", "Found T2");
                            T2Charac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_T3)) {
                            Log.i("println", "Found T3");
                            T3Charac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_T4)) {
                            Log.i("println", "Found T4");
                            T4Charac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_ST)) {
                            Log.i("println", "Found ST");
                            Log.i("println", characteristic.getUuid().toString());
                            STCharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_V)) {
                            Log.i("println", "Found V");
                            VCharac = characteristic;
                        }
                    }
                }
            }
            readAllCharac();
        }
    };

    public void readAllCharac() {
        enqueueRead(R.id.textView_Voltage);
        enqueueRead(R.id.textView_stimulation);
        enqueueRead(R.id.numberPickerT1);
        enqueueRead(R.id.numberPickerT2);
        enqueueRead(R.id.numberPickerT3);
        enqueueRead(R.id.numberPickerT4);
        enqueueRead(R.id.numberPickerTA);
        enqueueRead(R.id.numberPickerTP);
        enqueueRead(R.id.numberPickerTT);
    }

    private void updateUI(BluetoothGattCharacteristic characteristic, int value) {
        int id = remap(characteristic);
        ToothSettings ts = ToothSettings.getInstance();
        if (ts != null)
            ts.update(id, value);
    }

    private void scheduleNext() {
        synchronized (map) {
            Log.i("schedule", map.toString());
            // writes have priority
            for(CharacWrapper c : map)
            {
                if(c.write)
                {
                    bluetoothGatt.writeCharacteristic(c.c);
                    return;
                }
            }

            // do one read
            Iterator<CharacWrapper> it = map.iterator();
            if (it.hasNext()) {
                CharacWrapper c = it.next();
                if (c.write) {
                    Log.w("schedule", "write characteristic still available");
                } else {
                    bluetoothGatt.readCharacteristic(c.c);
                }
                //this.watchdog.start();
            }
        }
    }

    private String getName(String UUID) {
        if (UUID.equals(Config.TOOTH_UUID_OUT_ST)) {
            return "Start";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_V)) {
            return "Voltage";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_T1)) {
            return "T1";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_T2)) {
            return "T2";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_T3)) {
            return "T3";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_T4)) {
            return "T4";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_TA)) {
            return "TA";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_TT)) {
            return "TT";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_TP)) {
            return "TP";
        }
        if (UUID.equals(Config.TOOTH_UUID_IN_CHARAC_HUM)) {
            return "Hum";
        }
        if (UUID.equals(Config.TOOTH_UUID_IN_CHARAC_PH)) {
            return "pH";
        }
        return null;
    }

    private int remapFormat(int id) {
        if (id != R.id.button_start && id != R.id.button_stop) {
            return BluetoothGattCharacteristic.FORMAT_UINT32;
        }
        return BluetoothGattCharacteristic.FORMAT_UINT8;
    }

    public void enqueueWrite(int id, int val) {
        BluetoothGattCharacteristic charac = remap(id);

        if(charac == null) {
            Log.w("enque write", "characteristic not found (yet)");
            return;
        }

        Log.e("ceva", "enqueueWrite " + val);
        synchronized (map) {
            charac.setValue(val, remapFormat(id), 0);
            if (map.isEmpty()) {
                target = charac;
                bluetoothGatt.writeCharacteristic(charac);
                //this.watchdog.start();
            }
            CharacWrapper cw = new CharacWrapper();
            cw.c = charac;
            cw.write = true;
            map.add(cw);
        }
    }


    public void enqueueRead(int id) {
        BluetoothGattCharacteristic charac = remap(id);
        enqueueRead(charac);
    }

    private void enqueueRead(BluetoothGattCharacteristic charac) {
        if(charac == null) {
            Log.w("enque write", "characteristic not found (yet)");
            return;
        }

        synchronized (map) {
            CharacWrapper cw = new CharacWrapper();
            cw.c = charac;
            cw.write = false;
            map.add(cw);
            Log.i("bla", map.toString());
            if(map.size() == 1)
                scheduleNext();
        }
    }

    private BluetoothGattCharacteristic remap(int id) {
        switch (id) {
            case R.id.button_start: {
                return STCharac;
            }
            case R.id.button_stop: {
                return STCharac;
            }
            case R.id.textView_stimulation: {
                return STCharac;
            }
            case R.id.textView_Voltage: {
                return VCharac;
            }
            case R.id.numberPickerT1: {
                return T1Charac;
            }
            case R.id.numberPickerT2: {
                return T2Charac;
            }
            case R.id.numberPickerT3: {
                return T3Charac;
            }
            case R.id.numberPickerT4: {
                return T4Charac;
            }
            case R.id.numberPickerTA: {
                return TACharac;
            }
            case R.id.numberPickerTP: {
                return TPCharac;
            }
            case R.id.numberPickerTT: {
                return TTCharac;
            }
            default:
                return null;
        }
    }

    private int remap(BluetoothGattCharacteristic c) {
        if (c == STCharac) {
            return R.id.textView_stimulation;
        }
        if (c == VCharac) {
            return R.id.textView_Voltage;
        }
        if (c == T1Charac) {
            return R.id.numberPickerT1;
        }
        if (c == T2Charac) {
            return R.id.numberPickerT2;
        }
        if (c == T3Charac) {
            return R.id.numberPickerT3;
        }
        if (c == T4Charac) {
            return R.id.numberPickerT4;
        }
        if (c == TACharac) {
            return R.id.numberPickerTA;
        }
        if (c == TPCharac) {
            return R.id.numberPickerTP;
        }
        if (c == TTCharac) {
            return R.id.numberPickerTT;
        }
        return -1;
    }
}

