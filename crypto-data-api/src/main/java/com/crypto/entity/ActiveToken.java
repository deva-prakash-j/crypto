package com.crypto.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "active_tokens", uniqueConstraints = {@UniqueConstraint(columnNames = {"pair"})})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveToken {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private String pair;

  @Column(nullable = false)
  private String baseCurrency;

  @Column(nullable = false)
  private String quoteCurrency;

  @Column(nullable = false)
  private String market;
  
  private Boolean backfillDay;
  private Boolean backfillHour;
  private Boolean backfillMinute;

}
