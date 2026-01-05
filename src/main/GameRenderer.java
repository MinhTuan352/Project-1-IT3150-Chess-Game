package main;

import piece.Piece;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * GameRenderer - Chứa toàn bộ logic vẽ giao diện
 * Tách từ GamePanel để dễ quản lý
 */
public class GameRenderer {
    private DecimalFormat dFormat = new DecimalFormat("00");
    private Board board = new Board();

    public void paintGame(Graphics2D g2, GameLogic logic, Mouse mouse, int historyScrollIndex) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Vẽ bàn cờ (0-640)
        board.draw(g2);

        // Vẽ nghĩa địa (640-720)
        drawGraveyardZone(g2, mouse);

        // Vẽ vùng thông tin (720-880)
        drawSidebarInfo(g2, logic, historyScrollIndex);

        // Vẽ quân cờ
        for (Piece piece : GameLogic.simPieces) {
            piece.draw(g2);
        }

        // Vẽ các quân cờ đã bị ăn
        for (Piece piece : GameLogic.capturedPieces) {
            piece.draw(g2, Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE);
        }

        // Vẽ quân cờ khi kéo thả
        if (logic.activeP != null) {
            if (logic.canMove) {
                g2.setColor(logic.isIllegal(logic.activeP) ? Color.red : Color.gray);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2.fillRect(logic.activeP.col * Board.SQUARE_SIZE, logic.activeP.row * Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE + 1, Board.SQUARE_SIZE + 1);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            logic.activeP.draw(g2);
        }

        // Vẽ 4 ô chọn phong cấp
        if (logic.promotion && logic.activeP != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int x = logic.activeP.col * Board.SQUARE_SIZE;
            int y = logic.activeP.row * Board.SQUARE_SIZE;

            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillRect(x, y, Board.SQUARE_SIZE, Board.SQUARE_SIZE);

            g2.drawImage(logic.promoPieces.get(0).image, x, y, Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE, null);
            g2.drawImage(logic.promoPieces.get(1).image, x + Board.HALF_SQUARE_SIZE, y, Board.HALF_SQUARE_SIZE,
                    Board.HALF_SQUARE_SIZE, null);
            g2.drawImage(logic.promoPieces.get(2).image, x, y + Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE,
                    Board.HALF_SQUARE_SIZE, null);
            g2.drawImage(logic.promoPieces.get(3).image, x + Board.HALF_SQUARE_SIZE, y + Board.HALF_SQUARE_SIZE,
                    Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE, null);
        }
    }

    private void drawGraveyardZone(Graphics2D g2, Mouse mouse) {
        int gx = 640;
        int gw = 80;

        g2.setColor(new Color(45, 45, 45));
        g2.fillRect(gx, 0, gw, 640);

        g2.setColor(Color.black);
        g2.drawLine(gx, 0, gx, 640);

        // NÚT UNDO
        int btnY = 600;
        int btnH = 40;

        if (mouse.x >= gx && mouse.x <= gx + gw && mouse.y >= btnY && mouse.y <= btnY + btnH) {
            g2.setColor(new Color(70, 70, 70));
        } else {
            g2.setColor(new Color(55, 55, 55));
        }
        g2.fillRect(gx, btnY, gw, btnH);

        g2.setColor(Color.gray);
        g2.drawRect(gx, btnY, gw - 1, btnH - 1);

        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(2));
        g2.drawArc(gx + 30, btnY + 10, 20, 20, 0, 270);
    }

    private void drawSidebarInfo(Graphics2D g2, GameLogic logic, int historyScrollIndex) {
        int sx = 720;
        int sw = 160;
        boolean hasLimit = (logic.settings != null && logic.settings.timeLimit != -1);

        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(sx, 0, sw, 640);
        g2.setColor(Color.gray);
        g2.drawLine(sx, 0, sx, 640);

        int hInfo = hasLimit ? 160 : 80;

        // BLACK INFO (TOP)
        if (logic.settings != null) {
            String blackName = logic.settings.p1Color == GameLogic.BLACK ? logic.settings.p1Name
                    : logic.settings.p2Name;
            drawInfoBox(g2, sx, 0, sw, hInfo, blackName, logic.blackTime, false, hasLimit);

            // WHITE INFO (BOTTOM)
            String whiteName = logic.settings.p1Color == GameLogic.WHITE ? logic.settings.p1Name
                    : logic.settings.p2Name;
            drawInfoBox(g2, sx, 640 - hInfo, sw, hInfo, whiteName, logic.whiteTime, true, hasLimit);
        }

        // MOVE HISTORY
        int hy = hInfo;
        int hh = 640 - (hInfo * 2);

        g2.setColor(new Color(20, 20, 20));
        g2.fillRect(sx, hy, sw, hh);
        g2.setColor(Color.gray);
        g2.drawRect(sx, hy, sw - 1, hh - 1);

        int rowH = 40;
        int colW = 80;
        g2.setColor(new Color(50, 50, 50));
        g2.drawLine(sx + colW, hy, sx + colW, hy + hh);

        g2.setFont(new Font("Consolas", Font.PLAIN, 16));
        g2.setColor(Color.white);

        int totalVisualLines = (int) Math.ceil(logic.moveList.size() / 2.0);
        int maxLines = hh / rowH;

        for (int i = 0; i < maxLines; i++) {
            int lineIdx = historyScrollIndex + i;
            if (lineIdx >= totalVisualLines)
                break;

            int yText = hy + (i * rowH) + 25;

            if (lineIdx * 2 < logic.moveList.size()) {
                g2.drawString(logic.moveList.get(lineIdx * 2), sx + 10, yText);
            }

            if (lineIdx * 2 + 1 < logic.moveList.size()) {
                g2.drawString(logic.moveList.get(lineIdx * 2 + 1), sx + colW + 10, yText);
            }

            g2.setColor(new Color(50, 50, 50));
            g2.drawLine(sx, hy + (i + 1) * rowH, sx + sw, hy + (i + 1) * rowH);
            g2.setColor(Color.white);
        }
    }

    private void drawInfoBox(Graphics2D g2, int x, int y, int w, int h, String name, int time, boolean isWhite,
            boolean hasLimit) {
        g2.setColor(isWhite ? new Color(20, 20, 20) : new Color(230, 230, 230));
        g2.fillRect(x, y, w, h);
        g2.setColor(Color.gray);
        g2.drawRect(x, y, w - 1, h - 1);

        g2.setColor(isWhite ? Color.white : Color.black);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(name, x + (w - fm.stringWidth(name)) / 2, y + 30);

        if (hasLimit && h > 100) {
            g2.setFont(new Font("Impact", Font.PLAIN, 40));
            String tStr = dFormat.format(time / 60) + ":" + dFormat.format(time % 60);
            fm = g2.getFontMetrics();
            g2.drawString(tStr, x + (w - fm.stringWidth(tStr)) / 2, y + 100);
        }
    }
}
