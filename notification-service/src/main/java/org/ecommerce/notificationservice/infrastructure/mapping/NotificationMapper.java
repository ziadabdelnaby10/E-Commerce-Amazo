package org.ecommerce.notificationservice.infrastructure.mapping;

import org.ecommerce.notificationservice.api.dto.NotificationDetailResponse;
import org.ecommerce.notificationservice.api.dto.NotificationLogResponse;
import org.ecommerce.notificationservice.api.dto.NotificationPreferenceResponse;
import org.ecommerce.notificationservice.api.dto.NotificationSummaryResponse;
import org.ecommerce.notificationservice.domain.model.Notification;
import org.ecommerce.notificationservice.domain.model.NotificationLog;
import org.ecommerce.notificationservice.domain.model.NotificationPreference;
import org.ecommerce.notificationservice.infrastructure.persistence.projection.NotificationSummaryProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    NotificationSummaryResponse toSummary(NotificationSummaryProjection projection);

    @Mapping(target = "logs", source = "logs")
    NotificationDetailResponse toDetail(Notification notification);

    NotificationLogResponse toLogResponse(NotificationLog notificationLog);

    NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference);
}

