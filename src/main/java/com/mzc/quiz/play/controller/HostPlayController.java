package com.mzc.quiz.play.controller;

import com.mzc.quiz.global.Response.DefaultRes;
import com.mzc.quiz.play.entity.mongo.Show;
import com.mzc.quiz.play.entity.websocket.QuizMessage;
import com.mzc.quiz.play.service.HostPlayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HostPlayController {

    private final HostPlayService hostPlayService;

    // ============== REST API =================
    @PostMapping("/v1/host/createPlay")
    public ResponseEntity playCreate(@RequestBody Show show){
        return hostPlayService.playCreate(show.getId());
    }

    @PostMapping("/v1/host/getUserList")
    public List<String> playGetUserList(@RequestBody QuizMessage quizMessage){
        return hostPlayService.playGetUserList(quizMessage.getPinNum());
    }

    @GetMapping("/v1/host/test")
    public String test(String id) throws Exception {
        return hostPlayService.saveLogData(id);
    }

    // ============== Stomp(Websocket) =================
    @MessageMapping("/ban")
    @SendToUser("/queue/session")
    public void playUserBan(@RequestBody QuizMessage quizMessage){
        hostPlayService.playUserBan(quizMessage);
    }

    @MessageMapping("/final")
    public void playFinal(@RequestBody QuizMessage quizMessage){
        hostPlayService.playFinal(quizMessage);
    }

    @MessageMapping("/end")
    public void playEnd(@RequestBody QuizMessage quizMessage){
        hostPlayService.playEnd(quizMessage);
    }
}
