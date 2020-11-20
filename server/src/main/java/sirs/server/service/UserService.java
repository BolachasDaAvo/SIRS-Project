package sirs.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sirs.server.domain.User;
import sirs.server.repository.InviteRepository;
import sirs.server.repository.UserRepository;

import java.sql.SQLException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InviteRepository inviteRepository;

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public User createUser(String username, byte[] certificate) {
        User user = new User(username, certificate);
        userRepository.save(user);
        return user;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public User getUserById(int id) {
        User user = userRepository.findById(id).orElseThrow();
        return user;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public User getUserByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return user;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public User updateUserUsername(int id, String username) {
        User user = userRepository.findById(id).orElseThrow();
        user.setUsername(username);
        return user;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public User updateUserCertificate(int id, byte[] certificate) {
        User user = userRepository.findById(id).orElseThrow();
        user.setCertificate(certificate);
        return user;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void removeUser(int id) {
        userRepository.deleteById(id);
    }

}
