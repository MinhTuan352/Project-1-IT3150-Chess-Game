package main;

import piece.Piece;
import piece.Bishop;
import piece.Knight;
import piece.Queen;
import piece.Rook;

import javax.swing.JPanel;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {
    public static final int WIDTH = 880; // Chiều rộng cửa sổ trò chơi
    public static final int HEIGHT = 640; // Chiều cao cửa sổ trò chơi
    final int FPS = 60; // Số khung hình trên giây
    Thread gameThread; // Luồng chính của trò chơi
    Board board = new Board(); // Bàn cờ
    Mouse mouse = new Mouse(); // Chuột

    // Game Settings
    GameSettings settings;
    int whiteTime;
    int blackTime;
    long lastTimerTime;

    // Piece
    public static ArrayList<Piece> pieces = new ArrayList<>(); // Danh sách các quân cờ hiện có trên bàn cờ
    public static ArrayList<Piece> simPieces = new ArrayList<>(); // Danh sách các quân cờ mô phỏng để thử nước đi
    public static ArrayList<Piece> capturedPieces = new ArrayList<>(); // Danh sách các quân cờ đã bị ăn
    ArrayList<Piece> promoPieces = new ArrayList<>(); // Danh sách 4 quân cờ có thể phong cấp
    Piece activeP; // Quân cờ đang được chọn
    Piece checkingP; // Quân cờ đang chiếu
    public static Piece castlingP; // Nhập thành

    // Color
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;

    // Boolean
    boolean canMove;
    boolean validSquare;
    boolean promotion; // Trạng thái chờ phong cấp
    boolean gameOver; // Chiếu hết => Game Over

    // Lịch sử các nước đi
    ArrayList<String> moveList = new ArrayList<>(); // Danh sách các nước đi (theo ký hiệu cờ vua)
    int historyScrollIndex = 0; // Dòng đầu tiên đang hiển thị
    final int LINE_HEIGHT = 40; // Chiều cao mỗi dòng chữ

    // Format hiển thị đồng hồ
    DecimalFormat dFormat = new DecimalFormat("00");

    ArrayList<HistoryMove> history = new ArrayList<>(); // Stack lưu các trạng thái của ván đấu để có thể undo

    // 50-move rule: Đếm số bán nước (halfmove) kể từ lần ăn quân hoặc di chuyển Tốt
    // cuối cùng
    int halfmoveClock = 0;

    // Threefold Repetition: Lưu hash của vị trí bàn cờ và số lần xuất hiện
    HashMap<String, Integer> positionHistory = new HashMap<>();

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black); // Màu nền đen

        addMouseMotionListener(mouse);
        addMouseListener(mouse);
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                handleScroll(e);
            }
        });

        setPieces();
        copyPieces(pieces, simPieces);
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    // Thiết lập ván đấu dựa trên cài đặt nhận được
    public void setupGame(GameSettings settings) {
        this.settings = settings;

        // Reset toàn bộ quân cờ và lịch sử các nước đi
        setPieces();
        copyPieces(pieces, simPieces);
        capturedPieces.clear();
        moveList.clear();
        history.clear();
        historyScrollIndex = 0;

        // Thiết lập thời gian
        // Nếu timeLimit = -1 (Không giới hạn) thì ta không quan tâm giá trị khởi tạo
        this.whiteTime = settings.timeLimit;
        this.blackTime = settings.timeLimit;
        this.lastTimerTime = System.nanoTime();

        // Reset trạng thái game
        currentColor = WHITE;
        activeP = null;
        promotion = false;
        gameOver = false;
        halfmoveClock = 0; // Reset đếm 50-move rule
        positionHistory.clear(); // Reset lịch sử vị trí
        updatePositionHistory(); // Thêm vị trí khởi đầu vào lịch sử
    }

    public void setPieces() {
        // Reset các quân cờ của ván trước
        pieces.clear();
        simPieces.clear();
        capturedPieces.clear();

        // Bên trắng
        pieces.add(new piece.Rook(0, 7, WHITE));
        pieces.add(new piece.Knight(1, 7, WHITE));
        pieces.add(new piece.Bishop(2, 7, WHITE));
        pieces.add(new piece.Queen(3, 7, WHITE));
        pieces.add(new piece.King(4, 7, WHITE));
        pieces.add(new piece.Bishop(5, 7, WHITE));
        pieces.add(new piece.Knight(6, 7, WHITE));
        pieces.add(new piece.Rook(7, 7, WHITE));
        for (int i = 0; i < 8; i++) {
            pieces.add(new piece.Pawn(i, 6, WHITE));
        }

        // Bên đen
        pieces.add(new piece.Rook(0, 0, BLACK));
        pieces.add(new piece.Knight(1, 0, BLACK));
        pieces.add(new piece.Bishop(2, 0, BLACK));
        pieces.add(new piece.Queen(3, 0, BLACK));
        pieces.add(new piece.King(4, 0, BLACK));
        pieces.add(new piece.Bishop(5, 0, BLACK));
        pieces.add(new piece.Knight(6, 0, BLACK));
        pieces.add(new piece.Rook(7, 0, BLACK));
        for (int i = 0; i < 8; i++) {
            pieces.add(new piece.Pawn(i, 1, BLACK));
        }
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        for (int i = 0; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    @Override
    public void run() {
        // Tạo vòng lặp trò chơi (Game loop)
        double drawInterval = 1000000000 / FPS; // Thời gian giữa các khung hình (nanoseconds)
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();

            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update(); // Cập nhật trạng thái trò chơi
                repaint(); // Vẽ lại màn hình
                delta--;
            }
        }
    }

    // Lưu trạng thái hiện tại của ván đấu vào stack history
    private void saveState() {
        HistoryMove state = new HistoryMove();

        // Sao chép danh sách quân cờ hiện tại
        state.pieceList = new ArrayList<>();
        for (Piece piece : pieces) {
            Piece copy = piece.getCopy();
            // Nếu là quân đang di chuyển thì sao chép vị trí trước đó
            if (piece == activeP) {
                copy.col = piece.preCol;
                copy.row = piece.preRow;
                copy.x = piece.getX(copy.col);
                copy.y = piece.getY(copy.row);
            }
            state.pieceList.add(copy);
        }

        // Sao chép danh sách quân bị ăn
        state.capturedList = new ArrayList<>();
        for (Piece piece : capturedPieces) {
            state.capturedList.add(piece.getCopy());
        }

        // Sao chép các thuộc tính khác
        state.wTime = whiteTime;
        state.bTime = blackTime;
        state.turn = currentColor;
        state.castlingP = (castlingP != null) ? castlingP.getCopy() : null;

        history.add(state);
    }

    // Undo nước đi
    private void undo() {
        if (history.isEmpty()) {
            return;
        }

        // Nếu là PvP => Lùi 1 nước. Nếu là PvE => Lùi 2 nước
        int steps = 1;
        if (settings.isPvE && history.size() >= 2) {
            steps = 2;
        } else if (settings.isPvE && history.size() == 1) {
            steps = 1; // Trường hợp đánh với AI mà chỉ có 1 nước trong lịch sử
        }

        for (int i = 0; i < steps; i++) {
            if (history.isEmpty()) {
                break;
            }

            // Lấy trạng thái trước đó
            HistoryMove prevState = history.remove(history.size() - 1);

            // Khôi phục dữ liệu
            pieces = prevState.pieceList;
            capturedPieces = prevState.capturedList;
            whiteTime = prevState.wTime;
            blackTime = prevState.bTime;
            currentColor = prevState.turn;
            castlingP = prevState.castlingP;

            for (Piece piece : pieces) {
                piece.x = piece.getX(piece.col);
                piece.y = piece.getY(piece.row);
            }

            // Đồng bộ sang simPieces
            copyPieces(pieces, simPieces);

            // Xóa dòng log tương ứng
            if (!moveList.isEmpty()) {
                moveList.remove(moveList.size() - 1);
            }
        }

        // Reset các biến tạm
        activeP = null;
        promotion = false;
        gameOver = false;
        validSquare = false;
    }

    // Xử lý cuộn lịch sử nước đi
    private void handleScroll(MouseWheelEvent e) {
        int totalLines = (int) Math.ceil(moveList.size() / 2.0);
        int maxLines = (settings.timeLimit == -1) ? 12 : 8; // Số dòng tối đa hiển thị dựa trên việc có đồng hồ hay
                                                            // không

        if (totalLines > maxLines) {
            // Lăn xuống (e.getWheelRotation() > 0) hoặc lăn lên
            int newIndex = historyScrollIndex + e.getWheelRotation();
            int maxIndex = totalLines - maxLines;
            if (newIndex >= 0 && newIndex <= maxIndex) {
                historyScrollIndex = newIndex;
            }
        }
    }

    // Ghi lại lịch sử nước đi (Format truyền thống: E2E4)
    private void logMove(Piece p, Piece captured, Piece promoteTo) {
        // Định dạng: [Tọa độ xuất phát][Tọa độ đích][Ký hiệu phong cấp nếu có]
        String from = board.getSquareCoordinates(p.preCol, p.preRow).toUpperCase();
        String to = board.getSquareCoordinates(p.col, p.row).toUpperCase();
        String notation = from + to;

        // Nếu phong cấp, thêm ký hiệu
        if (promoteTo != null) {
            if (promoteTo.type == Type.QUEEN)
                notation += "Q";
            else if (promoteTo.type == Type.ROOK)
                notation += "R";
            else if (promoteTo.type == Type.BISHOP)
                notation += "B";
            else if (promoteTo.type == Type.KNIGHT)
                notation += "N";
        }

        moveList.add(notation);

        // Tự động cuộn xuống cuối cùng
        int totalLines = (int) Math.ceil(moveList.size() / 2.0);
        int maxLines = (settings.timeLimit == -1) ? 12 : 8; // Số dòng tối đa hiển thị dựa trên việc có đồng hồ hay
                                                            // không
        if (totalLines > maxLines) {
            historyScrollIndex = totalLines - maxLines;
        }
    }

    // Cập nhật trạng thái trò chơi ở đây
    private void update() {
        // Xử lý nút Undo
        if (mouse.pressed) {
            if (mouse.x >= 640 && mouse.x <= 720 && mouse.y >= 600 && mouse.y <= 640) {
                undo();
                mouse.pressed = false; // Tránh lặp lại nhiều lần
                return;
            }
        }

        if (gameOver) {
            return; // Trường hợp game kết thúc
        }

        // Đếm ngược thời gian (Chỉ chạy khi có giới hạn thời gian)
        if (settings.timeLimit != -1) {
            if (System.nanoTime() - lastTimerTime >= 1000000000) {
                if (currentColor == WHITE) {
                    whiteTime--;
                } else {
                    // Nếu là PvP thì trừ giờ Đen. Nếu là PvE (AI) thì không trừ
                    if (!settings.isPvE) {
                        blackTime--;
                    }
                }
                lastTimerTime = System.nanoTime();

                if (whiteTime <= 0 || blackTime <= 0) {
                    gameOver = true;
                    showGameOverDialog("HẾT GIỜ!");
                }
            }
        }

        // Logic xử lý lượt đi của AI
        if (settings.isPvE && currentColor != settings.p1Color && !promotion) {
            performAIMove();
            return; // AI đi xong thì return để chờ frame sau
        }

        // Logic xử lý lượt đi của người chơi
        if (promotion) {
            selectingPromotion(); // Nếu đang phong cấp, tạm dừng ván, chỉ chạy hàm chọn quân
        } else {
            // Khi chuột được nhấn
            if (mouse.pressed) {
                if (activeP == null) {
                    // Trong trường hợp activeP là null, kiểm tra xem có quân cờ nào được chọn không
                    for (Piece piece : simPieces) {
                        // Chỉ được chọn quân cờ của màu hiện tại
                        if (piece.color == currentColor && piece.col == mouse.x / Board.SQUARE_SIZE
                                && piece.row == mouse.y / Board.SQUARE_SIZE) {
                            activeP = piece;
                        }
                    }
                } else {
                    // Player đang giữ quân cờ. Trong trường hợp này player có thể đưa ra nước đi mô
                    // phỏng
                    simulate();
                }
            }

            // Khi chuột được thả
            if (!mouse.pressed) {
                if (activeP != null) {
                    // Thả quân cờ xuống. Cập nhật vị trí cuối cùng của quân cờ
                    if (validSquare) {
                        saveState(); // Lưu trạng thái hiện tại trước khi thực hiện nước đi

                        // Lưu lại quân bị ăn (nếu có) để ghi lịch sử nước đi
                        Piece capturedP = activeP.hittingP;

                        // Kiểm tra ăn quân
                        if (activeP.hittingP != null) {
                            changedToGraveyard(activeP.hittingP);
                        }

                        // Xác định nước đi
                        copyPieces(simPieces, pieces);
                        activeP.updatePosition();

                        // Nếu nhập thành
                        if (castlingP != null) {
                            castlingP.updatePosition();
                        }

                        // Kiểm tra quân Vua có đang bị chiếu không? Kiểm tra trường hợp chiếu hết
                        if (isKingInCheck(currentColor)) {
                            undo(); // Hoàn tác nước đi
                        } else { // Nước đi hợp lệ
                            // Cập nhật halfmoveClock cho 50-move rule
                            if (activeP.type == Type.PAWN || capturedP != null) {
                                halfmoveClock = 0; // Reset nếu Tốt di chuyển hoặc có ăn quân
                            } else {
                                halfmoveClock++; // Tăng lên nếu không phải Tốt và không ăn quân
                            }

                            // Nếu có quân phong cấp
                            if (canPromote()) {
                                promotion = true;
                            } else {
                                logMove(activeP, capturedP, null); // Ghi lại lịch sử nước đi
                                changePlayer(); // Đổi lượt
                            }
                        }
                    } else {
                        // Thực hiện nước đi thất bại
                        copyPieces(pieces, simPieces);
                        activeP.resetPosition();
                        activeP = null; // Không còn quân cờ nào được chọn
                    }
                }
            }
        }
    }

    private void simulate() {

        canMove = false;
        validSquare = false;

        // Mỗi vòng lặp sẽ cập nhật lại số quân cờ hiện có (Nhằm quay lại các nước đi
        // đang thử nghiệm)
        copyPieces(pieces, simPieces);

        // Reset vị trí nhập thành của các quân cờ
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        // Nếu một quân cờ đang được chọn, cập nhật vị trí của nó theo vị trí chuột
        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        // Kiểm tra xem vị trí mới có hợp lệ không
        if (activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;

            // Khi ăn được quân đối phương, quân cờ đó phải bị loại bỏ khỏi bàn cờ
            if (activeP.hittingP != null) {
                simPieces.remove(activeP.hittingP.getIndex());
            }

            checkCastling();

            if (!isIllegal(activeP)) {
                validSquare = true;
            }
        }
    }

    // Class lưu trạng thái 1 nước cờ (Snapshot)
    class HistoryMove {
        ArrayList<Piece> pieceList; // Danh sách quân trên bàn
        ArrayList<Piece> capturedList; // Danh sách quân bị ăn
        int wTime, bTime; // Thời gian
        int turn; // Lượt đi
        Piece castlingP; // Trạng thái nhập thành
    }

    // Logic đánh cờ của AI (Chọn nước đi ngẫu nhiên trong các nước đi hợp lệ)
    private void performAIMove() {
        // Lấy danh sách quân của AI
        ArrayList<Piece> myPieces = new ArrayList<>();
        for (Piece piece : simPieces) {
            if (piece.color == currentColor) {
                myPieces.add(piece);
            }
        }

        // Tìm tất cả nước đi hợp lệ
        class Move {
            Piece piece;
            int col, row;

            Move(Piece piece, int col, int row) {
                this.piece = piece;
                this.col = col;
                this.row = row;
            }
        }
        ArrayList<Move> validMoves = new ArrayList<>();

        // Duyệt qua từng quân cờ
        for (Piece piece : myPieces) {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (piece.canMove(col, row)) {
                        int oldCol = piece.col;
                        int oldRow = piece.row;
                        Piece hit = piece.hittingP;

                        piece.col = col;
                        piece.row = row;

                        // Xóa quân bị ăn khỏi bàn cờ giả lập
                        if (hit != null)
                            simPieces.remove(hit);

                        // Kiểm tra nước đi có hợp lệ không (không bị chiếu)
                        if (!isIllegal(piece)) {
                            validMoves.add(new Move(piece, col, row));
                        }

                        // Hoàn tác giả lập
                        piece.col = oldCol;
                        piece.row = oldRow;
                        if (hit != null)
                            simPieces.add(hit);
                    }
                }
            }
        }

        // Chọn ngẫu nhiên
        if (validMoves.size() > 0) {
            Random rand = new Random();
            Move bestMove = validMoves.get(rand.nextInt(validMoves.size()));

            // Thực hiện nước đi
            activeP = bestMove.piece;
            saveState(); // Lưu trạng thái hiện tại trước khi thực hiện nước đi
            activeP.col = bestMove.col;
            activeP.row = bestMove.row;
            activeP.x = activeP.getX(activeP.col);
            activeP.y = activeP.getY(activeP.row);

            // Ăn quân
            Piece capturedP = activeP.getHittingP(activeP.col, activeP.row);
            if (capturedP != null) {
                changedToGraveyard(capturedP);
                pieces.remove(capturedP);
                simPieces.remove(capturedP);
            }

            // Cập nhật vị trí
            activeP.updatePosition();
            copyPieces(simPieces, pieces);

            if (canPromote()) {
                promoPieces.clear();
                promoPieces.add(new Queen(9, 9, currentColor));
                replacePawn(promoPieces.get(0)); // AI luôn chọn Hậu
                logMove(activeP, capturedP, promoPieces.get(0)); // Ghi lại lịch sử nước đi
            } else {
                logMove(activeP, capturedP, null); // Ghi lại lịch sử nước đi
                changePlayer();
            }
        } else {
            gameOver = true;
            showGameOverDialog("CHECKMATE!");
        }
    }

    private void changedToGraveyard(Piece capturedP) {
        // Chuyển quân cờ bị ăn vào danh sách capturedPieces
        int count = 0;
        for (Piece piece : capturedPieces) {
            if (piece.color == capturedP.color) {
                count++;
            }
        }

        if (capturedP.color == WHITE) {
            // Bên trắng, xếp ở góc trên bên phải
            capturedP.x = (8 * Board.SQUARE_SIZE) + (count % 2) * (Board.HALF_SQUARE_SIZE); // 8, 8.5, 8, 8.5,...
            capturedP.y = (0 * Board.SQUARE_SIZE) + (count / 2) * (Board.HALF_SQUARE_SIZE); // 0, 0, 0.5, 0.5,...
        } else {
            // Bên đen, xếp ở góc trên bên trái
            capturedP.x = (8 * Board.SQUARE_SIZE) + (count % 2) * (Board.HALF_SQUARE_SIZE); // 8, 8.5, 8, 8.5,...
            capturedP.y = (7 * Board.SQUARE_SIZE) - (count / 2) * (Board.HALF_SQUARE_SIZE); // 7, 7, 6.5, 6.5,...
        }
        capturedPieces.add(capturedP);
    }

    public boolean isIllegal(Piece activeP) {
        if (isKingInCheck(currentColor)) {
            return true;
        }
        return false;
    }

    public boolean isKingInCheck(int kingColor) {
        // Tìm vị trí Vua của phe cần check
        Piece King = getKing(kingColor);

        for (Piece piece : simPieces) {
            // Duyệt qua tất cả các quân cờ trên bàn. Chỉ quan tâm quân đối phương
            if (piece.color != kingColor) {
                if (piece.canMove(King.col, King.row)) {
                    checkingP = piece; // Xác định quân đang chiếu là quân nào (Không nhất thiết phải là activeP chiếu)
                    return true;
                }
            }
        }
        checkingP = null;
        return false;
    }

    private Piece getKing(int color) {
        for (Piece piece : simPieces) {
            if (piece.type == Type.KING && piece.color == color) {
                return piece;
            }
        }
        return null;
    }

    private void checkCastling() {
        if (castlingP != null) {
            // Nhập thành xa với quân Xe bên trái
            if (castlingP.col == 0) {
                castlingP.col += 3;
            }
            // Nhập thành gần với quân Xe bên phải
            else if (castlingP.col == 7) {
                castlingP.col -= 2;
            }

            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    private void changePlayer() {
        // Phương thức để đổi lượt sau khi thực hiện nước đi
        if (currentColor == WHITE) {
            currentColor = BLACK;
            // Reset En Passant
            for (Piece piece : pieces) {
                if (piece.color == BLACK) {
                    piece.twoStepped = false;
                }
            }
        } else {
            currentColor = WHITE;
            // Reset En Passant
            for (Piece piece : pieces) {
                if (piece.color == WHITE) {
                    piece.twoStepped = false;
                }
            }
        }

        // Cập nhật vị trí hiện tại vào lịch sử
        updatePositionHistory();

        // Kiểm tra các điều kiện hòa cờ
        if (halfmoveClock >= 100) { // 50 full moves = 100 halfmoves
            gameOver = true;
            showGameOverDialog("HÒA CỜ - LUẬT 50 NƯỚC!");
            return;
        }

        if (isThreefoldRepetition()) {
            gameOver = true;
            showGameOverDialog("HÒA CỜ - LẶP LẠI 3 LẦN!");
            return;
        }

        if (isCheckmate()) {
            gameOver = true;
            showGameOverDialog("CHECKMATE!");
        } else if (isStalemate()) {
            gameOver = true;
            showGameOverDialog("STALEMATE!");
        }

        activeP = null;
    }

    private boolean canPromote() {
        if (activeP.type == Type.PAWN) {
            if ((currentColor == WHITE && activeP.row == 0) || (currentColor == BLACK && activeP.row == 7)) {
                promoPieces.clear();
                promoPieces.add(new Bishop(9, 9, currentColor));
                promoPieces.add(new Knight(9, 9, currentColor));
                promoPieces.add(new Queen(9, 9, currentColor));
                promoPieces.add(new Rook(9, 9, currentColor));
                return true;
            }
        }
        return false;
    }

    private void selectingPromotion() {
        // Hàm xử lý quân phong cấp
        if (mouse.pressed) {
            // Lấy toạ độ ô vuông đang diễn ra phong cấp (Vị trí quân Tốt đang đứng)
            // activeP lúc này đã updatePosition() nên đang nằm ở đích
            int x = activeP.col * Board.SQUARE_SIZE;
            int y = activeP.row * Board.SQUARE_SIZE;

            // Kiểm tra xem con chuột click vào góc phần tư nào
            // Góc Trái-Trên (Tượng)
            if (mouse.x >= x && mouse.x < x + Board.HALF_SQUARE_SIZE && mouse.y >= y
                    && mouse.y < y + Board.HALF_SQUARE_SIZE) {
                replacePawn(promoPieces.get(0));
            }
            // Góc Phải-Trên (Mã)
            else if (mouse.x >= x + Board.HALF_SQUARE_SIZE && mouse.x < x + Board.SQUARE_SIZE && mouse.y >= y
                    && mouse.y < y + Board.HALF_SQUARE_SIZE) {
                replacePawn(promoPieces.get(1));
            }
            // Góc Trái-Dưới (Hậu)
            else if (mouse.x >= x && mouse.x < x + Board.HALF_SQUARE_SIZE && mouse.y >= y + Board.HALF_SQUARE_SIZE
                    && mouse.y < y + Board.SQUARE_SIZE) {
                replacePawn(promoPieces.get(2));
            }
            // Góc Phải-Dưới (Xe)
            else if (mouse.x >= x + Board.HALF_SQUARE_SIZE && mouse.x < x + Board.SQUARE_SIZE
                    && mouse.y >= y + Board.HALF_SQUARE_SIZE && mouse.y < y + Board.SQUARE_SIZE) {
                replacePawn(promoPieces.get(3));
            }
        }
    }

    private void replacePawn(Piece promoPiece) {
        // Gán toạ độ quân mới trùng với quân Tốt (promoPiece này là quân đã chọn, khác
        // với promoPieces là array list bao gồm các quân có thể chọn khi phong cấp)
        promoPiece.col = activeP.col;
        promoPiece.row = activeP.row;
        promoPiece.x = activeP.x;
        promoPiece.y = activeP.y;

        promoPiece.preCol = activeP.col;
        promoPiece.preRow = activeP.row;

        // Kiểm tra Tốt có ăn chéo để phong cấp không (phục vụ ghi lịch sử nước đi)
        Piece capturedRef = null;
        if (Math.abs(promoPiece.col - activeP.preCol) == 1) {
            capturedRef = new Piece(0, 0, 0);
        }

        // Xoá quân Tốt cũ khỏi list pieces và simPieces
        int index = pieces.indexOf(activeP); // Tìm index của Pawn cũ
        if (index != -1) {
            pieces.set(index, promoPiece); // Thay thế trong list chính
            simPieces.set(index, promoPiece); // Thay thế trong list mô phỏng
        }

        logMove(activeP, capturedRef, promoPiece); // Ghi lại lịch sử nước đi

        // Kết thúc phong cấp
        promotion = false;
        activeP = null; // Reset lựa chọn
        changePlayer(); // Đổi lượt
    }

    private boolean isCheckmate() {
        // Phương thức kiểm tra chiếu hết
        if (!isKingInCheck(currentColor)) {
            return false; // Không bị chiếu thì không thể chiếu hết
        }

        ArrayList<Piece> testPieces = new ArrayList<>();
        for (Piece piece : simPieces) {
            testPieces.add(piece);
        }

        for (Piece piece : testPieces) {
            if (piece.color != currentColor) {
                continue;
            }

            // Lưu vị trí hiện tại của quân cờ
            int originalCol = piece.col;
            int originalRow = piece.row;

            // Thử di chuyển quân cờ đến tất cả các vị trí trên bàn cờ
            for (int targetCol = 0; targetCol < 8; targetCol++) {
                for (int targetRow = 0; targetRow < 8; targetRow++) {
                    if (piece.canMove(targetCol, targetRow)) {
                        // Lưu trạng thái hiện tại
                        Piece capturedPiece = piece.hittingP;
                        int tempCol = piece.col;
                        int tempRow = piece.row;
                        // Thực hiện di chuyển tạm thời
                        piece.col = targetCol;
                        piece.row = targetRow;

                        ArrayList<Piece> tempList = new ArrayList<>();
                        for (Piece p : simPieces) {
                            if (p != capturedPiece) {
                                tempList.add(p);
                            }
                        }
                        ArrayList<Piece> backupSimPieces = simPieces;
                        simPieces = tempList;

                        // Kiểm tra xem Vua còn bị chiếu không
                        boolean stillInCheck = isKingInCheck(currentColor);
                        simPieces = backupSimPieces;
                        // Khôi phục trạng thái ban đầu
                        piece.col = tempCol;
                        piece.row = tempRow;

                        // Nếu tìm được nước thoát chiếu
                        if (!stillInCheck) {
                            return false;
                        }
                    }
                }
            }
            // Khôi phục vị trí ban đầu của quân cờ
            piece.col = originalCol;
            piece.row = originalRow;
        }

        return true; // Không tìm được nước thoát chiếu, chiếu hết
    }

    private boolean isStalemate() {
        // Stalemate: Không bị chiếu nhưng không có nước đi hợp lệ
        if (isKingInCheck(currentColor)) {
            return false; // Đang bị chiếu thì không phải stalemate
        }

        // Tạo bản sao để tránh ConcurrentModificationException
        ArrayList<Piece> testPieces = new ArrayList<>();
        for (Piece p : simPieces) {
            testPieces.add(p);
        }

        // Kiểm tra xem có nước đi hợp lệ nào không
        for (Piece piece : testPieces) {
            if (piece.color != currentColor) {
                continue;
            }

            int originalCol = piece.col;
            int originalRow = piece.row;

            for (int targetCol = 0; targetCol < 8; targetCol++) {
                for (int targetRow = 0; targetRow < 8; targetRow++) {
                    if (piece.canMove(targetCol, targetRow)) {
                        Piece capturedPiece = piece.hittingP;
                        int tempCol = piece.col;
                        int tempRow = piece.row;

                        piece.col = targetCol;
                        piece.row = targetRow;

                        // Tạo list tạm để kiểm tra
                        ArrayList<Piece> tempList = new ArrayList<>();
                        for (Piece p : simPieces) {
                            if (p != capturedPiece) {
                                tempList.add(p);
                            }
                        }

                        // Backup và thay thế tạm thời
                        ArrayList<Piece> backupSimPieces = simPieces;
                        simPieces = tempList;
                        boolean wouldBeInCheck = isKingInCheck(currentColor);
                        simPieces = backupSimPieces;

                        piece.col = tempCol;
                        piece.row = tempRow;

                        if (!wouldBeInCheck) {
                            return false; // Có nước đi hợp lệ
                        }
                    }
                }
            }

            piece.col = originalCol;
            piece.row = originalRow;
        }

        return true; // Không có nước đi hợp lệ → Stalemate
    }

    // Tạo hash của vị trí bàn cờ hiện tại (FEN-like)
    private String getPositionHash() {
        StringBuilder hash = new StringBuilder();
        // Duyệt qua tất cả quân cờ và ghi lại vị trí
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = null;
                for (Piece piece : simPieces) {
                    if (piece.col == col && piece.row == row) {
                        p = piece;
                        break;
                    }
                }
                if (p == null) {
                    hash.append('.');
                } else {
                    // Mã hóa: w=white, b=black; P=Pawn, R=Rook, N=Knight, B=Bishop, Q=Queen, K=King
                    char colorChar = (p.color == WHITE) ? 'w' : 'b';
                    char typeChar = ' ';
                    if (p.type == Type.PAWN)
                        typeChar = 'P';
                    else if (p.type == Type.ROOK)
                        typeChar = 'R';
                    else if (p.type == Type.KNIGHT)
                        typeChar = 'N';
                    else if (p.type == Type.BISHOP)
                        typeChar = 'B';
                    else if (p.type == Type.QUEEN)
                        typeChar = 'Q';
                    else if (p.type == Type.KING)
                        typeChar = 'K';
                    hash.append(colorChar).append(typeChar);
                }
            }
        }
        // Thêm thông tin lượt đi
        hash.append('|').append(currentColor);
        return hash.toString();
    }

    // Cập nhật lịch sử vị trí
    private void updatePositionHistory() {
        String posHash = getPositionHash();
        positionHistory.put(posHash, positionHistory.getOrDefault(posHash, 0) + 1);
    }

    // Kiểm tra lặp lại 3 lần
    private boolean isThreefoldRepetition() {
        String currentHash = getPositionHash();
        return positionHistory.getOrDefault(currentHash, 0) >= 3;
    }

    private void showGameOverDialog(String reason) {
        String winner = (currentColor == WHITE) ? "BLACK WINS!" : "WHITE WINS!";

        // Xử lý thông báo thắng thua cụ thể
        if (reason.equals("TIME OUT!")) {
            if (whiteTime <= 0)
                winner = "BLACK WINS!";
            else
                winner = "WHITE WINS!";
        } else if (reason.contains("STALEMATE")) {
            winner = "DRAW!";
        }

        String message = reason + "\n" + winner + "\nBạn muốn làm gì?";
        Object[] options = { "Chơi lại", "Về Menu" };

        int choice = JOptionPane.showOptionDialog(Main.instance, message, "Kết thúc trận đấu",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        // Rematch
        if (choice == 0) {
            setupGame(this.settings);
            launchGame();
        } else { // Menu
            gameThread = null;
            Main.instance.returnToMenu();
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Khử răng cưa cho hình vẽ và chữ
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Vẽ bàn cờ (0-640)
        board.draw(g2);

        // Vẽ nghĩa địa (640-720)
        drawGraveyardZone(g2);

        // Vẽ vùng thông tin (720-880)
        drawSidebarInfo(g2);

        // Vẽ quân cờ
        for (Piece piece : simPieces) {
            piece.draw(g2);
        }

        // Vẽ các quân cờ đã bị ăn
        for (Piece piece : capturedPieces) {
            piece.draw(g2, Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE);
        }

        // Vẽ quân cờ khi kéo thả
        if (activeP != null) {
            if (canMove) {
                g2.setColor(isIllegal(activeP) ? Color.red : Color.gray);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                // Cộng 1 pixel khi tô để tránh hở nền
                g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE + 1,
                        Board.SQUARE_SIZE + 1);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }

            // Vẽ lại quân cờ đang được chọn lên trên cùng
            activeP.draw(g2);
        }

        // Vẽ 4 ô chọn đè lên vị trí activeP đang đứng (Trường hợp phong cấp)
        if (promotion && activeP != null) {
            // Tối ưu render hình ảnh
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int x = activeP.col * Board.SQUARE_SIZE;
            int y = activeP.row * Board.SQUARE_SIZE;

            // Vẽ nền mờ che quân Tốt bên dưới
            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillRect(x, y, Board.SQUARE_SIZE, Board.SQUARE_SIZE);

            // Vẽ 4 quân cờ thu nhỏ vào 4 góc
            g2.drawImage(promoPieces.get(0).image, x, y, Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE, null);
            g2.drawImage(promoPieces.get(1).image, x + Board.HALF_SQUARE_SIZE, y, Board.HALF_SQUARE_SIZE,
                    Board.HALF_SQUARE_SIZE, null);
            g2.drawImage(promoPieces.get(2).image, x, y + Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE,
                    Board.HALF_SQUARE_SIZE, null);
            g2.drawImage(promoPieces.get(3).image, x + Board.HALF_SQUARE_SIZE, y + Board.HALF_SQUARE_SIZE,
                    Board.HALF_SQUARE_SIZE, Board.HALF_SQUARE_SIZE, null);
        }
    }

    private void drawGraveyardZone(Graphics2D g2) {
        int gx = 640;
        int gw = 80;

        // Nền chung
        g2.setColor(new Color(45, 45, 45)); // Xám đậm
        g2.fillRect(gx, 0, gw, 640);

        // Vạch ngăn cách với bàn cờ
        g2.setColor(Color.black);
        g2.drawLine(gx, 0, gx, 640);

        // NÚT UNDO (Cố định ở đáy: 600-640)
        int btnY = 600;
        int btnH = 40;

        // Hiệu ứng hover cho nút Undo
        if (mouse.x >= gx && mouse.x <= gx + gw && mouse.y >= btnY && mouse.y <= btnY + btnH) {
            g2.setColor(new Color(70, 70, 70)); // Sáng hơn khi hover
        } else {
            g2.setColor(new Color(55, 55, 55)); // Màu thường
        }
        g2.fillRect(gx, btnY, gw, btnH);

        // Viền nút
        g2.setColor(Color.gray);
        g2.drawRect(gx, btnY, gw - 1, btnH - 1); // -1 để viền nằm trong

        // Vẽ Icon Mũi tên quay lại (↺)
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(2)); // Nét dày
        g2.drawArc(gx + 30, btnY + 10, 20, 20, 0, 270); // Vòng cung hở

    }

    private void drawSidebarInfo(Graphics2D g2) {
        int sx = 720;
        int sw = 160;
        boolean hasLimit = (settings.timeLimit != -1);

        // Nền Sidebar
        g2.setColor(new Color(30, 30, 30)); // Đen xám
        g2.fillRect(sx, 0, sw, 640);
        g2.setColor(Color.gray);
        g2.drawLine(sx, 0, sx, 640); // Vạch ngăn cách

        // BLACK INFO (TOP)
        int hInfo = hasLimit ? 160 : 80;
        drawInfoBox(g2, sx, 0, sw, hInfo, settings.p1Color == BLACK ? settings.p1Name : settings.p2Name, blackTime,
                false);

        // WHITE INFO (BOTTOM)
        drawInfoBox(g2, sx, 640 - hInfo, sw, hInfo, settings.p1Color == WHITE ? settings.p1Name : settings.p2Name,
                whiteTime, true);

        // MOVE HISTORY (MIDDLE)
        int hy = hInfo; // Bắt đầu sau Info Đen
        int hh = 640 - (hInfo * 2); // Chiều cao còn lại

        // Tiêu đề bảng
        g2.setColor(new Color(20, 20, 20));
        g2.fillRect(sx, hy, sw, hh);
        g2.setColor(Color.gray);
        g2.drawRect(sx, hy, sw - 1, hh - 1); // Khung viền

        // Vẽ lưới (Grid Lines)
        int rowH = 40;
        int colW = 80;
        g2.setColor(new Color(50, 50, 50));
        // Kẻ dọc ở giữa
        g2.drawLine(sx + colW, hy, sx + colW, hy + hh);

        // Vẽ nội dung Text
        g2.setFont(new Font("Consolas", Font.PLAIN, 16));
        g2.setColor(Color.white);

        int totalVisualLines = (int) Math.ceil(moveList.size() / 2.0);
        int maxLines = hh / rowH;

        for (int i = 0; i < maxLines; i++) {
            int lineIdx = historyScrollIndex + i;
            if (lineIdx >= totalVisualLines)
                break;

            int yText = hy + (i * rowH) + 25; // Căn giữa dòng

            // White Move (Cột trái)
            if (lineIdx * 2 < moveList.size()) {
                String s = moveList.get(lineIdx * 2);
                g2.drawString(s, sx + 10, yText);
            }

            // Black Move (Cột phải)
            if (lineIdx * 2 + 1 < moveList.size()) {
                String s = moveList.get(lineIdx * 2 + 1);
                g2.drawString(s, sx + colW + 10, yText);
            }

            // Kẻ ngang dưới mỗi dòng
            g2.setColor(new Color(50, 50, 50));
            g2.drawLine(sx, hy + (i + 1) * rowH, sx + sw, hy + (i + 1) * rowH);
            g2.setColor(Color.white); // Reset màu chữ
        }
    }

    private void drawInfoBox(Graphics2D g2, int x, int y, int w, int h, String name, int time, boolean isWhite) {
        // Nền
        g2.setColor(isWhite ? new Color(20, 20, 20) : new Color(230, 230, 230));
        g2.fillRect(x, y, w, h);
        g2.setColor(Color.gray);
        g2.drawRect(x, y, w - 1, h - 1);

        // Tên
        g2.setColor(isWhite ? Color.white : Color.black);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(name, x + (w - fm.stringWidth(name)) / 2, y + 30);

        // Đồng hồ (Chỉ hiện nếu có time limit và đủ chỗ)
        if (settings.timeLimit != -1 && h > 100) {
            g2.setFont(new Font("Impact", Font.PLAIN, 40));
            String tStr = dFormat.format(time / 60) + ":" + dFormat.format(time % 60);
            fm = g2.getFontMetrics();
            g2.drawString(tStr, x + (w - fm.stringWidth(tStr)) / 2, y + 100);
        }
    }
}