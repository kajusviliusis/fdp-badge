package com.bookworm.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublisherServiceTest {

    @Mock
    private PublisherRepository publisherRepository;

    @InjectMocks
    private PublisherService publisherService;

    @Test
    void createStripsNameAndSaves() {
        given(publisherRepository.existsByNameIgnoreCase("ABC")).willReturn(false);
        given(publisherRepository.save(any(Publisher.class))).willAnswer(i -> i.getArgument(0));

        PublisherResponse response = publisherService.create(new PublisherRequest("  ABC  "));

        assertThat(response.name()).isEqualTo("ABC");
    }

    @Test
    void createRejectsDuplicateName() {
        given(publisherRepository.existsByNameIgnoreCase("ABC")).willReturn(true);

        assertThatThrownBy(() -> publisherService.create(new PublisherRequest("ABC")))
                .isInstanceOf(ConflictException.class);
        verify(publisherRepository, never()).save(any());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        given(publisherRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> publisherService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Publisher with id 1 was not found.");
    }
}
