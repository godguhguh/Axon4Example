package nl.avthart.todo.app.query.task;

import java.time.Instant;

import nl.avthart.todo.app.common.axon.AbstractPrimaryProjector;
import nl.avthart.todo.app.common.exceptions.BusinessRuleException;
import nl.avthart.todo.app.common.exceptions.CantUpdateException;
import nl.avthart.todo.app.common.exceptions.DeletedException;
import nl.avthart.todo.app.domain.task.events.TaskEvent;
import nl.avthart.todo.app.domain.task.events.TaskEventCompleted;
import nl.avthart.todo.app.domain.task.events.TaskEventCreated;
import nl.avthart.todo.app.domain.task.events.TaskEventDelete;
import nl.avthart.todo.app.domain.task.events.TaskEventRestore;
import nl.avthart.todo.app.domain.task.events.TaskEventStarred;
import nl.avthart.todo.app.domain.task.events.TaskEventTitleModified;
import nl.avthart.todo.app.domain.task.events.TaskEventUnstarred;
import nl.avthart.todo.app.flags.Monitor;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.SequenceNumber;
import org.axonframework.eventhandling.Timestamp;
import org.springframework.stereotype.Component;

@Component
public class TaskPrimaryProjector extends AbstractPrimaryProjector<String, TaskActive, TaskDeleted> {

    public TaskPrimaryProjector( TaskPrimaryProjectionRepository repo ) {
        super( repo, "Task" );
    }

    public TaskEventCreated syncProcess( Instant createdAt, TaskEventCreated event ) {
        // vvv Testing
        if ( "BadTask".equals( event.getTitle() ) ) {
            throw new BusinessRuleException( "TaskEntryUpdatingEventHandler: BadTask" );
        }
        Monitor.show();
        // ^^^ Testing
        createdAt = ensureInstant( createdAt );
        syncCreate( createdAt, event, map( event, createdAt ) );
        return event;
    }

    @SuppressWarnings("unused")
    @EventHandler
    void on( TaskEventCreated event, @Timestamp Instant createdAt ) {
        createdAt = ensureInstant( createdAt );
        handlerCreate( event, createdAt, map( event, createdAt ) );
    }

    private TaskActive map( TaskEventCreated event, Instant createdAt ) {
        return new TaskActive( TaskEntity.builder()
                                       .id( event.getId() )
                                       .version( 0L )
                                       .lastModified( createdAt )
                                       .createdHour( toHour( createdAt ) )
                                       .username( event.getUsername() )
                                       .title( event.getTitle() )
                                       .completed( false )
                                       .starred( false )
                                       .build() );
    }

    private String toHour( Instant instant ) {
        // 2020-11-19T13Z
        // 01234567-10123
        // 2020-11-19T13:12:11.101Z
        return instant.toString().substring( 0, 13 ) + "Z";
    }

    public TaskEventStarred syncProcess( long currentVersion, Instant lastModifiedAt, TaskEventStarred event ) {
        syncUpdate( lastModifiedAt, map( event, checkSyncUpdate( currentVersion, event ) ) );
        return event;
    }

    @SuppressWarnings("unused")
    @EventHandler
    void on( TaskEventStarred event, @SequenceNumber long nextVersion, @Timestamp Instant createdAt ) {
        TaskActive active = checkHandlerUpdate( nextVersion, event );
        // vvv Testing
        if ( active != null ) {
            System.out.println( "************ Async: " + event.getClass().getSimpleName() );
        }
        // ^^^ Testing
        handlerUpdate( map( event, active ), createdAt );
    }

    @SuppressWarnings("unused")
    private TaskActive map( TaskEventStarred event, TaskActive task ) {
        if ( task != null ) {
            task.setStarred( true );
        }
        return task;
    }

    public TaskEventUnstarred syncProcess( long currentVersion, Instant lastModifiedAt, TaskEventUnstarred event ) {
        TaskActive task = checkSyncUpdate( currentVersion, event );
        // vvv Testing
        if ( "SkipUnstar".equals( task.getTitle() ) ) {
            return event;
        }
        // ^^^ Testing
        syncUpdate( lastModifiedAt, map( event, task ) );
        return event;
    }

    @SuppressWarnings("unused")
    @EventHandler
    void on( TaskEventUnstarred event, @SequenceNumber long nextVersion, @Timestamp Instant createdAt ) {
        TaskActive active = checkHandlerUpdate( nextVersion, event );
        // vvv Testing
        if ( active != null ) {
            System.out.println( "************ Async: " + event.getClass().getSimpleName() );
        }
        // ^^^ Testing
        handlerUpdate( map( event, active ), createdAt );
    }

    @SuppressWarnings("unused")
    private TaskActive map( TaskEventUnstarred event, TaskActive task ) {
        if ( task != null ) {
            task.setStarred( false );
        }
        return task;
    }

    public TaskEventTitleModified syncProcess( long currentVersion, Instant lastModifiedAt, TaskEventTitleModified event ) {
        TaskActive task = checkSyncUpdate( currentVersion, event );
        // vvv Testing
        if ( "BadVersion".equals( event.getTitle() ) && task.isStarred() ) { // Faking user sent bad Version!
            task = new TaskActive( task.toBuilder().version( 0L ).build() ); // Forces new entity which is NOT in the ORM transaction
        }
        // ^^^ Testing
        syncUpdate( lastModifiedAt, map( event, task ) );
        return event;
    }

    @SuppressWarnings("unused")
    @EventHandler
    void on( TaskEventTitleModified event, @SequenceNumber long nextVersion, @Timestamp Instant createdAt ) {
        TaskActive active = checkHandlerUpdate( nextVersion, event );
        // vvv Testing
        if ( active != null ) {
            System.out.println( "************ Async: " + event.getClass().getSimpleName() );
        }
        // ^^^ Testing
        handlerUpdate( map( event, active ), createdAt );
    }

    private TaskActive map( TaskEventTitleModified event, TaskActive task ) {
        if ( task != null ) {
            task.setTitle( event.getTitle() );
        }
        return task;
    }

    public TaskEventCompleted syncProcess( long currentVersion, Instant lastModifiedAt, TaskEventCompleted event ) {
        syncUpdate( lastModifiedAt, map( event, checkSyncUpdate( currentVersion, event ) ) );
        return event;
    }

    @SuppressWarnings("unused")
    @EventHandler
    void on( TaskEventCompleted event, @SequenceNumber long nextVersion, @Timestamp Instant createdAt ) {
        TaskActive active = checkHandlerUpdate( nextVersion, event );
        // vvv Testing
        if ( active != null ) {
            System.out.println( "************ Async: " + event.getClass().getSimpleName() );
        }
        // ^^^ Testing
        handlerUpdate( map( event, active ), createdAt );
    }

    @SuppressWarnings("unused")
    private TaskActive map( TaskEventCompleted event, TaskActive task ) {
        if ( task != null ) {
            task.setCompleted( true );
        }
        return task;
    }

    // vvvvvvvvvvvv TODO: XXX
    public TaskEventDelete syncProcess( long currentVersion, Instant lastModifiedAt, TaskEventDelete event ) {
        // Not currently Deleted (checked in Aggregate)
        update( event, lastModifiedAt, checkSyncDelete( currentVersion, event ) );
        return event;
    }

    @SuppressWarnings("unused")
    @EventHandler
    void on( TaskEventDelete event, @SequenceNumber long nextVersion, @Timestamp Instant createdAt ) {
        // Don't know if Deleted, but if not in (Active) then assume deleted
        update( event, createdAt, checkHandlerDelete( nextVersion, event ) );
    }

    @SuppressWarnings("unused")
    private void update( TaskEventDelete event, Instant lastModifiedAt, TaskActive active ) {
        if ( active != null ) {
            repo.delete( active, lastModifiedAt );
        }
    }

    public TaskEventRestore syncProcess( long currentVersion, Instant lastModifiedAt, TaskEventRestore event ) {
        update( event, lastModifiedAt, checkSyncRestore( currentVersion, event ) );
        return event;
    }

    @SuppressWarnings("unused")
    @EventHandler
    void on( TaskEventRestore event, @SequenceNumber long nextVersion, @Timestamp Instant createdAt ) {
        update( event, createdAt, checkHandlerRestore( nextVersion, event ) );
    }

    @SuppressWarnings("unused")
    private void update( TaskEventRestore event, Instant lastModifiedAt, TaskDeleted deleted ) {
        if ( deleted != null ) {
            repo.restore( deleted, lastModifiedAt );
        }
    }

    // Support
    private TaskActive checkSyncDelete( long currentVersion, TaskEvent event ) {
        TaskActive task = readActive( event );
        if ( task == null ) {
            boolean deleted = (null != readDeleted( event ));
            if ( deleted ) {
                throw new DeletedException( "Task (" + event.getId() + ") is currently deleted" );
            }
            throw new CantUpdateException( "Task (" + event.getId() + ") to delete does not exist" );
        }
        checkSyncUpdateVersion( currentVersion, task );
        return task;
    }

    private TaskActive checkHandlerDelete( long nextVersion, TaskEvent event ) {
        TaskActive task = readActive( event );
        if ( (task == null) || (task.getVersion() != (nextVersion - 1)) ) {
            return null;
        }
        String type = event.getClass().getSimpleName();
        System.out.println( "************ Async: " + type );
        return task;
    }

    private TaskDeleted checkSyncRestore( long currentVersion, TaskEvent event ) {
        TaskDeleted deleted = readDeleted( event );
        if ( deleted == null ) {
            boolean active = (null != readActive( event ));
            if ( active ) {
                throw new DeletedException( "Task (" + event.getId() + ") is currently active" );
            }
            throw new CantUpdateException( "Task (" + event.getId() + ") to restore does not exist" );
        }
        // TODO: xxx checkSyncUpdateVersion( currentVersion, deleted );
        return deleted;
    }

    private TaskDeleted checkHandlerRestore( long nextVersion, TaskEvent event ) {
        TaskDeleted task = readDeleted( event );
        if ( (task == null) || (task.getVersion() != (nextVersion - 1)) ) {
            return null;
        }
        String type = event.getClass().getSimpleName();
        System.out.println( "************ Async: " + type );
        return task;
    }
}