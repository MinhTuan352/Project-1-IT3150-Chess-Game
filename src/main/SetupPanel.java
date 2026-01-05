package main;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SetupPanel extends JPanel {

    // Các thành phần giao diện
    private JTextField p1NameField;
    private JTextField p2NameField;
    private JComboBox<TimeOption> timeComboBox;

    // 3 nút chọn màu
    private ColorButton btnWhite, btnBlack, btnRandom;
    private int selectedColorChoice = 0; // 0: White, 1: Black, 2: Random (Mặc định là Trắng)

    // Trạng thái hiện tại (PvE hay PvP)
    private boolean isPvEMode;

    public SetupPanel() {
        setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));
        setLayout(new GridBagLayout()); // Căn giữa mọi thứ

        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Khoảng cách giữa các ô
        gbc.fill = GridBagConstraints.HORIZONTAL; // Kéo giãn ngang cho đẹp

        // Tiêu đề
        JLabel titleLabel = new JLabel("CÀI ĐẶT TRẬN ĐẤU", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel.setForeground(Color.white);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Chiếm trọn 2 cột
        add(titleLabel, gbc);

        // Nhập tên Player 1
        p1NameField = addInputField("Tên Player 1:", 1, gbc);

        // Nhập tên Player 2/ AI
        p2NameField = addInputField("Tên Player 2:", 2, gbc);

        // Chọn màu quân
        JLabel colorLabel = new JLabel("Chọn màu quân (Player 1):");
        colorLabel.setForeground(Color.white);
        colorLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        add(colorLabel, gbc);

        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        colorPanel.setOpaque(false); // Trong suốt để hiển thị nền của SetupPanel

        // Tạo 3 nút chọn màu
        btnWhite = new ColorButton(0); // Trắng
        btnBlack = new ColorButton(1); // Đen
        btnRandom = new ColorButton(2); // Random

        ActionListener colorAction = e -> {
            ColorButton src = (ColorButton) e.getSource();
            selectColorButton(src.type);
        };

        // Thêm sự kiện click
        btnWhite.addActionListener(colorAction);
        btnBlack.addActionListener(colorAction);
        btnRandom.addActionListener(colorAction);

        colorPanel.add(btnWhite);
        colorPanel.add(btnBlack);
        colorPanel.add(btnRandom);

        // Mặc định chọn nút đầu tiên
        selectColorButton(0);

        gbc.gridx = 1;
        add(colorPanel, gbc);

        // Chọn thời gian
        JLabel timeLabel = new JLabel("Thời gian:");
        timeLabel.setForeground(Color.white);
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        gbc.gridy = 4;
        gbc.gridx = 0;
        add(timeLabel, gbc);

        // Tạo danh sách các mốc thời gian
        TimeOption[] options = {
                new TimeOption("1 phút", 60),
                new TimeOption("3 phút", 180),
                new TimeOption("5 phút", 300),
                new TimeOption("10 phút", 600),
                new TimeOption("15 phút", 900),
                new TimeOption("30 phút", 1800),
                new TimeOption("Không giới hạn", -1)
        };
        timeComboBox = new JComboBox<>(options);
        timeComboBox.setSelectedIndex(3); // Mặc định chọn 10 phút

        gbc.gridx = 1;
        add(timeComboBox, gbc);

        // Các nút hành động ở dưới cùng
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false); // Trong suốt để hiển thị nền của SetupPanel

        JButton btnBack = new JButton("Quay lại");
        JButton btnStart = new JButton("BẮT ĐẦU");
        btnStart.setFont(new Font("Arial", Font.BOLD, 16));

        // Sự kiện nút Quay lại -> Về Menu
        btnBack.addActionListener(e -> Main.instance.returnToMenu());

        // Sự kiện nút Start -> Tạo Settings và bắt đầu game
        btnStart.addActionListener(e -> startGame());

        buttonPanel.add(btnBack);
        buttonPanel.add(btnStart);

        gbc.gridy = 5;
        gbc.gridwidth = 2; // Chiếm 2 cột dưới cùng
        add(buttonPanel, gbc);
    }

    // --- TỰ VẼ NỀN (CUSTOM BACKGROUND) ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Vẽ bàn cờ chìm (Copy logic từ MenuPanel)
        int size = 80;
        for (int row = 0; row < getHeight() / size + 1; row++) {
            for (int col = 0; col < getWidth() / size + 1; col++) {
                if ((row + col) % 2 == 0)
                    g2.setColor(new Color(30, 30, 30));
                else
                    g2.setColor(new Color(45, 45, 45));
                g2.fillRect(col * size, row * size, size, size);
            }
        }
        // Lớp phủ đen mờ
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    // Hàm thêm nhanh một dòng Label + TextField
    private JTextField addInputField(String labelText, int y, GridBagConstraints gbc) {
        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(Color.white);
        lbl.setFont(new Font("Arial", Font.PLAIN, 18));

        gbc.gridy = y;
        gbc.gridx = 0;
        add(lbl, gbc);

        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(200, 30));

        // Giới hạn ký tự nhập (Max 15)
        field.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                if (str == null)
                    return;
                if ((getLength() + str.length()) <= 15) {
                    super.insertString(offs, str, a);
                }
            }
        });

        gbc.gridx = 1;
        add(field, gbc);
        return field;
    }

    // Hàm reset lại form mỗi khi vào màn hình Setup
    public void resetForm(boolean isPvE) {
        this.isPvEMode = isPvE;

        // Reset tên
        p1NameField.setText("");

        if (isPvE) {
            p2NameField.setText("AI Cờ Vua");
            p2NameField.setEditable(false); // Không cho sửa tên AI
        } else {
            p2NameField.setText("");
            p2NameField.setEditable(true); // PvP thì cho nhập tên P2
        }

        // Reset các lựa chọn khác về mặc định
        selectColorButton(0); // White
        timeComboBox.setSelectedIndex(3); // 10 phút
    }

    // Hàm xử lý hiển thị viền vàng quanh nút màu được chọn
    private void selectColorButton(int type) {
        selectedColorChoice = type;

        // Reset viền xám cho tất cả
        btnWhite.setBorder(new LineBorder(Color.gray, 1));
        btnBlack.setBorder(new LineBorder(Color.gray, 1));
        btnRandom.setBorder(new LineBorder(Color.gray, 1));

        // Tô viền vàng đậm cho nút đang chọn
        if (type == 0)
            btnWhite.setBorder(new LineBorder(Color.yellow, 3));
        if (type == 1)
            btnBlack.setBorder(new LineBorder(Color.yellow, 3));
        if (type == 2)
            btnRandom.setBorder(new LineBorder(Color.yellow, 3));
    }

    // Hàm tổng hợp dữ liệu và bắt đầu game
    private void startGame() {
        // Lấy tên (Nếu để trống thì đặt tên mặc định)
        String p1 = p1NameField.getText().trim();
        if (p1.isEmpty())
            p1 = "Player 1";
        String p2 = p2NameField.getText().trim();
        if (p2.isEmpty())
            p2 = isPvEMode ? "AI Cờ Vua" : "Player 2";

        // Xử lý logic Random màu
        int finalP1Color;
        if (selectedColorChoice == 2) {
            // Random ra 0 hoặc 1
            finalP1Color = Math.random() < 0.5 ? GameLogic.WHITE : GameLogic.BLACK;
        } else {
            // Nếu chọn Trắng (0) hoặc Đen (1)
            finalP1Color = (selectedColorChoice == 0) ? GameLogic.WHITE : GameLogic.BLACK;
        }

        // Lấy thời gian
        TimeOption selectedTime = (TimeOption) timeComboBox.getSelectedItem();
        int timeLimit = selectedTime.value;

        // Đóng gói dữ liệu và gửi đi
        GameSettings settings = new GameSettings(isPvEMode, p1, p2, finalP1Color, timeLimit);
        Main.instance.startGame(settings);
    }

    // Class giúp ComboBox hiển thị chữ nhưng lưu giá trị số
    class TimeOption {
        String label;
        int value;

        TimeOption(String label, int value) {
            this.label = label;
            this.value = value;
        }

        // JComboBox sẽ gọi hàm này để hiển thị text
        @Override
        public String toString() {
            return label;
        }
    }

    // Class nút màu tùy chỉnh (Vẽ hình vuông màu)
    class ColorButton extends JButton {
        int type; // 0: White, 1: Black, 2: Random

        ColorButton(int type) {
            this.type = type;
            setPreferredSize(new Dimension(40, 40)); // Kích thước nút
            setContentAreaFilled(false); // Tắt nền mặc định của nút
            setFocusPainted(false); // Tắt viền focus nét đứt
            setBorder(new LineBorder(Color.gray, 1)); // Viền mặc định
            setCursor(new Cursor(Cursor.HAND_CURSOR)); // Con trỏ tay khi hover
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Vẽ nội dung nút
            if (type == 0) {
                // Nút Trắng
                g2.setColor(Color.white);
                g2.fillRect(2, 2, 36, 36);
            } else if (type == 1) {
                // Nút Đen
                g2.setColor(Color.black);
                g2.fillRect(2, 2, 36, 36);
                // Vẽ thêm viền trắng nhỏ bên trong để nút đen không bị chìm vào nền đen của
                // Panel
                g2.setColor(Color.white);
                g2.drawRect(2, 2, 35, 35);
            } else {
                // Nút Random (Nửa trắng nửa đen)
                // Nửa trái trên (Trắng)
                g2.setColor(Color.white);
                g2.fillPolygon(new int[] { 2, 38, 2 }, new int[] { 2, 38, 38 }, 3);
                // Nửa phải dưới (Đen)
                g2.setColor(Color.black);
                g2.fillPolygon(new int[] { 2, 38, 38 }, new int[] { 2, 2, 38 }, 3);
            }
        }
    }

    // Class Nút Bo Tròn (Giống hệt MenuPanel nhưng thu nhỏ kích thước mặc định)
    class RoundedButton extends JButton {
        boolean isHovered = false;

        RoundedButton(String text) {
            super(text);
            setFont(new Font("Arial", Font.PLAIN, 16));
            setForeground(new Color(40, 40, 40));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setPreferredSize(new Dimension(150, 40)); // Kích thước nhỏ hơn Menu
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isHovered ? new Color(230, 230, 230) : Color.white);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Bo góc nhỏ hơn xíu
            super.paintComponent(g);
        }
    }
}