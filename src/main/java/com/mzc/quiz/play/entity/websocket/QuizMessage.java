package com.mzc.quiz.play.entity.websocket;

// WebSocket 통신용 Message model

import com.mzc.quiz.play.entity.mongo.Quiz;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@Setter
@Getter
@ToString
public class QuizMessage {
    //공통
    private String pinNum;
    private QuizCommandType command;
    private QuizActionType action;
    private String nickName;//Ban, setNickname
    private String startTime; //퀴즈 시작 서버 타임용
    private String submitCnt;


    private List<UserRank> rank;

    //type에 따른 분기
    //COMMAND
    private Quiz quiz;
    //SUBMIT
    private Submit submit;
    private Set<String> userList;

}


