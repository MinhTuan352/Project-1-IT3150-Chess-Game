package main;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Lớp xử lý các sự kiện chuột của người chơi
public class Mouse extends MouseAdapter {

    public int x, y; // Toạ độ hiện tại của con trỏ chuột trên panel
    public boolean pressed; // Trạng thái nút chuột: true = đang nhấn, false = đã thả

    // Xử lý khi nút chuột được nhấn xuống
    @Override
    public void mousePressed(MouseEvent e) {
        pressed = true;
    }

    // Xử lý khi nút chuột được thả ra
    @Override
    public void mouseReleased(MouseEvent e) {
        pressed = false;
    }

    // Xử lý khi chuột được kéo (nhấn giữ và di chuyển)
    @Override
    public void mouseDragged(MouseEvent e) {
        x = e.getX();
        y = e.getY();
    }

    // Xử lý khi chuột di chuyển (không nhấn)
    @Override
    public void mouseMoved(MouseEvent e) {
        x = e.getX();
        y = e.getY();
    }
}
