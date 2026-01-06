package main;

import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;

public class MenuPanel extends JPanel {

    public MenuPanel() {
        // Thiết lập kích thước giống hệt GamePanel để khi chuyển cảnh không bị giật
        setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));

        // Sử dụng BoxLayout để xếp các phần tử theo chiều dọc
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Tiêu đề
        JLabel titleLabel = new JLabel("CHESS GAME");
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 60));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang

        // Sử dụng hàm phụ trợ createButton để tạo nút
        JButton btnPvP = new RoundedButton("Đấu với người (PvP)");
        JButton btnPvE = new RoundedButton("Đấu với máy (PvE)");

        // Nút Đấu với người -> Chuyển sang Setup với chế độ PvP (isPvE = false)
        btnPvP.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.instance.showSetup(false);
            }
        });

        // Nút Đấu với máy -> Chuyển sang Setup với chế độ PvE (isPvE = true)
        btnPvE.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.instance.showSetup(true);
            }
        });

        // Nút Tải ván đấu
        JButton btnLoad = new RoundedButton("Tải ván đấu");
        btnLoad.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser("res/saves");
                fileChooser.setDialogTitle("Chọn file save game");
                fileChooser.setFileFilter(
                        new FileNameExtensionFilter("Save files (*.txt)", "txt"));

                int result = fileChooser.showOpenDialog(Main.instance);
                if (result == JFileChooser.APPROVE_OPTION) {
                    String filepath = fileChooser.getSelectedFile().getAbsolutePath();
                    Main.instance.loadSavedGame(filepath);
                }
            }
        });

        // Glue ở đầu và cuối giúp đẩy nội dung vào chính giữa theo chiều dọc
        add(Box.createVerticalGlue());
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 50))); // Khoảng cách cố định 50px dưới tiêu đề
        add(btnPvP);
        add(Box.createRigidArea(new Dimension(0, 20))); // Khoảng cách 20px giữa 2 nút
        add(btnPvE);
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(btnLoad);
        add(Box.createVerticalGlue());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Vẽ họa tiết bàn cờ chìm làm nền
        int size = 80; // Kích thước ô nền trang trí
        for (int row = 0; row < getHeight() / size + 1; row++) {
            for (int col = 0; col < getWidth() / size + 1; col++) {
                if ((row + col) % 2 == 0) {
                    g2.setColor(new Color(30, 30, 30)); // Xám đậm
                } else {
                    g2.setColor(new Color(45, 45, 45)); // Xám vừa
                }
                g2.fillRect(col * size, row * size, size, size);
            }
        }

        // Phủ một lớp đen bán trong suốt lên trên để làm nền tối đi, giúp nút nổi bật
        // hơn
        g2.setColor(new Color(0, 0, 0, 80)); // Alpha 80/255
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    // Class con kế thừa JButton để thay đổi cách vẽ
    private class RoundedButton extends JButton {
        private boolean isHovered = false; // Trạng thái di chuột

        public RoundedButton(String text) {
            super(text);
            setFont(new Font("Arial", Font.PLAIN, 24));
            // Màu chữ khi chưa hover (Xám đậm cho dễ đọc trên nền trắng)
            setForeground(new Color(40, 40, 40));

            setContentAreaFilled(false); // Tắt nền mặc định của Swing
            setFocusPainted(false); // Tắt viền focus nét đứt
            setBorderPainted(false); // Tắt viền nổi mặc định

            setMaximumSize(new Dimension(300, 60));
            setAlignmentX(Component.CENTER_ALIGNMENT);

            // Thêm sự kiện chuột để tạo hiệu ứng Hover
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    setCursor(new Cursor(Cursor.HAND_CURSOR)); // Đổi con trỏ thành bàn tay
                    repaint(); // Vẽ lại nút
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            // Bật khử răng cưa (Antialiasing) để đường cong mịn màng
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Chọn màu nền dựa trên trạng thái chuột
            if (isHovered) {
                g2.setColor(new Color(230, 230, 230)); // Trắng xám (Sáng hơn)
            } else {
                g2.setColor(Color.white); // Trắng tinh
            }

            // Vẽ hình chữ nhật bo tròn (Radius = 30)
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

            // Vẽ chữ lên trên (Super class sẽ tự xử lý việc căn giữa chữ)
            super.paintComponent(g);
        }
    }
}