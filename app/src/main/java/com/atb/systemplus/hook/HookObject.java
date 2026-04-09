package com.atb.systemplus.hook;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hook_objects")
public class HookObject {

    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "display_name")
    public String displayName;

    @ColumnInfo(name = "ui_xml_content")
    public String uiXmlContent;

    @ColumnInfo(name = "hook_xml_content")
    public String hookXmlContent;

    @ColumnInfo(name = "enabled")
    public boolean enabled;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "ui_source_uri")
    public String uiSourceUri;

    @ColumnInfo(name = "hook_source_uri")
    public String hookSourceUri;
}
