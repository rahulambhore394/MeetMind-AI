package com.meetmind.meetmind_backend.meeting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.meetmind_backend.chat.TestRedisConfig;
import com.meetmind.meetmind_backend.meeting.dto.CreateMeetingRequest;
import com.meetmind.meetmind_backend.meeting.dto.MeetingResponse;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class MeetingControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @MockitoBean
    private MeetingService meetingService;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("host@meetmind.com");
        testUser.setName("Host User");

        when(userRepository.findByEmail("host@meetmind.com")).thenReturn(Optional.of(testUser));
    }

    private Meeting createMeetingEntity(Long id, String title, String code) {
        Meeting meeting = new Meeting();
        meeting.setId(id);
        meeting.setTitle(title);
        meeting.setMeetingCode(code);
        meeting.setHost(testUser);
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setScheduledAt(LocalDateTime.now().plusHours(1));
        return meeting;
    }

    @Test
    void createMeeting_Unauthenticated_Returns403() throws Exception {
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle("Engineering Sync");
        request.setScheduledAt(LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "host@meetmind.com", roles = "USER")
    void createMeeting_ValidPayload_ReturnsCreated() throws Exception {
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle("Engineering Sync");
        request.setDescription("Sprint review");
        request.setScheduledAt(LocalDateTime.now().plusHours(1));

        Meeting meeting = createMeetingEntity(500L, "Engineering Sync", "ABC123");
        MeetingResponse response = new MeetingResponse(meeting);

        when(meetingService.createMeeting(any(CreateMeetingRequest.class), eq(10L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(500L))
                .andExpect(jsonPath("$.meetingCode").value("ABC123"))
                .andExpect(jsonPath("$.title").value("Engineering Sync"));
    }

    @Test
    @WithMockUser(username = "host@meetmind.com", roles = "USER")
    void createMeeting_BlankTitle_ReturnsBadRequest() throws Exception {
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle(""); // Blank title violates validation
        request.setScheduledAt(LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "host@meetmind.com", roles = "USER")
    void getAllMeetings_ReturnsList() throws Exception {
        Meeting meeting = createMeetingEntity(500L, "Weekly All-Hands", "ALL123");
        MeetingResponse response = new MeetingResponse(meeting);

        when(meetingService.getAllMeetings(eq(10L))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(500L))
                .andExpect(jsonPath("$[0].title").value("Weekly All-Hands"));
    }

    @Test
    @WithMockUser(username = "host@meetmind.com", roles = "USER")
    void getMeetingByCode_ValidCode_ReturnsMeeting() throws Exception {
        Meeting meeting = createMeetingEntity(500L, "Quick Connect", "XYZ789");
        MeetingResponse response = new MeetingResponse(meeting);

        when(meetingService.getMeetingByCode(eq("XYZ789"), eq(10L))).thenReturn(response);

        mockMvc.perform(get("/api/meetings/code/XYZ789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(500L))
                .andExpect(jsonPath("$.meetingCode").value("XYZ789"));
    }

    @Test
    @WithMockUser(username = "host@meetmind.com", roles = "USER")
    void deleteMeeting_ValidId_ReturnsNoContent() throws Exception {
        doNothing().when(meetingService).deleteMeeting(eq(500L), eq(10L));

        mockMvc.perform(delete("/api/meetings/500"))
                .andExpect(status().isNoContent());
    }
}
