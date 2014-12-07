package com.kritikalerror.pmt;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.kritikalerror.pmt.authenticator.AuthenticationActivity;
import com.kritikalerror.pmt.utils.FriendListViewAdapter;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael H.
 */
public class MainActivity extends Activity {

    private ParseUser mCurrentUser;
    private List<ParseUser> mUserFriends;
    private FriendListViewAdapter mFriendAdapter;
    private AdView mAdView;
    private RelativeLayout mLayout;
    private Comparator<ParseUser> mComparator = new UserComparator();
    private SharedPreferences mSharedPreferences;
    private Map<String, String> mGroupMap = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = new RelativeLayout(this);
        ParseAnalytics.trackAppOpened(getIntent());

        isUserAuthenticated();
        if (mCurrentUser != null)
        {
            registerPushNotification();
            loadFriendList();

            loadAds();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.add_group:
                pmtGroup();
                return true;
            case R.id.add_user:
                addFriend();
                return true;
            case R.id.settings:
                ParseUser.logOut();
                isUserAuthenticated();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // If user is not authenticated, take them to the AuthenticationActivity
    private void isUserAuthenticated()
    {
        // Check if user is authenticated
        mCurrentUser = ParseUser.getCurrentUser();
        if (mCurrentUser == null) {
            startActivity(new Intent(this, AuthenticationActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }

    private void pmtGroup()
    {
        final ArrayList<String> pmtSend = new ArrayList<String>();
        int friendsLength = mUserFriends.size();
        final CharSequence[] items = new CharSequence[friendsLength];
        final boolean[] itemsChecked = new boolean[items.length];
        Log.e("PMT", "length is " + items.length);
        Log.e("PMT", "checked is " + itemsChecked.length);
        for (int i = 0; i < friendsLength; i++)
        {
            Log.e("PMT", "i is " + i);
            items[i] = mUserFriends.get(i).getUsername();
        }

        //TODO: initializing all to false for now, discard later
        for (int j = 0; j < itemsChecked.length; j++)
        {
            itemsChecked[j] = false;
        }

        mSharedPreferences = getSharedPreferences("PMTGroupList", Context.MODE_PRIVATE);
        SharedPreferences.Editor keyValuesEditor = mSharedPreferences.edit();

        // Transfer current friend list to group list
        for (ParseUser friendObject : mUserFriends)
        {
            //mGroupMap.put(friendObject.getUsername(), "false");
            if (!mSharedPreferences.contains(friendObject.getUsername()))
            {
                keyValuesEditor.putBoolean(friendObject.getUsername(), false);
            }
        }
        keyValuesEditor.commit();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Group PMT");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (int i = 0; i < items.length; i++) {
                    if (itemsChecked[i]) {
                        // Create our Installation query
                        ParseQuery pushQuery = ParseInstallation.getQuery();
                        pushQuery.whereEqualTo("user", mUserFriends.get(i));

                        // Send push notification to query
                        ParsePush push = new ParsePush();
                        push.setQuery(pushQuery); // Set our Installation query
                        push.setMessage(mCurrentUser.getUsername() + " wants PMT");
                        push.sendInBackground();

                        Toast.makeText(MainActivity.this, "Sent the group a PMT", Toast.LENGTH_SHORT).show();

                        itemsChecked[i] = false;
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setMultiChoiceItems(items,
                itemsChecked,
                new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                itemsChecked[which] = isChecked;
            }
        });
        builder.show();
    }

    private void addFriend()
    {
        final RelativeLayout container = (RelativeLayout) findViewById(R.id.add_user_container);
        final EditText input = (EditText) findViewById(R.id.add_user_input);

        if (container.isShown())
        {
            container.setVisibility(View.GONE);
        }
        else
        {
            container.setVisibility(View.VISIBLE);
        }

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    container.setVisibility(View.GONE);

                    final String friend = v.getText().toString().toUpperCase();

                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("username", friend);
                    query.getFirstInBackground(new GetCallback<ParseUser>() {
                        @Override
                        public void done(final ParseUser parseUser, ParseException e) {
                            if (parseUser == null) {
                                Toast.makeText(getBaseContext(),
                                        getString(R.string.error_does_not_exist),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                // Save the friend relationship to Parse
                                ParseObject query = new ParseObject("Friend");
                                query.put("user", mCurrentUser);
                                query.put("friend", friend);
                                query.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            mUserFriends.add(parseUser);
                                            mFriendAdapter.notifyDataSetChanged();
                                        } else {
                                            Toast.makeText(getBaseContext(),
                                                    getString(R.string.error_oops),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                        }
                    });

                    // Clear the editor when finished
                    v.setText("");
                }
                return false;
            }
        });
    }

    private void deleteFriend(final ParseUser name)
    {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Friend");
        query.whereEqualTo("friend", name.getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> results, ParseException e) {
                if (results == null) {
                    Toast.makeText(getBaseContext(),
                            getString(R.string.error_does_not_exist),
                            Toast.LENGTH_LONG).show();
                } else {
                    // Find user to be deleted
                    for (ParseObject user : results) {
                        ParseUser tempUser = (ParseUser) user.get("user");
                        try {
                            tempUser = tempUser.fetchIfNeeded();
                        } catch (ParseException e1) {
                            e1.printStackTrace();
                        }

                        if ((tempUser.getUsername().equals(mCurrentUser.getUsername())) &&
                                (tempUser.getUsername().equals(name.getUsername()))) {

                            user.deleteInBackground();
                            if (e == null) {
                                mUserFriends.remove(name);
                                mFriendAdapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(getBaseContext(),
                                        getString(R.string.error_oops),
                                        Toast.LENGTH_LONG).show();
                            }

                            Log.e("PMT", "deleted user is: " + tempUser.getUsername());
                        }
                        else
                        {
                            Log.e("PMT", "cannot find: " + tempUser.getUsername()
                                    + " and " + mCurrentUser.getUsername());
                        }
                    }
                }
            }
        });
    }

    private void loadFriendList()
    {
        final ListView listview = (ListView) findViewById(R.id.listView);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                // Create our Installation query
                ParseQuery pushQuery = ParseInstallation.getQuery();
                pushQuery.whereEqualTo("user", mUserFriends.get(position));

                // Send push notification to query
                ParsePush push = new ParsePush();
                push.setQuery(pushQuery); // Set our Installation query
                push.setMessage(mCurrentUser.getUsername() + " wants PMT");
                push.sendInBackground();

                Toast.makeText(MainActivity.this, "Sent a PMT", Toast.LENGTH_SHORT).show();
            }
        });

        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                final ParseUser selectedUser = (ParseUser) parent.getItemAtPosition(position);

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Options")
                        .setItems(new CharSequence[]{"Delete"},
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case 0:
                                                deleteFriend(selectedUser);

                                                Toast.makeText(MainActivity.this, "Deleted " + selectedUser.getUsername(), Toast.LENGTH_SHORT).show();
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                return false;
            }
        });

        mUserFriends = new ArrayList<ParseUser>();
        mFriendAdapter = new FriendListViewAdapter(getBaseContext(), mUserFriends);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Friend");
        query.whereEqualTo("user", mCurrentUser);
        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.whereMatchesKeyInQuery("username", "friend", query);
        userQuery.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (parseUsers == null) {
                    Toast.makeText(getBaseContext(), getString(R.string.error_oops),
                            Toast.LENGTH_LONG).show();
                } else {
                    mUserFriends = parseUsers;
                    Collections.sort(mUserFriends, mComparator);
                    mFriendAdapter = new FriendListViewAdapter(getBaseContext(), mUserFriends);
                    listview.setAdapter(mFriendAdapter);
                    mFriendAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    // Associate the device with a user
    public void registerPushNotification() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", mCurrentUser);
        installation.saveInBackground();
    }

    /**
     * Load Ads in MainActivity screen
     * Don't want to load them first in Launcher,
     */
    private void loadAds()
    {
        /*
        // Create and setup the AdMob view
        mAdView = new AdView(this);
        FrameLayout layout = (FrameLayout) findViewById(R.id.map);
        mAdView.setAdSize(AdSize.SMART_BANNER);
        mAdView.setAdUnitId("ca-app-pub-6309606968767978/6485120847");
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        // Get the height for offset calculations
        AdSize adSize = mAdView.getAdSize();
        //mAdHeight = adSize.getHeight();
        mAdHeight = adSize.getHeightInPixels(getApplicationContext());
        // Add the AdMob view
        FrameLayout.LayoutParams adParams =
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mAdView, adParams);
        mAdView.loadAd(adRequestBuilder.build());
        */

        // Create and setup the AdMob view
        mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.SMART_BANNER);
        mAdView.setAdUnitId("ca-app-pub-6309606968767978/2177105243");
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();

        RelativeLayout.LayoutParams adParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        adParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        adParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        mLayout.addView(mAdView, adParams);

        mAdView.loadAd(adRequestBuilder.build());
    }


    public class UserComparator implements Comparator<ParseUser>
    {
        @Override
        public int compare(ParseUser arg0, ParseUser arg1)
        {
            return arg0.getUsername().compareToIgnoreCase(arg1.getUsername());
        }
    }
}
