package com.atb.systemplus.hook;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {HookObject.class}, version = 1, exportSchema = false)
public abstract class HookDatabase extends RoomDatabase {

    private static final String DB_NAME = "hook_loader.db";

    private static volatile HookDatabase instance;

    public abstract HookDao hookDao();

    public static HookDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (HookDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            HookDatabase.class,
                            DB_NAME
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return instance;
    }
}
