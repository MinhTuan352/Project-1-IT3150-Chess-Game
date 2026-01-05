package piece;

import main.GamePanel;
import main.Type;

public class Bishop extends Piece {
    public Bishop(int col, int row, int color) {
        super(col, row, color);
        type = Type.BISHOP;
        if (color == GamePanel.WHITE) {
            this.image = getImage("/piece/w-bishop");
        } else {
            this.image = getImage("/piece/b-bishop");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            // Tượng luôn di chuyển theo đường chéo
            if (Math.abs(targetCol - preCol) == Math.abs(targetRow - preRow)) {
                if (isValidSquare(targetCol, targetRow) && !pieceIsOnDiagonalLine(targetCol, targetRow)) {
                    return true;
                }
            }
        }
        return false;
    }
}