package com.mzc.quiz.play.service;

import com.google.gson.Gson;
import com.mzc.global.Response.DefaultRes;
import com.mzc.global.Response.ResponseMessages;
import com.mzc.global.Response.StatusCode;
import com.mzc.quiz.play.model.mongo.Quiz;
import com.mzc.quiz.play.model.websocket.QuizActionType;
import com.mzc.quiz.play.model.websocket.QuizCommandType;
import com.mzc.quiz.play.model.websocket.QuizMessage;
import com.mzc.quiz.play.model.websocket.UserRank;
import com.mzc.quiz.play.repository.QplayRepository;
import com.mzc.quiz.play.util.RedisPrefix;
import com.mzc.quiz.play.util.RedisUtil;
import com.mzc.quiz.show.entity.Show;
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

import static com.mzc.quiz.play.config.RabbitConfig.quizRoutingKey;
import static com.mzc.quiz.play.config.RabbitConfig.quizExchange;

@Service
@Log4j2
@RequiredArgsConstructor
public class HostService {

    private final RedisUtil redisUtil;
    private final QplayRepository qplayRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AmqpTemplate amqpTemplate;



    public void quizStart(QuizMessage quizMessage) {
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

            amqpTemplate.convertAndSend(quizExchange, quizRoutingKey, quizMessage);
        } else {

        }
    }

    public void quizResult(QuizMessage quizMessage) {
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), quizMessage.getPinNum());
        String resultKey = redisUtil.genKey(RedisPrefix.RESULT.name(), quizMessage.getPinNum());
        String logKey = redisUtil.genKey(RedisPrefix.LOG.name(), quizMessage.getPinNum());

        // 정답 데이터 가져오기
        int currentQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey, "currentQuiz").toString());
        String QuizDataToString = new String(Base64.getDecoder().decode(redisUtil.GetHashData(quizKey, RedisPrefix.P.name() + currentQuiz).toString()));

        Gson gson = new Gson();
        Quiz quiz = gson.fromJson(QuizDataToString, Quiz.class);
        quizMessage.setQuiz(quiz);

        // 랭킹 점수
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

        // 마지막 퀴즈인지 체크
        int lastQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey,"lastQuiz").toString());
        System.out.println(lastQuiz);
        if(currentQuiz < lastQuiz) {
            redisUtil.setHashData(quizKey, "currentQuiz", Integer.toString(quizMessage.getQuiz().getNum() + 1));

            quizMessage.setCommand(QuizCommandType.RESULT);
            quizMessage.setAction(QuizActionType.COMMAND);


            amqpTemplate.convertAndSend(quizExchange, quizRoutingKey, quizMessage);
        }else{
            quizFinal(quizMessage);
        }
    }

    public void quizSkip(QuizMessage quizMessage) {
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), quizMessage.getPinNum());

        int currentQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey, "currentQuiz").toString());
        int lastQuiz = Integer.parseInt(redisUtil.GetHashData(quizKey,"lastQuiz").toString());

        System.out.println("last" + lastQuiz);

        if(currentQuiz < lastQuiz){
            System.out.println("current" + currentQuiz);
            redisUtil.setHashData(quizKey, "currentQuiz", Integer.toString(currentQuiz + 1));
            quizStart(quizMessage);
        }else{
            quizFinal(quizMessage);
        }

//        quizMessage.setCommand(QuizCommandType.START);
//        quizMessage.setAction(QuizActionType.COMMAND);
        amqpTemplate.convertAndSend(quizExchange, quizRoutingKey,quizMessage);

    }

    public void quizFinal(QuizMessage quizMessage) {

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
        amqpTemplate.convertAndSend(quizExchange, quizRoutingKey,quizMessage);
    }

    public void userBan(QuizMessage quizMessage){
        String pin = quizMessage.getPinNum();
        String key = redisUtil.genKey(RedisPrefix.USER.name(), pin);
        String nickname = quizMessage.getNickName();
        System.out.printf(nickname);

        QuizMessage resMessage = new QuizMessage();
        if(redisUtil.SREM(key, nickname) == 1){
            List<String> userList = redisUtil.getUserList(quizMessage.getPinNum());
            resMessage.setAction(QuizActionType.BAN);
            resMessage.setCommand(QuizCommandType.KICK);
            resMessage.setPinNum(quizMessage.getPinNum());
            resMessage.setNickName(nickname);
            resMessage.setUserList(userList);
            System.out.println(getUserList(pin));
        }else{
//            resMessage.setPinNum(quizMessage.getPinNum());
//            resMessage.setAction(QuizActionType.BAN);
//            resMessage.setCommand(QuizCommandType.KICK);
//            resMessage.setNickName(nickname);

        }

        amqpTemplate.convertAndSend(quizExchange, quizRoutingKey,quizMessage);
    }


    // 공통 기능으로 빼기
    public List<String> getUserList(String pinNum){
        return redisUtil.getUserList(pinNum);
    }

    // 퀴즈 핀
    public DefaultRes createPlay(String quizId) {
        try {
            String pin = makePIN(quizId);
            System.out.println("createPlay : " + pin);
            // mongoDB 조회 -> 총 몇개인지 확인하고
            // 문제별 방도 생성을 해야 되나?
            return DefaultRes.res(StatusCode.OK, ResponseMessages.SUCCESS, pin);
        } catch (Exception e) {
            return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessages.BAD_REQUEST);
        }
    }


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

    public boolean NullCheck(QuizMessage quizMessage) {
//        if(quizMessage == null || quizMessage.getContent() == null){
//            return true;
//        }
        return false;
    }
    
    // 현재 시간
    public String nowTime(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        return ""+now.format(formatter);
    }
    
    public void playEnd(QuizMessage quizMessage) {
        String pin = quizMessage.getPinNum();
        String playKey = redisUtil.genKey(pin);
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), pin);

        redisUtil.DEL(playKey);
        redisUtil.DEL(quizKey);

        simpMessagingTemplate.convertAndSend(TOPIC + quizMessage.getPinNum(), quizMessage);
    }
}
