package sudokugen;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

interface Board {
    class Cell implements Serializable {
        final int row;
        final int col;
        final int value;

        Cell(int row, int col, int value) {
            this.row = row;
            this.col = col;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cell cell = (Cell) o;
            return row == cell.row &&
                    col == cell.col &&
                    value == cell.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, col, value);
        }

        @Override
        public String toString() {
            return String.format("%s[(%d, %d) = %d]", getClass().getSimpleName(), row, col, value);
        }
    }

    class Generate implements Serializable {
        @Override
        public String toString() {
            return String.format("%s[]", getClass().getSimpleName());
        }
    }

    class Generated implements Serializable {
        @Override
        public String toString() {
            return String.format("%s[]", getClass().getSimpleName());
        }
    }

    class Invalid implements Serializable {
        @Override
        public String toString() {
            return String.format("%s[]", getClass().getSimpleName());
        }
    }

    class Clone implements Serializable {
        final ActorRef boardFrom;
        final ActorRef boardTo;

        Clone(ActorRef boardFrom, ActorRef boardTo) {
            this.boardFrom = boardFrom;
            this.boardTo = boardTo;
        }

        @Override
        public String toString() {
            return String.format("%s[from %s, to %s]", getClass().getSimpleName(), boardFrom.path().name(), boardTo.path().name());
        }
    }

    class SetCell implements Serializable {
        final Cell cell;

        SetCell(Cell cell) {
            this.cell = cell;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SetCell setCell = (SetCell) o;
            return Objects.equals(cell, setCell.cell);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cell);
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), cell);
        }
    }

    class CloneUnassigned implements Serializable {
        final int row;
        final int col;
        final List<Integer> possibleValues;

        CloneUnassigned(int row, int col, List<Integer> possibleValues) {
            this.row = row;
            this.col = col;
            this.possibleValues = new ArrayList<>(possibleValues);
        }

        @Override
        public String toString() {
            return String.format("%s[(%d, %d) %s]", getClass().getSimpleName(), row, col, possibleValues);
        }
    }

    class CloneAssigned implements Serializable {
        final Cell cell;

        public CloneAssigned(Cell cell) {
            this.cell = cell;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), cell);
        }
    }
}
