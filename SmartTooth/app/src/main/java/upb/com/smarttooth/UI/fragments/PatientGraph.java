package upb.com.smarttooth.UI.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.achartengine.ChartFactory;

import adrian.upb.smarttooth.R;
import upb.com.smarttooth.Config;
import upb.com.smarttooth.Tooth;

public class PatientGraph implements Renderer {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        final View rootView = inflater.inflate(R.layout.fragment_pacient, container, false);
        LinearLayout graphLoc = (LinearLayout) rootView.findViewById(R.id.graph_root_view_rt);
        Tooth.getInstance().dataFrame.graph = ChartFactory.getCubeLineChartView(rootView.getContext(),
                Tooth.getInstance().dataFrame.dataset, Tooth.getInstance().dataFrame.renderer,
                Config.GRAPHSOFTNESS);
        graphLoc.addView(Tooth.getInstance().dataFrame.graph);
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
                return "PatientRealTime";
            case ENGLISH:
                return "PatientRealTime";
        }
        return null;
    }

    @Override
    public int getMenu() {
        // no menu
        return 0;
    }

}
