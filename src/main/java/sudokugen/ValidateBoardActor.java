package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

class ValidateBoardActor extends AbstractLoggingActor {
    private final ActorRef validateRows;
    private final ActorRef validateColumns;
    private final ActorRef validateBoxes;
    private final Board.Grid grid;

    private int validRowColBox = 0;

    {
        validateRows = getContext().actorOf(ValidateRowsActor.props(), "validateRows");
        validateColumns = getContext().actorOf(ValidateColumnsActor.props(), "validateColumns");
        validateBoxes = getContext().actorOf(ValidateBoxesActor.props(), "validateBoxes");

        grid = new Board.Grid();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Board.SetCell.class, this::setCell)
                .match(Validate.Valid.class, this::validRowColBox)
                .match(Validate.Invalid.class, this::invalidRowColBox)
                .match(Board.FetchAssignedCells.class, this::copyAssignedCellsIgnore)
                .build();
    }

    private void setCell(Board.SetCell setCell) {
        grid.set(setCell.cell);

        validateCell(setCell.cell);
    }

    private void validateCell(Board.Cell cell) {
        validateRows.tell(cell, getSelf());
        validateColumns.tell(cell, getSelf());
        validateBoxes.tell(cell, getSelf());
    }

    @SuppressWarnings("unused")
    private void validRowColBox(Validate.Valid valid) {
        validRowColBox++;
        if (validRowColBox == 3) {
            getContext().getParent().tell(new Validate.ValidBoard("Generated", grid), getSelf());
        }
    }

    private void invalidRowColBox(Validate.Invalid invalid) {
        log().debug("{}", invalid);
        getContext().getParent().tell(invalid, getSelf());
    }

    @SuppressWarnings("unused")
    private void copyAssignedCellsIgnore(Board.FetchAssignedCells fetchAssignedCells) {
        // ignore message
    }

    static Props props() {
        return Props.create(ValidateBoardActor.class);
    }
}
