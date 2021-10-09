package com.redple.walzo;

import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adapters.SuggestedAdapter;
import com.adapters.WallpaperAdapter;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;
import com.interfaces.RecyclerViewClickListener;
import com.models.SuggestedModel;
import com.models.WallpaperModel;
import com.redple.walzo.activities.FullScreenWallpaper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, RecyclerViewClickListener {


    static final float END_SCALE = 0.7f;
    ImageView menuIcon;
    LinearLayout contentView;

    DrawerLayout drawerLayout;
    NavigationView navigationView;

    RecyclerView recyclerView, topMostRecyclerView;
    RecyclerView.Adapter adapter;
    WallpaperAdapter wallpaperAdapter;
    List<WallpaperModel> wallpaperModelList;

    ArrayList<SuggestedModel> suggestedModels = new ArrayList<>();

    Boolean isScrolling = false;
    int currentItems, totalItems, scrollOutItems;

    ProgressBar progressBar;
    TextView replaceTitle;

    EditText searchEt;
    ImageView searchIv;

    int pageNumber = 1;

    private int REQUEST_CODE = 11;


    String url = "https://api.pexels.com/v1/curated?page=" + pageNumber + "&per_page=80"; // we need multiple pages when scrolled



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        menuIcon = findViewById(R.id.menu_icon);
        contentView = findViewById(R.id.content_view);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        navigationDrawer();

        // Navigation drawer profile

        View headerView = navigationView.getHeaderView(0);
        ImageView appLogo = headerView.findViewById(R.id.app_image);

        recyclerView = findViewById(R.id.recyclerView);
        topMostRecyclerView = findViewById(R.id.suggestedRecyclerView);

        wallpaperModelList = new ArrayList<>();
        wallpaperAdapter = new WallpaperAdapter(this, wallpaperModelList);

        recyclerView.setAdapter(wallpaperAdapter);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);



//        scrolling behaviour

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                currentItems = gridLayoutManager.getChildCount();
                totalItems = gridLayoutManager.getItemCount();
                scrollOutItems = gridLayoutManager.findFirstVisibleItemPosition();

                if (isScrolling && (currentItems + scrollOutItems == totalItems)) {
                    isScrolling = false;
                    fetchWallpaper();
                }
            }
        });


        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        replaceTitle = (TextView) findViewById(R.id.topMostTitle);

        fetchWallpaper();

        suggestedItems();

//        search edit text and image view

        searchEt = findViewById(R.id.searchEv);
        searchIv = findViewById(R.id.search_image);
        searchIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                set the functionality later
                Toast.makeText(MainActivity.this, "Search", Toast.LENGTH_SHORT).show();
                String query = searchEt.getText().toString().toLowerCase();

                url = "https://api.pexels.com/v1/search/?page=" + pageNumber + "&per_page=80&query=" + query;
                wallpaperModelList.clear();
                fetchWallpaper();

            }
        });
    }

    private void navigationDrawer() {
//        Navigation drawer
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_About);

        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

//        Animation in the drawer
        animateNavigationDrawer();
    }

    private void animateNavigationDrawer() {
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
//                Scale the view based on the current slide offset
                final float diffScaledOffset = slideOffset * (1 - END_SCALE);
                final float offsetScale = 1 - diffScaledOffset;
                contentView.setScaleX(offsetScale);
                contentView.setScaleY(offsetScale);

//                Translate the view accounting of the scaled width
                final float xOffset = drawerView.getWidth() * slideOffset;
                final float xOffsetDiff = contentView.getWidth() * diffScaledOffset / 2;
                final float xTranslation = xOffset - xOffsetDiff;
                contentView.setTranslationX(xTranslation);
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {


            case R.id.nav_About:
//                Toast.makeText(this, "logout clicked", Toast.LENGTH_SHORT).show();
                gotoUrl("https://redple.dorik.io/");
                break;

            case R.id.nav_More:
//                Toast.makeText(this, "about clicked", Toast.LENGTH_SHORT).show();
                gotoUrl("https://play.google.com/store/apps/dev?id=6726727956687471964");
                break;

            case R.id.nav_Privacy_policy:
//                Toast.makeText(this, "about clicked", Toast.LENGTH_SHORT).show();
                gotoUrl("https://redple.dorik.io/privacy-policy");
                break;


            case R.id.nav_Share:

//                Toast.makeText(this, "share clicked", Toast.LENGTH_SHORT).show();
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "Hello there, Checkout this amazing Wallpaper App : https://play.google.com/store/apps/details?id=com.redple.walzo ";
                String shareSub = "Walzo by Redple";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSub);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share Walzo using"));
                break;
        }

        return true;
    }

    private void gotoUrl(String s) {
        Uri uri = Uri.parse(s);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    private void suggestedItems() {
        topMostRecyclerView.setHasFixedSize(true);
        topMostRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        suggestedModels.add(new SuggestedModel(R.drawable.all_back, "Popular"));
        suggestedModels.add(new SuggestedModel(R.drawable.nature_back, "Nature"));
        suggestedModels.add(new SuggestedModel(R.drawable.architecture_back, "Cars"));
        suggestedModels.add(new SuggestedModel(R.drawable.people_back, "People"));
        suggestedModels.add(new SuggestedModel(R.drawable.business_back, "Business"));
        suggestedModels.add(new SuggestedModel(R.drawable.health_back, "Health"));
        suggestedModels.add(new SuggestedModel(R.drawable.fashion_back, "Fashion"));
        suggestedModels.add(new SuggestedModel(R.drawable.film_back, "Film"));
        suggestedModels.add(new SuggestedModel(R.drawable.travel_back, "Travel"));

        adapter = new SuggestedAdapter(suggestedModels, MainActivity.this);
        topMostRecyclerView.setAdapter(adapter);

    }

    private void fetchWallpaper() {
//      fetch image url and name from the pexels api

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        progressBar.setVisibility(View.GONE);

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray jsonArray = jsonObject.getJSONArray("photos");

                            int length = jsonArray.length();

                            for (int i = 0; i < length; i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                int id = object.getInt("id");


                                JSONObject objectImage = object.getJSONObject("src");
                                String originalUrl = objectImage.getString("original");
                                String mediumUrl = objectImage.getString("medium");

                                WallpaperModel wallpaperModel = new WallpaperModel(id, originalUrl, mediumUrl);
                                wallpaperModelList.add(wallpaperModel);
                            }

                            wallpaperAdapter.notifyDataSetChanged();
                            pageNumber++;

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("Authorization", "563492ad6f9170000100000109d7e60a736c42e09a6b60a10cc62b35");
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(request);

    }

    @Override
    public void onItemClick(int position) {
        progressBar.setVisibility(View.VISIBLE);
        if (position == 0) {
            replaceTitle.setText("Popular");
            url = "https://api.pexels.com/v1/search/?page=" + pageNumber + "&per_page=80&query=popular";
            wallpaperModelList.clear();
            fetchWallpaper();
            progressBar.setVisibility(View.GONE);
        } else if (position == 1) {
            replaceTitle.setText("Nature");
            url = "https://api.pexels.com/v1/search/?page" + pageNumber + "&per_page=80&query=nature";
            wallpaperModelList.clear();
            fetchWallpaper();
            progressBar.setVisibility(View.GONE);
        } else if (position == 2) {
            replaceTitle.setText("Cars");
            url = "https://api.pexels.com/v1/search/?page=" + pageNumber + "&per_page=80&query=cars";
            wallpaperModelList.clear();
            fetchWallpaper();
            progressBar.setVisibility(View.GONE);
        } else if (position == 3) {
            replaceTitle.setText("People");
            url = "https://api.pexels.com/v1/search/?page=" + pageNumber + "&per_page=80&query=people";
            wallpaperModelList.clear();
            fetchWallpaper();
            progressBar.setVisibility(View.GONE);
        } else if (position == 4) {
            replaceTitle.setText("Business");
            url = "https://api.pexels.com/v1/search/?page=" + pageNumber + "&per_page=80&query=business";
            wallpaperModelList.clear();
            fetchWallpaper();
            progressBar.setVisibility(View.GONE);
        } else if (position == 5) {
            replaceTitle.setText("Health");
            url = "https://api.pexels.com/v1/search/?page=" + pageNumber + "&per_page=80&query=health";
            wallpaperModelList.clear();
            fetchWallpaper();
            progressBar.setVisibility(View.GONE);
        } else if (position == 6) {
            replaceTitle.setText("Fashion");
            url = "https://api.pexels.com/v1/search/?page=" + pageNumber + "&per_page=80&query=fashion";
            wallpaperModelList.clear();
            fetchWallpaper();
            progressBar.setVisibility(View.GONE);
        } else if (position == 7) {
            replaceTitle.setText("Film");
            url = "https://api.pexels.com/v1/search/?page=" + pageNumber + "&per_page=80&query=film";
            wallpaperModelList.clear();
            fetchWallpaper();
            progressBar.setVisibility(View.GONE);
        } else if (position == 8) {
            replaceTitle.setText("Travel");
            url = "https://api.pexels.com/v1/search/?page=" + pageNumber + "&per_page=80&query=travel";
            wallpaperModelList.clear();
            fetchWallpaper();
            progressBar.setVisibility(View.GONE);
        }

//        App update

        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo result) {

                if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(result, AppUpdateType.IMMEDIATE, MainActivity.this, REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) ;
        Toast.makeText(this, "Start Download Update", Toast.LENGTH_SHORT).show();

        if (requestCode != REQUEST_CODE) {
            Log.d("walzo", "Update Failed" + resultCode);
        }


    }



}

