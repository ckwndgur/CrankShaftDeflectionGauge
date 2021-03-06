package nsl.orion.crankshaftdeflectiongauge.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import nsl.orion.crankshaftdeflectiongauge.CrankShaftDeflection;
import nsl.orion.crankshaftdeflectiongauge.R;
import nsl.orion.crankshaftdeflectiongauge.common.Cylinder;
import nsl.orion.crankshaftdeflectiongauge.common.CylinderArrayAdapter;
import nsl.orion.crankshaftdeflectiongauge.common.Engine;
import nsl.orion.crankshaftdeflectiongauge.common.EngineArrayAdapter;


public class Result extends Activity {

    public static ArrayAdapter engineArrayAdapter;
    public static ArrayAdapter cylinderArrayAdapter;
    private static Engine currentEngine;
    private static ListPopupWindow listPopupWindow;
    private static View deflectionChart;
    private static XYMultipleSeriesDataset dataset;
    private ListView cylinderListView;
    private TextView engineNameTextView;
    private XYSeries verDeflection, horDeflection, stddeflection;
    private XYMultipleSeriesRenderer multipleRenderer;
    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_screen);
        cylinderListView = (ListView) findViewById(R.id.cylinderTableListView);
        engineNameTextView = (TextView) findViewById(R.id.nameOfengine);

        engineArrayAdapter = new EngineArrayAdapter(this, new ArrayList());
        for (Engine engine : CrankShaftDeflection.getEngineMap().values()) {
            if (engine.getIsFinished() == Engine.FINISHED)
                engineArrayAdapter.add(engine);
        }

        decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setRoundingMode(RoundingMode.CEILING);

        // Creating an XYseries for Vertical Deflection and Horizontal Deflection
        verDeflection = new XYSeries(getString(R.string.vertical_deflection));
        horDeflection = new XYSeries(getString(R.string.horizontal_deflection));
        stddeflection = new XYSeries(getString(R.string.stand_deflection));

        //Creating a dataset to hold each series
        dataset = new XYMultipleSeriesDataset();

        //Creating XYSeriesRenderer to customize Horizontal Deflection line
        XYSeriesRenderer horrenderer = new XYSeriesRenderer();
        horrenderer.setColor(getResources().getColor(R.color.darkblue));
        horrenderer.setPointStyle(PointStyle.SQUARE);
        horrenderer.setFillPoints(true);
        horrenderer.setLineWidth(3);
        horrenderer.setDisplayChartValues(true);
        horrenderer.setChartValuesFormat(decimalFormat);
        horrenderer.setChartValuesTextSize(30);

        //Creating XYSeriesRenderer to customize Vertical Deflection line
        XYSeriesRenderer verrenderer = new XYSeriesRenderer();
        verrenderer.setColor(getResources().getColor(R.color.red));
        verrenderer.setPointStyle(PointStyle.TRIANGLE);
        verrenderer.setFillPoints(true);
        verrenderer.setLineWidth(3);
        verrenderer.setDisplayChartValues(true);
        verrenderer.setChartValuesFormat(decimalFormat);
        verrenderer.setChartValuesTextSize(30);
        //Creating XYSeries to customize stddeflection line
        XYSeriesRenderer stdrenderer = new XYSeriesRenderer();
        stdrenderer.setColor(getResources().getColor(R.color.black));
        stdrenderer.setFillPoints(true);
        stdrenderer.setLineWidth(1);

        multipleRenderer = new XYMultipleSeriesRenderer();
        multipleRenderer.setXTitle("\n \n \n \n" + getString(R.string.cylinder_number));
        multipleRenderer.setYTitle("\n \n \n \n" + getString(R.string.deflection_data));
        multipleRenderer.setLabelsTextSize(30);
        multipleRenderer.setLabelsColor(getResources().getColor(R.color.cyan));
        multipleRenderer.setAxisTitleTextSize(20);
        multipleRenderer.setChartTitleTextSize(20);
        multipleRenderer.setMarginsColor(getResources().getColor(R.color.white));
        multipleRenderer.setYAxisMax(0.2);
        multipleRenderer.setYAxisMin(-0.2);
        multipleRenderer.setLegendTextSize(25);
        multipleRenderer.setMargins(new int[]{20, 150, 100, 80});
        multipleRenderer.setXLabelsColor(getResources().getColor(R.color.cyan));
        multipleRenderer.setYLabelsColor(0, getResources().getColor(R.color.cyan));
        multipleRenderer.setAxisTitleTextSize(35);
        multipleRenderer.setYLabelsAlign(Paint.Align.RIGHT);
        multipleRenderer.setFitLegend(true);
        multipleRenderer.setXLabels(0);
        multipleRenderer.setYLabelsPadding(20);

        //Adding verrenderer and horrenderer to multipleRenderer
        multipleRenderer.addSeriesRenderer(verrenderer);
        multipleRenderer.addSeriesRenderer(horrenderer);
        multipleRenderer.addSeriesRenderer(stdrenderer);

        cylinderArrayAdapter = new CylinderArrayAdapter(this, new ArrayList());
        cylinderListView.setAdapter(cylinderArrayAdapter);
        if (currentEngine == null && engineArrayAdapter.getCount() != 0) {
            currentEngine = (Engine) engineArrayAdapter.getItem(0);
            openCylinder();
            openChart();
            engineNameTextView.setText(currentEngine.getName() + " - Deflection data");
        }

        listPopupWindow = new ListPopupWindow(this);
        listPopupWindow.setAdapter(engineArrayAdapter);
        listPopupWindow.setAnchorView(findViewById(R.id.list_btn));
        listPopupWindow.setModal(true);
        listPopupWindow.setWidth(500);
        listPopupWindow.setHeight(800);
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentEngine = (Engine) engineArrayAdapter.getItem(position);
                openCylinder();
                openChart();
                engineNameTextView.setText(currentEngine.getName() + " - Deflection data");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (engineArrayAdapter.getCount() != 0) {
            currentEngine = (Engine) engineArrayAdapter.getItem(0);
            openCylinder();
            openChart();
            engineNameTextView.setText(currentEngine.getName() + " - Deflection data");
        }
    }

    public void showEngineListPopup(View view) {
        listPopupWindow.show();
    }

    private void openCylinder() {
        cylinderArrayAdapter.clear();
        cylinderArrayAdapter.addAll(currentEngine.cylinderArrayList);
        cylinderArrayAdapter.notifyDataSetChanged();
    }

    private void openChart() {
        LinearLayout chartContainer = (LinearLayout) findViewById(R.id.chart_container);
        chartContainer.removeView(deflectionChart);

        int numberOfCylinder = currentEngine.cylinderArrayList.size();

        float[] ep = new float[numberOfCylinder];
        float[] tb = new float[numberOfCylinder];
        int[] stdline = new int[numberOfCylinder];

        int i = 0;
        for (Cylinder cylinder : currentEngine.cylinderArrayList) {
            ep[i] = cylinder.getE() - cylinder.getP();
            tb[i] = cylinder.getT() - ((cylinder.getBp() + cylinder.getBe()) / 2);
            i++;
        }

        verDeflection.clear();
        horDeflection.clear();
        stddeflection.clear();
        dataset.clear();
        for (i = 0; i < numberOfCylinder; i++) {
            stdline[i] = 0;
            multipleRenderer.addXTextLabel(i + 1, String.valueOf(i + 1));
            verDeflection.add(i + 1, tb[i]);
            horDeflection.add(i + 1, ep[i]);
            stddeflection.add(i + 1, stdline[i]);
        }
        dataset.addSeries(verDeflection);
        dataset.addSeries(horDeflection);
        dataset.addSeries(stddeflection);

        multipleRenderer.setXLabels(0);

        deflectionChart = ChartFactory.getLineChartView(getBaseContext(), dataset, multipleRenderer);
        chartContainer.addView(deflectionChart);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_result, menu);
        return true;
    }

    public void exportCurrentEngine(View view) {
        if (currentEngine != null) {
            SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            SimpleDateFormat destFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String date = new String();
            try {
                date = destFormat.format(sourceFormat.parse(currentEngine.getDateOfCreation()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String fileName = currentEngine.getName() + date + ".csv";
            final File file = new File(Environment.getExternalStorageDirectory(), fileName);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(Result.this, R.style.DialogTheme);
            String mes = "Do you wanna export current engine to \n" + file.getAbsolutePath().toString() + "?";
            dialogBuilder.setMessage(mes);
            dialogBuilder.setCancelable(true);
            dialogBuilder.setPositiveButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            dialogBuilder.setNegativeButton("Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            StringBuilder content = new StringBuilder();
                            content.append(getString(R.string.export_left_corner)).append(",");
                            content.append(getString(R.string.export_bp)).append(",");
                            content.append(getString(R.string.export_p)).append(",");
                            content.append(getString(R.string.export_t)).append(",");
                            content.append(getString(R.string.export_e)).append(",");
                            content.append(getString(R.string.export_be)).append(",");
                            content.append(getString(R.string.export_vf)).append(",");
                            content.append(getString(R.string.export_hf)).append("\n");

                            for (Cylinder cylinder : currentEngine.cylinderArrayList) {
                                content.append("Cylinder ").append(cylinder.getOrder()).append(",");
                                content.append(decimalFormat.format(cylinder.getBp())).append(",");
                                content.append(decimalFormat.format(cylinder.getP())).append(",");
                                content.append(decimalFormat.format(cylinder.getT())).append(",");
                                content.append(decimalFormat.format(cylinder.getE())).append(",");
                                content.append(decimalFormat.format(cylinder.getBe())).append(",");
                                Float vf = cylinder.getT() - (cylinder.getBp() + cylinder.getBe()) /2;
                                Float hf = cylinder.getE() - cylinder.getP();
                                content.append(decimalFormat.format(vf)).append(",");
                                content.append(decimalFormat.format(hf)).append("\n");

                            }
                            FileOutputStream outputStream;
                            try {
                                outputStream = new FileOutputStream(file);
                                outputStream.write(content.toString().getBytes());
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            dialog.cancel();
                        }
                    }

            );
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        }
    }
}
