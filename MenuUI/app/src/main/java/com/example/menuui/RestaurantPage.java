package com.example.menuui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.mashape.unirest.http.async.RequestThread;
import com.yelp.fusion.client.models.User;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RestaurantPage extends AppCompatActivity {
    // DrawerLayout for the nav menu
    private DrawerLayout mDrawerLayout;

    // Dialog for filter dishes button
    Dialog filterDialog;

    // dishes list for recycler views
    private List<Dish> popular_dishes;
    private List<Dish> dishes;

    // RestaurantPage info
    private JSONObject restaurant_info;
    private String restaurant_info_string;

    // Restaurant image url
    private String restaurant_image_url;
    private String restaurant_name;
    FetchMenu get_menu = null;

    private static Context appContext;
    public static Context getContext() {
        return appContext;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restaurant);
        appContext = this;

        // Retrieve data that was passed through intent
        try {
            restaurant_info = new JSONObject(getIntent().getStringExtra("RESTAURANT_INFO"));
            restaurant_info_string = getIntent().getStringExtra("RESTAURANT_INFO");
        } catch (Exception e) {
            Log.d("DEBUG", "ERROR: " + e.toString());
        }

        if (restaurant_info != null) {
            // get the menu dishes by scraping Zomato
            updateInfo();

            if (Looper.myLooper() == Looper.getMainLooper()) {
                Log.i("here", "im main");
                get_menu = new FetchMenu(this, restaurant_name);
                RequestThread get_request = new RequestThread();
                get_request.start();

                synchronized (get_request) {
                    try {
                        System.out.println("Waiting for request get to complete...");
                        get_request.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                DishThread get_dishes = new DishThread();
                get_dishes.start();

                synchronized (get_dishes) {
                    try {
                        System.out.println("Waiting for dish get to complete...");
                        get_dishes.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
            System.out.println("Trying to get here");
        }

        // handle nav bar implementation
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        actionbar.setTitle("Menu App");

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Update the UI based on the item selected
                        int id = menuItem.getItemId();
                        switch (id) {
                            case R.id.nav_homepage:
                                // send to the landing page
                                Intent home_intent = new Intent(RestaurantPage.this, Landing.class);
                                startActivity(home_intent);
                                break;
                            case R.id.nav_favorite_restaurants:
                                // send to favorite restaurants page
                                Intent fav_rest_page_intent = new Intent(RestaurantPage.this, FavoriteRestaurants.class);
                                startActivity(fav_rest_page_intent);
                                break;
                            case R.id.nav_logout:
                                // save the user favorites hashmap to the shared preferences
                                ((MenuApp)RestaurantPage.this.getApplication()).saveHashMap();
                                // save the reviews and ratings
                                ((MenuApp)RestaurantPage.this.getApplication()).saveReviews();
                                ((MenuApp)RestaurantPage.this.getApplication()).saveRatings();
                                // log out and send to the welcome page
                                Intent logout_intent = new Intent(RestaurantPage.this, MainActivity.class);
                                startActivity(logout_intent);
                                break;
                        }
                        return true;
                    }
                });

        // get the dish data
        if (dishes.size() != 0) {
            // there are menu dishes for this restaurant -- hide no results message
            TextView no_dish_tv = (TextView)findViewById(R.id.no_dishes_text);
            no_dish_tv.setVisibility(View.INVISIBLE);
            TextView pop_dishes_title = (TextView) findViewById(R.id.rest_pop_dish_title);
            pop_dishes_title.setVisibility(View.VISIBLE);
            TextView dishes_title = (TextView) findViewById(R.id.rest_dishes_title);
            dishes_title.setVisibility(View.VISIBLE);
            getDishData();
            //recycler view for most popular dishes - reuse the dish adapter
            RecyclerView popular_rv = (RecyclerView) findViewById(R.id.restaurant_popular_recycler);
            // linear layout manager for the popular dish recycler view
            LinearLayoutManager popular_llm = new LinearLayoutManager(this);
            popular_rv.setLayoutManager(popular_llm);
            // call the dish adapter on the popular dishes
            DishAdapter popular_dish_adapter = new DishAdapter(popular_dishes, restaurant_info_string, restaurant_name);
            popular_rv.setAdapter(popular_dish_adapter);


            // recycler view for dish cards
            RecyclerView menu_rv = (RecyclerView) findViewById(R.id.restaurant_dishes_recycler);
            // linear layout manager for the dish recycler view
            LinearLayoutManager llm = new LinearLayoutManager(this);
            menu_rv.setLayoutManager(llm);

            System.out.println("executed?");

            // call the dish adapter on the restaurant dishes
            DishAdapter adapter = new DishAdapter(dishes, restaurant_info_string, restaurant_name);
            menu_rv.setAdapter(adapter);

            // temp: set dish reviews
//            ((MenuApp)this.getApplication()).setReviews();
        }
        else {
            // no menu dishes for this restaurant
            // make no results message visible
            TextView no_dish_tv = (TextView)findViewById(R.id.no_dishes_text);
            no_dish_tv.setVisibility(View.VISIBLE);
            // hide popular dishes and dishes header
            TextView pop_dishes_title = (TextView) findViewById(R.id.rest_pop_dish_title);
            pop_dishes_title.setVisibility(View.INVISIBLE);
            TextView dishes_title = (TextView) findViewById(R.id.rest_dishes_title);
            dishes_title.setVisibility(View.INVISIBLE);
        }
    }

    // nav bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // add search bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.nav_bar_menu, menu);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setIconified(false);
        searchView.setQueryHint("Search for food, restaurants, ...");
        searchView.onActionViewExpanded();

        // Set listeners for UI Objects
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent search_intent = new Intent(RestaurantPage.this, SearchPage.class);
                search_intent.putExtra("SEARCH_INFO", query);
                startActivity(search_intent);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }


    // handle filter popup
    public void showFilterPopup(View view) {
        TextView close_txt;
        Button btn_filter_alpha;
        Button btn_filter_rating;
        filterDialog.setContentView(R.layout.filter_dishes_popup);
        close_txt = (TextView) filterDialog.findViewById(R.id.close_txt);
        btn_filter_alpha = (Button) filterDialog.findViewById(R.id.filter_dishes_alpha);
        btn_filter_rating = (Button) filterDialog.findViewById(R.id.filter_dishes_rating);
        close_txt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterDialog.dismiss();
            }
        });
        filterDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        filterDialog.show();
    }

    // get the dish data for this restaurant
    private void getDishData() {

//        // add dummy data reviews for each dish
//        // dummy reviews
//        Review review1 = new Review("This dish was excellent", 5, true, "User1");
//        Review review2 = new Review("This dish was delicious", 4, true, "User2");
//        Review review3 = new Review("This dish was mediocre", 3,  true, "User3");
//        // set dummy data reviews for each dish
//        for (int i = 0; i < dishes.size(); i++) {
//            Dish dish = dishes.get(i);
//            // check if there are already reviews for this dish
//            List<Review> dish_reviews = ((MenuApp)this.getApplication()).getDishReviews(dish.name);
//            if (dish_reviews.size() == 0) {
//                // if there are no reviews, add the 3 dummy reviews
//                ((MenuApp)this.getApplication()).addDishReview(dish.name, review1);
//                ((MenuApp)this.getApplication()).addDishReview(dish.name, review2);
//                ((MenuApp)this.getApplication()).addDishReview(dish.name, review3);
//            }
//        }

        // sort the dishes by rating
        Collections.sort(dishes, new Comparator<Dish>() {
            public int compare(Dish d1, Dish d2) {
                return d1.getDishRating() < d2.getDishRating() ? 1 : d1.getDishRating() > d2.getDishRating() ? -1 : 0;
            }
        });

        // get the top 3 rated dishes and add it to popular dishes (and remove it from the rest of the dishes)
        Dish dish1 = dishes.remove(0);
        Dish dish2 = dishes.remove(1);
        Dish dish3 = dishes.remove(2);
        popular_dishes = new ArrayList<>();
        popular_dishes.add(dish1);
        popular_dishes.add(dish2);
        popular_dishes.add(dish3);

        // sort the rest of the dishes alphabetically
        Collections.sort(dishes, new Comparator<Dish>() {
            public int compare(Dish d1, Dish d2) {
                return d1.getDishName().compareTo(d2.getDishName());
            }
        });
    }

    // Add restaurant to favorite restaurants
    public void addToFavorites(View view) {
        String user = ((MenuApp)this.getApplication()).getCurrentUser();
        ((MenuApp)this.getApplication()).addFavoriteRestaurant(user, restaurant_info_string);
        System.out.println("Adding to restaurant to favorites");
    }

    /**
     * Update the UI elements with restaurant info passed from the landing page
     */
    private void updateInfo() {
        TextView restaurant_title = findViewById(R.id.restaurant_title);
        TextView restaurant_location = findViewById(R.id.restaurant_location);
        TextView restaurant_phone = findViewById(R.id.restaurant_phone);
        ImageView restaurant_image = findViewById(R.id.restaurant_main_image);
        try {
            String location = restaurant_info.getString("street") + ", "
                    + restaurant_info.getString("city") + ", "
                    + restaurant_info.getString("state");
            restaurant_title.setText(restaurant_info.getString("name"));
            restaurant_name = restaurant_info.getString("name");
            restaurant_location.setText(location);
            restaurant_phone.setText(restaurant_info.getString("phone"));
            restaurant_image_url = restaurant_info.getString("image");
            new DownloadImageTask(restaurant_image).execute(restaurant_info.getString("image"));
        } catch (Exception e) {
            Log.d("DEBUG", "ERROR: Could not extract JSON from restaurant info");
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap mIcon11 = null;

            try {
                InputStream in = new java.net.URL(url).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("DEBUG", "Error: " + e.toString());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {

            bmImage.setImageBitmap(result);
        }
    }

    class RequestThread extends Thread {

        @Override
        public void run() {
            synchronized(this){
                get_menu.get_request();
                notify();

            }


        }
    }

    class DishThread extends Thread {

        @Override
        public void run() {
            synchronized (this) {
                dishes = get_menu.get_all_dishes();
                notify();
            }

        }

    }
}