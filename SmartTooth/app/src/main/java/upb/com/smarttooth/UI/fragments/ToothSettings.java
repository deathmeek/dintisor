package upb.com.smarttooth.UI.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.textservice.TextInfo;
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

    private void updateT14(EditText e)
    {
        int val;
        try {
            val = Integer.parseInt(e.getText().toString());
//            val = val == 0 ? 0 : (val < Config.MIN_PULSE_TIME ? Config.MIN_PULSE_TIME : Config.MIN_PULSE_TIME * (val / Config.MIN_PULSE_TIME));
            e.setText(Integer.toString(val));
        } catch (Exception ex) {
            e.setText("");
            return;
        }
        Tooth.getInstance().enqueueWrite(remapView(e), val);
        Tooth.getInstance().enqueueRead(remapView(e));
    }

    private int remapView(View e) {
        return e.getId();
    }

    private void updateTA()
    {
        int val;
        try {
            val = Integer.parseInt(Ta.getText().toString());
//            val = val == 0 ? 0 : (val < Config.MIN_PULSE_TIME ? Config.MIN_PULSE_TIME : Config.MIN_PULSE_TIME * (val / Config.MIN_PULSE_TIME));
            Ta.setText(Integer.toString(val));
        } catch (Exception ex) {
            Ta.setText("");
            return;
        }
        Tooth.getInstance().enqueueWrite(remapView(Ta), val);
        Tooth.getInstance().enqueueRead(remapView(Ta));
    }

    private void updateTP()
    {
        int val;
        try {
            val = Integer.parseInt(Tp.getText().toString());
//            val = val == 0 ? 0 : (val < Config.MIN_PULSE_TIME ? Config.MIN_PULSE_TIME : Config.MIN_PULSE_TIME * (val / Config.MIN_PULSE_TIME));
            Tp.setText(Integer.toString(val));
        } catch (Exception ex) {
            Tp.setText("");
            return;
        }
        Tooth.getInstance().enqueueWrite(remapView(Tp), val);
        Tooth.getInstance().enqueueRead(remapView(Tp));
    }

    private void updateTT() {
        int val;
        try {
            val = Integer.parseInt(Tt.getText().toString());
//            val = val == 0 ? 0 : (val < Config.MIN_STIM_TIME ? Config.MIN_STIM_TIME : val);
            Tt.setText(Integer.toString(val));
        } catch (Exception ex) {
            Tt.setText("");
            return;
        }
        Tooth.getInstance().enqueueWrite(remapView(Tt), val);
        Tooth.getInstance().enqueueRead(remapView(Tt));
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
                start[0] = true;
                Tooth.getInstance().enqueueWrite(remapView(b), 1);
            }
        });
        final Button bstop = (Button)rootView.findViewById(R.id.button_stop);
        bstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start[0] = false;
                Tooth.getInstance().enqueueWrite(remapView(bstop), 0);
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
        TextView.OnEditorActionListener l14 = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    EditText e = (EditText) v;
                    updateT14(e);
                }
                return false;
            }
        };
        T1.setOnEditorActionListener(l14);
        T2.setOnEditorActionListener(l14);
        T3.setOnEditorActionListener(l14);
        T4.setOnEditorActionListener(l14);
        Ta.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    updateTA();
                }
                return false;
            }
        });
        Tp.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    updateTP();
                }
                return false;
            }
        });
        Tt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    updateTT();
                }
                return false;
            }
        });
        if(Tt.getText().length() == 0)
            Tooth.getInstance().readAllCharac();
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
                    e.setText(Integer.toString(value));
                }
            }
        });
    }
}
