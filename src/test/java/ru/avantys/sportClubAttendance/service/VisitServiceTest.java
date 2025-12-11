package ru.avantys.sportClubAttendance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.avantys.sportClubAttendance.exception.MembershipNotFoundException;
import ru.avantys.sportClubAttendance.model.Membership;
import ru.avantys.sportClubAttendance.model.Visit;
import ru.avantys.sportClubAttendance.repository.VisitRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock
    VisitRepository visitRepository;

    @Mock
    MembershipService membershipService;

    @InjectMocks
    VisitService visitService;

    @Test
    void createVisit_success() {
        UUID id = UUID.randomUUID();

        Membership m = new Membership();
        m.setId(id);

        when(membershipService.getMembershipById(id)).thenReturn(Optional.of(m));
        when(visitRepository.findLastVisitByMembershipId(id)).thenReturn(Optional.empty());
        when(visitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Visit v = visitService.createVisit(id, "Z");

        assertEquals(m, v.getMembership());
        assertEquals("Z", v.getZone());
        assertNotNull(v.getEntryTime());
    }

    @Test
    void getVisitsByMembership_success() {
        UUID id = UUID.randomUUID();
        List<Visit> list = List.of(new Visit(), new Visit());

        when(visitRepository.findByMembershipId(id)).thenReturn(list);

        List<Visit> result = visitService.getVisitsByMembership(id);
        assertEquals(2, result.size());
    }

    @Test
    void getVisitCountByMembership_success() {
        UUID id = UUID.randomUUID();
        when(visitRepository.countByMembershipId(id)).thenReturn(5L);

        long count = visitService.getVisitCountByMembership(id);
        assertEquals(5L, count);
    }

    @Test
    void getLastVisitByMembership_success() {
        UUID id = UUID.randomUUID();
        Visit visit = new Visit();

        when(visitRepository.findLastVisitByMembershipId(id)).thenReturn(Optional.of(visit));

        Optional<Visit> result = visitService.getLastVisitByMembership(id);

        assertTrue(result.isPresent());
        assertEquals(visit, result.get());
    }

    @Test
    void recordExit_success() {
        UUID id = UUID.randomUUID();
        Visit visit = new Visit();

        when(visitRepository.findLastVisitByMembershipId(id)).thenReturn(Optional.of(visit));
        when(visitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Visit result = visitService.recordExit(id);

        assertNotNull(result.getExitTime());
        verify(visitRepository).save(visit);
    }

    @Test
    void recordExit_noLastVisit() {
        UUID id = UUID.randomUUID();

        when(visitRepository.findLastVisitByMembershipId(id)).thenReturn(Optional.empty());

        Visit result = visitService.recordExit(id);

        assertNull(result);
        verify(visitRepository, never()).save(any());
    }

    @Test
    void createVisit_nullMembershipId_throwsException() {
        assertThrows(MembershipNotFoundException.class, () ->
                visitService.createVisit(null, "A"));
    }

    @Test
    void createVisit_oldVisitNotClosed() {
        UUID membershipId = UUID.randomUUID();
        Membership membership = new Membership();
        membership.setId(membershipId);

        Visit oldVisit = new Visit();
        oldVisit.setEntryTime(LocalDateTime.now().minusHours(1));
        oldVisit.setExitTime(null);

        when(membershipService.getMembershipById(membershipId)).thenReturn(Optional.of(membership));
        when(visitRepository.findLastVisitByMembershipId(membershipId)).thenReturn(Optional.of(oldVisit));
        when(visitRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        visitService.createVisit(membershipId, "A");

        verify(visitRepository, never()).save(oldVisit);
        assertNull(oldVisit.getExitTime());
    }

    @Test
    void createVisit_noSaveMock_corrected() {
        UUID id = UUID.randomUUID();
        Membership m = new Membership();
        m.setId(id);

        Visit saved = new Visit();
        saved.setId(UUID.randomUUID());
        saved.setEntryTime(LocalDateTime.now());
        saved.setZone("A");

        when(membershipService.getMembershipById(id)).thenReturn(Optional.of(m));
        when(visitRepository.findLastVisitByMembershipId(id)).thenReturn(Optional.empty());
        when(visitRepository.save(any())).thenReturn(saved);

        Visit result = visitService.createVisit(id, "A");

        assertNotNull(result);
        assertNotNull(result.getEntryTime());
        assertEquals("A", result.getZone());

        verify(visitRepository).save(any());
    }


    @Test
    void recordExit_noVisit_returnsVisit_corrected() {
        UUID id = UUID.randomUUID();

        Visit saved = new Visit();
        saved.setId(UUID.randomUUID());
        saved.setExitTime(LocalDateTime.now());

        when(visitRepository.findLastVisitByMembershipId(id)).thenReturn(Optional.empty());
        when(visitRepository.save(any())).thenReturn(saved);

        Visit result = visitService.recordExit(id);

        assertNotNull(result);
        assertNotNull(result.getExitTime());

        verify(visitRepository).save(any());
    }


    @Test
    void getLastVisit_noVisit_returnsEmptyOptional() {
        UUID id = UUID.randomUUID();

        when(visitRepository.findLastVisitByMembershipId(id)).thenReturn(Optional.empty());

        Optional<Visit> result = visitService.getLastVisitByMembership(id);

        assertTrue(result.isEmpty());
    }


}

