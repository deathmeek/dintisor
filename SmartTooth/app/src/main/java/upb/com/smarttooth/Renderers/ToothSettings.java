package upb.com.smarttooth.Renderers;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.github.aakira.expandablelayout.ExpandableRelativeLayout;

import adrian.upb.smarttooth.R;
import upb.com.smarttooth.Config;
import upb.com.smarttooth.Tooth;

public class ToothSettings implements Renderer {
    EditText Ta;
    EditText Tp;
    EditText Tt;
    EditText T1;
    EditText T2;
    EditText T3;
    EditText T4;
    final int[] expandableLayoutsID = new int[]{R.id.expandableLayout_T2, R.id.expandableLayout_T4};
    final ExpandableRelativeLayout[] expandableLayouts = new ExpandableRelativeLayout[expandableLayoutsID.length];

    private void updateT14(EditText e){
        int val = 0;
        try { val = Integer.parseInt(e.getText().toString()); } catch (Exception ex) {}
        val = val == 0 ? 0 : (val < Config.MIN_PULSE_TIME ? Config.MIN_PULSE_TIME : Config.MIN_PULSE_TIME * (val / Config.MIN_PULSE_TIME));
        if(val == 0)
            e.setText("");
        else
            e.setText(val + "");
        Tooth.getInstance().setCharact(e, val);
        updateTA();
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
        Tooth.getInstance().setCharact(Ta, val);
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
        Tooth.getInstance().setCharact(Tp, val);
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
        if(val == 0)
            Tt.setText("");
        else
            Tt.setText(val + "");
        Tooth.getInstance().setCharact(Tt, val);
    }

    @Override
    public void render(View rootView) {
        for(int i = 0; i < expandableLayoutsID.length; i++) {
            expandableLayouts[i] = (ExpandableRelativeLayout) rootView.findViewById(expandableLayoutsID[i]);
        }
        Ta = (EditText) rootView.findViewById(R.id.numberPickerTA);
        Tp = (EditText) rootView.findViewById(R.id.numberPickerTP);
        Tt = (EditText) rootView.findViewById(R.id.numberPickerTT);
        T1 = (EditText) rootView.findViewById(R.id.numberPickerT1);
        T2 = (EditText) rootView.findViewById(R.id.numberPickerT2);
        T3 = (EditText) rootView.findViewById(R.id.numberPickerT3);
        T4 = (EditText) rootView.findViewById(R.id.numberPickerT4);
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
                if(hasFocus)
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
    }

    @Override
    public void onOptionsItemSelected(MenuItem item) {
        for(int i = 0; i < expandableLayoutsID.length; i++) {
            expandableLayouts[i].toggle();
        }
    }
}
