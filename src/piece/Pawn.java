package piece;

import main.GameLogic;
import main.Type;

// Quân Tốt: Đi thẳng 1 ô (đi 2 ô nước đầu), ăn chéo, phong cấp khi đến hàng cuối
public class Pawn extends Piece {
    public Pawn(int col, int row, int color) {
        super(col, row, color);
        type = Type.PAWN;
        if (color == GameLogic.WHITE) {
            this.image = getImage("/piece/w-pawn");
        } else {
            this.image = getImage("/piece/b-pawn");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            // Tốt nước đầu đi 1-2 ô lên, sau đó đi thẳng 1 ô mỗi nước, ăn chéo (Có thể bắt
            // Tốt qua đường)
            int moveValue; // Check màu quân, trắng đi lên, đen đi xuống
            if (color == GameLogic.WHITE) {
                moveValue = -1;
            } else {
                moveValue = 1;

            }

            hittingP = getHittingP(targetCol, targetRow);

            // Đi thẳng 1 ô (Không bị chặn quân trước mặt)
            if (targetCol == preCol && targetRow == preRow + moveValue && hittingP == null) {
                return true;
            }

            // Đi thẳng 2 ô (Nước đầu của quân Tốt + Không bị chặn quân)
            if (targetCol == preCol && targetRow == preRow + moveValue * 2 && hittingP == null && !moved
                    && !pieceIsOnStraightLine(targetCol, targetRow)) {
                return true;
            }

            // Ăn chéo quân
            if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue && hittingP != null
                    && hittingP.color != color) {
                return true;
            }

            // Bắt tốt qua đường (En Passant)
            if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue) {
                for (Piece piece : GameLogic.simPieces) {
                    if (piece.col == targetCol && piece.row == preRow && piece.twoStepped == true) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}