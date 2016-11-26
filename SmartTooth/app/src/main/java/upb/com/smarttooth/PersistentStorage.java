package upb.com.smarttooth;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class PersistentStorage {
    private static PersistentStorage instance;
    SQLiteDatabase pacientDeviceDB;
    private PersistentStorage(Context context){
        PacientDeviceMappingHelper pdmh = new PacientDeviceMappingHelper(context);
        pacientDeviceDB = pdmh.getWritableDatabase();
    }

    public String getPacient(String mac) throws android.database.CursorIndexOutOfBoundsException{
        Cursor resultSet = pacientDeviceDB.rawQuery("Select name from " +
                PacientDeviceMappingHelper.tableName + " where mac == \"" + mac + "\"", null);
        resultSet.moveToFirst();
        return resultSet.getString(0);
    }

    public void createPacient(String name, String mac){
        pacientDeviceDB.execSQL("insert into " + PacientDeviceMappingHelper.tableName +
                " (name, mac) values('"+name+"', '"+mac+"')");
    }

    public static PersistentStorage getInstance(Context context){
        if(instance == null && context != null){
            instance = new PersistentStorage(context);
        }
        return instance;
    }
}
