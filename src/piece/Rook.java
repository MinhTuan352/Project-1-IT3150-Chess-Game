package piece;

import main.GamePanel;
import main.Type;

public class Rook extends Piece {
    public Rook(int col, int row, int color) {
        super(col, row, color);
        type = Type.ROOK;
        if (color == GamePanel.WHITE) {
            this.image = getImage("/piece/w-rook");
        } else {
            this.image = getImage("/piece/b-rook");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            // Xe luôn di chuyển theo đường thẳng (dọc hoặc ngang)
            if (targetCol == preCol || targetRow == preRow) {
                if (isValidSquare(targetCol, targetRow) && !pieceIsOnStraightLine(targetCol, targetRow)) {
                    return true;
                }
            }
        }
        return false;
    }
}