package main;

import piece.Piece;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

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

    // Nút Options (thay thế Undo)
    private static final int OPTIONS_BTN_X = GRAVEYARD_X;
    private static final int OPTIONS_BTN_Y = 600;
    private static final int OPTIONS_BTN_WIDTH = GRAVEYARD_WIDTH;
    private static final int OPTIONS_BTN_HEIGHT = 40;

    // Popup menu (mỗi item 40x80)
    private static final int MENU_ITEM_HEIGHT = 40;
    private static final int MENU_ITEM_WIDTH = 80;

    // Lịch sử di chuyển
    private static final int HISTORY_ROW_HEIGHT = 32;
    private static final int HISTORY_HEADER_HEIGHT = 28;

    // Màu sắc
    private static final Color BG_GRAVEYARD = new Color(45, 45, 48);
    private static final Color BG_SIDEBAR = new Color(50, 50, 55);
    private static final Color BTN_NORMAL = new Color(55, 55, 55);
    private static final Color BTN_HOVER = new Color(70, 70, 70);
    private static final Color BTN_DISABLED = new Color(40, 40, 40);
    private static final Color TEXT_DISABLED = new Color(100, 100, 100);
    private static final Color MENU_BG = new Color(50, 50, 55);
    private static final Color HISTORY_BG = new Color(40, 40, 45);
    private static final Color HISTORY_HEADER_BG = new Color(55, 55, 60);
    private static final Color LINE_COLOR = new Color(70, 70, 70);
    private static final Color CHECK_HIGHLIGHT = new Color(200, 60, 60, 200);
    private static final Color LEGAL_MOVE = new Color(100, 100, 100, 180);
    private static final Color ILLEGAL_MOVE = new Color(200, 50, 50, 180);
    private static final Color PROMO_BG = new Color(255, 255, 255, 230);
    private static final Color TURN_BORDER = new Color(80, 180, 80);
    private static final Color LAST_MOVE_HIGHLIGHT = new Color(180, 180, 50, 120);

    // Font chữ
    private static final Font FONT_HISTORY = new Font("Consolas", Font.PLAIN, 14);
    private static final Font FONT_HEADER = new Font("Arial", Font.PLAIN, 11);
    private static final Font FONT_TIMER = new Font("Arial", Font.BOLD, 32);
    private static final Font FONT_BUTTON = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_MENU = new Font("Arial", Font.PLAIN, 13);

    // Fields
    private final DecimalFormat timeFormat = new DecimalFormat("00");
    private final Board board = new Board();
    public boolean showOptionsMenu = false; // Trạng thái hiện popup menu

    // Hàm vẽ game
    public void paintGame(Graphics2D g2, GameLogic logic, Mouse mouse, int historyScrollIndex) {
        setupRenderingHints(g2);

        // Vẽ bàn cờ
        board.draw(g2);

        // Highlight nước đi cuối
        drawLastMoveHighlight(g2, logic);

        // Highlight vua bị chiếu
        drawCheckHighlight(g2, logic);

        // Vẽ graveyard zone (captured pieces + Options button)
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

    private void drawLastMoveHighlight(Graphics2D g2, GameLogic logic) {
        if (logic.lastMoveFromCol >= 0 && logic.lastMoveToCol >= 0) {
            g2.setColor(LAST_MOVE_HIGHLIGHT);
            // Highlight ô xuất phát
            g2.fillRect(logic.lastMoveFromCol * Board.SQUARE_SIZE, logic.lastMoveFromRow * Board.SQUARE_SIZE,
                    Board.SQUARE_SIZE, Board.SQUARE_SIZE);
            // Highlight ô đích
            g2.fillRect(logic.lastMoveToCol * Board.SQUARE_SIZE, logic.lastMoveToRow * Board.SQUARE_SIZE,
                    Board.SQUARE_SIZE, Board.SQUARE_SIZE);
        }
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
        // Tạo bản sao để tránh ConcurrentModificationException
        ArrayList<Piece> piecesCopy = new ArrayList<>(GameLogic.simPieces);
        for (Piece piece : piecesCopy) {
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

        // Vẽ captured pieces với logic xếp thông minh
        // Tính toán vị trí khả dụng
        int optionsBtnY = OPTIONS_BTN_Y; // 600
        int availableHeight = optionsBtnY; // 600px cho captured pieces
        int maxSlotsPerSide = availableHeight / Board.HALF_SQUARE_SIZE; // 15 hàng mỗi bên

        // Đếm số quân đen (cần cho tính toán overflow)
        int blackCount = 0;
        for (Piece piece : GameLogic.capturedPieces) {
            if (piece.color == GameLogic.BLACK)
                blackCount++;
        }

        // Vẽ quân trắng bị ăn (từ trên xuống)
        int wIdx = 0;
        for (Piece piece : GameLogic.capturedPieces) {
            if (piece.color == GameLogic.WHITE) {
                int cx, cy;
                if (wIdx < maxSlotsPerSide * 2) {
                    // Còn chỗ phía trên
                    cx = GRAVEYARD_X + (wIdx % 2) * Board.HALF_SQUARE_SIZE;
                    cy = (wIdx / 2) * Board.HALF_SQUARE_SIZE;
                } else {
                    // Overflow: xếp vào vùng đen (từ dưới lên)
                    int overflow = wIdx - maxSlotsPerSide * 2;
                    cx = GRAVEYARD_X + (overflow % 2) * Board.HALF_SQUARE_SIZE;
                    cy = optionsBtnY - Board.HALF_SQUARE_SIZE
                            - (blackCount / 2 + overflow / 2 + 1) * Board.HALF_SQUARE_SIZE;
                }
                if (piece.image != null) {
                    g2.drawImage(piece.image, cx, cy, Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE, null);
                }
                wIdx++;
            }
        }

        // Vẽ quân đen bị ăn (từ dưới lên)
        int bIdx = 0;
        for (Piece piece : GameLogic.capturedPieces) {
            if (piece.color == GameLogic.BLACK) {
                int cx = GRAVEYARD_X + (bIdx % 2) * Board.HALF_SQUARE_SIZE;
                int cy = optionsBtnY - Board.HALF_SQUARE_SIZE - (bIdx / 2) * Board.HALF_SQUARE_SIZE;
                if (piece.image != null) {
                    g2.drawImage(piece.image, cx, cy, Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE, null);
                }
                bIdx++;
            }
        }

        // Nút Options
        drawOptionsButton(g2, mouse, logic);
    }

    private void drawOptionsButton(Graphics2D g2, Mouse mouse, GameLogic logic) {
        // Không vẽ nút khi game đã kết thúc
        if (logic.gameOver) {
            g2.setColor(BTN_DISABLED);
            g2.fillRect(OPTIONS_BTN_X, OPTIONS_BTN_Y, OPTIONS_BTN_WIDTH, OPTIONS_BTN_HEIGHT);
            g2.setColor(TEXT_DISABLED);
            g2.setFont(FONT_BUTTON);
            g2.drawString("Options", OPTIONS_BTN_X + 12, OPTIONS_BTN_Y + 25);
            return;
        }

        boolean isHovered = mouse.x >= OPTIONS_BTN_X && mouse.x <= OPTIONS_BTN_X + OPTIONS_BTN_WIDTH
                && mouse.y >= OPTIONS_BTN_Y && mouse.y <= OPTIONS_BTN_Y + OPTIONS_BTN_HEIGHT;

        // Nền nút
        g2.setColor(isHovered ? BTN_HOVER : BTN_NORMAL);
        g2.fillRect(OPTIONS_BTN_X, OPTIONS_BTN_Y, OPTIONS_BTN_WIDTH, OPTIONS_BTN_HEIGHT);

        // Viền
        g2.setColor(Color.GRAY);
        g2.drawRect(OPTIONS_BTN_X, OPTIONS_BTN_Y, OPTIONS_BTN_WIDTH - 1, OPTIONS_BTN_HEIGHT - 1);

        // Chữ "Options"
        g2.setFont(FONT_BUTTON);
        g2.drawString("Options", OPTIONS_BTN_X + 12, OPTIONS_BTN_Y + 25);
    }

    // Vẽ popup menu Options
    public void drawOptionsMenu(Graphics2D g2, GameLogic logic, Mouse mouse) {
        if (!showOptionsMenu)
            return;

        boolean hasTimeLimit = logic.settings != null && logic.settings.timeLimit != -1;

        // Danh sách menu items (động dựa vào có timer hay không)
        String[] items = hasTimeLimit
                ? new String[] { "Save", "Pause", "Undo", "Redo", "Draw", "Resign" }
                : new String[] { "Save", "Undo", "Redo", "Draw", "Resign" };

        boolean[] enabled = hasTimeLimit
                ? new boolean[] { true, true, logic.canUndo(), logic.canRedo(), true, true }
                : new boolean[] { true, logic.canUndo(), logic.canRedo(), true, true };

        int menuHeight = items.length * MENU_ITEM_HEIGHT;
        int menuX = OPTIONS_BTN_X;
        int menuY = OPTIONS_BTN_Y - menuHeight;

        // Nền menu
        g2.setColor(MENU_BG);
        g2.fillRect(menuX, menuY, MENU_ITEM_WIDTH, menuHeight);

        // Viền menu
        g2.setColor(LINE_COLOR);
        g2.drawRect(menuX, menuY, MENU_ITEM_WIDTH - 1, menuHeight - 1);

        // Vẽ từng item
        g2.setFont(FONT_MENU);
        for (int i = 0; i < items.length; i++) {
            int itemY = menuY + i * MENU_ITEM_HEIGHT;

            // Kiểm tra hover
            boolean itemHovered = mouse.x >= menuX && mouse.x <= menuX + MENU_ITEM_WIDTH
                    && mouse.y >= itemY && mouse.y <= itemY + MENU_ITEM_HEIGHT;

            // Nền item
            if (itemHovered && enabled[i]) {
                g2.setColor(BTN_HOVER);
                g2.fillRect(menuX, itemY, MENU_ITEM_WIDTH, MENU_ITEM_HEIGHT);
            }

            // Đường phân cách
            if (i > 0) {
                g2.setColor(LINE_COLOR);
                g2.drawLine(menuX, itemY, menuX + MENU_ITEM_WIDTH, itemY);
            }

            // Chữ
            g2.setColor(enabled[i] ? Color.WHITE : TEXT_DISABLED);
            g2.drawString(items[i], menuX + 15, itemY + 26);
        }
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
        g2.drawString("White Moves", SIDEBAR_X + 5, historyY + 18);
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
                g2.drawString(moveNum + logic.moveList.get(lineIdx * 2), SIDEBAR_X + 10, textY);
            }

            // Nước đen
            if (lineIdx * 2 + 1 < logic.moveList.size()) {
                g2.drawString(logic.moveList.get(lineIdx * 2 + 1), SIDEBAR_X + SIDEBAR_WIDTH / 2 + 20, textY);
            }

            // Đường phân cách dòng
            g2.setColor(LINE_COLOR);
            g2.drawLine(SIDEBAR_X, rowY + HISTORY_ROW_HEIGHT, SIDEBAR_X + SIDEBAR_WIDTH, rowY + HISTORY_ROW_HEIGHT);
            g2.setColor(Color.WHITE);
        }
    }

    // Hằng số công khai cho GamePanel
    public static int getOptionsBtnX() {
        return OPTIONS_BTN_X;
    }

    public static int getOptionsBtnY() {
        return OPTIONS_BTN_Y;
    }

    public static int getOptionsBtnWidth() {
        return OPTIONS_BTN_WIDTH;
    }

    public static int getOptionsBtnHeight() {
        return OPTIONS_BTN_HEIGHT;
    }

    // Menu item constants cho GamePanel
    public static int getMenuItemHeight() {
        return MENU_ITEM_HEIGHT;
    }

    public static int getMenuItemWidth() {
        return MENU_ITEM_WIDTH;
    }
}
