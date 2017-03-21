package upb.com.smarttooth.UI.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import adrian.upb.smarttooth.R;
import upb.com.smarttooth.Config;
import upb.com.smarttooth.Tooth;
import upb.com.smarttooth.storage.TransientStorage;

public class ToothSettings implements Renderer {
    EditText Ta;
    EditText Tp;
    EditText Tt;
    EditText T1;
    EditText T2;
    EditText T3;
    EditText T4;
    TextView f;
    public static TextView status;
    private View rootView;
    private static ToothSettings instance;

    public static ToothSettings getInstance() {
        return instance;
    }

    @Override
    public String getTitle() {
        switch (Config.LANGUAGE){
            case ROMANIAN:
                return "Setări Waveform";
            case ENGLISH:
                return "Waveform Settings";
        }
        return null;
    }

    @Override
    public int getMenu() {
        return R.menu.tooth_settings;
    }

    private void updateT14(EditText e){
        int val = 0;
        try { val = Integer.parseInt(e.getText().toString()); } catch (Exception ex) {}
        val = val == 0 ? 0 : (val < Config.MIN_PULSE_TIME ? Config.MIN_PULSE_TIME : Config.MIN_PULSE_TIME * (val / Config.MIN_PULSE_TIME));
        if(val == 0)
            e.setText("");
        else
            e.setText(val + "");
        Tooth.getInstance().enqueueWrite(remapView(e), val);
        updateTA();
    }

    private int remapView(View e) {
        return e.getId();
    }

    private void updateTA(){
        int val = 0;
        try { val = Integer.parseInt(Ta.getText().toString()); } catch (Exception ex) {}
        int sum;
        int v1 = 0, v2 = 0, v3 = 0, v4 = 0;
        try { v1 = Integer.parseInt(T1.getText().toString()); } catch (Exception ex) {}
        try { v2 = Integer.parseInt(T2.getText().toString()); } catch (Exception ex) {}
        try { v3 = Integer.parseInt(T3.getText().toString()); } catch (Exception ex) {}
        try { v4 = Integer.parseInt(T4.getText().toString()); } catch (Exception ex) {}
        sum = v1 + v2 + v3 + v4;

        if(sum == 0) {
            return;
        }
        val = val < sum ? sum : sum * (val / sum);
        if(val == 0)
            Ta.setText("");
        else
            Ta.setText(val + "");
        // 1000 ms / sum(ms)
        float fVal = 1000.0f/val;
        f.setText(fVal + "");
        Tooth.getInstance().enqueueWrite(remapView(Ta), val);
        updateTT();
    }
    private void updateTP(){
        int val=0;
        try { val = Integer.parseInt(Tp.getText().toString()); } catch (Exception ex) {}
        val = val == 0 ? 0 : (val < Config.MIN_PAUSE_TIME ? Config.MIN_PAUSE_TIME : Config.MIN_PAUSE_TIME * (val / Config.MIN_PAUSE_TIME));
        if(val == 0)
            Tp.setText("");
        else
            Tp.setText(val + "");
        Tooth.getInstance().enqueueWrite(remapView(Tp), val);
        updateTT();
    }
    private void updateTT() {
        int sum;
        int va = 0, vp = 0;
        int val=0;
        try { val = Integer.parseInt(Tt.getText().toString()); } catch (Exception ex) {}
        try { va = Integer.parseInt(Ta.getText().toString()); } catch (Exception ex) {}
        try { vp = Integer.parseInt(Tp.getText().toString()); } catch (Exception ex) {}

        sum = va + vp;
        if(sum == 0) {
            return;
        }
        val = val < sum ? sum : sum * (val / sum);
        if(val == 0) {
            Tt.setText("");
        } else {
            Tt.setText(val + "");
        }
        Tooth.getInstance().enqueueWrite(remapView(Tt), val);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                      Bundle savedInstanceState){
        this.instance = this;
        rootView = inflater.inflate(R.layout.fragment_tooth_settings, container, false);
        final boolean[] start = {false};
        final Button b = (Button)rootView.findViewById(R.id.button_start);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start[0] = !start[0];
                Tooth.getInstance().enqueueWrite(remapView(b), start[0] ? 1 : 0);
            }
        });
        Ta = (EditText) rootView.findViewById(R.id.numberPickerTA);
        Tp = (EditText) rootView.findViewById(R.id.numberPickerTP);
        Tt = (EditText) rootView.findViewById(R.id.numberPickerTT);
        T1 = (EditText) rootView.findViewById(R.id.numberPickerT1);
        T2 = (EditText) rootView.findViewById(R.id.numberPickerT2);
        T3 = (EditText) rootView.findViewById(R.id.numberPickerT3);
        T4 = (EditText) rootView.findViewById(R.id.numberPickerT4);
        f = (TextView) rootView.findViewById(R.id.textView_f);
        status = ( TextView) rootView.findViewById(R.id.textView_status);
        View.OnFocusChangeListener l14 = new  View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                    return;
                EditText e = (EditText) v;
                updateT14(e);
            }
        };
        T1.setOnFocusChangeListener(l14);
        T2.setOnFocusChangeListener(l14);
        T3.setOnFocusChangeListener(l14);
        T4.setOnFocusChangeListener(l14);
        Ta.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    return;
                updateTA();
            }
        });
        Tp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    return;
                updateTP();
            }
        });

        Tt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    return;
                updateTT();
            }
        });
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_adv){

        }
        else
        {
            Tooth.getInstance().resetBluetooth();
        }
        return true;
    }

    public void update(final int id, final int value) {
        TransientStorage.getTopMostActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(id == R.id.button_start){
                    //TODO set button
                } else {
                    EditText e = (EditText) rootView.findViewById(id);
                    e.setText(value + "");
                }
            }
        });

    }
}
