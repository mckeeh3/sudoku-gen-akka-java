package sudokugen;

import akka.actor.AbstractActorWithStash;
import akka.actor.Props;

class CellsUnassignedActor extends AbstractActorWithStash {
    private final Receive initializing;
    private final Receive running;
    private int cellInitializedCount;

    {
        initializing = receiveBuilder()
                .match(Board.SetCell.class, this::setCellInitializing)
                .match(Board.Clone.class, this::boardCloneInitializing)
                .matchEquals("cellUnassignedActorInitialized", this::cellUnassignedActorInitialized)
                .build();

        running = receiveBuilder()
                .match(Board.SetCell.class, this::setCell)
                .match(Board.Clone.class, this::boardClone)
                .build();
    }

    @Override
    public Receive createReceive() {
        return initializing;
    }

    @SuppressWarnings("unused")
    private void setCellInitializing(Board.SetCell setCell) {
        stash();
    }

    @SuppressWarnings("unused")
    private void boardCloneInitializing(Board.Clone boardClone) {
        stash();
    }

    @SuppressWarnings("unused")
    private void cellUnassignedActorInitialized(String message) {
        if (++cellInitializedCount == 81) {
            unstashAll();
            getContext().become(running);
        }
    }

    private void setCell(Board.SetCell setCell) {
        if (getContext().getChildren().iterator().hasNext()) {
            getContext().getChildren().forEach(child -> child.forward(setCell, getContext()));
        } else {
            getSender().tell(new Board.Generated(), getSelf());
        }
    }

    private void boardClone(Board.Clone boardClone) {
        getContext().getChildren().forEach(child -> child.tell(boardClone, getSelf()));
    }

    @Override
    public void preStart() {
        for (int row = 1; row <= 9; row++) {
            for (int col = 1; col <= 9; col++) {
                String name = String.format("unassigned-%d-%d", row, col);
                getContext().actorOf(CellUnassignedActor.props(row, col), name);
            }
        }
    }

    static Props props() {
        return Props.create(CellsUnassignedActor.class);
    }
}
