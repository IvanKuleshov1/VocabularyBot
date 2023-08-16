package com.example.vocabularybot.model;

import org.springframework.data.repository.CrudRepository;

public interface WordsRepository extends CrudRepository<Words, Long> {
}
