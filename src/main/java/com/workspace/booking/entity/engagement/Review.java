package com.workspace.booking.entity.engagement;

import com.workspace.booking.common.enums.*;
import com.workspace.booking.entity.BaseEntity;
import com.workspace.booking.entity.booking.Booking;
import com.workspace.booking.entity.identity.User;
import com.workspace.booking.entity.workspace.Workspace;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "WS_REVIEWS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "WORKSPACE_ID")
    private Workspace workspace;

    @ManyToOne @JoinColumn(name = "USER_ID")
    private User user;

    @ManyToOne @JoinColumn(name = "BOOKING_ID")
    private Booking booking;

    @Column(name = "RATING", nullable = false)
    private Double rating;

    @Column(name = "TITLE")
    private String title;

    @Lob
    @Column(name = "BODY")
    private String body;

    @Column(name = "IS_VERIFIED", nullable = false)
    private Integer isVerified;

    @Column(name = "IS_PUBLISHED", nullable = false)
    private Integer isPublished;
}
