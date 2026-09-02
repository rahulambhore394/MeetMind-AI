package com.meetmind.meetmind_backend.meeting;



import com.meetmind.meetmind_backend.meeting.dto.CreateMeetingRequest;
import com.meetmind.meetmind_backend.meeting.dto.MeetingResponse;
import com.meetmind.meetmind_backend.meeting.dto.UpdateMeetingRequest;
import com.meetmind.meetmind_backend.user.User;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(
            MeetingService meetingService
    ) {
        this.meetingService = meetingService;
    }


    // ==========================================
    // CREATE
    // ==========================================

    @PostMapping
    public ResponseEntity<MeetingResponse> createMeeting(
            @Valid @RequestBody CreateMeetingRequest request,
            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();

        MeetingResponse response =
                meetingService.createMeeting(
                        request,
                        currentUser.getId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ==========================================
    // GET ALL
    // ==========================================

    @GetMapping
    public ResponseEntity<List<MeetingResponse>> getAllMeetings(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                meetingService.getAllMeetings(user.getId())
        );
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<MeetingResponse> getMeetingByCode(
            @PathVariable String code,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                meetingService.getMeetingByCode(code, user.getId())
        );
    }

    @PostMapping("/code/{code}/join")
    public ResponseEntity<MeetingResponse> joinMeetingByCode(
            @PathVariable String code,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                meetingService.getMeetingByCode(code, user.getId())
        );
    }

    @PostMapping("/{meetingId}/start")
    public ResponseEntity<MeetingResponse> startMeeting(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();


        MeetingResponse response =
                meetingService.startMeeting(
                        meetingId,
                        currentUser.getId()
                );


        return ResponseEntity.ok(response);
    }

    @PostMapping("/{meetingId}/end")
    public ResponseEntity<MeetingResponse> endMeeting(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();


        MeetingResponse response =
                meetingService.endMeeting(
                        meetingId,
                        currentUser.getId()
                );


        return ResponseEntity.ok(response);
    }

    @GetMapping("/{meetingId}/status")
    public ResponseEntity<MeetingResponse> getMeetingStatus(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                meetingService.getMeetingStatus(
                        meetingId,
                        user.getId()
                )
        );
    }
    // ==========================================
    // GET ONE
    // ==========================================

    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingResponse> getMeeting(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                meetingService.getMeeting(meetingId, user.getId())
        );
    }


    // ==========================================
    // UPDATE
    // ==========================================

    @PutMapping("/{meetingId}")
    public ResponseEntity<MeetingResponse> updateMeeting(
            @PathVariable Long meetingId,

            @Valid
            @RequestBody
            UpdateMeetingRequest request,

            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();

        MeetingResponse response =
                meetingService.updateMeeting(
                        meetingId,
                        request,
                        currentUser.getId()
                );

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // DELETE
    // ==========================================

    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> deleteMeeting(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {

        User currentUser =
                (User) authentication.getPrincipal();

        meetingService.deleteMeeting(
                meetingId,
                currentUser.getId()
        );

        return ResponseEntity.noContent().build();
    }
}
