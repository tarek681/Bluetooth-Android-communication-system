package fh.kiel.interlockapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBase extends SQLiteOpenHelper {

    public DataBase(Context context) {
        super(context,
                "Device_Data.db",
                null,
                1);
    }

    @Override
    public void onCreate(SQLiteDatabase DB) {
        DB.execSQL("create Table Userdetails(name TEXT primary key, contact TEXT, dob TEXT)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase DB, int i, int ii) {
        DB.execSQL("drop Table if exists Userdetails");
    }
    public Boolean Insert_Data(String name, String Dev_Name, String Dev_Adress)
    {
        SQLiteDatabase DB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("contact", Dev_Name);
        contentValues.put("dob", Dev_Adress);
        long result=DB.insert("Userdetails", null, contentValues);
        if(result==-1){
            return false;
        }else{
            return true;
        }
    }

    public Boolean Delete_Data(String name)
    {
        SQLiteDatabase DB = this.getWritableDatabase();
        Cursor cursor = DB.rawQuery("Select * from Userdetails where name = ?", new String[]{name});
        if (cursor.getCount() > 0) {
            long result = DB.delete("Userdetails", "name=?", new String[]{name});
            if (result == -1) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public Cursor Get_Data(String name)
    {
        SQLiteDatabase DB = this.getWritableDatabase();
        Cursor cursor = DB.rawQuery("Select * from Userdetails where name = ?", new String[]{name});
        return cursor;
    }
}