package com.freeme.sms.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

public class SqliteWrapper {
    private static final String TAG = "SqliteWrapper";

    private SqliteWrapper() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static Cursor query(Context context, Uri uri, String[] projection, String selection,
                               String[] selectionArgs, String sortOrder) {
        final ContentResolver resolver = context.getContentResolver();
        try {
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (SQLiteException e) {
            Log.e(TAG, "catch an exception when query", e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "catch an exception when query", e);
            return null;
        }
    }
}
