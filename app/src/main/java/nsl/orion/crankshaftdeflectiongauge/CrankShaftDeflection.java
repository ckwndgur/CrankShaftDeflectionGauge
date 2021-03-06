package nsl.orion.crankshaftdeflectiongauge;

import android.app.Application;

import java.util.Map;
import java.util.TreeMap;

import nsl.orion.crankshaftdeflectiongauge.bluetooth.BluetoothConnector;
import nsl.orion.crankshaftdeflectiongauge.common.Engine;
import nsl.orion.crankshaftdeflectiongauge.sqlite.SQLiteConnector;

/**
 * Created by TienNT on 8/26/2015.
 */
public class CrankShaftDeflection extends Application {
    private static BluetoothConnector bluetoothConnector;
    private static SQLiteConnector sqliteConnector;
    private static Map<Long, Engine> engineMap;


    public static SQLiteConnector getSqliteConnector() {
        return sqliteConnector;
    }

    public static Map<Long, Engine> getEngineMap() {
        return engineMap;
    }

    public static BluetoothConnector getBluetoothConnector() {
        return bluetoothConnector;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (engineMap == null) CrankShaftDeflection.engineMap = new TreeMap<>();
        if (sqliteConnector == null)
            CrankShaftDeflection.sqliteConnector = new SQLiteConnector(getBaseContext());
        sqliteConnector.loadAll(engineMap);

        bluetoothConnector = new BluetoothConnector();

    }
}
