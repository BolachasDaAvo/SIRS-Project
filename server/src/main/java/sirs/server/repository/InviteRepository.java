package sirs.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sirs.server.domain.Invite;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface InviteRepository extends JpaRepository<Invite, Integer> {
}
