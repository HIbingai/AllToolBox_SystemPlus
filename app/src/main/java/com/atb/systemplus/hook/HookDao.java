package com.atb.systemplus.hook;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HookDao {

    @Query("SELECT * FROM hook_objects ORDER BY created_at DESC")
    List<HookObject> getAll();

    @Query("SELECT * FROM hook_objects WHERE id = :id LIMIT 1")
    HookObject findById(String id);

    @Query("SELECT * FROM hook_objects WHERE display_name = :displayName LIMIT 1")
    HookObject findByDisplayName(String displayName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(HookObject hookObject);

    @Delete
    void delete(HookObject hookObject);
}
