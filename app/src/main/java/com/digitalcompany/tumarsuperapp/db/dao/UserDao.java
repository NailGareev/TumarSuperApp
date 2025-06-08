package com.digitalcompany.tumarsuperapp.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.digitalcompany.tumarsuperapp.db.entity.User;
import java.util.List;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users") // Запрос для получения всех пользователей
    List<User> getAll();

    @Query("SELECT * FROM users WHERE uid = :userId LIMIT 1") // Запрос для получения пользователя по ID
    User findById(int userId);

    @Insert // Метод для вставки пользователя
    void insertUser(User user);


}