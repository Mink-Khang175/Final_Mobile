package com.finalproject.v_league_ticket.presentation.booking;

public class TicketSeat {
    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_LOCKED = "locked";
    public static final String STATUS_SOLD = "sold";

    private final String id;
    private final String stand;
    private final String seatNumber;
    private final String type;
    private final int row;
    private final int col;
    private final int price;
    private String status = STATUS_AVAILABLE;
    private String ownerId = "";
    private long lockUntilMs = 0L;

    public TicketSeat(String id, String stand, String seatNumber, String type, int row, int col, int price) {
        this.id = id;
        this.stand = stand;
        this.seatNumber = seatNumber;
        this.type = type;
        this.row = row;
        this.col = col;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getStand() {
        return stand;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getType() {
        return type;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public long getLockUntilMs() {
        return lockUntilMs;
    }

    public void markAvailable() {
        status = STATUS_AVAILABLE;
        ownerId = "";
        lockUntilMs = 0L;
    }

    public void applyRemoteState(String status, String ownerId, long lockUntilMs) {
        this.status = status == null || status.isEmpty() ? STATUS_AVAILABLE : status;
        this.ownerId = ownerId == null ? "" : ownerId;
        this.lockUntilMs = Math.max(0L, lockUntilMs);
    }

    public boolean isVip() {
        return "VIP".equalsIgnoreCase(type);
    }

    public boolean isSold() {
        return STATUS_SOLD.equals(status);
    }

    public boolean isLocked(long nowMs) {
        return STATUS_LOCKED.equals(status) && lockUntilMs > nowMs;
    }

    public boolean isLockedBy(String uid, long nowMs) {
        return isLocked(nowMs) && ownerId.equals(uid);
    }

    public boolean isLockedByOther(String uid, long nowMs) {
        return isLocked(nowMs) && !ownerId.equals(uid);
    }
}
