package com.bookworm.publisher;

import java.util.List;

import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ResourceNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PublisherService {

    private final PublisherRepository publisherRepository;

    public PublisherService(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }

    public List<PublisherResponse> findAll() {
        return publisherRepository.findAll(Sort.by("name")).stream()
                .map(PublisherResponse::from)
                .toList();
    }

    public PublisherResponse findById(Long id) {
        return publisherRepository.findById(id)
                .map(PublisherResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Publisher", id));
    }

    @Transactional
    public PublisherResponse create(PublisherRequest request) {
        String name = request.name().strip();
        if (publisherRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Publisher '%s' already exists.".formatted(name));
        }
        return PublisherResponse.from(publisherRepository.save(new Publisher(name)));
    }
}
