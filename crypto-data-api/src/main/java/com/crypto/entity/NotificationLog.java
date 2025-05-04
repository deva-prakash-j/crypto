package com.crypto.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications_log",
    indexes = {@Index(name = "idx_notifications_signal", columnList = "signal_id"),
        @Index(name = "idx_notifications_channel_time", columnList = "channel, sentAt DESC")})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationLog {

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "signal_id", nullable = false)
  private Signal signal;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Channel channel; // TELEGRAM, EMAIL

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Status status; // SENT, FAILED

  @Column(nullable = false)
  private LocalDateTime sentAt;

  public enum Channel {
    TELEGRAM, EMAIL
  }

  public enum Status {
    SENT, FAILED
  }
}
