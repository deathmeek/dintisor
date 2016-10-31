package upb.com.smarttooth;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import adrian.upb.smarttooth.R;

public class LookupActivity extends Activity {
    public static Tooth tooth;
    ArrayList<String> macs = new ArrayList<String>();
    List<Map<String, String>> patients = new ArrayList<Map<String, String>>();
    List<Map<String, String>> devices = new ArrayList<Map<String, String>>();

    SimpleAdapter patientsAdapter;
    SimpleAdapter devicesAdapter;

    PersistentStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tooth.setActivity(this);
        tooth = Tooth.getInstance();
        storage = PersistentStorage.getInstance(this);
        setContentView(R.layout.activity_lookup);
        Button b = (Button) findViewById(R.id.button_beginScanning);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tooth.startScan();
            }
        });
        patientsAdapter = new SimpleAdapter(this, patients,
                android.R.layout.simple_list_item_2,
                new String[] {"First Line", "Second Line" },
                new int[] {android.R.id.text1, android.R.id.text2 });
        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(patientsAdapter);

        devicesAdapter = new SimpleAdapter(this, devices,
                android.R.layout.simple_list_item_2,
                new String[] {"First Line", "Second Line" },
                new int[] {android.R.id.text1, android.R.id.text2 });
        ListView lv2 = (ListView) findViewById(R.id.listView2);
        lv2.setAdapter(devicesAdapter);

        lv2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, String> item = devices.get(position);

                Intent intent = new Intent(LookupActivity.this,NewPacient.class);
                intent.putExtra("MAC", item.get("Second Line"));
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tooth.setActivity(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        tooth.stopScan();
    }

    public void addDevice(String name, String address) {
        for(String s : macs){
            if (address.equals(s)) {
                return;
            }
        }
        final Map<String, String> datum = new HashMap<String, String>(2);
        datum.put("First Line", "First line of text");
        datum.put("Second Line",address);
        final boolean[] addToPatients = {false};
        String pacientName = storage.getPacient(address);
        if(pacientName != null){
            datum.put("First Line", pacientName);//put name of the pacient
            addToPatients[0] = true;
        } else {
            datum.put("First Line", name);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(addToPatients[0]){
                    patients.add(datum);
                    patientsAdapter.notifyDataSetChanged();
                } else {
                    devices.add(datum);
                    devicesAdapter.notifyDataSetChanged();
                }
            }
        });
    }
}
