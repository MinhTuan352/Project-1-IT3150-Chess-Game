package main;

import java.awt.Graphics2D;
import java.awt.Color;

public class Board {
    final int MAX_ROWS = 8;
    final int MAX_COLS = 8;
    public static final int SQUARE_SIZE = 80; // Kích thước mỗi ô vuông trên bàn cờ
    public static final int HALF_SQUARE_SIZE = SQUARE_SIZE / 2;

    public void draw(Graphics2D g2) {
        // Tô toàn bộ bàn cờ bằng màu sáng trước
        g2.setColor(new Color(240, 237, 212));
        g2.fillRect(0, 0, MAX_COLS * SQUARE_SIZE, MAX_ROWS * SQUARE_SIZE);

        // Vẽ các ô màu tối đè lên
        g2.setColor(new Color(108, 140, 100));
        for (int row = 0; row < MAX_ROWS; row++) {
            for (int col = 0; col < MAX_COLS; col++) {
                // Chỉ vẽ ô có tổng lẻ
                if ((row + col) % 2 != 0) {
                    g2.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
                }
            }
        }
    }

    // Chuyển tọa độ col, row thành ký hiệu cờ vua (ví dụ: 0,0 -> a8; 7,7 -> h1)
    public String getSquareCoordinates(int col, int row) {
        // Chuyển đổi cột: 0->a, 1->b, ...
        char file = (char) ('a' + col);
        
        // Chuyển đổi hàng: 0->8, 7->1
        int rank = 8 - row;
        
        return "" + file + rank;
    }
}