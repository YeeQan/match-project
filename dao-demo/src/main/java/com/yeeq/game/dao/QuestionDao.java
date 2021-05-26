package com.yeeq.game.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yeeq.game.mapper.QuestionMapper;
import com.yeeq.game.entity.Question;
import com.yeeq.game.redis.EnumRedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author yeeq
 * @date 2021/2/28
 */
@Repository
public class QuestionDao {

    @Autowired
    private QuestionMapper mapper;

    /**
     * 从数据库获取所有 question
     */
    public List<Question> getAllQuestion() {
        return mapper.selectList(null);
    }
}
