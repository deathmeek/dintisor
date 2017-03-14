package upb.com.smarttooth.UI.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.aakira.expandablelayout.ExpandableRelativeLayout;

import org.achartengine.ChartFactory;

import adrian.upb.smarttooth.R;
import upb.com.smarttooth.Config;
import upb.com.smarttooth.Tooth;
import upb.com.smarttooth.storage.TransientStorage;

public class Patient implements Renderer {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        final View rootView = inflater.inflate(R.layout.fragment_pacient, container, false);
        final int[] titles = new int[]{R.id.textView_realTimeData, R.id.textView_pastData};
        final int[] expandableLayoutsID = new int[]{R.id.expandableLayout_rtData, R.id.expandableLayout_pastData};
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
                                    TransientStorage.getTopMostActivity().runOnUiThread(new Runnable() {
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
                        Tooth.getInstance().dataFrame.graph = ChartFactory.getCubeLineChartView(rootView.getContext(),
                                Tooth.getInstance().dataFrame.dataset, Tooth.getInstance().dataFrame.renderer,
                                Config.GRAPHSOFTNESS);
                        graphLoc.addView(Tooth.getInstance().dataFrame.graph);
                    }
                }
            });
        }
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }
    @Override
    public String getTitle() {
        switch (Config.LANGUAGE){
            case ROMANIAN:
                return "Pacient";
            case ENGLISH:
                return "Patient";
        }
        return null;
    }

    @Override
    public int getMenu() {
        // no menu
        return 0;
    }

}
