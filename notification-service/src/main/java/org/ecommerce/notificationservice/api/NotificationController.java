package org.ecommerce.notificationservice.api;

import lombok.RequiredArgsConstructor;
import org.ecommerce.notificationservice.api.dto.NotificationDetailResponse;
import org.ecommerce.notificationservice.api.dto.NotificationPreferenceResponse;
import org.ecommerce.notificationservice.api.dto.NotificationSummaryResponse;
import org.ecommerce.notificationservice.api.dto.UpdateNotificationPreferenceRequest;
import org.ecommerce.notificationservice.application.service.NotificationApplicationService;
import org.ecommerce.notificationservice.domain.model.NotificationStatus;
import org.ecommerce.notificationservice.domain.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApplicationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationSummaryResponse>> getInbox(
            @RequestParam Long userId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getInbox(userId, type, status, pageable));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationDetailResponse> getByNotificationId(@PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.getByNotificationId(notificationId));
    }

    @GetMapping("/preferences/{userId}")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getPreferences(userId));
    }

    @PutMapping("/preferences/{userId}")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @PathVariable Long userId,
            @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        return ResponseEntity.ok(notificationService.updatePreferences(userId, request));
    }
}

