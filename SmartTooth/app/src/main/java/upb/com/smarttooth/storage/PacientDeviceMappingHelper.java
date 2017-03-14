package upb.com.smarttooth.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PacientDeviceMappingHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "SmartTooth.db";
    public static final int DATABASE_VERSION = 1;
    public static final String tableName = "patientDeviceMapping";

    public PacientDeviceMappingHelper(Context context)
    {
        super(context, DATABASE_NAME , null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + tableName + " " +
            "(id integer primary key autoincrement not null, mac text, name text);"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO?
    }
}
