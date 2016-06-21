package upb.com.smarttooth;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;

import adrian.upb.smarttooth.R;

public class LookupActivity extends Activity {
    public static Tooth tooth = Tooth.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lookup);
        Button b = (Button) findViewById(R.id.button_beginScanning);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tooth.setActivity(this);
    }

}
