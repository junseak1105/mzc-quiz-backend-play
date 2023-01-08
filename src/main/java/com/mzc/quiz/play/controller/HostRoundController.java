package com.mzc.quiz.play.controller;

import com.mzc.quiz.global.Response.DefaultRes;
import com.mzc.quiz.play.entity.mongo.Show;
import com.mzc.quiz.play.entity.websocket.QuizMessage;
import com.mzc.quiz.play.service.HostPlayService;
import com.mzc.quiz.play.service.HostRoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HostRoundController {

    private final HostRoundService hostRoundService;

    @MessageMapping("/start")
    public void quizStart(@RequestBody QuizMessage quizMessage){
        hostRoundService.roundStart(quizMessage);
    }

    @MessageMapping("/skip")
    public void quizSkip(@RequestBody QuizMessage quizMessage){
        hostRoundService.roundSkip(quizMessage);
    }

    @MessageMapping("/result")
    public void quizResult(@RequestBody QuizMessage quizMessage){
        hostRoundService.roundResult(quizMessage);
    }




}
