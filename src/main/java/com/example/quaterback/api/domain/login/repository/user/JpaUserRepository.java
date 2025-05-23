package com.example.quaterback.api.domain.login.repository.user;

import com.example.quaterback.api.domain.login.domain.UserDomain;
import com.example.quaterback.api.domain.login.entity.UserEntity;
import org.springframework.stereotype.Repository;

@Repository("jpaUserRepository")
public class JpaUserRepository implements UserRepository {

    private final SpringDataJpaUserRepository springDataJpaUserRepository;

    public JpaUserRepository(SpringDataJpaUserRepository springDataJpaUserRepository) {
        this.springDataJpaUserRepository = springDataJpaUserRepository;
    }

    @Override
    public Boolean existsByUsername(String username) {
        return springDataJpaUserRepository.existsByUsername(username);
    }

    @Override
    public UserEntity findByUsername(String username) {
        return springDataJpaUserRepository.findByUsername(username);
    }

    @Override
    public UserDomain save(UserDomain userDomain) {

        UserEntity userEntity = UserEntity.from(userDomain);
        UserEntity resultUserEntity = springDataJpaUserRepository.save(userEntity);
        return resultUserEntity.toDomain();
    }
}
