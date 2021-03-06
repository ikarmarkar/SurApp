package com.pusheenicorn.safetyapp.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pusheenicorn.safetyapp.R;
import com.pusheenicorn.safetyapp.adapters.events.EventUsersAdapter;
import com.pusheenicorn.safetyapp.adapters.friends.FriendsAdapter;
import com.pusheenicorn.safetyapp.models.Alert;
import com.pusheenicorn.safetyapp.models.Event;
import com.pusheenicorn.safetyapp.models.Friend;
import com.pusheenicorn.safetyapp.models.User;
import com.pusheenicorn.safetyapp.utils.NotificationUtil;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class EventsActivity extends BaseActivity {
    //declare the bottom navigation view
    private BottomNavigationView bottomNavigationView;
    // Variables for the draw out menu
    ListView mDrawerList;
    RelativeLayout mDrawerPane;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    ArrayList<NavItem> mNavItems = new ArrayList<NavItem>();
    //declared views
    private ImageButton ibBanner;
    private ImageButton ibAddMembers;
    private Button btnSendAlert;
    private ImageButton ibSearch;
    private TextView tvEventTitle;
    private EditText tvMessage;
    private EditText etUsername;
    private Button btnSend;
    // Declare notification utility
    NotificationUtil notificationUtil;
    static final int REQUEST_CODE = 1;
    static final String TAG = "Success";
    private static int RESULT_LOAD_IMAGE = 1;
    Event currentEvent;
    Context context;
    // Declare adapters.
    EventUsersAdapter eventUsersAdapter;
    ArrayList<User> users;
    RecyclerView rvUsers;
    FriendsAdapter eventFriendsAdapter;
    ArrayList<Friend> friends;
    RecyclerView rvFriends;
    // Declare current user and instantiate fields for later use.
    User currentUser;
    User user;
    List<Alert> alerts;
    // Declare some keys.
    private static String KEY_USERNAME = "username";
    private static String KEY_BANNER = "bannerimage";
    private static String KEY_EVENT = "event";
    public static String INTENT_EVENT_KEY = "event";
    public static String INTENT_USER_KEY = "user";
    public static String SERVICE_KEY = "service";
    public static String ALERT_EVENT = "eventAlert";
    public static String IMAGE_KEY = "bannerimage";
    public static String IMAGE_NAME = "image_file.jpeg";

    /**
     * This is executed on creation. It performs all the tasks
     * that need to be done in the "main" for this activity.
     *
     * @param savedInstanceState- the base bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);
        context = this;                             // Set the context.
        initializeViews();                          // Initialize all views.
        initializeNavigation();                     // Initialize navigation logic.
        allowBannerFunctionality();                 // Allow banner to change.
        loadEventUsers();                           // Populate the recycler views appropriately.
    }

    /***
     * This functions sets the functionality for changing the banner image.
     */
    public void allowBannerFunctionality() {
        ibBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //create a picture chooser from gallery
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
    }

    /**
     * This function initializes the navigation view for the activity.
     */
    public void initializeNavigation() {
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
                        EventsActivity.this, mNavItems);
            }
        });
    }

    /**
     * This function finds all the views and saves them
     * for future use.
     */
    public void initializeViews() {
        // Get the current user
        currentUser = (User) ParseUser.getCurrentUser();
        // Unwrap event
        currentEvent = (Event) getIntent().getParcelableExtra(KEY_EVENT);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        etUsername = (EditText) findViewById(R.id.etUsername);
        ibAddMembers = (ImageButton) findViewById(R.id.ibAddMembers);
        ibSearch = (ImageButton) findViewById(R.id.ibSearch);
        btnSendAlert = (Button) findViewById(R.id.btnSendAlert);
        //added title to the event page
        tvEventTitle.setText(currentEvent.getName());
        tvMessage = (EditText) findViewById(R.id.tvMessage);
        //initialize bottom navigation bar
        bottomNavigationView = findViewById(R.id.bottom_navigation_events);
        setNavigationDestinations(EventsActivity.this, bottomNavigationView);
        btnSend = (Button) findViewById(R.id.btnSend);
        notificationUtil = new NotificationUtil(context, currentUser);
        rvUsers = (RecyclerView) findViewById(R.id.rvUsers);
        users = new ArrayList<User>();
        rvFriends = (RecyclerView) findViewById(R.id.rvFriends);
        friends = new ArrayList<Friend>();
        eventFriendsAdapter = new FriendsAdapter(friends);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(eventFriendsAdapter);
        eventUsersAdapter = new EventUsersAdapter(users, friends, currentUser, eventFriendsAdapter);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(eventUsersAdapter);
        //set banner to allow user to access gallery
        ibBanner = (ImageButton) findViewById(R.id.ibBanner);
        ParseFile bannerImage = currentEvent.getParseFile(KEY_BANNER);
        if (bannerImage != null) {
            //load image using glide
            Glide.with(EventsActivity.this).load(bannerImage.getUrl())
                    .into(ibBanner);
        }
        etUsername.setSelection(etUsername.getText().length());
    }

    /**
     * This function populates the events page.
     */
    public void loadEventUsers() {
        final User.Query userQuery = new User.Query();
        userQuery.getTop();

        userQuery.findInBackground(new FindCallback<User>() {
            @Override
            public void done(List<User> objects, ParseException e) {
                if (e == null) {
                    for (int i = objects.size() - 1; i > -1; i--) {
                        // If this user belongs to the event
                        if (currentEvent.getUsersIds().contains(objects.get(i).getObjectId())) {
                            // If this user is a friend, then...
                            if (currentUser.getFriendUsers().contains(objects.get(i).getObjectId())) {
                                int index = currentUser.getFriendUsers()
                                        .indexOf(objects.get(i).getObjectId());
                                Friend newFriend = currentUser.getFriends().get(index);
                                friends.add(newFriend);
                            } else {
                                users.add(objects.get(i));
                            }
                        }
                    }
                    eventUsersAdapter.notifyDataSetChanged();
                } else {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This function is called once the implicit intent for the camera returns to
     * the activity
     *
     * @param requestCode- the request code attached to the result
     * @param resultCode-  the result code attached to the result
     * @param data-        the data passed back.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // if request code matches and the data is not null, set the image bitmap to be
        // that of the picture
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            //get file path from the URI
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            //set the banner to the image that is selected by the user
            ibBanner.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            //convert bitmap to a parsefile
            final ParseFile parseFile = conversionBitmapParseFile(
                    BitmapFactory.decodeFile(picturePath));
            //save in background so the image updates correctly
            parseFile.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    currentEvent.setBannerImage(parseFile);
                    currentEvent.put(IMAGE_KEY, parseFile);
                    currentEvent.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                currentEvent.saveInBackground();
                                Log.d("EventsActivity", "Successfully updated!");
                            } else {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * This function converts the bitmap into a parse file
     *
     * @param imageBitmap
     * @return a parse file of the image that is uploaded
     */
    public ParseFile conversionBitmapParseFile(Bitmap imageBitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // Compress image to lower quality scale 1 - 100
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 25, stream);
        Bitmap resized = Bitmap.createScaledBitmap(imageBitmap, 150, 80, true);
        byte[] image = stream.toByteArray();

        // Create the ParseFile
        final ParseFile file  = new ParseFile(IMAGE_NAME, image);
        // Upload the image into Parse Cloud
        currentEvent.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                currentEvent.put("bannerimage",file);
            }
        });
        return file;
    }

    /**
     * This function makes the functionality for adding a new user visible.
     *
     * @param view
     */
    public void onAddUser(View view) {
        etUsername.setVisibility(View.VISIBLE);
        ibAddMembers.setVisibility(View.INVISIBLE);
        ibSearch.setVisibility(View.VISIBLE);
    }


    /**
     * This function is called when the user hits the search button
     * to search for a user to add to the event.
     *
     * @param view- the search button to be clicked.
     */
    public void onSearchUser(View view) {
        String username = etUsername.getText().toString();
        // Do not allow user to add themselves to an event that they are already part of!
        if (username == currentUser.getUsername()) {
            Toast.makeText(this, "Sorry, you are already part of this event!",
                    Toast.LENGTH_LONG).show();
            etUsername.setText("");
            etUsername.setVisibility(View.INVISIBLE);
            ibAddMembers.setVisibility(View.VISIBLE);
            ibSearch.setVisibility(View.INVISIBLE);
            return;
        } else if (currentEvent.getUserNames() != null &&
                currentEvent.getUserNames().contains(username)) {
            Toast.makeText(this, "Sorry this user is already part of this event!",
                    Toast.LENGTH_LONG).show();
        }
        // Allow them to enter other users as long as they can provide a valid username.
        final User.Query userQuery = new User.Query();
        userQuery.getTop().whereEqualTo(KEY_USERNAME, username);
        userQuery.findInBackground(new FindCallback<User>() {
            @Override
            public void done(List<User> objects, ParseException e) {
                if (e == null) {
                    if (objects != null) {
                        final User newUser = objects.get(0);
                        currentEvent.addUser(newUser);
                        currentEvent.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                eventFriendsAdapter.clear();
                                eventUsersAdapter.clear();
                                loadEventUsers();
                                newUser.addEvent(currentEvent);
                                newUser.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        etUsername.setVisibility(View.INVISIBLE);
                                        ibAddMembers.setVisibility(View.VISIBLE);
                                        ibSearch.setVisibility(View.INVISIBLE);
                                    }
                                });
                            }
                        });
                    }
                    // If the username is invalid, toast a message to this effect.
                    else {
                        Toast.makeText(context, "Sorry, you entered an invalid username.",
                                Toast.LENGTH_LONG).show();
                        etUsername.setVisibility(View.INVISIBLE);
                        ibAddMembers.setVisibility(View.VISIBLE);
                        ibSearch.setVisibility(View.INVISIBLE);
                    }
                } else {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This function is called when the send alert button is clicked. It
     * sets the visibility to add an event.
     *
     * @param view- the sendAlert Button to toggle visibility
     */
    public void onSendAlert(View view) {
        tvMessage.setVisibility(View.VISIBLE);
        btnSend.setVisibility(View.VISIBLE);
    }

    /**
     * This function is called when the send button is clicked. It sends
     * an event alert to the group.
     *
     * @param view- the button
     */
    public void onSend(View view) {
        String message = tvMessage.getText().toString();
        final Alert alert = new Alert();
        alert.setMessage(message);
        alert.setName(currentUser.getName());
        alert.setUserame(currentUser.getUsername());
        alert.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                currentEvent.addAlert(alert);
                currentEvent.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        tvMessage.setVisibility(View.INVISIBLE);
                        btnSend.setVisibility(View.INVISIBLE);
                        // getEmergencyNotifications();
                    }
                });
            }
        });
    }

    /**
     * This function links to the Settings page when the settings button
     * is clicked.
     *
     * @param view- the button
     */
    public void onSettings(View view) {
        Intent intent = new Intent(EventsActivity.this, SettingsActivity.class);
        startActivity(intent);
    }
}
