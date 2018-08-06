package com.pusheenicorn.safetyapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class VideoActivity extends BaseActivity implements DialogInterface.OnClickListener, SurfaceHolder.Callback {

    public static final String LOGTAG = "VIDEOCAPTURE";

    //declare variable for media recorder (used to record video from app directly)
    private MediaRecorder recorder;
    //interface for someone holding a display surface
    private SurfaceHolder holder;
    //provides preset qualities
    private CamcorderProfile camcorderProfile;
    private Camera camera;
    ImageButton ibRecord;
    TextView tvStop;

    //booleans to keep track of whether or not the video attributes should be shown
    boolean recording = false;
    boolean usecamera = true;
    boolean previewRunning = false;

    // Define bottom navigation view.
    BottomNavigationView bottomNavigationView;

    //variables for the draw out menu
    ListView mDrawerList;
    RelativeLayout mDrawerPane;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    ArrayList<MainActivity.NavItem> mNavItems = new ArrayList<MainActivity.NavItem>();

    long duration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //ask system to exclude title at the top of the screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //set the orientation of the screen to be portrait, avoiding the auto-adjust to landscape for the app
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //set the camera quality
        camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);

        setContentView(R.layout.activity_video);

        // Logic for bottom navigation view
        bottomNavigationView = findViewById(R.id.bottom_navigation_video);
        setNavigationDestinations(VideoActivity.this, bottomNavigationView);

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
                        VideoActivity.this, mNavItems);
            }
        });

        //initialize the surfaceview
        SurfaceView cameraView = (SurfaceView) findViewById(R.id.CameraView);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        //create buffers for the camera device
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        prepareRecorder();

        //start and stop camera view
//        cameraView.setClickable(true);
//        cameraView.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                if (recording) {
//                    recorder.stop();
//                    recording = false;
//                    if (usecamera) {
//                        try {
//                            camera.reconnect();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    // recorder.release();
//                    Log.v(LOGTAG, "Recording Stopped");
//                    //finish recording and notify user, then return to main activity
//                    Toast.makeText(VideoActivity.this, "Successfully recorded video", Toast.LENGTH_SHORT).show();
//                    Intent finishRecording = new Intent(VideoActivity.this, MainActivity.class);
//                    startActivity(finishRecording);
//                    // Let's prepareRecorder so we can record again
////                    prepareRecorder();
//                } else {
//                    recording = true;
//                    recorder.start();
//                    Log.v(LOGTAG, "Recording Started");
//                }
//            }
//        });
        tvStop = findViewById(R.id.tvStop);
        tvStop.setVisibility(View.INVISIBLE);

        ibRecord = findViewById(R.id.ibRecord);
        ibRecord.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
//                tvStop.setVisibility(View.VISIBLE);
                if (recording) {
                    recorder.stop();
                    recording = false;
                    if (usecamera) {
                        try {
                            camera.reconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                     recorder.release();
//                    camera.release();
//                    recording = false;
                    Log.v(LOGTAG, "Recording Stopped");
                    //finish recording and notify user, then return to main activity
                    Toast.makeText(VideoActivity.this, "Successfully recorded video", Toast.LENGTH_SHORT).show();
                    Intent finishRecording = new Intent(VideoActivity.this, MainActivity.class);
                    startActivity(finishRecording);
                    // Let's prepareRecorder so we can record again
//                    prepareRecorder();
                } else {
                    recording = true;
                    tvStop.setVisibility(View.VISIBLE);
                    recorder.start();
                    Log.v(LOGTAG, "Recording Started");
                }
            }
        });

    }

        private void prepareRecorder() {
            recorder = new MediaRecorder();
            recorder.setPreviewDisplay(holder.getSurface());

            if (usecamera) {
                //set the camera to be portrait mode
                camera.setDisplayOrientation(90); // use for set the orientation of the preview
                recorder.setOrientationHint(90); // use for set the orientation of output video
                camera.unlock();
                recorder.setCamera(camera);
            }

            //allow recorder to have audio and video
            recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

            recorder.setProfile(camcorderProfile);

            //saving file based on the format
            if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.THREE_GPP ) {
                try {
                    File newFile = File.createTempFile("videocapture", ".3gp", Environment.getExternalStorageDirectory());
                    recorder.setOutputFile(newFile.getAbsolutePath());
//                    MediaPlayer mp = new MediaPlayer();
//                    FileInputStream stream = new FileInputStream(newFile);
//                    mp.setDataSource(stream.getFD());
//                    stream.close();
//                    mp.prepare();
//                    duration = mp.getDuration();
//                    mp.release();
                } catch (IOException e) {
                    Log.v(LOGTAG,"Couldn't create file");
                    e.printStackTrace();
                    finish();
                }
            } else if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
                try {
                    File newFile = File.createTempFile("videocapture", ".mp4", Environment.getExternalStorageDirectory());
                    recorder.setOutputFile(newFile.getAbsolutePath());
//                    MediaPlayer mp = new MediaPlayer();
//                    FileInputStream stream = new FileInputStream(newFile);
//                    mp.setDataSource(stream.getFD());
//                    stream.close();
//                    mp.prepare();
//                    duration = mp.getDuration();
//                    mp.release();
                } catch (IOException e) {
                    Log.v(LOGTAG,"Couldn't create file");
                    e.printStackTrace();
                    finish();
                }
            } else {
                try {
                    File newFile = File.createTempFile("videocapture", ".mp4", Environment.getExternalStorageDirectory());
                    recorder.setOutputFile(newFile.getAbsolutePath());
//                    MediaPlayer mp = new MediaPlayer();
//                    FileInputStream stream = new FileInputStream(newFile);
//                    mp.setDataSource(stream.getFD());
//                    stream.close();
//                    mp.prepare();
//                    duration = mp.getDuration();
//                    mp.release();
                } catch (IOException e) {
                    Log.v(LOGTAG,"Couldn't create file");
                    e.printStackTrace();
                    finish();
                }

            }

            try {
                recorder.prepare();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                finish();
            } catch (IOException e) {
                e.printStackTrace();
                finish();
            }
        }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(LOGTAG, "surfaceCreated");

        if (usecamera) {
            camera = Camera.open();

            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
                previewRunning = true;
            }
            catch (IOException e) {
                Log.e(LOGTAG,e.getMessage());
                e.printStackTrace();
            }
        }

    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(LOGTAG, "surfaceChanged");

        if (!recording && usecamera) {
            if (previewRunning){
                camera.stopPreview();
            }

            try {
                Camera.Parameters p = camera.getParameters();

                p.setPreviewSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
                p.setPreviewFrameRate(camcorderProfile.videoFrameRate);

                camera.setParameters(p);

                camera.setPreviewDisplay(holder);
                camera.startPreview();
                previewRunning = true;
            }
            catch (IOException e) {
                Log.e(LOGTAG,e.getMessage());
                e.printStackTrace();
            }

            prepareRecorder();
        }
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(LOGTAG, "surfaceDestroyed");
        if (recording) {
            recorder.stop();
            recording = false;
        }
        recorder.release();
        if (usecamera) {
            previewRunning = false;
            //camera.lock();
            camera.release();
        }
        finish();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }

}
