package com.example.vocabularybot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity(name = "users_data_table")
public class User {

    @Id
    private Long chatId;

    private String firstName;
    private String lastName;
    private String userName;
//    private TimeStamp registeredAt;
    private Integer recentWord;
    @Getter
    @Column(name = "right_answers")
    private Integer rightAnswers;
    @Getter
    @Column(name = "answers")
    private Integer answers;
    @Getter
    private int wrongworditer;
    private int favoriteworditer;

    public void setFavoriteworditer(int favoriteworditer) {
        this.favoriteworditer = favoriteworditer;
    }

    public int getFavoriteworditer() {
        return favoriteworditer;
    }

    public void setWrongworditer(int wrongwordister) {
        this.wrongworditer = wrongwordister;
    }

    public void setAnswers(Integer answers) {
        this.answers = answers;
    }

    public void setRightAnswers(Integer rightAnswers) {
        this.rightAnswers = rightAnswers;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
/*
    public TimeStamp getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(TimeStamp registeredAt) {
        this.registeredAt = registeredAt;
    }
*/
    public Integer getRecentWord() {
        return recentWord;
    }

    public void setRecentWord(Integer recentWord) {
        this.recentWord = recentWord;
    }

    @Override
    public String toString() {
        return "User{" +
                "chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
               /* ", registeredAt=" + registeredAt +*/
                ", recentWord=" + recentWord +
                '}';
    }
}
