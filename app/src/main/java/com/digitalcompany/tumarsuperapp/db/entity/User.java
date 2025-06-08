package com.digitalcompany.tumarsuperapp.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users") // Название таблицы
public class User {

    @PrimaryKey(autoGenerate = true) // Автоматически генерируемый ID
    public int uid;

    @ColumnInfo(name = "first_name") // Имя колонки для имени
    public String firstName;

    @ColumnInfo(name = "last_name") // Имя колонки для фамилии
    public String lastName;

    // Пустой конструктор (требуется для Room)
    public User() {}

    // Конструктор для удобного создания объекта
    public User(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}