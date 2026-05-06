package com.example.ThesisConnect;

import com.example.ThesisConnect.domain.User;
import com.example.ThesisConnect.repository.UserRepository;
import com.example.ThesisConnect.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GroupControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void createsGroupAndShowsCreatorAsAdminMember() throws Exception {
        User alice = createUser("Alice Admin", "alice@example.com");
        String aliceToken = tokenFor(alice);

        String response = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "AI for Healthcare",
                                  "description": "We want to build a diagnosis support thesis project."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("AI for Healthcare"))
                .andExpect(jsonPath("$.admin.email").value("alice@example.com"))
                .andExpect(jsonPath("$.members[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$.members[0].admin").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String groupId = response.replaceAll("(?s).*\"groupId\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/groups")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(Integer.parseInt(groupId)))
                .andExpect(jsonPath("$[0].currentUserAdmin").value(true));
    }

    @Test
    void approvesJoinRequestsAssignsAdminsAndLetsUsersRejectInvitations() throws Exception {
        User alice = createUser("Alice Admin", "alice2@example.com");
        User bob = createUser("Bob Builder", "bob@example.com");
        User carol = createUser("Carol Candidate", "carol@example.com");

        String aliceToken = tokenFor(alice);
        String bobToken = tokenFor(bob);
        String carolToken = tokenFor(carol);

        String createResponse = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "Distributed Systems",
                                  "description": "We are exploring resilient coordination patterns."
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String groupId = createResponse.replaceAll("(?s).*\"groupId\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/groups/{groupId}/join-requests", groupId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUserJoinRequestStatus").value("PENDING"));

        mockMvc.perform(get("/api/groups/{groupId}", groupId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingJoinRequests[0].sender.email").value("bob@example.com"));

        mockMvc.perform(post("/api/groups/{groupId}/requests/{requestId}/approve", groupId, 1)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[?(@.email=='bob@example.com')]").isNotEmpty());

        mockMvc.perform(post("/api/groups/{groupId}/members/{userId}/admins", groupId, bob.getUserId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[?(@.email=='bob@example.com' && @.admin==true)]").isNotEmpty());

        mockMvc.perform(post("/api/groups/{groupId}/invitations", groupId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d
                                }
                                """.formatted(carol.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingInvitations[0].recipient.email").value("carol@example.com"));

        mockMvc.perform(get("/api/groups/{groupId}", groupId)
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingInvitations[0].recipient.email").value("carol@example.com"));

        mockMvc.perform(post("/api/groups/{groupId}/requests/{requestId}/reject", groupId, 2)
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUserInvitationStatus").doesNotExist());
    }

    @Test
    void supportsDirectMessagesGroupDiscussionPinsAndNotifications() throws Exception {
        User alice = createUser("Alice Messenger", "alice-msg@example.com");
        User bob = createUser("Bob Partner", "bob-msg@example.com");

        String aliceToken = tokenFor(alice);
        String bobToken = tokenFor(bob);

        String createResponse = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "Human Computer Interaction",
                                  "description": "We are researching collaborative thesis tooling."
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String groupId = createResponse.replaceAll("(?s).*\"groupId\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/groups/{groupId}/join-requests", groupId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/groups/{groupId}", groupId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingJoinRequests[0].sender.email").value("bob-msg@example.com"));

        String approveResponse = mockMvc.perform(get("/api/groups/{groupId}", groupId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String requestId = approveResponse.replaceAll("(?s).*\"requestId\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/groups/{groupId}/requests/{requestId}/approve", groupId, requestId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        String conversationResponse = mockMvc.perform(post("/api/messages/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientUserId": %d,
                                  "content": "Want to discuss the thesis scope?"
                                }
                                """.formatted(bob.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].content").value("Want to discuss the thesis scope?"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String directMessageId = conversationResponse.replaceAll("(?s).*\"messageId\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/messages/direct/{messageId}/pin", directMessageId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].pinned").value(true));

        mockMvc.perform(get("/api/groups/notifications")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("New direct message from Alice Messenger."));

        String groupMessageResponse = mockMvc.perform(post("/api/messages/groups/{groupId}", groupId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "I can help with the user interviews section."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("I can help with the user interviews section."))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String groupMessageId = groupMessageResponse.replaceAll("(?s).*\"messageId\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/messages/groups/{groupId}/{messageId}/pin", groupId, groupMessageId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pinned").value(true));
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setResearchInterests(List.of("AI"));
        user.setSkills(List.of("Java"));
        user.setLookingForGroup(true);
        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user.getEmail());
    }
}
