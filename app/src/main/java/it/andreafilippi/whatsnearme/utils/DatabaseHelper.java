package it.andreafilippi.whatsnearme.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import it.andreafilippi.whatsnearme.entities.MyPlace;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "whatsnearme.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "luoghi_visitati";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NOME = "nome";
    public static final String COLUMN_LATITUDINE = "latitudine";
    public static final String COLUMN_LONGITUDINE = "longitudine";
    public static final String COLUMN_FOTO_PATH = "foto_path";
    public static final String COLUMN_CREATE_TS = "create_ts";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " TEXT PRIMARY KEY,"
                + COLUMN_NOME + " TEXT,"
                + COLUMN_LATITUDINE + " REAL,"
                + COLUMN_LONGITUDINE + " REAL,"
                + COLUMN_FOTO_PATH + " TEXT,"
                + COLUMN_CREATE_TS + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long addLuogo(MyPlace luogo, String fotoPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, luogo.getId());
        values.put(COLUMN_NOME, luogo.getName());
        values.put(COLUMN_LATITUDINE, luogo.getLat().toString());
        values.put(COLUMN_LONGITUDINE, luogo.getLng().toString());
        values.put(COLUMN_FOTO_PATH, fotoPath);

        return db.insert(TABLE_NAME, null, values);
    }

    public Cursor getAllLuoghi() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_CREATE_TS + " DESC",
                null);
    }

    public boolean doesLuogoExist(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ? LIMIT 1", new String[]{id});

        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();

        return exists;
    }

    public Cursor getLuogoById(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ? LIMIT 1", new String[]{id});
    }

    public boolean removeLuogo(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        int rowsAffected = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{id});
        db.close();
        return rowsAffected > 0;
    }
}
