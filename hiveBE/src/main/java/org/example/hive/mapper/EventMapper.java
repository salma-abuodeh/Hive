package org.example.hive.mapper;

import org.example.hive.config.AppEnums.RsvpStatus;
import org.example.hive.dto.response.EventResponse;
import org.example.hive.dto.response.EventRsvpResponse;
import org.example.hive.model.Event;
import org.example.hive.model.EventRsvp;

public final class EventMapper {

    private EventMapper() {
    }

    public static EventResponse toResponse(Event event, RsvpStatus myRsvpStatus) {
        return new EventResponse(
                event.getId(),
                event.getCompany().getId(),
                event.getTeam() != null ? event.getTeam().getId() : null,
                event.getCreatedBy().getId(),
                event.getCreatedBy().getFirstName() + " " + event.getCreatedBy().getLastName(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getVisibility(),
                event.getActive(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                myRsvpStatus,
                event.getCover() != null ? "/attachments/" + event.getCover().getId() : null
        );
    }

    public static EventRsvpResponse toRsvpResponse(EventRsvp rsvp) {
        return new EventRsvpResponse(
                rsvp.getUser().getId(),
                rsvp.getUser().getFirstName() + " " + rsvp.getUser().getLastName(),
                rsvp.getStatus(),
                rsvp.getRespondedAt()
        );
    }
}