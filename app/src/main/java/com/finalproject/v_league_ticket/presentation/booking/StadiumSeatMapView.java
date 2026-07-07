package com.finalproject.v_league_ticket.presentation.booking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StadiumSeatMapView extends View {
    public interface OnSeatClickListener {
        void onSeatClicked(TicketSeat seat);
    }

    private static final float LOGICAL_W = 620f;
    private static final float LOGICAL_H = 360f;
    private static final float PITCH_X = 190f;
    private static final float PITCH_Y = 115f;
    private static final float PITCH_W = 240f;
    private static final float PITCH_H = 150f;
    private static final float SEAT_SIZE = 13f;
    private static final float STEP = 16f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<TicketSeat> seats = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private OnSeatClickListener listener;
    private float scale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float zoomFactor = 1f;
    private String currentUid = "";

    public StadiumSeatMapView(Context context) {
        super(context);
        init();
    }

    public StadiumSeatMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        textPaint.setColor(Color.rgb(6, 18, 44));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setOnSeatClickListener(OnSeatClickListener listener) {
        this.listener = listener;
    }

    public void setCurrentUid(String currentUid) {
        this.currentUid = currentUid == null ? "" : currentUid;
        invalidate();
    }

    public void setSeats(List<TicketSeat> newSeats) {
        seats.clear();
        if (newSeats != null) seats.addAll(newSeats);
        invalidate();
    }

    public void setSelectedIds(Set<String> ids) {
        selectedIds.clear();
        if (ids != null) selectedIds.addAll(ids);
        invalidate();
    }

    public void setZoomFactor(float zoomFactor) {
        this.zoomFactor = Math.max(0.8f, Math.min(1.8f, zoomFactor));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        computeTransform();
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);
        drawBackground(canvas);
        drawPitch(canvas);
        drawSeats(canvas);
        drawLabels(canvas);
        canvas.restore();
    }

    private void computeTransform() {
        scale = Math.min(getWidth() / LOGICAL_W, getHeight() / LOGICAL_H) * 0.98f * zoomFactor;
        offsetX = (getWidth() - LOGICAL_W * scale) / 2f;
        offsetY = (getHeight() - LOGICAL_H * scale) / 2f;
    }

    private void drawBackground(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(241, 245, 249));
        canvas.drawRoundRect(new RectF(0f, 0f, LOGICAL_W, LOGICAL_H), 24f, 24f, paint);
    }

    private void drawPitch(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(18, 183, 106));
        canvas.drawRoundRect(new RectF(PITCH_X, PITCH_Y, PITCH_X + PITCH_W, PITCH_Y + PITCH_H), 10f, 10f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.4f);
        paint.setColor(Color.argb(190, 255, 255, 255));
        canvas.drawRect(PITCH_X + 12f, PITCH_Y + 12f, PITCH_X + PITCH_W - 12f, PITCH_Y + PITCH_H - 12f, paint);
        canvas.drawLine(PITCH_X + PITCH_W / 2f, PITCH_Y + 12f, PITCH_X + PITCH_W / 2f, PITCH_Y + PITCH_H - 12f, paint);
        canvas.drawCircle(PITCH_X + PITCH_W / 2f, PITCH_Y + PITCH_H / 2f, 34f, paint);
        canvas.drawRect(PITCH_X + 12f, PITCH_Y + PITCH_H / 2f - 48f, PITCH_X + 70f, PITCH_Y + PITCH_H / 2f + 48f, paint);
        canvas.drawRect(PITCH_X + PITCH_W - 70f, PITCH_Y + PITCH_H / 2f - 48f, PITCH_X + PITCH_W - 12f, PITCH_Y + PITCH_H / 2f + 48f, paint);
    }

    private void drawLabels(Canvas canvas) {
        textPaint.setColor(Color.rgb(6, 18, 44));
        textPaint.setTextSize(10f);
        canvas.drawText("KHÁN ĐÀI C", PITCH_X + PITCH_W / 2f, 18f, textPaint);
        canvas.drawText("KHÁN ĐÀI A (VIP)", PITCH_X - 116f, PITCH_Y - 30f, textPaint);
        canvas.drawText("KHÁN ĐÀI B", PITCH_X + PITCH_W + 116f, PITCH_Y - 30f, textPaint);
        canvas.drawText("KHÁN ĐÀI D", PITCH_X + PITCH_W / 2f, PITCH_Y + PITCH_H + 20f, textPaint);
    }

    private void drawSeats(Canvas canvas) {
        long now = System.currentTimeMillis();
        paint.setStyle(Paint.Style.FILL);
        boolean preview = seats.isEmpty();
        List<TicketSeat> drawableSeats = preview ? previewSeats() : seats;
        for (TicketSeat seat : drawableSeats) {
            RectF rect = seatRect(seat);
            paint.setColor(preview ? previewColor(seat) : colorForSeat(seat, now));
            canvas.drawRoundRect(rect, 2.5f, 2.5f, paint);
        }
    }

    private int previewColor(TicketSeat seat) {
        return seat.isVip() ? Color.rgb(245, 158, 11) : Color.rgb(59, 130, 246);
    }

    private List<TicketSeat> previewSeats() {
        List<TicketSeat> output = new ArrayList<>();
        appendPreview(output, "A", "VIP", 14, 8);
        appendPreview(output, "B", "REGULAR", 14, 8);
        appendPreview(output, "C", "REGULAR", 4, 18);
        appendPreview(output, "D", "REGULAR", 4, 18);
        return output;
    }

    private void appendPreview(List<TicketSeat> output, String stand, String type, int rows, int cols) {
        for (int row = 1; row <= rows; row++) {
            for (int col = 1; col <= cols; col++) {
                output.add(new TicketSeat("preview-" + stand + "-" + row + "-" + col, stand, "", type, row, col, 0));
            }
        }
    }

    private int colorForSeat(TicketSeat seat, long now) {
        if (selectedIds.contains(seat.getId()) || seat.isLockedBy(currentUid, now)) {
            return Color.rgb(34, 197, 94);
        }
        if (seat.isSold() || seat.isLockedByOther(currentUid, now)) {
            return Color.rgb(75, 85, 99);
        }
        if (seat.isVip()) return Color.rgb(245, 158, 11);
        return Color.rgb(59, 130, 246);
    }

    private RectF seatRect(TicketSeat seat) {
        float x;
        float y;
        switch (seat.getStand()) {
            case "A":
                x = PITCH_X - 8f * STEP - 30f + (seat.getCol() - 1) * STEP;
                y = PITCH_Y - 20f + (seat.getRow() - 1) * STEP;
                break;
            case "B":
                x = PITCH_X + PITCH_W + 30f + (seat.getCol() - 1) * STEP;
                y = PITCH_Y - 20f + (seat.getRow() - 1) * STEP;
                break;
            case "C":
                x = PITCH_X - 34f + (seat.getCol() - 1) * STEP;
                y = PITCH_Y - 4f * STEP - 26f + (seat.getRow() - 1) * STEP;
                break;
            case "D":
                x = PITCH_X - 34f + (seat.getCol() - 1) * STEP;
                y = PITCH_Y + PITCH_H + 28f + (seat.getRow() - 1) * STEP;
                break;
            default:
                x = 0f;
                y = 0f;
        }
        return new RectF(x, y, x + SEAT_SIZE, y + SEAT_SIZE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
        if (listener == null || seats.isEmpty()) return true;
        float lx = (event.getX() - offsetX) / scale;
        float ly = (event.getY() - offsetY) / scale;
        for (TicketSeat seat : seats) {
            RectF hitRect = seatRect(seat);
            hitRect.inset(-4f, -4f);
            if (hitRect.contains(lx, ly)) {
                listener.onSeatClicked(seat);
                return true;
            }
        }
        return true;
    }

    public String accessibilitySummary() {
        return String.format(Locale.US, "%d seats, %d selected", seats.size(), selectedIds.size());
    }
}
