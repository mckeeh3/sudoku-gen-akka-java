package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

class BoardGeneratorActor extends AbstractLoggingActor {
    private final int boardNumber;
    private final ActorRef boardFrom;
    private final Receive cloning;
    private final Receive cloned;

    private ActorRef boardTo;

    private int cloneUnassignedCount;
    private int cloneAssignedCount;
    private Board.CloneUnassigned cloneUnassignedLast;

    private BoardGeneratorActor(int boardNumber, ActorRef boardFrom) {
        this.boardNumber = ++boardNumber;
        this.boardFrom = boardFrom;

        cloning = receiveBuilder()
                .match(Board.CloneUnassigned.class, this::cloneUnassigned)
                .match(Board.CloneAssigned.class, this::cloneAssigned)
                .match(Board.SetCell.class, this::setCellCloning)
                .build();

        cloned = receiveBuilder()
                .match(Board.SetCell.class, this::setCellCloned)
                .match(Board.Invalid.class, this::boardInvalid)
                .match(Board.Generated.class, this::boardGenerated)
                .build();

        cloneBoardFrom();
    }

    @Override
    public Receive createReceive() {
        return cloning;
    }

    private void cloneUnassigned(Board.CloneUnassigned cloneUnassigned) {
        ++cloneUnassignedCount;
        cloneUnassignedLast = cloneUnassigned;
        checkIfCloningComplete();
    }

    private void cloneAssigned(Board.CloneAssigned cloneAssigned) {
        ++cloneAssignedCount;
        cloneAssignedSetCellInBoardTo(cloneAssigned);
        checkIfCloningComplete();
    }

    private void cloneAssignedSetCellInBoardTo(Board.CloneAssigned cloneAssigned) {
        boardTo.tell(new Board.SetCell(cloneAssigned.cell), getSelf());
    }

    private void setCellCloning(Board.SetCell setCell) {
//        log().debug("Set cell while cloning {} {}", setCell, getSender().path());
        boardTo.tell(setCell, getSelf());
        cloneAssignedCount++;
        checkIfCloningComplete();
    }

    private void setCellCloned(Board.SetCell setCell) {
//        log().debug("Set cell after cloning completed {} {}", setCell, getSender().path());
        boardTo.tell(setCell, getSelf());
    }

    private void boardInvalid(Board.Invalid boardInvalid) {
        log().debug("{}", boardInvalid);
        // TODO
        //getContext().getParent().tell(boardInvalid, getSelf());
    }

    private void boardGenerated(Board.Generated boardGenerated) {
        log().debug("{}", boardGenerated);
        getContext().getParent().tell(boardGenerated, getSelf());
    }

    private void cloneBoardFrom() {
        cloneUnassignedLast = null;
        cloneUnassignedCount = 0;
        cloneAssignedCount = 0;

        if (boardTo != null) {
            getContext().stop(boardTo);
        }
        boardTo = getContext().actorOf(BoardActor.props(), String.format("board-%d", boardNumber));

        boardFrom.tell(new Board.Clone(boardFrom, getSelf()), getSelf());
    }

    private void checkIfCloningComplete() {
//        log().debug("checkIfCloningComplete {} + {} = {}", cloneAssignedCount, cloneUnassignedCount, cloneAssignedCount + cloneUnassignedCount);
        if (cloneAssignedCount == 81) {
            log().info("All cels assigned");
        } else if (cloneAssignedCount + cloneUnassignedCount == 81) {
            cloningComplete();
        }
    }

    private void cloningComplete() {
        if (cloneUnassignedLast == null) {
            log().debug("Board generation completed");
            getSelf().tell(new Board.Generated(), getSelf());
        } else {
            log().debug("All cells cloned from {} to {}", boardFrom.path().name(), getSelf().path().name());
            getContext().become(cloned);
            setCellLastUnassigned();
        }
    }

    private void setCellLastUnassigned() {
        int row = cloneUnassignedLast.row;
        int col = cloneUnassignedLast.col;
        int value = Random.inList(cloneUnassignedLast.possibleValues);
        cloneUnassignedLast.possibleValues.removeIf(v -> v == value);
        Board.Cell cell = new Board.Cell(row, col, value);
        log().debug("Set unassigned {}", cell);

        boardTo.tell(new Board.SetCell(cell), getSelf());
        getContext().getSystem().actorOf(BoardGeneratorActor.props(boardNumber, boardTo), String.format("cellGenerator-%d", boardNumber));
    }

    static Props props(int boardNumber, ActorRef boardFrom) {
        return Props.create(BoardGeneratorActor.class, boardNumber, boardFrom);
    }
}
