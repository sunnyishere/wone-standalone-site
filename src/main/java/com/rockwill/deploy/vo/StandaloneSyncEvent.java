package com.rockwill.deploy.vo;

import lombok.Data;


@Data
public class StandaloneSyncEvent {
    private String eventId;
    private StandaloneSyncEntityType entityType;
    private StandaloneSyncAction action;
    private Long entityId;
    private Long brandUid;
    private String deployDomain;
    private String source;
}