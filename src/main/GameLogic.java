package main;

import piece.Piece;
import piece.Bishop;
import piece.Knight;
import piece.Queen;
import piece.Rook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

// Game Logic: Chứa toàn bộ logic game cờ vua
public class GameLogic {
    // Color constants
    public static final int WHITE = 0;
    public static final int BLACK = 1;

    // Piece lists
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    public static ArrayList<Piece> capturedPieces = new ArrayList<>();
    public ArrayList<Piece> promoPieces = new ArrayList<>();

    // Active pieces
    public Piece activeP;
    public Piece checkingP;
    public static Piece castlingP;

    // Game state
    public int currentColor = WHITE;
    public boolean canMove;
    public boolean validSquare;
    public boolean promotion;
    public boolean gameOver;

    // Timer
    public GameSettings settings;
    public int whiteTime;
    public int blackTime;
    public long lastTimerTime;

    // History
    public ArrayList<String> moveList = new ArrayList<>();
    public ArrayList<HistoryMove> history = new ArrayList<>();
    public int halfmoveClock = 0;
    public HashMap<String, Integer> positionHistory = new HashMap<>();

    // Last move highlight
    public int lastMoveFromCol = -1, lastMoveFromRow = -1;
    public int lastMoveToCol = -1, lastMoveToRow = -1;

    // Options menu support
    public boolean isPaused = false;
    public ArrayList<HistoryMove> redoHistory = new ArrayList<>();
    public ArrayList<String> redoMoveList = new ArrayList<>(); // Lưu các move bị undo
    public int winner = -1; // -1 = chưa có, 0 = WHITE, 1 = BLACK, 2 = HÒA

    // Board reference
    private Board board = new Board();

    public GameLogic() {
        setPieces();
        copyPieces(pieces, simPieces);
    }

    // Thiết lập ván đấu
    public void setupGame(GameSettings settings) {
        this.settings = settings;
        setPieces();
        copyPieces(pieces, simPieces);
        capturedPieces.clear();
        moveList.clear();
        history.clear();
        redoHistory.clear();
        redoMoveList.clear();

        whiteTime = settings.timeLimit;
        blackTime = settings.timeLimit;
        lastTimerTime = System.nanoTime();

        currentColor = WHITE;
        activeP = null;
        checkingP = null;
        castlingP = null;
        promotion = false;
        gameOver = false;
        isPaused = false;
        winner = -1;
        halfmoveClock = 0;

        // Reset nước đi cuối (Khi reset ván đấu)
        lastMoveFromCol = -1;
        lastMoveFromRow = -1;
        lastMoveToCol = -1;
        lastMoveToRow = -1;

        positionHistory.clear();
        updatePositionHistory();
    }

    public void setPieces() {
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

    public void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        for (int i = 0; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    // Lưu trạng thái để undo
    public void saveState() {
        HistoryMove state = new HistoryMove();
        state.pieceList = new ArrayList<>();
        for (Piece piece : pieces) {
            Piece copy = piece.getCopy();
            // Reset activeP (King) về vị trí trước khi di chuyển
            if (piece == activeP) {
                copy.col = piece.preCol;
                copy.row = piece.preRow;
                copy.x = piece.getX(copy.col);
                copy.y = piece.getY(copy.row);
                copy.moved = false; // Đánh dấu chưa di chuyển
            }
            // Reset castlingP (Rook) về vị trí trước khi nhập thành
            if (castlingP != null && piece == castlingP) {
                copy.col = piece.preCol;
                copy.row = piece.preRow;
                copy.x = piece.getX(copy.col);
                copy.y = piece.getY(copy.row);
                copy.moved = false; // Đánh dấu chưa di chuyển
            }
            state.pieceList.add(copy);
        }

        state.capturedList = new ArrayList<>();
        for (Piece piece : capturedPieces) {
            state.capturedList.add(piece.getCopy());
        }

        state.wTime = whiteTime;
        state.bTime = blackTime;
        state.turn = currentColor;
        state.castlingP = (castlingP != null) ? castlingP.getCopy() : null;
        history.add(state);
    }

    public void undo() {
        if (history.isEmpty())
            return;

        int steps = 1;
        if (settings.isPvE && history.size() >= 2) {
            steps = 2;
        } else if (settings.isPvE && history.size() == 1) {
            steps = 1;
        }

        for (int i = 0; i < steps; i++) {
            if (history.isEmpty())
                break;

            // Lưu trạng thái hiện tại vào redoHistory trước khi undo
            HistoryMove currentState = new HistoryMove();
            currentState.pieceList = new ArrayList<>();
            for (Piece p : pieces) {
                currentState.pieceList.add(p.getCopy());
            }
            currentState.capturedList = new ArrayList<>();
            for (Piece p : capturedPieces) {
                currentState.capturedList.add(p.getCopy());
            }
            currentState.wTime = whiteTime;
            currentState.bTime = blackTime;
            currentState.turn = currentColor;
            currentState.castlingP = (castlingP != null) ? castlingP.getCopy() : null;
            redoHistory.add(currentState);

            HistoryMove prevState = history.remove(history.size() - 1);
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
            copyPieces(pieces, simPieces);

            // Lưu move vào redoMoveList trước khi xóa (để redo có thể khôi phục)
            if (!promotion && !moveList.isEmpty()) {
                String removedMove = moveList.remove(moveList.size() - 1);
                redoMoveList.add(removedMove);
            }
        }

        activeP = null;
        promotion = false;
        gameOver = false;
        validSquare = false;

        // Reset last move highlight về trạng thái trước đó
        if (!moveList.isEmpty()) {
            // Nếu còn nước đi, parse từ moveList
            String lastMove = moveList.get(moveList.size() - 1);
            if (lastMove.length() >= 4) {
                lastMoveFromCol = lastMove.charAt(0) - 'A';
                lastMoveFromRow = 8 - (lastMove.charAt(1) - '0');
                lastMoveToCol = lastMove.charAt(2) - 'A';
                lastMoveToRow = 8 - (lastMove.charAt(3) - '0');
            }
        } else {
            lastMoveFromCol = -1;
            lastMoveFromRow = -1;
            lastMoveToCol = -1;
            lastMoveToRow = -1;
        }
    }

    // Simulate move (called when dragging)
    public void simulate(int mouseX, int mouseY) {
        canMove = false;
        validSquare = false;
        copyPieces(pieces, simPieces);

        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        activeP.x = mouseX - Board.HALF_SQUARE_SIZE;
        activeP.y = mouseY - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        if (activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;
            if (activeP.hittingP != null) {
                simPieces.remove(activeP.hittingP.getIndex());
            }
            checkCastling();
            if (!isIllegal(activeP)) {
                validSquare = true;
            }
        }
    }

    // Timer update
    public void updateTimer() {
        if (settings == null || settings.timeLimit == -1)
            return;

        if (System.nanoTime() - lastTimerTime >= 1000000000) {
            if (currentColor == WHITE) {
                whiteTime--;
            } else {
                if (!settings.isPvE) {
                    blackTime--;
                }
            }
            lastTimerTime = System.nanoTime();

            if (whiteTime <= 0 || blackTime <= 0) {
                gameOver = true;
            }
        }
    }

    // AI Move
    public void performAIMove() {
        ArrayList<Piece> myPieces = new ArrayList<>();
        for (Piece piece : simPieces) {
            if (piece.color == currentColor) {
                myPieces.add(piece);
            }
        }

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

        for (Piece piece : myPieces) {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (piece.canMove(col, row)) {
                        int oldCol = piece.col;
                        int oldRow = piece.row;
                        Piece hit = piece.hittingP;

                        piece.col = col;
                        piece.row = row;
                        if (hit != null)
                            simPieces.remove(hit);

                        if (!isIllegal(piece)) {
                            validMoves.add(new Move(piece, col, row));
                        }

                        piece.col = oldCol;
                        piece.row = oldRow;
                        if (hit != null)
                            simPieces.add(hit);
                    }
                }
            }
        }

        if (validMoves.size() > 0) {
            Random rand = new Random();
            Move bestMove = validMoves.get(rand.nextInt(validMoves.size()));

            activeP = bestMove.piece;
            saveState();
            activeP.col = bestMove.col;
            activeP.row = bestMove.row;
            activeP.x = activeP.getX(activeP.col);
            activeP.y = activeP.getY(activeP.row);

            Piece capturedP = activeP.getHittingP(activeP.col, activeP.row);
            if (capturedP != null) {
                changedToGraveyard(capturedP);
                pieces.remove(capturedP);
                simPieces.remove(capturedP);
            }

            // Log move sẽ được thực hiện trước khi updatePosition
            if (canPromote()) {
                promoPieces.clear();
                promoPieces.add(new Queen(9, 9, currentColor));
                logMove(activeP, capturedP, promoPieces.get(0));
                activeP.updatePosition();
                copyPieces(simPieces, pieces);
                replacePawn(promoPieces.get(0));
            } else {
                logMove(activeP, capturedP, null);
                activeP.updatePosition();
                copyPieces(simPieces, pieces);
                changePlayer();
            }
        } else {
            gameOver = true;
        }
    }

    public void changedToGraveyard(Piece capturedP) {
        // GameRenderer vẽ dựa trên index
        capturedPieces.add(capturedP);
    }

    public boolean isIllegal(Piece activeP) {
        return isKingInCheck(currentColor);
    }

    public boolean isKingInCheck(int kingColor) {
        Piece King = getKing(kingColor);
        if (King == null)
            return false;

        // Tạo bản sao để tránh ConcurrentModificationException
        ArrayList<Piece> piecesCopy = new ArrayList<>(simPieces);
        for (Piece piece : piecesCopy) {
            if (piece.color != kingColor) {
                if (piece.canMove(King.col, King.row)) {
                    checkingP = piece;
                    return true;
                }
            }
        }
        checkingP = null;
        return false;
    }

    public Piece getKing(int color) {
        // Tạo bản sao để tránh ConcurrentModificationException
        ArrayList<Piece> piecesCopy = new ArrayList<>(simPieces);
        for (Piece piece : piecesCopy) {
            if (piece.type == Type.KING && piece.color == color) {
                return piece;
            }
        }
        return null;
    }

    public void checkCastling() {
        if (castlingP != null) {
            if (castlingP.col == 0) {
                castlingP.col += 3;
            } else if (castlingP.col == 7) {
                castlingP.col -= 2;
            }
            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    public void changePlayer() {
        if (currentColor == WHITE) {
            currentColor = BLACK;
            for (Piece piece : pieces) {
                if (piece.color == BLACK)
                    piece.twoStepped = false;
            }
        } else {
            currentColor = WHITE;
            for (Piece piece : pieces) {
                if (piece.color == WHITE)
                    piece.twoStepped = false;
            }
        }

        updatePositionHistory();

        if (halfmoveClock >= 100) {
            gameOver = true;
            return;
        }

        if (isThreefoldRepetition()) {
            gameOver = true;
            return;
        }

        if (isInsufficientMaterial()) {
            gameOver = true;
            return;
        }

        if (isCheckmate()) {
            gameOver = true;
        } else if (isStalemate()) {
            gameOver = true;
        }

        activeP = null;
    }

    public boolean canPromote() {
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

    public void replacePawn(Piece promoPiece) {
        // Lưu vị trí gốc của Tốt trước khi thay đổi
        int fromCol = activeP.preCol;
        int fromRow = activeP.preRow;
        int toCol = activeP.col;
        int toRow = activeP.row;

        promoPiece.col = toCol;
        promoPiece.row = toRow;
        // Sử dụng getX/getY để căn giữa quân cờ
        promoPiece.x = promoPiece.getX(promoPiece.col);
        promoPiece.y = promoPiece.getY(promoPiece.row);
        promoPiece.preCol = toCol;
        promoPiece.preRow = toRow;

        Piece capturedRef = null;
        if (Math.abs(toCol - fromCol) == 1) {
            capturedRef = new Piece(0, 0, 0);
        }

        int index = pieces.indexOf(activeP);
        if (index != -1) {
            pieces.set(index, promoPiece);
            simPieces.set(index, promoPiece);
        }

        // Sync simPieces với promoPiece mới để check detection chính xác
        copyPieces(pieces, simPieces);

        // Log với vị trí gốc và quân mới
        logMoveWithCoords(fromCol, fromRow, toCol, toRow, capturedRef, promoPiece);

        promotion = false;
        activeP = null;
        changePlayer();
    }

    public boolean isCheckmate() {
        if (!isKingInCheck(currentColor))
            return false;

        ArrayList<Piece> testPieces = new ArrayList<>(simPieces);
        for (Piece piece : testPieces) {
            if (piece.color != currentColor)
                continue;

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

                        ArrayList<Piece> tempList = new ArrayList<>();
                        for (Piece p : simPieces) {
                            if (p != capturedPiece)
                                tempList.add(p);
                        }
                        ArrayList<Piece> backupSimPieces = simPieces;
                        simPieces = tempList;

                        boolean stillInCheck = isKingInCheck(currentColor);
                        simPieces = backupSimPieces;
                        piece.col = tempCol;
                        piece.row = tempRow;

                        if (!stillInCheck)
                            return false;
                    }
                }
            }
            piece.col = originalCol;
            piece.row = originalRow;
        }
        return true;
    }

    public boolean isStalemate() {
        if (isKingInCheck(currentColor))
            return false;

        ArrayList<Piece> testPieces = new ArrayList<>(simPieces);
        for (Piece piece : testPieces) {
            if (piece.color != currentColor)
                continue;

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

                        ArrayList<Piece> tempList = new ArrayList<>();
                        for (Piece p : simPieces) {
                            if (p != capturedPiece)
                                tempList.add(p);
                        }

                        ArrayList<Piece> backupSimPieces = simPieces;
                        simPieces = tempList;
                        boolean wouldBeInCheck = isKingInCheck(currentColor);
                        simPieces = backupSimPieces;

                        piece.col = tempCol;
                        piece.row = tempRow;

                        if (!wouldBeInCheck)
                            return false;
                    }
                }
            }
            piece.col = originalCol;
            piece.row = originalRow;
        }
        return true;
    }

    /**
     * Kiểm tra không đủ quân để chiếu hết (Insufficient Material)
     * Các trường hợp hòa:
     * - King vs King
     * - King + Bishop vs King
     * - King + Knight vs King
     * - King + Bishop(s) vs King + Bishop(s) (tất cả Bishop cùng màu ô)
     */
    public boolean isInsufficientMaterial() {
        int whitePieces = 0, blackPieces = 0;
        int whiteBishops = 0, blackBishops = 0;
        int whiteKnights = 0, blackKnights = 0;
        boolean hasLightSquareBishop = false, hasDarkSquareBishop = false;

        for (Piece piece : simPieces) {
            if (piece.type == Type.KING)
                continue;

            if (piece.color == WHITE) {
                whitePieces++;
                if (piece.type == Type.BISHOP) {
                    whiteBishops++;
                    if ((piece.col + piece.row) % 2 == 0)
                        hasLightSquareBishop = true;
                    else
                        hasDarkSquareBishop = true;
                } else if (piece.type == Type.KNIGHT) {
                    whiteKnights++;
                }
            } else {
                blackPieces++;
                if (piece.type == Type.BISHOP) {
                    blackBishops++;
                    if ((piece.col + piece.row) % 2 == 0)
                        hasLightSquareBishop = true;
                    else
                        hasDarkSquareBishop = true;
                } else if (piece.type == Type.KNIGHT) {
                    blackKnights++;
                }
            }
        }

        // King vs King
        if (whitePieces == 0 && blackPieces == 0) {
            return true;
        }

        // King + Bishop vs King hoặc King vs King + Bishop
        if ((whitePieces == 1 && whiteBishops == 1 && blackPieces == 0) ||
                (blackPieces == 1 && blackBishops == 1 && whitePieces == 0)) {
            return true;
        }

        // King + Knight vs King hoặc King vs King + Knight
        if ((whitePieces == 1 && whiteKnights == 1 && blackPieces == 0) ||
                (blackPieces == 1 && blackKnights == 1 && whitePieces == 0)) {
            return true;
        }

        // Chỉ có Bishops và tất cả đều cùng màu ô
        int totalBishops = whiteBishops + blackBishops;
        int totalPieces = whitePieces + blackPieces;
        if (totalPieces == totalBishops && totalBishops > 0) {
            // Nếu tất cả Bishop đều trên một loại ô (sáng hoặc tối)
            if (!hasLightSquareBishop || !hasDarkSquareBishop) {
                return true;
            }
        }

        return false;
    }

    public void logMove(Piece p, Piece captured, Piece promoteTo) {
        String from = board.getSquareCoordinates(p.preCol, p.preRow).toUpperCase();
        String to = board.getSquareCoordinates(p.col, p.row).toUpperCase();
        String notation = from + to;

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

        // Sync simPieces từ pieces để kiểm tra chiếu chính xác
        copyPieces(pieces, simPieces);

        // Kiểm tra chiếu hết hoặc chiếu để thêm ký hiệu
        // opponentColor là màu đối phương (người vừa bị di chuyển vào)
        int opponentColor = (currentColor == WHITE) ? BLACK : WHITE;
        if (isKingInCheck(opponentColor)) {
            // Kiểm tra xem có phải checkmate không
            // Tạm đổi currentColor để isCheckmate() kiểm tra đúng bên
            int savedColor = currentColor;
            currentColor = opponentColor;
            if (isCheckmate()) {
                notation += "#"; // Chiếu hết
            } else {
                notation += "+"; // Chiếu
            }
            currentColor = savedColor; // Khôi phục
        }

        moveList.add(notation);
    }

    // Overload để GamePanel truyền tọa độ trực tiếp (sau khi updatePosition)
    public void logMoveWithCoords(int fromCol, int fromRow, int toCol, int toRow, Piece captured, Piece promoteTo) {
        String from = board.getSquareCoordinates(fromCol, fromRow).toUpperCase();
        String to = board.getSquareCoordinates(toCol, toRow).toUpperCase();
        String notation = from + to;

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

        // simPieces đã được sync, kiểm tra chiếu
        int opponentColor = (currentColor == WHITE) ? BLACK : WHITE;
        if (isKingInCheck(opponentColor)) {
            int savedColor = currentColor;
            currentColor = opponentColor;
            if (isCheckmate()) {
                notation += "#";
            } else {
                notation += "+";
            }
            currentColor = savedColor;
        }

        // Lưu vị trí nước đi cuối để highlight
        lastMoveFromCol = fromCol;
        lastMoveFromRow = fromRow;
        lastMoveToCol = toCol;
        lastMoveToRow = toRow;

        moveList.add(notation);
    }

    private String getPositionHash() {
        StringBuilder hash = new StringBuilder();
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
        hash.append('|').append(currentColor);
        return hash.toString();
    }

    private void updatePositionHistory() {
        String posHash = getPositionHash();
        positionHistory.put(posHash, positionHistory.getOrDefault(posHash, 0) + 1);
    }

    private boolean isThreefoldRepetition() {
        String currentHash = getPositionHash();
        return positionHistory.getOrDefault(currentHash, 0) >= 3;
    }

    // =============== OPTIONS MENU METHODS ===============

    // Kiểm tra có thể Undo không (không cho phép khi đang phong cấp)
    public boolean canUndo() {
        return !history.isEmpty() && !gameOver && !promotion;
    }

    // Kiểm tra có thể Redo không (không cho phép khi đang phong cấp)
    public boolean canRedo() {
        return !redoHistory.isEmpty() && !gameOver && !promotion;
    }

    // Bật/tắt tạm dừng
    public void togglePause() {
        isPaused = !isPaused;
        if (!isPaused) {
            lastTimerTime = System.currentTimeMillis();
        }
    }

    // Redo - làm lại nước đã undo
    public void redo() {
        if (redoHistory.isEmpty() || gameOver)
            return;

        // Lưu trạng thái hiện tại vào history trước
        saveState();

        // Lấy trạng thái từ redoHistory
        HistoryMove redoState = redoHistory.remove(redoHistory.size() - 1);
        pieces = redoState.pieceList;
        capturedPieces = redoState.capturedList;
        whiteTime = redoState.wTime;
        blackTime = redoState.bTime;
        currentColor = redoState.turn;
        castlingP = redoState.castlingP;

        for (Piece piece : pieces) {
            piece.x = piece.getX(piece.col);
            piece.y = piece.getY(piece.row);
        }
        copyPieces(pieces, simPieces);

        // Khôi phục move từ redoMoveList vào moveList
        if (!redoMoveList.isEmpty()) {
            String restoredMove = redoMoveList.remove(redoMoveList.size() - 1);
            moveList.add(restoredMove);

            // Cập nhật lastMove highlight
            if (restoredMove.length() >= 4) {
                lastMoveFromCol = restoredMove.charAt(0) - 'A';
                lastMoveFromRow = 8 - (restoredMove.charAt(1) - '0');
                lastMoveToCol = restoredMove.charAt(2) - 'A';
                lastMoveToRow = 8 - (restoredMove.charAt(3) - '0');
            }
        }

        activeP = null;
    }

    // Đầu hàng
    public void resign() {
        if (gameOver)
            return;
        gameOver = true;
        winner = (currentColor == WHITE) ? BLACK : WHITE;
    }

    // Xin hòa (PvP: cần đối phương đồng ý, PvE: AI tự đồng ý)
    public boolean offerDraw() {
        if (gameOver)
            return false;

        // AI luôn đồng ý hòa
        if (settings != null && settings.isPvE) {
            gameOver = true;
            winner = 2; // Hòa
            return true;
        }
        // PvP: cần dialog xác nhận từ đối phương
        return true;
    }

    // Lưu game vào file
    public void saveGame(String filename) {
        try {
            File saveDir = new File("res/saves");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            PrintWriter writer = new PrintWriter("res/saves/" + filename + ".txt");

            // Lưu thông tin cơ bản
            writer.println("# Chess Save File");
            writer.println("currentColor=" + currentColor);
            writer.println("whiteTime=" + whiteTime);
            writer.println("blackTime=" + blackTime);
            writer.println("halfmoveClock=" + halfmoveClock);
            writer.println("timeLimit=" + (settings != null ? settings.timeLimit : -1));
            writer.println("isPvE=" + (settings != null ? settings.isPvE : false));

            // Lưu danh sách nước đi
            writer.println("moves=" + String.join(",", moveList));

            // Lưu vị trí quân cờ
            writer.println("# Pieces: type,color,col,row,moved");
            for (Piece p : pieces) {
                writer.println("piece=" + p.type + "," + p.color + "," + p.col + "," + p.row + "," + p.moved);
            }

            // Lưu quân bị ăn
            writer.println("# Captured pieces");
            for (Piece p : capturedPieces) {
                writer.println("captured=" + p.type + "," + p.color);
            }

            writer.close();
            System.out.println("Đã lưu game: res/saves/" + filename + ".txt");
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu game: " + e.getMessage());
        }
    }

    // Xuất log ván đấu khi kết thúc
    public void exportGameLog(String result) {
        try {
            File logDir = new File("res/log");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // Tạo tên file với timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            String filename = "game_" + timestamp + ".pgn";

            PrintWriter writer = new PrintWriter("res/log/" + filename);

            // Header PGN
            writer.println("[Event \"Chess Game\"]");
            writer.println("[Date \"" + new SimpleDateFormat("yyyy.MM.dd").format(new Date()) + "\"]");
            writer.println("[White \"" + (settings != null ? settings.p1Name : "Player 1") + "\"]");
            writer.println("[Black \"" + (settings != null ? settings.p2Name : "Player 2") + "\"]");
            writer.println("[Result \"" + result + "\"]");
            writer.println();

            // Danh sách nước đi (mỗi dòng 1 cặp nước)
            for (int i = 0; i < moveList.size(); i += 2) {
                StringBuilder line = new StringBuilder();
                line.append((i / 2 + 1)).append(". ").append(moveList.get(i));
                if (i + 1 < moveList.size()) {
                    line.append(" ").append(moveList.get(i + 1));
                }
                writer.println(line.toString());
            }
            writer.println(result);

            writer.close();
            System.out.println("Đã xuất log: res/log/" + filename);
        } catch (Exception e) {
            System.err.println("Lỗi khi xuất log: " + e.getMessage());
        }
    }

    // Tải game từ file
    public boolean loadGame(String filepath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String line;

            pieces.clear();
            simPieces.clear();
            capturedPieces.clear();
            moveList.clear();
            history.clear();
            redoHistory.clear();

            int timeLimit = -1;
            boolean isPvE = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty())
                    continue;

                String[] parts = line.split("=", 2);
                if (parts.length != 2)
                    continue;

                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "currentColor":
                        currentColor = Integer.parseInt(value);
                        break;
                    case "whiteTime":
                        whiteTime = Integer.parseInt(value);
                        break;
                    case "blackTime":
                        blackTime = Integer.parseInt(value);
                        break;
                    case "halfmoveClock":
                        halfmoveClock = Integer.parseInt(value);
                        break;
                    case "timeLimit":
                        timeLimit = Integer.parseInt(value);
                        break;
                    case "isPvE":
                        isPvE = Boolean.parseBoolean(value);
                        break;
                    case "moves":
                        if (!value.isEmpty()) {
                            String[] moves = value.split(",");
                            for (String m : moves) {
                                if (!m.isEmpty())
                                    moveList.add(m);
                            }
                        }
                        break;
                    case "piece":
                        String[] pParts = value.split(",");
                        if (pParts.length >= 5) {
                            Type type = Type.valueOf(pParts[0]);
                            int color = Integer.parseInt(pParts[1]);
                            int col = Integer.parseInt(pParts[2]);
                            int row = Integer.parseInt(pParts[3]);
                            boolean moved = Boolean.parseBoolean(pParts[4]);
                            Piece p = createPiece(type, col, row, color);
                            if (p != null) {
                                p.moved = moved;
                                pieces.add(p);
                            }
                        }
                        break;
                    case "captured":
                        String[] cParts = value.split(",");
                        if (cParts.length >= 2) {
                            Type type = Type.valueOf(cParts[0]);
                            int color = Integer.parseInt(cParts[1]);
                            Piece c = createPiece(type, 0, 0, color);
                            if (c != null)
                                capturedPieces.add(c);
                        }
                        break;
                }
            }

            reader.close();

            // Tạo settings với constructor đúng
            String p2Name = isPvE ? "Computer" : "Player 2";
            settings = new GameSettings(isPvE, "Player 1", p2Name, WHITE, timeLimit);

            // Sync simPieces
            copyPieces(pieces, simPieces);

            // Update pixel positions
            for (Piece p : pieces) {
                p.x = p.getX(p.col);
                p.y = p.getY(p.row);
            }

            // Reset timer
            lastTimerTime = System.currentTimeMillis();
            gameOver = false;
            activeP = null;
            promotion = false;

            System.out.println("Đã tải game: " + filepath);
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi khi tải game: " + e.getMessage());
            return false;
        }
    }

    // Tạo quân cờ từ type
    private Piece createPiece(Type type, int col, int row, int color) {
        switch (type) {
            case PAWN:
                return new piece.Pawn(col, row, color);
            case ROOK:
                return new piece.Rook(col, row, color);
            case KNIGHT:
                return new piece.Knight(col, row, color);
            case BISHOP:
                return new piece.Bishop(col, row, color);
            case QUEEN:
                return new piece.Queen(col, row, color);
            case KING:
                return new piece.King(col, row, color);
            default:
                return null;
        }
    }

    // Inner class for history
    public class HistoryMove {
        public ArrayList<Piece> pieceList;
        public ArrayList<Piece> capturedList;
        public int wTime, bTime;
        public int turn;
        public Piece castlingP;
    }
}
