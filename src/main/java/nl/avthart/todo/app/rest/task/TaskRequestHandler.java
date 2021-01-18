package nl.avthart.todo.app.rest.task;

import lombok.RequiredArgsConstructor;
import nl.avthart.todo.app.domain.task.commands.TaskCommand;
import nl.avthart.todo.app.domain.task.commands.TaskCommandComplete;
import nl.avthart.todo.app.domain.task.commands.TaskCommandCreate;
import nl.avthart.todo.app.domain.task.commands.TaskCommandDelete;
import nl.avthart.todo.app.domain.task.commands.TaskCommandModifyTitle;
import nl.avthart.todo.app.domain.task.commands.TaskCommandRestore;
import nl.avthart.todo.app.domain.task.commands.TaskCommandStar;
import nl.avthart.todo.app.domain.task.commands.TaskCommandUnstar;
import nl.avthart.todo.app.query.task.TaskActive;
import nl.avthart.todo.app.query.task.TaskPrimaryProjectionReadRepository;
import nl.avthart.todo.app.rest.task.requests.TaskRequestCreate;
import nl.avthart.todo.app.rest.task.requests.TaskRequestModifyTitle;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.common.IdentifierFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskRequestHandler {
    private final IdentifierFactory identifierFactory = IdentifierFactory.getInstance();
    private final TaskPrimaryProjectionReadRepository repo;
    private final CommandGateway commandGateway;

    public Page<TaskActive> findAll( String userName, boolean completed, Pageable pageable ) {
        return repo.findActiveByUsernameAndCompleted( userName, completed, pageable );
    }

    public String create( String userName, TaskRequestCreate request ) {
        Object id = sendAndWait( TaskCommandCreate.builder()
                                         .id( identifierFactory.generateIdentifier() )
                                         .username( userName )
                                         .title( request.getTitle() )
                                         .build() );
        return (id == null) ? null : id.toString();
    }

    public void modifyTitle( String identifier, TaskRequestModifyTitle request ) {
        sendAndWait( TaskCommandModifyTitle.builder()
                             .id( identifier )
                             .title( request.getTitle() )
                             .build() );
    }

    public void complete( String identifier ) {
        sendAndWait( new TaskCommandComplete( identifier ) );
    }

    public void star( String identifier ) {
        sendAndWait( new TaskCommandStar( identifier ) );
    }

    public void unstar( String identifier ) {
        sendAndWait( new TaskCommandUnstar( identifier ) );
    }

    public void delete( String identifier ) {
        sendAndWait( new TaskCommandDelete( identifier ) );
    }

    public void restore( String identifier ) {
        sendAndWait( new TaskCommandRestore( identifier ) );
    }

    private Object sendAndWait( TaskCommand command ) {
        return commandGateway.sendAndWait( command );
    }
}
