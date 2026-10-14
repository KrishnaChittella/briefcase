package com.krishna.briefcase.client;

import com.krishna.briefcase.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "client")
@Getter
@Setter
public class Client extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String email;

    private String phone;

    private String address;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
