package upb.com.smarttooth.UI;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import upb.com.smarttooth.Tooth;
import upb.com.smarttooth.storage.TransientStorage;

public abstract class BaseSmartToothActivity extends Activity {

    private String[] requiredPermissions = new String[]
            {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    private int REQUEST_ENABLE_BT = 5;
    private int REQUEST_PERMISSIONS = 123;

    protected Tooth tooth;

    protected boolean testPermissions() {
        boolean ret = true;
        for(int i = 0; i < requiredPermissions.length && ret; i++){
            String permission = requiredPermissions[i];
            ret = ret && ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
        }
        return ret;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!testPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(requiredPermissions, REQUEST_PERMISSIONS);
            } else {
                finish();
            }
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            //TODO display more persistent error
            finish();
        }
        tooth = Tooth.getInstance();
        tooth.createLogs(getApplicationContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("Permissions", grantResults.toString());
        if (!testPermissions()) {
            Toast.makeText(this, "We require access to all permissions", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TransientStorage.setTopMostActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
