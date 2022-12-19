package com.mzc.quiz.play.service;

import com.google.gson.Gson;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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


    public ResponseEntity playCreate(String quizId) {
        try {
            String pin = makePIN(quizId);
            return new ResponseEntity((DefaultRes.res(StatusCode.OK, ResponseMessages.SUCCESS, pin)), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity((DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessages.BAD_REQUEST)), HttpStatus.BAD_REQUEST);
        }
    }

    public List<String> playGetUserList(String pinNum) {
        return redisUtil.getUserList(pinNum);
    }

    public void playUserBan(QuizMessage quizMessage) {
        String pin = quizMessage.getPinNum();
        String key = redisUtil.genKey(RedisPrefix.USER.name(), pin);
        String nickname = quizMessage.getNickName();
        System.out.printf(nickname);

        if (redisUtil.SREM(key, nickname) == 1) {
            List<String> userList = redisUtil.getUserList(quizMessage.getPinNum());
            System.out.println(redisUtil.getUserList(pin));
        }

        amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey, quizMessage);
    }

    public void playFinal(QuizMessage quizMessage) {
        String resultKey = redisUtil.genKey(RedisPrefix.RESULT.name(), quizMessage.getPinNum());

        // 랭킹 갱신
        // userCount set->sorted set으로 변경해서 바꿔야함.
        long userCount = redisUtil.getZDataSize(redisUtil.genKey(RedisPrefix.USER.name(), quizMessage.getPinNum()));
        Set<ZSetOperations.TypedTuple<String>> ranking = redisUtil.getZData(resultKey, 0, userCount);

        Iterator<ZSetOperations.TypedTuple<String>> iterRank = ranking.iterator();
        List<UserRank> RankingList = new ArrayList<>();
        int rank = 1;
        while (iterRank.hasNext()) {
            ZSetOperations.TypedTuple<String> rankData = iterRank.next();
            RankingList.add(new UserRank(rank, rankData.getValue(), rankData.getScore()));
            System.out.println("rank : " + rank + ", NickName : " + rankData.getValue() + ", Score : " + rankData.getScore());
            rank++;
        }
        quizMessage.setRank(RankingList);

        // LOG:PIN - 끝난 시간, 유저별 랭킹데이터
        saveLogData("FINAL", quizMessage.getPinNum(), RankingList);

        quizMessage.setCommand(QuizCommandType.FINAL);
        quizMessage.setAction(QuizActionType.COMMAND);

//        simpMessagingTemplate.convertAndSend(TOPIC + quizMessage.getPinNum(), quizMessage);
        amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey, quizMessage);
    }

    public void playEnd(QuizMessage quizMessage) {
        String pin = quizMessage.getPinNum();
        String playKey = redisUtil.genKey(pin);
        String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), pin);

        redisUtil.DEL(playKey);
        redisUtil.DEL(quizKey);

        amqpTemplate.convertAndSend(RabbitConfig.quizExchange, RabbitConfig.quizRoutingKey, quizMessage);
    }

    // PlayService Util

    public String makePIN(String quizId) {
        String pin;

        while (true) {
            pin = RandomStringUtils.randomNumeric(6);
            String playKey = redisUtil.genKey(pin);
            String quizKey = redisUtil.genKey(RedisPrefix.QUIZ.name(), pin);
            String submitKey = redisUtil.genKey(RedisPrefix.SUBMIT.name(), pin);

            if (redisUtil.hasKey(playKey)) {
                // 다시 생성
            } else {
                Gson gson = new Gson();
                try {
                    String show = apiTestGet(quizId);

                    JsonObject jsonObject = JsonParser.parseString(show).getAsJsonObject();
                    Show showData = gson.fromJson(jsonObject.get("Item"), Show.class);

                    if (show != null) {
                        redisUtil.setHashData(quizKey, "currentQuiz", "1");
                        redisUtil.setHashData(quizKey, "lastQuiz", Integer.toString(showData.getQuizData().size()));
                        for (int i = 0; i < showData.getQuizData().size(); i++) {
                            System.out.println(submitKey);
                            System.out.println(RedisPrefix.P.name() + (i + 1));
                            String base64QuizData = Base64.getEncoder().encodeToString(gson.toJson(showData.getQuizData().get(i)).getBytes());
                            redisUtil.setHashData(quizKey, RedisPrefix.P.name() + (i + 1), base64QuizData);
                            redisUtil.setZData(submitKey, RedisPrefix.P.name() + (i + 1), 0);
                        }
                    }

                    saveLogData("START", pin, show);

                    // PLAY:pinNum  - 유저 리스트에 MongoDB의 ID가 들어감
                    redisUtil.SADD(playKey, quizId);
                    redisUtil.expire(playKey, 12, TimeUnit.HOURS);  // 하루만 유지??
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                break;
            }
        }
        return pin;
    }

    public String nowTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        return "" + now.format(formatter);
    }

    public void saveLogData(String command, String pin, Object data) {
        String logKey = redisUtil.genKey(RedisPrefix.LOG.name(), pin);

        switch (command) {
            case "START":
                Show startdata = (Show) data;
                redisUtil.leftPush(logKey, "showid:" + startdata.getId());
                redisUtil.leftPush(logKey, "showtitle:" + startdata.getQuizInfo().getTitle());
                redisUtil.leftPush(logKey, "quizcount:" + startdata.getQuizData().size());
                redisUtil.leftPush(logKey, "quizdate:" + nowTime());
                break;
            case "FINAL":
                String quizCollectKey = redisUtil.genKey(RedisPrefix.ANSCORLIST.name(), pin);
                String userKey = redisUtil.genKey(RedisPrefix.USER.name(), pin);

                List<UserRank> finaldata = (List<UserRank>) data;
                Map<Object, Object> iscorrectlist = redisUtil.GetAllHashData(quizCollectKey);
                Set<String> correctCountList = redisUtil.getAllZData(userKey);
                for (UserRank user : finaldata) {
                    String userdata = "nickname:" + user.getNickName() + ",rank:" + user.getRank() + ",rankscore:" + user.getRankScore();

                    userdata += ",iscorrectlist:" + iscorrectlist.get(user.getNickName());

                    Iterator iter = correctCountList.iterator();
                    while (iter.hasNext()) {
                        String nickname = (String) iter.next();
                        if (user.getNickName() == nickname) {
                            userdata += ",correctcount:" + redisUtil.getScore(userKey, nickname);
                        }
                    }
                    redisUtil.leftPush(logKey, userdata);
                }
                break;
        }
    }

    public String apiTestGet(String id) throws Exception {
        URL url = null;
        String readLine = null;
        StringBuilder buffer = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        HttpURLConnection urlConnection = null;

        int connTimeout = 5000;
        int readTimeout = 3000;

        String apiUrl = "https://ted9c640x5.execute-api.ap-northeast-3.amazonaws.com/v1/show/" + id;    // 각자 상황에 맞는 IP & url 사용

        try {
            url = new URL(apiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(connTimeout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setRequestProperty("Accept", "application/json;");

            buffer = new StringBuilder();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                while ((readLine = bufferedReader.readLine()) != null) {
                    buffer.append(readLine).append("\n");
                }
            } else {
                buffer.append("code : ");
                buffer.append(urlConnection.getResponseCode()).append("\n");
                buffer.append("message : ");
                buffer.append(urlConnection.getResponseMessage()).append("\n");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


        System.out.println("결과 : " + buffer.toString());
        return buffer.toString();
    }
}
