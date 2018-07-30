package com.pusheenicorn.safetyapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.pusheenicorn.safetyapp.adapters.ChatAdapter;
import com.pusheenicorn.safetyapp.adapters.friends.SafeFriendsAdapter;
import com.pusheenicorn.safetyapp.models.Friend;
import com.pusheenicorn.safetyapp.models.User;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends BaseActivity {
    //declare necessary variables for fields on the screen
    Button btnSendText;
    EditText etTextMessage;
    IntentFilter intentFilter;
    TextView tvTextMessage;

    //initializing variables to populate the friend recycler view
    SafeFriendsAdapter friendAdapter;
    ArrayList<Friend> friends;
    RecyclerView rvChatFriendList;

    ChatAdapter chatAdapter;
    ArrayList<Friend> chats;
    RecyclerView rvChatList;

    //initializing the current user
    User currentUser;

    // Declare views
    BottomNavigationView bottomNavigationView;

    //variables for the draw out menu
    ListView mDrawerList;
    RelativeLayout mDrawerPane;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    ArrayList<MainActivity.NavItem> mNavItems = new ArrayList<MainActivity.NavItem>();

    //created a broadcast receiver to receive sms messages by responding to system-wide broadcast announcements
    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //display message in the text view
            tvTextMessage = (TextView) findViewById(R.id.tvTextMessage);
            //TODO - figure this out?? if the check is added then the message will not display :O
//            if (intent.equals("SMS_RECEIVED_ACTION")) {
                //set text view with the message and phone number from the reply
                tvTextMessage.setText(intent.getExtras().getString("message"));
//            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //set and populated the bottom navigation view
        bottomNavigationView = findViewById(R.id.bottom_navigation_chat);
        setNavigationDestinations(ChatActivity.this, bottomNavigationView);

        initializeNavItems(mNavItems);
        // DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Populate the Navigtion Drawer with options
        mDrawerPane = (RelativeLayout) findViewById(R.id.drawerPane);
        mDrawerList = (ListView) findViewById(R.id.navList);
        DrawerListAdapter adapter = new DrawerListAdapter(this, mNavItems);
        mDrawerList.setAdapter(adapter);

        // Drawer Item click listeners
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItemFromDrawer(position, mDrawerList, mDrawerPane, mDrawerLayout,
                        ChatActivity.this, mNavItems);
            }
        });

        //intent filter allows activity to know what the broadcast receiver can respond to
        intentFilter = new IntentFilter();
        intentFilter.addAction("SMS_RECEIVED_ACTION");
        //declare fields
        btnSendText = (Button) findViewById(R.id.btnSendText);
        etTextMessage = (EditText) findViewById(R.id.etTextMessage);
        btnSendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //code for sending the message
                String message = etTextMessage.getText().toString();
                //TODO - allow clicking on the friend object to take user to chat activity and auto-populate phone number
                Intent intent = getIntent();
                String number = intent.getStringExtra("number");
                sendMessage(number, message);
            }
        });

        rvChatFriendList = (RecyclerView) findViewById(R.id.rvChatFriendList);
        friends = new ArrayList<>();
        // construct the adapter from this data source
        friendAdapter = new SafeFriendsAdapter(friends);
        // recycler view setup
        rvChatFriendList.setLayoutManager(new LinearLayoutManager(this));
        rvChatFriendList.setAdapter(friendAdapter);

        rvChatList = (RecyclerView) findViewById(R.id.rvChatList);
        chats = new ArrayList<>();
        chatAdapter = new ChatAdapter(friends);
        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        rvChatList.setAdapter(chatAdapter);

        //initialize current user
        currentUser = (User) ParseUser.getCurrentUser();
        //populate the friends list
        populateFriendList();

    }

    public void populateFriendList() {
        final User.Query userQuery = new User.Query();
        userQuery.getTop();

        userQuery.findInBackground(new FindCallback<User>() {
            @Override
            public void done(List<User> objects, ParseException e) {
                if (e == null) {
                    for (int i = objects.size() - 1; i > -1; i--) {
                        // If this user is a friend of the current user
                        if (currentUser.getFriendUsers().contains(objects.get(i).getObjectId())) {
                            int index = currentUser.getFriendUsers()
                                    .indexOf(objects.get(i).getObjectId());
                            Friend newFriend = currentUser.getFriends().get(index);
                                friends.add(newFriend);
                                chats.add(newFriend);
                                friendAdapter.notifyDataSetChanged();
                                chatAdapter.notifyDataSetChanged();
                        } else {
                        }
                    }
                } else {
                    e.printStackTrace();
                }
            }
        });
    }

    protected void sendMessage(String number, String message) {
        String SENT = "Message Sent!!";
        String DELIVERED = "Message Delivered!";

        //token given to allow foreign application to access permissions and execute code
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        PendingIntent delieveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(number, null, message, sentPI, delieveredPI);
        //toast if the text is successfully made
        Toast.makeText(ChatActivity.this, "YOU SENT A MESSAGE!!", Toast.LENGTH_SHORT).show();
    }

    protected void onResume() {
        //register the receiver
        registerReceiver(intentReceiver, intentFilter);
        super.onResume();
    }

    protected void onPause() {
        //unregister the receiver
        unregisterReceiver(intentReceiver);
        super.onPause();
    }
}
