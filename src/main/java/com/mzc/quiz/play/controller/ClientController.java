package com.mzc.quiz.play.controller;

import com.mzc.quiz.global.Response.DefaultRes;
import com.mzc.quiz.play.entity.websocket.QuizMessage;
import com.mzc.quiz.play.service.ClientService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
public class ClientController {

    @Autowired
    ClientService clientService;


    @PostMapping("/joinroom")
    @ApiOperation(value = "방 입장" ,
            notes = "{pinNum : 방번호}")
    public DefaultRes joinRoom(@RequestBody QuizMessage quizMessage){
        return clientService.joinRoom(quizMessage);
    }

    @MessageMapping("/setnickname")
    public void setNickname( Principal principal, @RequestBody QuizMessage quizMessage){
        clientService.setNickname(principal, quizMessage);
    }

    @MessageMapping("/submit")
    public void submit(Principal principal, @RequestBody QuizMessage quizMessage){
        clientService.submit(principal, quizMessage);
    }
}
