package upb.com.smarttooth.Renderers;

import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.aakira.expandablelayout.ExpandableRelativeLayout;

import org.achartengine.ChartFactory;

import adrian.upb.smarttooth.R;
import upb.com.smarttooth.Config;
import upb.com.smarttooth.MainActivity;
import upb.com.smarttooth.Tooth;

public class Patient implements Renderer {
    @Override
    public void render(final View rootView) {
        final int[] titles = new int[]{R.id.textView_pacientName, R.id.textView_device, R.id.textView_realTimeData, R.id.textView_pastData};
        final int[] expandableLayoutsID = new int[]{R.id.expandableLayout_pacient, R.id.expandableLayout_device, R.id.expandableLayout_rtData, R.id.expandableLayout_pastData};
        final ExpandableRelativeLayout[] expandableLayouts = new ExpandableRelativeLayout[expandableLayoutsID.length];
        for(int i = 0; i < expandableLayoutsID.length; i++) {
            expandableLayouts[i] = (ExpandableRelativeLayout) rootView.findViewById(expandableLayoutsID[i]);
        }
        for(int i = 0; i < titles.length; i++){
            TextView title = (TextView) rootView.findViewById(titles[i]);
            final int finalI = i;
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for(int j = 0; j < titles.length; j++){
                        if(j != finalI) {
                            expandableLayouts[j].collapse();
                        }
                    }
                    expandableLayouts[finalI].toggle();
                    //# close the other exp layouts
                    if(expandableLayoutsID[finalI] == R.id.expandableLayout_rtData){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while(true){
                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    MainActivity.instance.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Tooth.getInstance().dataFrame.graph.repaint();
                                        }
                                    });

                                }
                            }
                        }).start();
                        LinearLayout graphLoc = (LinearLayout) rootView.findViewById(R.id.graph_root_view_rt);
                        Tooth.getInstance().dataFrame.graph = ChartFactory.getCubeLineChartView(rootView.getContext(),
                                Tooth.getInstance().dataFrame.dataset, Tooth.getInstance().dataFrame.renderer,
                                Config.GRAPHSOFTNESS);
                        graphLoc.addView(Tooth.getInstance().dataFrame.graph);
                    }
                    if(expandableLayoutsID[finalI] == R.id.expandableLayout_pastData ){
                        LinearLayout graphLoc = (LinearLayout) rootView.findViewById(R.id.graph_root_view_past);
                        Tooth.getInstance().dataFrameLong.graph = ChartFactory.getCubeLineChartView(rootView.getContext(),
                                Tooth.getInstance().dataFrameLong.dataset, Tooth.getInstance().dataFrameLong.renderer,
                                Config.GRAPHSOFTNESS);
                        graphLoc.addView(Tooth.getInstance().dataFrame.graph);
                    }
                }
            });
        }
    }

    @Override
    public void onOptionsItemSelected(MenuItem item) {

    }
}
