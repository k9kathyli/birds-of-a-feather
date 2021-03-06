package com.example.birdsofafeather;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.birdsofafeather.models.db.AppDatabase;
import com.example.birdsofafeather.models.db.StudentWithCourses;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ViewProfileActivity extends AppCompatActivity {
    private MessageListener mMessageListener;
    private Message mMessage;
    private LoggedMessagesClient messagesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        String uuid = new UUIDManager(getApplicationContext()).getUserUUID();

        // Retrieve Student from database to put on ProfileView
        Bundle extras = getIntent().getExtras();
        String classmate_id = extras.getString("classmate_id");
        AppDatabase db = AppDatabase.singleton(getApplicationContext());
        StudentWithCourses student = db.studentWithCoursesDao().get(classmate_id);

        // get reference to me, the user
        StudentWithCourses me = db.studentWithCoursesDao().get(uuid);

        // build messageListener and Message
        mMessageListener = new NearbyMessagesFactory().build(uuid, this);
        mMessage = new NearbyMessagesFactory().buildMessage(me, classmate_id);

        // create a MessagesClient and subscribe so that messages will still be received while viewing profile
        messagesClient = new LoggedMessagesClient(Nearby.getMessagesClient(this));
        messagesClient.subscribe(mMessageListener);

        // Set profile name
        TextView nameView = findViewById(R.id.name_view);
        nameView.setText(student.getName());

        Picasso picasso = new Picasso.Builder(this).build();
        try {
            Picasso.setSingletonInstance(picasso);
        } catch (Exception e) {
        }

        CheckBox favoriteCheck =  findViewById(R.id.profile_favorite);
        CheckBox waveCheck = findViewById(R.id.send_wave);

        // Set favorite icon
        favoriteCheck.setChecked(student.isFavorite());
        favoriteCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (buttonView.isChecked()) {
                        Toast.makeText(ViewProfileActivity.this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                        db.studentWithCoursesDao().updateFavorite(student.getUUID(), true);

                        //wave visible if favorite
                        waveCheck.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(ViewProfileActivity.this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                        db.studentWithCoursesDao().updateFavorite(student.getUUID(), false);

                        //wave not visible if not favorite
                        if (!waveCheck.isChecked()) {
                            waveCheck.setVisibility(View.GONE);
                        }
                    }
                }
        );

        //case to handle: favorite -> wave -> unfavorite -> wave still visible
        if(!student.isFavorite() && !student.getWavedFromUser()){
            waveCheck.setVisibility(View.GONE);
        }

        //send wave to student
        boolean isWavedTo = student.getWavedFromUser();
        waveCheck.setEnabled(!isWavedTo);
        waveCheck.setChecked(isWavedTo);
        waveCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (buttonView.isChecked()) {
                        Toast.makeText(ViewProfileActivity.this, "Wave Sent!", Toast.LENGTH_SHORT).show();
                        db.studentWithCoursesDao().updateWaveFrom(student.getUUID(), true);
                        student.setWavedFromUser(true);

                        // publish wave message to Nearby Messages
                        messagesClient.publish(mMessage);

                        //wave cannot be unsent/sent again
                        waveCheck.setEnabled(false);
                    }
                }
        );

        // Retrieve profile image from URL using Picasso
        ImageView picture_view = (ImageView)findViewById(R.id.profile_picture_view);
        String url = student.getHeadshotURL();
        Picasso.get().load(url).into(picture_view);
        // Tag the image with its URL
        picture_view.setTag(url);

        // Compare other student with user's classes
        // The user is always the first entry in the database, so we use id 1
        List<String> cc = student.getCommonCourses(me);
        StringBuilder displayList = new StringBuilder();
        for (String course : cc){
            displayList.append(course);
            displayList.append("\n");
        }
        TextView common_courses = findViewById(R.id.common_classes_view);
        common_courses.setText(displayList.toString());
        common_courses.setVisibility(View.VISIBLE);
    }

    // make sure to stop service when ending activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // make sure to stop publishing wave message, and unsubscribe messageListener
        // from this context
        messagesClient.unpublish(mMessage);
        messagesClient.unsubscribe(mMessageListener);
    }
}