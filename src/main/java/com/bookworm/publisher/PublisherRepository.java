package com.bookworm.publisher;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    boolean existsByNameIgnoreCase(String name);
}
