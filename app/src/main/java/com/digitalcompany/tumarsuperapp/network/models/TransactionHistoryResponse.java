package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TransactionHistoryResponse {

    @SerializedName("success")
    private boolean success;

    // Поле "transactions" должно совпадать с ключом в JSON ответе от вашего бэкенда
    @SerializedName("transactions")
    private List<Transaction> transactions; // Список объектов Transaction

    // Геттеры
    public boolean isSuccess() {
        return success;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    @Override
    public String toString() {
        return "TransactionHistoryResponse{" +
                "success=" + success +
                ", transactionsCount=" + (transactions != null ? transactions.size() : 0) +
                '}';
    }
}