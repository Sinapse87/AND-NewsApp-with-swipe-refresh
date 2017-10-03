package com.example.android.newsapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

//TODO: Save search bar input: Throws NullPointerException, HELP!(l. 410)

public class NewsFeedActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<News>>{

    private ListView listNewsfeed;
    private NewsAdapter mAdapter;
    private ArrayList<News> newsArrayList = new ArrayList<News>();
    private ProgressBar progressBar;
    private TextView infoText;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private static final int NEWS_LOADER_ID = 1;


    /** Save Strings for InstanceState */
    private static final String SAVE_VIEW_INDEX = "SAVE_VIEW_INDEX";
    private static final String SAVE_VIEW_TOP = "SAVE_VIEW_TOP";
    private static final String SAVE_ARRAY_LIST = "SAVE_ARRAY_LIST";
    private static final String SAVE_PAGE = "SAVE_PAGE";
    private static final String SAVE_PROGRESSBAR = "SAVE_PROGRESSBAR";
    private static final String SAVE_INFOTEXT = "SAVE_INFOTEXT";
    private static final String SAVE_QUERY_PARAMETER = "SAVE_QUERY_PARAMETER";

    /** Url parts for API request */
    private static final String THEGUARDIAN_REQUEST_URL = "https://content.guardianapis.com/";
    private static final String THEGUARDIAN_SEARCH_PATH = "search";
    private static final String THEGUARDIAN_PAGE = "Page";
    private static final String THEGUARDIAN_QUERY_REQUEST = "Query request";
    private static final String THEGUARDIAN_REQUEST_URL_END = "&show-fields=trailText%2Cbyline%2Cthumbnail&api-key=f542786a-b102-48ab-8b7d-1aa76d1b8e0b";

    /** Starting value for page query for new requests */
    private static final int PAGE_NUMBER_START = 1;
    /** Page count for the URL */
    private int pageNumberInQuery = PAGE_NUMBER_START;
    /** Will be checked on onLoadMore() method */
    private boolean sentNoConnectionInfo = false;

    private static final String LOG_TAG = "Results: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newsfeed);

        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        listNewsfeed = (ListView) findViewById(R.id.list_item_newsfeed);
        progressBar = (ProgressBar) findViewById(R.id.list_progressBar);
        infoText = (TextView) findViewById(R.id.list_emptyText);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.list_swiperefresh);

        mAdapter = new NewsAdapter(this, newsArrayList);

        listNewsfeed.setAdapter(mAdapter);

        if(savedInstanceState != null){
            pageNumberInQuery = savedInstanceState.getInt(THEGUARDIAN_PAGE);
            newsArrayList = savedInstanceState.getParcelableArrayList(SAVE_ARRAY_LIST);

            int index = savedInstanceState.getInt(SAVE_VIEW_INDEX);
            int top = savedInstanceState.getInt(SAVE_VIEW_TOP);
            listNewsfeed.setSelectionFromTop(index, top);

            if(networkInfo != null && networkInfo.isConnected()){

                listNewsfeed.setOnScrollListener(new EndlessScrollListener(pageNumberInQuery) {
                    @Override
                    public boolean onLoadMore(int page, int totalItemsCount) {
                        NetworkInfo networkOnLoadMore = connectivityManager.getActiveNetworkInfo();
                        if(networkOnLoadMore != null && networkOnLoadMore.isConnected()) {
                            pageNumberInQuery = page;

                            initiateLoader(page, new Bundle());

                            return true;
                        }else{
                            /** Send user info about no connection only once, otherwise it will be displayed nonstop on the lower list items. */
                            if(sentNoConnectionInfo == false) {
                                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_connection_toast), Toast.LENGTH_LONG);
                                toast.show();
                                sentNoConnectionInfo = true;
                            }
                            return false;
                        }
                    }
                });

                initiateLoader(pageNumberInQuery, new Bundle());

            }
        /** Gets called, when the App is started */
        }else{

            if(networkInfo != null && networkInfo.isConnected()){

                listNewsfeed.setOnScrollListener(new EndlessScrollListener() {
                    @Override
                    public boolean onLoadMore(int page, int totalItemsCount) {
                        NetworkInfo networkOnLoadMore = connectivityManager.getActiveNetworkInfo();
                        if(networkOnLoadMore != null && networkOnLoadMore.isConnected()) {
                            pageNumberInQuery = page;

                            initiateLoader(page, new Bundle());

                            return true;
                        }else{
                            if(sentNoConnectionInfo == false) {
                                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_connection_toast), Toast.LENGTH_SHORT);
                                toast.show();
                                sentNoConnectionInfo = true;
                            }
                            return false;
                        }

                    }
                });

                initiateLoader(pageNumberInQuery, new Bundle());
            }
        }

        if(networkInfo == null){
            progressBar.setVisibility(View.GONE);
            infoText.setText(R.string.no_connection);
        }

        /** Implements refresh method on swipe down */
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                NetworkInfo networkRefresh = connectivityManager.getActiveNetworkInfo();

                /** If progress bar is visible: Loading process ongoing: do nothing */
                if (progressBar.getVisibility() == View.VISIBLE) {
                    swipeRefreshLayout.setRefreshing(false);

                /** Start refreshing when connection available */
                }else if(networkRefresh != null && networkRefresh.isConnected()) {
                    /** Clears the info text when refreshing */
                    infoText.setText("");
                    initiateLoader(PAGE_NUMBER_START, new Bundle());

                /** No connection available: Give feedback to user */
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_connection_toast), Toast.LENGTH_SHORT);
                    toast.show();
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

    }

    @Override
    public Loader<List<News>> onCreateLoader(int id, Bundle args) {

        /** Retrieves the set preferences of the user to modify the url request */
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.setting_order_by_default));

        String section = sharedPrefs.getString(
                getString(R.string.settings_category_key),
                getString(R.string.settings_category_default));

        /** Get the page number
         * @param page: equals 1 if new request is started or equals the last page number + 1 */
        int page = args.getInt(THEGUARDIAN_PAGE);
        String pageString = String.valueOf(page);

        Uri baseUri = Uri.parse(THEGUARDIAN_REQUEST_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        /** (1) Checks if user got a individual search: If so, add needed query tag
         * (2) If no user request is started: Check if category 'all' is selected: Append 'search' tag
         * (3) For all other categories append category path */
        if(args.getString(THEGUARDIAN_QUERY_REQUEST) != null){
            uriBuilder.appendPath(THEGUARDIAN_SEARCH_PATH);
            String queryUser = args.getString(THEGUARDIAN_QUERY_REQUEST);
            uriBuilder.appendQueryParameter("q", queryUser);
        }else if(section.equals("all")){
            uriBuilder.appendPath(THEGUARDIAN_SEARCH_PATH);
        }else{
            uriBuilder.appendPath(section);
        }

        uriBuilder.appendQueryParameter("order-by", orderBy);
        uriBuilder.appendQueryParameter("page", pageString);

        String requestUrl = uriBuilder.toString() + THEGUARDIAN_REQUEST_URL_END;
        Log.i(LOG_TAG, "Request URL " + requestUrl);

        return new NewsLoader(this, requestUrl);
    }

    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> data) {
        progressBar.setVisibility(View.GONE);

        /** Checks if the result isn´t empty */
       if(data != null && !data.isEmpty()){
           /** Checks if the swipe refresh is used and needed
            * True: Swipe refresh is used and a current list exists
            * False: Swipe refresh isn´t used and current list is empty
            * False: Swipe refresh isn´t used and current list exists
            * False: Swipe refresh is used but current list is empty */
           if(swipeRefreshLayout.isRefreshing() && !mAdapter.isEmpty()){
               /** Checks if the newly loaded data is equal to the current newest item
                * (1) The current news are up-to-date, no refreshing of list needed
                * (2) There are recent news to add, refresh list*/
               News currentNews = mAdapter.getItem(0);
               News refreshedNews = data.get(0);
               if(currentNews.getTitle().equals(refreshedNews.getTitle())){
                   // (1) Inform the user that data is up to date
                   Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.up_to_date), Toast.LENGTH_SHORT);
                   toast.show();

               }else {
                   // (2) Refresh the list
                   mAdapter.clear();
                   mAdapter.addAll(data);
                   // Reset the page count
                   pageNumberInQuery = PAGE_NUMBER_START;
               }
           /** Executed on all other requests: Endless scrolling, user search request, changed category */
           }else {
               mAdapter.addAll(data);
           }
       }

       swipeRefreshLayout.setRefreshing(false);

        Log.i(LOG_TAG, "Loader finished");
    }

    @Override
    public void onLoaderReset(Loader<List<News>> loader) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /** Inflates the designed menu 'main.xml' */
        getMenuInflater().inflate(R.menu.main, menu);

        final MenuItem actionSearch = menu.findItem(R.id.action_search);
        searchView = (SearchView) actionSearch.getActionView();

        /** Listener for user specific search request */
        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Reset page count
                pageNumberInQuery = PAGE_NUMBER_START;

                // Create a new bundle to initiate the loader
                Bundle args = new Bundle();
                // Add user request as string to the bundle
                args.putString(THEGUARDIAN_QUERY_REQUEST, query);
                // Clear the current news
                mAdapter.clear();
                // Set the progressbar for the user visible again
                progressBar.setVisibility(View.VISIBLE);
                // Initiate the loader with the resetted page count and the filled bundle
                initiateLoader(pageNumberInQuery, args);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        };

        searchView.setOnQueryTextListener(queryTextListener);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /** If menu is clicked */
        int id = item.getItemId();
        if(id == R.id.action_settings){
            Intent settingIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingIntent);
            return true;

        }
        /** Refresh button for users without access to swipe down refresh option.
         * Simulates a swipe down event */
        if(id == R.id.action_refresh){
            swipeRefreshLayout.setRefreshing(true);
            initiateLoader(PAGE_NUMBER_START, new Bundle());
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method to restart the loader from different points in the app
     * @param queryParamPage: Get value for the query parameter 'page'
     * @param args: Bundle used for the Loader. Can be initialized for user requests
     */
    public void initiateLoader(int queryParamPage, Bundle args){
        /** Connection must have been established again, so set value on false again until connection is lost again. */
        sentNoConnectionInfo = false;

        args.putInt(THEGUARDIAN_PAGE, queryParamPage);

        getSupportLoaderManager().restartLoader(NEWS_LOADER_ID, args, this);

    }

    /** Called when device is rotated */
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        /** Record the state of the ListView */
        int index = listNewsfeed.getFirstVisiblePosition();
        // First list item
        View view = listNewsfeed.getChildAt(0);
        // Actual 'distance' between top and current list item
        int top;
        if(view == null){
            top = 0;
        }else{
            top = view.getTop() - listNewsfeed.getPaddingTop();
        }

        // In most cases a list will already be loaded when device is rotated
        boolean visibilityProgressbar = false;

        if(progressBar.getVisibility() == View.VISIBLE){
            visibilityProgressbar = true;
        }

        if(searchView.getQuery() != null){
            String userRequest = searchView.getQuery().toString();
            outState.putString(SAVE_QUERY_PARAMETER, userRequest);

        }


        /** Saves all necessary data for rotating the device */
        outState.putInt(SAVE_VIEW_INDEX, index);
        outState.putInt(SAVE_VIEW_TOP, top);
        // Save the current News Array to display it again on the rotated device
        outState.putParcelableArrayList(SAVE_ARRAY_LIST, newsArrayList);
        // Save the current page, so that the method onLoadMore works as intended after rotation
        outState.putInt(SAVE_PAGE, pageNumberInQuery);
        // Save the current visibility of the progressbar
        outState.putBoolean(SAVE_PROGRESSBAR, visibilityProgressbar);
        // Save the current text of the info text in the center of the screen
        outState.putString(SAVE_INFOTEXT, (String) infoText.getText());

        super.onSaveInstanceState(outState);
    }

    //TODO: Where does the program know which Bitmap refers  to each News object? I don´t save them, but it still works
    /** Called after device rotation */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        progressBar.setVisibility(View.GONE);

        /** If the savedInstanceState isn´t empty, recreate the list */
        if(savedInstanceState != null){

            /** Restores the current visibility of the progressbar */
            if(savedInstanceState.getBoolean(SAVE_PROGRESSBAR)){
                progressBar.setVisibility(View.VISIBLE);
            }

            if(savedInstanceState.getString(SAVE_INFOTEXT) != null){
                infoText.setText(savedInstanceState.getString(SAVE_INFOTEXT));
            }

//            Throws NullPointer exception, but I can´t figure out why. It´s initiated on the menu creation.
//            if(savedInstanceState.getString(SAVE_QUERY_PARAMETER) != null) {
//                String userQuery = savedInstanceState.getString(SAVE_QUERY_PARAMETER);
//                searchView.setQuery(userQuery, false);
//            }

            if(savedInstanceState.getParcelableArrayList(SAVE_ARRAY_LIST).isEmpty()){
                initiateLoader(savedInstanceState.getInt(SAVE_PAGE), new Bundle());
            }else {
                newsArrayList = savedInstanceState.getParcelableArrayList(SAVE_ARRAY_LIST);
                int index = savedInstanceState.getInt(SAVE_VIEW_INDEX);
                int top = savedInstanceState.getInt(SAVE_VIEW_TOP);

                mAdapter.addAll(newsArrayList);

                listNewsfeed.setSelectionFromTop(index, top);
            }

        }
    }
}
