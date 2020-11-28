package sirs.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sirs.server.domain.File;
import sirs.server.domain.Invite;
import sirs.server.domain.User;
import sirs.server.repository.FileRepository;
import sirs.server.repository.InviteRepository;
import sirs.server.repository.UserRepository;

import java.sql.SQLException;

@Service
public class InviteService {

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Invite createInvite(int userId, int fileId, byte[] fileKey) {
        User user = userRepository.findById(userId).orElseThrow();
        File file = fileRepository.findById(fileId).orElseThrow();

        Invite invite = new Invite(user, file, fileKey);
        user.addInvite(invite);

        inviteRepository.save(invite);
        return invite;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Invite getInvite(int inviteId) {
        Invite invite = inviteRepository.findById(inviteId).orElseThrow();

        return invite;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Invite getInviteByUser(String fileName, int userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return user.getInviteByFileName(fileName);
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void removeInvite(int inviteId) {
        inviteRepository.deleteById(inviteId);
    }
}
