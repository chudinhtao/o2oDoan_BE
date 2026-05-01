package com.fnb.menu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "item_option_groups", schema = "menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "options")
public class ItemOptionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private MenuItem item;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String type;            // SINGLE / MULTI

    @Column(name = "is_required")
    @Builder.Default
    private boolean isRequired = false;

    @Column(name = "display_order")
    @Builder.Default
    private int displayOrder = 0;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 30)  // Hibernate batch-load options thay vì JOIN FETCH — tránh MultipleBagFetchException
    @Builder.Default
    private List<ItemOption> options = new ArrayList<>();
}
