package sudokugen;

import java.io.Serializable;
import java.util.Objects;

interface Board {
    class Cell implements Serializable {
        final int row;
        final int col;
        final int value;

        public Cell(int row, int col, int value) {
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

    class CellAssigned implements Serializable {
        final Cell cell;

        public CellAssigned(Cell cell) {
            this.cell = cell;
        }
    }

    class CellUnassigned implements Serializable {
        final Cell cell;

        public CellUnassigned(Cell cell) {
            this.cell = cell;
        }
    }
}
