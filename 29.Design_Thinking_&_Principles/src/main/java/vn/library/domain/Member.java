package vn.library.domain;

import java.util.Objects;
import java.util.UUID;

public record Member(UUID id, String name) {
    public Member {
        Objects.requireNonNull(id, "Member ID is required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Member name is required");
        }
        name = name.strip();
    }
}
