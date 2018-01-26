package sudokugen;

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

    class AllCellsAssigned implements Serializable {
        @Override
        public String toString() {
            return String.format("%s[]", getClass().getSimpleName());
        }
    }

    class NextBoard implements Serializable {
        @Override
        public String toString() {
            return String.format("%s[]", getClass().getSimpleName());
        }
    }

    class Generated implements Serializable {
        final Grid grid;

        Generated(Grid grid) {
            this.grid = grid;
        }

        @Override
        public String toString() {
            return String.format("%s[]", getClass().getSimpleName());
        }
    }

    class Invalid implements Serializable {
        private final String message;

        Invalid(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), message);
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

    class UnassignedNoChange implements Serializable {
        final int row;
        final int col;
        final List<Integer> possibleValues;
        final Board.SetCell setCell;

        UnassignedNoChange(int row, int col, List<Integer> possibleValues, Board.SetCell setCell) {
            this.row = row;
            this.col = col;
            this.possibleValues = new ArrayList<>(possibleValues);
            this.setCell = setCell;
        }

        @Override
        public String toString() {
            return String.format("%s[(%d, %d) %s]", getClass().getSimpleName(), row, col, possibleValues);
        }
    }

    class FetchAssignedCells implements Serializable {
        @Override
        public String toString() {
            return String.format("%s[]", getClass().getSimpleName());
        }
    }

    class AssignedCellTotal implements Serializable {
        final int total;

        AssignedCellTotal(int total) {
            this.total = total;
        }

        @Override
        public String toString() {
            return String.format("%s[%d]", getClass().getSimpleName(), total);
        }
    }

    class AssignedCell implements Serializable {
        final Board.Cell cell;

        AssignedCell(Cell cell) {
            this.cell = cell;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), cell);
        }
    }

    class FetchUnassignedCells implements Serializable {
        @Override
        public String toString() {
            return String.format("%s[]", getClass().getSimpleName());
        }
    }

    class UnassignedCellTotal implements Serializable {
        final int total;

        UnassignedCellTotal(int total) {
            this.total = total;
        }

        @Override
        public String toString() {
            return String.format("%s[%d]", getClass().getSimpleName(), total);
        }
    }

    class UnassignedCell implements Serializable {
        final int row;
        final int col;
        final List<Integer> possibleValues;

        UnassignedCell(int row, int col, List<Integer> possibleValues) {
            this.row = row;
            this.col = col;
            this.possibleValues = possibleValues;
        }

        @Override
        public String toString() {
            return String.format("%s[(%d, %d) %s]", getClass().getSimpleName(), row, col, possibleValues);
        }
    }

    class Reset implements Serializable {
        @Override
        public String toString() {
            return String.format("%s[]", getClass().getSimpleName());
        }
    }

    class Grid implements Serializable {
        private Row[] grid = new Row[9];

        {
            for (int row = 0; row < 9; row++) {
                grid[row] = new Row();
            }
        }

        private static class Row {
            private final Cell[] row = new Cell[9];
        }

        void set(Cell cell) {
            grid[cell.row - 1].row[cell.col - 1] = cell;
        }

        Cell get(int row, int col) {
            return grid[row - 1].row[col - 1];
        }

        @Override
        public String toString() {
            String delimiter = "| ";
            StringBuilder grid = new StringBuilder();
            for (int row = 1; row <= 9; row++) {
                grid.append("-------------------------------------\n");
                for (int col = 1; col <= 9; col++) {
                    Cell cell = get(row, col);
                    int value = cell == null ? 0 : cell.value;
                    grid.append(delimiter).append(value == 0 ? " " : value);
                    delimiter = " | ";
                }
                grid.append(" |\n");
                delimiter = "| ";
            }
            grid.append("-------------------------------------\n");

            return grid.toString();
        }
    }
}
