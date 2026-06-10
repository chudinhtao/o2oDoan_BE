package com.fnb.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance_logs", schema = "auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private WorkSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "check_in")
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    @Column(name = "check_in_note")
    private String checkInNote;

    @Column(name = "check_out_note")
    private String checkOutNote;

    @Column(name = "is_late")
    @Builder.Default
    private boolean late = false;

    @Column(name = "is_early_leave")
    @Builder.Default
    private boolean earlyLeave = false;

    @Column(name = "late_minutes")
    private Integer lateMinutes;

    @Column(name = "early_leave_minutes")
    private Integer earlyLeaveMinutes;

    @Column(name = "ot_minutes")
    private Integer otMinutes;

    public Integer getLateMinutes() {
        return lateMinutes != null ? lateMinutes : 0;
    }

    public Integer getEarlyLeaveMinutes() {
        return earlyLeaveMinutes != null ? earlyLeaveMinutes : 0;
    }

    public Integer getOtMinutes() {
        return otMinutes != null ? otMinutes : 0;
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
