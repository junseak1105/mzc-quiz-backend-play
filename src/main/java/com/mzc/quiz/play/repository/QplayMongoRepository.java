package com.mzc.quiz.play.repository;

import com.mzc.quiz.play.entity.mongo.Show;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QplayMongoRepository extends MongoRepository<Show, String> {
    Show findShowById(String Id);
}
