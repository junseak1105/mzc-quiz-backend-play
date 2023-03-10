package com.mzc.quiz.play.service;

import com.google.gson.Gson;
import com.mzc.quiz.global.Response.DefaultRes;
import com.mzc.quiz.global.Response.ResponseMessages;
import com.mzc.quiz.global.Response.StatusCode;
import com.mzc.quiz.rabbitMQ.config.RabbitConfig;
import com.mzc.quiz.play.stompConfig.StompWebSocketConfig;
import com.mzc.quiz.play.entity.mongo.Quiz;
import com.mzc.quiz.play.entity.websocket.QuizActionType;
import com.mzc.quiz.play.entity.websocket.QuizCommandType;
import com.mzc.quiz.play.entity.websocket.QuizMessage;
import com.mzc.quiz.global.redisUtil.RedisPrefix;
import com.mzc.quiz.global.redisUtil.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Base64;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final RedisUtil redisUtil;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final AmqpTemplate amqpTemplate;

    public ResponseEntity joinRoom(QuizMessage quizMessage) {
        String pin = redisUtil.genKey(quizMessage.getPinNum());
        if (redisUtil.hasKey(pin)) {
            return new ResponseEntity(DefaultRes.res(StatusCode.OK, ResponseMessages.SUCCESS, quizMessage), HttpStatus.OK);
        } else {
            return new ResponseEntity(DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessages.BAD_REQUEST), HttpStatus.BAD_REQUEST);
        }
    }

    public void setNickname(Principal principal, QuizMessage quizMessage) {
        String playKey = redisUtil.genKey(RedisPrefix.USER.name(), quizMessage.getPinNum());
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), quizMessage.getPinNum());
        String quizCollectKey = redisUtil.genKey(RedisPrefix.ANSCORLIST.name(), quizMessage.getPinNum());
        String resultKey = redisUtil.genKey(RedisPrefix.RESULT.name(), quizMessage.getPinNum());

        QuizMessage resMessage = new QuizMessage();
        System.out.println(quizMessage);
        System.out.println(redisUtil.getScore(playKey, quizMessage.getNickName()));

        if (redisUtil.getScore(playKey, quizMessage.getNickName()) != null) {
            resMessage.setNickName(quizMessage.getNickName());
            resMessage.setAction(QuizActionType.NICKNAMERETRY);
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), StompWebSocketConfig.DIRECT + quizMessage.getPinNum(), resMessage);
            System.out.println("????????? ??????");
        } else {
            redisUtil.setZData(playKey, quizMessage.getNickName(), 0);
            Set<String> userList = redisUtil.getAllZData(playKey);

            quizMessage.setAction(QuizActionType.COMMAND);
            quizMessage.setCommand(QuizCommandType.WAIT);
            quizMessage.setUserList(userList);

            // ?????? ??????????????? ?????? ????????????
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), StompWebSocketConfig.DIRECT + quizMessage.getPinNum(), quizMessage);

            // ????????? ????????? ???????????? ????????? ??????
            String initCorrectList = "";
            String lastquiz = redisUtil.GetHashData(quizKey, "lastQuiz").toString();
            for (int i = 0; i < Integer.parseInt(lastquiz); i++) {
                if (i == 0) {
                    initCorrectList += "-1";
                } else {
                    initCorrectList += "/-1";
                }
            }
            redisUtil.setHashData(quizCollectKey, quizMessage.getNickName(), initCorrectList);

            redisUtil.setZData(resultKey, quizMessage.getNickName(), 0);

            quizMessage.setAction(QuizActionType.ROBBY);
            quizMessage.setCommand(QuizCommandType.BROADCAST);
            amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey, quizMessage);
        }
    }

    public void submit(Principal principal, QuizMessage quizMessage) {
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), quizMessage.getPinNum());
        String userKey = redisUtil.genKey(RedisPrefix.USER.name(), quizMessage.getPinNum());
        String submitKey = redisUtil.genKey(RedisPrefix.SUBMIT.name(), quizMessage.getPinNum());
        String quizCollectKey = redisUtil.genKey(RedisPrefix.ANSCORLIST.name(), quizMessage.getPinNum());

        String QuizDataToString = new String(Base64.getDecoder().decode(redisUtil.GetHashData(quizKey, RedisPrefix.P.name() + quizMessage.getSubmit().getQuizNum()).toString()));
        Gson gson = new Gson();
        Quiz quiz = gson.fromJson(QuizDataToString, Quiz.class);

        double TotalTime = quiz.getTime();
        double AnswerTime = Integer.parseInt(quizMessage.getSubmit().getAnswerTime());
        double Rate = (int) quiz.getRate();

        String answer = quiz.getAnswer().toString().substring(1, quiz.getAnswer().toString().length() - 1);
        String[] answer_arr = answer.split(", ");

        int isCorrect = 0;
        if (quizMessage.getSubmit().getAnswer().length == answer_arr.length) {
            for (int i = 0; i < quizMessage.getSubmit().getAnswer().length; i++) {
                for (int j = 0; j < answer_arr.length; j++) {
                    if (quizMessage.getSubmit().getAnswer()[i].equals(answer_arr[j])) {
                        isCorrect = 1;
                        break;
                    } else if (j == answer_arr.length - 1) {
                        isCorrect = 0;
                    }
                }
            }
        }

        double Score = ((TotalTime * 1000 - AnswerTime) / (TotalTime * 1000)) * 1000 * Rate * isCorrect;

        // ????????? ??????/?????? ??????
        String quizCorrectData = redisUtil.GetHashData(quizCollectKey, quizMessage.getNickName()).toString();
        int currentQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey, "currentQuiz").toString());
        String[] quizCorrect = quizCorrectData.split("/");
        quizCorrect[currentQuiz-1] = Integer.toString(isCorrect);
        System.out.println(quizCorrect[currentQuiz-1]);

        String saveData="";
        for(int i = 0; i<quizCorrect.length;i++){
            if(i!=0){
                saveData += "/" + quizCorrect[i];
            }else{
                saveData += quizCorrect[i];
            }
        }

        redisUtil.setHashData(quizCollectKey, quizMessage.getNickName(), saveData);

        // ?????? ?????? ??? ?????????, ???????????? ??????
        if (isCorrect == 1) {
            quizMessage.getSubmit().setAns(true);
            redisUtil.plusScore(userKey, quizMessage.getNickName(), 1.0);
        } else if (isCorrect == 0) {
            quizMessage.getSubmit().setAns(false);
        }

        // ???????????? ??????
        String resultKey = redisUtil.genKey(RedisPrefix.RESULT.name(), quizMessage.getPinNum());
        // ?????? ?????? ??????????????? ??????
        if (redisUtil.hasKey(resultKey)) { // ????????? ?????? ??????
            redisUtil.plusScore(resultKey, quizMessage.getNickName(), Score);
        } else { // ?????????
            redisUtil.setZData(resultKey, quizMessage.getNickName(), Score);
        }

        // ????????? ??? ?????????
        redisUtil.plusScore(submitKey, RedisPrefix.P.name() + quizMessage.getSubmit().getQuizNum(), 1.0);

        quizMessage.setSubmitCnt(redisUtil.getScore(submitKey, RedisPrefix.P.name() + quizMessage.getSubmit().getQuizNum()).toString().substring(0,1));

        // ????????? ???????????? ?????? ?????? ??????
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), StompWebSocketConfig.DIRECT + quizMessage.getPinNum(), quizMessage);

        amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey, quizMessage);
    }
}
