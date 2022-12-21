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

    public DefaultRes joinRoom(QuizMessage quizMessage) {
        String pin = redisUtil.genKey(quizMessage.getPinNum());
        if (redisUtil.hasKey(pin)) {
            return DefaultRes.res(StatusCode.OK, ResponseMessages.SUCCESS, quizMessage);
        } else {
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessages.BAD_REQUEST);
        }
    }

    public void setNickname(Principal principal, QuizMessage quizMessage) {
        String playKey = redisUtil.genKey(RedisPrefix.USER.name(), quizMessage.getPinNum());
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), quizMessage.getPinNum());
        String quizCollectKey = redisUtil.genKey(RedisPrefix.ANSCORLIST.name(), quizMessage.getPinNum()); // 임시

        QuizMessage resMessage = new QuizMessage();
        System.out.println(quizMessage);
        System.out.println(redisUtil.getScore(playKey, quizMessage.getNickName()));

        if (redisUtil.getScore(playKey, quizMessage.getNickName()) != null) {
            // 변경 예정
            resMessage.setAction(QuizActionType.NICKNAMETRY);
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), StompWebSocketConfig.DIRECT + quizMessage.getPinNum(), resMessage);
            System.out.println("닉네임 중복");
        } else {
            redisUtil.setZData(playKey, quizMessage.getNickName(), 0);
            Set<String> userList = redisUtil.getAllZData(playKey);

            quizMessage.setAction(QuizActionType.COMMAND);
            quizMessage.setCommand(QuizCommandType.WAIT);
            quizMessage.setUserList(userList);

            // 보낸 유저한테만 다시 보내주고
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), StompWebSocketConfig.DIRECT + quizMessage.getPinNum(), quizMessage);

            // 닉네임 설정때 정답여부 초기값 설정
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

        // 문제별 정답/오답 저장
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

        // 맞은 문제 수 카운트, 정답여부 세팅
        if (isCorrect == 1) {
            quizMessage.getSubmit().setAns(true);
            redisUtil.plusScore(userKey, quizMessage.getNickName(), 1.0);
        } else if (isCorrect == 0) {
            quizMessage.getSubmit().setAns(false);
        }

        // 랭킹점수 증가
        String resultKey = redisUtil.genKey(RedisPrefix.RESULT.name(), quizMessage.getPinNum());
        // 해당 키가 존재하는지 체크
        if (redisUtil.hasKey(resultKey)) { // 있으면 점수 증가
            redisUtil.plusScore(resultKey, quizMessage.getNickName(), Score);
        } else { // 없으면
            redisUtil.setZData(resultKey, quizMessage.getNickName(), Score);
        }

        // 제출자 수 카운트
        redisUtil.plusScore(submitKey, RedisPrefix.P.name() + quizMessage.getSubmit().getQuizNum(), 1.0);

        // 제출한 사람에게 정답 여부 전달
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), StompWebSocketConfig.DIRECT + quizMessage.getPinNum(), quizMessage);
    }
}
