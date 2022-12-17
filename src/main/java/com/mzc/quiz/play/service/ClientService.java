package com.mzc.quiz.play.service;

import com.google.gson.Gson;
import com.mzc.global.Response.DefaultRes;
import com.mzc.global.Response.ResponseMessages;
import com.mzc.global.Response.StatusCode;
import com.mzc.quiz.play.model.mongo.Quiz;
import com.mzc.quiz.play.model.websocket.QuizActionType;
import com.mzc.quiz.play.model.websocket.QuizCommandType;
import com.mzc.quiz.play.model.websocket.QuizMessage;
import com.mzc.quiz.play.util.RedisPrefix;
import com.mzc.quiz.play.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Base64;
import java.util.List;

import static com.mzc.quiz.play.config.RabbitConfig.quieExchange;
import static com.mzc.quiz.play.config.StompWebSocketConfig.DIRECT;
import static com.mzc.quiz.play.config.StompWebSocketConfig.TOPIC;

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
        String playKey = redisUtil.genKey(RedisPrefix.USER.name(),quizMessage.getPinNum());
        String username = quizMessage.getNickName();
        // Set 조회해서 -> content에 넣어서 보내기
        QuizMessage resMessage = new QuizMessage();
        System.out.println(quizMessage);
        if (redisUtil.SISMEMBER(playKey, username)) {
            // Front에서 KickName 중복시 명령어 결정 후 추가 코드 작성
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), DIRECT + quizMessage.getPinNum(), "nicknametry");
            System.out.println("닉네임 중복");
        } else {
            redisUtil.SADD(playKey, username);
            List<String> userList = redisUtil.getUserList(quizMessage.getPinNum());

            quizMessage.setAction(QuizActionType.COMMAND);
            quizMessage.setCommand(QuizCommandType.WAIT);
            quizMessage.setUserList(userList);

            // 보낸 유저한테만 다시 보내주고
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), DIRECT + quizMessage.getPinNum(), quizMessage);

            // LOG:핀번호 - 유저수
            String logKey = redisUtil.genKey(RedisPrefix.LOG.name(), quizMessage.getPinNum());
            if(userList.size() == 1){
                redisUtil.leftPush(logKey,Integer.toString(userList.size()));
            }
            else{
                redisUtil.listDataSet(logKey, 0,Integer.toString(userList.size()));
            }

            quizMessage.setAction(QuizActionType.ROBBY);
            quizMessage.setCommand(QuizCommandType.BROADCAST);
            simpMessagingTemplate.convertAndSend(TOPIC + quizMessage.getPinNum(), quizMessage);
//            amqpTemplate.convertAndSend(quieExchange, "",quizMessage);
        }
    }

    public void submit(QuizMessage quizMessage) {
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), quizMessage.getPinNum());

        String QuizDataToString = new String(Base64.getDecoder().decode(redisUtil.GetHashData(quizKey, RedisPrefix.P.name() + quizMessage.getSubmit().getQuizNum()).toString()));
        Gson gson = new Gson();
        Quiz quiz = gson.fromJson(QuizDataToString, Quiz.class);


        double TotalTime = quiz.getTime();
        double AnswerTime = Integer.parseInt(quizMessage.getSubmit().getAnswerTime());
        double Rate = (int) quiz.getRate();

        //get rid of [ and ] in string
        String answer = quiz.getAnswer().toString().substring(1, quiz.getAnswer().toString().length() - 1);
        String[] answer_arr = answer.split(", ");

        int isCorrect = 0;
        if (quizMessage.getSubmit().getAnswer().length == answer_arr.length) {
            for (int i = 0; i < quizMessage.getSubmit().getAnswer().length; i++) {
                for(int j = 0; j < answer_arr.length; j++) {
                    if (quizMessage.getSubmit().getAnswer()[i].equals(answer_arr[j])) {
                        isCorrect = 1;
                        break;
                    }else if(j == answer_arr.length - 1) {
                        isCorrect = 0;
                    }
                }
                //isCorrect = 1;
            }
        }

//        // 문제별 정답/오답 저장
//        // 문제 제출하면 isCorrect를 저장
//        // 근데 제출한 후에 건너뛰기를 했을 경우, SKIP에서 모든 유저 해당문제 -1로 처리
//        String quizCollectKey=redisUtil.genKey("ANSWERCORRECT", quizMessage.getPinNum());
//        if(redisUtil.hasKey(quizCollectKey)){ // 키값이 있을 때
//            // 저장되어있는 데이터 + , + 정답여부
//            String quizCorrectData = redisUtil.GetHashData(quizCollectKey, quizMessage.getNickName()).toString() + ',' + isCorrect;
//            System.out.println("Client_quizCorrectData : " + quizCorrectData);
//            redisUtil.setHashData(quizCollectKey, quizMessage.getNickName(), quizCorrectData);
//        }
//        else{ // 키값이 없을 때
//            redisUtil.setHashData(quizCollectKey, quizMessage.getNickName(), Integer.toString(isCorrect));
//        }

        System.out.println("TotalTime : " + TotalTime);
        System.out.println("AnswerTime : " + AnswerTime);
        System.out.println("Rate : " + Rate);
        System.out.println("isCorrect : " + isCorrect);

        double Score = ((TotalTime*1000 - AnswerTime) / (TotalTime*1000)) * 1000 * Rate * isCorrect;


        // Result:키값 시작할 때 먼저 생성해놓는게 좋겠죠?
        // 랭킹점수 증가
        String resultKey = redisUtil.genKey(RedisPrefix.RESULT.name(), quizMessage.getPinNum());
        // 해당 키가 존재하는지 체크
        if(redisUtil.hasKey(resultKey)){ // 있으면 점수 증가
            System.out.println("HasKey");
            redisUtil.plusScore(resultKey, quizMessage.getNickName(), Score);
        }
        else{ // 없으면
            System.out.println("noHasKey");
            redisUtil.setZData(resultKey, quizMessage.getNickName(), Score);
        }
    }
}
