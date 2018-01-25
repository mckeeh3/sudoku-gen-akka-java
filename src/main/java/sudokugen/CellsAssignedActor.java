package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.Optional;

class CellsAssignedActor extends AbstractLoggingActor {
    private int cellCount;
    private int cellCountCopied;
    private ActorRef copyToBoard;
    private final ActorRef validateBoard;

    {
        validateBoard = getContext().actorOf(ValidateBoardActor.props(), "validate-board");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Board.SetCell.class, this::setCell)
                .match(Board.CopyAssignedCells.class, this::copyAssignedCell)
                .match(Board.CopyOfAssignedCell.class, this::copyOfAssignedCell)
                .build();
    }

    @SuppressWarnings("unused")
    private void copyOfAssignedCell(Board.CopyOfAssignedCell copyOfAssignedCell) {
        copyToBoard.tell(copyOfAssignedCell, getSelf());

        if (++cellCountCopied == cellCount) {
            copyToBoard.tell(new Board.CopiedAssignedCells(), getSelf());
        }
    }

    private void setCell(Board.SetCell setCell) {
        String cellActorName = cellActorName(setCell);
        Optional<ActorRef> cellAssigned = getContext().findChild(cellActorName);

        if (!cellAssigned.isPresent()) {
            log().debug("Assign {}", setCell);
            cellCount++;
            getContext().actorOf(CellAssignedActor.props(setCell.cell), cellActorName);
            validateBoard.tell(setCell, getSelf());
        }
    }

    private void copyAssignedCell(Board.CopyAssignedCells copyAssignedCells) {
        cellCountCopied = 0;
        copyToBoard = getSender();
        getContext().getChildren().forEach(child -> child.tell(copyAssignedCells, getSelf()));
    }

    private String cellActorName(Board.SetCell setCell) {
        return String.format("assigned-%d-%d", setCell.cell.row, setCell.cell.col);
    }

    static Props props() {
        return Props.create(CellsAssignedActor.class);
    }
}
