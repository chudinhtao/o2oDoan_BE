package com.fnb.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fnb.inventory.enums.StocktakeStatus;
import com.fnb.inventory.entity.Location;

@Entity
@Table(name = "stocktakes", schema = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "items")
public class Stocktake extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StocktakeStatus status;

    @Column(name = "snapshot_time")
    private LocalDateTime snapshotTime;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(length = 255)
    private String name;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "stocktake", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StocktakeItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;
}
