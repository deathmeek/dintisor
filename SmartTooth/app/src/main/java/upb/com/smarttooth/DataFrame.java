package upb.com.smarttooth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.ImageView;

import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import adrian.upb.smarttooth.R;

public class DataFrame extends ChartHelper{
    protected final XYMultipleSeriesDataset dataset;
    final XYMultipleSeriesRenderer renderer;
    public GraphicalView graph;
    private int pos = 0;
    Date[] valuesDate;
    public DataFrame(String[] titles, GraphicalView g){
        PointStyle[] styles;
        double[] values;
        List<double[]> xIndex = new ArrayList<double[]>(5);
        List<double[]> valuesList = new ArrayList<double[]>(5);
        int[] colors;
        String title;
        String xlegend = "";
        String ylegend = "";
        float minX = 0f;
        float maxX = Config.GRAPH_WIDTH -1;
        float minY = 50;
        float maxY = 110;
        this.graph = g;
        title = titles[0];
        double[] index = new double[Config.GRAPH_WIDTH];
        values = new double[Config.GRAPH_WIDTH];
        valuesDate = new Date[Config.GRAPH_WIDTH];
        for(int i = 0; i < Config.GRAPH_WIDTH; i++){
            index[i] = i;
            valuesDate[i] = new Date();
        }
        xIndex.add(index);
        valuesList.add(values);
        int GraphColor = Color.argb(255, 153, 153, 204);
        colors = new int[]{GraphColor};
        styles = new PointStyle[]{PointStyle.CIRCLE};
        this.renderer = buildRenderer(colors, styles);
        renderer.setXLabelFormat(new XNumberFormat(this));
        setChartSettings(renderer, title, xlegend, ylegend, minX, maxX, minY, maxY, Color.BLACK, Color.BLACK);
        renderer.setXLabelsAngle(Config.XLabelsAngle);
        renderer.setPanEnabled(false);
        renderer.setMargins(Config.margins);
        renderer.setZoomEnabled(false, false);
        renderer.setShowLegend(false);
        renderer.setMarginsColor(Color.WHITE);
        renderer.setAxesColor(Color.BLACK);
        renderer.setYLabelsColor(0, Color.BLACK);
        renderer.setXLabelsColor(Color.BLACK);
        renderer.setGridColor(Color.LTGRAY);
        renderer.setLabelsTextSize(20);
        renderer.setXLabels(20);
        renderer.setYLabels(10);
        renderer.setShowGrid(false);
        renderer.setXLabelsAlign(Paint.Align.RIGHT);
        renderer.setYLabelsAlign(Paint.Align.RIGHT);
        this.dataset = buildDataset(titles, xIndex, valuesList);
    }

    public void update(int value) {
        try {
            if (value - 10 < renderer.getYAxisMin())
                renderer.setYAxisMin(value - 10);
            if (value + 10 > renderer.getYAxisMax())
                renderer.setYAxisMax(value + 10);
            dataset.getSeriesAt(0).remove(pos);
            dataset.getSeriesAt(0).add(pos, pos, value);
            Log.e("pos", "" + pos);
            valuesDate[pos] = new Date();
            Log.e("pos", Config.dateformatOut.format(valuesDate[pos]));
            graph.zoomIn();
            MainActivity.instance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    graph.repaint();
                }
            });
            pos = (pos+1) % Config.GRAPH_WIDTH;
        } catch(Exception e){
            Log.e("CEva sa stricat", e.toString());
        }
    }
}
