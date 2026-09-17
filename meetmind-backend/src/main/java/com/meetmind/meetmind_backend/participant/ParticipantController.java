package com.meetmind.meetmind_backend.participant;



import com.meetmind.meetmind_backend.participant.dto.BatchInviteRequest;
import com.meetmind.meetmind_backend.participant.dto.BatchInviteResponse;
import com.meetmind.meetmind_backend.participant.dto.InviteParticipantRequest;
import com.meetmind.meetmind_backend.participant.dto.ParticipantResponse;
import com.meetmind.meetmind_backend.user.User;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings/{meetingId}/participants")
public class ParticipantController {

    private final ParticipantService participantService;


    public ParticipantController(
            ParticipantService participantService
    ) {

        this.participantService =
                participantService;
    }


    // =====================================================
    // INVITE
    // =====================================================

    @PostMapping
    public ResponseEntity<ParticipantResponse>
    inviteParticipant(

            @PathVariable Long meetingId,

            @Valid
            @RequestBody
            InviteParticipantRequest request,

            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();


        ParticipantResponse response =
                participantService.inviteParticipant(

                        meetingId,

                        request,

                        currentUser.getId()
                );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchInviteResponse> inviteParticipantsBatch(
            @PathVariable Long meetingId,
            @Valid @RequestBody BatchInviteRequest request,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        BatchInviteResponse response = participantService.inviteParticipantsBatch(
                meetingId,
                request,
                currentUser.getId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =====================================================
    // GET PARTICIPANTS
    // =====================================================

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>>
    getParticipants(

            @PathVariable Long meetingId,

            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();


        return ResponseEntity.ok(

                participantService.getParticipants(

                        meetingId,

                        currentUser.getId()
                )
        );
    }


    // =====================================================
    // ACCEPT
    // =====================================================

    @PatchMapping("/{userId}/accept")
    public ResponseEntity<ParticipantResponse>
    acceptInvitation(

            @PathVariable Long meetingId,

            @PathVariable Long userId,

            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();


        // Security:
        // URL userId must match logged-in user

        if (
                !currentUser
                        .getId()
                        .equals(userId)
        ) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }


        return ResponseEntity.ok(

                participantService
                        .acceptInvitation(

                                meetingId,

                                currentUser.getId()
                        )
        );
    }


    // =====================================================
    // DECLINE
    // =====================================================

    @PatchMapping("/{userId}/decline")
    public ResponseEntity<ParticipantResponse>
    declineInvitation(

            @PathVariable Long meetingId,

            @PathVariable Long userId,

            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();


        if (
                !currentUser
                        .getId()
                        .equals(userId)
        ) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }


        return ResponseEntity.ok(

                participantService
                        .declineInvitation(

                                meetingId,

                                currentUser.getId()
                        )
        );
    }


    // =====================================================
    // JOIN
    // =====================================================

    @PostMapping("/join")
    public ResponseEntity<ParticipantResponse>
    joinMeeting(

            @PathVariable Long meetingId,

            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();


        return ResponseEntity.ok(

                participantService.joinMeeting(

                        meetingId,

                        currentUser.getId()
                )
        );
    }


    // =====================================================
    // LEAVE
    // =====================================================

    @PostMapping("/leave")
    public ResponseEntity<ParticipantResponse>
    leaveMeeting(

            @PathVariable Long meetingId,

            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();


        return ResponseEntity.ok(

                participantService.leaveMeeting(

                        meetingId,

                        currentUser.getId()
                )
        );
    }
}