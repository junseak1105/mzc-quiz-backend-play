package com.mzc.quiz.play.entity.websocket;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Submit {
    private String quizNum;
    private String answerTime; //미제출자 -1
    private String[] answer; // 유저가 선택한 답안 ["num1", "num2", "num3"] ["O"] ["X"] ["답안"]
    private boolean isAns;
}

/*
1. Submit 메시지 양식
{
  "command": "submit",
  "sender": "알짤딱",
  "quizNum": "12",
  "content": {
    "selected": "1",
    "totalElapsedTime": "2.023",
    "ans": "2"
  }
}
 */
