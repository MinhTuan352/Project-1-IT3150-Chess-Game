package main;

import piece.Piece;
import java.awt.*;
import java.text.DecimalFormat;

// Game Renderer: Tập trung xử lý giao diện game
public class GameRenderer {
    // Layout
    public static final int BOARD_SIZE = 640;
    public static final int GRAVEYARD_WIDTH = 80;
    public static final int SIDEBAR_WIDTH = 160;
    public static final int TOTAL_WIDTH = BOARD_SIZE + GRAVEYARD_WIDTH + SIDEBAR_WIDTH; // 880
    public static final int TOTAL_HEIGHT = BOARD_SIZE; // 640 - không thêm space

    // Vị trí zone
    private static final int GRAVEYARD_X = BOARD_SIZE;
    private static final int SIDEBAR_X = BOARD_SIZE + GRAVEYARD_WIDTH;

    // Player info box
    private static final int PLAYER_INFO_HEIGHT = 100;

    // Nút Undo
    private static final int UNDO_BTN_X = GRAVEYARD_X;
    private static final int UNDO_BTN_Y = 600;
    private static final int UNDO_BTN_WIDTH = GRAVEYARD_WIDTH;
    private static final int UNDO_BTN_HEIGHT = 40;

    // Lịch sử di chuyển
    private static final int HISTORY_ROW_HEIGHT = 32;
    private static final int HISTORY_HEADER_HEIGHT = 28;

    // Màu sắc
    private static final Color BG_GRAVEYARD = new Color(45, 45, 48);
    private static final Color BG_SIDEBAR = new Color(50, 50, 55);
    private static final Color BTN_NORMAL = new Color(55, 55, 55);
    private static final Color BTN_HOVER = new Color(70, 70, 70);
    private static final Color HISTORY_BG = new Color(40, 40, 45);
    private static final Color HISTORY_HEADER_BG = new Color(55, 55, 60);
    private static final Color LINE_COLOR = new Color(70, 70, 70);
    private static final Color CHECK_HIGHLIGHT = new Color(200, 60, 60, 200);
    private static final Color LEGAL_MOVE = new Color(100, 100, 100, 180);
    private static final Color ILLEGAL_MOVE = new Color(200, 50, 50, 180);
    private static final Color PROMO_BG = new Color(255, 255, 255, 230);
    private static final Color TURN_BORDER = new Color(80, 180, 80);

    // Font chữ
    private static final Font FONT_HISTORY = new Font("Consolas", Font.PLAIN, 14);
    private static final Font FONT_HEADER = new Font("Arial", Font.PLAIN, 11);
    private static final Font FONT_TIMER = new Font("Arial", Font.BOLD, 32);
    private static final Font FONT_BUTTON = new Font("Arial", Font.BOLD, 12);

    // Fields
    private final DecimalFormat timeFormat = new DecimalFormat("00");
    private final Board board = new Board();

    // Hàm vẽ game
    public void paintGame(Graphics2D g2, GameLogic logic, Mouse mouse, int historyScrollIndex) {
        setupRenderingHints(g2);

        // Vẽ bàn cờ
        board.draw(g2);

        // Highlight vua bị chiếu
        drawCheckHighlight(g2, logic);

        // Vẽ graveyard zone (captured pieces + Undo button)
        drawGraveyardZone(g2, logic, mouse);

        // Vẽ sidebar (Player info + Move history)
        drawSidebar(g2, logic, historyScrollIndex);

        // Vẽ quân cờ trên bàn
        drawPieces(g2, logic);

        // Vẽ quân đang kéo thả
        drawActivePiece(g2, logic);

        // Vẽ UI phong cấp
        if (logic.promotion && logic.activeP != null) {
            drawPromotionUI(g2, logic);
        }
    }

    // Các hàm hỗ trợ vẽ
    private void setupRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void drawCheckHighlight(Graphics2D g2, GameLogic logic) {
        if (logic.checkingP != null) {
            Piece king = logic.getKing(logic.currentColor);
            if (king != null) {
                g2.setColor(CHECK_HIGHLIGHT);
                g2.fillRect(king.col * Board.SQUARE_SIZE, king.row * Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE, Board.SQUARE_SIZE);
            }
        }
    }

    private void drawPieces(Graphics2D g2, GameLogic logic) {
        for (Piece piece : GameLogic.simPieces) {
            if (piece != logic.activeP) {
                piece.draw(g2);
            }
        }
    }

    private void drawActivePiece(Graphics2D g2, GameLogic logic) {
        if (logic.activeP == null)
            return;

        if (logic.canMove) {
            g2.setColor(logic.isIllegal(logic.activeP) ? ILLEGAL_MOVE : LEGAL_MOVE);
            g2.fillRect(logic.activeP.col * Board.SQUARE_SIZE, logic.activeP.row * Board.SQUARE_SIZE,
                    Board.SQUARE_SIZE, Board.SQUARE_SIZE);
        }

        logic.activeP.draw(g2);
    }

    private void drawPromotionUI(Graphics2D g2, GameLogic logic) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int x = logic.activeP.col * Board.SQUARE_SIZE;
        int y = logic.activeP.row * Board.SQUARE_SIZE;
        int half = Board.HALF_SQUARE_SIZE;

        g2.setColor(PROMO_BG);
        g2.fillRect(x, y, Board.SQUARE_SIZE, Board.SQUARE_SIZE);

        g2.drawImage(logic.promoPieces.get(0).image, x, y, half, half, null);
        g2.drawImage(logic.promoPieces.get(1).image, x + half, y, half, half, null);
        g2.drawImage(logic.promoPieces.get(2).image, x, y + half, half, half, null);
        g2.drawImage(logic.promoPieces.get(3).image, x + half, y + half, half, half, null);
    }

    // Vùng Graveyard
    private void drawGraveyardZone(Graphics2D g2, GameLogic logic, Mouse mouse) {
        // Nền graveyard
        g2.setColor(BG_GRAVEYARD);
        g2.fillRect(GRAVEYARD_X, 0, GRAVEYARD_WIDTH, BOARD_SIZE);

        // Border trái
        g2.setColor(Color.BLACK);
        g2.drawLine(GRAVEYARD_X, 0, GRAVEYARD_X, BOARD_SIZE);

        // Vẽ captured pieces
        // Quân trắng bị ăn (hiện bên BLACK - phía trên)
        int whiteCount = 0;
        for (Piece piece : GameLogic.capturedPieces) {
            if (piece.color == GameLogic.WHITE) {
                int cx = GRAVEYARD_X + (whiteCount % 2) * Board.HALF_SQUARE_SIZE;
                int cy = (whiteCount / 2) * Board.HALF_SQUARE_SIZE;
                if (piece.image != null) {
                    g2.drawImage(piece.image, cx, cy, Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE, null);
                }
                whiteCount++;
            }
        }

        // Quân đen bị ăn (hiện bên WHITE - phía dưới, từ dưới lên)
        int blackCount = 0;
        for (Piece piece : GameLogic.capturedPieces) {
            if (piece.color == GameLogic.BLACK) {
                int cx = GRAVEYARD_X + (blackCount % 2) * Board.HALF_SQUARE_SIZE;
                int cy = BOARD_SIZE - UNDO_BTN_HEIGHT - Board.HALF_SQUARE_SIZE
                        - (blackCount / 2) * Board.HALF_SQUARE_SIZE;
                if (piece.image != null) {
                    g2.drawImage(piece.image, cx, cy, Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE, null);
                }
                blackCount++;
            }
        }

        // Nút Undo
        drawUndoButton(g2, mouse);
    }

    private void drawUndoButton(Graphics2D g2, Mouse mouse) {
        boolean isHovered = mouse.x >= UNDO_BTN_X && mouse.x <= UNDO_BTN_X + UNDO_BTN_WIDTH
                && mouse.y >= UNDO_BTN_Y && mouse.y <= UNDO_BTN_Y + UNDO_BTN_HEIGHT;

        // Nền nút
        g2.setColor(isHovered ? BTN_HOVER : BTN_NORMAL);
        g2.fillRect(UNDO_BTN_X, UNDO_BTN_Y, UNDO_BTN_WIDTH, UNDO_BTN_HEIGHT);

        // Viền
        g2.setColor(Color.GRAY);
        g2.drawRect(UNDO_BTN_X, UNDO_BTN_Y, UNDO_BTN_WIDTH - 1, UNDO_BTN_HEIGHT - 1);

        // Icon: vẽ mũi tên cong (undo arrow)
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.5f));
        int iconCenterX = UNDO_BTN_X + 25;
        int iconCenterY = UNDO_BTN_Y + 20;
        // Arc (phần cong)
        g2.drawArc(iconCenterX - 8, iconCenterY - 8, 16, 16, 240, 270);
        // Arrow head (tam giác nhỏ)
        int[] xPoints = { iconCenterX - 8, iconCenterX - 2, iconCenterX - 8 };
        int[] yPoints = { iconCenterY - 7, iconCenterY - 4, iconCenterY + 1 };
        g2.fillPolygon(xPoints, yPoints, 3);

        // Chữ "Undo"
        g2.setFont(FONT_BUTTON);
        g2.drawString("Undo", UNDO_BTN_X + 40, UNDO_BTN_Y + 25);
    }

    // Thanh bên (Sidebar)
    private void drawSidebar(Graphics2D g2, GameLogic logic, int historyScrollIndex) {
        // Nền sidebar
        g2.setColor(BG_SIDEBAR);
        g2.fillRect(SIDEBAR_X, 0, SIDEBAR_WIDTH, BOARD_SIZE);

        // Border trái
        g2.setColor(LINE_COLOR);
        g2.drawLine(SIDEBAR_X, 0, SIDEBAR_X, BOARD_SIZE);

        if (logic.settings == null)
            return;

        boolean hasTimeLimit = logic.settings.timeLimit != -1;

        // Thông tin Player 2 (Đen) - trên
        String p2Name = logic.settings.p1Color == GameLogic.BLACK ? logic.settings.p1Name : logic.settings.p2Name;
        boolean isP2Turn = logic.currentColor == GameLogic.BLACK;
        drawPlayerInfoBox(g2, SIDEBAR_X, 0, p2Name, logic.blackTime, false, hasTimeLimit, isP2Turn);

        // Thông tin Player 1 (Trắng) - dưới
        String p1Name = logic.settings.p1Color == GameLogic.WHITE ? logic.settings.p1Name : logic.settings.p2Name;
        boolean isP1Turn = logic.currentColor == GameLogic.WHITE;
        drawPlayerInfoBox(g2, SIDEBAR_X, BOARD_SIZE - PLAYER_INFO_HEIGHT, p1Name, logic.whiteTime, true, hasTimeLimit,
                isP1Turn);

        // Lịch sử nước đi - giữa
        int historyY = PLAYER_INFO_HEIGHT;
        int historyHeight = BOARD_SIZE - (PLAYER_INFO_HEIGHT * 2);
        drawMoveHistory(g2, logic, historyScrollIndex, historyY, historyHeight);
    }

    private void drawPlayerInfoBox(Graphics2D g2, int x, int y, String name, int time,
            boolean isWhitePlayer, boolean hasLimit, boolean isCurrentTurn) {
        // Nền
        g2.setColor(isWhitePlayer ? new Color(55, 55, 58) : new Color(45, 45, 48));
        g2.fillRect(x, y, SIDEBAR_WIDTH, PLAYER_INFO_HEIGHT);

        // Viền chỉ báo lượt đi
        if (isCurrentTurn) {
            g2.setColor(TURN_BORDER);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(x + 2, y + 2, SIDEBAR_WIDTH - 5, PLAYER_INFO_HEIGHT - 5);
            g2.setStroke(new BasicStroke(1));
        }

        // Tên người chơi với font động
        g2.setColor(Color.WHITE);
        int maxWidth = SIDEBAR_WIDTH - 10; // Padding 5px mỗi bên
        int fontSize = 16; // Base font size
        Font nameFont = new Font("Arial", Font.BOLD, fontSize);
        g2.setFont(nameFont);
        FontMetrics fm = g2.getFontMetrics();

        // Giảm font size nếu tên quá dài
        while (fm.stringWidth(name) > maxWidth && fontSize > 8) {
            fontSize--;
            nameFont = new Font("Arial", Font.BOLD, fontSize);
            g2.setFont(nameFont);
            fm = g2.getFontMetrics();
        }

        g2.drawString(name, x + (SIDEBAR_WIDTH - fm.stringWidth(name)) / 2, y + 30);

        // Đồng hồ
        if (hasLimit) {
            g2.setFont(FONT_TIMER);
            String timeStr = timeFormat.format(time / 60) + ":" + timeFormat.format(time % 60);
            fm = g2.getFontMetrics();
            g2.drawString(timeStr, x + (SIDEBAR_WIDTH - fm.stringWidth(timeStr)) / 2, y + 70);
        }
    }

    private void drawMoveHistory(Graphics2D g2, GameLogic logic, int scrollIndex, int historyY, int historyHeight) {
        // Nền
        g2.setColor(HISTORY_BG);
        g2.fillRect(SIDEBAR_X, historyY, SIDEBAR_WIDTH, historyHeight);

        // Tiêu đề
        g2.setColor(HISTORY_HEADER_BG);
        g2.fillRect(SIDEBAR_X, historyY, SIDEBAR_WIDTH, HISTORY_HEADER_HEIGHT);

        g2.setFont(FONT_HEADER);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("White Moves", SIDEBAR_X + 10, historyY + 18);
        g2.drawString("Black Moves", SIDEBAR_X + SIDEBAR_WIDTH / 2 + 5, historyY + 18);

        // Đường phân cách cột
        g2.setColor(LINE_COLOR);
        g2.drawLine(SIDEBAR_X + SIDEBAR_WIDTH / 2, historyY, SIDEBAR_X + SIDEBAR_WIDTH / 2, historyY + historyHeight);

        // Đường phân cách tiêu đề
        g2.drawLine(SIDEBAR_X, historyY + HISTORY_HEADER_HEIGHT, SIDEBAR_X + SIDEBAR_WIDTH,
                historyY + HISTORY_HEADER_HEIGHT);

        // Các nước đi
        g2.setFont(FONT_HISTORY);
        g2.setColor(Color.WHITE);

        int contentY = historyY + HISTORY_HEADER_HEIGHT;
        int contentHeight = historyHeight - HISTORY_HEADER_HEIGHT;
        int totalLines = (int) Math.ceil(logic.moveList.size() / 2.0);
        int maxVisibleLines = contentHeight / HISTORY_ROW_HEIGHT;

        for (int i = 0; i < maxVisibleLines; i++) {
            int lineIdx = scrollIndex + i;
            if (lineIdx >= totalLines)
                break;

            int rowY = contentY + i * HISTORY_ROW_HEIGHT;
            int textY = rowY + 22;

            // Số dòng + nước trắng
            if (lineIdx * 2 < logic.moveList.size()) {
                String moveNum = (lineIdx + 1) + ". ";
                g2.drawString(moveNum + logic.moveList.get(lineIdx * 2), SIDEBAR_X + 5, textY);
            }

            // Nước đen
            if (lineIdx * 2 + 1 < logic.moveList.size()) {
                g2.drawString(logic.moveList.get(lineIdx * 2 + 1), SIDEBAR_X + SIDEBAR_WIDTH / 2 + 8, textY);
            }

            // Đường phân cách dòng
            g2.setColor(LINE_COLOR);
            g2.drawLine(SIDEBAR_X, rowY + HISTORY_ROW_HEIGHT, SIDEBAR_X + SIDEBAR_WIDTH, rowY + HISTORY_ROW_HEIGHT);
            g2.setColor(Color.WHITE);
        }
    }

    // Hằng số công khai cho GamePanel
    public static int getUndoBtnX() {
        return UNDO_BTN_X;
    }

    public static int getUndoBtnY() {
        return UNDO_BTN_Y;
    }

    public static int getUndoBtnWidth() {
        return UNDO_BTN_WIDTH;
    }

    public static int getUndoBtnHeight() {
        return UNDO_BTN_HEIGHT;
    }
}
