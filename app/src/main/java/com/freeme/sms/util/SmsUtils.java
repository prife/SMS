package com.freeme.sms.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import com.freeme.sms.Factory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsUtils {
    private static final String TAG = "SmsUtils";

    private SmsUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    // Sync all remote messages apart from drafts
    private static final String REMOTE_SMS_SELECTION = String.format(
            Locale.US,
            "(%s IN (%d, %d, %d, %d, %d))",
            Sms.TYPE,
            Sms.MESSAGE_TYPE_INBOX,
            Sms.MESSAGE_TYPE_OUTBOX,
            Sms.MESSAGE_TYPE_QUEUED,
            Sms.MESSAGE_TYPE_FAILED,
            Sms.MESSAGE_TYPE_SENT);

    /**
     * Type selection for importing sms messages.
     *
     * @return The SQL selection for importing sms messages
     */
    public static String getSmsTypeSelectionSql() {
        return REMOTE_SMS_SELECTION;
    }

    private static final String[] TEST_DATE_SENT_PROJECTION = new String[]{Sms.DATE_SENT};
    private static Boolean sHasSmsDateSentColumn = null;

    /**
     * Check if date_sent column exists on ICS and above devices. We need to do a test
     * query to figure that out since on some ICS+ devices, somehow the date_sent column does
     * not exist. http://b/17629135 tracks the associated compliance test.
     *
     * @return Whether "date_sent" column exists in sms table
     */
    public static boolean hasSmsDateSentColumn() {
        if (sHasSmsDateSentColumn == null) {
            Cursor cursor = null;
            try {
                final Context context = Factory.get().getApplicationContext();
                cursor = SqliteWrapper.query(
                        context,
                        Sms.CONTENT_URI,
                        TEST_DATE_SENT_PROJECTION,
                        null/*selection*/,
                        null/*selectionArgs*/,
                        Sms.DATE_SENT + " ASC LIMIT 1");
                sHasSmsDateSentColumn = true;
            } catch (final SQLiteException e) {
                Log.w(TAG, "date_sent in sms table does not exist", e);
                sHasSmsDateSentColumn = false;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return sHasSmsDateSentColumn;
    }

    // Parse the message date
    public static Long getMessageDate(final SmsMessage sms, long now) {
        // Use now for the timestamp to avoid confusion with clock
        // drift between the handset and the SMSC.
        // Check to make sure the system is giving us a non-bogus time.
        final Calendar buildDate = new GregorianCalendar(2011, 8, 18);    // 18 Sep 2011
        final Calendar nowDate = new GregorianCalendar();
        nowDate.setTimeInMillis(now);
        if (nowDate.before(buildDate)) {
            // It looks like our system clock isn't set yet because the current time right now
            // is before an arbitrary time we made this build. Instead of inserting a bogus
            // receive time in this case, use the timestamp of when the message was sent.
            now = sms.getTimestampMillis();
        }
        return now;
    }

    /**
     * Parse values from a received sms message
     *
     * @param context
     * @param msgs    The received sms message content
     * @param error   The received sms error
     * @return Parsed values from the message
     */
    public static ContentValues parseReceivedSmsMessage(
            final Context context, final SmsMessage[] msgs, final int error) {
        final SmsMessage sms = msgs[0];
        final ContentValues values = new ContentValues();

        values.put(Sms.ADDRESS, sms.getDisplayOriginatingAddress());
        values.put(Sms.BODY, buildMessageBodyFromPdus(msgs));
        if (hasSmsDateSentColumn()) {
            // TODO:: The boxing here seems unnecessary.
            values.put(Sms.DATE_SENT, Long.valueOf(sms.getTimestampMillis()));
        }
        values.put(Sms.PROTOCOL, sms.getProtocolIdentifier());
        if (sms.getPseudoSubject().length() > 0) {
            values.put(Sms.SUBJECT, sms.getPseudoSubject());
        }
        values.put(Sms.REPLY_PATH_PRESENT, sms.isReplyPathPresent() ? 1 : 0);
        values.put(Sms.SERVICE_CENTER, sms.getServiceCenterAddress());
        // Error code
        values.put(Sms.ERROR_CODE, error);

        return values;
    }

    // Insert an SMS message to telephony
    public static Uri insertSmsMessage(final Context context, final Uri uri, final int subId,
                                       final String dest, final String text, final long timestamp, final int status,
                                       final int type, final long threadId) {
        Uri response = null;
        try {
            response = addMessageToUri(context.getContentResolver(), uri, subId, dest,
                    text, null /* subject */, timestamp, true /* read */,
                    true /* seen */, status, type, threadId);
            Log.d(TAG, "Inserted SMS message into telephony (type = " + type + ")"
                    + ", uri: " + response);
        } catch (final SQLiteException e) {
            Log.e(TAG, "persist sms message failure " + e, e);
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "persist sms message failure " + e, e);
        }
        return response;
    }

    /**
     * Add an SMS to the given URI with thread_id specified.
     *
     * @param resolver the content resolver to use
     * @param uri      the URI to add the message to
     * @param subId    subId for the receiving sim
     * @param address  the address of the sender
     * @param body     the body of the message
     * @param subject  the psuedo-subject of the message
     * @param date     the timestamp for the message
     * @param read     true if the message has been read, false if not
     * @param threadId the thread_id of the message
     * @return the URI for the new message
     */
    private static Uri addMessageToUri(final ContentResolver resolver,
                                       final Uri uri, final int subId, final String address, final String body,
                                       final String subject, final Long date, final boolean read, final boolean seen,
                                       final int status, final int type, final long threadId) {
        final ContentValues values = new ContentValues(7);

        values.put(Telephony.Sms.ADDRESS, address);
        if (date != null) {
            values.put(Telephony.Sms.DATE, date);
        }
        values.put(Telephony.Sms.READ, read ? 1 : 0);
        values.put(Telephony.Sms.SEEN, seen ? 1 : 0);
        values.put(Telephony.Sms.SUBJECT, subject);
        values.put(Telephony.Sms.BODY, body);
        if (OsUtil.isAtLeastL_MR1()) {
            values.put(Telephony.Sms.SUBSCRIPTION_ID, subId);
        }
        if (status != Telephony.Sms.STATUS_NONE) {
            values.put(Telephony.Sms.STATUS, status);
        }
        if (type != Telephony.Sms.MESSAGE_TYPE_ALL) {
            values.put(Telephony.Sms.TYPE, type);
        }
        if (threadId != -1L) {
            values.put(Telephony.Sms.THREAD_ID, threadId);
        }
        return resolver.insert(uri, values);
    }

    // Some providers send formfeeds in their messages. Convert those formfeeds to newlines.
    private static String replaceFormFeeds(final String s) {
        return s == null ? "" : s.replace('\f', '\n');
    }

    // Parse the message body from message PDUs
    private static String buildMessageBodyFromPdus(final SmsMessage[] msgs) {
        if (msgs.length == 1) {
            // There is only one part, so grab the body directly.
            return replaceFormFeeds(msgs[0].getDisplayMessageBody());
        } else {
            // Build up the body from the parts.
            final StringBuilder body = new StringBuilder();
            for (final SmsMessage msg : msgs) {
                try {
                    // getDisplayMessageBody() can NPE if mWrappedMessage inside is null.
                    body.append(msg.getDisplayMessageBody());
                } catch (final NullPointerException e) {
                    // Nothing to do
                }
            }
            return replaceFormFeeds(body.toString());
        }
    }

    /**
     * mailbox         =       name-addr
     * name-addr       =       [display-name] angle-addr
     * angle-addr      =       [CFWS] "<" addr-spec ">" [CFWS]
     */
    public static final Pattern NAME_ADDR_EMAIL_PATTERN =
            Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

    public static String extractAddrSpec(final String address) {
        final Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

        if (match.matches()) {
            return match.group(2);
        }
        return address;
    }

    /**
     * Returns true if the address is an email address
     *
     * @param address the input address to be tested
     * @return true if address is an email address
     */
    public static boolean isEmailAddress(final String address) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }

        final String s = extractAddrSpec(address);
        final Matcher match = Patterns.EMAIL_ADDRESS.matcher(s);
        return match.matches();
    }

    /**
     * Helper functions for the "threads" table used by MMS and SMS.
     */
    public static final class Threads implements android.provider.Telephony.ThreadsColumns {
        private static final String[] ID_PROJECTION = {BaseColumns._ID};
        private static final Uri THREAD_ID_CONTENT_URI = Uri.parse(
                "content://mms-sms/threadID");
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                android.provider.Telephony.MmsSms.CONTENT_URI, "conversations");

        // No one should construct an instance of this class.
        private Threads() {
        }

        /**
         * This is a single-recipient version of
         * getOrCreateThreadId.  It's convenient for use with SMS
         * messages.
         */
        public static long getOrCreateThreadId(final Context context, final String recipient) {
            final Set<String> recipients = new HashSet<String>();

            recipients.add(recipient);
            return getOrCreateThreadId(context, recipients);
        }

        /**
         * Given the recipients list and subject of an unsaved message,
         * return its thread ID.  If the message starts a new thread,
         * allocate a new thread ID.  Otherwise, use the appropriate
         * existing thread ID.
         * <p>
         * Find the thread ID of the same set of recipients (in
         * any order, without any additions). If one
         * is found, return it.  Otherwise, return a unique thread ID.
         */
        public static long getOrCreateThreadId(
                final Context context, final Set<String> recipients) {
            final Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();

            for (String recipient : recipients) {
                if (isEmailAddress(recipient)) {
                    recipient = extractAddrSpec(recipient);
                }

                uriBuilder.appendQueryParameter("recipient", recipient);
            }

            final Uri uri = uriBuilder.build();

            final Cursor cursor = SqliteWrapper.query(context, uri, ID_PROJECTION, null,
                    null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(0);
                    } else {
                        Log.e(TAG, "getOrCreateThreadId returned no rows!");
                    }
                } finally {
                    cursor.close();
                }
            }

            Log.e(TAG, "getOrCreateThreadId failed with " + recipients.toString());
            throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
        }
    }
}
