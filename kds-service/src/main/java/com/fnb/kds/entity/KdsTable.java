package com.fnb.kds.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tables", schema = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KdsTable {
    @Id
    private UUID id;

    @Column(name = "number")
    private Integer number;
}
