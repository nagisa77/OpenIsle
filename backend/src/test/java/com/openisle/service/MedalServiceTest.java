package com.openisle.service;

import com.openisle.dto.MedalDto;
import com.openisle.model.MedalType;
import com.openisle.model.User;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedalServiceTest {
    @Test
    void getMedalsWithoutUser() {
        CommentRepository commentRepo = mock(CommentRepository.class);
        PostRepository postRepo = mock(PostRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ContributorService contributorService = mock(ContributorService.class);

        MedalService service = new MedalService(commentRepo, postRepo, userRepo, contributorService);

        List<MedalDto> medals = service.getMedals(null);
        medals.forEach(m -> assertFalse(m.isCompleted()));
        assertEquals(5, medals.size());
    }

    @Test
    void getMedalsWithUser() {
        CommentRepository commentRepo = mock(CommentRepository.class);
        PostRepository postRepo = mock(PostRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ContributorService contributorService = mock(ContributorService.class);

        when(commentRepo.countByAuthor_Id(1L)).thenReturn(120L);
        when(postRepo.countByAuthor_Id(1L)).thenReturn(80L);
        when(contributorService.getContributionLines(anyString())).thenReturn(0L);
        when(userRepo.countByCreatedAtBefore(any())).thenReturn(50L);
        User user = new User();
        user.setId(1L);
        user.setCreatedAt(LocalDateTime.of(2025, 9, 15, 0, 0));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findByUsername("user")).thenReturn(Optional.of(user));

        MedalService service = new MedalService(commentRepo, postRepo, userRepo, contributorService);
        List<MedalDto> medals = service.getMedals(1L);

        assertEquals(MedalType.COMMENT, user.getDisplayMedal());
        assertTrue(medals.stream().filter(m -> m.getType() == MedalType.COMMENT).findFirst().orElseThrow().isCompleted());
        assertTrue(medals.stream().filter(m -> m.getType() == MedalType.COMMENT).findFirst().orElseThrow().isSelected());
        assertFalse(medals.stream().filter(m -> m.getType() == MedalType.POST).findFirst().orElseThrow().isCompleted());
        assertFalse(medals.stream().filter(m -> m.getType() == MedalType.POST).findFirst().orElseThrow().isSelected());
        assertTrue(medals.stream().filter(m -> m.getType() == MedalType.SEED).findFirst().orElseThrow().isCompleted());
        assertFalse(medals.stream().filter(m -> m.getType() == MedalType.SEED).findFirst().orElseThrow().isSelected());
        assertTrue(medals.stream().filter(m -> m.getType() == MedalType.PIONEER).findFirst().orElseThrow().isCompleted());
        assertFalse(medals.stream().filter(m -> m.getType() == MedalType.PIONEER).findFirst().orElseThrow().isSelected());
        verify(userRepo).save(user);
    }

    @Test
    void selectMedal() {
        CommentRepository commentRepo = mock(CommentRepository.class);
        PostRepository postRepo = mock(PostRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ContributorService contributorService = mock(ContributorService.class);

        when(commentRepo.countByAuthor_Id(1L)).thenReturn(120L);
        when(postRepo.countByAuthor_Id(1L)).thenReturn(0L);
        when(contributorService.getContributionLines(anyString())).thenReturn(0L);
        when(userRepo.countByCreatedAtBefore(any())).thenReturn(0L);
        User user = new User();
        user.setId(1L);
        user.setCreatedAt(LocalDateTime.of(2025, 9, 15, 0, 0));
        when(userRepo.findByUsername("user")).thenReturn(Optional.of(user));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        MedalService service = new MedalService(commentRepo, postRepo, userRepo, contributorService);
        service.selectMedal("user", MedalType.COMMENT);
        assertEquals(MedalType.COMMENT, user.getDisplayMedal());
    }

    @Test
    void selectMedalNotCompleted() {
        CommentRepository commentRepo = mock(CommentRepository.class);
        PostRepository postRepo = mock(PostRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ContributorService contributorService = mock(ContributorService.class);

        when(commentRepo.countByAuthor_Id(1L)).thenReturn(10L);
        when(postRepo.countByAuthor_Id(1L)).thenReturn(0L);
        when(contributorService.getContributionLines(anyString())).thenReturn(0L);
        when(userRepo.countByCreatedAtBefore(any())).thenReturn(0L);
        User user = new User();
        user.setId(1L);
        user.setCreatedAt(LocalDateTime.of(2025, 9, 15, 0, 0));
        when(userRepo.findByUsername("user")).thenReturn(Optional.of(user));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        MedalService service = new MedalService(commentRepo, postRepo, userRepo, contributorService);
        assertThrows(IllegalArgumentException.class, () -> service.selectMedal("user", MedalType.COMMENT));
    }
}
