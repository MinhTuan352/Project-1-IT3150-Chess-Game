package piece;

import main.GameLogic;
import main.Type;

public class King extends Piece {
    public King(int col, int row, int color) {
        super(col, row, color);
        type = Type.KING;
        if (color == GameLogic.WHITE) {
            this.image = getImage("/piece/w-king");
        } else {
            this.image = getImage("/piece/b-king");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow)) {
            // Vua chỉ có thể di chuyển một ô theo mọi hướng
            if (Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) == 1
                    || Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 1) {
                if (isValidSquare(targetCol, targetRow)) {
                    return true;
                }
            }

            // Vua nhập thành với Xe
            if (!moved) {
                // Nhập thành phải
                if (targetCol == preCol + 2 && targetRow == preRow && !pieceIsOnStraightLine(targetCol, targetRow)) {
                    for (Piece piece : GameLogic.simPieces) {
                        if (piece.col == preCol + 3 && piece.row == preRow && !piece.moved) {
                            // Kiểm tra Vua không bị chiếu ở vị trí hiện tại, ô trung gian, và ô đích
                            if (!isSquareUnderAttack(preCol, preRow) && !isSquareUnderAttack(preCol + 1, preRow)
                                    && !isSquareUnderAttack(preCol + 2, preRow)) {
                                GameLogic.castlingP = piece;
                                return true;
                            }
                        }
                    }
                }
                // Nhập thành trái
                if (targetCol == preCol - 2 && targetRow == preRow && !pieceIsOnStraightLine(targetCol, targetRow)) {
                    Piece p[] = new Piece[2]; // Tạo mảng chứa quân Xe (p[1]) và quân Mã (p[0])
                    for (Piece piece : GameLogic.simPieces) {
                        if (piece.col == preCol - 3 && piece.row == preRow) {
                            p[0] = piece; // Kiểm tra xem quân Mã có ở đây không
                        }
                        if (piece.col == preCol - 4 && piece.row == preRow) {
                            p[1] = piece; // Kiểm tra xem quân Xe có ở đây không
                        }
                        if (p[0] == null && p[1] != null && !p[1].moved) {
                            // Kiểm tra Vua không bị chiếu ở vị trí hiện tại, các ô trung gian, và ô đích
                            if (!isSquareUnderAttack(preCol, preRow) && !isSquareUnderAttack(preCol - 1, preRow)
                                    && !isSquareUnderAttack(preCol - 2, preRow)) {
                                GameLogic.castlingP = p[1];
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // Phương thức kiểm tra xem một ô có bị quân đối phương tấn công không
    private boolean isSquareUnderAttack(int col, int row) {
        for (Piece piece : GameLogic.simPieces) {
            // Chỉ kiểm tra quân đối phương
            if (piece.color != this.color && piece != this) {
                // Kiểm tra xem quân đối phương có thể di chuyển đến ô này không
                if (piece.canMove(col, row)) {
                    return true;
                }
            }
        }
        return false;
    }
}