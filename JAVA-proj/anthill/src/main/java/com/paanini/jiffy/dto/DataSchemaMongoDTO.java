package com.paanini.jiffy.dto;

import com.option3.docube.schema.jiffytable.DataSchema;

public class DataSchemaMongoDTO {
    private String _id;
    private DataSchema dataSchema;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public DataSchema getDataSchema() {
        return dataSchema;
    }

    public void setDataSchema(DataSchema dataSchema) {
        this.dataSchema = dataSchema;
    }
}
