package me.pood1e.jobstream.common;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    private String id;
    private NotificationType type;
    private String className;
    private Object config;
}
