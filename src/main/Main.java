package main;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.CardLayout;

public class Main extends JFrame {

    // Biến static để các màn hình con có thể truy cập lại Main dễ dàng
    public static Main instance;

    // CardLayout dùng để quản lý việc chuyển đổi giữa các màn hình
    public CardLayout cardLayout;
    public JPanel cardPanel; // Panel chứa tất cả các màn hình con

    // Khai báo 3 màn hình chính của game
    public MenuPanel menuPanel;
    public SetupPanel setupPanel;
    public GamePanel gamePanel;

    public Main() {
        instance = this; // Gán chính nó vào biến static

        setTitle("Project 1 - Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Tắt chương trình khi bấm X
        setResizable(false); // Không cho phép đổi kích thước cửa sổ

        // Khởi tạo CardLayout
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Khởi tạo các màn hình con
        menuPanel = new MenuPanel();
        setupPanel = new SetupPanel();
        gamePanel = new GamePanel();

        // Thêm các màn hình vào CardLayout với tên định danh
        cardPanel.add(menuPanel, "MENU");
        cardPanel.add(setupPanel, "SETUP");
        cardPanel.add(gamePanel, "GAME");

        // Thêm cardPanel vào cửa sổ chính
        add(cardPanel);

        pack(); // Tự động co giãn cửa sổ vừa khít với nội dung bên trong
        setLocationRelativeTo(null); // Hiển thị giữa màn hình
        setVisible(true);
    }

    // Hàm chuyển sang màn hình Cài đặt (Setup)
    public void showSetup(boolean isPvE) {
        setupPanel.resetForm(isPvE);
        cardLayout.show(cardPanel, "SETUP");
    }

    // Hàm bắt đầu game (Chuyển từ Setup sang Game). Nhận vào object GameSettings
    // chứa toàn bộ cấu hình người chơi đã chọn
    public void startGame(GameSettings settings) {
        // Truyền cài đặt vào bàn cờ để khởi tạo quân, tên, đồng hồ...
        gamePanel.setupGame(settings);
        gamePanel.launchGame(); // Bắt đầu Game Loop
        cardLayout.show(cardPanel, "GAME");
        gamePanel.requestFocus();
    }

    // Hàm quay về Menu chính (Từ Game hoặc Setup sang Menu)
    public void returnToMenu() {
        cardLayout.show(cardPanel, "MENU");
    }

    // Hàm tải ván đấu đã lưu
    public void loadSavedGame(String filepath) {
        // Tạo GameLogic mới và load file
        GameLogic tempLogic = new GameLogic();
        if (tempLogic.loadGame(filepath)) {
            // Load thành công -> chuyển sang game với settings đã load
            gamePanel.loadGameFromLogic(tempLogic);
            cardLayout.show(cardPanel, "GAME");
            gamePanel.requestFocus();
        } else {
            JOptionPane.showMessageDialog(this, "Không thể tải file save!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        new Main();
    }
}