package com.example.vocabularybot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "words")
public class Words {

    @Id
    private Long wordid;

    private String engword;
    private String translation;
    private String example;
    //    private TimeStamp registeredAt;
//    @Column(name = "imagePath")
    private String imagePath;

    public Long getWordid() {
        return wordid;
    }

    public void setWordid(Long wordid) {
        this.wordid = wordid;
    }

    public String getEngword() {
        return engword;
    }

    public void setEngword(String engword) {
        this.engword = engword;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public String toString() {
        return "Words{" +
                "wordid=" + wordid +
                ", engword='" + engword + '\'' +
                ", translation='" + translation + '\'' +
                ", example='" + example + '\'' +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }
}
