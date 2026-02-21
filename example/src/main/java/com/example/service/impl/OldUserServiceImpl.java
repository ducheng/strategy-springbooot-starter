package com.example.service.impl;

import com.example.model.User;
import com.example.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 老的用户服务实现
 *
 * <p>这是项目原有的实现，我们不修改这个类
 *
 * @author Example
 */
@Slf4j
@Service("oldUserService")
public class OldUserServiceImpl implements UserService {

    /**
     * 模拟数据库
     */
    private final Map<Long, User> database = new HashMap<Long, User>() {{
        put(1L, new User(1L, "old-user-1", "old@example.com", "OLD_DATABASE"));
        put(2L, new User(2L, "old-user-2", "old2@example.com", "OLD_DATABASE"));
        put(3L, new User(3L, "old-user-3", "old3@example.com", "OLD_DATABASE"));
    }};

    @Override
    public User findById(Long id) {
        log.info("【老实现】正在查询用户，ID: {}", id);
        User user = database.get(id);
        if (user != null) {
            user.setSource("OLD_IMPLEMENTATION");
        }
        return user;
    }

    @Override
    public User create(User user) {
        log.info("【老实现】正在创建用户: {}", user);
        user.setId(System.currentTimeMillis());
        user.setSource("OLD_IMPLEMENTATION");
        database.put(user.getId(), user);
        return user;
    }

    @Override
    public String getDescription() {
        return "老的用户服务实现 - 基于本地HashMap存储";
    }
}
