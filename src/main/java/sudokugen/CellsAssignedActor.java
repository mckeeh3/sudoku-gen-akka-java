package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.Optional;

class CellsAssignedActor extends AbstractLoggingActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Board.SetCell.class, this::setCell)
                .match(Board.Clone.class, this::boardClone)
                .build();
    }

    private void setCell(Board.SetCell setCell) {
        String cellName = cellName(setCell);
        Optional<ActorRef> cellAssigned = getContext().findChild(cellName);

        if (!cellAssigned.isPresent()) {
            log().debug("Assign {}", setCell);
            getContext().actorOf(CellAssignedActor.props(setCell.cell.row, setCell.cell.col, setCell.cell.value), cellName);
        }
    }

    private void boardClone(Board.Clone boardClone) {
        getContext().getChildren().forEach(child -> child.tell(boardClone, getSelf()));
    }

    private String cellName(Board.SetCell setCell) {
        return String.format("assigned-row-%d-col-%d", setCell.cell.row, setCell.cell.col);
    }

    static Props props() {
        return Props.create(CellsAssignedActor.class);
    }
}
