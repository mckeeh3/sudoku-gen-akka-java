package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;

import java.util.Optional;

class CellsAssignedActor extends AbstractLoggingActor {
    private int cellCount;
    private ActorRef validateBoard;

    {
        validateBoard = createValidateBoardActor();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Board.SetCell.class, this::setCell)
                .match(Board.FetchAssignedCells.class, this::copyAssignedCell)
                .match(Validate.Invalid.class, this::boardInvalid)
                .match(Board.Reset.class, this::boardReset)
                .match(Terminated.class, this::validatorStopped)
                .match(Validate.ValidBoard.class, this::boardValid)
                .build();
    }

    private void setCell(Board.SetCell setCell) {
        String cellActorName = cellActorName(setCell);
        Optional<ActorRef> cellAssigned = getContext().findChild(cellActorName);

        if (!cellAssigned.isPresent()) {
            assignCell(setCell, cellActorName);
        }
    }

    private void assignCell(Board.SetCell setCell, String cellActorName) {
        log().debug("Assign {}", setCell);
        cellCount++;
        getContext().actorOf(CellAssignedActor.props(setCell.cell), cellActorName);
        validateBoard.tell(setCell, getSelf());
    }

    private void copyAssignedCell(Board.FetchAssignedCells fetchAssignedCells) {
        getSender().tell(new Board.AssignedCellTotal(cellCount), getSelf());
        getContext().getChildren().forEach(child -> child.forward(fetchAssignedCells, getContext()));
    }

    private void boardInvalid(Validate.Invalid cellInvalid) {
        getContext().getParent().tell(new Board.Invalid(cellInvalid.toString()), getSelf());
    }

    private void boardReset(Board.Reset boardReset) {
        log().debug("{}", boardReset);
        cellCount = 0;

        getContext().stop(validateBoard);
        getContext().getChildren().forEach(child -> getContext().stop(child));
    }

    @SuppressWarnings("unused")
    private void validatorStopped(Terminated validatorStopped) {
        validateBoard = createValidateBoardActor();
    }

    private void boardValid(Validate.ValidBoard boardValid) {
        getContext().getParent().tell(new Board.Generated(boardValid.grid), getSelf());
    }

    private ActorRef createValidateBoardActor() {
        return getContext().watch(getContext().actorOf(ValidateBoardActor.props(), "validateBoard"));
    }

    private String cellActorName(Board.SetCell setCell) {
        return String.format("assigned-%d-%d", setCell.cell.row, setCell.cell.col);
    }

    static Props props() {
        return Props.create(CellsAssignedActor.class);
    }
}
