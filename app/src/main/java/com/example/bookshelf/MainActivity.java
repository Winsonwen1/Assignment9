package com.example.bookshelf;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import edu.temple.audiobookplayer.AudiobookService;
import edu.temple.audiobookplayer.AudiobookService.MediaControlBinder;

public class MainActivity extends AppCompatActivity implements BookListFragment.BookSelectedInterface, BookDetailsFragment.RouterInter {

    private static final String BOOKS_KEY = "books";
    private static final String SELECTED_BOOK_KEY = "selectedBook";
    private static final String TAG = "MainActivity";

    FragmentManager fm;

    boolean twoPane;
    BookListFragment bookListFragment;
    BookDetailsFragment bookDetailsFragment;
    private AudiobookService.MediaControlBinder mediaControlBinder;
    private Intent serviceIntent;

    ArrayList<Book> books;
    RequestQueue requestQueue;
    Book selectedBook;

    EditText searchEditText;

    private final String SEARCH_API = "https://kamorris.com/lab/abp/booksearch.php?search=";
    private static Handler handler;


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaControlBinder = (AudiobookService.MediaControlBinder) service;
            Log.d(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEditText = findViewById(R.id.searchEditText);

        /*
        Perform a search
         */
        findViewById(R.id.searchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchBooks(searchEditText.getText().toString());
            }
        });

        findViewById(R.id.pauseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaControlBinder.pause();

            }
        });

        findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaControlBinder.isPlaying()) {
                    mediaControlBinder.stop();
                    ((SeekBar) findViewById(R.id.seekBar)).setProgress(0);
                }
            }
        });

        ((SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser==true){
                    mediaControlBinder.seekTo((int) (((double) progress / 200) * selectedBook.getLength()));
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        serviceIntent = new Intent(this, AudiobookService.class);
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);

        /*
        If we previously saved a book search and/or selected a book, then use that
        information to set up the necessary instance variables
         */
        if (savedInstanceState != null) {
            books = savedInstanceState.getParcelableArrayList(BOOKS_KEY);
            selectedBook = savedInstanceState.getParcelable(SELECTED_BOOK_KEY);
            if (savedInstanceState.containsKey("MEDIA")) {
                this.mediaControlBinder = ((ArrayList<MediaControlBinder>) (savedInstanceState.getSerializable("MEDIA"))).get(0);
            }
            if (savedInstanceState.containsKey("HAND")) {
                handler = new Handler() {
                    public void handleMessage(Message msg) {
                        AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) msg.obj;
                        if (bookProgress == null) {

                        } else {
                            System.out.println(bookProgress.getProgress());
                            int progress =  ((int)(bookProgress.getProgress()*200) / selectedBook.getLength()) ;
                            System.out.println(progress);
                            ((SeekBar) findViewById(R.id.seekBar)).setProgress(progress);

                        }
                        return;
                    }

                };
            }
        } else {
            books = new ArrayList<Book>();
            handler = new Handler() {
                public void handleMessage(Message msg) {
                    AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) msg.obj;
                    if (bookProgress == null) {

                    } else {
                        System.out.println(bookProgress.getProgress());
                        int progress =  ((int)(bookProgress.getProgress()*200) / selectedBook.getLength()) ;
                        System.out.println(progress);
                        ((SeekBar) findViewById(R.id.seekBar)).setProgress(progress);

                    }
                    return;
                }

            };


        }
        twoPane = findViewById(R.id.container2) != null;
        fm = getSupportFragmentManager();

        requestQueue = Volley.newRequestQueue(this);

        /*
        Get an instance of BookListFragment with an empty list of books
        if we didn't previously do a search, or use the previous list of
        books if we had previously performed a search
         */
        bookListFragment = BookListFragment.newInstance(books);

        fm.beginTransaction()
                .replace(R.id.container1, bookListFragment)
                .commit();

        /*
        If we have two containers available, load a single instance
        of BookDetailsFragment to display all selected books.

        If a book was previously selected, show that book in the book details fragment
        *NOTE* we could have simplified this to a single line by having the
        fragment's newInstance() method ignore a null reference, but this way allow
        us to limit the amount of things we have to change in the Fragment's implementation.
         */
        if (twoPane) {
            if (selectedBook != null)
                bookDetailsFragment = BookDetailsFragment.newInstance(selectedBook, mediaControlBinder, handler);
            else
                bookDetailsFragment = new BookDetailsFragment();

            fm.beginTransaction()
                    .replace(R.id.container2, bookDetailsFragment)
                    .commit();
        } else {
            if (selectedBook != null) {
                fm.beginTransaction()
                        .replace(R.id.container1, BookDetailsFragment.newInstance(selectedBook, mediaControlBinder, handler))
                        // Transaction is reversible
                        .addToBackStack(null)
                        .commit();
            }
        }

//        if (selectedBook == null) {
//            findViewById(R.id.pauseButton).setVisibility(View.INVISIBLE);
//            findViewById(R.id.stopButton).setVisibility(View.INVISIBLE);
//
//        }
    }


    /*
    Fetch a set of "books" from from the web service API
     */
    private void fetchBooks(String searchString) {
        /*
        A Volloy JSONArrayRequest will automatically convert a JSON Array response from
        a web server to an Android JSONArray object
         */
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(SEARCH_API + searchString, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                if (response.length() > 0) {
                    books.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject bookJSON;
                            bookJSON = response.getJSONObject(i);
                            books.add(new Book(bookJSON.getInt(Book.JSON_ID),
                                    bookJSON.getString(Book.JSON_TITLE),
                                    bookJSON.getString(Book.JSON_AUTHOR),
                                    bookJSON.getString(Book.JSON_COVER_URL),
                                    bookJSON.getInt(Book.JSON_BOOK_DURATION)));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    updateBooksDisplay();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.search_error_message), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        requestQueue.add(jsonArrayRequest);
    }

    ;

    private void updateBooksDisplay() {
        /*
        Remove the BookDetailsFragment from the container after a search
        if it is the currently attached fragment
         */
        if (fm.findFragmentById(R.id.container1) instanceof BookDetailsFragment)
            fm.popBackStack();
        bookListFragment.updateBooksDisplay(books);
    }

    @Override
    public void bookSelected(int index) {
        selectedBook = books.get(index);
        if (twoPane)
            /*
            Display selected book using previously attached fragment
             */
            bookDetailsFragment.displayBook(selectedBook, mediaControlBinder, handler);
        else {
            /*
            Display book using new fragment
             */
            fm.beginTransaction()
                    .replace(R.id.container1, BookDetailsFragment.newInstance(selectedBook, mediaControlBinder, handler))
                    // Transaction is reversible
                    .addToBackStack(null)
                    .commit();
        }

//        findViewById(R.id.pauseButton).setVisibility(View.VISIBLE);
//        findViewById(R.id.stopButton).setVisibility(View.VISIBLE);


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save previously searched books as well as selected book
        outState.putParcelableArrayList(BOOKS_KEY, books);
        outState.putParcelable(SELECTED_BOOK_KEY, selectedBook);

        ArrayList<MediaControlBinder> save3 = new ArrayList<MediaControlBinder>();
        if (bookListFragment != null) {
            save3.add(mediaControlBinder);
            outState.putSerializable("MEDIA", save3);
        }
        ArrayList<Handler> save = new ArrayList<Handler>();
        if (handler != null) {
            save.add(handler);
            outState.putSerializable("HAND", save);
        }
    }

    @Override
    public void routed(Book book1) {
        if (mediaControlBinder != null) {
            if (mediaControlBinder.isPlaying()) {
                mediaControlBinder.stop();
            }
        }

        mediaControlBinder.play(book1.getId());
        mediaControlBinder.setProgressHandler(handler);
    }

}
