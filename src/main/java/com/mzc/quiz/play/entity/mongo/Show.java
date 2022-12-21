package com.mzc.quiz.play.entity.mongo;

import lombok.*;

import javax.persistence.Id;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Show {
    @Id
    private String id;
    private QuizInfo quizInfo;
    private List<Quiz> quizData;
}
