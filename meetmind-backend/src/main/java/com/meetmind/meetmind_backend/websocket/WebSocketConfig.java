package com.meetmind.meetmind_backend.websocket;



import com.meetmind.meetmind_backend.auth.jwt.JwtService;
import com.meetmind.meetmind_backend.user.UserRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public WebSocketConfig(
            JwtService jwtService,
            UserRepository userRepository,
            ParticipantRepository participantRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {

        registry.enableSimpleBroker(
                "/topic",
                "/queue"
        );

        registry.setApplicationDestinationPrefixes(
                "/app"
        );
    }
    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration
    ) {

        registration.interceptors(
                new WebSocketAuthInterceptor(
                        jwtService,
                        userRepository,
                        participantRepository
                )
        );
    }
    @Override
    public boolean configureMessageConverters(
            List<MessageConverter> messageConverters
    ) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        converter.setObjectMapper(mapper);
        messageConverters.add(converter);
        return true;
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {

        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}