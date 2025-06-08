package com.digitalcompany.tumarsuperapp.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.digitalcompany.tumarsuperapp.db.dao.UserDao;
import com.digitalcompany.tumarsuperapp.db.entity.User;

@Database(entities = {User.class}, version = 1, exportSchema = false) // Укажите сущности и версию
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao(); // Абстрактный метод для получения DAO

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "tumar_super_app_db"; // Имя файла БД

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            // В реальном приложении избегайте .allowMainThreadQueries()
                            // Используйте фоновые потоки (например, Executors, AsyncTask, Coroutines)
                            // .allowMainThreadQueries() // Для простоты примера, разрешим запросы в главном потоке
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}