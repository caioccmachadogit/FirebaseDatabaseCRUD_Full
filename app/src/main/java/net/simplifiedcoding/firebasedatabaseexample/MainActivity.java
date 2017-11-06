package net.simplifiedcoding.firebasedatabaseexample;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String ARTIST_NAME = "net.simplifiedcoding.firebasedatabaseexample.artistname";
    public static final String ARTIST_ID = "net.simplifiedcoding.firebasedatabaseexample.artistid";

    EditText editTextName;
    Spinner spinnerGenre;
    Button buttonAddArtist;
    ListView listViewArtists;

    //a list to store all the artist from firebase database
    List<Artist> artists;

    //our database reference object
    DatabaseReference databaseArtists;
    private FirebaseDatabase mFirebaseInstance;
    private ValueEventListener addValueEventListener;
    private EditText editTextFind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseInstance = FirebaseDatabase.getInstance();
        //habilitar persistencia off
        mFirebaseInstance.setPersistenceEnabled(true);

        //getting the reference of artists node
        databaseArtists = mFirebaseInstance.getReference("artists");

        //getting views
        editTextName = (EditText) findViewById(R.id.editTextName);
        spinnerGenre = (Spinner) findViewById(R.id.spinnerGenres);
        editTextFind = (EditText) findViewById(R.id.editTextFind);
        editTextFind.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                Query queryFind = databaseArtists.orderByChild("artistName").equalTo(editTextFind.getText().toString());
//                Query queryFind = databaseArtists.orderByChild("artistName").startAt(editTextFind.getText().toString());
                queryFind.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        for (DataSnapshot data: dataSnapshot.getChildren()) {
//                            Artist value = data.getValue(Artist.class);
//                            Log.d("findItem", value.getArtistName()+"-"+value.getArtistGenre());
//                            Toast.makeText(getApplicationContext(), "Find: "+ value.getArtistName()+"-"+value.getArtistGenre(),Toast.LENGTH_SHORT).show();
//                        }
//
                        Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                        Iterator<DataSnapshot> iterator = snapshotIterator.iterator();
                        while((iterator.hasNext())){
                            Artist value = iterator.next().getValue(Artist.class);
                            Log.d("findItem", value.getArtistName()+"-"+value.getArtistGenre());
                            Toast.makeText(getApplicationContext(), "Find: "+ value.getArtistName()+"-"+value.getArtistGenre(),Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                hideSoftKeyBoard();
                return false;
            }
        });
        listViewArtists = (ListView) findViewById(R.id.listViewArtists);

        buttonAddArtist = (Button) findViewById(R.id.buttonAddArtist);

        //list to store artists
        artists = new ArrayList<>();


        //adding an onclicklistener to button
        buttonAddArtist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //calling the method addArtist()
                //the method is defined below
                //this method is actually performing the write operation
                addArtist();

//                addValueEventListener.onDataChange();
            }
        });

        //attaching listener to listview
        listViewArtists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //getting the selected artist
                Artist artist = artists.get(i);

                //creating an intent
                Intent intent = new Intent(getApplicationContext(), ArtistActivity.class);

                //putting artist name and id to intent
                intent.putExtra(ARTIST_ID, artist.getArtistId());
                intent.putExtra(ARTIST_NAME, artist.getArtistName());

                //starting the activity with intent
                startActivity(intent);
            }
        });

        listViewArtists.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Artist artist = artists.get(i);
                showUpdateDeleteDialog(artist.getArtistId(), artist.getArtistName());
                return true;
            }
        });


    }

    private void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if(imm.isAcceptingText()) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void showUpdateDeleteDialog(final String artistId, String artistName) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.update_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText editTextName = (EditText) dialogView.findViewById(R.id.editTextName);
        final Spinner spinnerGenre = (Spinner) dialogView.findViewById(R.id.spinnerGenres);
        final Button buttonUpdate = (Button) dialogView.findViewById(R.id.buttonUpdateArtist);
        final Button buttonDelete = (Button) dialogView.findViewById(R.id.buttonDeleteArtist);

        dialogBuilder.setTitle(artistName);
        final AlertDialog b = dialogBuilder.create();
        b.show();


        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = editTextName.getText().toString().trim();
                String genre = spinnerGenre.getSelectedItem().toString();
                if (!TextUtils.isEmpty(name)) {
                    updateArtist(artistId, name, genre);
                    b.dismiss();
                }
            }
        });


        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                deleteArtist(artistId);
                b.dismiss();
            }
        });
    }

    private boolean updateArtist(String id, String name, String genre) {
        //getting the specified artist reference
        DatabaseReference dR = FirebaseDatabase.getInstance().getReference("artists").child(id);

        //updating artist
        Artist artist = new Artist(id, name, genre);
        dR.setValue(artist);
        Toast.makeText(getApplicationContext(), "Artist Updated", Toast.LENGTH_LONG).show();
        return true;
    }

    private boolean deleteArtist(String id) {
        //getting the specified artist reference
        DatabaseReference dR = FirebaseDatabase.getInstance().getReference("artists").child(id);

        //removing artist
        dR.removeValue();

        //getting the tracks reference for the specified artist
        DatabaseReference drTracks = FirebaseDatabase.getInstance().getReference("tracks").child(id);

        //removing all tracks
        drTracks.removeValue();
        Toast.makeText(getApplicationContext(), "Artist Deleted", Toast.LENGTH_LONG).show();

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadArtistsFirebase();
    }

    private void loadArtistsFirebase() {
        //attaching value event listener
        databaseArtists.addValueEventListener(getAddValueEventListener());
    }

    private ValueEventListener getAddValueEventListener() {
        addValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("onDataChange", String.valueOf(dataSnapshot.getChildrenCount()));

                //clearing the previous artist list
                artists.clear();

                //iterating through all the nodes
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    //getting artist
                    Artist artist = postSnapshot.getValue(Artist.class);
                    //adding artist to the list
                    artists.add(artist);
                }

                //creating adapter
                ArtistList artistAdapter = new ArtistList(MainActivity.this, artists);
                //attaching adapter to the listview
                listViewArtists.setAdapter(artistAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        return addValueEventListener;
    }


    /*
    * This method is saving a new artist to the
    * Firebase Realtime Database
    * */
    private void addArtist() {
        //getting the values to save
        String name = editTextName.getText().toString().trim();
        String genre = spinnerGenre.getSelectedItem().toString();

        //checking if the value is provided
        if (!TextUtils.isEmpty(name)) {

            //getting a unique id using push().getKey() method
            //it will create a unique id and we will use it as the Primary Key for our Artist
            String id = databaseArtists.push().getKey();

            //creating an Artist Object
            Artist artist = new Artist(id, name, genre);

            //Saving the Artist
            databaseArtists.child(id).setValue(artist);

            //setting edittext to blank again
            editTextName.setText("");

            //displaying a success toast
            Toast.makeText(this, "Artist added", Toast.LENGTH_LONG).show();
        } else {
            //if the value is not given displaying a toast
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_LONG).show();
        }
    }
}
