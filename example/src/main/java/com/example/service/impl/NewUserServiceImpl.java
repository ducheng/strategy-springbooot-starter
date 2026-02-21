package com.example.service.impl;

import com.example.model.User;
import com.example.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 新的用户服务实现
 *
 * <p>这是对接第三方的新实现，通过动态路由在特定条件下调用
 *
 * @author Example
 */
@Slf4j
@Service("newUserService")
public class NewUserServiceImpl implements UserService {

    /**
     * 模拟第三方数据库
     */
    private final Map<Long, User> thirdPartyDatabase = new HashMap<Long, User>() {{
        put(1L, new User(1L, "new-user-1", "new@third-party.com", "THIRD_PARTY_API"));
        put(2L, new User(2L, "new-user-2", "new2@third-party.com", "THIRD_PARTY_API"));
        put(4L, new User(4L, "new-user-4", "new4@third-party.com", "THIRD_PARTY_API"));
        put(5L, new User(5L, "new-user-5", "new5@third-party.com", "THIRD_PARTY_API"));
    }};

    @Override
    public User findById(Long id) {
        log.info("【新实现】正在从第三方API查询用户，ID: {}", id);
        User user = thirdPartyDatabase.get(id);
        if (user != null) {
            user.setSource("NEW_IMPLEMENTATION_THIRD_PARTY");
        }
        return user;
    }

    @Override
    public User create(User user) {
        log.info("【新实现】正在通过第三方API创建用户: {}", user);
        user.setId(System.currentTimeMillis());
        user.setSource("NEW_IMPLEMENTATION_THIRD_PARTY");
        thirdPartyDatabase.put(user.getId(), user);
        return user;
    }

    @Override
    public String getDescription() {
        return "新的用户服务实现 - 对接第三方API";
    }
}
