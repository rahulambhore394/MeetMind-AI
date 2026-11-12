package com.meetmind.meetmind_backend.invitation;

import com.meetmind.meetmind_backend.common.SecurityUtils;
import com.meetmind.meetmind_backend.invitation.dto.InvitationDto;
import com.meetmind.meetmind_backend.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /**
     * Get all invitations for the authenticated user.
     */
    @GetMapping("/my-invitations")
    public ResponseEntity<List<InvitationDto>> getMyInvitations(Authentication authentication) {
        User currentUser = SecurityUtils.extractUser(authentication);
        return ResponseEntity.ok(invitationService.getMyInvitations(currentUser.getId()));
    }

    /**
     * Get invitation details by meeting code.
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<InvitationDto> getInvitationByCode(
            @PathVariable String code,
            Authentication authentication
    ) {
        Long currentUserId = authentication != null ? SecurityUtils.extractUser(authentication).getId() : null;
        return ResponseEntity.ok(invitationService.getInvitationByCode(code, currentUserId));
    }

    /**
     * Public endpoint to fetch basic invitation preview without authentication.
     */
    @GetMapping("/public/{code}")
    public ResponseEntity<InvitationDto> getPublicInvitationPreview(@PathVariable String code) {
        return ResponseEntity.ok(invitationService.getInvitationByCode(code, null));
    }

    /**
     * Accept invitation by meeting ID.
     */
    @PostMapping("/{meetingId}/accept")
    public ResponseEntity<InvitationDto> acceptInvitation(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User currentUser = SecurityUtils.extractUser(authentication);
        return ResponseEntity.ok(invitationService.acceptInvitation(meetingId, currentUser.getId()));
    }

    /**
     * Decline invitation by meeting ID.
     */
    @PostMapping("/{meetingId}/decline")
    public ResponseEntity<InvitationDto> declineInvitation(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User currentUser = SecurityUtils.extractUser(authentication);
        return ResponseEntity.ok(invitationService.declineInvitation(meetingId, currentUser.getId()));
    }

    /**
     * Accept invitation by meeting code (e.g. from deep link or web redirect).
     */
    @PostMapping("/code/{code}/accept")
    public ResponseEntity<InvitationDto> acceptInvitationByCode(
            @PathVariable String code,
            Authentication authentication
    ) {
        User currentUser = SecurityUtils.extractUser(authentication);
        return ResponseEntity.ok(invitationService.acceptInvitationByCode(code, currentUser.getId()));
    }
}

