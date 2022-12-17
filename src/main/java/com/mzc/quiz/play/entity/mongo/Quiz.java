package com.mzc.quiz.play.entity.mongo;


import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Quiz {
    private int num;
    private String type;
    private String question;
    private Media media;
    private Choice choiceList;
    private List<String> answer;
    private int time;
    private boolean useScore;
    private double rate;
}
