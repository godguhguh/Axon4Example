package nl.avthart.todo.app.domain.task.events;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nl.avthart.todo.app.query.task.AbstractTaskEntry_v001;

@NoArgsConstructor
@ToString(callSuper = true)
public class TaskEventCreated extends AbstractAllFieldsTaskEvent implements TaskEvent {
    @SuppressWarnings("unused")
    @Builder
    public TaskEventCreated( String id,
                             String createdHour,
                             String username,
                             String title,
                             boolean completed,
                             boolean starred ) {
        super( id, createdHour, username, title, completed, starred );
    }

    public TaskEventCreated( String id, AbstractTaskEntry_v001 fields ) {
        super( id, fields );
    }
}
