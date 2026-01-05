package piece;

import main.GamePanel;
import main.Type;

public class Queen extends Piece {
    public Queen(int col, int row, int color) {
        super(col, row, color);
        type = Type.QUEEN;
        if (color == GamePanel.WHITE) {
            this.image = getImage("/piece/w-queen");
        } else {
            this.image = getImage("/piece/b-queen");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow) && isValidSquare(targetCol, targetRow)) {
            // Hậu di chuyển dọc ngang và chéo (Kết hợp của Xe và Tượng)
            if (targetCol == preCol || targetRow == preRow) {
                if (!pieceIsOnStraightLine(targetCol, targetRow)) {
                    return true;
                }
            }
            if (Math.abs(targetCol - preCol) == Math.abs(targetRow - preRow)) {
                if (!pieceIsOnDiagonalLine(targetCol, targetRow)) {
                    return true;
                }
            }
        }
        return false;
    }
}