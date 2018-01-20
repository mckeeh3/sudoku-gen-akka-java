package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

class SudokuGenActor extends AbstractLoggingActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SudokuGen.Level.class, this::generateBoard)
                .build();
    }

    private void generateBoard(SudokuGen.Level difficultyLevel) {
        log().info("Generate board with difficulty level {}", difficultyLevel);

        // TODO
        getSender().tell("[-A grid-]", getSelf());
    }

    static Props props() {
        return Props.create(SudokuGenActor.class);
    }
}
