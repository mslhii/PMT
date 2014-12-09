package com.kritikalerror.pmt.utils;

import com.kritikalerror.pmt.R;
import com.parse.ParseUser;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

/**
 * @author Michael H.
 */
public class FriendListViewAdapter extends BaseAdapter {

    private static LayoutInflater mLayoutInflater;
    private List<ParseUser> mParseUsers;

    private final int[] colorPicker = {Color.BLACK,
            Color.BLUE,
            Color.CYAN,
            Color.DKGRAY,
            Color.GRAY,
            Color.GREEN,
            Color.LTGRAY,
            Color.MAGENTA,
            Color.RED,
            Color.YELLOW};

    public FriendListViewAdapter(Context ctx, List<ParseUser> parseUsers) {
        mLayoutInflater = LayoutInflater.from(ctx);
        mParseUsers = parseUsers;
    }

    @Override
    public int getCount() {
        return mParseUsers != null ? mParseUsers.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mParseUsers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        ParseUser friend = (ParseUser) getItem(position);

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_item_friend, parent, false);

            holder = new ViewHolder();
            holder.username = (TextView) convertView.findViewById(R.id.friend_username);

            convertView.setTag(holder);

            // Generate some nice backgrounds per user
            convertView.setBackgroundColor(colorPicker[randInt(0, 9)]);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.username.setText(friend.getUsername());

        return convertView;
    }

    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    public static class ViewHolder {

        public TextView username;
    }

}
