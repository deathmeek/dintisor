package upb.com.smarttooth;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import adrian.upb.smarttooth.R;

public class NewPacient extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_pacient);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent i = getIntent();

        TextView mactv = (TextView) findViewById(R.id.textView_MAC);
        final String mac = i.getStringExtra("MAC");
        mactv.setText(mac);
        Button add = (Button) findViewById(R.id.button_add);
        final EditText et = (EditText) findViewById(R.id.editText_pacient);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pacientName = String.valueOf(et.getText());
                if (et.equals("")) {
                    Toast.makeText(NewPacient.this, "Please enter a pacient name", Toast.LENGTH_SHORT);
                } else {
                    PersistentStorage.getInstance(NewPacient.this).createPacient(pacientName, mac);
                    finish();
                }
            }
        });
    }

}
