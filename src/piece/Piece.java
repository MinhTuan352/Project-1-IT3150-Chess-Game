package piece;

import main.Board;
import main.GamePanel;
import main.Type;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

public class Piece {
    public Type type;
    public BufferedImage image;
    public int x, y;    // Toạ độ vẽ quân cờ trên bàn cờ
    public int col, row, preCol, preRow;
    public int color;   // 0: white, 1: black
    public Piece hittingP;
    public boolean moved;   // Check quân đã di chuyển chưa (Áp dụng cho Tốt và Vua)
    public boolean twoStepped;  // Kiểm tra quân Tốt đi 2 nước

    public Piece(int col, int row, int color) {
        this.col = col;
        this.row = row;
        this.color = color;
        x = getX(col);
        y = getY(row);
        preCol = col;
        preRow = row;
    }

    public BufferedImage getImage (String imagePath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(getClass().getResourceAsStream(imagePath + ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }

    public int getX(int col) {
        // Tính toạ độ x dựa trên cột
        return col * Board.SQUARE_SIZE;
    }

    public int getY(int row) {
        // Tính toạ độ y dựa trên hàng
        return row  * Board.SQUARE_SIZE;
    }

    public int getCol(int x) {
        // Tính cột dựa trên toạ độ x
        return (x + Board.HALF_SQUARE_SIZE) / Board.SQUARE_SIZE;
    }

    public int getRow(int y) {
        // Tính hàng dựa trên toạ độ y
        return (y + Board.HALF_SQUARE_SIZE) / Board.SQUARE_SIZE;
    }

    public int getIndex() {
        for (int index = 0; index < GamePanel.simPieces.size(); index++) {
            if (GamePanel.simPieces.get(index) == this) {
                return index;
            }
        }
        return 0;
    }

    public void updatePosition() {
        // Kiểm tra Bắt tốt qua đường (En Passant)
        if (type == Type.PAWN) {
            if (Math.abs(row - preRow) == 2) {
                twoStepped = true;
            }
        }

        // Cập nhật toạ độ vẽ dựa trên cột và hàng hiện tại
        x = getX(col);
        y = getY(row);
        preCol = getCol(x);
        preRow = getRow(y);
        moved = true;
    }

    public void resetPosition() {
        // Đặt lại toạ độ vẽ về vị trí trước đó
        col = preCol;
        row = preRow;
        x = getX(col);
        y = getY(row);
    }

    public boolean canMove(int targetCol, int targetRow) {
        // Kiểm tra xem quân cờ có thể di chuyển đến vị trí (targetCol, targetRow) hay không
        // Mặc định trả về false, các lớp con sẽ ghi đè phương thức này để kiểm tra hợp lệ
        return false;
    }

    public boolean isWithinBoard(int targetCol, int targetRow) {
        // Kiểm tra xem vị trí mục tiêu có nằm trong phạm vi bàn cờ hay không
        if (targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7) {
            return true;
        }
        return false;
    }

    public boolean isSameSquare(int targetCol, int targetRow) {
        if (targetCol == preCol && targetRow == preRow) {
            return true;
        }
        return false;
    }

    public Piece getHittingP(int targetCol, int targetRow) {
        // Phương thức này sẽ giải quyết vấn đề khi quân cờ di chuyển đến vị trí đang có quân cờ khác
        for (Piece piece : GamePanel.simPieces) {
            if (piece.col == targetCol && piece.row == targetRow && piece != this) {
                return piece;
            }
        }
        return null;
    }

    public boolean isValidSquare(int targetCol, int targetRow) {
        hittingP = getHittingP(targetCol, targetRow);
        // Ô này đang trống
        if (hittingP == null) {
            return true;
        } else {
            // Nếu là quân cờ phe đối thủ, có thể ăn quân
            if (hittingP.color != this.color) {
                return true;
            } else {
                hittingP = null;
            }
        }
        return false;
    }

    public boolean pieceIsOnStraightLine(int targetCol, int targetRow) {
        // Khi quân cờ của bạn di chuyển sang trái
        for (int c = preCol - 1; c > targetCol; c--) {
            for (Piece piece: GamePanel.simPieces) {
                // Nếu có quân cờ cản đường ở bên trái
                if (piece.col == c && piece.row == targetRow) {
                    hittingP = piece;
                    return true;
                }
            }
        }

        // Khi quân cờ của bạn di chuyển sang phải
        for (int c = preCol + 1; c < targetCol; c++) {
            for (Piece piece: GamePanel.simPieces) {
                // Nếu có quân cờ cản đường ở bên phải
                if (piece.col == c && piece.row == targetRow) {
                    hittingP = piece;
                    return true;
                }
            }
        }

        // Khi quân cờ của bạn di chuyển lên trên
        for (int r = preRow - 1; r > targetRow; r--) {
            for (Piece piece: GamePanel.simPieces) {
                // Nếu có quân cờ cản đường ở phía trên
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }

        // Khi quân cờ của bạn di chuyển xuống dưới
        for (int r = preRow + 1; r < targetRow; r++) {
            for (Piece piece: GamePanel.simPieces) {
                // Nếu có quân cờ cản đường ở phía dưới
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }

        return false;
    }

    public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow) {
        if (targetRow < preRow) {
            // Khi quân cờ di chuyển lên trái
            for (int c = preCol - 1; c > targetCol; c--) {
                int diff = Math.abs(c - preCol);
                for (Piece piece: GamePanel.simPieces) {
                    if (piece.col == c && piece.row == preRow - diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }

            // Khi quân cờ di chuyển lên phải
            for (int c = preCol + 1; c < targetCol; c++) {
                int diff = Math.abs(c - preCol);
                for (Piece piece: GamePanel.simPieces) {
                    if (piece.col == c && piece.row == preRow - diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }

        } else {
            // Khi quân cờ di chuyển xuống trái
            for (int c = preCol - 1; c > targetCol; c--) {
                int diff = Math.abs(c - preCol);
                for (Piece piece: GamePanel.simPieces) {
                    if (piece.col == c && piece.row == preRow + diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }

            // Khi quân cờ di chuyển xuống phải
            for (int c = preCol + 1; c < targetCol; c++) {
                int diff = Math.abs(c - preCol);
                for (Piece piece: GamePanel.simPieces) {
                    if (piece.col == c && piece.row == preRow + diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void draw(Graphics2D g2) {
        g2.drawImage(image, x, y, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
    }

    // Vẽ quân cờ với kích thước tuỳ chỉnh
    public void draw(Graphics2D g2, int width, int height) {
        g2.drawImage(image, x, y, width, height, null);
    }
    
    // Tạo bản sao của quân cờ để undo
    public Piece getCopy() {
        Piece newPiece = null;

        // Tạo quân mới đúng loại dựa theo Type
        if (this instanceof Pawn) {
            newPiece = new Pawn(col, row, color);
        } else if (this instanceof Rook) {
            newPiece = new Rook(col, row, color);
        } else if (this instanceof Knight) {
            newPiece = new Knight(col, row, color);
        } else if (this instanceof Bishop) {
            newPiece = new Bishop(col, row, color);
        } else if (this instanceof Queen) {
            newPiece = new Queen(col, row, color);
        } else if (this instanceof King) {
            newPiece = new King(col, row, color);
        }

        // Sao chép các thuộc tính
        newPiece.image = this.image;
        newPiece.type = this.type;
        newPiece.preCol = this.preCol;
        newPiece.preRow = this.preRow;
        newPiece.x = this.x;
        newPiece.y = this.y;
        newPiece.moved = this.moved;
        newPiece.twoStepped = this.twoStepped;

        return newPiece;
    }
}