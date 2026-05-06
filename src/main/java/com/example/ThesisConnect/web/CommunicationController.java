package com.example.ThesisConnect.web;

import com.example.ThesisConnect.dto.CreateDirectMessageRequest;
import com.example.ThesisConnect.dto.CreateGroupMessageRequest;
import com.example.ThesisConnect.dto.DirectConversationResponse;
import com.example.ThesisConnect.dto.GroupMessageResponse;
import com.example.ThesisConnect.service.CommunicationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class CommunicationController {

    private final CommunicationService communicationService;

    public CommunicationController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @GetMapping("/direct/{otherUserId}")
    public DirectConversationResponse getConversation(
            Authentication authentication,
            @PathVariable Long otherUserId
    ) {
        return communicationService.getConversation(authentication.getName(), otherUserId);
    }

    @PostMapping("/direct")
    public DirectConversationResponse sendDirectMessage(
            Authentication authentication,
            @Valid @RequestBody CreateDirectMessageRequest request
    ) {
        return communicationService.sendDirectMessage(authentication.getName(), request);
    }

    @PostMapping("/direct/{messageId}/pin")
    public DirectConversationResponse pinDirectMessage(
            Authentication authentication,
            @PathVariable Long messageId
    ) {
        return communicationService.pinDirectMessage(authentication.getName(), messageId);
    }

    @PostMapping("/direct/{messageId}/unpin")
    public DirectConversationResponse unpinDirectMessage(
            Authentication authentication,
            @PathVariable Long messageId
    ) {
        return communicationService.unpinDirectMessage(authentication.getName(), messageId);
    }

    @GetMapping("/groups/{groupId}")
    public List<GroupMessageResponse> getGroupMessages(
            Authentication authentication,
            @PathVariable Long groupId
    ) {
        return communicationService.getGroupMessages(authentication.getName(), groupId);
    }

    @PostMapping("/groups/{groupId}")
    public List<GroupMessageResponse> sendGroupMessage(
            Authentication authentication,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupMessageRequest request
    ) {
        return communicationService.sendGroupMessage(authentication.getName(), groupId, request);
    }

    @PostMapping("/groups/{groupId}/{messageId}/pin")
    public List<GroupMessageResponse> pinGroupMessage(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long messageId
    ) {
        return communicationService.pinGroupMessage(authentication.getName(), groupId, messageId);
    }

    @PostMapping("/groups/{groupId}/{messageId}/unpin")
    public List<GroupMessageResponse> unpinGroupMessage(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long messageId
    ) {
        return communicationService.unpinGroupMessage(authentication.getName(), groupId, messageId);
    }
}
