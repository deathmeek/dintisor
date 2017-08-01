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
    TextView d;
    TextView v;
    TextView stim;
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

    private void updateTs(EditText e)
    {
        int val;
        try {
            val = Integer.parseInt(e.getText().toString());
            if (val == Tooth.getInstance().getValue(e.getId()))
                return;

            Tooth.getInstance().enqueueWrite(remapView(e), val);
            Tooth.getInstance().enqueueRead(remapView(e));
        } catch (NumberFormatException ex) {
            e.setText("");
        }
    }

    private int remapView(View e) {
        return e.getId();
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
        d = (TextView) rootView.findViewById(R.id.textView_d);
        v = (TextView) rootView.findViewById(R.id.textView_Voltage);
        stim = (TextView) rootView.findViewById(R.id.textView_stimulation);
        T3.setKeyListener(null);
        T4.setKeyListener(null);
        f.setKeyListener(null);
        d.setKeyListener(null);
        v.setKeyListener(null);
        status = ( TextView) rootView.findViewById(R.id.textView_status);

        TextView.OnEditorActionListener al14 = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE)
                    updateTs((EditText) v);

                return false;
            }
        };
        View.OnFocusChangeListener fl14 = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                    updateTs((EditText) v);
            }
        };
        T1.setOnEditorActionListener(al14);
        T2.setOnEditorActionListener(al14);
        T3.setOnEditorActionListener(al14);
        T4.setOnEditorActionListener(al14);
        Ta.setOnEditorActionListener(al14);
        Tp.setOnEditorActionListener(al14);
        Tt.setOnEditorActionListener(al14);
        T1.setOnFocusChangeListener(fl14);
        T2.setOnFocusChangeListener(fl14);
        T3.setOnFocusChangeListener(fl14);
        T4.setOnFocusChangeListener(fl14);
        Ta.setOnFocusChangeListener(fl14);
        Tp.setOnFocusChangeListener(fl14);
        Tt.setOnFocusChangeListener(fl14);

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
                } else if(id == R.id.textView_Voltage) {
                    TextView t = (TextView) rootView.findViewById(id);
                    t.setText(value + " mV");
                } else if(id == R.id.textView_stimulation) {
                    TextView t = (TextView) rootView.findViewById(id);
                    if(value != 0)
                        t.setText("Stimulation on");
                    else
                        t.setText("Stimulation off");
                } else {
                    EditText e = (EditText) rootView.findViewById(id);
                    e.setText(Integer.toString(value));

                    if(id == R.id.numberPickerT1 || id == R.id.numberPickerT2)
                    {
                        try {
                            int valT1 = Integer.parseInt(T1.getText().toString());
                            int valT2 = Integer.parseInt(T2.getText().toString());
                            float valf = 1000000.0f / (valT1 + valT2);
                            float vald = 100.0f * valT1 / (valT1 + valT2);
                            String suff = "";
                            if(valf >= 1000)
                            {
                                valf /= 1000;
                                suff = "K";
                            } else if(valf >= 1000000)
                            {
                                valf /= 1000000;
                                suff = "M";
                            }
                            f.setText(String.format("%.2f %sHz", valf, suff));
                            d.setText(String.format("%.2f %%", vald));
                        } catch (NumberFormatException ex) {}
                    }
                }
            }
        });
    }
}
