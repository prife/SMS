package com.freeme.sms.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.freeme.sms.R;

public class ConversationListAdapter
        extends CursorRecyclerAdapter<ConversationListAdapter.ConversationListViewHolder> {
    private final ConversationListItemView.HostInterface mHostInterface;

    public ConversationListAdapter(Context context, Cursor c,
                                   ConversationListItemView.HostInterface hostInterface) {
        super(context, c, 0);
        mHostInterface = hostInterface;
        setHasStableIds(true);
    }

    @Override
    public void bindViewHolder(ConversationListViewHolder holder, Context context, Cursor cursor) {
        final ConversationListItemView conversationListItemView = holder.mView;
        conversationListItemView.bind(cursor, mHostInterface);
    }

    @Override
    public ConversationListViewHolder createViewHolder(Context context, ViewGroup parent, int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        final ConversationListItemView itemView =
                (ConversationListItemView) layoutInflater.inflate(
                        R.layout.layout_conversation_list_item_view, null);
        return new ConversationListViewHolder(itemView);
    }

    public static class ConversationListViewHolder extends RecyclerView.ViewHolder {
        final ConversationListItemView mView;

        public ConversationListViewHolder(final ConversationListItemView itemView) {
            super(itemView);
            mView = itemView;
        }
    }
}
