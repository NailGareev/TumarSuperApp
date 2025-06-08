package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class RegistrationRequest {

    @SerializedName("firstName") // Имя поля в JSON должно совпадать с ожидаемым на бэкенде
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("age")
    private int age; // Используем int, если на бэкенде ожидается число

    @SerializedName("password")
    private String password;

    // Конструктор
    public RegistrationRequest(String firstName, String lastName, String email, String phone, int age, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.age = age;
        this.password = password;
    }

    // Геттеры (необязательны для Gson, но могут быть полезны)
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public int getAge() { return age; }
    public String getPassword() { return password; }
}