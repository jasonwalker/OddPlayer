package com.jmw.rd.oddplay.storage;

import android.content.Context;

public class StorageUtil {

    public static Storage getStorage(Context context) {
        return DBStorage.getDBStorage(context);
    }
}
