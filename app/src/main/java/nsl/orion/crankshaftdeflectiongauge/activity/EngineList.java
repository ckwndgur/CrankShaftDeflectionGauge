package nsl.orion.crankshaftdeflectiongauge.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import nsl.orion.crankshaftdeflectiongauge.CrankShaftDeflection;
import nsl.orion.crankshaftdeflectiongauge.R;
import nsl.orion.crankshaftdeflectiongauge.bluetooth.BluetoothConnector;
import nsl.orion.crankshaftdeflectiongauge.common.Cylinder;
import nsl.orion.crankshaftdeflectiongauge.common.Engine;
import nsl.orion.crankshaftdeflectiongauge.common.EngineArrayAdapter;


public class EngineList extends Activity {

    private static BluetoothBatteryHandler bluetoothBatteryHandler;
    private EditText engineNameEditText, numOfCylinderEditText;
    private ListView engineListView;
    private Integer numberOfCylinder;
    private Map<Long, Engine> engineMap;
    private ArrayAdapter<Engine> engineAdapter;
    private TextView connectedDeviceName, batteryPercent;
    private ProgressBar batteryProgressBar;
    private Button addButton, updateButton;
    private Engine selectedEngine;

    private boolean addMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.engine_list_screen);

        engineListView = (ListView) findViewById(R.id.engineListView);
        engineNameEditText = (EditText) findViewById(R.id.etEngineName);
        numOfCylinderEditText = (EditText) findViewById(R.id.etNumOfCylinder);
        connectedDeviceName = (TextView) findViewById(R.id.device_name_text_view);
        batteryPercent = (TextView) findViewById(R.id.percent_battery);
        batteryProgressBar = (ProgressBar) findViewById(R.id.progressbar_battery);
        batteryProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.layerlist));
        addButton = (Button) findViewById(R.id.add_btn);
        updateButton = (Button) findViewById(R.id.update_btn);
        addMode = true;

        if (BluetoothConnector.getTargetDevice() != null)
            connectedDeviceName.setText(BluetoothConnector.getTargetDevice().getName());

        engineNameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        numOfCylinderEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        engineMap = CrankShaftDeflection.getEngineMap();
        engineAdapter = new EngineArrayAdapter(this, new ArrayList(engineMap.values()));
        engineListView.setAdapter(engineAdapter);
        engineListView.setSelection(engineAdapter.getCount() - 1);

        engineListView.setOnItemClickListener(new EngineOnclickListener());
        numOfCylinderEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (addMode) addEngine(null);
                    else updateEngine(null);
                }
                return false;
            }
        });

        engineNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    engineNameEditText.requestFocus();
                }
                return false;
            }
        });

        if (bluetoothBatteryHandler == null)
            bluetoothBatteryHandler = new BluetoothBatteryHandler();

        BluetoothConnector.setBluetoothBatteryHandler(bluetoothBatteryHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        engineAdapter.clear();
        if (bluetoothBatteryHandler == null)
            bluetoothBatteryHandler = new BluetoothBatteryHandler();
        BluetoothConnector.setBluetoothBatteryHandler(bluetoothBatteryHandler);
        engineAdapter.addAll(engineMap.values());
        engineAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothBatteryHandler = null;
        BluetoothConnector.setBluetoothDataHandler(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_engine_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void addEngine(View v) {
        if (engineNameEditText.getText() != null &&
                isNumeric(numOfCylinderEditText.getText().toString())) {
            Time time = new Time(Calendar.getInstance().getTimeInMillis());
            SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String dateString = date.format(time);
            String name = engineNameEditText.getText().toString();

            Engine tempEngine = new Engine();
            tempEngine.setName(name);
            tempEngine.setNumberOfCylinder(numberOfCylinder);
            tempEngine.setDateOfCreation(dateString);
            tempEngine.setLastUpdate(dateString);
            tempEngine.setType("default");
            tempEngine.setIsFinished(Engine.NOT_FINISHED);

            tempEngine.setId(CrankShaftDeflection.getSqliteConnector().insertEngine(tempEngine));
            ArrayList<Cylinder> tempCylinderList = new ArrayList();
            for (int i = 0; i < numberOfCylinder; i++) {
                Cylinder tempCylinder = new Cylinder();
                tempCylinder.setEngineId(tempEngine.getId());
                tempCylinder.setOrder(i + 1);
                tempCylinder.setT(100);
                tempCylinder.setE(100);
                tempCylinder.setBe(100);
                tempCylinder.setBp(100);
                tempCylinder.setP(100);
                tempCylinder.setDateOfCreation(dateString);
                tempCylinder.setLastUpdate(dateString);
                tempCylinder.setIsFinished(Cylinder.NOT_FINISHED);

                tempCylinderList.add(tempCylinder);
            }
            CrankShaftDeflection.getSqliteConnector().insertEmptyCylinders(tempCylinderList);
            tempEngine.cylinderArrayList = tempCylinderList;
            engineMap.put(tempEngine.getId(), tempEngine);
            engineAdapter.add(tempEngine);
            engineAdapter.notifyDataSetChanged();
            engineNameEditText.setText(null);
            numOfCylinderEditText.setText(null);
            engineListView.setSelection(engineAdapter.getCount() - 1);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(0, 0);
        }
    }

    public void updateEngine(View v) {
        if (engineNameEditText.getText() != null &&
                isNumeric(numOfCylinderEditText.getText().toString())) {
            Time time = new Time(Calendar.getInstance().getTimeInMillis());
            SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String dateString = date.format(time);
            String name = engineNameEditText.getText().toString();
            Integer prevNumberOfCylinder = selectedEngine.getNumberOfCylinder();
            selectedEngine.setName(name);
            selectedEngine.setNumberOfCylinder(numberOfCylinder);

            if (numberOfCylinder > prevNumberOfCylinder) {
                for (int i = prevNumberOfCylinder; i < numberOfCylinder; i++) {
                    Cylinder tempCylinder = new Cylinder();
                    tempCylinder.setEngineId(selectedEngine.getId());
                    tempCylinder.setOrder(i + 1);
                    tempCylinder.setT(100);
                    tempCylinder.setE(100);
                    tempCylinder.setBe(100);
                    tempCylinder.setBp(100);
                    tempCylinder.setP(100);
                    tempCylinder.setDateOfCreation(dateString);
                    tempCylinder.setLastUpdate(dateString);
                    tempCylinder.setIsFinished(Cylinder.NOT_FINISHED);
                    selectedEngine.cylinderArrayList.add(tempCylinder);
                    CrankShaftDeflection.getSqliteConnector().addCylinder(tempCylinder);
                }

                selectedEngine.setIsFinished(Engine.NOT_FINISHED);

            } else if (numberOfCylinder < prevNumberOfCylinder) {
                ArrayList<Cylinder> temList = new ArrayList();
                for (Cylinder cylinder : selectedEngine.cylinderArrayList) {
                    if (cylinder.getOrder() > numberOfCylinder)
                        temList.add(cylinder);
                }
                for (Cylinder cylinder : temList) {
                    CrankShaftDeflection.getSqliteConnector().deleteCylinder(cylinder.getEngineId(), cylinder.getOrder());
                    selectedEngine.cylinderArrayList.remove(cylinder);
                }
                boolean finished = true;
                for (Cylinder cylinder : selectedEngine.cylinderArrayList) {
                    if (cylinder.getIsFinished() != Cylinder.FINISHED) finished = false;
                }
                if (finished) selectedEngine.setIsFinished(Engine.FINISHED);
            }

            CrankShaftDeflection.getSqliteConnector().updateEngine(selectedEngine, dateString);

            addButton.setVisibility(View.VISIBLE);
            updateButton.setVisibility(View.GONE);
            addMode = true;

            engineMap.put(selectedEngine.getId(), selectedEngine);
            engineAdapter.remove(selectedEngine);
            engineAdapter.add(selectedEngine);
            engineAdapter.notifyDataSetChanged();
            engineNameEditText.setText(null);
            numOfCylinderEditText.setText(null);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(0, 0);
        }
    }

    private boolean isNumeric(String str) {
        try {
            int number = Integer.parseInt(str);
            if (number <= 0) return false;
            numberOfCylinder = number;
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private class EngineOnclickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(final AdapterView<?> parent, final View view, final int position, long id) {

            PopupMenu popup = new PopupMenu(EngineList.this, view);
            popup.getMenuInflater().inflate(R.menu.engine_popup_menu, popup.getMenu());
            final Engine currentEngine = engineAdapter.getItem(position);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.measureDeflection:
                            Bundle bundle = new Bundle();
                            bundle.putLong("engineId", currentEngine.getId());
                            Intent intent = new Intent(getApplicationContext(), CylinderTest.class);
                            intent.putExtras(bundle);
                            startActivity(intent);
                            break;
                        case R.id.modifyEngine:
                            addMode = false;
                            selectedEngine = currentEngine;
                            engineNameEditText.setText(selectedEngine.getName());
                            numOfCylinderEditText.setText(selectedEngine.getNumberOfCylinder().toString());
                            addButton.setVisibility(View.GONE);
                            updateButton.setVisibility(View.VISIBLE);
                            break;
                        case R.id.deleteEngine:
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EngineList.this, R.style.DialogTheme);
                            dialogBuilder.setTitle(getString(R.string.app_name));
                            dialogBuilder.setMessage("Are your sure you want to delete this engine?");
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
                                            engineAdapter.remove(currentEngine);
                                            CrankShaftDeflection.getEngineMap().remove(currentEngine.getId());
                                            CrankShaftDeflection.getSqliteConnector().deleteEngine(currentEngine.getId());
                                            CrankShaftDeflection.getSqliteConnector().deleteCylinders(currentEngine.getId());
                                            engineAdapter.notifyDataSetChanged();
                                            dialog.cancel();
                                        }
                                    });
                            AlertDialog alertDialog = dialogBuilder.create();
                            alertDialog.show();
                            break;
                        case R.id.engineDetail:
                            AlertDialog.Builder dialogMessageBuilder = new AlertDialog.Builder(EngineList.this, R.style.DialogTheme);
                            String engineDetail = "Engine name: " + currentEngine.getName() + "\n" +
                                    "Number of Cylinder: " + currentEngine.getNumberOfCylinder() + "\n" +
                                    "Engine type: " + currentEngine.getType() + "\n" +
                                    "Date of Creation: " + currentEngine.getDateOfCreation() + "\n" +
                                    "Last update: " + currentEngine.getLastUpdate();
                            dialogMessageBuilder.setTitle(getString(R.string.app_name));
                            dialogMessageBuilder.setMessage(engineDetail);
                            dialogMessageBuilder.setCancelable(true);
                            dialogMessageBuilder.setPositiveButton("Cancel",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            dialogMessageBuilder.create().show();
                            break;
                    }
                    return true;
                }
            });

            popup.show();
        }
    }

    private class BluetoothBatteryHandler extends android.os.Handler {

        @Override
        public void handleMessage(Message message) {
            Integer battery = (Integer) message.obj;
            Float floatBatteryPercent;
            battery = battery - 200;
            if (battery < 0) battery = 0;
            floatBatteryPercent = (float) battery / 55 * 100;
            batteryPercent.setText(String.valueOf(floatBatteryPercent.intValue()) + "%");
            batteryProgressBar.setProgress(floatBatteryPercent.intValue());

        }
    }
}
