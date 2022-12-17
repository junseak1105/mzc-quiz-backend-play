package com.mzc.quiz.play.service;

import com.google.gson.Gson;
import com.mzc.quiz.global.Response.DefaultRes;
import com.mzc.quiz.global.Response.ResponseMessages;
import com.mzc.quiz.global.Response.StatusCode;
import com.mzc.quiz.global.redisUtil.RedisPrefix;
import com.mzc.quiz.global.redisUtil.RedisUtil;
import com.mzc.quiz.play.entity.mongo.Show;
import com.mzc.quiz.play.entity.websocket.QuizActionType;
import com.mzc.quiz.play.entity.websocket.QuizCommandType;
import com.mzc.quiz.play.entity.websocket.QuizMessage;
import com.mzc.quiz.play.entity.websocket.UserRank;
import com.mzc.quiz.play.repository.QplayMongoRepository;
import com.mzc.quiz.rabbitMQ.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class HostPlayService {
    private final RedisUtil redisUtil;
    private final QplayMongoRepository qplayRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AmqpTemplate amqpTemplate;


    public DefaultRes playCreate(String quizId) {
        try {
            String pin = makePIN(quizId);
            return DefaultRes.res(StatusCode.OK, ResponseMessages.SUCCESS, pin);
        } catch (Exception e) {
            return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessages.BAD_REQUEST);
        }
    }

    public List<String> playGetUserList(String pinNum){
        return redisUtil.getUserList(pinNum);
    }

    public void playUserBan(QuizMessage quizMessage){
        String pin = quizMessage.getPinNum();
        String key = redisUtil.genKey(RedisPrefix.USER.name(), pin);
        String nickname = quizMessage.getNickName();
        System.out.printf(nickname);


        if(redisUtil.SREM(key, nickname) == 1){
            List<String> userList = redisUtil.getUserList(quizMessage.getPinNum());
            System.out.println(redisUtil.getUserList(pin));
        }else{

        }

        amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey,quizMessage);
    }

    public void playFinal(QuizMessage quizMessage) {

        String resultKey = redisUtil.genKey(RedisPrefix.RESULT.name(), quizMessage.getPinNum());
        String logKey = redisUtil.genKey(RedisPrefix.LOG.name(), quizMessage.getPinNum());

        // 랭킹 갱신
        long userCount = redisUtil.setDataSize(redisUtil.genKey(RedisPrefix.USER.name(), quizMessage.getPinNum()));
        Set<ZSetOperations.TypedTuple<String>> ranking = redisUtil.getRanking(resultKey, 0, userCount);

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

        // LOG:PIN - 끝난 시간, 유저별 랭킹데이터
        redisUtil.leftPush(logKey,"playendtime:"+nowTime());


        quizMessage.setCommand(QuizCommandType.FINAL);
        quizMessage.setAction(QuizActionType.COMMAND);

//        simpMessagingTemplate.convertAndSend(TOPIC + quizMessage.getPinNum(), quizMessage);
        amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey,quizMessage);
    }

    public void playEnd(QuizMessage quizMessage) {
        String pin = quizMessage.getPinNum();
        String playKey = redisUtil.genKey(pin);
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), pin);

        redisUtil.DEL(playKey);
        redisUtil.DEL(quizKey);

        amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey,quizMessage);
    }


    // PlayService Util

    public String makePIN(String quizId) {
        String pin;

        while (true) {
            pin = RandomStringUtils.randomNumeric(6);
            String playKey = redisUtil.genKey(pin);
            String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), pin);

            if (redisUtil.hasKey(playKey)) {
                // 다시 생성
            } else {
                Show show = qplayRepository.findShowById(quizId);
                Gson gson = new Gson();

                if (show != null) {
                    redisUtil.setHashData(quizKey, "currentQuiz", "1");
                    redisUtil.setHashData(quizKey, "lastQuiz", Integer.toString(show.getQuizData().size()));
                    for (int i = 0; i < show.getQuizData().size(); i++) {
                        String base64QuizData = Base64.getEncoder().encodeToString(gson.toJson(show.getQuizData().get(i)).getBytes());
                        redisUtil.setHashData(quizKey, RedisPrefix.P.name() + (i + 1), base64QuizData);
                    }
                } else {
                    //return "퀴즈데이터가 정상적으로 저장되지 않았습니다.";
                }

                // 퀴즈 생성할 때 Log:핀번호 - Show Id, Show Title, 총 문제수 저장, 시작 시간
                String logKey = redisUtil.genKey(RedisPrefix.LOG.name(), pin);
                redisUtil.leftPush(logKey, "showid:"+quizId);
                redisUtil.leftPush(logKey, "showtitle:"+show.getQuizInfo().getTitle());
                redisUtil.leftPush(logKey, "quizcount:"+show.getQuizData().size());
                redisUtil.leftPush(logKey, "quizstarttime:"+nowTime());

                // PLAY:pinNum  - 유저 리스트에 MongoDB의 ID가 들어감
                redisUtil.SADD(playKey, quizId);
                redisUtil.expire(playKey, 12, TimeUnit.HOURS);  // 하루만 유지??
                break;
            }
        }
        return pin;
    }

    public String nowTime(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        return ""+now.format(formatter);
    }
}
