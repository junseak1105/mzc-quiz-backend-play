package com.mzc.quiz.play.entity.mongo;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Choice {
    private String num1;
    private String num2;
    private String num3;
    private String num4;
}