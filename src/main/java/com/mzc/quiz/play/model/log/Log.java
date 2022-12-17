package com.mzc.quiz.play.model.log;

import lombok.Data;

@Data
public class Log {
    private ShowData showdata; // Show Id, Show 제목

    private String playdate; // 시작 날짜/시간
    private String quizcount; // 총 문제 수
    private String playtime; // 진행한 시간
    private String usercount; // 참가한 유저

    private UserData userData; // 랭킹, 닉네임, 랭킹점수, 맞은 문제수, 정답여부
}
