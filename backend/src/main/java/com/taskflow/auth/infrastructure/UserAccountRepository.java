package com.taskflow.auth.infrastructure;

import com.taskflow.auth.domain.UserAccount;

import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccount> findByEmail(String email);

    UserAccount save(String email, String displayName, String passwordHash);
}
