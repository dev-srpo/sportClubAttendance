package ru.avantys.sportClubAttendance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.avantys.sportClubAttendance.dto.AccessRuleDto;
import ru.avantys.sportClubAttendance.exception.AccessRuleNotFoundException;
import ru.avantys.sportClubAttendance.exception.MembershipNotFoundException;
import ru.avantys.sportClubAttendance.model.AccessRule;
import ru.avantys.sportClubAttendance.model.Membership;
import ru.avantys.sportClubAttendance.repository.AccessRuleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccessService {
    private final AccessRuleRepository accessRuleRepository;
    private final MembershipService membershipService;

    public AccessService(AccessRuleRepository accessRuleRepository, MembershipService membershipService) {
        this.accessRuleRepository = accessRuleRepository;
        this.membershipService = membershipService;
    }

    public AccessRule createAccessRule(AccessRuleDto accessRuleDto, UUID membershipId) {
        Membership membership = membershipService.getMembershipById(membershipId)
                .orElseThrow(() -> new MembershipNotFoundException("Membership not found with id: " + membershipId));

        AccessRule accessRule = AccessRuleDto.toAccessRule(accessRuleDto, membership);

        return accessRuleRepository.save(accessRule);
    }

    @Transactional(readOnly = true)
    public List<AccessRule> getAccessRulesByMembership(UUID membershipId) {
        return accessRuleRepository.findByMembershipId(membershipId);
    }

    public void deleteAccessRule(UUID id) {
        if (!accessRuleRepository.existsById(id)) {
            throw new AccessRuleNotFoundException("AccessRule not found with id: " + id);
        }

        accessRuleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean checkAccessRule(UUID membershipId, String zone) {
        List<AccessRule> accessRuleList = getAccessRulesByMembership(membershipId);

        //if (!membershipService.isActiveMembership(membershipId)) return false;

        AccessRule accessRule = accessRuleList.stream()
                .filter(this::isAccessRuleValid)
                .max(Comparator.comparing(AccessRule::getPriority))
                .orElse(null);

        return accessRule != null && accessRule.getZones().contains(zone);
    }

    private boolean isAccessRuleValid(AccessRule accessRule) {
        LocalTime currentTime = LocalTime.now();
        if (currentTime.isBefore(accessRule.getValidFromTime()) ||
                currentTime.isAfter(accessRule.getValidToTime())) {
            return false;
        }

        String currentDayValue = String.valueOf(LocalDate.now().getDayOfWeek().getValue());
        String allowedDays = accessRule.getAllowedDays();
        return !allowedDays.contains(currentDayValue);
    }
}