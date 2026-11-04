package com.example.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

@Entity
@Table(name = "revinfo")
@RevisionEntity
public class InventoryRevision extends DefaultRevisionEntity {}
