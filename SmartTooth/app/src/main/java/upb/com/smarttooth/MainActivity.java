package upb.com.smarttooth;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;

import java.util.Date;
import java.util.logging.StreamHandler;

import adrian.upb.smarttooth.R;

public class MainActivity extends Activity {

    public static MainActivity instance;
    public static String error = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        Tooth tooth = new Tooth();
        setContentView(R.layout.activity_main);
        new DataStore();
        DataStore.pullCachedData();
        if(tooth.isOnline()) {
            //tooth.update();
        }
        LinearLayout graphLocPH = (LinearLayout) findViewById(R.id.graph_root_view_PH);
        LinearLayout graphLocHUM = (LinearLayout) findViewById(R.id.graph_root_view_HUM);
        populateView(graphLocPH, DataStore.data.get("PH"));
        populateView(graphLocHUM, DataStore.data.get("HUM"));
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DataFrame f = DataStore.data.get("PH");
                            f.valuesDate[0] = new Date();
                            f.graph.repaint();
                        }
                    });

                }
            }
        }).start();
    }
    public void populateView(LinearLayout rootView, DataFrame frame) {
        frame.graph = ChartFactory.getCubeLineChartView(rootView.getContext(), frame.dataset, frame.renderer, Config.GRAPHSOFTNESS);
        rootView.addView(frame.graph);
    }
}
