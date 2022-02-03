package com.example.birdsofafeather.models.db;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface StudentWithNotesDao {
   @Transaction
   @Query("SELECT * FROM students")
   List<StudentWithCourses> getAll();

   @Query("SELECT * FROM students WHERE id=:id")
   StudentWithCourses get(int id);

   @Query("SELECT COUNT(*) FROM students")
   int count();
}
