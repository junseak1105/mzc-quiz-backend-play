package com.mzc.quiz.play.controller;

import com.mzc.global.Response.DefaultRes;
import com.mzc.quiz.play.model.websocket.QuizMessage;
import com.mzc.quiz.play.model.mongo.Show;
import com.mzc.quiz.play.service.HostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HostController {

    @Autowired
    private HostService hostService;


    @PostMapping("/v1/host/createPlay")
    public DefaultRes createPlay(@RequestBody Show show){
        return hostService.createPlay(show.getId());
    }

    @PostMapping("/v1/host/getUserList")
    public List<String> getUserList(@RequestBody QuizMessage quizMessage){
        return hostService.getUserList(quizMessage.getPinNum());
    }

    // START
    // - 클라이언트에 퀴즈 시작 명령어 전송
    @MessageMapping("/start")
    public void quizStart(@RequestBody QuizMessage quizMessage){
        hostService.quizStart(quizMessage);
    }

    // SKIP
    // - 클라이언트에 다음문제 스킵 명령어 전송
    @MessageMapping("/skip")
    public void quizSkip(@RequestBody QuizMessage quizMessage){
        hostService.quizSkip(quizMessage);
    }

    // RESULT
    // - 클라이언트에 현재 퀴즈 결과 명령어 전송
    @MessageMapping("/result")
    public void quizResult(@RequestBody QuizMessage quizMessage){
        hostService.quizResult(quizMessage);
    }


    // FINAL
    // - 클라이언트에 최종 퀴즈 결과 명령어 전송
    @MessageMapping("/final")
    public void quizFinal(@RequestBody QuizMessage quizMessage){
        hostService.quizFinal(quizMessage);
    }

    // BAN
    // - 특정 유저 강퇴
    @MessageMapping("/ban")
    @SendToUser("/queue/session")
    public void userBan(@RequestBody QuizMessage quizMessage){
        hostService.userBan(quizMessage);
    }

    // END
    // - 플레이 종료
    @MessageMapping("/end")
    public void playEnd(@RequestBody QuizMessage quizMessage){
        hostService.playEnd(quizMessage);
    }
}
