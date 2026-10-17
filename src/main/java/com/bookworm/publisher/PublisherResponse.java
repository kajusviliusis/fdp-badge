package com.bookworm.publisher;

public record PublisherResponse(Long id, String name) {

    public static PublisherResponse from(Publisher publisher) {
        return new PublisherResponse(publisher.getId(), publisher.getName());
    }
}
