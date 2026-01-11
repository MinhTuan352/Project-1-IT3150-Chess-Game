package main;

// Lớp chứa các cài đặt cho một ván cờ
public class GameSettings {
    public boolean isPvE; // Chế độ chơi: true = PvE, false = PvP
    public String p1Name; // Tên người chơi 1
    public String p2Name; // Tên người chơi 2 hoặc AI
    public int p1Color; // Màu quân của Player 1 (0 = Trắng, 1 = Đen)
    public int timeLimit; // Thời gian mỗi bên (giây), -1 = không giới hạn

    public GameSettings(boolean isPvE, String p1Name, String p2Name, int p1Color, int timeLimit) {
        this.isPvE = isPvE;
        this.p1Name = p1Name;
        this.p2Name = p2Name;
        this.p1Color = p1Color;
        this.timeLimit = timeLimit;
    }
}
