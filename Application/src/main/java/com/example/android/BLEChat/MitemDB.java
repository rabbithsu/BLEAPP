package com.example.android.BLEChat;

/**
 * Created by nccu_dct on 15/9/15.
 */
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.android.common.logger.Log;


public class MitemDB {
    public static final String TABLE_NAME = "item";

    // 編號表格欄位名稱，固定不變
    public static final String KEY_ID = "_id";

    // 其它表格欄位名稱
    public static final String DATETIME_COLUMN = "datetime";
    public static final String TYPE_COLUMN = "type";
    //public static final String TITLE_COLUMN = "title";
    public static final String NAME_COLUMN = "name";
    public static final String CONTENT_COLUMN = "content";
    //public static final String FILENAME_COLUMN = "filename";
    //public static final String LATITUDE_COLUMN = "latitude";
    //public static final String LONGITUDE_COLUMN = "longitude";
    //public static final String LASTMODIFY_COLUMN = "lastmodify";

    // 使用上面宣告的變數建立表格的SQL指令
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DATETIME_COLUMN + " REAL NOT NULL, " +
                    TYPE_COLUMN + " INTEGER NOT NULL, " +
                    NAME_COLUMN + " TEXT, " +
                    CONTENT_COLUMN + " TEXT NOT NULL)";

    // 資料庫物件
    private SQLiteDatabase db;

    // 建構子，一般的應用都不需要修改
    public MitemDB (Context context) {
        db = MyDBHelper.getDatabase(context);
    }

    // 關閉資料庫，一般的應用都不需要修改
    public void close() {
        db.close();
    }

    // 新增參數指定的物件
    public CheckMessage insert(CheckMessage item) {
        // 建立準備新增資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的新增資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        cv.put(DATETIME_COLUMN, item.getTime());
        cv.put(TYPE_COLUMN, item.getType());
        cv.put(NAME_COLUMN, item.getName());
        cv.put(CONTENT_COLUMN, item.getContent());


        // 新增一筆資料並取得編號
        // 第一個參數是表格名稱
        // 第二個參數是沒有指定欄位值的預設值
        // 第三個參數是包裝新增資料的ContentValues物件
        long id = db.insert(TABLE_NAME, null, cv);

        // 設定編號
        item.setId(id);
        // 回傳結果
        return item;
    }

    // 修改參數指定的物件
    public boolean update(CheckMessage item) {
        // 建立準備修改資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的修改資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        cv.put(DATETIME_COLUMN, item.getTime());
        cv.put(TYPE_COLUMN, item.getType());
        cv.put(NAME_COLUMN, item.getName());
        cv.put(CONTENT_COLUMN, item.getContent());

        // 設定修改資料的條件為編號
        // 格式為「欄位名稱＝資料」
        String where = KEY_ID + "=" + item.getId();

        // 執行修改資料並回傳修改的資料數量是否成功
        return db.update(TABLE_NAME, cv, where, null) > 0;
    }

    // 刪除參數指定編號的資料
    public boolean delete(long id){
        // 設定條件為編號，格式為「欄位名稱=資料」
        String where = KEY_ID + "=" + id;
        // 刪除指定編號資料並回傳刪除是否成功
        return db.delete(TABLE_NAME, where, null) > 0;
    }

    // 讀取所有記事資料
    public List<CheckMessage> getAll() {
        List<CheckMessage> result = new ArrayList<>();
        //Cursor cursor = db.query(
        //        TABLE_NAME, null, null, null, null, null, null, null);
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME+" ORDER BY datetime ASC", null);

        while (cursor.moveToNext()) {
            result.add(getRecord(cursor));
        }

        cursor.close();
        return result;
    }

    // 取得指定編號的資料物件
    public CheckMessage get(long id) {
        // 準備回傳結果用的物件
        CheckMessage item = null;
        // 使用編號為查詢條件
        String where = KEY_ID + "=" + id;
        // 執行查詢
        Cursor result = db.query(
                TABLE_NAME, null, where, null, null, null, null, null);

        // 如果有查詢結果
        if (result.moveToFirst()) {
            // 讀取包裝一筆資料的物件
            item = getRecord(result);
        }

        // 關閉Cursor物件
        result.close();
        // 回傳結果
        return item;
    }

    // 把Cursor目前的資料包裝為物件
    public CheckMessage getRecord(Cursor cursor) {
        // 準備回傳結果用的物件
        //MessageItem result = new MessageItem();
        CheckMessage result = new CheckMessage(cursor.getLong(0), cursor.getLong(1), cursor.getInt(2), cursor.getString(3), cursor.getString(4));

        /*result.setId(cursor.getLong(0));
        result.setTime(cursor.getLong(1));
        result.setType(cursor.getInt(2));
        result.setContent(cursor.getString(3));*/


        // 回傳結果
        return result;
    }

    // 取得資料數量
    public int getCount() {
        int result = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);

        if (cursor.moveToNext()) {
            result = cursor.getInt(0);
        }

        return result;
    }

    //SYNC max
    public long getMaxTs(){
        long result = 0;
        Cursor cursor = db.rawQuery("SELECT MAX(datetime) FROM " + TABLE_NAME, null);
        if (cursor == null)
                return result;

        if (cursor.moveToNext()) {
            result = cursor.getLong(0);
        }
        cursor.close();
        return result;
    }

    public List<CheckMessage> getHistory(long start, long end){
        List<CheckMessage> result = new ArrayList<>();
        //Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME +
        //        " WHERE datetime > " + String.valueOf(start) + " and datetime <=" + String.valueOf(end), null);
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME +
                " WHERE datetime > " + String.valueOf(start), null);
        while (cursor.moveToNext()) {
            result.add(getRecord(cursor));
        }

        cursor.close();
        return result;
    }

    // 建立範例資料
    public void sample() {
        //MyDBHelper.onUpgrade(MyDBHelper.getDatabase(Context context,));
        CheckMessage item = new CheckMessage(0, System.currentTimeMillis(), 1, "USER_A", "你好");
        CheckMessage item2 = new CheckMessage(0, System.currentTimeMillis(), 2, "USER_B", "嗨");
        CheckMessage item3 = new CheckMessage(0, System.currentTimeMillis(), 1, "USER_A", "哈囉");
        CheckMessage item4 = new CheckMessage(0, System.currentTimeMillis(), 2, "USER_C", "hello");

        insert(item);
        insert(item2);
        insert(item3);
        insert(item4);
    }
    public Boolean Check(Long time, String name) {
        //  long result = 0;
        // Boolean timeRight = true;
        // Boolean nameRight = true;
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE datetime=" + time + " AND " + "name='" + name + "'", null);
        if(cursor.getCount()==0){
            Log.d("DB", "Accept.");
            return false;
        }
        else{
            Log.d("DB", "Reject.");
            return true;
        }
    }

}
