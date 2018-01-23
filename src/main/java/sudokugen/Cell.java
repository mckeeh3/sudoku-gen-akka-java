package sudokugen;

import java.io.Serializable;

interface Cell {
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
}
