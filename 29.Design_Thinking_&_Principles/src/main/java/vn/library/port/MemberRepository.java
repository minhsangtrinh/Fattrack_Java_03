package vn.library.port;

import java.util.Optional;
import java.util.UUID;
import vn.library.domain.Member;

/** Saves by ID; findById returns empty when the member does not exist. */
public interface MemberRepository {
    void save(Member member);
    Optional<Member> findById(UUID id);
}
