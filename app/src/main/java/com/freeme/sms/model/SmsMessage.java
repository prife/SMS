package com.freeme.sms.model;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Telephony.Sms;
import android.util.Log;

import com.freeme.sms.util.OsUtil;
import com.freeme.sms.util.PhoneUtils;
import com.freeme.sms.util.SmsUtils;

/**
 * SMS message
 */
public class SmsMessage implements Parcelable {
    private static final String TAG = "SmsMessage";

    private static int sIota = 0;
    public static final int INDEX_ID = sIota++;
    public static final int INDEX_TYPE = sIota++;
    public static final int INDEX_ADDRESS = sIota++;
    public static final int INDEX_BODY = sIota++;
    public static final int INDEX_DATE = sIota++;
    public static final int INDEX_THREAD_ID = sIota++;
    public static final int INDEX_STATUS = sIota++;
    public static final int INDEX_READ = sIota++;
    public static final int INDEX_SEEN = sIota++;
    public static final int INDEX_DATE_SENT = sIota++;
    public static final int INDEX_SUB_ID = sIota++;

    private static String[] sProjection;

    public static String[] getProjection() {
        if (sProjection == null) {
            String[] projection = new String[]{
                    Sms._ID,
                    Sms.TYPE,
                    Sms.ADDRESS,
                    Sms.BODY,
                    Sms.DATE,
                    Sms.THREAD_ID,
                    Sms.STATUS,
                    Sms.READ,
                    Sms.SEEN,
                    Sms.DATE_SENT,
                    Sms.SUBSCRIPTION_ID,
            };
            if (!SmsUtils.hasSmsDateSentColumn()) {
                projection[INDEX_DATE_SENT] = Sms.DATE;
            }
            if (!OsUtil.isAtLeastL_MR1()) {
                final int indexSubId = projection.length - 1;
                if (INDEX_SUB_ID != indexSubId) {
                    Log.w(TAG, "Expected " + INDEX_SUB_ID + " but got " + indexSubId);
                }
                String[] withoutSubId = new String[indexSubId];
                System.arraycopy(projection, 0, withoutSubId, 0, withoutSubId.length);
                projection = withoutSubId;
            }

            sProjection = projection;
        }

        return sProjection;
    }

    public String mUri;
    public String mAddress;
    public String mBody;
    private long mRowId;
    public long mTimestampInMillis;
    public long mTimestampSentInMillis;
    public int mType;
    public long mThreadId;
    public int mStatus;
    public boolean mRead;
    public boolean mSeen;
    public int mSubId;

    private SmsMessage() {
    }

    /**
     * Load from a cursor of a query that returns the SMS to import
     *
     * @param cursor
     */
    private void load(final Cursor cursor) {
        mRowId = cursor.getLong(INDEX_ID);
        mAddress = cursor.getString(INDEX_ADDRESS);
        mBody = cursor.getString(INDEX_BODY);
        mTimestampInMillis = cursor.getLong(INDEX_DATE);
        // Before ICS, there is no "date_sent" so use copy of "date" value
        mTimestampSentInMillis = cursor.getLong(INDEX_DATE_SENT);
        mType = cursor.getInt(INDEX_TYPE);
        mThreadId = cursor.getLong(INDEX_THREAD_ID);
        mStatus = cursor.getInt(INDEX_STATUS);
        mRead = cursor.getInt(INDEX_READ) == 0 ? false : true;
        mSeen = cursor.getInt(INDEX_SEEN) == 0 ? false : true;
        mUri = ContentUris.withAppendedId(Sms.CONTENT_URI, mRowId).toString();
        mSubId = PhoneUtils.getDefault().getSubIdFromTelephony(cursor, INDEX_SUB_ID);
    }

    /**
     * Get a new SmsMessage by loading from the cursor of a query
     * that returns the SMS to import
     *
     * @param cursor
     * @return
     */
    public static SmsMessage get(final Cursor cursor) {
        final SmsMessage msg = new SmsMessage();
        msg.load(cursor);
        return msg;
    }

    public static SmsMessage get(final ContentValues messageValues) {
        final SmsMessage msg = new SmsMessage();
        msg.mAddress = (String) messageValues.get(Sms.ADDRESS);
        msg.mSubId = (Integer) messageValues.get(Sms.SUBSCRIPTION_ID);
        msg.mBody = (String) messageValues.get(Sms.BODY);
        msg.mTimestampInMillis = (Long) messageValues.get(Sms.DATE_SENT);

        return msg;
    }

    @Override
    public String toString() {
        return "发件人:" + mAddress
                + "\n收件人:" + PhoneUtils.get(mSubId).getSelfRawNumber(true)
                + "\n内容:" + mBody;
    }

    public static boolean isSame(SmsMessage s1, SmsMessage s2) {
        return s1 != null && s2 != null
                && s1.mSubId == s2.mSubId
                && s1.mTimestampInMillis == s2.mTimestampInMillis
                && s1.mAddress != null && s1.mAddress.equals(s2.mAddress)
                && s1.mBody != null && s1.mBody.equals(s2.mBody);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private SmsMessage(final Parcel in) {
        mUri = in.readString();
        mRowId = in.readLong();
        mTimestampInMillis = in.readLong();
        mTimestampSentInMillis = in.readLong();
        mType = in.readInt();
        mThreadId = in.readLong();
        mStatus = in.readInt();
        mRead = in.readInt() != 0;
        mSeen = in.readInt() != 0;
        mSubId = in.readInt();

        // SMS specific
        mAddress = in.readString();
        mBody = in.readString();
    }

    public static final Parcelable.Creator<SmsMessage> CREATOR
            = new Parcelable.Creator<SmsMessage>() {
        @Override
        public SmsMessage createFromParcel(final Parcel in) {
            return new SmsMessage(in);
        }

        @Override
        public SmsMessage[] newArray(final int size) {
            return new SmsMessage[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        out.writeString(mUri);
        out.writeLong(mRowId);
        out.writeLong(mTimestampInMillis);
        out.writeLong(mTimestampSentInMillis);
        out.writeInt(mType);
        out.writeLong(mThreadId);
        out.writeInt(mStatus);
        out.writeInt(mRead ? 1 : 0);
        out.writeInt(mSeen ? 1 : 0);
        out.writeInt(mSubId);

        // SMS specific
        out.writeString(mAddress);
        out.writeString(mBody);
    }
}
