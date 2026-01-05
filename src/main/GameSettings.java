package main;

public class GameSettings {
    public boolean isPvE;   // Đấu với máy hoặc Đấu với người
    public String p1Name;
    public String p2Name;   // Nếu la PvE => AI Cờ Vua
    public int p1Color; // Màu quân của Player 1 (0 là trắng, 1 là đen)
    public int timeLimit;   // Thời gian 2 bên tính bằng giây

    public GameSettings(boolean isPvE, String p1Name, String p2Name, int p1Color, int timeLimit) {
        this.isPvE = isPvE;
        this.p1Name = p1Name;
        this.p2Name = p2Name;
        this.p1Color = p1Color;
        this.timeLimit = timeLimit;
    }
}
