package com.bajaj.rcs.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "handler_leave")
public class HandlerLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handler_id")
    private ClaimHandler handler;

    @Column(nullable = false)
    private LocalDateTime leaveFrom;

    @Column(nullable = false)
    private LocalDateTime leaveTo;

    @Column(nullable = false)
    private boolean halfDay;

    protected HandlerLeave() { }

    public HandlerLeave(ClaimHandler handler, LocalDateTime leaveFrom, LocalDateTime leaveTo, boolean halfDay) {
        this.handler = handler;
        this.leaveFrom = leaveFrom;
        this.leaveTo = leaveTo;
        this.halfDay = halfDay;
    }

    public Long getId()                 { return id; }
    public LocalDateTime getLeaveFrom() { return leaveFrom; }
    public LocalDateTime getLeaveTo()   { return leaveTo; }
    public boolean isHalfDay()          { return halfDay; }
}
