package nsl.orion.crankshaftdeflectiongauge.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.List;
import java.util.Map;

import nsl.orion.crankshaftdeflectiongauge.common.Cylinder;
import nsl.orion.crankshaftdeflectiongauge.common.Engine;

/**
 * Created by TienNT on 9/7/2015.
 */
public class SQLiteConnector {
    public static final String TAG = "SQLiteConnector";
    public static final long SUCCESS = 0;
    public static final long FAIL = -1;
    private static final String DB_NAME = "cdg.db";
    private static final int DB_VERSION = 1;
    private static SQLiteDatabase database;
    private static CDGSQLiteOpenHelper openHelper;

    public SQLiteConnector(Context context) {
        if (openHelper == null)
            openHelper = new CDGSQLiteOpenHelper(context, DB_NAME, null, DB_VERSION);
    }

    public Long loadAll(Map<Long, Engine> engineMap) {

        try {
            database = openHelper.getReadableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "loadAll() cannot open database");
            return FAIL;
        }

        Cursor cursor = database.rawQuery(EngineTable.QUERY_SELECT_ALL, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Engine tempEngine = new Engine();
                tempEngine.setId(cursor.getLong(EngineTable.INDEX_ENGINE_ID));
                tempEngine.setName(cursor.getString(EngineTable.INDEX_ENGINE_NAME));
                tempEngine.setType(cursor.getString(EngineTable.INDEX_ENGINE_TYPE));
                tempEngine.setNumberOfCylinder(cursor.getInt(EngineTable.INDEX_NUMBER_OF_CYLINDER));
                tempEngine.setDateOfCreation(cursor.getString(EngineTable.INDEX_DATE_OF_CREATION));
                tempEngine.setLastUpdate(cursor.getString(EngineTable.INDEX_LAST_UPDATE));
                tempEngine.setIsFinished(cursor.getInt(EngineTable.INDEX_IS_FINISHED));

                engineMap.put(tempEngine.getId(), tempEngine);

            } while (cursor.moveToNext());
            cursor.close();
        }

        cursor = database.rawQuery(CylinderTable.QUERY_SELECT_ALL, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Cylinder tempCylinder = new Cylinder();

                tempCylinder.setEngineId(cursor.getLong(CylinderTable.INDEX_ENGINE_ID));
                tempCylinder.setOrder(cursor.getInt(CylinderTable.INDEX_ORDER));
                tempCylinder.setT(cursor.getFloat(CylinderTable.INDEX_T));
                tempCylinder.setP(cursor.getFloat(CylinderTable.INDEX_P));
                tempCylinder.setBp(cursor.getFloat(CylinderTable.INDEX_BP));
                tempCylinder.setBe(cursor.getFloat(CylinderTable.INDEX_BE));
                tempCylinder.setE(cursor.getFloat(CylinderTable.INDEX_E));
                tempCylinder.setDateOfCreation(cursor.getString(CylinderTable.INDEX_DATE_OF_CREATION));
                tempCylinder.setLastUpdate(cursor.getString(CylinderTable.INDEX_LAST_UPDATE));
                tempCylinder.setIsFinished(cursor.getInt(CylinderTable.INDEX_IS_FINISHED));

                engineMap.get(tempCylinder.getEngineId()).cylinderArrayList.add(tempCylinder);

            } while (cursor.moveToNext());
            cursor.close();
        }

        return SUCCESS;
    }

    public Long insertEngine(Engine engine) {

        try {
            database = openHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "insertEngine() cannot open database");
            return FAIL;
        }

        ContentValues engineEntry = new ContentValues();

        engineEntry.put(EngineTable.NAME_ENGINE_NAME, engine.getName());
        engineEntry.put(EngineTable.NAME_ENGINE_TYPE, engine.getType());
        engineEntry.put(EngineTable.NAME_NUMBER_OF_CYLINDER, engine.getNumberOfCylinder());
        engineEntry.put(EngineTable.NAME_DATE_OF_CREATION, engine.getDateOfCreation());
        engineEntry.put(EngineTable.NAME_LAST_UPDATE, engine.getLastUpdate());
        engineEntry.put(EngineTable.NAME_IS_FINISHED, Engine.NOT_FINISHED);

        return database.insert(EngineTable.TABLE_NAME, EngineTable.NAME_ENGINE_ID, engineEntry);
    }

    public Long insertEmptyCylinders(List<Cylinder> cylinderList) {

        try {
            database = openHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "insertEmptyCylinders() cannot open database");
            return FAIL;
        }

        for (Cylinder cylinder : cylinderList) {
            ContentValues cylinderEntry = new ContentValues();

            cylinderEntry.put(CylinderTable.NAME_ENGINE_ID, cylinder.getEngineId());
            cylinderEntry.put(CylinderTable.NAME_ORDER, cylinder.getOrder());
            cylinderEntry.put(CylinderTable.NAME_DATE_OF_CREATION, cylinder.getDateOfCreation());
            cylinderEntry.put(CylinderTable.NAME_LAST_UPDATE, cylinder.getLastUpdate());
            cylinderEntry.put(CylinderTable.NAME_IS_FINISHED, Cylinder.NOT_FINISHED);

            database.insert(CylinderTable.TABLE_NAME, null, cylinderEntry);
        }

        return SUCCESS;
    }

    public Long addCylinder(Cylinder cylinder) {

        try {
            database = openHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "addCylinder() cannot open database");
            return FAIL;
        }

        ContentValues cylinderEntry = new ContentValues();

        cylinderEntry.put(CylinderTable.NAME_ENGINE_ID, cylinder.getEngineId());
        cylinderEntry.put(CylinderTable.NAME_ORDER, cylinder.getOrder());
        cylinderEntry.put(CylinderTable.NAME_DATE_OF_CREATION, cylinder.getDateOfCreation());
        cylinderEntry.put(CylinderTable.NAME_LAST_UPDATE, cylinder.getLastUpdate());
        cylinderEntry.put(CylinderTable.NAME_IS_FINISHED, Cylinder.NOT_FINISHED);

        return database.insert(CylinderTable.TABLE_NAME, null, cylinderEntry);
    }


    public Long updateCylinder(int updatePosition, Cylinder cylinder, String lastUpdate) {

        try {
            database = openHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "updateCylinder() cannot open database");
            return FAIL;
        }

        String updateColumn = "";
        Float value = Float.valueOf(0);
        switch (updatePosition) {
            case Cylinder.POSITION_T:
                updateColumn = CylinderTable.NAME_T;
                value = cylinder.getT();
                break;
            case Cylinder.POSITION_P:
                updateColumn = CylinderTable.NAME_P;
                value = cylinder.getP();
                break;
            case Cylinder.POSITION_BP:
                updateColumn = CylinderTable.NAME_BP;
                value = cylinder.getBp();
                break;
            case Cylinder.POSITION_BE:
                updateColumn = CylinderTable.NAME_BE;
                value = cylinder.getBe();
                break;
            case Cylinder.POSITION_E:
                updateColumn = CylinderTable.NAME_E;
                value = cylinder.getE();
                break;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(updateColumn, value);
        contentValues.put(CylinderTable.NAME_LAST_UPDATE, lastUpdate);

        String whereClause = CylinderTable.NAME_ENGINE_ID + " = ? " + " AND "
                + CylinderTable.NAME_ORDER + " = ? ";
        String[] whereValue = {cylinder.getEngineId().toString(), cylinder.getOrder().toString()};
        try {
            database.update(CylinderTable.TABLE_NAME, contentValues, whereClause, whereValue);
        } catch (SQLException e) {
            Log.e(TAG, "couldnt exec sql command");
            return FAIL;
        }
        return SUCCESS;
    }

    public Long updateIsFinishedCylinder(Cylinder cylinder) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CylinderTable.NAME_IS_FINISHED, cylinder.getIsFinished());

        String whereClause = CylinderTable.NAME_ENGINE_ID + " = ? " + " AND "
                + CylinderTable.NAME_ORDER + " = ? ";
        String[] whereValue = {cylinder.getEngineId().toString(), cylinder.getOrder().toString()};
        try {
            database.update(CylinderTable.TABLE_NAME, contentValues, whereClause, whereValue);
        } catch (SQLException e) {
            Log.e(TAG, "couldnt exec sql command");
            return FAIL;
        }
        return SUCCESS;
    }


    public Long updateEngine(Engine engine, String lastUpdate) {

        try {
            database = openHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "updateEngine() cannot open database");
            return FAIL;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(EngineTable.NAME_ENGINE_NAME, engine.getName());
        contentValues.put(EngineTable.NAME_NUMBER_OF_CYLINDER, engine.getNumberOfCylinder());
        contentValues.put(EngineTable.NAME_IS_FINISHED, engine.getIsFinished());
        contentValues.put(EngineTable.NAME_LAST_UPDATE, lastUpdate);

        String whereClause = EngineTable.NAME_ENGINE_ID + " = ? ";

        String[] whereValue = {engine.getId().toString()};
        try {
            database.update(EngineTable.TABLE_NAME, contentValues, whereClause, whereValue);
        } catch (SQLException e) {
            return FAIL;
        }
        return SUCCESS;
    }


    public Long deleteCylinder(Long engineId, Integer order) {

        try {
            database = openHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "deleteCylinder() cannot open database");
            return FAIL;
        }

        String whereClause = CylinderTable.NAME_ENGINE_ID + " = ? AND " + CylinderTable.NAME_ORDER + " = ?";
        String[] whereArgs = new String[2];
        whereArgs[0] = engineId.toString();
        whereArgs[1] = order.toString();

        database.delete(CylinderTable.TABLE_NAME, whereClause, whereArgs);

        return SUCCESS;
    }

    public Long deleteCylinders(Long engineId) {

        try {
            database = openHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "deleteCylinder() cannot open database");
            return FAIL;
        }

        String whereClause = CylinderTable.NAME_ENGINE_ID + " = ?";
        String[] whereArgs = new String[1];
        whereArgs[0] = engineId.toString();

        database.delete(CylinderTable.TABLE_NAME, whereClause, whereArgs);

        return SUCCESS;
    }


    public Long deleteEngine(Long id) {

        try {
            database = openHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "deleteEngine() cannot open database");
            return FAIL;
        }

        String whereClause = EngineTable.NAME_ENGINE_ID + " = ? ";
        String[] whereArgs = new String[1];
        whereArgs[0] = id.toString();

        database.delete(EngineTable.TABLE_NAME, whereClause, whereArgs);

        return SUCCESS;
    }
}
