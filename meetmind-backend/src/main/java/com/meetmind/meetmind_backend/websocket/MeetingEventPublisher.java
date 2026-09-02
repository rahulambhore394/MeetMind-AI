package com.meetmind.meetmind_backend.websocket;



import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MeetingEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;


    public MeetingEventPublisher(
            SimpMessagingTemplate messagingTemplate
    ) {

        this.messagingTemplate =
                messagingTemplate;
    }


    public void publish(
            Long meetingId,
            MeetingEvent event
    ) {

        String destination =
                "/topic/meetings/"
                        + meetingId;


        messagingTemplate.convertAndSend(
                destination,
                event
        );
    }
}
