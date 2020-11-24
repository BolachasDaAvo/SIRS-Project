package sirs.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sirs.server.domain.File;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
@Transactional
public interface FileRepository extends JpaRepository<File, Integer> {

    @Query(value = "SELECT * FROM file WHERE name = :name", nativeQuery = true)
    Optional<File> findByName(String name);
}
