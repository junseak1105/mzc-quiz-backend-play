package com.mzc.quiz.play.entity.websocket;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class UserRank {
    int rank;
    String nickName;
    Double rankScore;
}
