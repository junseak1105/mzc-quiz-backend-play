package com.mzc.quiz.play.service;

import com.google.gson.Gson;
import com.mzc.quiz.global.Response.DefaultRes;
import com.mzc.quiz.global.Response.ResponseMessages;
import com.mzc.quiz.global.Response.StatusCode;
import com.mzc.quiz.rabbitMQ.config.RabbitConfig;
import com.mzc.quiz.play.entity.mongo.Quiz;
import com.mzc.quiz.play.entity.mongo.Show;
import com.mzc.quiz.play.entity.websocket.QuizActionType;
import com.mzc.quiz.play.entity.websocket.QuizCommandType;
import com.mzc.quiz.play.entity.websocket.QuizMessage;
import com.mzc.quiz.play.entity.websocket.UserRank;
import com.mzc.quiz.play.repository.QplayMongoRepository;
import com.mzc.quiz.global.redisUtil.RedisPrefix;
import com.mzc.quiz.global.redisUtil.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Log4j2
@RequiredArgsConstructor
public class HostRoundService {

    private final RedisUtil redisUtil;
    private final AmqpTemplate amqpTemplate;
    private final HostPlayService hostPlayService;


    public void roundStart(QuizMessage quizMessage) {
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), quizMessage.getPinNum());
        if (redisUtil.hasKey(quizKey)) {
            int currentQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey, "currentQuiz").toString());

            String QuizDataToString = new String(Base64.getDecoder().decode(redisUtil.GetHashData(quizKey, RedisPrefix.P.name() + currentQuiz).toString()));

            Gson gson = new Gson();
            Quiz quiz = gson.fromJson(QuizDataToString, Quiz.class);
            quiz.setAnswer(null);

            quizMessage.setAction(QuizActionType.COMMAND);
            quizMessage.setCommand(QuizCommandType.START);
            quizMessage.setQuiz(quiz);

            amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey, quizMessage);
        } else {

        }
    }

    public void roundResult(QuizMessage quizMessage) {
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), quizMessage.getPinNum());
        String resultKey = redisUtil.genKey(RedisPrefix.RESULT.name(), quizMessage.getPinNum());
        String logKey = redisUtil.genKey(RedisPrefix.LOG.name(), quizMessage.getPinNum());

        // 정답 데이터 가져오기
        int currentQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey, "currentQuiz").toString());
        String QuizDataToString = new String(Base64.getDecoder().decode(redisUtil.GetHashData(quizKey, RedisPrefix.P.name() + currentQuiz).toString()));

        Gson gson = new Gson();
        Quiz quiz = gson.fromJson(QuizDataToString, Quiz.class);
        quizMessage.setQuiz(quiz);

        // 랭킹 점수 final이랑 공통 리펙토링할 때 공통 클래스를 만들거나 하면 좋을 듯
        long userCount = redisUtil.getZDataSize(redisUtil.genKey(RedisPrefix.USER.name(), quizMessage.getPinNum()));
        Set<ZSetOperations.TypedTuple<String>> ranking = redisUtil.getZData(resultKey, 0, userCount);

        Iterator<ZSetOperations.TypedTuple<String>> iterRank = ranking.iterator();
        List<UserRank> RankingList = new ArrayList<>();
        int rank=1;
        while(iterRank.hasNext()){
            ZSetOperations.TypedTuple<String> rankData = iterRank.next();
            RankingList.add(new UserRank(rank, rankData.getValue(), rankData.getScore()));
            System.out.println("rank : "+rank+", NickName : "+ rankData.getValue()+", Score : "+ rankData.getScore());
            rank++;
        }
        quizMessage.setRank(RankingList);

        // 마지막 퀴즈인지 체크
        int lastQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey, "lastQuiz").toString());
        System.out.println(lastQuiz);
        if (currentQuiz < lastQuiz) {
            redisUtil.setHashData(quizKey, "currentQuiz", Integer.toString(quizMessage.getQuiz().getNum() + 1));

            quizMessage.setCommand(QuizCommandType.RESULT);
            quizMessage.setAction(QuizActionType.COMMAND);
            amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey, quizMessage);
        } else {
            hostPlayService.playFinal(quizMessage);
        }
    }

    public void roundSkip(QuizMessage quizMessage) {
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), quizMessage.getPinNum());
        String quizCollectKey = redisUtil.genKey(RedisPrefix.ANSCORLIST.name(), quizMessage.getPinNum());

        int currentQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey, "currentQuiz").toString());
        int lastQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey, "lastQuiz").toString());

        // 건너뛰기때 정답여부 변경
//        if (redisUtil.hasKey(quizCollectKey)) { // 키값이 있을 때
//            String quizCorrectData = redisUtil.GetHashData(quizCollectKey, quizMessage.getNickName()).toString();
//            String[] quizCorrect = quizCorrectData.split(",");
//
//            if (currentQuiz == quizCorrect.length) { // 제출한 상태
//                redisUtil.setHashData(quizCollectKey, quizMessage.getNickName(), quizCorrectData.substring(0, quizCorrectData.length() - 1) + "-1");
//            } else if (currentQuiz > quizCorrect.length) { // 제줄안한 상태
//                redisUtil.setHashData(quizCollectKey, quizMessage.getNickName(), quizCorrectData + ",-1");
//            }
//        } else { // 키값이 없을 때
//            redisUtil.setHashData(quizCollectKey, quizMessage.getNickName(), "-1");
//        }

        if (currentQuiz < lastQuiz) {
            redisUtil.setHashData(quizKey, "currentQuiz", Integer.toString(currentQuiz + 1));
            roundStart(quizMessage);
        } else {
            hostPlayService.playFinal(quizMessage);
        }

        amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey, quizMessage);
    }
}
