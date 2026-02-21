package com.example.service;

import com.example.model.User;

/**
 * 用户服务接口
 *
 * @author Example
 */
public interface UserService {

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User findById(Long id);

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建后的用户
     */
    User create(User user);

    /**
     * 获取服务描述
     *
     * @return 服务描述信息
     */
    String getDescription();
}
