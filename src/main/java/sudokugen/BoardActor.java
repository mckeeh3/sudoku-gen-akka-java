package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

class BoardActor extends AbstractLoggingActor {
    private final ActorRef cellsUnassigned;
    private final ActorRef cellsAssigned;

    private final Receive generating;
    private final Receive generated;
    private final Receive failed;

    private Board.Clone clone;

    private enum State {
        generating, generated, failed
    }

    {
        cellsUnassigned = getContext().actorOf(CellsUnassignedActor.props(), "cellsUnassigned");
        cellsAssigned = getContext().actorOf(CellsAssignedActor.props(), "cellsAssigned");

        generating = receiveBuilder()
                .match(Board.Clone.class, this::boardClone)
                .match(Board.SetCell.class, this::setCell)
                .match(Board.Invalid.class, this::boardInvalid)
                .match(Board.Generated.class, this::boardGenerated)
                .build();

        generated = receiveBuilder()
                .match(Board.Generated.class, this::boardGeneratedIgnore)
                .build();

        failed = receiveBuilder()
                .build();
    }

    @Override
    public Receive createReceive() {
        return generating;
    }

    private void boardClone(Board.Clone boardClone) {
        log().debug("Clone board {}", boardClone);
        cellsUnassigned.tell(boardClone, getSelf());
        cellsAssigned.tell(boardClone, getSelf());
        clone = boardClone;
    }

    private void setCell(Board.SetCell setCell) {
//        log().debug("{} {}", setCell, getSender().path().name());
        cellsUnassigned.tell(setCell, getSelf());
        cellsAssigned.tell(setCell, getSelf());

        if (clone != null) {
            clone.boardTo.tell(setCell, getSelf());
        }
    }

    private void boardInvalid(Board.Invalid boardInvalid) {
        log().info("Board invalid {}", boardInvalid);
        getContext().getParent().tell(generated, getSelf());
        become(State.failed);
    }

    private void boardGenerated(Board.Generated generated) {
        log().info("Board generated {}", generated);
        getContext().getParent().tell(generated, getSelf());
        become(State.generated);
    }

    @SuppressWarnings("unused")
    private void boardGeneratedIgnore(Board.Generated boardGenerated) {
        // ignore message
    }

    private void become(State state) {
        log().debug("Become state {}", state);
        switch (state) {
            case generating:
                getContext().become(generating);
                break;
            case failed:
                getContext().become(failed);
                break;
            case generated:
                getContext().become(generated);
                break;
        }
    }

    static Props props() {
        return Props.create(BoardActor.class);
    }
}
