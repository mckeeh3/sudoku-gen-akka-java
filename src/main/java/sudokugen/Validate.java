package sudokugen;

import java.io.Serializable;

interface Validate {
    class Board implements Serializable {
    }

    class Valid implements Serializable {
        final String message;

        Valid(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), message);
        }
    }

    class ValidBoard implements Serializable {
        final String message;
        final sudokugen.Board.Grid grid;

        ValidBoard(String message, sudokugen.Board.Grid grid) {
            this.message = message;
            this.grid = grid;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), message);
        }
    }

    class Invalid implements Serializable {
        final String message;

        Invalid(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), message);
        }
    }
}
