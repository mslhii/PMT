package com.kritikalerror.pmt.utils;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.kritikalerror.pmt.R;
import com.parse.ParseUser;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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

    private static final int[] colorPicker = {Color.parseColor("#9933CC"),
            Color.parseColor("#669900"),
            Color.parseColor("#0099CC"),
            Color.DKGRAY,
            Color.parseColor("#FF8800"),
            Color.parseColor("#CC0000"),
            Color.parseColor("#FFDE82")};

    private static boolean[] colorUsage = {false,
            false,
            false,
            false,
            false,
            false,
            false};

    private Context context;
    private int mLength;

    private static final String ADMOB_PUBLISHER_ID = "ca-app-pub-6309606968767978/2177105243";

    public FriendListViewAdapter(Context ctx, List<ParseUser> parseUsers) {
        parseUsers.add(0, null);

        this.context = ctx;
        this.mLength = parseUsers.size();
        this.mLayoutInflater = LayoutInflater.from(ctx);
        this.mParseUsers = parseUsers;
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

        Log.e("PMTFA", "position is: " + position);
        if (position == 0) {
            if (convertView == null || !(convertView instanceof AdView)) {
                AdView adView = new AdView(this.context);
                adView.setAdSize(AdSize.BANNER);
                adView.setAdUnitId(ADMOB_PUBLISHER_ID);
                AdRequest.Builder adRequestBuilder = new AdRequest.Builder();

                for (int i = 0; i < adView.getChildCount(); i++) {
                    adView.getChildAt(i).setFocusable(false);
                }

                adView.setFocusable(false);

                float density = this.context.getResources().getDisplayMetrics().density;
                int height = Math.round(AdSize.BANNER.getHeight() * density);
                AbsListView.LayoutParams params
                        = new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT,
                        height);
                adView.setLayoutParams(params);
                adView.loadAd(adRequestBuilder.build());

                convertView = adView;
            }
        }
        else {
            if (convertView == null || convertView instanceof AdView) {
                convertView = mLayoutInflater.inflate(R.layout.list_item_friend, parent, false);

                holder = new ViewHolder();
                holder.username = (TextView) convertView.findViewById(R.id.friend_username);

                convertView.setTag(holder);

                // Generate some nice backgrounds per user
                convertView.setBackgroundColor(determineColor());
                Log.e("PMTFA", "crashes in convertview");
            } else {
                holder = (ViewHolder) convertView.getTag();
                Log.e("PMTFA", "crashes in else");
            }

            holder.username.setText(friend.getUsername());
        }

        return convertView;
    }

    /**
     * Function to pick color in sequential order based on the boolean
     * @return
     */
    public static int determineColor()
    {
        for (int i = 0; i < colorUsage.length; i++)
        {
            if (!colorUsage[i])
            {
                colorUsage[i] = true;
                return colorPicker[i];
            }
        }

        for (int j = 1; j < colorUsage.length; j++)
        {
            colorUsage[j] = false;
        }
        return colorPicker[0];
    }

    public static class ViewHolder {
        public TextView username;
    }

}
