package upb.com.smarttooth;

import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataFrame extends ChartHelper{

    public enum DataType{
        Humidity,
        PH
    };

    public XYMultipleSeriesDataset dataset;
    public final XYMultipleSeriesRenderer renderer;
    private final String[] titles;
    public GraphicalView graph;
    private int pos[] = new int[]{0,0};
    Date[] valuesDate;
    boolean rt;
    List<double[]> xIndex = new ArrayList<double[]>(5);
    List<double[]> valuesList = new ArrayList<double[]>(5);
    public DataFrame(String[] titles, GraphicalView g, boolean rt){
        this.rt = rt;
        PointStyle[] styles;
        double[] values;
        int[] colors;
        this.titles = titles;
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
        xIndex.add(index);
        valuesList.add(values);
        valuesList.add(values);
        colors = new int[]{Color.BLUE, Color.GREEN};
        styles = new PointStyle[]{PointStyle.CIRCLE, PointStyle.TRIANGLE};
        this.renderer = buildRenderer(colors, styles);
        renderer.setXLabelFormat(new XNumberFormat(this));
        setChartSettings(renderer, title, xlegend, ylegend, minX, maxX, minY, maxY, Color.BLACK, Color.BLACK);
        renderer.setXLabelsAngle(Config.XLabelsAngle);
        renderer.setPanEnabled(false);
        renderer.setMargins(Config.margins);
        renderer.setZoomEnabled(false, false);
        renderer.setShowLegend(true);
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

    public void update(int value, DataType dt) {
        try {
            if (Config.USING_TEST_DEVICE && dt == DataType.Humidity) {
                //TODO remove this
                value = value - 10;
            }
            int posIndex = 0;
            if (dt == DataType.Humidity) {
                posIndex = 1;
            }
            Log.e("Value", value + "");
            if (value - 10 < renderer.getYAxisMin())
                renderer.setYAxisMin(value - 10);
            if (value + 10 > renderer.getYAxisMax())
                renderer.setYAxisMax(value + 10);
            XYSeries series = this.dataset.getSeriesAt(posIndex);
            XYSeries new_series = new XYSeries(titles[posIndex], 0);
            this.dataset.removeSeries(posIndex);
            if (pos[posIndex] >= Config.GRAPH_WIDTH) {
                for (int i = 0; i < Config.GRAPH_WIDTH - 1; i++) {
                    new_series.add(i, series.getY(i + 1));
                    valuesDate[i] = valuesDate[i + 1];
                }
                new_series.add(Config.GRAPH_WIDTH - 1, value);
                valuesDate[Config.GRAPH_WIDTH - 1] = new Date();
            } else {
                for (int i = 0; i < Config.GRAPH_WIDTH; i++) {
                    if (i != pos[posIndex]) {
                        new_series.add(i, series.getY(i));
                    } else {
                        new_series.add(i, value);
                    }
                }
                valuesDate[pos[posIndex]] = new Date();
                pos[posIndex]++;
            }
            this.dataset.addSeries(posIndex, new_series);
            Log.e("pos", "" + pos[posIndex]);
            Log.e("pos", Config.dateformatOut.format(valuesDate[pos[posIndex]]));
            graph.zoomIn();
            MainActivity.instance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    graph.repaint();
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
