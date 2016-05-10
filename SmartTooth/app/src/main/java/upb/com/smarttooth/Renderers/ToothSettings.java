package upb.com.smarttooth.Renderers;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.aakira.expandablelayout.ExpandableRelativeLayout;

import adrian.upb.smarttooth.R;
import upb.com.smarttooth.DataFrame;
import upb.com.smarttooth.Tooth;

public class ToothSettings implements Renderer {
    final int[] expandableLayoutsID = new int[]{R.id.expandableLayout_T2, R.id.expandableLayout_T4};
    final ExpandableRelativeLayout[] expandableLayouts = new ExpandableRelativeLayout[expandableLayoutsID.length];
    boolean change = true;
    @Override
    public void render(View rootView) {
        for(int i = 0; i < expandableLayoutsID.length; i++) {
            expandableLayouts[i] = (ExpandableRelativeLayout) rootView.findViewById(expandableLayoutsID[i]);
        }
        final EditText Ta = (EditText) rootView.findViewById(R.id.numberPickerTA);
        final EditText Tp = (EditText) rootView.findViewById(R.id.numberPickerTP);
        final EditText Tt = (EditText) rootView.findViewById(R.id.numberPickerTT);
        final EditText T1 = (EditText) rootView.findViewById(R.id.numberPickerT1);
        final EditText T2 = (EditText) rootView.findViewById(R.id.numberPickerT2);
        final EditText T3 = (EditText) rootView.findViewById(R.id.numberPickerT3);
        final EditText T4 = (EditText) rootView.findViewById(R.id.numberPickerT4);
        Ta.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (change) {
                    change = false;
                    int value = Integer.parseInt(String.valueOf(s));
                    int v1 = 0, v2 = 0, v3 = 0, v4 = 0;
                    try {
                        v1 = Integer.parseInt(T1.getText().toString());
                        Tooth.getInstance().setCharact(Tooth.getInstance().T1Charac, v1);
                    } catch (Exception e) {
                        ;
                    }
                    try {
                        v2 = Integer.parseInt(T2.getText().toString());
                        Tooth.getInstance().setCharact(Tooth.getInstance().T2Charac, v2);
                    } catch (Exception e) {
                        ;
                    }
                    try {
                        v3 = Integer.parseInt(T3.getText().toString());
                        Tooth.getInstance().setCharact(Tooth.getInstance().T3Charac, v3);
                    } catch (Exception e) {
                        ;
                    }
                    try {
                        v4 = Integer.parseInt(T4.getText().toString());
                        Tooth.getInstance().setCharact(Tooth.getInstance().T4Charac, v4);
                    } catch (Exception e) {
                        ;
                    }
                    int sum = v1 + v2 + v3 + v4;
                    if (sum == 0) {
                        return;
                    }
                    int mult = (value + sum - 1) / sum;
                    value = mult * sum;
                    Ta.setText(value + "");
                } else {
                    change = true;
                }
            }
        });
        Tt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (change) {
                    change = false;
                    int value = Integer.parseInt(String.valueOf(s));
                    int vp = 0, va = 0;
                    try {
                        va = Integer.parseInt(Ta.getText().toString());
                        Tooth.getInstance().setCharact(Tooth.getInstance().TACharac, va);
                    } catch (Exception e) {
                        ;
                    }
                    try {
                        vp = Integer.parseInt(Tp.getText().toString());
                        Tooth.getInstance().setCharact(Tooth.getInstance().TPCharac, vp);
                    } catch (Exception e) {
                        ;
                    }
                    int sum = va + vp;
                    if (sum == 0) {
                        return;
                    }
                    int mult = (value + sum - 1) / sum;
                    value = mult * sum;
                    Tt.setText(value + "");
                } else {
                    change = true;
                }
            }
        });
    }

    @Override
    public void onOptionsItemSelected(MenuItem item) {
        for(int i = 0; i < expandableLayoutsID.length; i++) {
            expandableLayouts[i].toggle();
            Log.d("test", "meniu merge ish " + expandableLayouts[i]);
        }
    }
}
