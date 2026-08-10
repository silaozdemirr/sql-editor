package com.sqleditor.model;

/**
 * Kolon bilgisi — tablo kolonlarını döner.
 */
public class ColumnInfo {

    private String name;
    private String dataType;
    private String fullType;   // örn: varchar(150)
    private boolean nullable;
    private boolean primaryKey;
    private boolean foreignKey;
    private boolean unique;
    private String defaultValue;
    private String extra;      // AUTO_INCREMENT vb.

    public ColumnInfo() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ColumnInfo obj = new ColumnInfo();
        public Builder name(String v)         { obj.name = v;         return this; }
        public Builder dataType(String v)     { obj.dataType = v;     return this; }
        public Builder fullType(String v)     { obj.fullType = v;     return this; }
        public Builder nullable(boolean v)    { obj.nullable = v;     return this; }
        public Builder primaryKey(boolean v)  { obj.primaryKey = v;   return this; }
        public Builder foreignKey(boolean v)  { obj.foreignKey = v;   return this; }
        public Builder unique(boolean v)      { obj.unique = v;       return this; }
        public Builder defaultValue(String v) { obj.defaultValue = v; return this; }
        public Builder extra(String v)        { obj.extra = v;        return this; }
        public ColumnInfo build() { return obj; }
    }

    public String getName()         { return name; }
    public String getDataType()     { return dataType; }
    public String getFullType()     { return fullType; }
    public boolean isNullable()     { return nullable; }
    public boolean isPrimaryKey()   { return primaryKey; }
    public boolean isForeignKey()   { return foreignKey; }
    public boolean isUnique()       { return unique; }
    public String getDefaultValue() { return defaultValue; }
    public String getExtra()        { return extra; }
}
