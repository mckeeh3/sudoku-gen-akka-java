package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

class SudokuGenActor extends AbstractLoggingActor {
    private int boardNumber = 0;
    private ActorRef runner;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SudokuGen.Level.class, this::boardGenerate)
                .match(Board.NextBoard.class, this::nextBoard)
                .match(Board.Generated.class, this::boardGenerated)
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
        log().info("{}", boardGenerated);

        runner.tell(boardGenerated, getSelf());
    }

    private void bootstrapFirstBoard() {
        ActorRef board = createBoardActor();

        board.tell(new Board.AssignedCellTotal(1), getSelf());
        board.tell(new Board.AssignedCell(randomCell()), getSelf());
//        board.tell(new Board.CopiedAssignedCells(), getSelf());
    }

    private Board.Cell randomCell() {
        int row = Random.inRange(1, 9);
        int col = Random.inRange(1, 9);
        int value = Random.inRange(1, 9);

        return new Board.Cell(row, col, value);
    }

    private void triggerCopyOfAssignedCellsFromPrevToNextBoard() {
        if (boardNumber < 81) {
            ActorRef board = createBoardActor();
            getSender().tell(new Board.FetchAssignedCells(), board);
        } else {
            runner.tell(new Board.Invalid("Board generation failed"), getSelf());
        }
    }

    private ActorRef createBoardActor() {
        return getContext().actorOf(BoardActor.props(getSender()), String.format("board-%d", ++boardNumber));
    }

    static Props props() {
        return Props.create(SudokuGenActor.class);
    }
}
