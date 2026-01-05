package piece;

import main.GameLogic;
import main.Type;

public class Knight extends Piece {
    public Knight(int col, int row, int color) {
        super(col, row, color);
        type = Type.KNIGHT;
        if (color == GameLogic.WHITE) {
            this.image = getImage("/piece/w-knight");
        } else {
            this.image = getImage("/piece/b-knight");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow)) {
            // Mã chỉ có thể di chuyển theo hình chữ L (tỉ lệ col:row luôn là 1:2 hoặc 2:1)
            if (Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 2) {
                if (isValidSquare(targetCol, targetRow)) {
                    return true;
                }
            }
        }
        return false;
    }
}