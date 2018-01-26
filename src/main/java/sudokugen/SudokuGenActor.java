package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SudokuGenActor extends AbstractLoggingActor {
    private int boardNumber = 0;
    private ActorRef runner;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SudokuGen.Level.class, this::boardGenerate)
                .match(Board.NextBoard.class, this::nextBoard)
                .match(Board.Generated.class, this::boardGenerated)
                .match(Board.FetchUnassignedCells.class, this::bootstrapFirstBoard)
                .build();
    }

    private void boardGenerate(SudokuGen.Level difficultyLevel) {
        log().info("Generate board with difficulty level {}", difficultyLevel);
        runner = getSender();

        bootstrapFirstBoard();
    }

    private void nextBoard(Board.NextBoard nextBoard) {
        log().debug("Created {} requested by {}", nextBoard, getSender().path().name());
        triggerCopyOfAssignedCellsFromPrevToNextBoard();
    }

    private void boardGenerated(Board.Generated boardGenerated) {
        log().info("Board {}", boardGenerated);

        runner.tell(boardGenerated, getSelf());
    }

    @SuppressWarnings("unused")
    private void bootstrapFirstBoard(Board.FetchUnassignedCells fetchUnassignedCells) {
        List<Integer> possibleValues = new ArrayList<>();
        Collections.addAll(possibleValues, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        getSender().tell(new Board.UnassignedCellTotal(81), getSelf());

        for (int row = 1; row <= 9; row++) {
            for (int col = 1; col <= 9; col++) {
                getSender().tell(new Board.UnassignedCell(row, col, possibleValues), getSelf());
            }
        }
    }

    private void bootstrapFirstBoard() {
        ActorRef board = createBoardActor(getSelf());

        board.tell(new Board.AssignedCellTotal(0), getSelf());
    }

    private void triggerCopyOfAssignedCellsFromPrevToNextBoard() {
        if (boardNumber < 81) {
            ActorRef board = createBoardActor(getSender());
            getSender().tell(new Board.FetchAssignedCells(), board);
        } else {
            runner.tell(new Board.Invalid("Board generation failed"), getSelf());
        }
    }

    private ActorRef createBoardActor(ActorRef boardPrev) {
        return getContext().actorOf(BoardActor.props(boardPrev), String.format("board-%d", ++boardNumber));
    }

    static Props props() {
        return Props.create(SudokuGenActor.class);
    }
}
