package com.example.inventory;

import com.example.inventory.InventoryDtos.HistoryResponse;
import com.example.inventory.InventoryDtos.InventoryResponse;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
    private final InventoryRepository repository;
    private final EntityManager entityManager;

    public InventoryService(InventoryRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void initializeDemoInventory() {
        if (!repository.existsById(1L)) {
            // Persist through Hibernate so Envers also records the initial ADD revision.
            repository.save(new Inventory(1L, "Demo product", 100L));
        }
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(long productId) {
        return InventoryResponse.from(find(productId));
    }

    @Transactional(timeout = 10)
    public InventoryResponse stockIn(long productId, long amount) {
        // PostgreSQL transaction-local timeout; do not rely on a non-portable lock hint.
        entityManager.createNativeQuery("SELECT set_config('lock_timeout', '3s', true)")
                .getSingleResult();
        Inventory inventory = repository.findForStockIn(productId)
                .orElseThrow(() -> notFound(productId));
        try {
            inventory.stockIn(amount);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Quantity would exceed the supported integer range");
        }
        repository.flush();
        return InventoryResponse.from(inventory);
    }

    @Transactional(timeout = 10)
    public InventoryResponse update(long productId, String productName, Long expectedVersion) {
        Inventory inventory = find(productId);
        if (!Objects.equals(inventory.getVersion(), expectedVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stale version. Reload the inventory before editing again");
        }
        inventory.rename(productName.strip());
        // Hibernate's version predicate detects another commit after the check above.
        // Flush before building the DTO so the returned version reflects the UPDATE.
        repository.flush();
        return InventoryResponse.from(inventory);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(long productId, int offset, int limit) {
        List<?> revisions = AuditReaderFactory.get(entityManager).createQuery()
                .forRevisionsOfEntity(Inventory.class, false, true)
                .add(AuditEntity.id().eq(productId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        List<HistoryResponse> result = new ArrayList<>(revisions.size());
        for (Object revisionRow : revisions) {
            Object[] row = (Object[]) revisionRow;
            Inventory inventory = (Inventory) row[0];
            InventoryRevision revision = (InventoryRevision) row[1];
            RevisionType type = (RevisionType) row[2];
            result.add(new HistoryResponse(revision.getId(),
                    Instant.ofEpochMilli(revision.getTimestamp()), type.name(),
                    inventory.getProductId(), inventory.getProductName(),
                    inventory.getQuantity(), inventory.getVersion()));
        }
        return result;
    }

    private Inventory find(long productId) {
        return repository.findById(productId).orElseThrow(() -> notFound(productId));
    }

    private ResponseStatusException notFound(long productId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Inventory not found: " + productId);
    }
}
