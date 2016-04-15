package upb.com.smarttooth;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.aakira.expandablelayout.ExpandableRelativeLayout;

import org.achartengine.ChartFactory;

import java.util.Date;

import adrian.upb.smarttooth.R;

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static Activity instance;
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    public static String[] pagini = new String []{ "Pacient", "Selecție pacient",
        "Access la date", "Opțiuni", "Setări waveform"};
    public static int[] view = new int[]{R.layout.fragment_pacient, R.layout.fragment_select_pacient,
            R.layout.fragment_data_access, R.layout.fragment_options, R.layout.fragment_tooth_settings};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.instance = this;
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        Tooth tooth = new Tooth(this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        mTitle = pagini[number-1];
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int sn =  getArguments().getInt(ARG_SECTION_NUMBER);
            final View rootView = inflater.inflate(view[sn-1], container, false);
            if(sn == 1){
                final int[] titles = new int[]{R.id.textView_pacientName, R.id.textView_device, R.id.textView_realTimeData, R.id.textView_pastData};
                final int[] expandableLayoutsID = new int[]{R.id.expandableLayout_pacient, R.id.expandableLayout_device, R.id.expandableLayout_rtData, R.id.expandableLayout_pastData};
                final ExpandableRelativeLayout[] expandableLayouts = new ExpandableRelativeLayout[expandableLayoutsID.length];
                for(int i = 0; i < titles.length; i++) {
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
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
