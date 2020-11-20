package sirs.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sirs.server.domain.File;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface FileRepository extends JpaRepository<File, Integer> {
}
