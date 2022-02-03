package com.example.birdsofafeather.models.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface CourseDao {
    @Transaction
    @Query("SELECT * FROM courses WHERE student_id=:studentId")
    List<Course> getForStudent(int studentId);

    @Query("SELECT * FROM courses WHERE id=:id")
    Course get(int id);

    @Insert
    void insert(Course course);

    @Delete
    void delete(Course course);

    @Query("SELECT COUNT(*) FROM courses")
    int count();
}