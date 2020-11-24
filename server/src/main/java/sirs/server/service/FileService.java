package sirs.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sirs.server.domain.File;
import sirs.server.domain.User;
import sirs.server.repository.FileRepository;
import sirs.server.repository.UserRepository;

import java.sql.SQLException;

@Service
public class FileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void createFile(int ownerId, String fileName, String path) {
        //TODO: figure out file path
        User owner = userRepository.findById(ownerId).orElseThrow();
        File file = new File(1, owner, fileName, path);
        file.addCollaborator(owner);
        file.setLastModifier(owner);
        owner.addFile(file);
        fileRepository.save(file);
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public File getFile(String name) {
        File file = fileRepository.findByName(name).orElse(null);
        return file;
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updateFileContent() {
        //TODO: decide how we want to do this since files may be very big and not fit in memory
        // we should do it by batches, just need to decide where.
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updateVersion(String name) {
        File file = getFile(name);
        file.incrementVersion();
    }


    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deleteFile(int fileId) {
        fileRepository.deleteById(fileId);
    }

    @Retryable(value = {SQLException.class}, backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void addCollaborator(int fileId, int userId) {
        File file = fileRepository.findById(fileId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        file.addCollaborator(user);
        user.addFile(file);
    }
}
