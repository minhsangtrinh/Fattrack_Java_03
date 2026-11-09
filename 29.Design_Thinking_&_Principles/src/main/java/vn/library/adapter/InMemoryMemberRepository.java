package vn.library.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import vn.library.domain.Member;
import vn.library.port.MemberRepository;

public final class InMemoryMemberRepository implements MemberRepository {
    private final Map<UUID, Member> members = new HashMap<>();

    @Override
    public void save(Member member) {
        Objects.requireNonNull(member, "Member is required");
        members.put(member.id(), member);
    }

    @Override
    public Optional<Member> findById(UUID id) {
        return Optional.ofNullable(members.get(Objects.requireNonNull(id)));
    }
}
