package main;

import piece.Piece;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// GamePanel: View + Input + Game Loop
public class GamePanel extends JPanel implements Runnable {
    public static final int WIDTH = GameRenderer.TOTAL_WIDTH;
    public static final int HEIGHT = GameRenderer.TOTAL_HEIGHT;
    final int FPS = 60;

    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    // Modules
    public GameLogic logic;
    public GameRenderer renderer;

    // View state
    int historyScrollIndex = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);

        addMouseMotionListener(mouse);
        addMouseListener(mouse);
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                handleScroll(e);
            }
        });

        logic = new GameLogic();
        renderer = new GameRenderer();
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setupGame(GameSettings settings) {
        logic.setupGame(settings);
        historyScrollIndex = 0;
        renderer.showOptionsMenu = false;
    }

    // Tải game từ GameLogic đã load
    public void loadGameFromLogic(GameLogic loadedLogic) {
        this.logic = loadedLogic;
        historyScrollIndex = 0;
        renderer.showOptionsMenu = false;
        launchGame();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        // Xử lý nút Options và popup menu
        if (mouse.pressed) {
            int ox = GameRenderer.getOptionsBtnX();
            int oy = GameRenderer.getOptionsBtnY();
            int ow = GameRenderer.getOptionsBtnWidth();
            int oh = GameRenderer.getOptionsBtnHeight();

            // Click Options button
            if (!logic.gameOver && mouse.x >= ox && mouse.x <= ox + ow && mouse.y >= oy && mouse.y <= oy + oh) {
                renderer.showOptionsMenu = !renderer.showOptionsMenu;
                mouse.pressed = false;
                return;
            }

            // Click menu item khi popup đang mở
            if (renderer.showOptionsMenu) {
                boolean hasTimeLimit = logic.settings != null && logic.settings.timeLimit != -1;
                String[] items = hasTimeLimit
                        ? new String[] { "Save", "Pause", "Undo", "Redo", "Draw", "Resign" }
                        : new String[] { "Save", "Undo", "Redo", "Draw", "Resign" };

                int mh = GameRenderer.getMenuItemHeight();
                int mw = GameRenderer.getMenuItemWidth();
                int menuHeight = items.length * mh;
                int menuX = ox;
                int menuY = oy - menuHeight;

                // Kiểm tra click trong vùng menu
                if (mouse.x >= menuX && mouse.x <= menuX + mw && mouse.y >= menuY && mouse.y <= menuY + menuHeight) {
                    int index = (mouse.y - menuY) / mh;
                    if (index >= 0 && index < items.length) {
                        handleMenuClick(items[index]);
                        renderer.showOptionsMenu = false;
                        mouse.pressed = false;
                        return;
                    }
                }

                // Click ngoài menu → đóng menu
                renderer.showOptionsMenu = false;
                mouse.pressed = false;
                return;
            }
        }

        if (logic.gameOver)
            return;

        // Timer
        logic.updateTimer();
        if (logic.gameOver) {
            SwingUtilities.invokeLater(() -> showGameOverDialog("HẾT GIỜ!"));
            return;
        }

        // AI turn
        if (logic.settings != null && logic.settings.isPvE && logic.currentColor != logic.settings.p1Color
                && !logic.promotion) {
            logic.performAIMove();
            if (logic.gameOver) {
                SwingUtilities.invokeLater(() -> showGameOverDialog("CHIẾU HẾT!"));
            }
            return;
        }

        // Player turn
        if (logic.promotion) {
            selectingPromotion();
        } else {
            if (mouse.pressed) {
                if (logic.activeP == null) {
                    for (Piece piece : GameLogic.simPieces) {
                        if (piece.color == logic.currentColor &&
                                piece.col == mouse.x / Board.SQUARE_SIZE &&
                                piece.row == mouse.y / Board.SQUARE_SIZE) {
                            logic.activeP = piece;
                        }
                    }
                } else {
                    logic.simulate(mouse.x, mouse.y);
                }
            }

            if (!mouse.pressed) {
                if (logic.activeP != null) {
                    if (logic.validSquare) {
                        logic.saveState();

                        Piece capturedP = logic.activeP.hittingP;
                        if (capturedP != null) {
                            logic.changedToGraveyard(capturedP);
                        }

                        // Copy simPieces (có vị trí mới) sang pieces
                        logic.copyPieces(GameLogic.simPieces, GameLogic.pieces);

                        if (GameLogic.castlingP != null) {
                            GameLogic.castlingP.updatePosition();
                        }

                        if (logic.isKingInCheck(logic.currentColor)) {
                            logic.undo();
                        } else {
                            if (logic.activeP.type == Type.PAWN || capturedP != null) {
                                logic.halfmoveClock = 0;
                            } else {
                                logic.halfmoveClock++;
                            }

                            if (logic.canPromote()) {
                                logic.promotion = true;
                            } else {
                                // Lưu preCol/preRow trước khi updatePosition
                                int fromCol = logic.activeP.preCol;
                                int fromRow = logic.activeP.preRow;
                                int toCol = logic.activeP.col;
                                int toRow = logic.activeP.row;

                                // Update position trước để pieces phản ánh đúng vị trí
                                logic.activeP.updatePosition();

                                // Đảm bảo pieces đã có vị trí mới, sync lại simPieces
                                logic.copyPieces(GameLogic.pieces, GameLogic.simPieces);

                                // Log với vị trí gốc được truyền trực tiếp
                                logic.logMoveWithCoords(fromCol, fromRow, toCol, toRow, capturedP, null);
                                logic.changePlayer();
                            }
                        }

                        // Kiểm tra kết thúc game - delay dialog để repaint hiển thị log trước
                        if (logic.gameOver) {
                            final String gameOverReason;
                            if (logic.isCheckmate()) {
                                gameOverReason = "CHIẾU HẾT!";
                            } else if (logic.isStalemate()) {
                                gameOverReason = "HÒA - HẾT NƯỚC!";
                            } else if (logic.halfmoveClock >= 100) {
                                gameOverReason = "HÒA - LUẬT 50 NƯỚC!";
                            } else if (logic.isInsufficientMaterial()) {
                                gameOverReason = "HÒA - THIẾU QUÂN!";
                            } else {
                                gameOverReason = "HÒA - LẶP LẠI 3 LẦN!";
                            }

                            // Delay dialog để UI cập nhật log trước
                            SwingUtilities.invokeLater(() -> {
                                showGameOverDialog(gameOverReason);
                            });
                        }
                    } else {
                        logic.copyPieces(GameLogic.pieces, GameLogic.simPieces);
                        logic.activeP.resetPosition();
                        logic.activeP = null;
                    }
                }
            }
        }
    }

    private void selectingPromotion() {
        if (mouse.pressed) {
            int x = logic.activeP.col * Board.SQUARE_SIZE;
            int y = logic.activeP.row * Board.SQUARE_SIZE;

            Piece selectedPromo = null;

            if (mouse.x >= x && mouse.x < x + Board.HALF_SQUARE_SIZE && mouse.y >= y
                    && mouse.y < y + Board.HALF_SQUARE_SIZE) {
                selectedPromo = logic.promoPieces.get(0);
            } else if (mouse.x >= x + Board.HALF_SQUARE_SIZE && mouse.x < x + Board.SQUARE_SIZE && mouse.y >= y
                    && mouse.y < y + Board.HALF_SQUARE_SIZE) {
                selectedPromo = logic.promoPieces.get(1);
            } else if (mouse.x >= x && mouse.x < x + Board.HALF_SQUARE_SIZE && mouse.y >= y + Board.HALF_SQUARE_SIZE
                    && mouse.y < y + Board.SQUARE_SIZE) {
                selectedPromo = logic.promoPieces.get(2);
            } else if (mouse.x >= x + Board.HALF_SQUARE_SIZE && mouse.x < x + Board.SQUARE_SIZE
                    && mouse.y >= y + Board.HALF_SQUARE_SIZE && mouse.y < y + Board.SQUARE_SIZE) {
                selectedPromo = logic.promoPieces.get(3);
            }

            if (selectedPromo != null) {
                logic.replacePawn(selectedPromo);

                // Kiểm tra game over sau phong cấp (có thể checkmate)
                if (logic.gameOver) {
                    final String gameOverReason;
                    if (logic.isCheckmate()) {
                        gameOverReason = "CHIẾU HẾT!";
                    } else if (logic.isStalemate()) {
                        gameOverReason = "HÒA - HẾT NƯỚC!";
                    } else if (logic.isInsufficientMaterial()) {
                        gameOverReason = "HÒA - THIẾU QUÂN!";
                    } else {
                        gameOverReason = "HÒA!";
                    }
                    SwingUtilities.invokeLater(() -> showGameOverDialog(gameOverReason));
                }
            }
        }
    }

    private void handleScroll(MouseWheelEvent e) {
        int totalLines = (int) Math.ceil(logic.moveList.size() / 2.0);
        int maxLines = (logic.settings != null && logic.settings.timeLimit == -1) ? 12 : 8;

        if (totalLines > maxLines) {
            int newIndex = historyScrollIndex + e.getWheelRotation();
            int maxIndex = totalLines - maxLines;
            if (newIndex >= 0 && newIndex <= maxIndex) {
                historyScrollIndex = newIndex;
            }
        }
    }

    private void showGameOverDialog(String reason) {
        String winner = (logic.currentColor == GameLogic.WHITE) ? "BLACK WINS!" : "WHITE WINS!";

        if (reason.equals("HẾT GIỜ!")) {
            if (logic.whiteTime <= 0)
                winner = "BLACK WINS!";
            else
                winner = "WHITE WINS!";
        } else if (reason.contains("STALEMATE") || reason.contains("HÒA")) {
            winner = "DRAW!";
        }

        // Xuất log ván đấu tự động
        String pgnResult = winner.equals("DRAW!") ? "1/2-1/2" : (winner.equals("WHITE WINS!") ? "1-0" : "0-1");
        logic.exportGameLog(pgnResult);

        String message = reason + "\n" + winner + "\nBạn muốn làm gì?";
        Object[] options = { "Chơi lại", "Về Menu" };

        int choice = JOptionPane.showOptionDialog(Main.instance, message, "Kết thúc trận đấu",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            setupGame(logic.settings);
            launchGame();
        } else {
            gameThread = null;
            Main.instance.returnToMenu();
        }
    }

    // Xử lý click menu item
    private void handleMenuClick(String item) {
        switch (item) {
            case "Save":
                // Lưu game với timestamp
                String filename = "game_" + System.currentTimeMillis();
                logic.saveGame(filename);
                JOptionPane.showMessageDialog(this, "Đã lưu game!", "Save", JOptionPane.INFORMATION_MESSAGE);
                break;
            case "Pause":
                logic.togglePause();
                if (logic.isPaused) {
                    JOptionPane.showMessageDialog(this, "Game đang tạm dừng.\nBấm OK để tiếp tục.", "Pause",
                            JOptionPane.INFORMATION_MESSAGE);
                    logic.togglePause(); // Resume khi bấm OK
                }
                break;
            case "Undo":
                if (logic.canUndo()) {
                    logic.undo();
                }
                break;
            case "Redo":
                if (logic.canRedo()) {
                    logic.redo();
                }
                break;
            case "Draw":
                int drawChoice = JOptionPane.showConfirmDialog(this,
                        "Bạn có chắc muốn xin hòa?", "Draw", JOptionPane.YES_NO_OPTION);
                if (drawChoice == JOptionPane.YES_OPTION) {
                    if (logic.offerDraw()) {
                        showGameOverDialog("HÒA - THỎA THUẬN!");
                    }
                }
                break;
            case "Resign":
                int resignChoice = JOptionPane.showConfirmDialog(this,
                        "Bạn có chắc muốn đầu hàng?", "Resign", JOptionPane.YES_NO_OPTION);
                if (resignChoice == JOptionPane.YES_OPTION) {
                    logic.resign();
                    showGameOverDialog("ĐẦU HÀNG!");
                }
                break;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        renderer.paintGame(g2, logic, mouse, historyScrollIndex);

        // Vẽ popup menu (trên cùng)
        renderer.drawOptionsMenu(g2, logic, mouse);
    }
}