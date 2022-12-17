package com.mzc.quiz.play.util;

import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisUtil {

    private RedisTemplate redisTemplate;

    private ValueOperations<String, String> stringOperation;
    private HashOperations<String, Object, Object> hashOperations;
    private SetOperations<String, String> setOperations;
    private ZSetOperations<String, String> zSetOperations;
    private ListOperations<String,String> listOperations;

    public RedisUtil(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringOperation = redisTemplate.opsForValue();
        this.hashOperations = redisTemplate.opsForHash();
        this.setOperations = redisTemplate.opsForSet();
        this.zSetOperations = redisTemplate.opsForZSet();
        this.listOperations = redisTemplate.opsForList();
    }

    // [ Gen Key ]

    // PIN입력시 PLAY 키 반환
    public String genKey(String pin) {
        return "PLAY" + ":" + pin;
    }

    // PIN입력시 Answer 키 반환
    public String genKey_ans(String pin, String quizNum) {
        return "ANS" + quizNum + ":" + pin;
    }

    public String genKey(String name, String pin){
        return name + ":" + pin;
    }

    // --------------------------------------------------------------------------
    // [ Generic ]
    // 해당 키가 존재하는지 체크
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }


    public Boolean expire(String key, long expireTime, TimeUnit timeUnit) {
        return redisTemplate.expire(key, expireTime, timeUnit);
    }

    // 키 삭제
    public Boolean DEL(String key) {
        return redisTemplate.delete(key);
    }


    // --------------------------------------------------------------------------
    // [ String ]
    // key값에 따른 데이터 가져오기
    public String getData(String key) {
        return stringOperation.get(key);
    }

    // 데이터 저장
    public void SET(String key, String value) {
        stringOperation.set(key, value);
    }

    // 만료기간,데이터 저장
    public void setDataExpire(String key, String value, long duration) {
        Duration expireDuration = Duration.ofSeconds(duration);
        stringOperation.set(key, value, expireDuration);
    }

    // 데이터 삭제
    public void deleteData(String key) {
        redisTemplate.delete(key);
    }


    // ----------------------------------------------------------------------------------------
    // [ Hash ]
    // - 유저 각각 문제별 결과 저장

    // Hash 데이터 저장
    public void setHashData(String key, String field, String value) {
        hashOperations.put(key, field, value);
    }

    // Hash 데이터 가져오기
    public Object GetHashData(String key, String field) {
        return hashOperations.get(key, field);
    }

    // Hash 데이터 전부 가져오기
    public Map<Object, Object> GetAllHashData(String key) {
        return hashOperations.entries(key);
    }

    // 해당 Hash에 키값이 있는지 체크
    public Boolean hasKey(String key, String field) {
        return hashOperations.hasKey(key, field);
    }

    // 해당 Hash의 field 삭제
    public Long deleteHashData(String key, String field) {
        return hashOperations.delete(key, field);
    }


    // -----------------------------------------------------------------
    // [ Set ]
    // - 유저 닉네임

    // Set 데이터 저장
    public void SADD(String key, String value) {
        setOperations.add(key, value);
    }

    // Set 데이터 삭제
    public Long SPOP(String key, String value) {
        return setOperations.remove(key, value);
    }

    // Set 데이터에 저장된 데이터 수
    public Long sizeSetData(String key) {
        return setOperations.size(key);
    }

    // Set 데이터 전부 가져오기
    public Set<String> SMEMBERS(String key) {
        return setOperations.members(key);
    }

    // 삭제된 member수를 리턴, 버전 2.4 이후 부터 여러 개의 member를 받을 수 있다
    // key에 저장된 집합에서 지정된 멤버를 제거, 해당 멤버가 아닌 경우 무시됨,
    // key가 없으면 빈 세트로 처리되고 명령은 0을 리턴, key에 저장된 값이 집합이 아닌 경우 오류 반환
    public Long SREM(String key, String nickname) {
        return setOperations.remove(key,nickname);
    }

    // Set - Returns if member is a member of the set stored at key.
    // true - exist
    // false - not exist
    public boolean SISMEMBER(String key, String value) {

        return setOperations.isMember(key, value);
    }

    public Long setDataSize(String key){
        return setOperations.size(key);
    }
    // -------------------------------------------------------------

    // [ Sorted Set ]
    // - 종합 랭킹
    // Sorted Set 데이터 추가
    // 해당 데이터가 없으면 추가하고, 있으면 점수 업데이트
    public void setZData(String key, String value, double score) {
        zSetOperations.add(key, value, score);
    }

    public Set<ZSetOperations.TypedTuple<String>> getRanking(String key, long startIndex, long endIndex) {
        return zSetOperations.reverseRangeWithScores(key, startIndex, endIndex);
    }

    // 점수 증가시키기
    public Double plusScore(String key, String nickname, Double score) {
        return zSetOperations.incrementScore(key, nickname, score);
    }

    // 점수 가져오기
    public Double getScore(String key, Object nickname) {
        return zSetOperations.score(key, nickname);
    }

    // Sorted Set 데이터 삭제
    public Long removeZData(String key, Object value) {
        return zSetOperations.remove(key, value);
    }

    // -------------------------------------------------------------

    // [ List ]
    // - 유저의 모든 로그 저장
    public List leftPop(String key, long count){
        return listOperations.leftPop(key,count);
    }

    public Long leftPush(String key, String value){
        return listOperations.leftPush(key, value);
    }

    public void listDataSet(String key, long index, String value){
        listOperations.set(key,index,value);
    }
    // 리스트 크기
    public Long listSize(String key){
        return listOperations.size(key);
    }

    // ----------------------------------------------------------------
    // [Custom]


    // Set에서 유저 목록 조회 후 리턴
    public List<String> getUserList(String pinNum){


        Set<String> setUserList = setOperations.members(genKey(RedisPrefix.USER.name(), pinNum));

        List<String> stringUserList = new ArrayList<>();
        stringUserList.addAll(setUserList);

        return stringUserList;
    }

}
