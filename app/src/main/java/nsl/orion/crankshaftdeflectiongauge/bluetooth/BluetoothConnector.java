package nsl.orion.crankshaftdeflectiongauge.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by TienNT on 7/6/2015.+
 */

public class BluetoothConnector {
    // Constants that indicate the current connection connectionState
    public static final int CONNECTION_STATE_NONE = 0;       // we're doing nothing
    public static final int CONNECTION_STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int CONNECTION_STATE_CONNECTED = 2;  // now connected to a remote device
    public static final int DATA_MESSAGE = 40;
    public static final int CONNECTION_STATE_CHANGE = 10;
    public static final int BT_STATE_CHANGE = 20;
    public static final int BATTERY_INFO = 30;
    private static final String TAG = "BluetoothConnector";
    private static final boolean D = false;
    private static BluetoothAdapter btAdapter;
    private static BluetoothDevice targetDevice;
    private static Handler bluetoothDataHandler;
    private static Handler bluetoothBatteryHandler;
    private static Handler bluetoothSettingHandler;
    private static Integer standardValue;
    private static int connectionState;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    public BluetoothConnector() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothDevice getTargetDevice() {
        return BluetoothConnector.targetDevice;
    }

    public static void setTargetDevice(BluetoothDevice targetDevice) {
        BluetoothConnector.targetDevice = targetDevice;
    }

    public static void setBluetoothDataHandler(Handler bluetoothDataHandler) {
        BluetoothConnector.bluetoothDataHandler = bluetoothDataHandler;
    }

    public static void setBluetoothBatteryHandler(Handler bluetoothBatteryHandler) {
        BluetoothConnector.bluetoothBatteryHandler = bluetoothBatteryHandler;
    }

    public static void setBluetoothSettingHandler(Handler bluetoothSettingHandler) {
        BluetoothConnector.bluetoothSettingHandler = bluetoothSettingHandler;
    }

    public static Integer getStandardValue() {
        return standardValue;
    }

    public static void setStandardValue(Integer standardValue) {
        BluetoothConnector.standardValue = standardValue;
    }

    public static int getConnectionState() {
        return connectionState;
    }

    private void setConnectionState(int connectionState) {
        BluetoothConnector.connectionState = connectionState;
        connectionStateChange();
    }

    public static void turnOnBT() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (btAdapter != null)
                    btAdapter.enable();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                bluetoothSettingHandler.obtainMessage(BT_STATE_CHANGE, 8, -1, null).sendToTarget();

                Thread t = Thread.currentThread();
                t = null;
            }
        };

        Thread temp = new Thread(r);
        temp.start();
    }

    public static void turnOffBT() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (btAdapter != null)
                    btAdapter.disable();
                BluetoothConnector.setTargetDevice(null);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                bluetoothSettingHandler.obtainMessage(BT_STATE_CHANGE, 8, -1, null).sendToTarget();

                Thread t = Thread.currentThread();
                t = null;
            }
        };

        Thread temp = new Thread(r);
        temp.start();
    }

    private void connectionStateChange() {
        if (bluetoothSettingHandler != null)
            bluetoothSettingHandler.obtainMessage(CONNECTION_STATE_CHANGE, 8, -1, null).sendToTarget();
    }

    public synchronized void connect() {
        if (D) Log.d(TAG, "connect to: " + targetDevice);

        if (connectionState == CONNECTION_STATE_CONNECTING) {
            if (connectThread != null) {
                if (D) Log.d(TAG, "cancel connectThread");
                connectThread.cancel();
                connectThread = null;
            }
        }

        if (connectedThread != null) {
            if (D) Log.d(TAG, "cancel connectedThread");
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(targetDevice);
        connectThread.start();
        setConnectionState(CONNECTION_STATE_CONNECTING);
    }

    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (connectThread != null) {
            if (D) Log.d(TAG, "cancel connectThread");
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            if (D) Log.d(TAG, "cancel connectedThread");
            connectedThread.cancel();
            connectedThread = null;
        }

        setConnectionState(CONNECTION_STATE_NONE);
    }


    public synchronized void connected(BluetoothSocket bluetoothSocket) {
        if (D) Log.d(TAG, "connected");

        if (connectThread != null) {
            if (D) Log.d(TAG, "cancel connectThread");
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            if (D) Log.d(TAG, "cancel connectedThread");
            connectedThread.cancel();
            connectedThread = null;
        }

        setConnectionState(CONNECTION_STATE_CONNECTED);

        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
    }

    private void connectionFailed() {
        if (D) Log.d(TAG, "connectionFailed");
        setConnectionState(CONNECTION_STATE_NONE);
    }

    private void connectionLost() {
        setConnectionState(CONNECTION_STATE_NONE);
        targetDevice = null;
    }

    private class ConnectThread extends Thread {
        private static final String TAG = "ConnectThread";
        private static final boolean D = false;

        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice device) {
            if (D) Log.d(TAG, "create ConnectThread");
            bluetoothDevice = device;
            bluetoothSocket = BluetoothUtils.createRfcommSocket(bluetoothDevice);
        }

        public void run() {
            if (D) Log.d(TAG, "ConnectThread run");
            btAdapter.cancelDiscovery();
            if (bluetoothSocket == null) {
                if (D) Log.d(TAG, "unable to connect to device, socket isn't created");
                connectionFailed();
                return;
            }

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                bluetoothSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    bluetoothSocket.close();
                } catch (IOException e2) {
                    if (D) Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnector.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(bluetoothSocket);
        }

        public void cancel() {
            if (D) Log.d(TAG, "ConnectThread cancel");

            if (bluetoothSocket == null) {
                if (D) Log.d(TAG, "unable to close null socket");
                return;
            }
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private static final String TAG = "ConnectedThread";
        private static final boolean D = false;

        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private boolean shouldConnect;

        public ConnectedThread(BluetoothSocket socket) {
            if (D) Log.d(TAG, "create ConnectedThread");

            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                if (D) Log.e(TAG, "temp sockets not created", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
            shouldConnect = true;
        }

        public void run() {
            if (D) Log.i(TAG, "ConnectedThread run");
            byte[] buffer = new byte[32];
            int bytes;
            Integer[] data = new Integer[5];
            Integer measuredValue = 0;
            int offset = 0;
            try {
                outputStream.write(85);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Log.e(TAG, "sleep error", ie);
                }

                for (int i = 0; i < 5; i++) {
                    bytes = inputStream.read(buffer, offset, 32 - offset);
                    if (0 < bytes) offset = offset + bytes;
                    if (offset >= 5) break;
                }

                for (int i = 0; i < 5; i++) data[i] = buffer[i] & 0xFF;

                measuredValue = 256 * data[1] + data[0];
                if (data[3] == 0) data[3] = 255;
                BluetoothConnector.standardValue = measuredValue;

            } catch (IOException e) {
                if (D) Log.e(TAG, "disconnected", e);
                connectionLost();
            }

            while (shouldConnect) {
                try {
                    outputStream.write(85);
                    if (bluetoothDataHandler != null)
                        bluetoothDataHandler.obtainMessage(BluetoothConnector.DATA_MESSAGE, 8, -1, measuredValue).sendToTarget();
                    if (bluetoothDataHandler != null)
                        bluetoothDataHandler.obtainMessage(BluetoothConnector.BATTERY_INFO, 8, -1, data[3]).sendToTarget();
                    if (bluetoothBatteryHandler != null)
                        bluetoothBatteryHandler.obtainMessage(BluetoothConnector.BATTERY_INFO, 8, -1, data[3]).sendToTarget();
                    offset = 0;
                    for (int i = 0; i < 5; i++) {
                        bytes = inputStream.read(buffer, offset, 32 - offset);
                        if (0 < bytes) offset = offset + bytes;
                        if (offset >= 5) break;
                    }
                    for (int i = 0; i < 5; i++)
                        data[i] = buffer[i] & 0xFF;
                    measuredValue = 256 * data[1] + data[0];
                    if (data[3] == 0) data[3] = 255;
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Log.e(TAG, "sleep error", ie);
                    }


                } catch (IOException e) {
                    if (D) Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                shouldConnect = false;
                Thread.sleep(20);
                bluetoothSocket.close();
            } catch (Exception e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
