package piece;

import main.GamePanel;
import main.Type;

public class King extends Piece {
    public King(int col, int row, int color) {
        super(col, row, color);
        type = Type.KING;
        if (color == GamePanel.WHITE) {
            this.image = getImage("/piece/w-king");
        } else {
            this.image = getImage("/piece/b-king");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow)) {
            // Vua chỉ có thể di chuyển một ô theo mọi hướng
            if (Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) == 1 || Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 1) {
                if (isValidSquare(targetCol, targetRow)) {
                    return true;
                }
            }

            // Vua nhập thành với Xe
            if (!moved) {
                // Nhập thành phải
                if (targetCol == preCol + 2 && targetRow == preRow && !pieceIsOnStraightLine(targetCol, targetRow)) {
                    for (Piece piece: GamePanel.simPieces) {
                        if (piece.col == preCol + 3 && piece.row == preRow && !piece.moved) {
                            GamePanel.castlingP = piece;
                            return true;
                        }
                    }
                }
                // Nhập thành trái
                if (targetCol == preCol - 2 && targetRow == preRow && !pieceIsOnStraightLine(targetCol, targetRow)) {
                    Piece p[] = new Piece[2];   // Tạo mảng chứa quân Xe (p[1]) và quân Mã (p[0])
                    for (Piece piece: GamePanel.simPieces) {
                        if (piece.col == preCol - 3 && piece.row == preRow) {
                            p[0] = piece;   // Kiểm tra xem quân Mã có ở đây không
                        }
                        if (piece.col == preCol - 4 && piece.row == preRow) {
                            p[1] = piece;   // Kiểm tra xem quân Xe có ở đây không
                        }
                        if (p[0] == null && p[1] != null && !p[1].moved) {
                            GamePanel.castlingP = p[1];
                            return true;
                        }
                    }
                }
            }

        }
        return false;
    }
}