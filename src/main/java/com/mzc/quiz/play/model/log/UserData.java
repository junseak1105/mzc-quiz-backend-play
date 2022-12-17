package com.mzc.quiz.play.model.log;

import lombok.Data;

@Data
public class UserData {
    private String nickname; // 닉네임
    private String rank; // 랭킹
    private String rankScore; // 랭킹점수
    private String correctCount; // 맞은개수
    private String isCorrectList; // 정답여부
}
