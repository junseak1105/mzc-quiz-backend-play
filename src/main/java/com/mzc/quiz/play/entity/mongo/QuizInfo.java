package com.mzc.quiz.play.entity.mongo;



import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class QuizInfo {

    private String email;
    private String title;
    private String category;
    private List<String> tags;
    private String titleImg_origin;
    private String titleImg_thumb;
    private String createDate;
    private String lastModifyDate;
    private boolean isPulic;
    private String state;
}

