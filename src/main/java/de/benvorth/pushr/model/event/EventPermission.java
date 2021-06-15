package de.benvorth.pushr.model.event;

import de.benvorth.pushr.model.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Setter
@Getter
public class EventPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "event_permission_id")
    private Long eventPermissionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // do not set this to EAGER!
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // do not set this to EAGER!
    @JoinColumn(name="event_id", nullable = false)
    private Event event;

    private String permissionName;
    private String permissionValue;

    public EventPermission (User user, Event event, String permissionName, String permissionValue) {
        this.user = user;
        this.event = event;
        this.permissionName = permissionName;
        this.permissionValue = permissionValue;
    }

}
