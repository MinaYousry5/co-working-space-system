package com.workspace.booking.entity.workspace;

import com.workspace.booking.common.enums.YesNo;
import com.workspace.booking.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "WS_WORKSPACES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Workspace extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TYPE_ID", nullable = false)
    private WorkspaceType workspaceType;

    private String workspaceCode;
    private String workspaceName;

    @Lob
    private String description;

    private Integer floorNumber;
    private String roomNumber;
    private Integer capacity;
    private BigDecimal priceHourly;
    private BigDecimal priceDaily;
    private BigDecimal priceMonthly;
    private String currency;

    @Column(name = "IS_AVAILABLE")
    @Enumerated(EnumType.STRING)  // This stores "Y" or "N" in the database
    private YesNo isAvailable;

    @Column(name = "IS_FEATURED")
    @Enumerated(EnumType.STRING)  // This stores "Y" or "N" in the database
    private YesNo isFeatured;

    private Integer minBookingHrs;
    private Integer maxBookingHrs;
    private Integer advanceNotice;
    private Integer cancellationHrs;

}