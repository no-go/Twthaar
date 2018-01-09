package de.digisocken.Read_o_Tweet;

import android.app.UiModeManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.Tweet;
import de.digisocken.Read_o_Tweet.tweetcomposer.ComposerActivity;

import android.support.design.widget.NavigationView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Stack;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public ListView mListView;
    public ArrayList<Tweet> tweetArrayList;
    public TweetAdapter adapter;
    public ImageButton button;
    public ImageButton buttonRevert;
    public ImageButton buttonAddStart;
    public EditText editText;
    public RelativeLayout searchBox;
    public Stack< String > history;

    private String iniQuery;

    TwitterLoginButton loginButton;


    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem mi2 = menu.findItem(R.id.action_allImages);
        mi2.setChecked(ReadOTweetApp.mPreferences.getBoolean("imageful", true));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggleSearch:
                if (searchBox.getVisibility() == View.GONE) {
                    try {
                        ActionBar ab = getSupportActionBar();
                        if(ab != null) ab.setElevation(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    searchBox.setVisibility(View.VISIBLE);
                } else {
                    try {
                        ActionBar ab = getSupportActionBar();
                        if(ab != null) ab.setElevation(10);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    searchBox.setVisibility(View.GONE);
                }
                break;
            case R.id.action_preferences2:
                Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                break;
            case R.id.action_allImages:
                if (item.isChecked()) {
                    adapter.imageful = false;
                    ReadOTweetApp.mPreferences.edit().putBoolean("imageful", false).apply();
                    item.setChecked(false);
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.imageful = true;
                    ReadOTweetApp.mPreferences.edit().putBoolean("imageful", true).apply();
                    item.setChecked(true);
                    adapter.notifyDataSetChanged();
                }
                break;
            case R.id.action_flattr:
                Intent intentFlattr = new Intent(Intent.ACTION_VIEW, Uri.parse(ReadOTweetApp.FLATTR_LINK));
                startActivity(intentFlattr);
                break;
            case R.id.action_project:
                Intent intentProj= new Intent(Intent.ACTION_VIEW, Uri.parse(ReadOTweetApp.PROJECT_LINK));
                startActivity(intentProj);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.together);

        try {
            ActionBar ab = getSupportActionBar();
            if(ab != null) {
                ab.setDisplayShowHomeEnabled(true);
                ab.setHomeButtonEnabled(true);
                ab.setDisplayUseLogoEnabled(true);
                ab.setLogo(R.mipmap.ic_launcher);
                if (BuildConfig.DEBUG) {
                    ab.setTitle(" " + getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
                } else {
                    ab.setTitle(" " + getString(R.string.app_name));
                }
                ab.setElevation(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        searchBox = (RelativeLayout) findViewById(R.id.searchBox);
        mListView = (ListView) findViewById(R.id.list);
        button = (ImageButton) findViewById(R.id.button);
        buttonRevert = (ImageButton) findViewById(R.id.cleanUpBtn);
        buttonAddStart = (ImageButton) findViewById(R.id.addStartBtn);
        editText = (EditText) findViewById(R.id.editText);
        loginButton = new TwitterLoginButton(this);

        iniQuery = ReadOTweetApp.mPreferences.getString("STARTUSERS", getString(R.string.defaultStarts));
        editText.setText(iniQuery);
        button.setOnClickListener(new SearchListener(this, ""));

        buttonRevert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setText("");
            }
        });

        buttonAddStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] queries = editText.getText().toString().split(",");
                for (String q : queries) {
                    q = q.trim();
                    if (q.startsWith("#")) {
                        if(!iniQuery.contains(q)) {
                            iniQuery += "," + q;
                        }
                    } else if (q.startsWith("@")) {
                        if(!iniQuery.contains(q)) {
                            iniQuery += "," + q;
                        }
                    }
                }
                TextView favouriteList = (TextView) findViewById(R.id.favouriteList);
                favouriteList.setText(iniQuery.replace(",","\n"));
                ReadOTweetApp.mPreferences.edit().putString("STARTUSERS",iniQuery).apply();
                Toast.makeText(MainActivity.this, R.string.favAdded, Toast.LENGTH_SHORT).show();
            }
        });

        tweetArrayList = new ArrayList<>();
        history = new Stack<>();
        history.push(iniQuery);
        adapter = new TweetAdapter(this, tweetArrayList);
        mListView.setAdapter(adapter);

        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                ReadOTweetApp.session = result.data;
                ReadOTweetApp.username = ReadOTweetApp.session.getUserName();
            }

            @Override
            public void failure(TwitterException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, getString(R.string.youNeedApiKey), Toast.LENGTH_LONG).show();
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ReadOTweetApp.username.equals("")) {
                    loginButton.callOnClick();
                } else {
                    String[] qeris = editText.getText().toString().split(",");
                    final Intent intent;

                    if (ReadOTweetApp.umm.getNightMode() == UiModeManager.MODE_NIGHT_YES) {

                        intent = new ComposerActivity.Builder(MainActivity.this)
                                .session(ReadOTweetApp.session)
                                .text(qeris[0])
                                .darkTheme()
                                .createIntent();
                    } else {
                        intent = new ComposerActivity.Builder(MainActivity.this)
                                .session(ReadOTweetApp.session)
                                .text(qeris[0])
                                .createIntent();
                    }
                    //intent.putExtra("EXTRA_THEME", R.style.ComposerLight);
                    //if (ReadOTweetApp.night) intent.putExtra("EXTRA_THEME", R.style.ComposerDark);
                    startActivity(intent);
                }
            }
        });

        FloatingActionButton fabCam = (FloatingActionButton) findViewById(R.id.fabCam);
        int numberOfCameras = Camera.getNumberOfCameras();

        PackageManager pm = getPackageManager();
        final boolean deviceHasCameraFlag = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if( !deviceHasCameraFlag || numberOfCameras==0 ) fabCam.setVisibility(View.GONE);

        fabCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ReadOTweetApp.username.equals("")) {
                    loginButton.callOnClick();
                } else {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        });

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (
                ReadOTweetApp.mPreferences.getString("CONSUMER_KEY","").equals("") ||
                ReadOTweetApp.mPreferences.getString("CONSUMER_SECRET","").equals("")
        ) {
            ViewGroup hintView = (ViewGroup) getLayoutInflater().inflate(R.layout.tweet_item, null);
            mListView.addFooterView(hintView);
        }

        button.callOnClick();
    }

    public void sortTweetArrayList() {
        Collections.sort(tweetArrayList, new Comparator<Tweet>() {
            @Override
            public int compare(Tweet t2, Tweet t1) {
                try {
                    Date parsedDate1 = ReadOTweetApp.formatIn.parse(t1.createdAt);
                    Date parsedDate2 = ReadOTweetApp.formatIn.parse(t2.createdAt);
                    return parsedDate1.compareTo(parsedDate2);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
    }



    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        TextView favouriteList = (TextView) findViewById(R.id.favouriteList);
        TextView meTxt = (TextView) findViewById(R.id.fullname);
        TextView meScrTxt = (TextView) findViewById(R.id.screenname);

        if (! ReadOTweetApp.username.equals("")) {
            meScrTxt.setText("@" + ReadOTweetApp.username);
            meTxt.setText(ReadOTweetApp.realname);
        }

        favouriteList.setText(
                ReadOTweetApp.mPreferences.getString("STARTUSERS", getString(R.string.defaultStarts)).replace(",","\n")
        );
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_preferences) {
            Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }else if (id == R.id.action_info) {
            Intent intentProj= new Intent(Intent.ACTION_VIEW, Uri.parse(ReadOTweetApp.PROJECT_LINK));
            startActivity(intentProj);
        } else {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            Snackbar.make(
                    drawer,
                    "Replace with your own action",
                    Snackbar.LENGTH_LONG
            ).setAction("Action", null).show();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (history.size() > 0) {
            editText.setText(history.pop());
            button.callOnClick();
        } else {
            history.push(iniQuery);
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(String.format(getString(R.string.closing), getString(R.string.app_name)))
                    .setMessage(getString(R.string.sureToClose))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            realExit();
                        }

                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // image preview
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] bitmapdata = bos.toByteArray();

            try {
                File mTmpFile = File.createTempFile("tmp", ".png", getCacheDir());
                FileOutputStream fos = new FileOutputStream(mTmpFile);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();

                Uri imgUri = Uri.fromFile(mTmpFile);

                final Intent intent;

                // make image tweet
                if (ReadOTweetApp.umm.getNightMode() == UiModeManager.MODE_NIGHT_YES) {

                    intent = new ComposerActivity.Builder(MainActivity.this)
                            .session(ReadOTweetApp.session)
                            .image(imgUri)
                            .darkTheme()
                            .createIntent();
                } else {
                    intent = new ComposerActivity.Builder(MainActivity.this)
                            .session(ReadOTweetApp.session)
                            .image(imgUri)
                            .createIntent();
                }
                startActivity(intent);

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            loginButton.onActivityResult(requestCode, resultCode, data);
        }

    }

    public void realExit() {
        super.onBackPressed();
    }
}