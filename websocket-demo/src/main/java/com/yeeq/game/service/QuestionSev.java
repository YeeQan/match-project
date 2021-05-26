package com.yeeq.game.service;

import com.yeeq.game.dao.QuestionDao;
import com.yeeq.game.entity.Question;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yeeq
 * @date 2021/5/1
 */
@Slf4j
@Service
public class QuestionSev {

    @Autowired
    private QuestionDao questionDao;

    /**
     * 获取问题
     */
    public List<Question> getAllQuestion() {
        return questionDao.getAllQuestion();

    }
}
