package com.example.birdsofafeather;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.birdsofafeather.models.db.AppDatabase;
import com.example.birdsofafeather.models.db.StudentWithCourses;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ViewProfileActivity extends AppCompatActivity {
    private NearbyBackgroundService nearbyService;
    private boolean isBound;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        // called when connection to NearbyBackgroundService has been established
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            NearbyBackgroundService.NearbyBinder nearbyBinder = (NearbyBackgroundService.NearbyBinder)iBinder;
            nearbyService = nearbyBinder.getService();
            isBound = true;
        }

        // - called when connection to NearbyBackgroundService has been lost
        // - does not remove binding; can still receive call to onServiceConnected
        //   when service is running
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        Intent intent = new Intent(this, NearbyBackgroundService.class);
        intent.putExtra("uuid", new UUIDManager(getApplicationContext()).getUserUUID());
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        isBound = true;

        // Retrieve Student from database to put on ProfileView
        Bundle extras = getIntent().getExtras();
        String classmate_id = extras.getString("classmate_id");
        AppDatabase db = AppDatabase.singleton(getApplicationContext());
        StudentWithCourses student = db.studentWithCoursesDao().get(classmate_id);

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
                        waveCheck.setVisibility(View.GONE);
                    }
                }
        );

        //send wave to student
        if(!student.isFavorite()){
            waveCheck.setVisibility(View.GONE);
        }

        boolean isWavedTo = student.getWavedFromUser();
        waveCheck.setEnabled(!isWavedTo);
        waveCheck.setChecked(isWavedTo);
        waveCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (buttonView.isChecked()) {
                        Toast.makeText(ViewProfileActivity.this, "Send Wave", Toast.LENGTH_SHORT).show();
                        db.studentWithCoursesDao().updateWaveFrom(student.getUUID(), true);
                        student.setWavedFromUser(true);

                        //send wave service
                        String studentInfo = sendWaveService(student);
                        nearbyService.publish(studentInfo);

                        //disable checkbox
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
        String currentUserID = new UUIDManager(getApplicationContext()).getUserUUID();

        StudentWithCourses me = db.studentWithCoursesDao().get(currentUserID);
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

    protected String sendWaveService(StudentWithCourses student){
        //obtain strings to input into service
      //  String userUUID = new UUIDManager(getApplicationContext()).getUserUUID();
        String studentInfo = student.getUUID() + ",,,,\n" +
                student.getName() + ",,,,\n" +
                student.getHeadshotURL() + ",,,,\n";
        for(String course: student.getCourses()){
            String temp = course.replace(" ", ",");
            studentInfo += temp + ",\n";
        }
        studentInfo += student.getUUID() + ",wave,,,";

        return studentInfo;
    }

    // make sure to stop service when ending activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound)  {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}