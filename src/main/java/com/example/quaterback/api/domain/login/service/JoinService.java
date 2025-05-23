package com.example.quaterback.api.domain.login.service;

import com.example.quaterback.common.exception.DuplicateJoinException;
import com.example.quaterback.api.domain.login.domain.UserDomain;
import com.example.quaterback.api.domain.login.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JoinService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public JoinService(@Qualifier("jpaUserRepository") UserRepository userRepository,
                       BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Transactional
    public String joinProcess(String username, String password) {

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateJoinException("이미 존재하는 ID입니다.");
        }

        UserDomain userDomain = UserDomain.createUserDomain(username, bCryptPasswordEncoder.encode(password));

        UserDomain returnUserDomain = userRepository.save(userDomain);
        String returnUsername = returnUserDomain.getUsername();
        return returnUsername;

    }
}
