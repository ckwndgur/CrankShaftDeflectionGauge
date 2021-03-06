package nsl.orion.crankshaftdeflectiongauge.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.locks.ReentrantLock;

import nsl.orion.crankshaftdeflectiongauge.CrankShaftDeflection;
import nsl.orion.crankshaftdeflectiongauge.R;
import nsl.orion.crankshaftdeflectiongauge.bluetooth.BluetoothConnector;
import nsl.orion.crankshaftdeflectiongauge.common.Cylinder;
import nsl.orion.crankshaftdeflectiongauge.common.Engine;


public class CylinderTest extends Activity {

    private static final double cos_54degree = Math.cos(0.942477796);
    private static final double cos_126degree = Math.cos(2.19911486);
    private static final double cos_198degree = Math.cos(3.45575192);
    private static final double cos_270degree = Math.cos(4.71238898);
    private static final double cos_342degree = Math.cos(5.96902604);
    private static BluetoothDataHandler bluetoothDataHandler;
    private static boolean btConnected;
    private static float currentValue;
    private Engine currentEngine;
    private Cylinder currentCylinder;
    private ReentrantLock lock = new ReentrantLock();
    private TextView measuredText, engineNameTextView, cylinderOrderTextView, displayTTextView, displayPTextView, displayBPTextView, displayBETextView, displayETextView, connectedDeviceName, batteryPercent;
    private ImageButton nextCylinderImageButton, prevCylinderImageButton;
    private ProgressBar batteryProgressBar;
    private Integer receivedValue;
    private int currentOrder;

    public synchronized void setCurrentValue(Float currentValue) {
        lock.lock();
        try {
            CylinderTest.currentValue = currentValue;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cyliner_test_screen);
        currentOrder = 0;

        displayTTextView = (TextView) findViewById(R.id.tvT);
        displayPTextView = (TextView) findViewById(R.id.tvP);
        displayBPTextView = (TextView) findViewById(R.id.tvBP);
        displayBETextView = (TextView) findViewById(R.id.tvBE);
        displayETextView = (TextView) findViewById(R.id.tvE);
        engineNameTextView = (TextView) findViewById(R.id.tvEngineName);
        cylinderOrderTextView = (TextView) findViewById(R.id.tvCylinderNumber);
        connectedDeviceName = (TextView) findViewById(R.id.device_name_text_view);
        batteryPercent = (TextView) findViewById(R.id.percent_battery);
        batteryProgressBar = (ProgressBar) findViewById(R.id.progressbar_battery);
        batteryProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.layerlist));
        nextCylinderImageButton = (ImageButton) findViewById(R.id.arrow_right_btn);
        prevCylinderImageButton = (ImageButton) findViewById(R.id.arrow_left_btn);
        final ImageView gaugeImageView = (ImageView) findViewById(R.id.gauge);
        if (BluetoothConnector.getTargetDevice() != null)
            connectedDeviceName.setText(BluetoothConnector.getTargetDevice().getName());
        prevCylinderImageButton.setVisibility(View.GONE);

        currentEngine = CrankShaftDeflection.getEngineMap().get(getIntent().getExtras().getLong("engineId"));
        currentCylinder = currentEngine.cylinderArrayList.get(currentOrder);

        gaugeImageView.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(CylinderTest.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent motionEvent) {
                    double halfViewSize = gaugeImageView.getWidth() / 2;
                    double coodinate_Bx = motionEvent.getX() - halfViewSize;
                    double coodinate_By = halfViewSize - motionEvent.getY();
                    double segment_OB = Math.sqrt(Math.pow((coodinate_Bx), 2) + Math.pow((coodinate_By), 2));

                    double cos_AOB = coodinate_Bx / segment_OB;

                    if (segment_OB > halfViewSize / 2 && segment_OB < halfViewSize) {
                        if (coodinate_By > 0) {
                            if (cos_AOB > cos_54degree) {
                                updateP();
                            } else if (cos_AOB > cos_126degree) {
                                updateT();
                            } else {
                                updateE();
                            }
                        } else {
                            if (cos_AOB > cos_342degree) {
                                updateP();
                            } else if (cos_AOB > cos_270degree) {
                                updateBP();
                            } else if (cos_AOB > cos_198degree) {
                                updateBE();
                            } else {
                                updateE();
                            }
                        }
                    } else if (segment_OB < halfViewSize / 2) {
                        updateStandardValue();
                    }
                    return super.onDoubleTap(motionEvent);
                }
            });

            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
        if (bluetoothDataHandler == null)
            bluetoothDataHandler = new BluetoothDataHandler();

        BluetoothConnector.setBluetoothDataHandler(bluetoothDataHandler);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            showAlertDialog(getString(R.string.no_bt_support));
        }
        this.measuredText = (TextView) findViewById(R.id.measuredText);
        loadMeasuredValue();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothDataHandler == null)
            bluetoothDataHandler = new BluetoothDataHandler();

        BluetoothConnector.setBluetoothDataHandler(bluetoothDataHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothDataHandler = null;
        BluetoothConnector.setBluetoothDataHandler(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cyliner_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void updateMeasuredValue(String message) {
        measuredText.setText(message);
    }

    private void showAlertDialog(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CylinderTest.this, R.style.DialogTheme);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder.setMessage(message);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void updateT() {
        if (BluetoothConnector.getConnectionState() == BluetoothConnector.CONNECTION_STATE_CONNECTED) {
            lock.lock();
            try {
                displayTTextView.setText(String.valueOf(currentValue));
                currentCylinder.setT(currentValue);
            } finally {
                lock.unlock();
            }
            Time time = new Time(Calendar.getInstance().getTimeInMillis());
            SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String dateString = date.format(time);
            CrankShaftDeflection.getSqliteConnector().updateCylinder(Cylinder.POSITION_T, currentCylinder, dateString);
            currentCylinder.finishedPositions[Cylinder.POSITION_T] = true;

            updateFinished(dateString);

            Log.d("DoubleTap", "updateT() function called");
        } else showAlertDialog("There is a problem with your bluetooth connection.");
    }

    private void updateP() {
        if (BluetoothConnector.getConnectionState() == BluetoothConnector.CONNECTION_STATE_CONNECTED) {
            lock.lock();
            try {
                displayPTextView.setText(String.valueOf(currentValue));
                currentCylinder.setP(currentValue);
            } finally {
                lock.unlock();
            }
            Time time = new Time(Calendar.getInstance().getTimeInMillis());
            SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String dateString = date.format(time);
            CrankShaftDeflection.getSqliteConnector().updateCylinder(Cylinder.POSITION_P, currentCylinder, dateString);
            currentCylinder.finishedPositions[Cylinder.POSITION_P] = true;

            updateFinished(dateString);

            Log.d("DoubleTap", "updateP() function called");
        } else showAlertDialog("There is a problem with your bluetooth connection.");

    }

    private void updateBP() {
        if (BluetoothConnector.getConnectionState() == BluetoothConnector.CONNECTION_STATE_CONNECTED) {
            lock.lock();
            try {
                displayBPTextView.setText(String.valueOf(currentValue));
                currentCylinder.setBp(currentValue);
            } finally {
                lock.unlock();
            }
            Time time = new Time(Calendar.getInstance().getTimeInMillis());
            SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String dateString = date.format(time);
            CrankShaftDeflection.getSqliteConnector().updateCylinder(Cylinder.POSITION_BP, currentCylinder, dateString);
            currentCylinder.finishedPositions[Cylinder.POSITION_BP] = true;

            updateFinished(dateString);

            Log.d("DoubleTap", "updateBP() function called");
        } else showAlertDialog("There is a problem with your bluetooth connection.");

    }

    private void updateBE() {
        if (BluetoothConnector.getConnectionState() == BluetoothConnector.CONNECTION_STATE_CONNECTED) {
            lock.lock();
            try {
                displayBETextView.setText(String.valueOf(currentValue));
                currentCylinder.setBe(currentValue);
            } finally {
                lock.unlock();
            }
            Time time = new Time(Calendar.getInstance().getTimeInMillis());
            SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String dateString = date.format(time);
            CrankShaftDeflection.getSqliteConnector().updateCylinder(Cylinder.POSITION_BE, currentCylinder, dateString);
            currentCylinder.finishedPositions[Cylinder.POSITION_BE] = true;

            updateFinished(dateString);

            Log.d("DoubleTap", "updateBE() function called");
        } else showAlertDialog("There is a problem with your bluetooth connection.");

    }

    private void updateE() {
        if (BluetoothConnector.getConnectionState() == BluetoothConnector.CONNECTION_STATE_CONNECTED) {
            lock.lock();
            try {
                displayETextView.setText(String.valueOf(currentValue));
                currentCylinder.setE(currentValue);
            } finally {
                lock.unlock();
            }
            Time time = new Time(Calendar.getInstance().getTimeInMillis());
            SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String dateString = date.format(time);
            CrankShaftDeflection.getSqliteConnector().updateCylinder(Cylinder.POSITION_E, currentCylinder, dateString);
            currentCylinder.finishedPositions[Cylinder.POSITION_E] = true;

            updateFinished(dateString);

            Log.d("DoubleTap", "updateE() function called");
        } else showAlertDialog("There is a problem with your bluetooth connection.");

    }

    private void updateFinished(String lastUpdate) {
        if (currentCylinder.finishedPositions[Cylinder.POSITION_T] &&
                currentCylinder.finishedPositions[Cylinder.POSITION_P] &&
                currentCylinder.finishedPositions[Cylinder.POSITION_BP] &&
                currentCylinder.finishedPositions[Cylinder.POSITION_BE] &&
                currentCylinder.finishedPositions[Cylinder.POSITION_E]) {
            currentCylinder.setIsFinished(Cylinder.FINISHED);
            CrankShaftDeflection.getSqliteConnector().updateIsFinishedCylinder(currentCylinder);
            boolean enginedFinished = true;
            for (Cylinder cylinder : currentEngine.cylinderArrayList)
                if (cylinder.getIsFinished() == Cylinder.NOT_FINISHED) {
                    enginedFinished = false;
                    break;
                }
            if (enginedFinished) {
                currentEngine.setIsFinished(Engine.FINISHED);
                CrankShaftDeflection.getSqliteConnector().updateEngine(currentEngine, lastUpdate);
            }
        }
    }

    private void loadMeasuredValue() {

        currentCylinder = currentEngine.cylinderArrayList.get(currentOrder);
        engineNameTextView.setText(currentEngine.getName());
        cylinderOrderTextView.setText("Cylinder " + currentCylinder.getOrder().toString());
        if (currentCylinder.getIsFinished() == Cylinder.FINISHED) {
            for (int i = 0; i < currentCylinder.finishedPositions.length; i++)
                currentCylinder.finishedPositions[i] = true;
            displayTTextView.setText(currentCylinder.getT().toString());
            displayPTextView.setText(currentCylinder.getP().toString());
            displayBPTextView.setText(currentCylinder.getBp().toString());
            displayBETextView.setText(currentCylinder.getBe().toString());
            displayETextView.setText(currentCylinder.getE().toString());
        } else {
            displayTTextView.setText(getResources().getString(R.string.T));
            displayPTextView.setText(getResources().getString(R.string.P));
            displayBPTextView.setText(getResources().getString(R.string.BP));
            displayBETextView.setText(getResources().getString(R.string.BE));
            displayETextView.setText(getResources().getString(R.string.E));
        }
    }

    public void nextCylinder(View v) {
        if (currentOrder < currentEngine.cylinderArrayList.size() - 1) {
            currentOrder++;
            loadMeasuredValue();
            prevCylinderImageButton.setVisibility(View.VISIBLE);
            if (currentOrder == currentEngine.cylinderArrayList.size() - 1) {
                nextCylinderImageButton.setVisibility(View.GONE);
            }
        }
    }

    public void prevCylinder(View v) {
        if (currentOrder >= 1) {
            currentOrder--;
            loadMeasuredValue();
            nextCylinderImageButton.setVisibility(View.VISIBLE);
            if (currentOrder == 0) {
                prevCylinderImageButton.setVisibility(View.GONE);
            }
        }
    }

    public void addCylinder(View v) {
        currentOrder++;
        Time time = new Time(Calendar.getInstance().getTimeInMillis());
        SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String dateString = date.format(time);
        currentEngine.setNumberOfCylinder(currentEngine.getNumberOfCylinder() + 1);
        currentCylinder = new Cylinder();
        currentCylinder.setEngineId(currentEngine.getId());
        currentCylinder.setOrder(currentOrder);
        currentCylinder.setT(100);
        currentCylinder.setE(100);
        currentCylinder.setBe(100);
        currentCylinder.setBp(100);
        currentCylinder.setP(100);
        currentCylinder.setDateOfCreation(dateString);
        currentCylinder.setLastUpdate(dateString);
        currentCylinder.setIsFinished(Cylinder.NOT_FINISHED);
        currentEngine.cylinderArrayList.add(currentCylinder);

        CrankShaftDeflection.getSqliteConnector().addCylinder(currentCylinder);
        CrankShaftDeflection.getSqliteConnector().updateEngine(currentEngine, dateString);
        loadMeasuredValue();
    }

    private void updateStandardValue() {
        if (BluetoothConnector.getConnectionState() == BluetoothConnector.CONNECTION_STATE_CONNECTED) {
            BluetoothConnector.setStandardValue(receivedValue);
        } else showAlertDialog("There is a problem with your bluetooth connection.");
    }

    public void setReceivedValue(Integer receivedValue) {
        this.receivedValue = receivedValue;
    }

    private class BluetoothDataHandler extends android.os.Handler {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case BluetoothConnector.DATA_MESSAGE:
                    Integer value = (Integer) message.obj;
                    if (value != null) {
                        setReceivedValue(value);
                        Float fValue = (float) (value.shortValue() - BluetoothConnector.getStandardValue().shortValue()) / 100;
                        setCurrentValue(fValue);
                        updateMeasuredValue(fValue.toString());
                    }
                    break;
                case BluetoothConnector.BATTERY_INFO:
                    Integer battery = (Integer) message.obj;
                    Float floatBatteryPercent;
                    battery = battery - 200;
                    if (battery < 0) battery = 0;
                    floatBatteryPercent = (float) battery / 55 * 100;
                    batteryPercent.setText(String.valueOf(floatBatteryPercent.intValue()) + "%");
                    batteryProgressBar.setProgress(floatBatteryPercent.intValue());
                    break;
            }
        }
    }
}