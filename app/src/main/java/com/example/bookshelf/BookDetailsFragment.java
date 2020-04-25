package com.example.bookshelf;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import edu.temple.audiobookplayer.AudiobookService;


public class BookDetailsFragment extends Fragment {

    private static final String BOOK_KEY = "book";
    private Book book;
    private AudiobookService.MediaControlBinder mediaControlBinder;
    private RouterInter parentActivity;

    TextView titleTextView, authorTextView;
    ImageView coverImageView;
    View v;
    Handler handler;

    public BookDetailsFragment() {
    }

    public static BookDetailsFragment newInstance(Book book, AudiobookService.MediaControlBinder mediaControlBinder, Handler handler) {
        BookDetailsFragment fragment = new BookDetailsFragment();
        Bundle args = new Bundle();

        /*
         Our Book class implements the Parcelable interface
         therefore we can place one inside a bundle
         by using that put() method.
         */
        ArrayList<AudiobookService.MediaControlBinder> save3 = new ArrayList<AudiobookService.MediaControlBinder>();
        save3.add(mediaControlBinder);
        ArrayList<Handler> save = new ArrayList<Handler>();
        save.add(handler);
        args.putSerializable("HAND",save);
        args.putSerializable("DATAB", save3);
        args.putParcelable(BOOK_KEY, book);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        /*
         This fragment needs to communicate with its parent activity
         so we verify that the activity implemented our known interface
         */
        if (context instanceof RouterInter) {
            parentActivity = (RouterInter) context;
        } else {
            throw new RuntimeException("Please implement the required interface(s)");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            book = (Book) getArguments().getParcelable(BOOK_KEY);
            mediaControlBinder = (AudiobookService.MediaControlBinder) ((ArrayList) getArguments().getSerializable("DATAB")).get(0);
            handler = (Handler) ((ArrayList) getArguments().getSerializable("HAND")).get(0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_book_details, container, false);

        titleTextView = v.findViewById(R.id.titleTextView);
        authorTextView = v.findViewById(R.id.authorTextView);
        coverImageView = v.findViewById(R.id.coverImageView);
//        if (book == null) {
////            v.findViewById(R.id.playButton).setVisibility(View.INVISIBLE);
//        }

        /*
        Because this fragment can be created with or without
        a book to display when attached, we need to make sure
        we don't try to display a book if one isn't provided
         */

        v.findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parentActivity.routed(book);

            }
        });

        if (book != null) {
            displayBook(book, mediaControlBinder, handler);

        }
        return v;
    }

    /*
    This method is used both internally and externally (from the activity)
    to display a book
     */
    public void displayBook(Book book, AudiobookService.MediaControlBinder mediaControlBinder, Handler handler) {
        titleTextView.setText(book.getTitle());
        authorTextView.setText(book.getAuthor());
        this.mediaControlBinder = mediaControlBinder;
        this.book = book;
        this.handler = handler;

        // Picasso simplifies image loading from the web.
//        v.findViewById(R.id.playButton).setVisibility(View.VISIBLE);
        // No need to download separately.
        Picasso.get().load(book.getCoverUrl()).into(coverImageView);
    }

    interface RouterInter {
        void routed(Book book1);
    }
}
